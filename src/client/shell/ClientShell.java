package client.shell;

import java.io.IOException;
import java.net.Authenticator.RequestorType;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.io.StringReader;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import common.User;
import common.request.RequestObject;
import common.request.ResponseObject;
import common.request.ResponseObject.Result;
import common.rmi.RemoteRegistrationInterface;

public class ClientShell implements WinsomeInterface {

    // connection socket
    SocketChannel sock;

    // rmi registration service handler
    private RemoteRegistrationInterface rmiRegistration;

    // token identifying the user to the server
    private String authToken;

    // keeps track of the connection status
    private boolean isLogged = false;

    // buffer to be used in NIO data exchanges
	private static final int BUF_DIM = 8192;
	private ByteBuffer readBuffer = ByteBuffer.allocate(BUF_DIM);


    // max number of arguments for the register call
    private static final int MAX_REG_ARGS = 5;

    public ClientShell(SocketChannel sock, RemoteRegistrationInterface rmiRegistration) {
        this.sock = Objects.requireNonNull(sock, "Socket cannot be null to communicate with server");
        this.rmiRegistration = Objects.requireNonNull(rmiRegistration, "RMI handler cannot be null");
        this.authToken = null;
    }


    // function used to parse given commands
    public ResponseObject parseCommand(String command, String[] args) throws RemoteException {
        Objects.requireNonNull(command, "command cannot be null");

        switch (command) {
            case "register":
                if(args == null || args.length < 2) return new ResponseObject(ResponseObject.Result.ERROR, "Username and Password are needed", null);
                String[] tags = new String[args.length-2];
                if(args.length > (2+MAX_REG_ARGS)) return new ResponseObject(ResponseObject.Result.ERROR, "You can only add up to 5 tags", null);
                if(args.length > 2) System.arraycopy(args, 2, tags, 0, (args.length-2));
                return this.register(args[0], args[1], tags);
            
            case "login":
                if(args == null || args.length < 2) return new ResponseObject(ResponseObject.Result.ERROR, "Username and Password are needed", null);
                if(args.length > 2) return new ResponseObject(ResponseObject.Result.ERROR, "Too many arguments", null);
                return this.login(args[0], args[1]);
                

            case "logout":
                return this.logout();
            default:
                break;
        }

        return new ResponseObject(ResponseObject.Result.ERROR, "Invalid command", null);
    }


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
        System.out.println("DEBUG: "+jsonResponse);    // TODO REMOVE

        // extracting a RequestObject from the JSON string and putting it in the task
        Gson gson = new GsonBuilder().serializeNulls().create();
        JsonReader reader = new JsonReader(new StringReader(jsonResponse));
        reader.setLenient(true);
        this.readBuffer.clear();
        
        return gson.fromJson(reader, ResponseObject.class);
    }


    // function used to send requests to the server
    private void sendRequest(RequestObject request) {
        // making a JSON string out of the request
        Gson gson = new GsonBuilder().serializeNulls().create();
        String json = gson.toJson(request);
        System.out.println("DEBUG: "+json);

        try {
            // sending the request through the socket
            this.sock.write(ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8)));
        } catch(IOException e) {
            System.err.println("The operation failed due to an I/O error. Quitting...");
            System.exit(1);
        }
    }


    public ResponseObject register(String username, String password, String[] tags) throws RemoteException {
        if(this.isLogged) return new ResponseObject(Result.ERROR, "You are already logged in.", null);
        return this.rmiRegistration.register(username, password, tags);
    }

    public ResponseObject login(String username, String password) {
        String command = "login";
        RequestObject request = new RequestObject(this.authToken, command, username, password, null, null, false);
        this.sendRequest(request);

        ResponseObject response = readResponse();
        if(response.isSuccess()) {
            this.isLogged = true;
            this.authToken = response.getStringData();
        }

        return response;
    }

    public ResponseObject logout() {
        // failing if the user is not logged
        if(!this.isLogged) return new ResponseObject(ResponseObject.Result.ERROR, "You are not logged in.", null);

        String command = "logout";
        RequestObject request = new RequestObject(this.authToken, command, null, null, null, null, false);
        this.sendRequest(request);

        ResponseObject response = readResponse();
        if(response.isSuccess()) {
            this.isLogged = false;
            this.authToken = null;
        }

        return response;
    }

    public ResponseObject listUsers() {
        return new ResponseObject(ResponseObject.Result.SUCCESS, "Success", null);
    }

    public ResponseObject listFollowers() {
        return new ResponseObject(ResponseObject.Result.SUCCESS, "Success", null);
    }

    public ResponseObject listFollowing() {
        return new ResponseObject(ResponseObject.Result.SUCCESS, "Success", null);
    }

    public ResponseObject followUser(String userId) {
        return new ResponseObject(ResponseObject.Result.SUCCESS, "Success", null);
    }

    public ResponseObject unfollowUser(String userId) {
        return new ResponseObject(ResponseObject.Result.SUCCESS, "Success", null);
    }

    public ResponseObject viewBlog() {
        return new ResponseObject(ResponseObject.Result.SUCCESS, "Success", null);
    }

    public ResponseObject createPost(String title, String content) {
        return new ResponseObject(ResponseObject.Result.SUCCESS, "Success", null);
    }

    public ResponseObject showFeed() {
        return new ResponseObject(ResponseObject.Result.SUCCESS, "Success", null);
    }

    public ResponseObject showPost(String postId) {
        return new ResponseObject(ResponseObject.Result.SUCCESS, "Success", null);
    }

    public ResponseObject deletePost(String postId) {
        return new ResponseObject(ResponseObject.Result.SUCCESS, "Success", null);
    }

    public ResponseObject rewinPost(String postId) {
        return new ResponseObject(ResponseObject.Result.SUCCESS, "Success", null);
    }

    public ResponseObject ratePost(String postId) {
        return new ResponseObject(ResponseObject.Result.SUCCESS, "Success", null);
    }

    public ResponseObject addComment(String postId, String comment) {
        return new ResponseObject(ResponseObject.Result.SUCCESS, "Success", null);
    }

    public ResponseObject getWallet() {
        return new ResponseObject(ResponseObject.Result.SUCCESS, "Success", null);
    }

    public ResponseObject getWalletInBitcoin() {
        return new ResponseObject(ResponseObject.Result.SUCCESS, "Success", null);
    }

}
