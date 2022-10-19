package server.storage;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import server.config.ServerConfig;

public class Storage<T> {
    
    // ########## DATA ##########

    private ConcurrentHashMap<String, T> data;
    private String storagePath;

    // used to determine the type of storage
    public enum StorageType {
        USERS,
        POSTS
    }


    // ########## METHODS ##########

    public Storage(String storagePath, StorageType storageType) throws IllegalArgumentException {
        switch(storageType) {
            case USERS:
                this.storagePath = storagePath+"users.json";
                break;
            case POSTS:
                this.storagePath = storagePath+"posts.json";
                break;
            default:
                throw new IllegalArgumentException("Invalid storage type");
        }

        this.data = new ConcurrentHashMap<>();
        this.loadInternal();
    }

    public synchronized void store() {
        // code
    }

    private void loadInternal() {
        if(!data.isEmpty()) store();
    }
}
