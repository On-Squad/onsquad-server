package revi1337.onsquad.auth.token.domain.model;

import io.jsonwebtoken.Claims;
import revi1337.onsquad.member.domain.vo.UserType;

public class ClaimsParser {

    public static final String ACCESS_TOKEN_SUBJECT = "access_token";
    public static final String REFRESH_TOKEN_SUBJECT = "refresh_token";
    public static final String IDENTITY_CLAIM = "identity";
    public static final String EMAIL_CLAIM = "email";
    public static final String USERTYPE_CLAIM = "type";

    private final Claims claims;

    public ClaimsParser(Claims claims) {
        this.claims = claims;
    }

    public Long parseIdentity() {
        return claims.get(IDENTITY_CLAIM, Long.class);
    }

    public String parseEmail() {
        return claims.get(EMAIL_CLAIM, String.class);
    }

    public UserType parseUserType() {
        return UserType.valueOf(claims.get(USERTYPE_CLAIM, String.class));
    }
}
