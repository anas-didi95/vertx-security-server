package com.anasdidi.security.api.jwt;

final class JwtConstants {

  static final String REQUEST_URI = "/api/jwt";

  static final String COLLECTION_NAME = "jwts";

  static final String MSG_ERR_INVALID_CREDENTIAL = "Invalid credential!";
  static final String MSG_ERR_INVALID_USERNAME_PASSWORD = "Invalid username/password!";
  static final String MSG_ERR_REFRESH_TOKEN_NOT_FOUND = "Refresh token not found!";
  static final String MSG_ERR_REFRESH_TOKEN_FAILED = "Refresh token failed!";
  static final String MSG_ERR_REFRESH_TOKEN_INVALID = "Refresh token is invalid!";
  static final String MSG_ERR_REFRESH_TOKEN_CREDENTIAL_MISMATCH =
      "Refresh token credential mismatch!";
  static final String MSG_ERR_REFRESH_TOKEN_EMPTY = "Refresh token is empty!";

  static final String MSG_OK_TOKEN_REFRESHED = "Token refreshed.";

  static final String REFRESH_TOKEN_DELIMITER = ":";
}
