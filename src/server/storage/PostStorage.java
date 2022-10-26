package server.storage;

import java.io.IOException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import common.Post;
import common.User;

public class PostStorage extends Storage<Post> {
    
    public PostStorage(String storageDirPath, String relativeFilePath) throws IOException, IllegalArgumentException, NullPointerException {
        // calling parent constructor
        super(storageDirPath, relativeFilePath);

        // loading users data from storage file
        this.loadData();
    }


    private void loadData() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        
        try(FileReader reader = new FileReader(this.getStorageFilePath())) {
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.setLenient(true);
            Type storageType = new TypeToken<ConcurrentHashMap<String, Post>>(){}.getType();
            this.data = gson.fromJson(jsonReader, storageType);
        } catch(IOException e) {
            System.err.println("Error opening storage file (read-mode)");
            e.printStackTrace();
            System.exit(1);
        }

        if(this.data == null) this.data = new ConcurrentHashMap<>();
    }
}
