package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteRegistrationInteface extends Remote {
    
    public void register(String username, String password, String[] tags) throws RemoteException;
}
