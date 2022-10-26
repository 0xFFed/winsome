package server.storage;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import common.Post;
import common.User;
import server.config.ServerConfig;

public class Storage<T> {
    
    // ########## DATA ##########

    protected ConcurrentHashMap<String, T> data;
    protected String storageDirPath;
    protected String storageFilePath;


    // ########## METHODS ##########

    public Storage(String storageDirPath, String relativeFilePath) throws IOException, IllegalArgumentException, NullPointerException {
        // setting the storage file path
        this.storageDirPath = Objects.requireNonNull(storageDirPath, "null storage path: can't set storage");

        // creating storage directory if non-existing
        File storageDir = new File(this.storageDirPath);
        storageDir.mkdir();

        // setting the storage file path
        this.storageFilePath = storageDirPath+Objects.requireNonNull(relativeFilePath, "null storage file path: can't set storage");

        // creating storage file if non-existing
        File storageFile = new File(this.storageFilePath);
        storageFile.createNewFile();
    }


    public synchronized void write() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        try(FileWriter writer = new FileWriter(this.storageFilePath)) {
            String jsonText = gson.toJson(this.data);
            writer.write(jsonText);
        } catch(IOException e) {
            System.err.println("Error opening storage file (write-mode)");
            e.printStackTrace();
            System.exit(1);
        }
    }


    public boolean add(String key, T elem) throws NullPointerException {
        Objects.requireNonNull(key, "Username cannot be null");
        Objects.requireNonNull(elem, "User data cannot be null");

        boolean success;
        if(this.data.putIfAbsent(key, elem) == null) {
            success = true;
            this.write();
        }
        else success = false;

        return success;
    }


    public void remove(String key) {
        this.data.remove(key);
        this.write();
    }


    public T get(String key) {
        return this.data.get(key);
    }


    protected String getStorageDirPath() {
        return this.storageDirPath;
    }


    protected String getStorageFilePath() {
        return this.storageFilePath;
    }
}
