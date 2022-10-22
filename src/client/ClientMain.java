package client;

import java.util.Iterator;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import common.RemoteRegistrationInterface;
import common.config.Config;

public class ClientMain implements Runnable {

    // ########## DATA ##########

    // used to handle the connection
    private SocketChannel sock;

    // termination condition
    private boolean isStopping = false;


    // RMI object handle
    protected RemoteRegistrationInterface rmiRegistration;

    // token identifying the user to the server
    private String authToken;

    // config object
    private static final Config config = Config.getConfig();

    // ########## METHODS ##########

    private ClientMain() throws IOException, NotBoundException {
        this.initConnection();
        this.rmiRegistration = this.rmiConnect();
    }


    // initiates the connection setting up the Socket
    private void initConnection() throws IOException {

        // creating a non-blocking SocketChannel
        this.sock = SocketChannel.open();

        // establishing connection
        InetSocketAddress sockAddr = new InetSocketAddress(config.getAddr(), config.getPort());
        this.sock.connect(sockAddr);
    }


    // connects to the remote registration object
    private RemoteRegistrationInterface rmiConnect() throws RemoteException, NotBoundException {
        Remote stub;
        Registry reg = LocateRegistry.getRegistry(config.getRmiAddr(), config.getRmiPort());
        stub = reg.lookup(config.getRmiName());
        return (RemoteRegistrationInterface) stub;
    }


    // main thread main loop
    public void run() {
        try {   // TODO remove
            if(this.rmiRegistration.register("Dyxo", "omg", null)) System.out.println("Registration completed");
            else System.out.println("User already exists");
        } catch(RemoteException e) {
            System.err.println("Error using the RMI registration method");
            e.printStackTrace();
            System.exit(1);
        }
        
        while(!(this.isStopping)) {
            try {
                Thread.sleep(500);
            } catch(InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
                System.exit(1);
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
