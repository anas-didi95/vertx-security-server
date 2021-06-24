package com.anasdidi.security.common;

import io.vertx.core.Future;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.ext.web.Router;

public abstract class BaseVerticle extends AbstractVerticle {

  public abstract String getContextPath();

  public abstract Router getRouter();

  protected abstract Future<Void> setHandler(Router router, EventBus eventBus);
}
