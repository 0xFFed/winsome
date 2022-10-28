package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import common.Post;
import common.RewardTransaction;
import common.Transaction;
import server.config.ServerConfig;
import server.storage.ServerStorage;

public class ServerMulticastWorker implements Runnable {

    // multicast connection handling
    private MulticastSocket mcSocket;
    private InetAddress mcAddress;
    private int mcPort;

    // timer for the main cycle
    private int sleepTimer;

    // percentage of the reward reserved to the author of the post
    private double authorPercentage;

    // storage used to access Posts and Users data
    private ServerStorage serverStorage;

    // macro for 1 satoshi's value
    private static final double SATOSHI_UNIT = 0.00000001;

    
    public ServerMulticastWorker(ServerStorage serverStorage) {
        this.serverStorage = Objects.requireNonNull(serverStorage, "Server storage cannot be null");

        ServerConfig config = ServerConfig.getServerConfig();

        try {
            this.mcAddress = InetAddress.getByName(config.getMulticastAddress());
        } catch(UnknownHostException e) {
            System.err.println("Fatal failure: invalid multicast address");
        }

        this.mcPort = config.getMulticastPort();
        this.sleepTimer = config.getMulticastTimer();
        this.authorPercentage = config.getAuthorPercentage();

        try {
            this.mcSocket = new MulticastSocket();
        } catch(IOException e) {
            System.err.println("Fatal error: could not start multicast socket");
            System.exit(1);
        }
    }


    // thread main function
    public void run() {
        try {
            while(!(Thread.currentThread().isInterrupted())) {
                Thread.sleep(sleepTimer);

                // getting all posts to iterate through them
                ArrayList<Post> allPosts = this.serverStorage.getPostStorage().getPostSet();
                Iterator<Post> postIter = allPosts.iterator();

                // calculating rewards for each post
                while(postIter.hasNext()) {
                    Post currPost = postIter.next();
                    calculateReward(currPost);
                }

                // registering the rewards added into local storage
                this.serverStorage.getPostStorage().write();
                this.serverStorage.getUserStorage().write();

                // writing all reward messages on the multicast group
                sendRewardSummary();
            }
        } catch(InterruptedException e) {
            this.mcSocket.close();
            Thread.currentThread().interrupt();
        }
    }


    
    /** 
     * @param post
     * @throws NullPointerException
     */
    private void calculateReward(Post post) throws NullPointerException {
        Objects.requireNonNull(post);

        // calculating contribution from likes/dislikes
        HashSet<String> newLikers = post.getNewLikers();
        HashSet<String> newDislikers = post.getNewDislikers();
        int newLikeContribution = newLikers.size() - newDislikers.size();

        // calculating contribution (minus the author's) from comments
        double newCommentsContribution = 0;
        HashSet<String> newCommenters = post.getNewCommenters();
        newCommenters.remove(post.getAuthor());

        Iterator<String> commenterIter = newCommenters.iterator();
        while(commenterIter.hasNext()) {
            String user = commenterIter.next();
            int commentNumber = post.getCommentsOfUser(user).size();
            newCommentsContribution += (2/(1+(Math.exp(-(commentNumber-1)))));
        }

        // calculating final reward
        double firstMember = Math.log((double)(Math.max(newLikeContribution, 0))+1);
        double secondMember = Math.log(newCommentsContribution + 1);
        double reward = (firstMember + secondMember)/(post.getTimesRewarded()+1);

        // getting the final set of curators
        HashSet<String> curators = new HashSet<>();
        curators.addAll(newLikers);
        curators.addAll(newDislikers);
        curators.addAll(newCommenters);

        // calculating the correct rewards for author and curators
        double authorReward = reward*this.authorPercentage;
        double curatorsReward = 0;
        if(!(curators.isEmpty())) curatorsReward = (reward*(1-this.authorPercentage))/curators.size();

        // if the reward is more than one satoshi, register it, create transactions and produce a message
        if(authorReward+curatorsReward >= SATOSHI_UNIT) {

            // registering the author's reward
            this.serverStorage.getUserStorage().get(post.getAuthor()).sendTransaction(
                new RewardTransaction(authorReward, post.getPostId())
            );

            // registering the the curators' rewards
            Iterator<String> curatorsIter = curators.iterator();
            while(curatorsIter.hasNext()) {
                String curator = curatorsIter.next();
                this.serverStorage.getUserStorage().get(curator).sendTransaction(
                    new RewardTransaction(curatorsReward, post.getPostId())
                );
            }

            post.registerRewardCycle();
        }
    }


    // method used to send notifications of calculated rewards event to the multicast group
    public void sendRewardSummary() {
        byte[] outputbytes = ("Rewards calculated! Check your wallets!").getBytes();

        DatagramPacket outputMessage = new DatagramPacket(outputbytes, outputbytes.length, this.mcAddress, this.mcPort);
        try {
            this.mcSocket.send(outputMessage);
        } catch(IOException e) {
            System.err.println("Error sending message to multicast group");
        }
    }
}
