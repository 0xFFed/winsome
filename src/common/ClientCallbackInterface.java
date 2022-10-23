package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientCallbackInterface extends Remote {
    
    public void notifyFollow(String message) throws RemoteException;
}
