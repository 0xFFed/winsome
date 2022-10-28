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


    
    /** 
     * @return double
     */
    public double getValue() {
        return this.value;
    }

    
    /** 
     * @return String
     */
    public String getTime() {
        return this.timestamp;
    }
}
