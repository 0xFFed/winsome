package server.config;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import common.config.Config;

public class ServerConfig extends Config {

    // ########## DATA ##########

    // default config file path
    protected static String serverConfigPath = "./src/server/config/serverConfig.json";

    // timeout for the select call
    private int selTimeout;

    // path to the storage folder
    private String storagePath;

    // relative path to the users storage file
    private String userStoragePath;

    // relative path to the posts storage file
    private String postStoragePath;

    // relative path to the user counter file
    private String userCounterPath;

    // relative path to the post counter file
    private String postCounterPath;

    // address used in multicast communications
    private String multicastAddress;

    // port used in multicast communications
    private int multicastPort;

    // timer that intervals multicast communications
    private int multicastTimer;

    // number of threads per core
    private int threadsPerCore;

    // percentage of the reward given to the author
    private double authorPercentage;


    // ########## METHODS ##########

    // constructor
    private ServerConfig() {
        super();
    }

    
    /** 
     * @return ServerConfig
     */
    // returns a parsed config object
    public static ServerConfig getServerConfig() {

        Gson gson = new GsonBuilder().serializeNulls().create();
        String configJson = null;

        try {

            byte[] configBytes = Files.readAllBytes(Paths.get(configPath));
            byte[] serverConfigBytes = Files.readAllBytes(Paths.get(serverConfigPath));

            String configString = new String(configBytes);
            String serverConfigString = new String(serverConfigBytes);

            configString = configString.replace("}", ",");
            serverConfigString = serverConfigString.replace("{", "");

            configJson = configString+serverConfigString;

        } catch(IOException e) {
            System.err.println("Fatal error parsing server's config file");
            e.printStackTrace();
            System.exit(1);
        }

        return gson.fromJson(configJson, ServerConfig.class);
    }

    
    /** 
     * @return int
     */
    public int getTimeout() {
        return this.selTimeout;
    }

    
    /** 
     * @return String
     */
    public String getStoragePath() {
        return this.storagePath;
    }

    
    /** 
     * @return String
     */
    public String getUserStoragePath() {
        return this.userStoragePath;
    }

    
    /** 
     * @return String
     */
    public String getPostStoragePath() {
        return this.postStoragePath;
    }

    
    /** 
     * @return String
     */
    public String getUserCounterPath() {
        return this.userCounterPath;
    }

    
    /** 
     * @return String
     */
    public String getPostCounterPath() {
        return this.postCounterPath;
    }

    
    /** 
     * @return String
     */
    public String getMulticastAddress() {
        return this.multicastAddress;
    }

    
    /** 
     * @return int
     */
    public int getMulticastPort() {
        return this.multicastPort;
    }

    
    /** 
     * @return int
     */
    public int getMulticastTimer() {
        return this.multicastTimer;
    }

    
    /** 
     * @return int
     */
    public int getCoreMult() {
        return this.threadsPerCore;
    }

    
    /** 
     * @return double
     */
    public double getAuthorPercentage() {
        return this.authorPercentage;
    }
}