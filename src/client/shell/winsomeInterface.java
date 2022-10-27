package client.shell;

import java.rmi.RemoteException;

import common.request.ResponseObject;

public interface WinsomeInterface {
    
    public ResponseObject register(String username, String password, String[] tags) throws RemoteException;

    public ResponseObject login(String username, String password) throws RemoteException;

    public ResponseObject logout() throws RemoteException;

    public ResponseObject listUsers();

    public ResponseObject listFollowers();

    public ResponseObject listFollowing();

    public ResponseObject followUser(String userId);

    public ResponseObject unfollowUser(String userId);

    public ResponseObject viewBlog();

    public ResponseObject createPost(String title, String content);

    public ResponseObject showFeed();

    public ResponseObject showPost(String postId);

    public ResponseObject deletePost(String postId);

    public ResponseObject rewinPost(String postId);

    public ResponseObject ratePost(String postId, String vote);

    public ResponseObject addComment(String postId, String comment);

    public ResponseObject getWallet();

    public ResponseObject getWalletInBitcoin();
}
