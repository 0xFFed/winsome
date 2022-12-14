package server.rmi;

import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import common.User;
import common.crypto.Cryptography;
import common.request.ResponseObject;
import common.rmi.RemoteRegistrationInterface;
import common.Post;
import server.storage.ServerStorage;
import server.storage.Storage;

public class RemoteRegistration implements RemoteRegistrationInterface {

    // server storage object
    protected ServerStorage serverStorage;


    public RemoteRegistration(ServerStorage serverStorage) throws RemoteException {
        this.serverStorage = Objects.requireNonNull(serverStorage, "Server storage cannot be null");
    }
    
    
    /** 
     * @param username
     * @param password
     * @param tags
     * @return ResponseObject
     * @throws RemoteException
     * @throws NullPointerException
     */
    public ResponseObject register(String username, String password, ConcurrentLinkedQueue<String> tags) throws RemoteException, NullPointerException {
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);

        boolean success = false;

        try {
            success = this.serverStorage.getUserStorage().add(username, new User(username, Cryptography.digest(password), tags));
        } catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(1);
        }

        if(success) return new ResponseObject(ResponseObject.Result.SUCCESS, "Registration successful", null, null, null);
        else return new ResponseObject(ResponseObject.Result.ERROR, "User already exists", null, null, null);
    }
}
