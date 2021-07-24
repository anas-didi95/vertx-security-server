package com.anasdidi.security.domain.graphql;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.vertx.ext.web.handler.graphql.schema.VertxDataFetcher;
import io.vertx.rxjava3.core.Vertx;

class GraphqlUtils {

  static GraphQL createGraphQL(Vertx vertx) {
    String schema = vertx.fileSystem().readFileBlocking("schema.graphql").toString();
    SchemaParser schemaParser = new SchemaParser();
    TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);
    GraphqlDataFetcher dataFetcher = new GraphqlDataFetcher(vertx.eventBus());

    RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
        .type("Query",
            builder -> builder.dataFetcher("ping", VertxDataFetcher.create(dataFetcher::ping))
                .dataFetcher("getUserList", VertxDataFetcher.create(dataFetcher::getUserList))
                .dataFetcher("getUserById", VertxDataFetcher.create(dataFetcher::getUserById)))
        .build();

    SchemaGenerator schemaGenerator = new SchemaGenerator();
    GraphQLSchema graphQLSchema =
        schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
    return GraphQL.newGraphQL(graphQLSchema).build();
  }
}
