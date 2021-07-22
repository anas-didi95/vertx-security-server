package com.anasdidi.security.domain.graphql;

import com.anasdidi.security.common.BaseVerticle;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.ext.web.handler.graphql.schema.VertxDataFetcher;
import io.vertx.rxjava3.core.eventbus.EventBus;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.RoutingContext;
import io.vertx.rxjava3.ext.web.handler.JWTAuthHandler;
import io.vertx.rxjava3.ext.web.handler.graphql.GraphQLHandler;

public class GraphqlVerticle extends BaseVerticle {

  private final GraphqlDataFetcher dataFetcher;

  public GraphqlVerticle() {
    this.dataFetcher = new GraphqlDataFetcher();
  }

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    super.start(startFuture);
    startFuture.complete();
  }

  @Override
  public String getContextPath() {
    return GraphqlConstants.CONTEXT_PATH;
  }

  @Override
  protected String getPermission() {
    return null;
  }

  @Override
  protected void setHandler(Router router, EventBus eventBus, JWTAuthHandler jwtAuthHandler,
      Handler<RoutingContext> jwtAuthzHandler) {
    router.post("/").handler(GraphQLHandler.create(createGraphQL()));
  }

  private GraphQL createGraphQL() {
    String schema = vertx.fileSystem().readFileBlocking("schema.graphql").toString();
    SchemaParser schemaParser = new SchemaParser();
    TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

    RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
        .type("Query",
            builder -> builder.dataFetcher("ping", VertxDataFetcher.create(dataFetcher::ping)))
        .build();

    SchemaGenerator schemaGenerator = new SchemaGenerator();
    GraphQLSchema graphQLSchema =
        schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
    return GraphQL.newGraphQL(graphQLSchema).build();
  }
}
