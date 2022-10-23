package server;

import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.rmi.RemoteException;

import common.User;
import common.Post;
import server.config.ServerConfig;
import server.rmi.ServerCallback;
import server.storage.Storage;
import server.ServerMain;

class ServerWorkerPool {

    // ########## VARIABLES ##########

    // worker threads threadpool and parameters
    private static final int CPUS = Runtime.getRuntime().availableProcessors();
    private ThreadPoolExecutor workerPool;


    // user-storage and post-storage objects
    protected Storage<User> userStorage;
    protected Storage<Post> postStorage;

    // handle for follow/unfollow callbacks
    protected ServerCallback callbackHandle;

    private class ParsedRequest {

        SelectionKey key;
        Pipe.SinkChannel pipe;
        byte[] array;
        int bytesRead;
        
        // constructor
        public ParsedRequest(SelectionKey key, Pipe.SinkChannel pipe, byte[] array, int bytesRead) {
            this.key = key;
            this.pipe = pipe;
            this.array = array;
            this.bytesRead = bytesRead;
        }
    }


    // ########## METHODS #########

    // constructor
    public ServerWorkerPool(Storage<User> userStorage, Storage<Post> postStorage, ServerCallback callbackHandle) {
        this.userStorage = Objects.requireNonNull(userStorage, "User storage cannot be null");
        this.postStorage = Objects.requireNonNull(postStorage, "Post storage cannot be null");
        this.callbackHandle = Objects.requireNonNull(callbackHandle, "callback object cannot be null");
        this.workerPool = (ThreadPoolExecutor)Executors.newFixedThreadPool(CPUS*ServerMain.config.getCoreMult());
    }


    // method used for request dispatching
    public void dispatchRequest(SelectionKey key, Pipe.SinkChannel pipe, byte[] array, int bytesRead) {
        byte[] data = new byte[bytesRead];  // allocating a new byte buffer
        System.arraycopy(array, 0, data, 0, bytesRead);     // copying the input array into a local array

        // creating a new parsedRequest object to pass to the relevant method
        ParsedRequest request = new ParsedRequest(key, pipe, data, bytesRead);
    }


    // ########## REQUEST EXECUTION METHODS ##########

}
