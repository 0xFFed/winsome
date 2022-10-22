package server.rmi;

import java.rmi.RemoteException;

import common.RemoteRegistrationInterface;
import common.User;
import common.Post;
import server.storage.Storage;

public class RemoteRegistration implements RemoteRegistrationInterface {

    // user-storage and post-storage objects
    protected Storage<User> userStorage;
    protected Storage<Post> postStorage;


    public RemoteRegistration(Storage<User> userStorage, Storage<Post> postStorage) throws RemoteException {
        this.userStorage = userStorage;
        this.postStorage = postStorage;
    }
    
    public boolean register(String username, String password, String[] tags) throws RemoteException {
        return this.userStorage.add(username, new User(username, password, tags));
    }
}
