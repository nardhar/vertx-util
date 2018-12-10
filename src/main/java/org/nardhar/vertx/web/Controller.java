package org.nardhar.vertx.web;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.nardhar.vertx.eventbus.BusSender;

import java.util.function.Function;

public interface Controller extends BusSender {

    void init();

    Router getRouter();

    void setRouter(Router router);

    void setEventBus(EventBus eventBus);

    default void action(HttpMethod method, String path, Function<RoutingContext, Future<JsonObject>> caller) {
        getRouter()
            .route(method, path)
            .handler((ctx) -> caller.apply(ctx).setHandler((result) -> {
                if (!ctx.response().ended()) {
                    if (result.succeeded()) {
                        ctx.response()
                            .setStatusCode(ctx.request().method().equals(HttpMethod.POST) ? 201 : 200)
                            .end(result.result().encode());
                    } else {
                        // TODO: create a proper error handler
                        ctx.response()
                            .setStatusCode(ctx.request().method().equals(HttpMethod.GET) ? 404 : 400)
                            .end(result.cause().getMessage());
                    }
                }
            }));
    }

    default void actionArray(HttpMethod method, String path, Function<RoutingContext, Future<JsonArray>> caller) {
        getRouter()
            .route(method, path)
                .handler((ctx) -> caller.apply(ctx).setHandler((result) -> {
                    if (!ctx.response().ended()) {
                        if (result.succeeded()) {
                            ctx.response()
                                .setStatusCode(ctx.request().method().equals(HttpMethod.POST) ? 201 : 200)
                                .end(result.result().encode());
                        } else {
                            // TODO: create a proper error handler
                            ctx.response()
                                .setStatusCode(ctx.request().method().equals(HttpMethod.GET) ? 404 : 400)
                                .end(result.cause().getMessage());
                        }
                    }
                }));
    }

    // shortcuts
    default void list(String path, Function<RoutingContext, Future<JsonArray>> caller) {
        actionArray(HttpMethod.GET, path, caller);
    }

    default void get(String path, Function<RoutingContext, Future<JsonObject>> caller) {
        action(HttpMethod.GET, path, caller);
    }

    default void post(String path, Function<RoutingContext, Future<JsonObject>> caller) {
        action(HttpMethod.POST, path, caller);
    }

    default void put(String path, Function<RoutingContext, Future<JsonObject>> caller) {
        action(HttpMethod.PUT, path, caller);
    }

    default void delete(String path, Function<RoutingContext, Future<JsonObject>> caller) {
        action(HttpMethod.DELETE, path, caller);
    }

    default <T> void sendToEventBus(String address, Object message, Handler<AsyncResult<Message<T>>> replyHandler) {
        getEventBus().send(address, message, replyHandler);
    }

    default void sendToEventBus(String address, Object message, Future<JsonObject> future) {
        getEventBus().send(address, message, (result) -> {
            if (result.succeeded()) {
                JsonObject res = (JsonObject)result.result().body();
                future.complete(res);
            } else {
                future.fail(result.cause());
            }
        });
    }

    default Future<JsonObject> getJsonBody(RoutingContext ctx) {
        Future<JsonObject> future = Future.future();

        ctx.request().bodyHandler((body) -> future.complete(body.toJsonObject()));

        return future;
    }

}
