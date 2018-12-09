package org.nardhar.vertx.exception;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.Builder;
import lombok.Data;

import java.util.Arrays;

@Data
@Builder
public class FieldError {

    private String field;
    private String code;
    private Object[] args;

    public String encode() {
        return encodeToJsonObject().encode();
    }

    public JsonObject encodeToJsonObject() {
        return new JsonObject()
            .put("field", field)
            .put("code", code)
            .put("args", new JsonArray(Arrays.asList(args)));
    }

}
