package org.nardhar.vertx.repository;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.nardhar.vertx.eventbus.BusSender;
import org.nardhar.vertx.exception.ValidationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface RepositorySender extends BusSender {

    default <T extends Model> Map<String, String> dbHeaders(Class<T> clazz) {
        Map<String, String> headers = new HashMap<>();

        headers.put("model", clazz.getName());

        return headers;
    }

    default <T extends Model> Function<T, Future<T>> dbSave(Class<T> clazz) {
        // we get the class of the model
        return (model) -> dbSave(clazz, model);
    }

    default <T extends Model> Future<T> dbSave(Class<T> clazz, T model) {
        // sends to databaseVerticle converting the model to a jsonObject
        return busGetObject("repository.save", dbHeaders(clazz), JsonObject.mapFrom(model))
            .compose((savedModel) -> Future.succeededFuture(savedModel.mapTo(clazz)));
    }

    default <T extends Model> Future<T> dbInsert(Class<T> clazz, T model) {
        return busGetObject("repository.insert", dbHeaders(clazz), JsonObject.mapFrom(model))
            .compose((savedModel) -> Future.succeededFuture(savedModel.mapTo(clazz)));
    }

    default <T extends Model> Future<T> dbUpdate(Class<T> clazz, T model) {
        return dbUpdate(clazz, model, new JsonObject());
    }

    default <T extends Model> Future<T> dbUpdate(Class<T> clazz, T model, JsonObject options) {
        Map<String, String> headers = dbHeaders(clazz);
        if (options.containsKey("upsert")) headers.put("upsert", "true");
        if (options.containsKey("writeConcern")) headers.put("writeConcern", options.getString("writeConcern"));

        return busGetObject("repository.update", headers, JsonObject.mapFrom(model))
            .compose((updatedModel) -> Future.succeededFuture(updatedModel.mapTo(clazz)));
    }

    default <T extends Model> Function<T, Future<T>> dbUpdate(Class<T> clazz) {
        return (model) -> dbUpdate(clazz, model);
    }

    default <T extends Model> Function<T, Future<T>> dbUpdate(Class<T> clazz, JsonObject options) {
        return (model) -> dbUpdate(clazz, model, options);
    }

    default <T extends Model> Future<List<T>> dbUpdateMulti(Class<T> clazz, JsonObject query, JsonObject data) {
        return dbUpdateMulti(clazz, query, data, new JsonObject());
    }

    default <T extends Model> Future<List<T>> dbUpdateMulti(Class<T> clazz, JsonObject query, JsonObject data, JsonObject options) {
        Map<String, String> headers = dbHeaders(clazz);
        headers.put("multi", options.getString("multi"));
        if (options.containsKey("upsert")) headers.put("upsert", options.getString("upsert"));
        if (options.containsKey("writeConcern")) headers.put("writeConcern", options.getString("writeConcern"));

        return busGetObject(
            "repository.updateMulti",
            headers,
            new JsonObject()
                .put("query", query)
                .put("data", data)
        )
            .compose((result) -> dbFindAll(clazz, query));
    }

    default <T extends Model> Future<List<T>> dbReplace(Class<T> clazz, JsonObject query, JsonObject data, JsonObject options) {
        return busGetArray(
            "repository.replace",
            dbHeaders(clazz),
            new JsonObject()
                .put("query", query)
                .put("data", data)
        )
            .compose((result) -> dbFindAll(clazz, query));
    }

    default <T extends Model> Future<T> dbFindOne(Class<T> clazz, JsonObject query) {
        return busGetObject("repository.findOne", dbHeaders(clazz), query)
            .compose((foundModel) -> Future.succeededFuture(foundModel.mapTo(clazz)));
    }

    default <T extends Model> Future<List<T>> dbFindAll(Class<T> clazz, JsonObject query) {
        return busGetArray("repository.findAll", dbHeaders(clazz), query)
            .compose((foundList) -> Future.succeededFuture(
                foundList.stream()
                    .map(row -> ((JsonObject)row).mapTo(clazz))
                    .collect(Collectors.toList())
            ));
    }

    default <T extends Model> Future<T> dbDelete(Class<T> clazz, JsonObject query) {
        return busGetObject("repository.delete", dbHeaders(clazz), query)
            .compose((foundModel) -> Future.succeededFuture(foundModel.mapTo(clazz)));
    }

    default <T extends Model> Future<T> dbDelete(Class<T> clazz, T model) {
        return dbDelete(clazz, JsonObject.mapFrom(model));
    }

    default <T extends Model> Function<T, Future<T>> dbDelete(Class<T> clazz) {
        return (model) -> dbDelete(clazz, model);
    }

    default <T extends Model> Future<List<T>> dbDeleteAll(Class<T> clazz, JsonObject query) {
        // sends to databaseVerticle converting the model to a jsonObject
        return busGetObject("repository.deleteAll", dbHeaders(clazz), query)
            .compose((result) -> dbFindAll(clazz, query));
    }

    default <T extends Model> Future<Long> dbCount(Class<T> clazz, JsonObject query) {
        return busGetObject("repository.count", dbHeaders(clazz), query)
            .compose((result) -> Future.succeededFuture(result.getLong("count")));
    }

    /**
     * Returns a function which in turn verifies if a ValidationException has any errors or if should continue with the validated model
     * @param modelInstance The model to respond
     * @param <T> The type of the model
     * @return The function to be executed for chaining
     */
    default <T extends Model> Function<ValidationException, Future<T>> verifyErrors(T modelInstance) {
        return (validationException) -> {
            if (validationException.hasErrors()) {
                return Future.failedFuture(validationException);
            }
            return Future.succeededFuture(modelInstance);
        };
    }

}
