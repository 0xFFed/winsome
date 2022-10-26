package server.storage;

import java.util.Objects;

import common.User;
import common.Post;

public class ServerStorage {

    // user-storage and post-storage objects
    protected UserStorage userStorage;
    protected PostStorage postStorage;

    public ServerStorage(UserStorage userStorage, PostStorage postStorage) {
        this.userStorage = Objects.requireNonNull(userStorage, "Server user storage cannot be null");
        this.postStorage = Objects.requireNonNull(postStorage, "Server post storage cannot be null");
    }


    public UserStorage getUserStorage() {
        return this.userStorage;
    }

    public PostStorage getPostStorage() {
        return this.postStorage;
    }
}
