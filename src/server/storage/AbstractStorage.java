package server.storage;

import java.util.ArrayList;

import server.config.ServerConfig;

public abstract class AbstractStorage<T> implements Storable {
    
    // ########## DATA ##########

    private ArrayList<T> data;
    private ServerConfig config;


    // ########## METHODS ##########

    public AbstractStorage(ServerConfig config) {
        this.config = config;
        this.data = new ArrayList<>();
        this.load();
    }

    public void store() throws IllegalMonitorStateException {
        if(!Thread.holdsLock(this)) throw new IllegalMonitorStateException("Can only be called while holding the object's lock");


    }

    private void load() throws IllegalStateException {
        if (!data.isEmpty()) throw new IllegalStateException("Can only be called when there is no data");
    }
}
