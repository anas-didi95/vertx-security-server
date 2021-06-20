package com.anasdidi.security.domain.mongo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.ext.mongo.MongoClient;

class MongoService {

  private final static Logger logger = LogManager.getLogger(MongoService.class);
  private final MongoClient mongoClient;

  MongoService(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
  }

  Single<String> create(MongoDTO dto) {
    if (logger.isDebugEnabled()) {
      logger.debug(dto.document);
    }

    return mongoClient.rxSave(dto.collection, dto.document).toSingle();
  }
}
