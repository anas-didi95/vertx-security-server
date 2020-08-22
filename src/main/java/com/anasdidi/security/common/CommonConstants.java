package com.anasdidi.security.common;

public class CommonConstants {

  public enum Header {
    ACCEPT("Accept"), CONTENT_TYPE("Content-Type");

    public String value;

    Header(String value) {
      this.value = value;
    }
  }

  public enum MediaType {
    APP_JSON("application/json");

    public String value;

    MediaType(String value) {
      this.value = value;
    }
  }
}
