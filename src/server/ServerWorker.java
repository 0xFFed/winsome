package server;

import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
import java.nio.Buffer;
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

                case "list following":
                    this.listFollowing(task.getRequest(), task.getSock());
                    break;

                case "follow":
                    this.followUser(task.getRequest(), task.getSock());
                    break;

                case "unfollow":
                    this.unfollowUser(task.getRequest(), task.getSock());
                    break;

                case "blog":
                    this.viewBlog(task.getRequest(), task.getSock());
                    break;

                case "post":
                    this.createPost(task.getRequest(), task.getSock());
                    break;

                case "show feed":
                    this.showFeed(task.getRequest(), task.getSock());
                    break;

                case "show post":
                    this.showPost(task.getRequest(), task.getSock());
                    break;

                case "delete":
                    this.deletePost(task.getRequest(), task.getSock());
                    break;

                case "rewin":
                    this.rewinPost(task.getRequest(), task.getSock());
                    break;

                case "rate":
                    this.ratePost(task.getRequest(), task.getSock());
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
                    ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "You are already logged in", null, null, null);
                    sendResponse(response, sock);
                    return;
                }

                // linking the client's socket to the auth token
                this.connectedUsers.putIfAbsent(sock.socket().toString(), token);

                // linking the auth token to the authenticated user
                this.loggedUsers.putIfAbsent(token, user);

                // writing the response to the client
                ResponseObject response = new ResponseObject(ResponseObject.Result.SUCCESS, "Successfully logged in", token, null, null);
                sendResponse(response, sock);
            }
            else {
                // writing the response to the client
                ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "Wrong password", null, null, null);
                sendResponse(response, sock);
            }
        }
        else {
            // writing the response to the client
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "The user does not exist", null, null, null);
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
            ResponseObject response = new ResponseObject(ResponseObject.Result.SUCCESS, "Successfully logged out", null, null, null);
            sendResponse(response, sock);
        }
        else {
            // the user was not logged in, sending error response
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "You are not logged in", null, null, null);
            sendResponse(response, sock);
        }
    }

    
    private void listUsers(RequestObject request, SocketChannel sock) {
        User user = this.loggedUsers.get(request.getToken());

        if(Objects.isNull(user)) {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "You are not logged in", null, null, null);
            sendResponse(response, sock);
            return;
        }

        List<String> userTags = user.getTags();
        List<User> selectedUsers = new ArrayList<>();

        Iterator<User> userIter = this.serverStorage.getUserStorage().getUserSet().iterator();
        while(!(userTags.isEmpty()) && userIter.hasNext()) {
            User currUser = userIter.next();
            if(user.getUsername().equals(currUser.getUsername())) continue;
            for (String tag : currUser.getTags()) {
                if (userTags.contains(tag)) {
                    selectedUsers.add(currUser);
                    break;
                }
            }
        }

        ArrayList<String> result = new ArrayList<>();

        Iterator<User> tempResult = selectedUsers.iterator();
        while(!(userTags.isEmpty()) && tempResult.hasNext()) {
            User currUser = tempResult.next();
            result.add(currUser.getUsername());
        }

        ResponseObject response = new ResponseObject(ResponseObject.Result.SUCCESS, "Users sharing your tags: "+String.join(", ", result), null, result, null);
        sendResponse(response, sock);
    }

    /*
    private void listFollowers(RequestObject request, SocketChannel sock);

    */

    private void listFollowing(RequestObject request, SocketChannel sock) {
        User user = this.loggedUsers.get(request.getToken());

        if(Objects.isNull(user)) {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "You are not logged in", null, null, null);
            sendResponse(response, sock);
            return;
        }

        ResponseObject response = new ResponseObject(ResponseObject.Result.SUCCESS, "You are following: "+String.join(", ", user.getFollowings()), null, user.getFollowings(), null);
        sendResponse(response, sock);
    }

    
    private void followUser(RequestObject request, SocketChannel sock) {
        User user = this.loggedUsers.get(request.getToken());

        if(Objects.isNull(user)) {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "You are not logged in", null, null, null);
            sendResponse(response, sock);
            return;
        }

        User userToFollow = this.serverStorage.getUserStorage().get(request.getUsername());
        if(Objects.isNull(userToFollow)) {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "The specified user does not exist", null, null, null);
            sendResponse(response, sock);
            return;
        }

        if(user.getUsername().equals(userToFollow.getUsername())) {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "You cannot follow yourself", null, null, null);
            sendResponse(response, sock);
            return;
        }

        boolean success = userToFollow.addFollower(user);

        if(success) {
            this.serverStorage.getUserStorage().write();
            ResponseObject response = new ResponseObject(ResponseObject.Result.SUCCESS, "You are now following "+request.getUsername(), null, null, null);
            sendResponse(response, sock);
        }
        else {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "You are already following "+request.getUsername(), null, null, null);
            sendResponse(response, sock);
        }

    }


    private void unfollowUser(RequestObject request, SocketChannel sock) {
        User user = this.loggedUsers.get(request.getToken());

        if(Objects.isNull(user)) {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "You are not logged in", null, null, null);
            sendResponse(response, sock);
            return;
        }

        User userToUnfollow = this.serverStorage.getUserStorage().get(request.getUsername());
        if(Objects.isNull(userToUnfollow)) {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "The specified user does not exist", null, null, null);
            sendResponse(response, sock);
            return;
        }

        if(user.getUsername().equals(userToUnfollow.getUsername())) {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "You cannot unfollow yourself", null, null, null);
            sendResponse(response, sock);
            return;
        }

        boolean success = userToUnfollow.removeFollower(user);

        if(success) {
            this.serverStorage.getUserStorage().write();
            ResponseObject response = new ResponseObject(ResponseObject.Result.SUCCESS, "You are no longer following "+request.getUsername(), null, null, null);
            sendResponse(response, sock);
        }
        else {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "You were not following "+request.getUsername(), null, null, null);
            sendResponse(response, sock);
        }
    }

    
    private void viewBlog(RequestObject request, SocketChannel sock) {
        User user = this.loggedUsers.get(request.getToken());

        if(Objects.isNull(user)) {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "You are not logged in", null, null, null);
            sendResponse(response, sock);
            return;
        }

        ArrayList<Post> allPosts = this.serverStorage.getPostStorage().getPostSet();
        ArrayList<String> result = new ArrayList<>();

        Iterator<Post> postIter = allPosts.iterator();
        while(postIter.hasNext()) {
            Post currPost = postIter.next();
            if(currPost.getAuthor().equals(user.getUsername())) {
                result.add("Post ID: "+currPost.getPostId()+"\nAuthor: "+currPost.getAuthor()+"\nTitle: "+currPost.getTitle());
            }
        }

        ResponseObject response = new ResponseObject(ResponseObject.Result.SUCCESS, "Your blog:\n##########\n"+String.join("\n##########\n", result), null, result, null);
        sendResponse(response, sock);
    }

    
    private void createPost(RequestObject request, SocketChannel sock) {
        User user = this.loggedUsers.get(request.getToken());

        if(Objects.isNull(user)) {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "You are not logged in", null, null, null);
            sendResponse(response, sock);
            return;
        }

        Post newPost = new Post(request.getPost().getTitle(), request.getPost().getContent(), user.getUsername(), null, false);
        boolean success = this.serverStorage.getPostStorage().add(Integer.toString(newPost.getPostId()), newPost);

        if(success) {
            ResponseObject response = new ResponseObject(ResponseObject.Result.SUCCESS, "Post added", null, null, null);
            sendResponse(response, sock);
        }
        else {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "Error adding post", null, null, null);
            sendResponse(response, sock);
        }
    }

    
    private void showFeed(RequestObject request, SocketChannel sock) {
        User user = this.loggedUsers.get(request.getToken());

        if(Objects.isNull(user)) {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "You are not logged in", null, null, null);
            sendResponse(response, sock);
            return;
        }

        ArrayList<String> following = user.getFollowings();
        ArrayList<Post> allPosts = this.serverStorage.getPostStorage().getPostSet();
        ArrayList<String> result = new ArrayList<>();
        
        Iterator<Post> postIter = allPosts.iterator();
        while(postIter.hasNext()) {
            Post currPost = postIter.next();
            if(following.contains(currPost.getAuthor())) {
                result.add("Post ID: "+currPost.getPostId()+"\nAuthor: "+currPost.getAuthor()+"\nTitle: "+currPost.getTitle());
            }
        }

        ResponseObject response = new ResponseObject(ResponseObject.Result.SUCCESS, "Your feed:\n##########\n"+String.join("\n##########\n", result), null, result, null);
        sendResponse(response, sock);
    }

    
    private void showPost(RequestObject request, SocketChannel sock) {
        User user = this.loggedUsers.get(request.getToken());

        if(Objects.isNull(user)) {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "You are not logged in", null, null, null);
            sendResponse(response, sock);
            return;
        }

        Post post = this.serverStorage.getPostStorage().get(request.getData().get(0));

        if(Objects.isNull(post)) {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "This post does not exist", null, null, null);
            sendResponse(response, sock);
            return;
        }

        ArrayList<Comment> comments = post.getComments();
        ArrayList<String> commentList = new ArrayList<>();
        Iterator<Comment> commentIter = comments.iterator();
        while(commentIter.hasNext()) {
            Comment currComment = commentIter.next();
            commentList.add(currComment.toString());
        }

        String result = "\n##########\nTitle: "+post.getTitle()+"\nContent: "+post.getContent()+"\nLikes: "+
            post.getLikes().size()+"\nDislikes: "+post.getDislikes().size()+"\nComments: "+String.join("\n----------\n", commentList);
        if(post.isRewin()) result = result+"----------\n(Rewin from "+post.getOriginalAuthor()+"'s post)";
        
        ResponseObject response = new ResponseObject(ResponseObject.Result.SUCCESS, result, null, null, null);
        sendResponse(response, sock);
    }

    
    private void deletePost(RequestObject request, SocketChannel sock) {
        User user = this.loggedUsers.get(request.getToken());

        if(Objects.isNull(user)) {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "You are not logged in", null, null, null);
            sendResponse(response, sock);
            return;
        }

        Post post = this.serverStorage.getPostStorage().get(request.getData().get(0));

        if(Objects.isNull(post)) {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "The post does not exist", null, null, null);
            sendResponse(response, sock);
            return;
        }

        if(post.getAuthor().equals(user.getUsername())) {
            this.serverStorage.getPostStorage().remove(request.getData().get(0));
            ResponseObject response = new ResponseObject(ResponseObject.Result.SUCCESS, "Post succesfully removed", null, null, null);
            sendResponse(response, sock);
        }
        else {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "You are not the post owner", null, null, null);
            sendResponse(response, sock);
        }
    }

    
    private void rewinPost(RequestObject request, SocketChannel sock) {
        User user = this.loggedUsers.get(request.getToken());

        if(Objects.isNull(user)) {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "You are not logged in", null, null, null);
            sendResponse(response, sock);
            return;
        }

        Post post = this.serverStorage.getPostStorage().get(request.getData().get(0));

        if(Objects.isNull(post)) {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "The post does not exist", null, null, null);
            sendResponse(response, sock);
            return;
        }

        if(post.getAuthor().equals(user.getUsername()) || post.getOriginalAuthor().equals(user.getUsername())) {
            this.serverStorage.getPostStorage().remove(request.getData().get(0));
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "You are the author of this post", null, null, null);
            sendResponse(response, sock);
            return;
        }


        ArrayList<String> following = user.getFollowings();
        ArrayList<Post> allPosts = this.serverStorage.getPostStorage().getPostSet();
        ArrayList<String> feedPostsId = new ArrayList<>();
        
        Iterator<Post> postIter = allPosts.iterator();
        while(postIter.hasNext()) {
            Post currPost = postIter.next();
            if(following.contains(currPost.getAuthor())) {
                feedPostsId.add(Integer.toString(currPost.getPostId()));
            }
        }

        if(feedPostsId.contains(request.getData().get(0))) {
            Post rewin = new Post(post.getTitle(), post.getContent(), user.getUsername(), post.getAuthor(), true);
            this.serverStorage.getPostStorage().add(Integer.toString(rewin.getPostId()), rewin);
            ResponseObject response = new ResponseObject(ResponseObject.Result.SUCCESS, "Rewin completed", null, null, null);
            sendResponse(response, sock);
        }
        else {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "The post is not on your feed", null, null, null);
            sendResponse(response, sock);
        }
    }

    
    private void ratePost(RequestObject request, SocketChannel sock) {
        User user = this.loggedUsers.get(request.getToken());

        if(Objects.isNull(user)) {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "You are not logged in", null, null, null);
            sendResponse(response, sock);
            return;
        }

        Post post = this.serverStorage.getPostStorage().get(request.getData().get(0));

        if(Objects.isNull(post)) {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "The post does not exist", null, null, null);
            sendResponse(response, sock);
            return;
        }

        if(post.getAuthor().equals(user.getUsername())) {
            this.serverStorage.getPostStorage().remove(request.getData().get(0));
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "You are the author of this post", null, null, null);
            sendResponse(response, sock);
            return;
        }

        if(Objects.isNull(request.getData().get(0)) || Objects.isNull(request.getData().get(1)) ||
            (!(request.getData().get(1).equals("+1")) && !(request.getData().get(1).equals("-1")))) {
                ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "Invalid vote", null, null, null);
                sendResponse(response, sock);
                return;
        }


        ArrayList<String> following = user.getFollowings();
        ArrayList<Post> allPosts = this.serverStorage.getPostStorage().getPostSet();
        ArrayList<String> feedPostsId = new ArrayList<>();
        
        Iterator<Post> postIter = allPosts.iterator();
        while(postIter.hasNext()) {
            Post currPost = postIter.next();
            if(following.contains(currPost.getAuthor())) {
                feedPostsId.add(Integer.toString(currPost.getPostId()));
            }
        }

        if(feedPostsId.contains(request.getData().get(0))) {
            boolean success = false;
            if(request.getData().get(1).equals("+1")) success = post.like(user);
            else if(request.getData().get(1).equals("-1")) success = post.dislike(user);

            if(success) {
                this.serverStorage.getPostStorage().write();
                ResponseObject response = new ResponseObject(ResponseObject.Result.SUCCESS, "You rated the post", null, null, null);
                sendResponse(response, sock);
            }
            else {
                ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "You already voted this post", null, null, null);
                sendResponse(response, sock);
            }
        }
        else {
            ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "The post is not on your feed", null, null, null);
            sendResponse(response, sock);
        }
    }

    /*
    private void addComment(RequestObject request, SocketChannel sock);

    private void getWallet(RequestObject request, SocketChannel sock);

    private void getWalletInBitcoin(RequestObject request, SocketChannel sock);

    */

    private void invalidCommand(SocketChannel sock) {

        // writing the response to the client
        ResponseObject response = new ResponseObject(ResponseObject.Result.ERROR, "Invalid command", null, null, null);
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
