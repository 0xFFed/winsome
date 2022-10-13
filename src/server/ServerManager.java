package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class ServerManager implements Runnable {

    // ########## VARIABLES ##########

    // worker pool wrapper
    private ServerWorkerPool workerPool;
	
	// IP-port for listening
	private InetAddress hostAddr;
	private int port;
	
	// used for selection
	private ServerSocketChannel managerChannel;
    private Selector selector;

    // used for switching channel mode between OP_READ and OP_WRITE
    private Pipe registrationPipe;
    private ConcurrentLinkedQueue<WritingTask> registrationQueue;
	
	// buffer to be used in NIO data exchanges
	private static final int BUF_DIM = 8192;
	private ByteBuffer readBuffer = ByteBuffer.allocate(BUF_DIM);

    // main-loop determinant
    private static boolean isStopping = false;

    // timeout for the select call
    private static final int SEL_TIMEOUT = 100;


    // ########## METHODS ##########

    // constructor
    public ServerManager(InetAddress hostAddr, int port) throws IOException {
        System.out.println("Hello there");
        this.workerPool = new ServerWorkerPool();
        this.hostAddr = hostAddr;
        this.port = port;
        this.registrationPipe = Pipe.open();
        this.selector = this.createSelector();
    }


    // spawns a selector to be used in the server acceptance loop
    private Selector createSelector() throws IOException {

        // creating selector and setting up the main channel
        Selector sel = SelectorProvider.provider().openSelector();
        this.managerChannel = ServerSocketChannel.open();
        this.managerChannel.configureBlocking(false);
        System.out.println(this.managerChannel.toString());

        // setting up the listener socket
        InetSocketAddress sockAddr = new InetSocketAddress(this.hostAddr, this.port);
        this.managerChannel.socket().bind(sockAddr);
        
        // setting up the manager socket as an "accepting socket"
        this.managerChannel.register(sel, SelectionKey.OP_ACCEPT);

        // setting up the registration pipe's read-end
        this.registrationPipe.source().configureBlocking(false);
        this.registrationPipe.source().register(sel, SelectionKey.OP_READ);

        return sel;
    }


    // method that handles the acceptance of incoming connections on a key
    private void acceptConnection(SelectionKey key) throws IOException {
        // getting the acceptance socket
        ServerSocketChannel serverSockChannel = (ServerSocketChannel)key.channel();

        // generating a socket for the client in non-blocking mode
        SocketChannel sockChannel = serverSockChannel.accept();
        sockChannel.configureBlocking(false);

        // registering the new socket channel with the selector on the READ op
        sockChannel.register(this.selector, SelectionKey.OP_READ);
    }


    // method used to switch the mode (read/write) of a socket channel
    private void switchChannelMode() {
        // get the head of the queue
        WritingTask writingTask = registrationQueue.poll();
        if(writingTask == null) return;

        // getting the <channel, mode, message> triple
        SelectionKey key = writingTask.key;
        boolean isWriteMode = writingTask.isWriteMode;

        // registering the channel for the right operation
        if(isWriteMode) key.interestOps(SelectionKey.OP_WRITE);
        else key.interestOps(SelectionKey.OP_READ);
    }


    // method that handles reading the clients' requests
    private void readRequest(SelectionKey key) throws IOException {

        // getting the relevant channel from the active socket
        SocketChannel sockChannel = (SocketChannel)key.channel();

        // clearing buffer
        this.readBuffer.clear();

        // reading from the socket channel
        int bytesRead = 0;
        try {
            bytesRead = sockChannel.read(this.readBuffer);
        } catch(IOException e) {
            // connection was dropped, cancel the key and close it on server's part
            key.cancel();
            sockChannel.close();
            return;
        }

        if(bytesRead < 0) {
            // the client disconnected, cancel the key and close the connection
            key.cancel();
            sockChannel.close();
            return;
        }

        // extracting a bytes array from the buffer
        byte[] data = new byte[this.readBuffer.remaining()];
        this.readBuffer.get(data);

        // hand over the data read to a worker thread
        this.workerPool.dispatchRequest(key, this.registrationPipe.sink(), data, bytesRead);
    }


    // thread main function
    public void run() {
        while(!isStopping) {
            try {
                // waiting for the accepting channel to become readable
                int timeLeft = this.selector.select(SEL_TIMEOUT);
                if(timeLeft == 0) continue; // re-try if timeout was reached

                // iterate over the set of ready keys
                Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();
                while(selectedKeys.hasNext()) {
                    // get the next key and remove it from the set
                    SelectionKey selKey = selectedKeys.next();
                    selectedKeys.remove();

                    // check key validity
                    if(!selKey.isValid()) continue;

                    // if a connection attempt was made, accept it
                    if(selKey.isAcceptable()) this.acceptConnection(selKey);
                    else if(selKey.isReadable()) {
                        if(selKey.channel() == registrationPipe.source()) this.switchChannelMode();
                        else this.readRequest(selKey);
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
	

	public static void main(String[] args) {
        System.out.println("Heeey");
        try {
            new Thread(new ServerManager(null, 8080)).start();
        } catch(IOException e) {
            e.printStackTrace();
        }
	}
}
