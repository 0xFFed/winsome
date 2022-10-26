package common.request;

import java.io.Serializable;
import java.util.Objects;

public class ResponseObject implements Serializable {

    private static final long serialVersionUID = 3462L;

    // true if the command has failed, false otherwise
    private boolean success;

    // success/failure output message for the given command
    private String message;

    // token returned at registration/login
    private String token;

    // enum for easy outcome determination
    public enum Result {
        SUCCESS,
        ERROR
    }


    public ResponseObject(Result result, String message, String token) {
        this.success = Objects.requireNonNull(result, "You must specify whether the requested operation failed or not") == Result.SUCCESS;
        this.message = Objects.requireNonNull(message, "You must provide a success/failure message for the request result");
        this.token = token;
    }

    public boolean hasFailed() {
        return this.success;
    }

    public String getOutput() {
        return this.message;
    }

    public String getToken() {
        return this.token;
    }

    @Override
    public String toString() {
        return "ResultObject [success="+this.success+", message="+this.message+"]";
    }

}