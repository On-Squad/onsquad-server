package revi1337.onsquad.announce.domain.entity;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import revi1337.onsquad.announce.domain.vo.Title;
import revi1337.onsquad.common.domain.BaseEntity;
import revi1337.onsquad.crew.domain.entity.Crew;
import revi1337.onsquad.member.domain.entity.Member;

@Getter
@NoArgsConstructor(access = PROTECTED)
@Entity
public class Announce extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Embedded
    private Title title;

    @Lob
    @Column(nullable = false, length = 10000)
    private String content;

    @Column(name = "pinned")
    private boolean pinned = false;

    @Column(name = "pinned_at")
    private LocalDateTime pinnedAt;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    public Announce(String title, String content, Crew crew, Member member) {
        this.title = new Title(title);
        this.content = content;
        this.crew = crew;
        this.member = member;
    }

    public void update(String title, String content) {
        this.title = this.title.update(title);
        this.content = content;
    }

    public void pin(LocalDateTime pinnedAt) {
        this.pinned = true;
        this.pinnedAt = pinnedAt;
    }

    public void unpin() {
        this.pinned = false;
        this.pinnedAt = null;
    }

    public boolean isUnpinned() {
        return !isPinned();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Announce announce)) {
            return false;
        }
        return id != null && Objects.equals(getId(), announce.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
