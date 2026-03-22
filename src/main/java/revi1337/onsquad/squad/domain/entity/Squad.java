package revi1337.onsquad.squad.domain.entity;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;
import static revi1337.onsquad.squad.domain.error.SquadErrorCode.NOT_ENOUGH_LEFT;
import static revi1337.onsquad.squad.domain.error.SquadErrorCode.SQUAD_MEMBER_UNDERFLOW;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import revi1337.onsquad.common.domain.BaseEntity;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.squad.domain.SquadPolicy;
import revi1337.onsquad.squad.domain.error.SquadDomainException;
import revi1337.onsquad.squad.domain.model.SquadCreateSpec;
import revi1337.onsquad.squad.domain.vo.Address;
import revi1337.onsquad.squad.domain.vo.Content;
import revi1337.onsquad.squad.domain.vo.Title;
import revi1337.onsquad.squad_category.domain.entity.SquadCategory;
import revi1337.onsquad.squad_member.domain.entity.SquadMember;
import revi1337.onsquad.squad_member.domain.entity.SquadMemberFactory;

@Getter
@NoArgsConstructor(access = PROTECTED)
@Entity
public class Squad extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Embedded
    private Title title;

    @Embedded
    private Content content;

    @Embedded
    private Address address;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "current_size", nullable = false)
    private int currentSize;

    @Column(name = "remain", nullable = false)
    private int remain;

    private String kakaoLink;

    private String discordLink;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    @OneToMany(mappedBy = "squad", cascade = {PERSIST, MERGE})
    private final List<SquadMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "squad")
    private final List<SquadCategory> categories = new ArrayList<>();

    public static Squad create(SquadCreateSpec spec, Crew crew, Member member, LocalDateTime leaderParticipateAt) {
        Objects.requireNonNull(spec, "squadCreateSpec must not be null");
        Objects.requireNonNull(crew, "crew must not be null");
        Objects.requireNonNull(member, "member must not be null");
        Objects.requireNonNull(leaderParticipateAt, "leaderParticipateAt must not be null");

        Squad squad = new Squad(
                spec.getTitle(),
                spec.getContent(),
                spec.getCapacity(),
                spec.getAddress(),
                spec.getKakaoLink(),
                spec.getDiscordLink()
        );

        squad.registerMember(member);
        squad.registerCrew(crew);
        squad.addMembers(SquadMemberFactory.leader(squad, member, leaderParticipateAt));
        return squad;
    }

    private Squad(Title title, Content content, int capacity, Address address, String kakaoLink, String discordLink) {
        SquadPolicy.validateCapacity(capacity);
        this.title = title;
        this.content = content;
        this.address = address;
        this.capacity = capacity;
        this.remain = capacity;
        this.kakaoLink = kakaoLink;
        this.discordLink = discordLink;
    }

    public void addMembers(SquadMember... squadMembers) {
        for (SquadMember squadMember : squadMembers) {
            increaseCurrentSize();
            squadMember.addSquad(this);
            this.members.add(squadMember);
        }
    }

    public void increaseCurrentSize() {
        if (this.currentSize + 1 > this.capacity) {
            throw new SquadDomainException.NotEnoughLeft(NOT_ENOUGH_LEFT);
        }
        this.currentSize++;
        this.remain--;
    }

    public void decreaseCurrentSize() {
        if (this.currentSize - 1 < 0) {
            throw new SquadDomainException.UnderflowSize(SQUAD_MEMBER_UNDERFLOW);
        }
        this.currentSize--;
        this.remain++;
    }

    public void delegateLeader(SquadMember currentLeader, SquadMember nextLeader) {
        if (currentLeader.isLeader()) {
            nextLeader.promoteToLeader();
            currentLeader.demoteToGeneral();
            updateLeader(nextLeader.getMember());
        }
    }

    public void registerMember(Member member) {
        this.member = member;
    }

    public void registerCrew(Crew crew) {
        this.crew = crew;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Squad squad)) {
            return false;
        }
        return id != null && Objects.equals(id, squad.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    private void updateLeader(Member member) {
        this.member = member;
    }
}
