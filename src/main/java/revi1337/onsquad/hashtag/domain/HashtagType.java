package revi1337.onsquad.hashtag.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import revi1337.onsquad.hashtag.domain.error.HashtagDomainException;
import revi1337.onsquad.hashtag.domain.error.HashtagErrorCode;

@Getter
@RequiredArgsConstructor
public enum HashtagType {

    ACTIVE("활발한", 1L),
    TRENDY("트랜디한", 2L),
    INTROVERTED("내향적인", 3L),
    EXTROVERTED("외향적인", 4L),
    QUIET("조용한", 5L),
    GAME_LOVER_MALE("겜돌이", 6L),
    GAME_LOVER_FEMALE("겜순이", 7L),
    HOME_BODY_MALE("집돌이", 8L),
    HOME_BODY_FEMALE("집순이", 9L),
    DRINK_LOVER("술꾼러버", 10L),
    THRILLING("짜릿한", 11L),
    ADVENTUROUS("모험적인", 12L),
    CREATIVE("창의적인", 13L),
    PASSIONATE("열정적인", 14L),
    CHALLENGING("도전적인", 15L),
    POSITIVE("긍정적인", 16L),
    OPTIMISTIC("낙천적인", 17L),
    SOCIAL("사교적인", 18L),
    FRIENDLY("친화적인", 19L),
    CHEERFUL("유쾌한", 20L),
    WITTY("재치있는", 21L),
    LIVELY("활기찬", 22L),
    OPEN_MINDED("열린마음의", 23L),
    FREE_SPIRITED("자유로운", 24L),
    INDEPENDENT("독립적인", 25L),
    TRAVEL("여행", 26L),
    IMPULSIVE("즉흥적인", 27L),
    MOVIE("영화", 28L),
    REFRESHING("리프레시", 29L),
    ESCAPE("일탈", 30L),
    READING("독서", 31L),
    WALKING("산책", 32L),
    DOG_MEETUP("애견모임", 33L),
    MUKBANG("먹방", 34L),
    FOODIE("맛집탐방", 35L),
    STUDY_TOGETHER("모각공", 36L),
    CODING_TOGETHER("모각코", 37L),
    CAFE_LOVER("카페러버", 38L),
    MOODY("분위기있는", 39L),
    WINE("와인", 40L);

    private final String text;
    private final Long pk;

    private static final Map<String, HashtagType> hashtagTypeStorage = Stream.of(values()).collect(Collectors.toUnmodifiableMap(
            h -> h.getText().toUpperCase(),
            h -> h
    ));

    public static List<HashtagType> unmodifiableList() {
        return List.of(HashtagType.values());
    }

    public static List<HashtagType> fromTexts(List<String> hashtagTexts) {
        return new HashSet<>(hashtagTexts).stream()
                .map(HashtagType::fromText)
                .toList();
    }

    public static HashtagType fromText(String hashtagText) {
        HashtagType hashtagType = hashtagTypeStorage.get(hashtagText.toUpperCase());
        if (hashtagType == null) {
            throw new HashtagDomainException.InvalidHashtag(HashtagErrorCode.INVALID_HASHTAG);
        }
        return hashtagType;
    }
}
