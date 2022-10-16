package client;

import java.util.Iterator;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

public class ClientMain implements Runnable {

    // ########## DATA ##########

    // used for selection
    private Selector selector;

    // used to handle the connection
    private SocketChannel sock;

    // termination condition
    private boolean isStopping = false;

    // config object
    private final ClientConfig config = ClientConfig.getClientConfig();

    // ########## METHODS ##########

    private ClientMain() throws IOException {
        this.selector = this.createSelector();
    }

    // returns a new selector for the client
    private Selector createSelector() throws IOException {
        Selector sel = SelectorProvider.provider().openSelector();

        // creating a non-blocking SocketChannel
        this.sock = SocketChannel.open();
        this.sock.configureBlocking(false);

        // establishing connection
        System.out.println("Host: "+config.getAddr()+";\tPort: "+config.getPort());
        InetSocketAddress sockAddr = new InetSocketAddress(config.getAddr(), config.getPort());
        this.sock.connect(sockAddr);

        // registering the socket to detect when the connection is completely established
        this.sock.register(sel, SelectionKey.OP_CONNECT);

        System.out.println("Connection request sent...");

        return sel;
    }


    // completes the connection phase
    private void finishConnection(SelectionKey key) {
        // obtaining the channel from the key
        SocketChannel sockChannel = (SocketChannel) key.channel();

        // try and finish the connection
        try {
            sockChannel.finishConnect();
        } catch (IOException e) {
            // the connection failed: cancel the key and exit
            key.cancel();
            e.printStackTrace();
            System.exit(1);
        }
    }


    // main thread main loop
    public void run() {
        while(!(this.isStopping)) {
            try {
                // waiting for the accepting channel to become readable
                int timeLeft = this.selector.select(this.config.getTimeout());
                if(timeLeft == 0) continue; // re-try if timeout was reached

                // iterate over the set of ready keys
                Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();
                while(selectedKeys.hasNext()) {
                    // get the next key and remove it from the set
                    SelectionKey selKey = selectedKeys.next();
                    selectedKeys.remove();

                    // check key validity
                    if(!selKey.isValid()) continue;

                    // if a connection attempt was made, accept it
                    if(selKey.isConnectable()) this.finishConnection(selKey);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) {
        try {
            new Thread(new ClientMain()).start();
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
	}
}
