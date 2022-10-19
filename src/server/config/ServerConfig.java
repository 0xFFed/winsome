package server.config;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import common.config.Config;

public class ServerConfig extends Config {

    // ########## DATA ##########

    // default config file path
    protected static String serverConfigPath = "./src/server/config/serverConfig.json";

    // timeout for the select call
    private int selTimeout;

    // path to the storage folder
    private String storagePath;

    // number of threads per core
    private int coreMultiplier;


    // ########## METHODS ##########

    // constructor
    private ServerConfig() {}

    // returns a parsed config object
    public static ServerConfig getServerConfig() {

        Gson gson = new Gson();
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

    // getter
    public int getTimeout() {
        return this.selTimeout;
    }

    // getter
    public String getStoragePath() {
        return this.storagePath;
    }

    // getter
    public int getCoreMult() {
        return this.coreMultiplier;
    }
}