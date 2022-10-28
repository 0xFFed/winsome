package common.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

import common.request.ResponseObject;

public interface RemoteRegistrationInterface extends Remote {
    
    public ResponseObject register(String username, String password, ConcurrentLinkedQueue<String> tags) throws RemoteException;
}
