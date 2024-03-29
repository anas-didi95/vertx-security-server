package com.anasdidi.security.common;

public final class TestConstants {

  // { "sub": "SYSTEM", "iss": "anasdidi.dev", "pms": [ "security:user", "security:graphql" ],
  // "typ": "TOKEN_ACCESS" } = secret
  public static final String ACCESS_TOKEN =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJTWVNURU0iLCJpc3MiOiJhbmFzZGlkaS5kZXYiLCJwbXMiOlsic2VjdXJpdHk6dXNlciIsInNlY3VyaXR5OmdyYXBocWwiXSwidHlwIjoiVE9LRU5fQUNDRVNTIn0.wf4aY4LVqN7YeTR8dVN2GLSelW84WQwVJ1Hd6D2bYIY";

  // { "sub": "SYSTEM", "iss": "anasdidi.dev", "typ": "TOKEN_ACCESS" } = secret
  public static final String ACCESS_TOKEN_NO_PERMISSION =
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJTWVNURU0iLCJpc3MiOiJhbmFzZGlkaS5kZXYiLCJ0eXAiOiJUT0tFTl9BQ0NFU1MifQ.nh_hBp9L0WlRi2nseuDooDdSkqK4zLDY_ImrYJCvBng";

  // { "iss": "anasdidi.dev", "typ": "TOKEN_ACCESS" } = secret
  public static final String ACCESS_TOKEN_NO_CLAIMS =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhbmFzZGlkaS5kZXYiLCJ0eXAiOiJUT0tFTl9BQ0NFU1MifQ.AhovN0SfzkgBlzhNYb5BLZlyoSlwDWETt4BOw6Hrr50";

  // { "sub": "SYSTEM", "iss": "anasdidi.dev", "typ": "TOKEN_ACCESS" } = secret1
  public static final String ACCESS_TOKEN_INVALID_SIGNATURE =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJTWVNURU0iLCJpc3MiOiJhbmFzZGlkaS5kZXYiLCJ0eXAiOiJUT0tFTl9BQ0NFU1MifQ.bO2Ch7HjO2AfwBK6Pb-T1IVEIuivdOSuVuU21lhoHsU";

  // { "sub": "SYSTEM", "iss": "anasdidi.dev", "typ": "TOKEN_REFRESH" } = secret
  public static final String REFRESH_TOKEN =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJTWVNURU0iLCJpc3MiOiJhbmFzZGlkaS5kZXYiLCJ0eXAiOiJUT0tFTl9SRUZSRVNIIn0.Wie-HReiLjlUdwxIC0di2ACQFVOB_PmjPq52zOStRmY";
}
