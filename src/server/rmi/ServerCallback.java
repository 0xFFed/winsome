package server.rmi;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

import common.rmi.ClientCallbackInterface;
import common.rmi.ServerCallbackInterface;

public class ServerCallback extends RemoteObject implements ServerCallbackInterface {

    private static final long serialVersionUID = 4640L;

    private HashMap<String,ClientCallbackInterface> users = new HashMap<>();

    public ServerCallback() throws RemoteException {
        // no-op
    }

    
    /** 
     * @param token
     * @param clientInterface
     * @throws RemoteException
     */
    public synchronized void registerForCallback(String token, ClientCallbackInterface clientInterface) throws RemoteException {
        this.users.put(token, clientInterface);
    }

    
    /** 
     * @param token
     * @throws RemoteException
     */
    public synchronized void unregisterForCallback(String token) throws RemoteException {
        this.users.remove(token);
    }

    
    /** 
     * @param token
     * @param follower
     * @throws RemoteException
     */
    public void notifyFollow(String token, String follower) throws RemoteException {
        if(Objects.isNull(token) || Objects.isNull(follower)) return;
        ClientCallbackInterface clientInterface = this.users.get(token);
        if(Objects.nonNull(clientInterface)) clientInterface.notifyFollow(follower);
    }

    
    /** 
     * @param token
     * @param unfollower
     * @throws RemoteException
     */
    public void notifyUnfollow(String token, String unfollower) throws RemoteException {
        if(Objects.isNull(token) || Objects.isNull(unfollower)) return;
        ClientCallbackInterface clientInterface = this.users.get(token);
        if(Objects.nonNull(clientInterface)) clientInterface.notifyUnfollow(unfollower);
    }
}
