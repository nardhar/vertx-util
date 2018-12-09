package org.nardhar.vertx.eventbus;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import org.nardhar.vertx.exception.ApplicationException;

public interface BusConsumer {

    default void busRespondSuccess(Message<JsonObject> message, Object result) {
        message.reply(result);
    }

    default void busRespondFailure(Message<JsonObject> message, int statusCode, Throwable cause) {
        cause.printStackTrace();
        if (cause instanceof ApplicationException) {
            message.fail(statusCode, ((ApplicationException)cause).encode());
        } else {
            // cause.getMessage() could be an encoded ApplicationException
            // so we should try to parse it and convert to an instance of ApplicationException
            try {
                JsonObject exceptionJson = new JsonObject(cause.getMessage());
                message.fail(statusCode, new ApplicationException(
                    exceptionJson.getString("message"),
                    exceptionJson.getString("code")
                ).encode());
            } catch (DecodeException ex) {
                // otherwise
                message.fail(statusCode, new ApplicationException(cause, "service.error").encode());
            }
        }
    }

    default <T> Handler<AsyncResult<T>> busRespond(Message<JsonObject> message) {
        return (handler) -> {
            if (handler.succeeded()) {
                busRespondSuccess(message, handler.result());
            } else {
                busRespondFailure(message, 400, handler.cause());
            }
        };
    }

    default <T> Handler<AsyncResult<T>> busRespondToJsonObject(Message<JsonObject> message) {
        return (handler) -> {
            if (handler.succeeded()) {
                busRespondSuccess(message, JsonObject.mapFrom(handler.result()));
            } else {
                busRespondFailure(message, 400, handler.cause());
            }
        };
    }

    default <T> Future<JsonObject> toFutureJsonObject(T model) {
        return Future.succeededFuture(JsonObject.mapFrom(model));
    }

}
