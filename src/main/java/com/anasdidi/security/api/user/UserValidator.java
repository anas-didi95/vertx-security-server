package com.anasdidi.security.api.user;

import java.util.ArrayList;
import java.util.List;

import com.anasdidi.security.common.ApplicationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonArray;

class UserValidator {

  private final Logger logger = LogManager.getLogger(UserValidator.class);

  enum Validate {
    CREATE("CREATE");

    String value;

    Validate(String value) {
      this.value = value;
    }
  }

  void validate(String requestId, Validate val, UserVO vo) throws Exception {
    String tag = "validate";
    List<String> errorList = new ArrayList<>();

    switch (val) {
      case CREATE:
        errorList = validateCreate(vo, errorList);
        break;
    }

    if (!errorList.isEmpty()) {
      logger.error("[{}:{}] Validation error! validate={}\n{}", tag, requestId, val.value, vo.toString());
      throw new ApplicationException("Validation error!", requestId, new JsonArray(errorList));
    }
  }

  private List<String> validateCreate(UserVO vo, List<String> errorList) {
    String username = vo.username;
    String password = vo.password;
    String fullName = vo.fullName;
    String email = vo.email;

    if (username == null || username.isBlank()) {
      errorList.add("Username field is mandatory!");
    }

    if (password == null || password.isBlank()) {
      errorList.add("Password field is mandatory!");
    }

    if (fullName == null || fullName.isBlank()) {
      errorList.add("Full Name field is mandatory!");
    }

    if (email == null || email.isBlank()) {
      errorList.add("Email field is mandatory!");
    }

    return errorList;
  }
}
