package server.storage;

public interface Storable {
    
    // stores the data held on local storage as a JSON file
    public void store();

    // loads the data from a JSON file on local storage
    private void load();
}
