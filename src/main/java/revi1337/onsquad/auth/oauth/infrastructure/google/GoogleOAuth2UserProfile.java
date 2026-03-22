package revi1337.onsquad.auth.oauth.infrastructure.google;

import revi1337.onsquad.auth.oauth.application.contract.OAuth2VendorUserProfile;
import revi1337.onsquad.member.domain.vo.UserType;

public record GoogleOAuth2UserProfile(
        String name,
        String nickname,
        String email,
        boolean isEmailVerified,
        String profileImage,
        String thumbnailImage
) implements OAuth2VendorUserProfile {

    public static GoogleOAuth2UserProfile from(GoogleUserInfoResponse response) {
        return new GoogleOAuth2UserProfile(
                response.name(),
                response.name(),
                response.email(),
                response.verifiedEmail(),
                response.picture(),
                response.picture()
        );
    }

    @Override
    public UserType getUserType() {
        return UserType.GOOGLE;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getNickname() {
        return nickname;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getProfileImage() {
        return profileImage;
    }

    @Override
    public String getThumbnailImage() {
        return thumbnailImage;
    }
}
