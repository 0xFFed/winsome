package server;

import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;

import com.google.gson.Gson;

public class ServerWorkerPool {

    // ########## VARIABLES ##########

    // worker threads threadpool and parameters
    private static final int CORE_MULT = 10;
    private static final int CPUS = Runtime.getRuntime().availableProcessors();
    private ThreadPoolExecutor workerPool;

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
    public ServerWorkerPool() {
        this.workerPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(CPUS*CORE_MULT);
    }


    // method used for request dispatching
    public void dispatchRequest(SelectionKey key, Pipe.SinkChannel pipe, byte[] array, int bytesRead) {
        byte[] data = new byte[bytesRead];  // allocating a new byte buffer
        System.arraycopy(array, 0, data, 0, bytesRead);     // copying the input array into a local array

        // creating a new parsedRequest object to pass to the relevant method
        ParsedRequest request = new ParsedRequest(key, pipe, data, bytesRead);

        return null;
    }


    // ########## REQUEST EXECUTION METHODS ##########


    // ########## OTHERS ##########


    // overriding of the call() method
    public String call() {
        // code
        return "";
    }

}
