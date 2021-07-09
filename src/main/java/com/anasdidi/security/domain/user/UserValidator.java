package com.anasdidi.security.domain.user;

import java.util.ArrayList;
import java.util.List;
import com.anasdidi.security.common.BaseValidator;

class UserValidator extends BaseValidator<UserVO> {

  @Override
  protected List<String> validateCreate(UserVO vo) {
    List<String> errorList = new ArrayList<>();

    isMandatory(errorList, vo.username, "Username");
    isMandatory(errorList, vo.password, "Password");
    isMandatory(errorList, vo.fullName, "Full Name");
    isMandatory(errorList, vo.email, "Email");

    return errorList;
  }

  @Override
  protected List<String> validateUpdate(UserVO vo) {
    List<String> errorList = new ArrayList<>();

    isMandatory(errorList, vo.fullName, "Full Name");
    isMandatory(errorList, vo.email, "Email");
    isMandatory(errorList, vo.version, "Version");

    return errorList;
  }

  @Override
  protected List<String> validateDelete(UserVO vo) {
    List<String> errorList = new ArrayList<>();

    isMandatory(errorList, vo.version, "Version");

    return errorList;
  }
}
