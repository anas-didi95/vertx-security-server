package com.anasdidi.security.common;

public class ApplicationConfig {

  private static ApplicationConfig config;

  public static ApplicationConfig instance() {
    if (config == null) {
      System.err.println("ERROR! ApplicationConfig is null!");
    }
    return config;
  }

  @Override
  public String toString() {
    return super.toString();
  }

  public String getAppHost() {
    return null;
  }

  public String getAppPort() {
    return null;
  }

  public String getMongoConnectionString() {
    return null;
  }
}
