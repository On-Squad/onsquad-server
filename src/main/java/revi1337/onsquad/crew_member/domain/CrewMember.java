package revi1337.onsquad.crew_member.domain;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;
import static revi1337.onsquad.crew_member.domain.vo.CrewRole.GENERAL;
import static revi1337.onsquad.crew_member.domain.vo.CrewRole.MANAGER;
import static revi1337.onsquad.crew_member.domain.vo.CrewRole.OWNER;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import revi1337.onsquad.common.domain.RequestEntity;
import revi1337.onsquad.crew.domain.Crew;
import revi1337.onsquad.crew_member.domain.vo.CrewRole;
import revi1337.onsquad.member.domain.Member;

@DynamicInsert
@Getter
@NoArgsConstructor(access = PROTECTED)
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "crewmember_uidx_crew_id_member_id", columnNames = {"crew_id", "member_id"})
})
@AttributeOverrides({
        @AttributeOverride(name = "requestAt", column = @Column(name = "participate_at", nullable = false))
})
public class CrewMember extends RequestEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ColumnDefault("'GENERAL'")
    @Enumerated(STRING)
    @Column(nullable = false)
    private CrewRole role = GENERAL;

    private CrewMember(Member member, CrewRole role, LocalDateTime participantAt) {
        this(null, member, role, participantAt);
    }

    private CrewMember(Crew crew, Member member, CrewRole role, LocalDateTime participantAt) {
        super(participantAt);
        this.crew = crew;
        this.member = member;
        this.role = role == null ? GENERAL : role;
    }

    public static CrewMember forGeneral(Crew crew, Member member, LocalDateTime participantAt) {
        CrewMember crewMember = forGeneral(member, participantAt);
        crewMember.addCrew(crew);
        return crewMember;
    }

    public static CrewMember forGeneral(Crew crew, Member member) {
        CrewMember crewMember = forGeneral(member, LocalDateTime.now());
        crewMember.addCrew(crew);
        return crewMember;
    }

    public static CrewMember forManager(Crew crew, Member member, LocalDateTime participantAt) {
        CrewMember crewMember = new CrewMember(crew, member, MANAGER, participantAt);
        crewMember.addCrew(crew);
        return crewMember;
    }

    public static CrewMember forOwner(Crew crew, Member member, LocalDateTime participantAt) {
        CrewMember crewMember = forOwner(member, participantAt);
        crewMember.addCrew(crew);
        return crewMember;
    }

    public static CrewMember forGeneral(Member member, LocalDateTime participantAt) {
        return new CrewMember(member, GENERAL, participantAt);
    }

    public static CrewMember forManager(Member member, LocalDateTime participantAt) {
        return new CrewMember(member, MANAGER, participantAt);
    }

    public static CrewMember forOwner(Member member, LocalDateTime participantAt) {
        return new CrewMember(member, OWNER, participantAt);
    }

    public void addCrew(Crew crew) {
        this.crew = crew;
    }

    public void releaseCrew() {
        this.crew = null;
    }

    public boolean doesNotMatchMemberId(Long memberId) {
        return !matchMemberId(memberId);
    }

    public boolean matchMemberId(Long memberId) {
        return member.getId().equals(memberId);
    }

    public boolean hasSameId(Long crewMemberId) {
        return id.equals(crewMemberId);
    }

    public boolean isOwnerOfSquad(Long squadId) {
        return id.equals(squadId);
    }

    public boolean isNotOwner() {
        return !isOwner();
    }

    public boolean isOwner() {
        return role == OWNER;
    }

    public boolean isManager() {
        return role == MANAGER;
    }

    public boolean isGeneral() {
        return role == GENERAL;
    }

    public boolean isLessThenManager() {
        return !isGreaterThenManager();
    }

    public boolean isGreaterThenManager() {
        return role == OWNER || role == MANAGER;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof CrewMember that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
