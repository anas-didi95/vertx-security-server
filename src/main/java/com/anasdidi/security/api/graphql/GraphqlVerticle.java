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
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;
import io.vertx.ext.web.handler.graphql.VertxDataFetcher;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.reactivex.ext.web.handler.graphql.GraphiQLHandler;

public class GraphqlVerticle extends AbstractVerticle {

  private final Logger logger = LogManager.getLogger(GraphqlVerticle.class);
  private final Router mainRouter;
  private final GraphqlDataFetcher dataFetcher;

  public GraphqlVerticle(Router mainRouter, EventBus eventBus) {
    this.mainRouter = mainRouter;
    this.dataFetcher = new GraphqlDataFetcher(eventBus);
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    mainRouter.post("/graphql").handler(GraphQLHandler.create(createGraphQL()));
    mainRouter.get("/graphiql*").handler(GraphiQLHandler.create(new GraphiQLHandlerOptions().setEnabled(true)));

    logger.info("[start] Deployed success");
    startPromise.complete();
  }

  private GraphQL createGraphQL() {
    String schema = vertx.fileSystem().readFileBlocking("schema.graphql").toString();
    SchemaParser schemaParser = new SchemaParser();
    TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

    RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()//
        .type("Query", builder -> builder//
            .dataFetcher("getUser", new VertxDataFetcher<>(dataFetcher::getUser)))//
        .build();

    SchemaGenerator schemaGenerator = new SchemaGenerator();
    GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
    return GraphQL.newGraphQL(graphQLSchema).build();
  }
}
