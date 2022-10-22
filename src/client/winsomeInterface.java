package client;

public interface WinsomeInterface {
    
    public void register(String username, String password, String[] tags);

    public void login(String username, String password);

    public void logout();

    public void listUsers();

    public void listFollowers();

    public void listFollowing();

    public void followUser(String userId);

    public void unfollowUser(String userId);

    public void viewBlog();

    public void createPost(String title, String content);

    public void showFeed();

    public void showPost(String postId);

    public void deletePost(String postId);

    public void rewinPost(String postId);

    public void ratePost(String postId);

    public void addComment(String postId, String comment);

    public void getWallet();

    public void getWalletInBitcoin();
}
