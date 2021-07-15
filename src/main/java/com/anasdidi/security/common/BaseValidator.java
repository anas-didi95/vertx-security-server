package com.anasdidi.security.common;

import java.util.List;
import com.anasdidi.security.common.ApplicationConstants.ErrorValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.reactivex.rxjava3.core.Single;

public abstract class BaseValidator<T extends BaseVO> {

  private static Logger logger = LogManager.getLogger(BaseValidator.class);

  public enum ValidateAction {
    CREATE, UPDATE, DELETE, LOGIN, CHECK
  }

  protected List<String> validateCreate(T vo) {
    return null;
  }

  protected List<String> validateUpdate(T vo) {
    return null;
  }

  protected List<String> validateDelete(T vo) {
    return null;
  }

  protected List<String> validateLogin(T vo) {
    return null;
  }

  protected List<String> validateCheck(T vo) {
    return null;
  }

  public final Single<T> validate(T vo, ValidateAction action) throws ApplicationException {
    List<String> errorList = null;

    switch (action) {
      case CREATE:
        errorList = validateCreate(vo);
        break;
      case UPDATE:
        errorList = validateUpdate(vo);
        break;
      case DELETE:
        errorList = validateDelete(vo);
        break;
      case LOGIN:
        errorList = validateLogin(vo);
        break;
      case CHECK:
        errorList = validateCheck(vo);
        break;
    }

    return validate(errorList, vo, action);
  };

  private final Single<T> validate(List<String> errorList, T vo, ValidateAction action) {
    if (errorList == null) {
      logger.warn("[validate:{}] action={}, vo={}", vo.traceId, action, vo);
      logger.warn("[validate:{}] Validation not implemented!", vo.traceId);
    } else if (!errorList.isEmpty()) {
      logger.error("[validate:{}] action={}, vo={}", vo.traceId, action, vo);
      return Single.error(new ApplicationException(ErrorValue.VALIDATION, vo.traceId, errorList));
    }

    return Single.just(vo);
  }

  protected final void isMandatory(List<String> errorList, Object value, String fieldName) {
    isMandatory(errorList, value, fieldName, "%s is mandatory field!");
  }

  @SuppressWarnings({"rawtypes"})
  protected final void isMandatory(List<String> errorList, Object value, String fieldName,
      String template) {
    boolean isFailed = false;

    if (value instanceof String) {
      isFailed = (value == null || ((String) value).isBlank());
    } else if (value instanceof List) {
      isFailed = (value == null || ((List) value).isEmpty());
    } else if (value instanceof Boolean) {
      isFailed = (value == null || !((Boolean) value));
    } else {
      isFailed = (value == null);
    }

    if (isFailed) {
      errorList.add(String.format(template, fieldName));
    }
  }
}
