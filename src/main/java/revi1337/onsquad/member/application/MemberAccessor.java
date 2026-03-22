package revi1337.onsquad.member.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.error.MemberBusinessException;
import revi1337.onsquad.member.domain.error.MemberErrorCode;
import revi1337.onsquad.member.domain.repository.MemberRepository;
import revi1337.onsquad.member.domain.vo.Email;
import revi1337.onsquad.member.domain.vo.Nickname;

@Component
@RequiredArgsConstructor
public class MemberAccessor {

    private final MemberRepository memberRepository;

    public Member getById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberBusinessException.NotFound(MemberErrorCode.NOT_FOUND));
    }

    public Member getReferenceById(Long memberId) {
        return memberRepository.getReferenceById(memberId);
    }

    public boolean checkNicknameDuplicate(String nickname) {
        return memberRepository.existsByNickname(new Nickname(nickname));
    }

    public boolean checkEmailDuplicate(String email) {
        return memberRepository.existsByEmail(new Email(email));
    }
}
