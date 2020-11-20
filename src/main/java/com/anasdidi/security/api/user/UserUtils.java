package com.anasdidi.security.api.user;

import org.mindrot.jbcrypt.BCrypt;

class UserUtils {

  static String encryptPassword(String password) {
    String encryptedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
    return encryptedPassword;
  }
}
