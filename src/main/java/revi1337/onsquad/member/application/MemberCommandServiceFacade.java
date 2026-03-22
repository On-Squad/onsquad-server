package revi1337.onsquad.member.application;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import revi1337.onsquad.crew.domain.error.CrewBusinessException;
import revi1337.onsquad.infrastructure.aws.s3.event.FileDeleteEvent;
import revi1337.onsquad.member.application.dto.MemberCreateDto;
import revi1337.onsquad.member.application.dto.MemberPasswordUpdateDto;
import revi1337.onsquad.member.application.dto.MemberUpdateDto;
import revi1337.onsquad.member.infrastructure.MemberFileStorageManager;

@Service
@RequiredArgsConstructor
public class MemberCommandServiceFacade {

    private final MemberCommandService memberCommandService;
    private final MemberFileStorageManager fileStorageManager;
    private final ApplicationEventPublisher eventPublisher;

    public void newMember(MemberCreateDto dto) {
        memberCommandService.newMember(dto);
    }

    public void updateProfile(Long memberId, MemberUpdateDto dto) {
        memberCommandService.updateProfile(memberId, dto);
    }

    public void updatePassword(Long memberId, MemberPasswordUpdateDto dto) {
        memberCommandService.updatePassword(memberId, dto);
    }

    public void updateImage(Long memberId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        String imageUrl = null;
        try {
            imageUrl = fileStorageManager.upload(file);
            memberCommandService.updateImage(memberId, imageUrl);
        } catch (CrewBusinessException exception) {
            if (imageUrl != null) {
                eventPublisher.publishEvent(new FileDeleteEvent(imageUrl));
            }
            throw exception;
        }
    }

    public void deleteImage(Long memberId) {
        memberCommandService.deleteImage(memberId);
    }

    public void deleteMember(Long memberId) {
        memberCommandService.deleteMember(memberId);
    }
}
