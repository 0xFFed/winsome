package client.shell;

import java.net.Authenticator.RequestorType;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.util.Objects;

import common.request.ResultObject;
import common.rmi.RemoteRegistrationInterface;

public class ClientShell implements WinsomeInterface {

    // connection socket
    SocketChannel sock;

    // rmi registration service handler
    protected RemoteRegistrationInterface rmiRegistration;

    // token identifying the user to the server
    protected String authToken;

    // keeps track of the connection status
    protected boolean isConnected = false;


    // max number of arguments for the register call
    private static final int MAX_REG_ARGS = 5;

    public ClientShell(SocketChannel sock, RemoteRegistrationInterface rmiRegistration) {
        this.sock = Objects.requireNonNull(sock, "Socket cannot be null to communicate with server");
        this.rmiRegistration = Objects.requireNonNull(rmiRegistration, "RMI handler cannot be null");
    }


    // function used to parse given commands
    public ResultObject parseCommand(String command, String[] args) throws RemoteException {
        Objects.requireNonNull(command, "command cannot be null");

        switch (command) {
            case "register":
                if(args == null || args.length < 2) return new ResultObject(ResultObject.Result.ERROR, "Username and Password are needed");
                String[] tags = new String[args.length-2];
                if(args.length > (2+MAX_REG_ARGS)) return new ResultObject(ResultObject.Result.ERROR, "You can only add up to 5 tags");
                if(args.length > 2) System.arraycopy(args, 2, tags, 0, (args.length-2));
                return this.register(args[0], args[1], tags);
            
            case "login":
                if(args == null || args.length < 2) return new ResultObject(ResultObject.Result.ERROR, "Username and Password are needed");
                if(args.length > 2) return new ResultObject(ResultObject.Result.ERROR, "Too many arguments");
                return this.login(args[0], args[1]);
                

            case "logout":
                return this.logout();
            default:
                break;
        }

        return new ResultObject(ResultObject.Result.ERROR, "Invalid command");
    }


    // TODO IMPLEMENT
    public ResultObject register(String username, String password, String[] tags) throws RemoteException {
        return this.rmiRegistration.register(username, password, tags);
    }

    public ResultObject login(String username, String password) {
        this.isConnected = true;

        return new ResultObject(ResultObject.Result.SUCCESS, "Success");
    }

    public ResultObject logout() {
        this.isConnected = false;
        return new ResultObject(ResultObject.Result.SUCCESS, "Success");
    }

    public ResultObject listUsers() {
        return new ResultObject(ResultObject.Result.SUCCESS, "Success");
    }

    public ResultObject listFollowers() {
        return new ResultObject(ResultObject.Result.SUCCESS, "Success");
    }

    public ResultObject listFollowing() {
        return new ResultObject(ResultObject.Result.SUCCESS, "Success");
    }

    public ResultObject followUser(String userId) {
        return new ResultObject(ResultObject.Result.SUCCESS, "Success");
    }

    public ResultObject unfollowUser(String userId) {
        return new ResultObject(ResultObject.Result.SUCCESS, "Success");
    }

    public ResultObject viewBlog() {
        return new ResultObject(ResultObject.Result.SUCCESS, "Success");
    }

    public ResultObject createPost(String title, String content) {
        return new ResultObject(ResultObject.Result.SUCCESS, "Success");
    }

    public ResultObject showFeed() {
        return new ResultObject(ResultObject.Result.SUCCESS, "Success");
    }

    public ResultObject showPost(String postId) {
        return new ResultObject(ResultObject.Result.SUCCESS, "Success");
    }

    public ResultObject deletePost(String postId) {
        return new ResultObject(ResultObject.Result.SUCCESS, "Success");
    }

    public ResultObject rewinPost(String postId) {
        return new ResultObject(ResultObject.Result.SUCCESS, "Success");
    }

    public ResultObject ratePost(String postId) {
        return new ResultObject(ResultObject.Result.SUCCESS, "Success");
    }

    public ResultObject addComment(String postId, String comment) {
        return new ResultObject(ResultObject.Result.SUCCESS, "Success");
    }

    public ResultObject getWallet() {
        return new ResultObject(ResultObject.Result.SUCCESS, "Success");
    }

    public ResultObject getWalletInBitcoin() {
        return new ResultObject(ResultObject.Result.SUCCESS, "Success");
    }

}
