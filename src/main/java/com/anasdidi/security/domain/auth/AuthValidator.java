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
  protected List<String> validateCheck(AuthVO vo) {
    List<String> errorList = new ArrayList<>();

    isMandatory(errorList, vo.subject, "User Id", "%s not defined in token!");
    isMandatory(errorList, vo.hasPermissionsKey, "Permissions", "%s not defined in token!");

    return errorList;
  }

  @Override
  protected List<String> validateRefresh(AuthVO vo) {
    List<String> errorList = new ArrayList<>();

    isMandatory(errorList, vo.subject, "Token Id", "%s not defined in token!");

    return errorList;
  }

  @Override
  protected List<String> validateLogout(AuthVO vo) {
    return super.validateLogout(vo);
  }
}
