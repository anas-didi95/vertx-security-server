package com.anasdidi.security.domain.user;

import java.util.ArrayList;
import java.util.List;
import com.anasdidi.security.common.ApplicationException;
import com.anasdidi.security.common.BaseValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class UserValidator extends BaseValidator<UserVO> {

  private static Logger logger = LogManager.getLogger(UserValidator.class);

  @Override
  protected UserVO validate(UserVO vo, Action action) throws ApplicationException {
    List<String> errorList = null;

    switch (action) {
      case CREATE:
        errorList = validateCreate(vo);
        break;
    }

    if (errorList == null) {
      logger.error("[validate] Validation not implemented!");
    } else if (!errorList.isEmpty()) {
      logger.error("[validate] action={}, vo={}", action, vo);
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
}
