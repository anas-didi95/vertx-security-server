package com.anasdidi.security.common;

import java.util.List;

public abstract class BaseValidator<T> {

  public enum Action {
    CREATE
  }

  protected abstract T validate(T vo, Action action) throws ApplicationException;

  protected void isMandatory(List<String> errorList, String value, String fieldName) {
    if (value == null || value.isBlank()) {
      errorList.add(String.format("%s is mandatory field!", fieldName));
    }
  }
}
