package org.nardhar.vertx.repository;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.ext.mongo.WriteOption;
import org.nardhar.vertx.eventbus.ConsumerVerticle;
import org.nardhar.vertx.exception.ApplicationException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RepositoryVerticle extends ConsumerVerticle {

    private MongoClient mongoClient;

    private Map<String, Class<? extends Model>> modelClass;
    private Map<String, String> modelCollection;

    @Override
    @SuppressWarnings("unchecked")
    public void registerConsumers() {
        mongoClient = MongoClient.createShared(vertx, new JsonObject()
            .put("connection_string", config().getString("mongoConnectionString"))
        );

        List<Class<? extends Model>> clazzModels = new ArrayList<>();

        // TODO: we could get all @Model annotated classes instead of passing a configuration variable
        config().getJsonArray("models")
            .forEach((clazz) -> {
                try {
                    clazzModels.add((Class<Model>)Class.forName((String)clazz));
                } catch (ClassNotFoundException ex) {
                    System.out.println("Could not find model " + clazz);
                }
            });

        // registering model classes
        modelClass = new HashMap<>();

        clazzModels.forEach((clazz) -> modelClass.put(clazz.getName(), clazz));

        // registering model collection names for mongodb
        modelCollection = new HashMap<>();

        Future<CompositeFuture> modelsFuture = Future.future();
        CompositeFuture.all(clazzModels.stream().map((clazz) -> {
            Future<Void> future = Future.future();
            try {
                Field field = clazz.getDeclaredField("collection");
                field.setAccessible(true);
                modelCollection.put(clazz.getName(), (String)field.get(null));
                future.complete();
            } catch (NoSuchFieldException ex) {
                future.fail("No collection property for model " + clazz.getName());
            } catch (IllegalAccessException ex) {
                future.fail("Collection property not readable for model " + clazz.getName());
            }
            return future;
        }).collect(Collectors.toList()))
        .setHandler(modelsFuture.completer());

        // adding endpoints
        addConsumer("repository.save", this::save);
        addConsumer("repository.insert", this::insert);
        addConsumer("repository.update", this::update);
        addConsumer("repository.updateMulti", this::updateMulti);
        addConsumer("repository.replace", this::replace);
        addConsumer("repository.findAll", this::findAll);
        addConsumer("repository.findOne", this::findOne);
        addConsumer("repository.delete", this::delete);
        addConsumer("repository.deleteAll", this::deleteAll);
        addConsumer("repository.count", this::count);
        addDeployFuture(modelsFuture);
    }

    public void save(Message<JsonObject> message) {
        String model = message.headers().get("model");

        mongoClient.save(modelCollection.get(model), message.body(), res -> {
            if (res.succeeded()) {
                // returning an object copy with its id modified and removing the _id from mongo
                JsonObject record = message.body().copy();
                record.put("id", res.result())
                    .remove("_id");

                message.reply(record);
            } else {
                message.fail(400, new ApplicationException(
                    res.cause(),
                    "repository.save.error"
                ).encode());
            }
        });
    }

    public void insert(Message<JsonObject> message) {
        String model = message.headers().get("model");

        mongoClient.insert(modelCollection.get(model), message.body(), res -> {
            if (res.succeeded()) {
                JsonObject record = message.body().copy();
                record.put("id", res.result())
                        .remove("_id");

                message.reply(record);
            } else {
                message.fail(400, new ApplicationException(
                        res.cause(),
                        "repository.save.error"
                ).encode());
            }
        });
    }

    public void update(Message<JsonObject> message) {
        String model = message.headers().get("model");

        JsonObject modelData = message.body().copy();

        // remove the id from the modelData and put it in the query
        JsonObject query = new JsonObject()
            .put("_id", modelData.remove("id"));
        // update with the current modelData, id is removed so only updates the other data
        JsonObject update = new JsonObject().put("$set", modelData);

        if (message.headers().contains("upsert") || message.headers().contains("writeConcern")) {
            UpdateOptions options = new UpdateOptions();
            if (message.headers().contains("upsert")) {
                options.setUpsert(true);
            }
            if (message.headers().contains("writeConcern")) {
                options.setWriteOption(WriteOption.valueOf(message.headers().get("writeConcern").toUpperCase()));
            }
            mongoClient.updateCollectionWithOptions(modelCollection.get(model), query, update, options, res -> {
                if (res.succeeded()) {
                    message.reply(modelData.put("id", query.getString("_id")));
                } else {
                    message.fail(400, new ApplicationException(
                        res.cause(),
                        "repository.update.error"
                    ).encode());
                }
            });
        } else {
            mongoClient.updateCollection(modelCollection.get(model), query, update, res -> {
                if (res.succeeded()) {
                    message.reply(modelData.put("id", query.getString("_id")));
                } else {
                    message.fail(400, new ApplicationException(
                        res.cause(),
                        "repository.update.error"
                    ).encode());
                }
            });
        }
    }

    public void updateMulti(Message<JsonObject> message) {
        String model = message.headers().get("model");

        JsonObject modelData = message.body().getJsonObject("data").copy();

        // remove the id from the modelData and put it in the query
        JsonObject query = message.body().getJsonObject("query");
        // update with the current modelData, id is removed so only updates the other data
        JsonObject update = new JsonObject().put("$set", modelData);

        UpdateOptions options = new UpdateOptions();
        if (message.headers().contains("upsert")) {
            options.setUpsert(true);
        }
        if (message.headers().contains("writeConcern")) {
            options.setWriteOption(WriteOption.valueOf(message.headers().get("writeConcern").toUpperCase()));
        }
        mongoClient.updateCollectionWithOptions(modelCollection.get(model), query, update, options, res -> {
            if (res.succeeded()) {
                message.reply(new JsonObject().put("success", true));
            } else {
                message.fail(400, new ApplicationException(
                    res.cause(),
                    "repository.update.error"
                ).encode());
            }
        });
    }

    public void replace(Message<JsonObject> message) {
        String model = message.headers().get("model");

        JsonObject modelData = message.body().getJsonObject("data").copy();

        // remove the id from the modelData and put it in the query
        JsonObject query = message.body().getJsonObject("query");
        // update with the current modelData, id is removed so only updates the other data
        JsonObject replace = new JsonObject().put("$set", modelData);

        mongoClient.replaceDocuments(modelCollection.get(model), query, replace, res -> {
            if (res.succeeded()) {
                message.reply(new JsonObject().put("success", true));
            } else {
                message.fail(400, new ApplicationException(
                    res.cause(),
                    "repository.replace.error"
                ).encode());
            }
        });
    }

    public void findAll(Message<JsonObject> message) {
        String model = message.headers().get("model");
        JsonObject query = message.body() != null ? message.body() : new JsonObject();

        mongoClient.find(modelCollection.get(model), query, res -> {
            if (res.succeeded()) {
                message.reply(new JsonArray(
                    res.result()
                        .stream()
                        .map((record) -> record.put("id", record.remove("_id")))
                        .collect(Collectors.toList())
                ));
            } else {
                message.fail(400, new ApplicationException(
                    res.cause(),
                    "repository.findAll.error"
                ).encode());
            }
        });
    }

    public void findOne(Message<JsonObject> message) {
        String model = message.headers().get("model");
        JsonObject query = message.body() != null ? message.body() : new JsonObject();

        mongoClient.findOne(modelCollection.get(model), query, null, res -> {
            if (res.succeeded()) {
                if (res.result() == null) {
                    message.fail(
                        404,
                        new ApplicationException(
                            modelClass.get(model).getSimpleName() + " Not Found",
                            "repository.notFound.error"
                        ).encode()
                    );
                } else {
                    JsonObject object = res.result().copy();
                    object.put("id", object.remove("_id"));

                    message.reply(object);
                }
            } else {
                message.fail(400, new ApplicationException(
                    res.cause(),
                    "repository.findOne.error"
                ).encode());
            }
        });
    }

    public void delete(Message<JsonObject> message) {
        String model = message.headers().get("model");

        JsonObject modelData = message.body().copy();

        // remove the id from the modelData and put it in the query
        JsonObject query = new JsonObject()
            .put("_id", modelData.getString("id"));

        mongoClient.removeDocument(modelCollection.get(model), query, res -> {
            if (res.succeeded()) {
                message.reply(modelData);
            } else {
                message.fail(400, new ApplicationException(
                    res.cause(),
                    "repository.delete.error"
                ).encode());
            }
        });
    }

    public void deleteAll(Message<JsonObject> message) {
        String model = message.headers().get("model");

        JsonObject query = message.body().copy();

        mongoClient.removeDocuments(modelCollection.get(model), query, res -> {
            if (res.succeeded()) {
                message.reply(new JsonObject().put("success", true));
            } else {
                message.fail(400, new ApplicationException(
                    res.cause(),
                    "repository.deleteAll.error"
                ).encode());
            }
        });
    }

    public void count(Message<JsonObject> message) {
        String model = message.headers().get("model");

        JsonObject query = message.body().copy();

        mongoClient.count(modelCollection.get(model), query, res -> {
            if (res.succeeded()) {
                message.reply(new JsonObject().put("count", res.result()));
            } else {
                message.fail(400, new ApplicationException(
                    res.cause(),
                    "repository.count.error"
                ).encode());
            }
        });
    }

}
