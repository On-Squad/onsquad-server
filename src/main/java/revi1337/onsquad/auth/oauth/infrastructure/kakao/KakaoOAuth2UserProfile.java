package revi1337.onsquad.auth.oauth.infrastructure.kakao;

import revi1337.onsquad.auth.oauth.application.contract.OAuth2VendorUserProfile;
import revi1337.onsquad.member.domain.vo.UserType;

public record KakaoOAuth2UserProfile(
        String name,
        String nickname,
        String email,
        boolean isEmailVerified,
        String profileImage,
        String thumbnailImage
) implements OAuth2VendorUserProfile {

    public static KakaoOAuth2UserProfile from(KakaoUserInfoResponse response) {
        return new KakaoOAuth2UserProfile(
                response.kakaoAccount().profile().nickname(),
                response.kakaoAccount().profile().nickname(),
                response.kakaoAccount().email(),
                response.kakaoAccount().isEmailVerified(),
                response.kakaoAccount().profile().profileImageUrl(),
                response.kakaoAccount().profile().thumbnailImageUrl()
        );
    }

    @Override
    public UserType getUserType() {
        return UserType.KAKAO;
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
