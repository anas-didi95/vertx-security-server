package com.anasdidi.security.common;

public abstract class BaseVO {

  public final String traceId;

  public BaseVO(String traceId) {
    this.traceId = traceId;
  }
}
