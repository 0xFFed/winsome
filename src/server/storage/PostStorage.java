package server.storage;

import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import common.Post;
import common.config.IdCounter;
import server.config.ServerConfig;

public class PostStorage extends Storage<Post> {

    private static final ServerConfig config = ServerConfig.getServerConfig();
    private String counterFilePath;
    
    public PostStorage(String storageDirPath, String relativeFilePath) throws IOException, IllegalArgumentException, NullPointerException {
        // calling parent constructor
        super(storageDirPath, relativeFilePath);

        // setting counter file path
        this.counterFilePath = config.getStoragePath()+config.getPostCounterPath();

        // loading posts data from storage file
        this.loadData();

        // initializing the Post global counter
        this.initCounter();
    }


    // used to initialize the Post global counter
    private void initCounter() {

        // creating counter file if non-existing
        try {
            File counterFile = new File(this.counterFilePath);
            counterFile.createNewFile();
            System.out.println("DEBUG: "+counterFile);  // TODO REMOVE
        } catch(IOException e) {
            System.err.println("Critical failure: storage corrupted");
            System.exit(1);
        }


        Gson gson = new GsonBuilder().serializeNulls().create();

        try(FileReader reader = new FileReader(this.counterFilePath)) {
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.setLenient(true);
            IdCounter counter = gson.fromJson(jsonReader, IdCounter.class);

            if(Objects.nonNull(counter)) Post.setCounter(counter.getCounter());
            else Post.setCounter(0);

        } catch(IOException e) {
            System.err.println("Error opening counter file (read-mode)");
            e.printStackTrace();
            System.exit(1);
        }
    }


    private void loadData() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        
        try(FileReader reader = new FileReader(this.storageFilePath)) {
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


    // setter
    private void updateCounterFile() {
        

        Gson gson = new GsonBuilder().serializeNulls().create();
        try(FileWriter writer = new FileWriter(this.counterFilePath)) {
            IdCounter counter = new IdCounter();
            counter.setCounter(Post.incrementCounter());
            String jsonText = gson.toJson(counter);
            writer.write(jsonText);
        } catch(IOException e) {
            System.err.println("Error opening counter file (write-mode)");
            e.printStackTrace();
            System.exit(1);
        }
    }


    @Override
    public synchronized boolean add(String key, Post elem) throws NullPointerException {
        System.out.println("New user created.");
        boolean result = super.add(key, elem);
        if(result) {
            this.updateCounterFile();
        }
        return result;
    }
}
