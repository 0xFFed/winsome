package server;

import java.nio.channels.SelectionKey;
import java.util.Objects;
import java.nio.channels.SocketChannel;

import common.request.RequestObject;

public class ServerTask {
    private SocketChannel sock;
    private RequestObject request;
    
    public ServerTask(SocketChannel sock, RequestObject request) throws NullPointerException {
        this.sock = Objects.requireNonNull(sock, "the client's socket cannot be null");
        this.request = Objects.requireNonNull(request, "the client's request object cannot be null");
    }

    public SocketChannel getSock() {
        return this.sock;
    }

    public RequestObject getRequest() {
        return this.request;
    }
}
