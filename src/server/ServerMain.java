package server;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.net.InetAddress;
import java.nio.channels.Pipe;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.Selector;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.nio.channels.SelectionKey;
import java.util.concurrent.Executors;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.StandardCharsets;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import server.rmi.ServerCallback;
import server.config.ServerConfig;
import server.storage.PostStorage;
import server.storage.ServerStorage;
import server.storage.Storage;
import server.storage.UserStorage;
import common.User;
import common.request.RequestObject;
import common.rmi.ServerCallbackInterface;
import common.Post;

public class ServerMain implements Runnable {

    // ########## CONFIGURATION DATA ##########

    // rmi callback handle
    protected ServerCallback callbackHandle;
	
	// used for selection
	private ServerSocketChannel managerChannel;
    private Selector selector;

    // used for switching channel mode between OP_READ and OP_WRITE
    private Pipe registrationPipe;
    private Queue<WritingTask> registrationQueue;
	
	// buffer to be used in NIO data exchanges
	private static final int BUF_DIM = 8192;
	private ByteBuffer readBuffer = ByteBuffer.allocate(BUF_DIM);

    // main-loop determinant
    public static AtomicBoolean isStopping = new AtomicBoolean();


    // ########## SERVICE DATA ##########

    // map linking a connected client's socket to their auth token
    protected ConcurrentMap<String, String> connectedUsers;

    // map linking a logged in client's auth token to their user profile
    protected ConcurrentMap<String, User> loggedUsers;

    // concurrent queue used to transfer tasks to the workers
    BlockingQueue<ServerTask> taskQueue;

    // server-storage object
    protected ServerStorage serverStorage;

    // generating the server's config object (contains all configuration fields)
    public static final ServerConfig config = ServerConfig.getServerConfig();
    


    // ########## METHODS ##########

    // constructor
    private ServerMain() throws IOException {
        this.serverStorage = new ServerStorage(
            new UserStorage(config.getStoragePath(), config.getUserStoragePath()),
            new PostStorage(config.getStoragePath(), config.getPostStoragePath()));
        this.registrationQueue = new ConcurrentLinkedQueue<>();
        this.taskQueue = new LinkedBlockingQueue<>();
        this.connectedUsers = new ConcurrentHashMap<>();
        this.loggedUsers = new ConcurrentHashMap<>();
        this.registrationPipe = Pipe.open();
        this.callbackHandle = this.startCallback();
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
        new Thread(new RemoteTask(this.serverStorage)).start();

        // starting the worker thread
        new Thread(new ServerWorker(this.serverStorage,
            this.callbackHandle,
            this.registrationPipe.sink(),
            this.taskQueue,
            this.registrationQueue,
            this.connectedUsers,
            this.loggedUsers)).start();

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
            System.out.println("Client disconnected");
            this.disconnectUser(sockChannel);
            key.cancel();
            sockChannel.close();
            return;
        }

        if(bytesRead < 0) {
            // the client disconnected, cancel the key and close the connection
            System.out.println("Client disconnected");
            this.disconnectUser(sockChannel);
            key.cancel();
            sockChannel.close();
            return;
        }

        byte[] data = this.readBuffer.array();

        // extracting the JSON string representing the request from the buffer
        String jsonRequest = new String(data, StandardCharsets.UTF_8);
        System.out.println("\nDEBUG: "+jsonRequest);    // TODO REMOVE

        // extracting a RequestObject from the JSON string and putting it in the task
        Gson gson = new GsonBuilder().serializeNulls().create();
        JsonReader reader = new JsonReader(new StringReader(jsonRequest));
        reader.setLenient(true);
        RequestObject request = gson.fromJson(reader, RequestObject.class);
        this.readBuffer.clear();
        
        // hand over the request to the worker threads
        try {
            this.taskQueue.put(new ServerTask(sockChannel, request));
        } catch(InterruptedException e) {
            // TODO CLOSE
            Thread.currentThread().interrupt();
        }
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


    // removes all references to the disconnected user
    private void disconnectUser(SocketChannel channel) {

        if((Objects.nonNull(channel)) && (Objects.nonNull(this.connectedUsers.get(channel.toString())))) {
            this.loggedUsers.remove(this.connectedUsers.remove(channel.toString()));
        }
    }


    // thread main function
    public void run() {

        // starting up all of the server's services
        try {
            this.startServer();
        } catch(IOException e) {
            System.err.println("Error starting the server. Quitting...");
            System.exit(1);
        }

        // server's main loop
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
