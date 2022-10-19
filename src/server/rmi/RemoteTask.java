package server.rmi;

import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import server.ServerMain;
import server.rmi.RemoteRegistration;
import common.RemoteRegistrationInterface;

public class RemoteTask implements Runnable {

    private RemoteRegistration remoteRegistration;
    private RemoteRegistrationInterface stub;
    private Registry reg;
    
    public RemoteTask() throws RemoteException {
        // generating and exposing the remote object
        this.remoteRegistration = new RemoteRegistration();
        this.stub = (RemoteRegistrationInterface) 
            UnicastRemoteObject.exportObject(this.remoteRegistration, ServerMain.config.getRmiPort());
        
        // creating and retrieving the RMI registry
        LocateRegistry.createRegistry(ServerMain.config.getRmiPort());
        this.reg = LocateRegistry.getRegistry(ServerMain.config.getRmiAddr(), ServerMain.config.getRmiPort());

        // binding the registration stub to its symbolic name
        this.reg.rebind(ServerMain.config.getRmiName(), this.stub);
    }

    public void run() {

        try {   // keeping the thread alive until shutdown, then deallocating resources
            while(!(ServerMain.isShuttingDown())) Thread.sleep(ServerMain.config.getTimeout());
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            try {
                this.reg.unbind(ServerMain.config.getRmiName());
                UnicastRemoteObject.unexportObject(this.remoteRegistration, true);
            } catch(RemoteException | NotBoundException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}
