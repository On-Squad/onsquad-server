package revi1337.onsquad.crew.application;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import revi1337.onsquad.crew.application.dto.CrewCreateDto;
import revi1337.onsquad.crew.application.dto.CrewUpdateDto;
import revi1337.onsquad.crew.domain.error.CrewBusinessException;
import revi1337.onsquad.crew.infrastructure.CrewFileStorageManager;
import revi1337.onsquad.infrastructure.aws.s3.event.FileDeleteEvent;

@Service
@RequiredArgsConstructor
public class CrewCommandServiceFacade {

    private final CrewCommandService crewCommandService;
    private final CrewFileStorageManager fileStorageManager;
    private final ApplicationEventPublisher eventPublisher;

    public void newCrew(Long memberId, CrewCreateDto dto, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            crewCommandService.newCrew(memberId, dto, null);
            return;
        }
        String imageUrl = null;
        try {
            imageUrl = fileStorageManager.upload(file);
            crewCommandService.newCrew(memberId, dto, imageUrl);
        } catch (CrewBusinessException exception) {
            if (imageUrl != null) {
                eventPublisher.publishEvent(new FileDeleteEvent(imageUrl));
            }
            throw exception;
        }
    }

    public void updateCrew(Long memberId, Long crewId, CrewUpdateDto dto) {
        crewCommandService.updateCrew(memberId, crewId, dto);
    }

    public void updateImage(Long memberId, Long crewId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        String imageUrl = null;
        try {
            imageUrl = fileStorageManager.upload(file);
            crewCommandService.updateImage(memberId, crewId, imageUrl);
        } catch (CrewBusinessException exception) {
            if (imageUrl != null) {
                eventPublisher.publishEvent(new FileDeleteEvent(imageUrl));
            }
            throw exception;
        }
    }

    public void deleteImage(Long memberId, Long crewId) {
        crewCommandService.deleteImage(memberId, crewId);
    }

    public void deleteCrew(Long memberId, Long crewId) {
        crewCommandService.deleteCrew(memberId, crewId);
    }
}
