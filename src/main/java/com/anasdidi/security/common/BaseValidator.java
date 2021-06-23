package com.anasdidi.security.common;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class BaseValidator<T> {

  private static Logger logger = LogManager.getLogger(BaseValidator.class);

  public enum Action {
    CREATE
  }

  protected abstract List<String> validateCreate(T vo);

  public final T validate(T vo, Action action) throws ApplicationException {
    List<String> errorList = null;

    switch (action) {
      case CREATE:
        errorList = validateCreate(vo);
        break;
    }

    return validate(errorList, vo, action);
  };

  private final T validate(List<String> errorList, T vo, Action action)
      throws ApplicationException {
    if (errorList == null) {
      logger.warn("[validate] action={}, vo={}", action, vo);
      logger.warn("[validate] Validation not implemented!");
    } else if (!errorList.isEmpty()) {
      logger.error("[validate] action={}, vo={}", action, vo);
      throw new ApplicationException(ApplicationConstants.ErrorValue.VALIDATION, errorList);
    }

    return vo;
  }

  protected void isMandatory(List<String> errorList, String value, String fieldName) {
    if (value == null || value.isBlank()) {
      errorList.add(String.format("%s is mandatory field!", fieldName));
    }
  }
}