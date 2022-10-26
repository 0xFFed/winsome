package common.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

import common.request.ResponseObject;

public interface RemoteRegistrationInterface extends Remote {
    
    public ResponseObject register(String username, String password, ArrayList<String> tags) throws RemoteException;
}
