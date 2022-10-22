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
import java.util.concurrent.ConcurrentSkipListMap;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import common.Post;
import common.User;
import server.config.ServerConfig;

public class Storage<T> {
    
    // ########## DATA ##########

    private ConcurrentHashMap<String, T> data;
    private String storageDirPath;
    private String storageFilePath;


    // ########## METHODS ##########

    public Storage(String storageDirPath, String relativeFilePath) throws IOException, IllegalArgumentException, NullPointerException {
        Objects.requireNonNull(storageDirPath, "null storage path: can't set storage");
        Objects.requireNonNull(relativeFilePath, "null storage file path: can't set storage");

        // setting the storage file path
        this.storageDirPath = storageDirPath;

        // creating storage directory if non-existing
        File storageDir = new File(this.storageDirPath);
        storageDir.mkdir();

        // setting the storage file path
        this.storageFilePath = storageDirPath+relativeFilePath;

        // creating storage file if non-existing
        File storageFile = new File(this.storageFilePath);
        storageFile.createNewFile();

        // loading users data from storage file
        this.loadData();

        System.out.println(this.data.mappingCount());
        this.data.forEach((key, value) -> System.out.println(key));
    }


    public void write() {
        Gson gson = new Gson();
        try(FileWriter writer = new FileWriter(this.storageFilePath)) {
            String jsonText = gson.toJson(this.data);
            writer.write(jsonText);
        } catch(IOException e) {
            System.err.println("Error opening storage file (write-mode)");
            e.printStackTrace();
            System.exit(1);
        }
    }


    private void loadData() {
        Gson gson = new Gson();
        
        try(FileReader reader = new FileReader(this.storageFilePath)) {
            this.data = gson.fromJson(new JsonReader(reader), ConcurrentHashMap.class);
        } catch(IOException e) {
            System.err.println("Error opening storage file (read-mode)");
            e.printStackTrace();
            System.exit(1);
        }

        if(this.data == null) this.data = new ConcurrentHashMap<>();
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
}
