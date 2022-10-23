package server.rmi;

import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import common.User;
import common.crypto.Cryptography;
import common.request.ResultObject;
import common.rmi.RemoteRegistrationInterface;
import common.Post;
import server.storage.Storage;

public class RemoteRegistration implements RemoteRegistrationInterface {

    // user-storage and post-storage objects
    protected Storage<User> userStorage;
    protected Storage<Post> postStorage;


    public RemoteRegistration(Storage<User> userStorage, Storage<Post> postStorage) throws RemoteException {
        this.userStorage = Objects.requireNonNull(userStorage, "User storage cannot be null");
        this.postStorage = Objects.requireNonNull(postStorage, "Post storage cannot be null");
    }
    
    public ResultObject register(String username, String password, String[] tags) throws RemoteException {
        boolean success = false;

        try {
            success = this.userStorage.add(username, new User(username, Cryptography.digest(password), tags));
        } catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(1);
        }

        if(success) return new ResultObject(ResultObject.Result.SUCCESS, "Registration successful");
        else return new ResultObject(ResultObject.Result.ERROR, "User already exists");
    }
}
