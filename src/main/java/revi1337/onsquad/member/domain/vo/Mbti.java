package revi1337.onsquad.member.domain.vo;

import static revi1337.onsquad.member.domain.error.MemberErrorCode.INVALID_MBTI;

import revi1337.onsquad.member.domain.error.MemberDomainException;

public enum Mbti {

    ISTJ,
    ISFJ,
    INFJ,
    INTJ,
    ISTP,
    ISFP,
    INFP,
    INTP,
    ESTP,
    ESFP,
    ENFP,
    ENTP,
    ESTJ,
    ESFJ,
    ENFJ,
    ENTJ;

    public static Mbti parse(String value) {
        try {
            return Mbti.valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new MemberDomainException.InvalidMbti(INVALID_MBTI);
        }
    }

    public static String getOrDefault(String mbti) {
        try {
            return parse(mbti).name();
        } catch (MemberDomainException.InvalidMbti e) {
            return "";
        }
    }

    public static String getOrDefault(Mbti mbti) {
        if (mbti == null) {
            return "";
        }
        return mbti.name();
    }
}
