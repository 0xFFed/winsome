package client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
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
import java.rmi.server.UnicastRemoteObject;

import client.shell.ClientShell;
import common.config.Config;
import common.rmi.ClientCallbackInterface;
import common.rmi.RemoteRegistrationInterface;
import common.rmi.ServerCallbackInterface;


public class ClientMain implements Runnable {

    // ########## DATA ##########

    // used to handle the connection
    protected SocketChannel sock;

    // shell interface used to issue commands to communicate with clients
    private ClientShell shell;

    // RMI object handle
    protected RemoteRegistrationInterface rmiRegistration;

    // Callback handles
    protected ServerCallbackInterface callbackHandle;
    protected ClientCallbackInterface callbackStub;

    // config object
    private static final Config config = Config.getConfig();


    // list containing the user's followers, updated via RMI callback
    protected ArrayList<String> followers = new ArrayList<>();

    // ########## METHODS ##########

    private ClientMain() throws IOException, NotBoundException {
        this.initConnection();
        this.rmiRegistration = this.rmiConnect();
        this.setupCallbackService();
        this.shell = new ClientShell(this.sock, this.rmiRegistration, this.callbackHandle, this.callbackStub, this.followers);
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


    // registers for the follow/unfollow notification service
    private void setupCallbackService() throws RemoteException,NotBoundException {
        Registry reg = LocateRegistry.getRegistry(ClientMain.config.getCallbackPort());
        this.callbackHandle = (ServerCallbackInterface) reg.lookup(ClientMain.config.getCallbackName());
        ClientCallbackInterface callbackObject = new ClientCallback(this.followers);
        this.callbackStub = (ClientCallbackInterface) UnicastRemoteObject.exportObject(callbackObject, 0);

        // adding a cleanup-handler
        Thread cleanupCallback = new Thread(() -> {
            try {
                System.err.println("\nQuitting...\n");
                UnicastRemoteObject.unexportObject(callbackObject, true);
                this.shell.parseCommand("logout", null);
                this.sock.close();
            } catch(IOException | NoSuchElementException e) {
                e.printStackTrace();
                System.exit(1);
            }
        });

        // registering the cleanup-handler
        Runtime.getRuntime().addShutdownHook(cleanupCallback);

        
    }

    // prints the given message formatted according to the winsome shell visualization
    public void shellPrint(String message) {
        System.out.println("< "+message);
    }


    // main thread main loop
    public void run() {
        try(Scanner scanner = new Scanner(System.in)) {
            System.out.println("\nWelcome to WINSOME, a reWardINg SOcial MEdia!");
            String command = "";
            String[] args = null;

            while(true) {
                // getting the command string from input
                System.out.print("\n> ");
                String commandString = scanner.nextLine();

                // getting command and tokens from command string
                StringTokenizer st = new StringTokenizer(commandString);
                if(!(st.hasMoreTokens())) continue;
                else command = st.nextToken();

                // checking secondary termination condition
                if(command.equals("exit")) break;

                // getting args array from tokens
                if(st.countTokens() > 0) {
                    int counter = 0;
                    args = new String[st.countTokens()];
                    while(st.hasMoreTokens()) args[counter++] = st.nextToken();
                }

                shellPrint(this.shell.parseCommand(command, args).getOutput());
            }

        } catch(RemoteException e) {
            System.err.println("WARNING: Server not reachable");
            e.printStackTrace();
        } catch(NoSuchElementException e) {
            // ignored
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
