package revi1337.onsquad.squad_member.domain.entity;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;
import static revi1337.onsquad.squad_member.domain.SquadRole.GENERAL;
import static revi1337.onsquad.squad_member.domain.SquadRole.LEADER;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.squad.domain.entity.Squad;
import revi1337.onsquad.squad_member.domain.SquadRole;

@Getter
@NoArgsConstructor(access = PROTECTED)
@Entity
@Table(
        uniqueConstraints = {@UniqueConstraint(name = "uk_squadmember_squad_member", columnNames = {"squad_id", "member_id"})},
        indexes = {@Index(name = "idx_squadmember_participate_at", columnList = "participate_at")}
)
public class SquadMember {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "squad_id", nullable = false)
    private Squad squad;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(STRING)
    @Column(nullable = false)
    private SquadRole role;

    @Column(nullable = false)
    private LocalDateTime participateAt;

    SquadMember(Squad squad, Member member, SquadRole role, LocalDateTime participantAt) {
        this.squad = squad;
        this.member = member;
        this.role = role == null ? GENERAL : role;
        this.participateAt = participantAt;
    }

    public void addSquad(Squad squad) {
        this.squad = squad;
    }

    public void leaveSquad() {
        squad.decreaseCurrentSize();
        releaseMember();
        releaseSquad();
    }

    public void promoteToLeader() {
        this.role = SquadRole.LEADER;
    }

    public void demoteToGeneral() {
        this.role = SquadRole.GENERAL;
    }

    public boolean isLeader() {
        return this.role == LEADER;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof SquadMember that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    private void releaseMember() {
        this.member = null;
    }

    private void releaseSquad() {
        this.squad = null;
    }
}
