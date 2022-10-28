package common;

import java.sql.Timestamp;

public class Transaction {

    // value of the transaction
    private double value;

    // timestamp of when the transaction was made
    private String timestamp;
    
    public Transaction(double value) {
        this.value = value;
        this.timestamp = new Timestamp(System.currentTimeMillis()).toString();
    }


    // getter
    public double getValue() {
        return this.value;
    }

    // getter
    public String getTime() {
        return this.timestamp;
    }
}
