package edu.tlu.klgd.infracstructure.security;

public final class JwtConstant {
    private JwtConstant() {
    }

    public static final String HEADER_ALGORITHM_KEY = "alg";
    public static final String HEADER_TYPE_KEY = "typ";
    public static final String HMAC_SHA256_ALGORITHM = "HS256";
    public static final String HMAC_SHA256_JCA_NAME = "HmacSHA256";
    public static final String JWT_TYPE = "JWT";
    public static final String SUBJECT_CLAIM = "sub";
    public static final String USER_ID_CLAIM = "uid";
    public static final String NAME_CLAIM = "name";
    public static final String ROLES_CLAIM = "roles";
    public static final String ISSUED_AT_CLAIM = "iat";
    public static final String EXPIRES_AT_CLAIM = "exp";
    public static final String JWT_PART_SEPARATOR_REGEX = "\\.";
    public static final String JWT_PART_SEPARATOR = ".";
    public static final int JWT_PART_COUNT = 3;
    public static final int JWT_HEADER_INDEX = 0;
    public static final int JWT_PAYLOAD_INDEX = 1;
    public static final int JWT_SIGNATURE_INDEX = 2;
    public static final long SECONDS_PER_MINUTE = 60;
}
