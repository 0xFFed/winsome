package common.config;

import java.net.InetAddress;
import java.io.FileNotFoundException;
import java.io.FileReader;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.GsonBuilder;

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
    private String callbackAddr;
    private int callbackPort;
    private String callbackName;


    // ########## METHODS ##########

    protected Config() {}

    
    /** 
     * @return Config
     */
    // returns a parsed config object
    public static Config getConfig() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        Config result = null;

        try {
            result = gson.fromJson(new JsonReader(new FileReader(configPath)), Config.class);
        } catch(FileNotFoundException e) {
            System.err.println("Config file not found");
            System.exit(1);
        }
        
        return result;
    }

    
    /** 
     * @return InetAddress
     */
    public InetAddress getAddr() {
        return this.serverAddr;
    }

    
    /** 
     * @return int
     */
    public int getPort() {
        return this.serverPort;
    }

    
    /** 
     * @return String
     */
    public String getRmiAddr() {
        return this.rmiAddr;
    }

    
    /** 
     * @return int
     */
    public int getRmiPort() {
        return this.rmiPort;
    }

    
    /** 
     * @return String
     */
    public String getRmiName() {
        return this.rmiName;
    }

    
    /** 
     * @return String
     */
    public String getCallbackAddr() {
        return this.callbackAddr;
    }

    
    /** 
     * @return int
     */
    public int getCallbackPort() {
        return this.callbackPort;
    }

    
    /** 
     * @return String
     */
    public String getCallbackName() {
        return this.callbackName;
    }
}
