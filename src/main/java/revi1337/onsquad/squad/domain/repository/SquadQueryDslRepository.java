package revi1337.onsquad.squad.domain.repository;

import static revi1337.onsquad.category.domain.entity.QCategory.category;
import static revi1337.onsquad.member.domain.entity.QMember.member;
import static revi1337.onsquad.squad.domain.entity.QSquad.squad;
import static revi1337.onsquad.squad_category.domain.entity.QSquadCategory.squadCategory;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import revi1337.onsquad.category.domain.vo.CategoryType;
import revi1337.onsquad.common.domain.OrderSpecifierBuilder;
import revi1337.onsquad.member.domain.model.SimpleMember;
import revi1337.onsquad.squad.domain.entity.Squad;
import revi1337.onsquad.squad.domain.model.SimpleSquad;
import revi1337.onsquad.squad.domain.model.SquadDetail;

@Repository
@RequiredArgsConstructor
public class SquadQueryDslRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Optional<Squad> fetchSquadDetailById(Long id) {
        return Optional.ofNullable(
                jpaQueryFactory
                        .selectFrom(squad)
                        .innerJoin(squad.member, member).fetchJoin()
                        .leftJoin(squad.categories, squadCategory).fetchJoin()
                        .leftJoin(squadCategory.category, category).fetchJoin()
                        .where(squad.id.eq(id))
                        .fetchOne()
        );
    }

    public Page<SquadDetail> fetchSquadsWithDetailByCrewIdAndCategory(Long crewId, CategoryType categoryType, Pageable pageable) {
        JPQLQuery<SquadDetail> baseQuery = jpaQueryFactory
                .select(Projections.constructor(SquadDetail.class,
                        squad.id,
                        squad.title,
                        squad.content,
                        squad.capacity,
                        squad.remain,
                        squad.address,
                        squad.kakaoLink,
                        squad.discordLink,
                        Projections.constructor(SimpleMember.class,
                                member.id,
                                member.nickname,
                                member.introduce,
                                member.mbti
                        )
                ))
                .from(squad)
                .innerJoin(squad.member, member)
                .where(squad.crew.id.eq(crewId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(OrderSpecifierBuilder.startWith(squad, pageable).build());

        JPAQuery<Long> baseCountQuery = jpaQueryFactory
                .select(squad.id.count())
                .from(squad)
                .where(squad.crew.id.eq(crewId));

        if (categoryType != null) {
            baseQuery.innerJoin(squad.categories, squadCategory)
                    .innerJoin(squadCategory.category, category)
                    .where(category.categoryType.eq(categoryType));

            baseCountQuery.innerJoin(squad.categories, squadCategory)
                    .innerJoin(squadCategory.category, category)
                    .where(category.categoryType.eq(categoryType));
        }

        List<SquadDetail> squads = baseQuery.fetch();

        return PageableExecutionUtils.getPage(squads, pageable, baseCountQuery::fetchOne);
    }

    public Page<SimpleSquad> fetchSquadsByCrewId(Long crewId, Pageable pageable) {
        List<SimpleSquad> squads = jpaQueryFactory
                .select(Projections.constructor(SimpleSquad.class,
                        squad.id,
                        squad.title,
                        squad.capacity,
                        squad.remain,
                        Projections.constructor(SimpleMember.class,
                                member.id,
                                member.nickname,
                                member.introduce,
                                member.mbti
                        )
                ))
                .from(squad)
                .innerJoin(squad.member, member)
                .where(squad.crew.id.eq(crewId))
                .orderBy(OrderSpecifierBuilder.startWith(squad, pageable).build())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(squad.id.count())
                .from(squad)
                .where(squad.crew.id.eq(crewId));

        return PageableExecutionUtils.getPage(squads, pageable, countQuery::fetchOne);
    }
}
