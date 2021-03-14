package com.anasdidi.security.common;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import graphql.execution.ExecutionId;
import io.reactivex.Single;
import io.vertx.reactivex.ext.auth.User;

public class CommonUtils {

  public static String generateUUID() {
    return generateUUID(null);
  }

  public static String generateUUID(ExecutionId executionId) {
    String uuid = (executionId != null ? executionId.toString() : UUID.randomUUID().toString());
    return uuid.replace("-", "").toUpperCase();
  }

  public static String getFormattedDateString(Instant instant, String format) {
    Date date = Date.from(instant);
    SimpleDateFormat sdf = new SimpleDateFormat(format);

    return sdf.format(date);
  }

  public static String getUserIdFromToken(User user) {
    return user.principal().getString(CommonConstants.JWT_CLAIM_KEY_USERID);
  }

  @SuppressWarnings({"deprecation"})
  public static Single<User> isAuthorized(User user, String authority, String requestId) {
    return user.rxIsAuthorised(authority).map(isAuthorized -> {
      if (!isAuthorized) {
        throw new ApplicationException(CommonConstants.MSG_ERR_NOT_AUTHZ, requestId,
            CommonConstants.MSG_ERR_INSUFF_PERMISSION, CommonConstants.STATUS_CODE_FORBIDDEN);
      }

      return user;
    });
  }
}
