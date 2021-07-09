package com.anasdidi.security.domain.auth;

import java.util.ArrayList;
import java.util.List;
import com.anasdidi.security.common.BaseValidator;

class AuthValidator extends BaseValidator<AuthVO> {

  @Override
  protected List<String> validateLogin(AuthVO vo) {
    List<String> errorList = new ArrayList<>();

    isMandatory(errorList, vo.username, "Username");
    isMandatory(errorList, vo.password, "Password");

    return errorList;
  }

  @Override
  protected List<String> validateCreate(AuthVO vo) {
    return null;
  }

  @Override
  protected List<String> validateDelete(AuthVO vo) {
    return null;
  }

  @Override
  protected List<String> validateUpdate(AuthVO vo) {
    return null;
  }
}
