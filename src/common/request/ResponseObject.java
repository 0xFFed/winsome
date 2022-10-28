package common.request;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.naming.StringRefAddr;

import common.Comment;
import common.Post;

public class ResponseObject implements Serializable {

    private static final long serialVersionUID = 3462L;

    // true if the command has failed, false otherwise
    private boolean success;

    // success/failure output message for the given command
    private String message;

    // string data returned at registration/login
    private String stringData;

    // array of string data returned in various operations
    private ArrayList<String> stringArray;

    // array of Posts returned in various operations
    private ArrayList<String> secondStringArray;

    // enum for easy outcome determination
    public enum Result {
        SUCCESS,
        ERROR
    }


    public ResponseObject(Result result, String message, String stringData, ArrayList<String> stringArray, ArrayList<String> secondStringArray) {
        this.success = Objects.requireNonNull(result, "You must specify whether the requested operation failed or not") == Result.SUCCESS;
        this.message = Objects.requireNonNull(message, "You must provide a success/failure message for the request result");
        this.stringData = stringData;
        this.stringArray = stringArray;
        this.secondStringArray = secondStringArray;
    }

    
    /** 
     * @return boolean
     */
    public boolean isSuccess() {
        return this.success;
    }

    
    /** 
     * @return String
     */
    public String getOutput() {
        return this.message;
    }

    
    /** 
     * @return String
     */
    public String getStringData() {
        return this.stringData;
    }

    
    /** 
     * @return ArrayList<String>
     */
    public ArrayList<String> getStringArray() {
        return this.stringArray;
    }

    
    /** 
     * @return ArrayList<String>
     */
    public ArrayList<String> getSecondStringArray() {
        return this.secondStringArray;
    }

    
    /** 
     * @return String
     */
    @Override
    public String toString() {
        return "ResultObject [success="+this.success+", message="+this.message+"]";
    }

}