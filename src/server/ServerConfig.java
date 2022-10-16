package server;

import java.net.InetAddress;
import java.io.FileNotFoundException;
import java.io.FileReader;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

class ServerConfig {

    // ########## DATA ##########

    // default config file path
    private static String configPath = "./src/server/config.json";

    // IP-port for listening
    private InetAddress hostAddr;
    private int port;

    // timeout for the select call
    private int selTimeout;


    // ########## METHODS ##########

    // constructor
    private ServerConfig() {}

    // returns a parsed config object
    public static ServerConfig getServerConfig() throws FileNotFoundException {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(configPath));
        
        return gson.fromJson(reader, ServerConfig.class);
    }

    // getter
    public InetAddress getAddr() {
        return this.hostAddr;
    }

    // getter
    public int getPort() {
        return this.port;
    }

    // getter
    public int getTimeout() {
        return this.selTimeout;
    }
}