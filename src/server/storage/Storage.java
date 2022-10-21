package server.storage;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import common.Post;
import common.User;
import server.config.ServerConfig;

public class Storage<T> {
    
    // ########## DATA ##########

    // used to determine the type of storage
    public enum StorageType {
        USERS,
        POSTS
    }
    private StorageType type;

    private HashMap<String, T> data;
    private String storagePath;
    private ReentrantReadWriteLock lock;

    private static String userPath = "users.json";
    private static String postPath = "posts.json";


    // ########## METHODS ##########

    public Storage(String storagePath, StorageType storageType) throws IllegalArgumentException {
        switch(storageType) {
            case USERS:
                this.type = StorageType.USERS;
                this.storagePath = storagePath+userPath;
                break;
            case POSTS:
                this.type = StorageType.POSTS;
                this.storagePath = storagePath+postPath;
                break;
            default:
                throw new IllegalArgumentException("Invalid storage type");
        }

        this.lock = new ReentrantReadWriteLock();
        this.data = new HashMap<>();
        this.read();
    }


    public synchronized void write() {
        this.lock.writeLock().lock();
        Gson gson = new Gson();

        try {
            gson.toJson(this.data, new FileWriter(this.storagePath));
        } catch(IOException e) {
            System.err.println("Error saving JSON file");
            e.printStackTrace();
            System.exit(1);
        }

        this.lock.writeLock().unlock();
    }


    private void read() {
        this.lock.writeLock().lock();
        if(!data.isEmpty()) this.write();
        data.clear();

        Gson gson = new Gson();
        
        try {
            if(this.type==StorageType.USERS)
                this.data = gson.fromJson(new JsonReader(new FileReader(this.storagePath)), User.class);
            else
                this.data = gson.fromJson(new JsonReader(new FileReader(this.storagePath)), Post.class);
        } catch(IOException e) {
            File storageFile = new File(this.storagePath);
            try {
                if(!storageFile.createNewFile()) {
                    System.err.println("Error opening storage file");
                    e.printStackTrace();
                    System.exit(1);
                }
            } catch(IOException ex) {
                System.err.println("Error creating storage file");
                ex.printStackTrace();
                System.exit(1);
            }
        }

        this.lock.writeLock().unlock();
    }


    public void add(String key, T elem) {
        this.lock.writeLock().lock();
        this.data.putIfAbsent(key, elem);
        this.write();
        this.lock.writeLock().unlock();
    }


    public void remove(String key) {
        this.lock.writeLock().lock();
        this.data.remove(key);
        this.write();
        this.lock.writeLock().unlock();
    }
}
