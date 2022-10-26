package server;

import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentMap;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import common.User;
import common.Post;
import common.Comment;
import common.request.RequestObject;
import common.request.ResponseObject;
import common.crypto.Cryptography;
import server.config.ServerConfig;
import server.rmi.ServerCallback;
import server.storage.ServerStorage;
import server.storage.Storage;
import server.ServerMain;

class ServerWorker implements Runnable {

    // ########## VARIABLES ##########

    // worker threads threadpool and parameters
    private static final int CPUS = Runtime.getRuntime().availableProcessors();
    private ThreadPoolExecutor workerPool;

    // pipe channel to communicate OP_WRITE requests
    private Pipe.SinkChannel pipe;
    private Queue<WritingTask> registrationQueue;

    // queue used to get tasks from the main thread
    private BlockingQueue<ServerTask> taskQueue;

    // map linking a connected client's socket to their auth token
    private ConcurrentMap<String, String> connectedUsers;

    // map linking a logged in client's auth token to their user profile
    private ConcurrentMap<String, User> loggedUsers;

    // server-storage object
    private ServerStorage serverStorage;

    // handle for follow/unfollow callbacks
    private ServerCallback callbackHandle;


    // ########## METHODS #########

    // constructor
    public ServerWorker(ServerStorage serverStorage,
            ServerCallback callbackHandle,
            Pipe.SinkChannel pipe,
            BlockingQueue<ServerTask> taskQueue,
            Queue<WritingTask> registrationQueue,
            ConcurrentMap<String, String> connectedUsers,
            ConcurrentMap<String, User> loggedUsers) {
        this.serverStorage = Objects.requireNonNull(serverStorage, "Server storage cannot be null");
        this.callbackHandle = Objects.requireNonNull(callbackHandle, "callback object cannot be null");
        this.pipe = Objects.requireNonNull(pipe, "Main-Worker pipe cannot be null");
        this.taskQueue = Objects.requireNonNull(taskQueue, "Task queue cannot be null");
        this.registrationQueue = Objects.requireNonNull(registrationQueue, "Write-task queue cannot be null");
        this.connectedUsers = Objects.requireNonNull(connectedUsers, "Connected users map cannot be null");
        this.loggedUsers = Objects.requireNonNull(loggedUsers, "Logged users map cannot be null");
        this.workerPool = (ThreadPoolExecutor)Executors.newFixedThreadPool(CPUS*ServerMain.config.getCoreMult());
    }


    // request dispatching
    public void run() {
        ServerTask task = null;
        while(!(ServerMain.isStopping.get())) {
            try {
                task = this.taskQueue.take();
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // dispatching the request to the relative method
            switch (task.getRequest().getCommand()) {
                case "login":
                    this.login(task);
                    break;
            
                default:
                    break;
            }
        }
    }


    // ########## REQUEST EXECUTION METHODS ##########

    private void login(ServerTask task) {
        RequestObject request = task.getRequest();
        SocketChannel sock = task.getSock();
        User user = this.serverStorage.getUserStorage().get(request.getUsername());
        if(!(Objects.isNull(user))) {
            String providedPassword = "";
            
            try {
                providedPassword = Cryptography.digest(request.getPassword());
            } catch(NoSuchAlgorithmException e) {
                System.err.println("Could not hash the provided password");
                return;
            }

            if(user.checkPassword(providedPassword)) {
                // registering the client's auth token
                String token = Cryptography.getSecureToken();
                this.connectedUsers.putIfAbsent(sock.toString(), token);

                // linking the auth token to the authenticated user
                this.loggedUsers.putIfAbsent(token, user);

                // writing the response to the client
                ResponseObject response = new ResponseObject(ResponseObject.Result.SUCCESS, "Successfully logged in", token);
                Gson gson = new GsonBuilder().serializeNulls().create();
                String jsonResponse = gson.toJson(response);
                
                try {
                    // sending the result through the socket
                    sock.write(ByteBuffer.wrap(jsonResponse.getBytes(StandardCharsets.UTF_8)));
                } catch(IOException e) {
                    System.err.println("Write operation failed");
                }
            }
            else {
                // writing the response to the client
                ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "Wrong password", null);
                Gson gson = new GsonBuilder().serializeNulls().create();
                String jsonResponse = gson.toJson(response);
                
                try {
                    // sending the result through the socket
                    sock.write(ByteBuffer.wrap(jsonResponse.getBytes(StandardCharsets.UTF_8)));
                } catch(IOException e) {
                    System.err.println("Write operation failed");
                }
            }
        }
        else {
            // writing the response to the client
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "The user does not exist", null);
            Gson gson = new GsonBuilder().serializeNulls().create();
            String jsonResponse = gson.toJson(response);
            
            try {
                // sending the result through the socket
                sock.write(ByteBuffer.wrap(jsonResponse.getBytes(StandardCharsets.UTF_8)));
            } catch(IOException e) {
                System.err.println("Write operation failed");
            }
        }
    }

    /*

    private void logout(RequestObject request);

    private void listUsers(RequestObject request);

    private void listFollowers(RequestObject request);

    private void listFollowing(RequestObject request);

    private void followUser(RequestObject request);

    private void unfollowUser(RequestObject request);

    private void viewBlog(RequestObject request);

    private void createPost(RequestObject request);

    private void showFeed(RequestObject request);

    private void showPost(RequestObject request);

    private void deletePost(RequestObject request);

    private void rewinPost(RequestObject request);

    private void ratePost(RequestObject request);

    private void addComment(RequestObject request);

    private void getWallet(RequestObject request);

    private void getWalletInBitcoin(RequestObject request);

    */
}
