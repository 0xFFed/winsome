package common.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteRegistrationInterface extends Remote {
    
    public boolean register(String username, String password, String[] tags) throws RemoteException;
}
