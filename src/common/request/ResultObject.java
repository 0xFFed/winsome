package common.request;

import java.io.Serializable;
import java.util.Objects;

public class ResultObject implements Serializable {

    private static final long serialVersionUID = 3462L;

    // true if the command has failed, false otherwise
    protected boolean success;

    // success/failure output message for the given command
    protected String message;

    // enum for easy outcome determination
    public enum Result {
        SUCCESS,
        ERROR
    }


    public ResultObject(Result result, String message) {
        this.success = Objects.requireNonNull(result, "You must specify whether the requested operation failed or not") == Result.SUCCESS;
        this.message = Objects.requireNonNull(message, "You must provide a success/failure message for the request result");
    }

    public boolean hasFailed() {
        return this.success;
    }

    public String getOutput() {
        return this.message;
    }

    @Override
    public String toString() {
        return "RequestResult [success="+this.success+", message="+this.message+"]";
    }

}