package com.anasdidi.security.common;

import java.util.HashMap;
import java.util.Map;

public class CommonConstants {

  public static final Map<String, String> HEADERS;

  static {
    HEADERS = new HashMap<>();
    HEADERS.put("Content-Type", "application/json");
    HEADERS.put("Cache-Control", "no-store, no-cache");
    HEADERS.put("X-Content-Type-Options", "nosniff");
    HEADERS.put("X-XSS-Protection", "1; mode=block");
    HEADERS.put("X-Frame-Options", "deny");
  }

  public static final String CONTEXT_PATH = "/security";

  public static final int STATUS_CODE_OK = 200;
  public static final int STATUS_CODE_CREATED = 201;
  public static final int STATUS_CODE_BAD_REQUEST = 400;
  public static final int STATUS_CODE_FORBIDDEN = 403;

  public static final String MSG_ERR_REQUEST_FAILED = "Request failed!";
  public static final String MSG_ERR_REQUEST_BODY_EMPTY = "Request body is empty!";
  public static final String MSG_ERR_VALIDATE_ERROR = "Validation error!";
  public static final String MSG_ERR_NOT_AUTHZ = "You are not authorized for this request!";
  public static final String MSG_ERR_INSUFF_PERMISSION = "Insufficient permission!";

  public static final String MSG_OK_RECORD_CREATED = "Record successfully created.";
  public static final String MSG_OK_RECORD_UPDATE = "Record successfully updated.";
  public static final String MSG_OK_RECORD_DELETE = "User successfully deleted.";
  public static final String MSG_OK_USER_VALIDATE = "User successfully validated.";
  public static final String MSG_OK_USER_LOGOUT = "User successfully logout.";

  public static final String TMPT_FIELD_IS_MANDATORY = "%s field is mandatory!";

  public static final String EVT_USER_GET_BY_USERNAME = "user-get-by-username";
  public static final String EVT_USER_GET_LIST = "user-get-list";
  public static final String EVT_USER_GET_BY_ID = "user-get-by-id";

  public static final String PERMISSION_CLAIM_KEY = "permissions";
  public static final String PERMISSION_USER_WRITE = "user:write";
}
