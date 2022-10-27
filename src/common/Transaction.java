package common;

import java.sql.Timestamp;

public class Transaction {

    private int value;
    private String timestamp;
    
    public Transaction(int value) {
        this.value = value;
        this. timestamp = new Timestamp(System.currentTimeMillis()).toString();
    }


    // getter
    public int getValue() {
        return this.value;
    }

    // getter
    public String getTime() {
        return this.timestamp;
    }
}
