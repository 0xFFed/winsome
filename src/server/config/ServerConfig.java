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


    // ########## METHODS ##########

    // constructor
    private ServerConfig() {}

    // returns a parsed config object
    public static ServerConfig getServerConfig() throws IOException {
        Gson gson = new Gson();

        byte[] configBytes = Files.readAllBytes(Paths.get(configPath));
        byte[] serverConfigBytes = Files.readAllBytes(Paths.get(serverConfigPath));

        String configString = new String(configBytes);
        String serverConfigString = new String(serverConfigBytes);

        configString = configString.replace("}", ",");
        serverConfigString = serverConfigString.replace("{", "");
        
        return gson.fromJson(configString+serverConfigString, ServerConfig.class);
    }

    // getter
    public int getTimeout() {
        return this.selTimeout;
    }
}