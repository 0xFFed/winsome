package server.rmi;

import java.rmi.RemoteException;

import common.RemoteRegistrationInterface;

public class RemoteRegistration implements RemoteRegistrationInterface {

    public RemoteRegistration() throws RemoteException {
        System.out.println("Remote object created");
    }
    
    public void register(String username, String password, String[] tags) throws RemoteException {
        System.out.println("register() called");
    }
}
