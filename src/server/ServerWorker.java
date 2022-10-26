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


    // stock message for write-fails
    private static final String FAILED_WRITE = "Write operation failed";


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
                    this.login(task.getRequest(), task.getSock());
                    break;

                case "logout":
                    this.logout(task.getRequest(), task.getSock());
                    break;

                case "list users":
                    this.listUsers(task.getRequest(), task.getSock());
                    break;
            
                default:
                    this.invalidCommand(task.getSock());
                    break;
            }
        }
    }


    public void sendResponse(ResponseObject response, SocketChannel sock) {
        Gson gson = new GsonBuilder().serializeNulls().create();
        String jsonResponse = gson.toJson(response);
        
        try {
            // sending the result through the socket
            sock.write(ByteBuffer.wrap(jsonResponse.getBytes(StandardCharsets.UTF_8)));
        } catch(IOException e) {
            System.err.println(FAILED_WRITE);
        }
    }


    // ########## REQUEST EXECUTION METHODS ##########

    private void login(RequestObject request, SocketChannel sock) {
        User user = this.serverStorage.getUserStorage().get(request.getUsername());
        if(Objects.nonNull(user)) {
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
                
                // returning error if already logged
                if(Objects.nonNull(this.connectedUsers.get(sock.socket().toString()))) {
                    
                    // writing the response to the client
                    ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "You are already logged in", null);
                    sendResponse(response, sock);
                    return;
                }

                // linking the client's socket to the auth token
                this.connectedUsers.putIfAbsent(sock.socket().toString(), token);

                // linking the auth token to the authenticated user
                this.loggedUsers.putIfAbsent(token, user);

                // writing the response to the client
                ResponseObject response = new ResponseObject(ResponseObject.Result.SUCCESS, "Successfully logged in", token);
                sendResponse(response, sock);
            }
            else {
                // writing the response to the client
                ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "Wrong password", null);
                sendResponse(response, sock);
            }
        }
        else {
            // writing the response to the client
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "The user does not exist", null);
            sendResponse(response, sock);
        }
    }


    private void logout(RequestObject request, SocketChannel sock) {
        
        // disconnecting the client
        if(this.connectedUsers.get(sock.socket().toString()).equals(request.getToken()) &&
            Objects.nonNull(this.loggedUsers.get(this.connectedUsers.get(sock.socket().toString())))) {
            
            // disconnecting and logging out the user
            this.loggedUsers.remove(this.connectedUsers.remove(sock.socket().toString()));

            // sending success response
            ResponseObject response = new ResponseObject(ResponseObject.Result.SUCCESS, "Successfully logged out", null);
            sendResponse(response, sock);
        }
        else {
            // the user was not logged in, sending error response
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "You are not logged in", null);
            sendResponse(response, sock);
        }
    }

    
    private void listUsers(RequestObject request, SocketChannel sock) {
        User user = this.serverStorage.getUserStorage().get(request.getToken());
        String[] userTags = user.getTags();
    }

    /*
    private void listFollowers(RequestObject request, SocketChannel sock);

    private void listFollowing(RequestObject request, SocketChannel sock);

    private void followUser(RequestObject request, SocketChannel sock);

    private void unfollowUser(RequestObject request, SocketChannel sock);

    private void viewBlog(RequestObject request, SocketChannel sock);

    private void createPost(RequestObject request, SocketChannel sock);

    private void showFeed(RequestObject request, SocketChannel sock);

    private void showPost(RequestObject request, SocketChannel sock);

    private void deletePost(RequestObject request, SocketChannel sock);

    private void rewinPost(RequestObject request, SocketChannel sock);

    private void ratePost(RequestObject request, SocketChannel sock);

    private void addComment(RequestObject request, SocketChannel sock);

    private void getWallet(RequestObject request, SocketChannel sock);

    private void getWalletInBitcoin(RequestObject request, SocketChannel sock);

    */

    private void invalidCommand(SocketChannel sock) {

        // writing the response to the client
        ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "Invalid command", null);
        Gson gson = new GsonBuilder().serializeNulls().create();
        String jsonResponse = gson.toJson(response);
        
        try {
            // sending the result through the socket
            sock.write(ByteBuffer.wrap(jsonResponse.getBytes(StandardCharsets.UTF_8)));
        } catch(IOException e) {
            System.err.println(FAILED_WRITE);
        }
    }
}
