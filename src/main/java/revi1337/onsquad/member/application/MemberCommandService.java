package revi1337.onsquad.member.application;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import revi1337.onsquad.auth.verification.application.EmailVerificationValidator;
import revi1337.onsquad.infrastructure.aws.s3.event.FileDeleteEvent;
import revi1337.onsquad.member.application.dto.MemberCreateDto;
import revi1337.onsquad.member.application.dto.MemberPasswordUpdateDto;
import revi1337.onsquad.member.application.dto.MemberUpdateDto;
import revi1337.onsquad.member.domain.entity.Member;
import revi1337.onsquad.member.domain.error.MemberBusinessException;
import revi1337.onsquad.member.domain.error.MemberErrorCode;
import revi1337.onsquad.member.domain.repository.MemberRepository;
import revi1337.onsquad.member.domain.vo.PasswordPolicy;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberCommandService {

    private final MemberRepository memberRepository;
    private final EmailVerificationValidator emailVerificationValidator;
    private final MemberAccessor memberAccessor;
    private final PasswordEncoder passwordEncoder;
    private final MemberContextHandler memberContextHandler;
    private final ApplicationEventPublisher applicationEventPublisher;

    public void newMember(MemberCreateDto dto) {
        ensureRequirements(dto);
        Member member = dto.toEntity();
        member.updatePassword(passwordEncoder.encode(dto.password()), PasswordPolicy.BCRYPT);
        memberRepository.save(member);
    }

    public void updateProfile(Long memberId, MemberUpdateDto dto) {
        Member member = memberAccessor.getById(memberId);
        member.updateProfile(dto.toSpec());
    }

    public void updatePassword(Long memberId, MemberPasswordUpdateDto dto) {
        Member member = memberAccessor.getById(memberId);
        if (!passwordEncoder.matches(dto.currentPassword(), member.getPassword().getValue())) {
            throw new MemberBusinessException.WrongPassword(MemberErrorCode.WRONG_PASSWORD);
        }
        member.updatePassword(passwordEncoder.encode(dto.newPassword()), PasswordPolicy.BCRYPT);
    }

    public void updateImage(Long memberId, String newImageUrl) {
        Member member = memberAccessor.getById(memberId);
        if (member.hasImage()) {
            applicationEventPublisher.publishEvent(new FileDeleteEvent(member.getProfileImage()));
        }
        member.updateImage(newImageUrl);
    }

    public void deleteImage(Long memberId) {
        Member member = memberAccessor.getById(memberId);
        if (member.hasImage()) {
            applicationEventPublisher.publishEvent(new FileDeleteEvent(member.getProfileImage()));
            member.deleteImage();
        }
    }

    public void deleteMember(Long memberId) {
        Member member = memberAccessor.getById(memberId);
        memberContextHandler.disposeContext(member);
    }

    private void ensureRequirements(MemberCreateDto dto) {
        emailVerificationValidator.ensureEmailVerified(dto.email());
        if (memberAccessor.checkNicknameDuplicate(dto.nickname())) {
            throw new MemberBusinessException.DuplicateNickname(MemberErrorCode.DUPLICATE_NICKNAME);
        }
        if (memberAccessor.checkEmailDuplicate(dto.email())) {
            throw new MemberBusinessException.DuplicateEmail(MemberErrorCode.DUPLICATE_EMAIL);
        }
    }
}
