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


    
    /** 
     * @param password
     * @return boolean
     */
    public boolean checkPassword(String password) {
        Objects.requireNonNull(password);

        return (this.password.equals(password));
    }

    
    /** 
     * @return int
     */
    public int getUserId() {
        return this.userId;
    }

    
    /** 
     * @return String
     */
    public String getUsername() {
        return this.username;
    }

    
    /** 
     * @return ArrayList<String>
     */
    public ArrayList<String> getTags() {
        return new ArrayList<>(this.tags);
    }

    
    /** 
     * @return ArrayList<String>
     */
    public ArrayList<String> getFollowers() {
        return new ArrayList<>(this.followers);
    }

    
    /** 
     * @return ArrayList<String>
     */
    public ArrayList<String> getFollowings() {
        return new ArrayList<>(this.followings);
    }

    
    /** 
     * @return ArrayList<RewardTransaction>
     */
    public ArrayList<RewardTransaction> getTransactionHistory() {
        return new ArrayList<>(this.transactionHistory);
    }

    
    /** 
     * @return double
     */
    public double getBalance() {
        return this.balance;
    }

    
    /** 
     * @return int
     */
    public static int incrementCounter() {
        return counter.incrementAndGet();
    }

    
    /** 
     * @param value
     */
    public static void setCounter(int value) {
        counter.set(value);
    }


    
    // ########## UTILITY FUNCTIONS ##########

    /** 
     * @param follower
     * @return boolean
     * @throws NullPointerException
     */
    // adds a follower to the user; returns true if it wasn't already a follower
    public boolean addFollower(User follower) throws NullPointerException {
        Objects.requireNonNull(follower);

        if(this.followers.contains(follower.getUsername())) return false;
        else {
            follower.addFollowing(this);
            return this.followers.add(follower.getUsername());
        }
    }

    
    /** 
     * @param unfollower
     * @return boolean
     * @throws NullPointerException
     */
    // removes a follower from the user; returns true if it was already a follower
    public boolean removeFollower(User unfollower) throws NullPointerException {
        Objects.requireNonNull(unfollower);

        unfollower.removeFollowing(this);
        return this.followers.remove(unfollower.getUsername());
    }

    
    /** 
     * @param following
     * @throws NullPointerException
     */
    // adds a following to the user
    public void addFollowing(User following) throws NullPointerException {
        Objects.requireNonNull(following);

        this.followings.add(following.getUsername());
    }

    
    /** 
     * @param following
     * @throws NullPointerException
     */
    // removes a following from the user
    public void removeFollowing(User following) throws NullPointerException {
        Objects.requireNonNull(following);

        this.followings.remove(following.getUsername());
    }

    
    /** 
     * @param tx
     * @throws NullPointerException
     */
    // registers the funds in the user's wallet
    public void sendTransaction(RewardTransaction tx) throws NullPointerException {
        Objects.requireNonNull(tx);
        // ignoring null transactions
        if(Objects.isNull(tx)) return;

        this.balance += tx.getValue();
        this.transactionHistory.add(tx);
    }
}
