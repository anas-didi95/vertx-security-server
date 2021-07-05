package com.anasdidi.security.domain.auth;

import com.anasdidi.security.common.BaseService;
import org.mindrot.jbcrypt.BCrypt;
import io.reactivex.rxjava3.core.Single;

class AuthService extends BaseService {

  Single<String> login(AuthVO vo) {
    if (vo.username.equals("admin")
        && BCrypt.checkpw(vo.password, BCrypt.hashpw("password", BCrypt.gensalt()))) {
      return Single.just("helloworld");
    }
    return Single.error(new Exception("Login failed"));
  }
}
