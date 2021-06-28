package com.anasdidi.security.common;

import java.util.List;
import com.anasdidi.security.common.ApplicationConstants.ErrorValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class BaseValidator<T extends BaseVO> {

  private static Logger logger = LogManager.getLogger(BaseValidator.class);

  public enum ValidateAction {
    CREATE, UPDATE
  }

  protected abstract List<String> validateCreate(T vo);

  protected abstract List<String> validateUpdate(T vo);

  public final T validate(T vo, ValidateAction action) throws ApplicationException {
    List<String> errorList = null;

    switch (action) {
      case CREATE:
        errorList = validateCreate(vo);
        break;
      case UPDATE:
        errorList = validateUpdate(vo);
        break;
    }

    return validate(errorList, vo, action);
  };

  private final T validate(List<String> errorList, T vo, ValidateAction action)
      throws ApplicationException {
    if (errorList == null) {
      logger.warn("[validate:{}] action={}, vo={}", vo.traceId, action, vo);
      logger.warn("[validate:{}] Validation not implemented!", vo.traceId);
    } else if (!errorList.isEmpty()) {
      logger.error("[validate:{}] action={}, vo={}", vo.traceId, action, vo);
      throw new ApplicationException(ErrorValue.VALIDATION, vo.traceId, errorList);
    }

    return vo;
  }

  protected final void isMandatory(List<String> errorList, Object value, String fieldName) {
    if (value instanceof String) {
      if (value == null || ((String) value).isBlank()) {
        errorList.add(String.format("%s is mandatory field!", fieldName));
      }
    } else {
      if (value == null) {
        errorList.add(String.format("%s is mandatory field!", fieldName));
      }
    }
  }
}
