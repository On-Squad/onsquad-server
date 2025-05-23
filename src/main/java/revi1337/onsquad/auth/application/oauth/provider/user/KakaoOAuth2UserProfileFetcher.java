package revi1337.onsquad.auth.application.oauth.provider.user;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import revi1337.onsquad.auth.application.oauth.model.KakaoUserProfile;
import revi1337.onsquad.auth.application.oauth.model.PlatformUserProfile;
import revi1337.onsquad.auth.application.token.model.AccessToken;
import revi1337.onsquad.auth.config.properties.OAuth2ClientProperties.OAuth2Properties;
import revi1337.onsquad.auth.presentation.oauth.dto.response.KakaoUserInfoResponse;

public class KakaoOAuth2UserProfileFetcher implements PlatformOAuth2UserProfileFetcher {

    @Override
    public PlatformUserProfile fetch(AccessToken accessToken, OAuth2Properties oAuth2Properties) {
        KakaoUserInfoResponse kakaoUserInfoResponse = fetchUserInfoResponse(accessToken, oAuth2Properties);
        return KakaoUserProfile.from(kakaoUserInfoResponse);
    }

    private KakaoUserInfoResponse fetchUserInfoResponse(AccessToken accessToken,
                                                        OAuth2Properties oAuth2Properties) {
        MultiValueMap<String, String> userInfoHeaders = new LinkedMultiValueMap<>() {{
            add(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken.value()));
            add(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded;");
            add(HttpHeaders.CONTENT_TYPE, "charset=utf-8");
        }};
        ResponseEntity<KakaoUserInfoResponse> kakaoProfileResponse = new RestTemplate().exchange(
                oAuth2Properties.accountUri(),
                HttpMethod.GET,
                new HttpEntity<>(userInfoHeaders),
                KakaoUserInfoResponse.class
        );

        return kakaoProfileResponse.getBody();
    }
}
