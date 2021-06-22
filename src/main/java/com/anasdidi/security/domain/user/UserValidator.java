package com.anasdidi.security.domain.user;

import java.util.ArrayList;
import java.util.List;
import com.anasdidi.security.common.ApplicationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class UserValidator {

  private static Logger logger = LogManager.getLogger(UserValidator.class);

  enum Action {
    CREATE
  }

  UserVO validate(UserVO vo, Action type) throws ApplicationException {
    List<String> errorList = null;

    switch (type) {
      case CREATE:
        errorList = validateCreate(vo);
        break;
    }

    if (errorList == null) {
      logger.error("[validate] Validation not implemented!");
    } else if (!errorList.isEmpty()) {
      logger.error("[validate] Validation error! type={}, vo={}", type, vo);
      throw new ApplicationException("E002", "Validation error!", errorList);
    }

    return vo;
  }

  private List<String> validateCreate(UserVO vo) {
    List<String> errorList = new ArrayList<>();

    isMandatory(errorList, vo.username, "Username");
    isMandatory(errorList, vo.password, "Password");
    isMandatory(errorList, vo.fullName, "Full Name");
    isMandatory(errorList, vo.email, "Email");

    return errorList;
  }

  private void isMandatory(List<String> errorList, String value, String fieldName) {
    if (value == null || value.isBlank()) {
      errorList.add(String.format("%s is mandatory field!", fieldName));
    }
  }
}
