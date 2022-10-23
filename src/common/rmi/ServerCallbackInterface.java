package common.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerCallbackInterface extends Remote {
    
    public void registerForCallback(ClientCallbackInterface clientInterface) throws RemoteException;

    public void unregisterForCallback(ClientCallbackInterface clientInterface) throws RemoteException;
    
}
