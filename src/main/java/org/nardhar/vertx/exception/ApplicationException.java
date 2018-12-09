package org.nardhar.vertx.exception;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;

public class ApplicationException extends Exception {

    private String code;
    private Object[] args;

    public ApplicationException(Throwable cause, String code) {
        super(cause.getMessage(), cause);
        this.code = code;
    }

    public ApplicationException(String message, String code) {
        super(message);
        this.code = code;
    }

    public ApplicationException(String message, String code, Object... args) {
        super(message);
        this.code = code;
        this.args = args;
    }

    public ApplicationException(String message, Throwable cause, String code) {
        super(message, cause);
        this.code = code;
    }

    public ApplicationException(String message, Throwable cause, String code, Object... args) {
        super(message, cause);
        this.code = code;
        this.args = args;
    }

    public String encode() {
        if (args != null) {
            return new JsonObject()
                .put("message", getMessage())
                .put("code", code)
                .put("args", new JsonArray(Arrays.asList(args)))
                .encode();
        }
        return new JsonObject()
            .put("message", getMessage())
            .put("code", code)
            .encode();
    }

}
