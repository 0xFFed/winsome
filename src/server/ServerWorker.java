package server;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;

public class ServerWorker implements Callable<String> {

    // ########## VARIABLES ##########

    // socket channel corresponding to the client-server communication
    private SocketChannel sockChannel;

    // buffer used for communication
    private ByteBuffer buffer;

    // number of bytes read in the initial message
    private final int bytesRead;


    // ########## METHODS #########

    // constructor
    public ServerWorker(SocketChannel sockChannel, ByteBuffer buffer, int bytesRead) {
        this.sockChannel = sockChannel;
        this.buffer = buffer;
        this.bytesRead = bytesRead;
    }


    // overriding of the call() method
    public String call() {
        // code
        return "";
    }

}
