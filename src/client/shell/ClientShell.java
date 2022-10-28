package client.shell;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.Authenticator.RequestorType;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import client.ClientMulticastWorker;
import common.Comment;
import common.Post;
import common.User;
import common.request.RequestObject;
import common.request.ResponseObject;
import common.request.ResponseObject.Result;
import common.rmi.ClientCallbackInterface;
import common.rmi.RemoteRegistrationInterface;
import common.rmi.ServerCallbackInterface;

public class ClientShell implements WinsomeInterface {

    // connection socket
    SocketChannel sock;

    // rmi registration service handler
    private RemoteRegistrationInterface rmiRegistration;

    // callback handlers
    private ServerCallbackInterface callbackHandle;
    private ClientCallbackInterface callbackStub;

    // token identifying the user to the server
    private String authToken;

    // data structure holding the current logged user's followers
    private ArrayList<String> followers;

    // keeps track of the connection status
    private boolean isLogged = false;

    // buffer to be used in NIO data exchanges
	private static final int BUF_DIM = 8192;
	private ByteBuffer readBuffer = ByteBuffer.allocate(BUF_DIM);

    // handle for the multicast worker thread
    private Thread mcWorkerThread;


    // max number of arguments for the register call
    private static final int MAX_REG_ARGS = 5;

    // macros for recurrent error messages
    private static final String TOO_MANY = "Too many arguments";
    private static final String INVALID = "Invalid command";
    private static final String INCOMPLETE = "Incomplete command";

    public ClientShell(SocketChannel sock, RemoteRegistrationInterface rmiRegistration, ServerCallbackInterface callbackHandle,
        ClientCallbackInterface callbackStub, ArrayList<String> followers) {

        this.sock = Objects.requireNonNull(sock, "Socket cannot be null to communicate with server");
        this.rmiRegistration = Objects.requireNonNull(rmiRegistration, "RMI handler cannot be null");
        this.callbackHandle = Objects.requireNonNull(callbackHandle, "Follow/Unfollow handle cannot be null");
        this.callbackStub = Objects.requireNonNull(callbackStub, "Stub for to notification service cannot be null");
        this.followers = Objects.requireNonNull(followers, "Followers data structure cannot be null");
        this.authToken = null;
    }


    
    /** 
     * @param command
     * @param args
     * @return ResponseObject
     * @throws RemoteException
     */
    // function used to parse given commands
    public ResponseObject parseCommand(String command, String[] args) throws RemoteException {
        Objects.requireNonNull(command, "command cannot be null");

        switch (command) {
            case "register":
                if(args == null || args.length < 2) return new ResponseObject(ResponseObject.Result.ERROR, "Username and Password are needed", null, null, null);
                String[] tags = new String[args.length-2];
                if(args.length > (2+MAX_REG_ARGS)) return new ResponseObject(ResponseObject.Result.ERROR, "You can only add up to 5 tags", null, null, null);
                if(args.length > 2) System.arraycopy(args, 2, tags, 0, (args.length-2));
                return this.register(args[0], args[1], tags);
            
            case "login":
                if(args == null || args.length < 2) return new ResponseObject(ResponseObject.Result.ERROR, "Username and Password are needed", null, null, null);
                if(args.length > 2) return new ResponseObject(ResponseObject.Result.ERROR, TOO_MANY, null, null, null);
                return this.login(args[0], args[1]);
                

            case "logout":
                return this.logout();

            case "list":
                if(args == null || args.length == 0) return new ResponseObject(ResponseObject.Result.ERROR, INCOMPLETE, null, null, null);
                if(args.length > 1) return new ResponseObject(ResponseObject.Result.ERROR, INVALID, null, null, null);
                if(args[0].equals("users")) return this.listUsers();
                if(args[0].equals("followers")) return this.listFollowers();
                if(args[0].equals("following")) return this.listFollowing();
                break;

            case "follow":
                if(args == null || args.length == 0) return new ResponseObject(ResponseObject.Result.ERROR, "Username of the user to follow is needed", null, null, null);
                if(args.length > 1) return new ResponseObject(ResponseObject.Result.ERROR, TOO_MANY, null, null, null);
                return this.followUser(args[0]);

            case "unfollow":
                if(args == null || args.length == 0) return new ResponseObject(ResponseObject.Result.ERROR, "Username of the user to unfollow is needed", null, null, null);
                if(args.length > 1) return new ResponseObject(ResponseObject.Result.ERROR, TOO_MANY, null, null, null);
                return this.unfollowUser(args[0]);

            case "blog":
                return this.viewBlog();

            case "post":
                if(args == null || args.length == 0) return new ResponseObject(ResponseObject.Result.ERROR, "You have to specify a title for the post", null, null, null);
                if(args.length == 1) return new ResponseObject(ResponseObject.Result.ERROR, "You need to write content for the post (between apices)", null, null, null);
                StringTokenizer tokenizer = new StringTokenizer(String.join(" ", Arrays.asList(args)), "-");
                if(tokenizer.countTokens() != 2) return new ResponseObject(ResponseObject.Result.ERROR, "Correct format is: Title - Content", null, null, null);
                String title = tokenizer.nextToken();
                String content = tokenizer.nextToken();
                return this.createPost(title, content);

            case "show":
                if(args == null || args.length == 0) return new ResponseObject(ResponseObject.Result.ERROR, INCOMPLETE, null, null, null);
                if(args[0].equals("feed")) {
                    if(args.length > 1) return new ResponseObject(ResponseObject.Result.ERROR, INVALID, null, null, null);
                    return this.showFeed();
                }
                else if(args[0].equals("post")) {
                    if(args.length < 2) return new ResponseObject(ResponseObject.Result.ERROR, "You have to specify a post ID", null, null, null);
                    if(args.length > 2) return new ResponseObject(ResponseObject.Result.ERROR, TOO_MANY, null, null, null);
                    return this.showPost(args[1]);
                }
                else break;

            case "delete":
                if(args == null || args.length == 0) return new ResponseObject(ResponseObject.Result.ERROR, "You have to specify the ID of the post to remove", null, null, null);
                if(args.length > 1) return new ResponseObject(ResponseObject.Result.ERROR, TOO_MANY, null, null, null);
                return this.deletePost(args[0]);

            case "rewin":
                if(args == null || args.length == 0) return new ResponseObject(ResponseObject.Result.ERROR, "You have to specify the ID of the post to rewin", null, null, null);
                if(args.length > 1) return new ResponseObject(ResponseObject.Result.ERROR, TOO_MANY, null, null, null);
                return this.rewinPost(args[0]);

            case "rate":
                if(args == null || args.length == 0) return new ResponseObject(ResponseObject.Result.ERROR, "You have to specify a post ID", null, null, null);
                if(args.length == 1) return new ResponseObject(ResponseObject.Result.ERROR, "You have to specify your vote (+1/-1)", null, null, null);
                if(args.length > 2) return new ResponseObject(ResponseObject.Result.ERROR, TOO_MANY, null, null, null);
                return this.ratePost(args[0], args[1]);

            case "comment":
                if(args == null || args.length == 0) return new ResponseObject(ResponseObject.Result.ERROR, "You have to specify the ID of the post to comment", null, null, null);
                if(args.length == 1) return new ResponseObject(ResponseObject.Result.ERROR, "You have to write a comment", null, null, null);
                String[] comment = new String[args.length-1];
                System.arraycopy(args, 1, comment, 0, (args.length-1));
                String commentText = String.join(" ", Arrays.asList(comment));
                return addComment(args[0], commentText);

            case "wallet":
                if(args == null || args.length < 1) {
                    return this.getWallet();
                }
                else {
                    if(args.length > 1) return new ResponseObject(ResponseObject.Result.ERROR, TOO_MANY, null, null, null);
                    if(args[0].equals("btc")) return this.getWalletInBitcoin();
                    else return new ResponseObject(ResponseObject.Result.ERROR, INVALID, null, null, null);
                }

            default:
                break;
        }

        return new ResponseObject(ResponseObject.Result.ERROR, INVALID, null, null, null);
    }


    
    /** 
     * @return ResponseObject
     */
    // function used to read server response messages
    private ResponseObject readResponse() {

        this.readBuffer.clear();

        // reading from the socket channel
        int bytesRead = 0;
        try {
            bytesRead = this.sock.read(this.readBuffer);
        } catch(IOException e) {
            // connection lost
            System.out.println("The server dropped the connection. Quitting...");
            System.exit(1);
        }

        if(bytesRead < 0) {
            // connection lost
            System.out.println("Error while reading the server's response. Quitting...");
            System.exit(1);
        }

        byte[] data = this.readBuffer.array();

        // extracting the JSON string representing the response from the server
        String jsonResponse = new String(data, StandardCharsets.UTF_8);

        // extracting a RequestObject from the JSON string and putting it in the task
        Gson gson = new GsonBuilder().serializeNulls().create();
        JsonReader reader = new JsonReader(new StringReader(jsonResponse));
        reader.setLenient(true);
        this.readBuffer.clear();
        
        return gson.fromJson(reader, ResponseObject.class);
    }


    
    /** 
     * @param request
     */
    // function used to send requests to the server
    private void sendRequest(RequestObject request) {
        // making a JSON string out of the request
        Gson gson = new GsonBuilder().serializeNulls().create();
        String json = gson.toJson(request);

        try {
            // sending the request through the socket
            this.sock.write(ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8)));
        } catch(IOException e) {
            System.err.println("The operation failed due to an I/O error. Quitting...");
            System.exit(1);
        }
    }


    
    /** 
     * @param username
     * @param password
     * @param tags
     * @return ResponseObject
     * @throws RemoteException
     */
    public ResponseObject register(String username, String password, String[] tags) throws RemoteException {
        if(this.isLogged) return new ResponseObject(Result.ERROR, "You are already logged in.", null, null, null);
        ConcurrentLinkedQueue<String> tagsList = new ConcurrentLinkedQueue<>(Arrays.asList(tags));
        return this.rmiRegistration.register(username, password, tagsList);
    }

    
    /** 
     * @param username
     * @param password
     * @return ResponseObject
     * @throws RemoteException
     */
    public ResponseObject login(String username, String password) throws RemoteException {
        String command = "login";
        RequestObject request = new RequestObject(this.authToken, command, username, password, null, null, null);
        this.sendRequest(request);

        ResponseObject response = readResponse();
        if(response.isSuccess()) {
            this.followers.addAll(response.getStringArray());
            this.isLogged = true;
            this.authToken = response.getStringData();
            this.callbackHandle.registerForCallback(this.authToken, this.callbackStub);
            try {
                this.mcWorkerThread = new Thread(new ClientMulticastWorker(InetAddress.getByName(response.getSecondStringArray().get(0)),
                    Integer.parseInt(response.getSecondStringArray().get(1))));
                this.mcWorkerThread.start();
            } catch(UnknownHostException e) {
                System.err.println("Fatal failure: multicast address is invalid");
                System.exit(1);
            }
        }

        return response;
    }

    
    /** 
     * @return ResponseObject
     * @throws RemoteException
     */
    public ResponseObject logout() throws RemoteException {

        String command = "logout";
        RequestObject request = new RequestObject(this.authToken, command, null, null, null, null, null);
        this.sendRequest(request);

        ResponseObject response = readResponse();
        if(response.isSuccess()) {
            this.followers.removeAll(followers);
            this.callbackHandle.unregisterForCallback(this.authToken);
            if(Objects.nonNull(this.mcWorkerThread)) this.mcWorkerThread.interrupt();
            this.isLogged = false;
            this.authToken = null;
        }

        return response;
    }

    
    /** 
     * @return ResponseObject
     */
    public ResponseObject listUsers() {

        String command = "list users";
        RequestObject request = new RequestObject(this.authToken, command, null, null, null, null, null);
        this.sendRequest(request);

        return readResponse();
    }

    
    /** 
     * @return ResponseObject
     */
    public ResponseObject listFollowers() {
        if(!(this.isLogged)) return new ResponseObject(ResponseObject.Result.ERROR, "You are not logged in", null, null, null);

        String result = "People following you: "+String.join(", ", this.followers);

        return new ResponseObject(ResponseObject.Result.SUCCESS, result, null, null, null);
    }

    
    /** 
     * @return ResponseObject
     */
    public ResponseObject listFollowing() {

        String command ="list following";
        RequestObject request = new RequestObject(this.authToken, command, null, null, null, null, null);
        this.sendRequest(request);

        return readResponse();
    }

    
    /** 
     * @param userId
     * @return ResponseObject
     */
    public ResponseObject followUser(String userId) {

        String command = "follow";
        RequestObject request = new RequestObject(this.authToken, command, userId, null, null, null, null);
        sendRequest(request);

        return readResponse();
    }

    
    /** 
     * @param userId
     * @return ResponseObject
     */
    public ResponseObject unfollowUser(String userId) {

        String command = "unfollow";
        RequestObject request = new RequestObject(this.authToken, command, userId, null, null, null, null);
        sendRequest(request);

        return readResponse();
    }

    
    /** 
     * @return ResponseObject
     */
    public ResponseObject viewBlog() {

        String command = "blog";
        RequestObject request = new RequestObject(this.authToken, command, null, null, null, null, null);
        sendRequest(request);

        return readResponse();
    }

    
    /** 
     * @param title
     * @param content
     * @return ResponseObject
     */
    public ResponseObject createPost(String title, String content) {

        String command = "post";
        RequestObject request = new RequestObject(this.authToken, command, null, null, new Post(title, content, "", null, false), null, null);
        sendRequest(request);

        return readResponse();
    }

    
    /** 
     * @return ResponseObject
     */
    public ResponseObject showFeed() {

        String command = "show feed";
        RequestObject request = new RequestObject(this.authToken, command, null, null, null, null, null);
        sendRequest(request);

        return readResponse();
    }

    
    /** 
     * @param postId
     * @return ResponseObject
     */
    public ResponseObject showPost(String postId) {

        String command = "show post";
        ArrayList<String> data = new ArrayList<>();
        data.add(postId);
        RequestObject request = new RequestObject(this.authToken, command, null, null, null, null, data);
        sendRequest(request);

        return readResponse();
    }

    
    /** 
     * @param postId
     * @return ResponseObject
     */
    public ResponseObject deletePost(String postId) {

        String command = "delete";
        ArrayList<String> data = new ArrayList<>();
        data.add(postId);
        RequestObject request = new RequestObject(this.authToken, command, null, null, null, null, data);
        sendRequest(request);

        return readResponse();
    }

    
    /** 
     * @param postId
     * @return ResponseObject
     */
    public ResponseObject rewinPost(String postId) {

        String command = "rewin";
        ArrayList<String> data = new ArrayList<>();
        data.add(postId);
        RequestObject request = new RequestObject(this.authToken, command, null, null, null, null, data);
        sendRequest(request);

        return readResponse();
    }

    
    /** 
     * @param postId
     * @param vote
     * @return ResponseObject
     */
    public ResponseObject ratePost(String postId, String vote) {

        String command = "rate";
        ArrayList<String> data = new ArrayList<>();
        data.add(postId); data.add(vote);
        RequestObject request = new RequestObject(this.authToken, command, null, null, null, null, data);
        sendRequest(request);

        return readResponse();
    }

    
    /** 
     * @param postId
     * @param comment
     * @return ResponseObject
     */
    public ResponseObject addComment(String postId, String comment) {

        String command = "comment";
        ArrayList<String> data = new ArrayList<>();
        data.add(postId);
        RequestObject request = new RequestObject(this.authToken, command, null, null, null, new Comment(null, comment), data);
        sendRequest(request);

        return readResponse();
    }

    
    /** 
     * @return ResponseObject
     */
    public ResponseObject getWallet() {

        String command = "wallet";
        RequestObject request = new RequestObject(this.authToken, command, null, null, null, null, null);
        sendRequest(request);

        return readResponse();
    }

    
    /** 
     * @return ResponseObject
     */
    public ResponseObject getWalletInBitcoin() {

        String command = "wallet btc";
        RequestObject request = new RequestObject(this.authToken, command, null, null, null, null, null);
        sendRequest(request);

        return readResponse();
    }

}
