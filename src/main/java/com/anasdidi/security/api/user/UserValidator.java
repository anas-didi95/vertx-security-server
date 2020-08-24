package com.anasdidi.security.api.user;

import java.util.ArrayList;
import java.util.List;

class UserValidator {

  enum Validate {
    CREATE
  }

  void validate(Validate val, UserVO vo) throws Exception {
    List<String> errorList = new ArrayList<>();

    switch (val) {
      case CREATE:
        errorList = validateCreate(vo, errorList);
        break;
    }

    if (!errorList.isEmpty()) {
      throw new Exception("Validation error!");
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
