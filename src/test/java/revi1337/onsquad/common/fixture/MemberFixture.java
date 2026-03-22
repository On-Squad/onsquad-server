package revi1337.onsquad.common.fixture;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.test.util.ReflectionTestUtils;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.vo.Introduce;
import revi1337.onsquad.member.domain.vo.Mbti;
import revi1337.onsquad.member.domain.vo.Nickname;
import revi1337.onsquad.member.domain.vo.PasswordPolicy;

public class MemberFixture {

    public static final String REVI_EMAIL_VALUE = "david122123@gmail.com";
    public static final String REVI_NICKNAME_VALUE = "revi1337";
    public static final String REVI_ADDRESS_VALUE = "REVI 주소";
    public static final String REVI_ADDRESS_DETAIL_VALUE = "REVI 상세 주소";
    public static final String REVI_PASSWORD_VALUE = "12345!@asa";
    public static final String REVI_ENCRYPTED_PASSWORD_VALUE = "{bcrypt}$2a$10$9qzyli03H7Z2dsPn3e56Su5CoOolFJApvRS86AA7d8cSvuBwnqLG.";
    public static final String REVI_INTRODUCE_VALUE = "Hello World REVI";
    public static final String REVI_PROFILE_IMAGE_LINK = "https://d3jao8gvkosd1k.cloudfront.net/onsquad/default/member-default.svg";
    public static final String REVI_OAUTH_PROFILE_IMAGE_LINK = "https://oauth2/onsquad/default/member-default.svg";
    public static final String REVI_KAKAO_LINK = "https://revi.kakao.com";
    public static final String REVI_MBTI_VALUE = Mbti.ISTP.name();

    public static final String ANDONG_EMAIL_VALUE = "ax34554@gmail.com";
    public static final String ANDONG_NICKNAME_VALUE = "andong";
    public static final String ANDONG_ADDRESS_VALUE = "ANDONG 주소";
    public static final String ANDONG_ADDRESS_DETAIL_VALUE = "ANDONG 상세 주소";
    public static final String ANDONG_PASSWORD_VALUE = "12345!@asb";
    public static final String ANDONG_ENCRYPTED_PASSWORD_VALUE = "{bcrypt}$2a$10$ocuCgB1yFSe34jsD20MC4eWiXIoUqtUaRpw2DaAbLMrJpDGp2fSYW";
    public static final String ANDONG_INTRODUCE_VALUE = "Hello World ANDONG";
    public static final String ANDONG_PROFILE_IMAGE_LINK = "https://d3jao8gvkosd1k.cloudfront.net/onsquad/default/member-default.svg";
    public static final String ANDONG_KAKAO_LINK = "https://andong.kakao.com";
    public static final String ANDONG_MBTI_VALUE = Mbti.ISFP.name();

    public static final String KWANGWON_EMAIL_VALUE = "kakd1313@gmail.com";
    public static final String KWANGWON_NICKNAME_VALUE = "kwon";
    public static final String KWANGWON_ADDRESS_VALUE = "KWANGWON 주소";
    public static final String KWANGWON_ADDRESS_DETAIL_VALUE = "KWANGWON 상세 주소";
    public static final String KWANGWON_PASSWORD_VALUE = "12345!@asc";
    public static final String KWANGWON_ENCRYPTED_PASSWORD_VALUE = "{bcrypt}$2a$10$NaBOLRjMFC0BHl7AnnXrV.srIYwV9uTwiStOHdr15P4mR6vEVp2Z2";
    public static final String KWANGWON_INTRODUCE_VALUE = "Hello World KWANGWON";
    public static final String KWANGWON_PROFILE_IMAGE_LINK = "https://d3jao8gvkosd1k.cloudfront.net/onsquad/default/member-default.svg";
    public static final String KWANGWON_KAKAO_LINK = "https://kwangwon.kakao.com";
    public static final String KWANGWON_MBTI_VALUE = Mbti.ENFJ.name();

    public static final String EMAIL_VALUE = "member@gmail.com";
    public static final String NICKNAME_VALUE = "nick";
    public static final String ADDRESS_VALUE = "공백";
    public static final String ADDRESS_DETAIL_VALUE = "공백";
    public static final String PASSWORD_VALUE = "12345!@asa";
    public static final String ENCRYPTED_PASSWORD_VALUE = "{bcrypt}$2a$10$9qzyli03H7Z2dsPn3e56Su5CoOolFJApvRS86AA7d8cSvuBwnqLG.";
    public static final String INTRODUCE_VALUE = "소개 없음";
    public static final String PROFILE_IMAGE_LINK = "https://d3jao8gvkosd1k.cloudfront.net/onsquad/default/member-default.svg";
    public static final String KAKAO_LINK = "https://dummy.kakao.com";

    public static Member createRevi() {
        Member revi = Member.general(
                REVI_EMAIL_VALUE,
                REVI_PASSWORD_VALUE,
                REVI_NICKNAME_VALUE,
                REVI_ADDRESS_VALUE,
                REVI_ADDRESS_DETAIL_VALUE
        );

        revi.updatePassword(REVI_ENCRYPTED_PASSWORD_VALUE, PasswordPolicy.BCRYPT);

        ReflectionTestUtils.setField(revi, "introduce", new Introduce(REVI_INTRODUCE_VALUE));
        ReflectionTestUtils.setField(revi, "profileImage", REVI_PROFILE_IMAGE_LINK);
        ReflectionTestUtils.setField(revi, "kakaoLink", REVI_KAKAO_LINK);
        ReflectionTestUtils.setField(revi, "mbti", Mbti.parse(REVI_MBTI_VALUE));

        return revi;
    }

    public static Member createAndong() {
        Member andong = Member.general(
                ANDONG_EMAIL_VALUE,
                ANDONG_PASSWORD_VALUE,
                ANDONG_NICKNAME_VALUE,
                ANDONG_ADDRESS_VALUE,
                ANDONG_ADDRESS_DETAIL_VALUE
        );

        andong.updatePassword(ANDONG_ENCRYPTED_PASSWORD_VALUE, PasswordPolicy.BCRYPT);

        ReflectionTestUtils.setField(andong, "introduce", new Introduce(ANDONG_INTRODUCE_VALUE));
        ReflectionTestUtils.setField(andong, "profileImage", ANDONG_PROFILE_IMAGE_LINK);
        ReflectionTestUtils.setField(andong, "kakaoLink", ANDONG_KAKAO_LINK);
        ReflectionTestUtils.setField(andong, "mbti", Mbti.parse(ANDONG_MBTI_VALUE));

        return andong;
    }

    public static Member createKwangwon() {
        Member kwangwon = Member.general(
                KWANGWON_EMAIL_VALUE,
                KWANGWON_PASSWORD_VALUE,
                KWANGWON_NICKNAME_VALUE,
                KWANGWON_ADDRESS_VALUE,
                KWANGWON_ADDRESS_DETAIL_VALUE
        );

        kwangwon.updatePassword(KWANGWON_ENCRYPTED_PASSWORD_VALUE, PasswordPolicy.BCRYPT);

        ReflectionTestUtils.setField(kwangwon, "introduce", new Introduce(KWANGWON_INTRODUCE_VALUE));
        ReflectionTestUtils.setField(kwangwon, "profileImage", KWANGWON_PROFILE_IMAGE_LINK);
        ReflectionTestUtils.setField(kwangwon, "kakaoLink", KWANGWON_KAKAO_LINK);
        ReflectionTestUtils.setField(kwangwon, "mbti", Mbti.parse(KWANGWON_MBTI_VALUE));

        return kwangwon;
    }

    public static Member createMember(int sequence) {
        Member member = Member.general(
                String.format("test-%d@email.com", sequence),
                PASSWORD_VALUE,
                NICKNAME_VALUE + sequence,
                ADDRESS_VALUE,
                ADDRESS_DETAIL_VALUE
        );

        member.updatePassword(ENCRYPTED_PASSWORD_VALUE, PasswordPolicy.BCRYPT);

        ReflectionTestUtils.setField(member, "introduce", new Introduce(INTRODUCE_VALUE));
        ReflectionTestUtils.setField(member, "profileImage", PROFILE_IMAGE_LINK);
        ReflectionTestUtils.setField(member, "kakaoLink", KAKAO_LINK);
        ReflectionTestUtils.setField(member, "mbti", peekRandomMbti());

        return member;
    }

    public static Member createMember(String nickname) {
        int randomSequence = (int) ThreadLocalRandom.current().nextLong(1, 100_000_001);
        Member member = Member.general(
                String.format("test-%d@email.com", randomSequence),
                PASSWORD_VALUE,
                nickname,
                ADDRESS_VALUE,
                ADDRESS_DETAIL_VALUE
        );

        member.updatePassword(ENCRYPTED_PASSWORD_VALUE, PasswordPolicy.BCRYPT);

        ReflectionTestUtils.setField(member, "introduce", new Introduce(INTRODUCE_VALUE));
        ReflectionTestUtils.setField(member, "profileImage", PROFILE_IMAGE_LINK);
        ReflectionTestUtils.setField(member, "kakaoLink", KAKAO_LINK);
        ReflectionTestUtils.setField(member, "mbti", peekRandomMbti());

        return member;
    }

    public static Member createRevi(Long id) {
        Member revi = createRevi();
        ReflectionTestUtils.setField(revi, "id", id);
        return revi;
    }

    public static Member createAndong(Long id) {
        Member andong = createAndong();
        ReflectionTestUtils.setField(andong, "id", id);
        return andong;
    }

    public static Member createKwangwon(Long id) {
        Member kwangwon = createKwangwon();
        ReflectionTestUtils.setField(kwangwon, "id", id);
        return kwangwon;
    }

    public static Member createMember(Long id) {
        Member member = createMember(id.intValue());
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    public static Member createMember(Long id, String nickname) {
        Member member = createMember(id.intValue());
        ReflectionTestUtils.setField(member, "id", id);
        ReflectionTestUtils.setField(member, "nickname", new Nickname(nickname));
        return member;
    }

    private static Mbti peekRandomMbti() {
        List<Mbti> mbtiList = Arrays.asList(Mbti.values());
        Collections.shuffle(mbtiList);
        return mbtiList.get(0);
    }
}
