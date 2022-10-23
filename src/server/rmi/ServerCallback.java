package server.rmi;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.ArrayList;
import java.util.Iterator;

import common.ClientCallbackInterface;
import common.ServerCallbackInterface;

public class ServerCallback extends RemoteObject implements ServerCallbackInterface {

    private ArrayList<ClientCallbackInterface> users;

    public ServerCallback() throws RemoteException {
        users = new ArrayList<>();
    }

    public synchronized void registerForCallback(ClientCallbackInterface clientInterface) throws RemoteException {
        if(!users.contains(clientInterface)) users.add(clientInterface);
    }

    public synchronized void unregisterForCallback(ClientCallbackInterface clientInterface) throws RemoteException {
        users.remove(clientInterface);
    }

    public void notifyUsers() throws RemoteException {
        Iterator<ClientCallbackInterface> iter = users.iterator();
        while(iter.hasNext()) {
            ClientCallbackInterface client = iter.next();
            client.notifyFollow("You have been notified!");
            System.out.println("Clients notified");
        }
    }
}
