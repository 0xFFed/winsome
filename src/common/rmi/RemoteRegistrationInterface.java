package common.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import common.request.ResultObject;

public interface RemoteRegistrationInterface extends Remote {
    
    public ResultObject register(String username, String password, String[] tags) throws RemoteException;
}
