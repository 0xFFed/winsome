package client;

import java.util.Iterator;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

import common.config.Config;

public class ClientMain implements Runnable {

    // ########## DATA ##########

    // used for selection
    private Selector selector;

    // used to handle the connection
    private SocketChannel sock;

    // token identifying the user to the server
    private String authToken;

    // termination condition
    private boolean isStopping = false;

    // config object
    private final Config config = Config.getConfig();

    // ########## METHODS ##########

    private ClientMain() throws IOException {
        this.initConnection();
    }

    // initiates the connection setting up the Selector and Socket
    private void initConnection() throws IOException {
        this.selector = SelectorProvider.provider().openSelector();

        // creating a non-blocking SocketChannel
        this.sock = SocketChannel.open();
        this.sock.configureBlocking(false);

        // establishing connection
        InetSocketAddress sockAddr = new InetSocketAddress(config.getAddr(), config.getPort());
        this.sock.connect(sockAddr);

        // registering the socket to detect when the connection is completely established
        this.sock.register(this.selector, SelectionKey.OP_CONNECT);
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
                int timeLeft = this.selector.select(500);
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
            } catch (IOException e) {
                System.err.println("Error in Select()");
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
