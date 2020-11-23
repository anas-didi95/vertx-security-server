package com.anasdidi.security.api.user;

import java.util.ArrayList;
import java.util.List;

import com.anasdidi.security.common.ApplicationException;
import com.anasdidi.security.common.CommonConstants;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonArray;

class UserValidator {

  private final Logger logger = LogManager.getLogger(UserValidator.class);

  enum Validate {
    CREATE, UPDATE, DELETE;
  }

  UserVO validate(Validate val, UserVO vo, String requestId) throws ApplicationException {
    String tag = "validate";
    List<String> errorList = new ArrayList<>();

    switch (val) {
      case CREATE:
        errorList = validateCreate(vo, errorList);
        break;
      case UPDATE:
        errorList = validateUpdate(vo, errorList);
        break;
      case DELETE:
        errorList = validateDelete(vo, errorList);
        break;
    }

    if (!errorList.isEmpty()) {
      logger.error("[{}:{}] {} validate={}\n{}", tag, requestId,
          CommonConstants.MSG_ERR_VALIDATE_ERROR, val, vo.toString());
      throw new ApplicationException(CommonConstants.MSG_ERR_VALIDATE_ERROR, requestId,
          new JsonArray(errorList));
    }

    return vo;
  }

  private List<String> validateCreate(UserVO vo, List<String> errorList) {
    String username = vo.username;
    String password = vo.password;
    String fullName = vo.fullName;
    String email = vo.email;

    if (username == null || username.isBlank()) {
      errorList.add(String.format(CommonConstants.TMPT_FIELD_IS_MANDATORY, "Username"));
    }

    if (password == null || password.isBlank()) {
      errorList.add(String.format(CommonConstants.TMPT_FIELD_IS_MANDATORY, "Password"));
    }

    if (fullName == null || fullName.isBlank()) {
      errorList.add(String.format(CommonConstants.TMPT_FIELD_IS_MANDATORY, "Full Name"));
    }

    if (email == null || email.isBlank()) {
      errorList.add(String.format(CommonConstants.TMPT_FIELD_IS_MANDATORY, "Email"));
    }

    return errorList;
  }

  private List<String> validateUpdate(UserVO vo, List<String> errorList) {
    String id = vo.id;
    String fullName = vo.fullName;
    String email = vo.email;
    Long version = vo.version;

    if (id == null || id.isBlank()) {
      errorList.add(String.format(CommonConstants.TMPT_FIELD_IS_MANDATORY, "Id"));
    }

    if (fullName == null || fullName.isBlank()) {
      errorList.add(String.format(CommonConstants.TMPT_FIELD_IS_MANDATORY, "Full Name"));
    }

    if (email == null || email.isBlank()) {
      errorList.add(String.format(CommonConstants.TMPT_FIELD_IS_MANDATORY, "Email"));
    }

    if (version == null) {
      errorList.add(String.format(CommonConstants.TMPT_FIELD_IS_MANDATORY, "Version"));
    }

    return errorList;
  }

  private List<String> validateDelete(UserVO vo, List<String> errorList) {
    String id = vo.id;
    Long version = vo.version;

    if (id == null || id.isBlank()) {
      errorList.add(String.format(CommonConstants.TMPT_FIELD_IS_MANDATORY, "Id"));
    }

    if (version == null) {
      errorList.add(String.format(CommonConstants.TMPT_FIELD_IS_MANDATORY, "Version"));
    }

    return errorList;
  }
}
