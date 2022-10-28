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
    private ConcurrentLinkedQueue<Transaction> transactionHistory = new ConcurrentLinkedQueue<>();
    private int balance;


    // ########## METHODS ##########

    // constructor
    public User(String username, String password, ConcurrentLinkedQueue<String> tags) throws NullPointerException {
        Objects.requireNonNull(username, "username cannot be null");
        Objects.requireNonNull(password, "password cannot be null");

        this.userId = counter.get();
        this.username = username;
        this.password = password;
        this.tags = tags;
        this.balance = 0;
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
    public ArrayList<Transaction> getTransactionHistory() {
        return new ArrayList<>(this.transactionHistory);
    }

    // getter
    public int getBalance() {
        return this.balance;
    }

    // getter
    public static int incrementCounter() {
        return counter.incrementAndGet();
    }

    // setter
    public static synchronized void setCounter(int value) {
        counter.set(value);
    }


    // ########## UTILITY FUNCTIONS ##########

    // adds a follower to the user; returns true if it wasn't already a follower
    public synchronized boolean addFollower(User follower) {
        if(this.followers.contains(follower.getUsername())) return false;
        else {
            follower.getFollowings().add(this.getUsername());
            return this.followers.add(follower.getUsername());
        }
    }

    // removes a follower from the user; returns true if it was already a follower
    public synchronized boolean removeFollower(User unfollower) {
        unfollower.getFollowings().remove(this.getUsername());
        return this.followers.remove(unfollower.getUsername());
    }
}
