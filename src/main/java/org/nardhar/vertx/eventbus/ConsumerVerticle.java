package org.nardhar.vertx.eventbus;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;

import java.util.ArrayList;
import java.util.List;

public abstract class ConsumerVerticle extends AbstractVerticle {

    // Creating a list of Futures for tracking the register completion
    private List<Future> consumers = new ArrayList<>();

    /**
     * Usual override of AbstractVerticle.start
     * @param startFuture
     */
    @Override
    public void start(Future<Void> startFuture) {
        // calling to dummy method
        registerConsumers();

        // actual tracking of consumer registering
        completeRegistering(startFuture);
    }

    /**
     * Dummy method for overriding with the actual consumer registering process
     */
    public void registerConsumers() {}

    /**
     * Tracking of registering completion
     * @param startFuture
     */
    protected void completeRegistering(Future<Void> startFuture) {
        // waiting for all the consumers to be completed
        CompositeFuture.all(consumers).setHandler((ar) -> {
            // if all the consumers had been successfully registered, then startFuture is complete
            if (ar.succeeded()) startFuture.complete();
            // otherwise propagate the error
            else startFuture.fail(ar.cause());
        });
    }

    /**
     * EventBus Consumer adding with default wrapper for completing
     * @param address The eventBus address
     * @param handler The handler
     */
    protected <T> void addConsumer(String address, Handler<Message<T>> handler) {
        // creating a future for adding to the consumer list
        Future<Void> completer = Future.future();
        // actual registering of the handler in the eventBus
        vertx.eventBus().consumer(address, handler).completionHandler((ar) -> {
            // waiting for its registering to be completed
            if (ar.succeeded()) {
                System.out.println("Consumer registered at " + address);
                completer.complete();
            } else {
                completer.fail(ar.cause());
            }
        });
        // adding the completer future to the consumer list
        consumers.add(completer);
    }

    protected void addDeployFuture(Future future) {
        consumers.add(future);
    }

}
