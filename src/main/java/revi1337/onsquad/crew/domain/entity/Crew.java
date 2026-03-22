package revi1337.onsquad.crew.domain.entity;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import revi1337.onsquad.common.domain.BaseEntity;
import revi1337.onsquad.crew.domain.vo.Detail;
import revi1337.onsquad.crew.domain.vo.Introduce;
import revi1337.onsquad.crew.domain.vo.Name;
import revi1337.onsquad.crew.domain.model.CrewCreateSpec;
import revi1337.onsquad.crew_hashtag.domain.entity.CrewHashtag;
import revi1337.onsquad.crew_member.domain.entity.CrewMember;
import revi1337.onsquad.crew_member.domain.entity.CrewMemberFactory;
import revi1337.onsquad.member.domain.entity.Member;

@Getter
@NoArgsConstructor(access = PROTECTED)
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(name = "uk_crew_name", columnNames = "name")})
public class Crew extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Embedded
    private Name name;

    @Embedded
    private Introduce introduce;

    @Embedded
    private Detail detail;

    private long currentSize;

    private String kakaoLink;

    private String imageUrl;

    @Version
    private Long version;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToMany(mappedBy = "crew")
    private final List<CrewHashtag> hashtags = new ArrayList<>();

    @OneToMany(mappedBy = "crew", cascade = {PERSIST, MERGE})
    private final List<CrewMember> crewMembers = new ArrayList<>();

    public static Crew create(CrewCreateSpec spec, LocalDateTime ownerParticipateAt) {
        Objects.requireNonNull(spec, "CrewCreateSpec must not be null");
        Objects.requireNonNull(ownerParticipateAt, "ownerParticipateAt must not be null");

        Crew crew = new Crew(
                spec.getName(),
                spec.getIntroduce(),
                spec.getDetail(),
                spec.getKakaoLink(),
                spec.getImageUrl()
        );

        crew.updateOwner(spec.getOwner());
        crew.addCrewMember(CrewMemberFactory.owner(crew, spec.getOwner(), ownerParticipateAt));
        return crew;
    }

    public static List<Long> extractIds(List<Crew> crews) {
        return crews.stream()
                .map(Crew::getId)
                .toList();
    }

    public static List<String> extractImageUrls(List<Crew> crews) {
        return crews.stream()
                .filter(Crew::hasImage)
                .map(Crew::getImageUrl)
                .toList();
    }

    private Crew(String name, String introduce, String detail, String kakaoLink, String imageUrl) {
        this.name = new Name(name);
        this.introduce = new Introduce(introduce);
        this.detail = new Detail(detail);
        this.kakaoLink = kakaoLink;
        this.imageUrl = imageUrl;
    }

    public void addCrewMember(CrewMember... crewMembers) {
        for (CrewMember crewMember : crewMembers) {
            increaseSize();
            crewMember.addCrew(this);
            this.crewMembers.add(crewMember);
        }
    }

    public void delegateOwner(CrewMember currentOwner, CrewMember nextOwner) {
        Objects.requireNonNull(currentOwner, "currentOwner must not be null");
        Objects.requireNonNull(nextOwner, "nextOwner must not be null");
        if (currentOwner.isOwner()) {
            nextOwner.promoteToOwner();
            currentOwner.demoteToGeneral();
            updateOwner(nextOwner.getMember());
        }
    }

    public void update(String name, String introduce, String detail, String kakaoLink) {
        this.name = new Name(name);
        this.introduce = new Introduce(introduce);
        this.detail = new Detail(detail);
        this.kakaoLink = kakaoLink;
    }

    public void increaseSize() {
        this.currentSize = this.currentSize + 1;
    }

    public void decreaseSize() {
        this.currentSize = this.currentSize - 1;
    }

    private void updateOwner(Member member) {
        this.member = member;
    }

    public void updateImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            this.imageUrl = imageUrl;
        }
    }

    public void deleteImage() {
        if (hasImage()) {
            this.imageUrl = null;
        }
    }

    public boolean hasImage() {
        return !hasNotImage();
    }

    public boolean hasNotImage() {
        return imageUrl == null || imageUrl.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Crew crew)) {
            return false;
        }
        return id != null && Objects.equals(getId(), crew.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
