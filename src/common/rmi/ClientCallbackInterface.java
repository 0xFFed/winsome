package common.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientCallbackInterface extends Remote {
    
    public void notifyFollow(String follower) throws RemoteException;

    public void notifyUnfollow(String unfollower) throws RemoteException;
}
