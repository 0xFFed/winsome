package common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class User {

    // ########## DATA ##########

    private static int userId;
    private String username;
    private String password;
    private String[] tags;
    private ArrayList<String> followers;
    private ArrayList<String> followings;
    private int balance;


    // ########## METHODS ##########

    // constructor
    public User(String username, String password, String[] tags) throws NullPointerException {
        Objects.requireNonNull(username, "username cannot be null");
        Objects.requireNonNull(password, "password cannot be null");
        
        this.username = username;
        this.password = password;
        this.tags = tags;
        this.followers = new ArrayList<>();
        this.followings = new ArrayList<>();
        this.balance = 0;
    }


    // checks if the password given is equal to the user's password
    public boolean checkPassword(String password) {
        return (this.password.equals(password));
    }

    // getter
    public String getUsername() {
        return this.username;
    }

    // getter
    public String[] getTags() {
        return this.tags;
    }

    // getter
    public List<String> getFollowers() {
        return this.followers;
    }

    // getter
    public List<String> getFollowings() {
        return this.followings;
    }

    // getter
    public int getBalance() {
        return this.balance;
    }
}
