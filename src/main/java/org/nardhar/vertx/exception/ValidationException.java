package org.nardhar.vertx.exception;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ValidationException extends ApplicationException {

    private List<FieldError> fieldErrorList;

    public ValidationException(String message, String code) {
        super(message, code);
        fieldErrorList = new ArrayList<>();
    }

    public ValidationException(String message, String code, Object... args) {
        super(message, code, args);
        fieldErrorList = new ArrayList<>();
    }

    public void addFieldError(FieldError fieldError) {
        this.fieldErrorList.add(fieldError);
    }

    public void addError(String field, String code, Object... args) {
        this.fieldErrorList.add(FieldError.builder()
            .code(code)
            .field(field)
            .args(args)
            .build()
        );
    }

    public boolean hasErrors() {
        return this.fieldErrorList.size() > 0;
    }

    public List<FieldError> getErrors() {
        return fieldErrorList;
    }

    @Override
    public String encode() {
        return new JsonObject()
            .put("errors", new JsonArray(
                fieldErrorList.stream()
                    .map(FieldError::encodeToJsonObject)
                    .collect(Collectors.toList())
            ))
            .encode();
    }

}
