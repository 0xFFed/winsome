package common;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class User {

    // ########## DATA ##########

    private static AtomicInteger counter = new AtomicInteger();
    private int userId;
    private String username;
    private String password;
    private ArrayList<String> tags = new ArrayList<>();
    private ArrayList<String> followers = new ArrayList<>();
    private ArrayList<String> followings = new ArrayList<>();
    private int balance;


    // ########## METHODS ##########

    // constructor
    public User(String username, String password, ArrayList<String> tags) throws NullPointerException {
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
        return this.tags;
    }

    // getter
    public ArrayList<String> getFollowers() {
        return this.followers;
    }

    // getter
    public ArrayList<String> getFollowings() {
        return this.followings;
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
    public static void setCounter(int value) {
        counter.set(value);
    }


    // ########## UTILITY FUNCTIONS ##########

    // adds a follower to the user; returns true if it wasn't already a follower
    public boolean addFollower(User follower) {
        if(this.followers.contains(follower.getUsername())) return false;
        else {
            follower.getFollowings().add(this.getUsername());
            return this.followers.add(follower.getUsername());
        }
    }

    // removes a follower from the user; returns true if it was already a follower
    public boolean removeFollower(User unfollower) {
        unfollower.getFollowings().remove(this.getUsername());
        return this.followers.remove(unfollower.getUsername());
    }
}
