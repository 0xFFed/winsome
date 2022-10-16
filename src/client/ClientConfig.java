package client;

import java.net.InetAddress;
import java.io.FileNotFoundException;
import java.io.FileReader;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class ClientConfig {

    // ########## DATA ##########

    // default config file path
    private static String configPath = "./src/client/config.json";

    // IP-port for connecting to the server
    private InetAddress serverAddr;
    private int serverPort;

    // timeout for the select call
    private int selTimeout;


    // ########## METHODS ##########

    // constructor
    private ClientConfig() {}

    // returns a parsed config object
    public static ClientConfig getClientConfig() throws FileNotFoundException {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(configPath));

        return gson.fromJson(reader, ClientConfig.class);
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
    public int getTimeout() {
        return this.selTimeout;
    }
}