package client;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;

import common.rmi.ClientCallbackInterface;

public class ClientCallback extends RemoteObject implements ClientCallbackInterface {
    
    public ClientCallback() throws RemoteException {
        super();
    }

    public void notifyFollow(String message) throws RemoteException {
        System.out.println(message);
    }
}
