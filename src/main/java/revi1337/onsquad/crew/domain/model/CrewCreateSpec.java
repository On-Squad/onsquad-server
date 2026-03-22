package revi1337.onsquad.crew.domain.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import revi1337.onsquad.hashtag.domain.HashtagType;
import revi1337.onsquad.member.domain.entity.Member;

@Getter
public class CrewCreateSpec {

    private final Member owner;
    private final String name;
    private final String introduce;
    private final String detail;
    private final List<HashtagType> hashtags;
    private final String kakaoLink;
    private final String imageUrl;
    
    public CrewCreateSpec(Member owner, String name, String introduce, String detail, String kakaoLink, String imageUrl) {
        this(owner, name, introduce, detail, new ArrayList<>(), kakaoLink, imageUrl);
    }

    public CrewCreateSpec(Member owner, String name, String introduce, String detail, List<HashtagType> hashtags, String kakaoLink, String imageUrl) {
        this.owner = owner;
        this.name = name;
        this.introduce = introduce;
        this.detail = detail;
        this.hashtags = hashtags;
        this.kakaoLink = kakaoLink;
        this.imageUrl = imageUrl;
    }
}
