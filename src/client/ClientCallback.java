package client;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import common.rmi.ClientCallbackInterface;

public class ClientCallback extends RemoteObject implements ClientCallbackInterface {

    private static final long serialVersionUID = 8252L;

    // data structure holding followers
    private ArrayList<String> followers = new ArrayList<>();
    
    public ClientCallback(ArrayList<String> followers) throws RemoteException {
        super();
        this.followers = followers;
    }

    public void notifyFollow(String follower) throws RemoteException {
        this.followers.add(follower);
    }

    public void notifyUnfollow(String unfollower) throws RemoteException {
        this.followers.remove(unfollower);
    }
}
