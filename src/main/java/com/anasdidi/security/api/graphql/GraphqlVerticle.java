package com.anasdidi.security.api.graphql;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;
import io.vertx.ext.web.handler.graphql.VertxDataFetcher;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.JWTAuthHandler;
import io.vertx.reactivex.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.reactivex.ext.web.handler.graphql.GraphiQLHandler;

public class GraphqlVerticle extends AbstractVerticle {

  private final Logger logger = LogManager.getLogger(GraphqlVerticle.class);
  private final Router mainRouter;
  private final JWTAuth jwtAuth;
  private final JsonObject cfg;
  private final GraphqlDataFetcher dataFetcher;

  public GraphqlVerticle(Router mainRouter, EventBus eventBus, JWTAuth jwtAuth, JsonObject cfg) {
    this.mainRouter = mainRouter;
    this.jwtAuth = jwtAuth;
    this.cfg = cfg;
    this.dataFetcher = new GraphqlDataFetcher(eventBus);
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    Router router = Router.router(vertx);
    router.route().handler(JWTAuthHandler.create(jwtAuth));
    router.post("/").handler(GraphQLHandler.create(createGraphQL()));
    mainRouter.mountSubRouter("/graphql", router);

    if (cfg.getBoolean("GRAPHIQL_IS_ENABLE", false)) {
      Router router1 = Router.router(vertx);
      router1.post("/graphql").handler(GraphQLHandler.create(createGraphQL()));
      router1.get("/*").handler(GraphiQLHandler.create(new GraphiQLHandlerOptions()//
          .setGraphQLUri("/graphiql/graphql")//
          .setEnabled(cfg.getBoolean("GRAPHIQL_IS_ENABLE", false))));
      mainRouter.mountSubRouter("/graphiql", router1);
    }

    logger.info("[start] Deployed success");
    startPromise.complete();
  }

  private GraphQL createGraphQL() {
    String schema = vertx.fileSystem().readFileBlocking("schema.graphql").toString();
    SchemaParser schemaParser = new SchemaParser();
    TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

    RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()//
        .type("Query", builder -> builder//
            .dataFetcher("getUserList", new VertxDataFetcher<>(dataFetcher::getUserList))//
            .dataFetcher("getUserById", new VertxDataFetcher<>(dataFetcher::getUserById)))//
        .build();

    SchemaGenerator schemaGenerator = new SchemaGenerator();
    GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
    return GraphQL.newGraphQL(graphQLSchema).build();
  }
}
