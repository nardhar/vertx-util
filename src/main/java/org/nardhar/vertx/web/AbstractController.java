package org.nardhar.vertx.web;

import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.Router;
import lombok.Data;

@Data
public abstract class AbstractController implements Controller {

    private Router router;
    private EventBus eventBus;

    public AbstractController() {
    }

    public abstract void init();

}
