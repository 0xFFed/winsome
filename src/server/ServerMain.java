package server;

import java.util.HashMap;
import java.util.Iterator;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.net.InetAddress;
import java.nio.channels.Pipe;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.Selector;
import java.util.concurrent.Future;
import java.nio.channels.SelectionKey;
import java.util.concurrent.Executors;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import server.rmi.ServerCallback;
import server.config.ServerConfig;
import server.storage.Storage;
import common.User;
import common.rmi.ServerCallbackInterface;
import common.Post;

public class ServerMain implements Runnable {

    // ########## CONFIGURATION DATA ##########

    // worker pool wrapper
    private ServerWorkerPool workerPool;

    // rmi callback handle
    protected ServerCallback callbackHandle;
	
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
    public static AtomicBoolean isStopping = new AtomicBoolean();


    // ########## SERVICE DATA ##########

    // data structure mapping a logged in client's socket to their username
    protected HashMap<String, String> loggedUsers;

    // user-storage and post-storage objects
    protected Storage<User> userStorage;
    protected Storage<Post> postStorage;

    // generating the server's config object (contains all configuration fields)
    public static final ServerConfig config = ServerConfig.getServerConfig();
    


    // ########## METHODS ##########

    // constructor
    private ServerMain() throws IOException {
        this.userStorage = new Storage<>(config.getStoragePath(), config.getUserStoragePath());
        this.postStorage = new Storage<>(config.getStoragePath(), config.getPostStoragePath());
        this.loggedUsers = new HashMap<>();
        this.callbackHandle = this.startCallback();
        this.workerPool = new ServerWorkerPool(this.userStorage, this.postStorage, this.callbackHandle);
        this.registrationPipe = Pipe.open();
        this.startServer();
    }


    // Selector and Accepting Socket setup
    private void startServer() throws IOException {

        // creating selector
        this.selector = SelectorProvider.provider().openSelector();

        // creating a non-blocking ServerSocketChannel
        this.managerChannel = ServerSocketChannel.open();
        this.managerChannel.configureBlocking(false);

        // setting up the listener socket
        InetSocketAddress sockAddr = new InetSocketAddress(config.getAddr(), config.getPort());
        this.managerChannel.socket().bind(sockAddr);
        
        // setting up the manager socket as an "accepting socket"
        this.managerChannel.register(this.selector, SelectionKey.OP_ACCEPT);

        // setting up the registration pipe's read-end
        this.registrationPipe.source().configureBlocking(false);
        this.registrationPipe.source().register(this.selector, SelectionKey.OP_READ);

        // starting up the RMI handler thread
        new Thread(new RemoteTask(this.userStorage, this.postStorage)).start();

        // printing connection information
        System.out.println("Server listening on "+config.getAddr()+':'+config.getPort());
    }


    // method that handles the acceptance of incoming connections on a key
    private void acceptConnection(SelectionKey key) throws IOException {
        // getting the acceptance socket
        ServerSocketChannel serverSockChannel = (ServerSocketChannel)key.channel();

        // generating a socket for the client in non-blocking mode
        SocketChannel sockChannel = serverSockChannel.accept();
        sockChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        sockChannel.configureBlocking(false);

        // registering the new socket channel with the selector on the READ op
        sockChannel.register(this.selector, SelectionKey.OP_READ);

        System.out.println("Connection accepted from "+sockChannel.socket().toString());
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
            System.out.println("Client disconnected");
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


    // sets up the RMI callback service for follow/unfollow operations
    public ServerCallback startCallback() throws RemoteException {
        ServerCallback rmiHandle = new ServerCallback();
        ServerCallbackInterface stub = (ServerCallbackInterface) UnicastRemoteObject.exportObject(rmiHandle, config.getCallbackPort());
        LocateRegistry.createRegistry(config.getCallbackPort());
        Registry reg = LocateRegistry.getRegistry(config.getCallbackPort());
        reg.rebind(config.getCallbackName(), stub);

        return rmiHandle;
    }


    // thread main function
    public void run() {
        while(!(isStopping.get())) {
            try {
                // waiting for the accepting channel to become readable
                int timeLeft = this.selector.select(config.getTimeout());
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
            } catch(IOException e) {
                System.err.println("Critical I/O failure");
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
	

	public static void main(String[] args) {
        try {
            new Thread(new ServerMain()).start();   // starting server's main thread
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
	}
}
