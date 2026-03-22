package revi1337.onsquad.auth.oauth.application.contract;

import revi1337.onsquad.member.domain.vo.UserType;

public interface OAuth2VendorUserProfile {

    UserType getUserType();

    String getName();

    String getNickname();

    String getEmail();

    boolean isEmailVerified();

    String getProfileImage();

    String getThumbnailImage();

}
