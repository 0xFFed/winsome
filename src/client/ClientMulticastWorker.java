package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ClientMulticastWorker implements Runnable {

    // multicast connection handling
    private MulticastSocket mcSocket;
    private InetAddress mcGroupAddress;
    private int mcGroupPort;
    
    public ClientMulticastWorker(InetAddress mcGroupAddress, int mcGroupPort) {
        this.mcGroupAddress = Objects.requireNonNull(mcGroupAddress, "the address of the multicast group cannot be null");
        this.mcGroupPort = mcGroupPort;

        try {
            this.mcSocket = new MulticastSocket(this.mcGroupPort);
        } catch(IOException e) {
            System.err.println("Fatal error: could not start multicast socket");
            System.exit(1);
        }

        try {
            this.mcSocket.joinGroup(this.mcGroupAddress);
        } catch(IOException e) {
            e.printStackTrace();
            System.err.println("Fatal error: Could not join multicast group");
            System.exit(1);
        }
    }



    public void run() {

        while(!(Thread.currentThread().isInterrupted())) {
            // allocating buffer to read messages
            byte[] buffer = new byte[1024];

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                this.mcSocket.receive(packet);
            } catch(IOException e) {
                System.err.println("Error reading from the multicast socket");
            }

            String message = new String(buffer, StandardCharsets.UTF_8);
            System.out.print(message+"\n\n> ");
        }
    }
}
