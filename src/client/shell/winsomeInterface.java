package client.shell;

import java.rmi.RemoteException;

import common.request.ResultObject;

public interface WinsomeInterface {
    
    public ResultObject register(String username, String password, String[] tags) throws RemoteException;

    public ResultObject login(String username, String password);

    public ResultObject logout();

    public ResultObject listUsers();

    public ResultObject listFollowers();

    public ResultObject listFollowing();

    public ResultObject followUser(String userId);

    public ResultObject unfollowUser(String userId);

    public ResultObject viewBlog();

    public ResultObject createPost(String title, String content);

    public ResultObject showFeed();

    public ResultObject showPost(String postId);

    public ResultObject deletePost(String postId);

    public ResultObject rewinPost(String postId);

    public ResultObject ratePost(String postId);

    public ResultObject addComment(String postId, String comment);

    public ResultObject getWallet();

    public ResultObject getWalletInBitcoin();
}
