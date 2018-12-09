package org.nardhar.vertx.eventbus;

import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Map;

public interface BusSender {

    EventBus getEventBus();

    default Future<JsonObject> busGetObject(String address, Object message) {
        return busGet(address, null, message);
    }

    default Future<JsonObject> busGetObject(String address, Map<String, String> headers, Object message) {
        return busGet(address, headers, message);
    }

    default Future<JsonArray> busGetArray(String address, Object message) {
        return busGet(address, message);
    }

    default Future<JsonArray> busGetArray(String address, Map<String, String> headers, Object message) {
        return busGet(address, headers, message);
    }

    default <T> Future<T> busGet(String address, Object message) {
        return busGet(address, null, message);
    }

    @SuppressWarnings("unchecked")
    default <T> Future<T> busGet(String address, Map<String, String> headers, Object message) {
        Future<T> future = Future.future();

        DeliveryOptions deliveryOptions = new DeliveryOptions();
        if (headers != null) {
            headers.keySet().forEach((key) -> deliveryOptions.addHeader(key, headers.get(key)));
        }

        Object actualMessage = message != null ? JsonObject.mapFrom(message) : null;

        getEventBus().send(address, actualMessage, deliveryOptions, (result) -> {
            if (result.succeeded()) {
                future.complete((T)result.result().body());
            } else {
                future.fail(result.cause());
            }
        });

        return future;
    }

}
