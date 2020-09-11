package com.anasdidi.security.api.jwt;

import java.util.ArrayList;
import java.util.List;

import com.anasdidi.security.common.ApplicationException;
import com.anasdidi.security.common.CommonConstants;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonArray;

class JwtValidator {

  private final Logger logger = LogManager.getLogger(JwtValidator.class);

  enum Validate {
    LOGIN, REFRESH
  }

  void validate(String requestId, Validate val, JwtVO vo) throws ApplicationException {
    String tag = "validate";
    List<String> errorList = new ArrayList<>();

    switch (val) {
      case LOGIN:
        errorList = validateLogin(vo, errorList);
        break;
      case REFRESH:
        errorList = validateRefresh(vo, errorList);
        break;
    }

    if (!errorList.isEmpty()) {
      logger.error("[{}:{}] {} validate={}\n{}", tag, requestId, CommonConstants.MSG_ERR_VALIDATE_ERROR, val,
          vo.toString());
      throw new ApplicationException(CommonConstants.MSG_ERR_VALIDATE_ERROR, requestId, new JsonArray(errorList));
    }
  }

  private List<String> validateLogin(JwtVO vo, List<String> errorList) {
    String username = vo.username;
    String password = vo.password;

    if (username == null || username.isBlank()) {
      errorList.add(String.format(CommonConstants.TMPT_FIELD_IS_MANDATORY, "Username"));
    }

    if (password == null || password.isBlank()) {
      errorList.add(String.format(CommonConstants.TMPT_FIELD_IS_MANDATORY, "Password"));
    }

    return errorList;
  }

  private List<String> validateRefresh(JwtVO vo, List<String> errorList) {
    String id = vo.id;

    if (id == null || id.isBlank()) {
      errorList.add(String.format(CommonConstants.TMPT_FIELD_IS_MANDATORY, "Id"));
    }

    return errorList;
  }
}
