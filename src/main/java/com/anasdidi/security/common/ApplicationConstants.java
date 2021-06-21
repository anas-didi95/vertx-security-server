package com.anasdidi.security.common;

public class ApplicationConstants {

  public enum Event {
    MONGO_CREATE("mongo-create");

    public final String address;

    Event(String address) {
      this.address = address;
    }
  }
}
