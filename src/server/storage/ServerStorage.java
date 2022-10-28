package server.storage;

import java.util.Objects;

import common.User;
import common.Post;

public class ServerStorage {

    // user-storage and post-storage objects
    protected UserStorage userStorage;
    protected PostStorage postStorage;

    // this class abstracts a storage mechanism that manages users and posts
    public ServerStorage(UserStorage userStorage, PostStorage postStorage) {
        this.userStorage = Objects.requireNonNull(userStorage, "Server user storage cannot be null");
        this.postStorage = Objects.requireNonNull(postStorage, "Server post storage cannot be null");
    }


    
    /** 
     * @return UserStorage
     */
    public UserStorage getUserStorage() {
        return this.userStorage;
    }

    
    /** 
     * @return PostStorage
     */
    public PostStorage getPostStorage() {
        return this.postStorage;
    }
}
