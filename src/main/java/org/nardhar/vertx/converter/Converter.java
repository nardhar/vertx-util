package org.nardhar.vertx.converter;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Converter {

    private Converter() {}

    public static <T> JsonObject toJsonObject(T object) {
        return JsonObject.mapFrom(object);
    }

    public static <T> Future<JsonObject> toFutureJsonObject(T object) {
        return Future.succeededFuture(toJsonObject(object));
    }

    public static <T> JsonArray toJsonArray(List<T> list) {
        return new JsonArray(
            list.stream()
                .map(Converter::toJsonObject)
                .collect(Collectors.toList())
        );
    }

    public static <T> Future<JsonArray> toFutureJsonArray(List<T> list) {
        return Future.succeededFuture(toJsonArray(list));
    }

    public static <T, U> Function<T, Future<U>> toFutureJsonObject(Function<T, U> fun) {
        return (obj) -> Future.succeededFuture(fun.apply(obj));
    }

    @SuppressWarnings("unchecked")
    public static <T> Function<JsonArray, Future<JsonArray>> toFutureJsonArray(Function<T, JsonObject> fun) {
        return (array) -> Future.succeededFuture(new JsonArray(
            array.stream()
                .map(obj -> fun.apply((T) obj))
                .collect(Collectors.toList())
        ));
    }
}
