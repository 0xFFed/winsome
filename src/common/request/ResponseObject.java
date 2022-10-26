package common.request;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private ArrayList<Post> postArray;

    // enum for easy outcome determination
    public enum Result {
        SUCCESS,
        ERROR
    }


    public ResponseObject(Result result, String message, String stringData, ArrayList<String> stringArray, ArrayList<Post> postArray) {
        this.success = Objects.requireNonNull(result, "You must specify whether the requested operation failed or not") == Result.SUCCESS;
        this.message = Objects.requireNonNull(message, "You must provide a success/failure message for the request result");
        this.stringData = stringData;
        this.stringArray = stringArray;
        this.postArray = postArray;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public String getOutput() {
        return this.message;
    }

    public String getStringData() {
        return this.stringData;
    }

    public ArrayList<String> getStringArray() {
        return this.stringArray;
    }

    public ArrayList<Post> getPostArray() {
        return this.postArray;
    }

    @Override
    public String toString() {
        return "ResultObject [success="+this.success+", message="+this.message+"]";
    }

}