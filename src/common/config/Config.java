package common.config;

import java.net.InetAddress;
import java.nio.file.Files;
import java.io.FileNotFoundException;
import java.io.FileReader;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class Config {
    
    // ########## DATA ##########

    // default config file path
    protected static String configPath = "./src/common/config/config.json";

    // IP-port for listening
    private InetAddress serverAddr;
    private int serverPort;


    // ########## METHODS ##########

    // constructor
    protected Config() {}

    // returns a parsed config object
    public static Config getConfig() throws FileNotFoundException {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(configPath));
        
        return gson.fromJson(reader, Config.class);
    }

    // getter
    public InetAddress getAddr() {
        return this.serverAddr;
    }

    // getter
    public int getPort() {
        return this.serverPort;
    }
}
