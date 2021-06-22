package com.anasdidi.security.domain.user;

import java.util.ArrayList;
import java.util.List;
import com.anasdidi.security.common.ApplicationException;
import com.anasdidi.security.common.BaseValidator;

class UserValidator extends BaseValidator<UserVO> {

  @Override
  protected UserVO validate(UserVO vo, Action action) throws ApplicationException {
    List<String> errorList = null;

    switch (action) {
      case CREATE:
        errorList = validateCreate(vo);
        break;
    }

    return validate(errorList, vo, action);
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
