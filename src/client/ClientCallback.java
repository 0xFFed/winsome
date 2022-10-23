package client;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.UUID;

import common.rmi.ClientCallbackInterface;

public class ClientCallback extends RemoteObject implements ClientCallbackInterface {

    private static final long serialVersionUID = 8252L;
    
    public ClientCallback() throws RemoteException {
        super();
    }

    public void notifyFollow(String message) throws RemoteException {
        System.out.println(message);
    }
}
