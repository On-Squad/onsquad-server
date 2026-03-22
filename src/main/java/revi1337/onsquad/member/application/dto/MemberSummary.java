package revi1337.onsquad.member.application.dto;

import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.vo.UserType;

public record MemberSummary(
        Long id,
        String email,
        String password,
        UserType userType
) {

    public MemberSummary(Long id, String email, String password, UserType userType) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.userType = userType;
    }

    public static MemberSummary from(Member member) {
        return new MemberSummary(
                member.getId(),
                member.getEmail().getValue(),
                member.getPassword().getValue(),
                member.getUserType()
        );
    }
}
