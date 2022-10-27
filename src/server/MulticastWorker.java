package server;

import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Objects;

import server.config.ServerConfig;
import server.storage.ServerStorage;

public class MulticastWorker implements Runnable {

    // multicast connection handling
    private MulticastSocket mcSocket;
    private InetAddress mcAddress;
    private int mcPort;

    // timer for the main cyclo
    private int sleepTimer;

    // storage used to access Posts and Users data
    private ServerStorage serverStorage;

    
    public MulticastWorker(ServerStorage serverStorage) {
        this.serverStorage = Objects.requireNonNull(serverStorage, "Server storage cannot be null");

        ServerConfig config = ServerConfig.getServerConfig();

        this.mcAddress = config.getMulticastAddress();
        this.mcPort = config.getMulticastPort();
        this.sleepTimer = config.getMulticastTimer();

        this.mcSocket = new MulticastSocket(this.mcPort);
        this.mcSocket.joinGroup(this.mcAddress, null);
    }


    public void run() {
        // CODE
    }
}
