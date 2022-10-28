package server;

import java.nio.channels.SelectionKey;
import java.util.Objects;
import java.nio.channels.SocketChannel;

import common.request.RequestObject;

public class ServerTask {

    // client's socket and request object
    private SocketChannel sock;
    private RequestObject request;

    // the class abstract a task that a server's worker has to execute
    public ServerTask(SocketChannel sock, RequestObject request) throws NullPointerException {
        this.sock = Objects.requireNonNull(sock, "the client's socket cannot be null");
        this.request = Objects.requireNonNull(request, "the client's request object cannot be null");
    }

    
    /** 
     * @return SocketChannel
     */
    public SocketChannel getSock() {
        return this.sock;
    }

    
    /** 
     * @return RequestObject
     */
    public RequestObject getRequest() {
        return this.request;
    }
}
