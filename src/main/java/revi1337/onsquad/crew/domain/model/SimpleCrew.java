package revi1337.onsquad.crew.domain.model;

import revi1337.onsquad.crew.domain.vo.Introduce;
import revi1337.onsquad.crew.domain.vo.Name;
import revi1337.onsquad.member.domain.model.SimpleMember;

public record SimpleCrew(
        Long id,
        Name name,
        Introduce introduce,
        String kakaoLink,
        String imageUrl,
        SimpleMember owner
) {

}
