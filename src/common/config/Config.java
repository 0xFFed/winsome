package common.config;

import java.net.InetAddress;
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

    // IP-port for RMI calls
    private String rmiAddr;
    private int rmiPort;
    private String rmiName;


    // ########## METHODS ##########

    // constructor
    protected Config() {}

    // returns a parsed config object
    public static Config getConfig() {
        Gson gson = new Gson();
        Config result = null;

        try {
            result = gson.fromJson(new JsonReader(new FileReader(configPath)), Config.class);
        } catch(FileNotFoundException e) {
            System.err.println("Config file not found");
            System.exit(1);
        }
        
        return result;
    }

    // getter
    public InetAddress getAddr() {
        return this.serverAddr;
    }

    // getter
    public int getPort() {
        return this.serverPort;
    }

    // getter
    public String getRmiAddr() {
        return this.rmiAddr;
    }

    // getter
    public int getRmiPort() {
        return this.rmiPort;
    }

    // getter
    public String getRmiName() {
        return this.rmiName;
    }
}
