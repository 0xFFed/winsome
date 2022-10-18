package common;

public class User {

    // ########## DATA ##########

    private static int userId;
    private String username;
    private String password;
    private String[] tags;
    private String[] followers;
    private String[] followings;


    // ########## METHODS ##########

    // constructor
    public User(String username, String password, String[] tags) {
        this.username = username;
        this.password = password;
        this.tags = tags;
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
}
