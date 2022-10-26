package common;

import java.util.Objects;

public class User {

    // ########## DATA ##########

    private static AtomicInteger counter = new AtomicInteger();
    private int userId;
    private String username;
    private String password;
    private String[] tags;
    private String[] followers;
    private String[] followings;
    private int balance;


    // ########## METHODS ##########

    // constructor
    public User(String username, String password, String[] tags) throws NullPointerException {
        Objects.requireNonNull(username, "username cannot be null");
        Objects.requireNonNull(password, "password cannot be null");
        
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
    public String[] getTags() {
        return this.tags;
    }

    // getter
    public String[] getFollowers() {
        return this.followers;
    }

    // getter
    public String[] getFollowings() {
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
}
