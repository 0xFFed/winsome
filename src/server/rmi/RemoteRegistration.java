package server.rmi;

import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

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
    
    public ResponseObject register(String username, String password, String[] tags) throws RemoteException {
        boolean success = false;

        try {
            success = this.serverStorage.getUserStorage().add(username, new User(username, Cryptography.digest(password), tags));
        } catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(1);
        }

        if(success) return new ResponseObject(ResponseObject.Result.SUCCESS, "Registration successful", null);
        else return new ResponseObject(ResponseObject.Result.ERROR, "User already exists", null);
    }
}
