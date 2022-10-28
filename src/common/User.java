package common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class User {

    // ########## DATA ##########

    private static AtomicInteger counter = new AtomicInteger();
    private int userId;
    private String username;
    private String password;
    private ConcurrentLinkedQueue<String> tags = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<String> followers = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<String> followings = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<RewardTransaction> transactionHistory = new ConcurrentLinkedQueue<>();
    private double balance;


    // ########## METHODS ##########

    // constructor
    public User(String username, String password, ConcurrentLinkedQueue<String> tags) throws NullPointerException {
        Objects.requireNonNull(username, "username cannot be null");
        Objects.requireNonNull(password, "password cannot be null");

        this.userId = counter.get();
        this.username = username;
        this.password = password;
        this.tags = tags;
        this.balance = 0.0;
    }


    // checks if the password given is equal to the user's password
    public boolean checkPassword(String password) {
        return (this.password.equals(password));
    }

    public int getUserId() {
        return this.userId;
    }

    // getter
    public String getUsername() {
        return this.username;
    }

    // getter
    public ArrayList<String> getTags() {
        return new ArrayList<>(this.tags);
    }

    // getter
    public ArrayList<String> getFollowers() {
        return new ArrayList<>(this.followers);
    }

    // getter
    public ArrayList<String> getFollowings() {
        return new ArrayList<>(this.followings);
    }

    // getter
    public ArrayList<RewardTransaction> getTransactionHistory() {
        return new ArrayList<>(this.transactionHistory);
    }

    // getter
    public double getBalance() {
        return this.balance;
    }

    // getter
    public static int incrementCounter() {
        return counter.incrementAndGet();
    }

    // setter
    public static void setCounter(int value) {
        counter.set(value);
    }


    // ########## UTILITY FUNCTIONS ##########

    // adds a follower to the user; returns true if it wasn't already a follower
    public boolean addFollower(User follower) {
        if(this.followers.contains(follower.getUsername())) return false;
        else {
            follower.addFollowing(this);
            return this.followers.add(follower.getUsername());
        }
    }

    // removes a follower from the user; returns true if it was already a follower
    public boolean removeFollower(User unfollower) {
        unfollower.removeFollowing(this);
        return this.followers.remove(unfollower.getUsername());
    }

    // adds a following to the user
    public void addFollowing(User following) {
        this.followings.add(following.getUsername());
    }

    // removes a following from the user
    public void removeFollowing(User following) {
        this.followings.remove(following.getUsername());
    }

    // registers the funds in the user's wallet
    public void sendTransaction(RewardTransaction tx) {
        // ignoring null transactions
        if(Objects.isNull(tx)) return;

        this.balance += tx.getValue();
        this.transactionHistory.add(tx);
    }
}
