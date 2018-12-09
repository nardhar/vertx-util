package org.nardhar.vertx.repository;

import io.vertx.core.Future;
import org.nardhar.vertx.exception.ValidationException;

public interface Model {

    String getId();

    void setId(String id);

    default Future<ValidationException> validate() {
        return Future.succeededFuture(new ValidationException("", ""));
    };

}
