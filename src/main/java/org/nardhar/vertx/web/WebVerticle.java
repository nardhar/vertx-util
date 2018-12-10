package org.nardhar.vertx.web;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.web.Router;

public class WebVerticle extends AbstractVerticle {

    @Override
    public void start(Future<Void> startFuture) {
        Router router = Router.router(vertx);

        initControllers(router);

        vertx.createHttpServer()
            .requestHandler(router::accept)
            .listen(config().getInteger("port"), (ar) -> {
                if (ar.succeeded()) {
                    System.out.println("Web server started");
                    startFuture.complete();
                } else {
                    startFuture.fail(ar.cause());
                }
            });
    }

    @SuppressWarnings("unchecked")
    public void initControllers(Router router) {
        // TODO: we could get all @Controller annotated classes instead of passing a configuration variable
        config().getJsonArray("controllers")
            .forEach((controllerClass) -> {
                try {
                    Controller controller = ((Class<Controller>) Class.forName((String)controllerClass)).newInstance();
                    controller.setRouter(router);
                    controller.setEventBus(vertx.eventBus());
                    controller.init();
                } catch (IllegalAccessException|InstantiationException|ClassNotFoundException ex) {
                    // TODO: add a proper logger
                    System.out.println("Could not deploy controller " + controllerClass);
                }
            });
    }

}
