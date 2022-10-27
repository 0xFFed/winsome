package common.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerCallbackInterface extends Remote {
    
    public void registerForCallback(String token, ClientCallbackInterface clientInterface) throws RemoteException;

    public void unregisterForCallback(String token) throws RemoteException;
    
}
