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
import java.rmi.NotBoundException;
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
import java.util.concurrent.ExecutorService;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import server.rmi.RemoteRegistration;
import server.rmi.ServerCallback;
import server.config.ServerConfig;
import server.storage.PostStorage;
import server.storage.ServerStorage;
import server.storage.Storage;
import server.storage.UserStorage;
import common.User;
import common.request.RequestObject;
import common.rmi.RemoteRegistrationInterface;
import common.rmi.ServerCallbackInterface;
import common.Post;

public class ServerMain implements Runnable {

    // ########## CONFIGURATION DATA ##########

    // worker threads threadpool and parameters
    private static final int CPUS = Runtime.getRuntime().availableProcessors();
    private static final int CPU_MULT = 2;
    private ExecutorService workerPool;

    // rmi callback handle
    protected ServerCallback callbackHandle;
	
	// used for selection
	private ServerSocketChannel managerChannel;
    private Selector selector;
	
	// buffer to be used in NIO data exchanges
	private static final int BUF_DIM = 8192;
	private ByteBuffer readBuffer = ByteBuffer.allocate(BUF_DIM);


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
        this.taskQueue = new LinkedBlockingQueue<>();
        this.connectedUsers = new ConcurrentHashMap<>();
        this.loggedUsers = new ConcurrentHashMap<>();
        this.callbackHandle = this.startCallback();
    }


    
    /** 
     * @throws IOException
     */
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

        // starting the worker threadpool
        this.workerPool = Executors.newFixedThreadPool(CPUS*CPU_MULT);
        for(int i=0; i<(CPUS*CPU_MULT); i++) {
            this.workerPool.submit(
                new ServerWorker(
                    this.serverStorage,
                    this.callbackHandle,
                    this.taskQueue,
                    this.connectedUsers,
                    this.loggedUsers)
            );
        }
        
        // starting the reward worker thread
        Thread rewardWorker = new Thread(new ServerMulticastWorker(this.serverStorage));
        rewardWorker.start();

        // printing connection information
        System.out.println("Server listening on "+config.getAddr()+':'+config.getPort());

        // adding a cleanup-handler
        Thread cleanupThreads = new Thread(() -> {
            this.workerPool.shutdown();
            rewardWorker.interrupt();
        });

        // registering the cleanup-handler
        Runtime.getRuntime().addShutdownHook(cleanupThreads);
    }


    
    /** 
     * @param key
     * @throws IOException
     * @throws NullPointerException
     */
    // method that handles the acceptance of incoming connections on a key
    private void acceptConnection(SelectionKey key) throws IOException, NullPointerException {
        Objects.requireNonNull(key);

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


    
    /** 
     * @param key
     * @throws IOException
     * @throws NullPointerException
     */
    // method that handles reading the clients' requests
    private void readRequest(SelectionKey key) throws IOException, NullPointerException {
        Objects.requireNonNull(key);

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
            System.out.println("Client "+sockChannel.socket().toString()+" disconnected");
            this.disconnectUser(sockChannel);
            key.cancel();
            sockChannel.close();
            return;
        }

        if(bytesRead < 0) {
            // the client disconnected, cancel the key and close the connection
            System.out.println("Client "+sockChannel.socket().toString()+" disconnected");
            this.disconnectUser(sockChannel);
            key.cancel();
            sockChannel.close();
            return;
        }

        byte[] data = this.readBuffer.array();

        // extracting the JSON string representing the request from the buffer
        String jsonRequest = new String(data, StandardCharsets.UTF_8);

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


    
    /** 
     * @return ServerCallback
     * @throws RemoteException
     */
    // sets up the RMI callback service for follow/unfollow operations
    public ServerCallback startCallback() throws RemoteException {
        ServerCallback rmiHandle = new ServerCallback();
        ServerCallbackInterface stub = (ServerCallbackInterface) UnicastRemoteObject.exportObject(rmiHandle, config.getCallbackPort());
        LocateRegistry.createRegistry(config.getCallbackPort());
        Registry reg = LocateRegistry.getRegistry(config.getCallbackPort());
        reg.rebind(config.getCallbackName(), stub);

        // adding a cleanup-handler
        Thread cleanupNotificationService = new Thread(() -> {
            try {
                System.out.println("exiting notification service...");
                reg.unbind(config.getCallbackName());
                UnicastRemoteObject.unexportObject(rmiHandle, true);
            } catch(RemoteException | NotBoundException e) {
                System.err.println("Error during notification service cleanup");
                e.printStackTrace();
                System.exit(1);
            }
        });

        // registering the cleanup-handler
        Runtime.getRuntime().addShutdownHook(cleanupNotificationService);

        return rmiHandle;
    }


    
    /** 
     * @throws RemoteException
     */
    // sets up the RMI registration service
    public void startRegistrationService() throws RemoteException {
        // generating and exposing the remote object
        RemoteRegistration remoteRegistration = new RemoteRegistration(this.serverStorage);
        RemoteRegistrationInterface stub = (RemoteRegistrationInterface) 
            UnicastRemoteObject.exportObject(remoteRegistration, config.getRmiPort());
        
        // creating and retrieving the RMI registry
        LocateRegistry.createRegistry(config.getRmiPort());
        Registry reg = LocateRegistry.getRegistry(config.getRmiAddr(), config.getRmiPort());

        // binding the registration stub to its symbolic name
        reg.rebind(config.getRmiName(), stub);

        // adding a cleanup-handler
        Thread cleanupRegisterService = new Thread(() -> {
            try {
                System.out.println("exiting remote registration service...");
                reg.unbind(config.getRmiName());
                UnicastRemoteObject.unexportObject(remoteRegistration, true);
            } catch(RemoteException | NotBoundException e) {
                System.err.println("Error during remote registration service cleanup");
                e.printStackTrace();
                System.exit(1);
            }
        });

        // registering the cleanup-handler
        Runtime.getRuntime().addShutdownHook(cleanupRegisterService);
    }


    
    /** 
     * @param channel
     */
    // removes all references to the disconnected user
    private void disconnectUser(SocketChannel channel) {
        if((Objects.nonNull(channel)) && (Objects.nonNull(this.connectedUsers.get(channel.socket().toString())))) {
            this.loggedUsers.remove(this.connectedUsers.remove(channel.socket().toString()));
        }
    }


    // thread main function
    public void run() {

        // starting up all of the server's services
        try {
            this.startServer();
            this.startRegistrationService();
        } catch(IOException e) {
            System.err.println("Error starting the server. Quitting...");
            System.exit(1);
        }

        // server's main loop
        while(!(Thread.currentThread().isInterrupted())) {
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
                        this.readRequest(selKey);
                    }
                }
            } catch(IOException e) {
                System.err.println("Critical I/O failure");
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
	

	
    /** 
     * @param args
     */
    public static void main(String[] args) {
        try {
            new Thread(new ServerMain()).start();   // starting server's main thread
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
	}
}
