package revi1337.onsquad.crew.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import revi1337.onsquad.crew.application.dto.CrewCreateDto;
import revi1337.onsquad.crew.domain.error.CrewBusinessException;
import revi1337.onsquad.crew.infrastructure.CrewFileStorageManager;
import revi1337.onsquad.infrastructure.aws.s3.event.FileDeleteEvent;

@ExtendWith(MockitoExtension.class)
class CrewCommandServiceFacadeTest {

    @Mock
    private CrewCommandService crewCommandService;

    @Mock
    private CrewFileStorageManager crewFileStorageManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CrewCommandServiceFacade facade;

    @Nested
    class newCrew {

        @Test
        @DisplayName("이미지 파일이 없으면 S3 업로드 없이 크루를 생성한다.")
        void test1() {
            Long memberId = 1L;
            CrewCreateDto dto = mock(CrewCreateDto.class);
            MultipartFile file = mock(MultipartFile.class);
            given(file.isEmpty()).willReturn(true);

            facade.newCrew(memberId, dto, file);

            verify(crewFileStorageManager, times(0)).upload(file);
        }

        @Test
        @DisplayName("이미지 파일이 있으면 S3 업로드 후 크루를 생성한다.")
        void test2() {
            Long memberId = 1L;
            CrewCreateDto dto = mock(CrewCreateDto.class);
            MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "data".getBytes());
            String uploadedUrl = "https://s3.url/test.png";
            given(crewFileStorageManager.upload(file)).willReturn(uploadedUrl);

            facade.newCrew(memberId, dto, file);

            verify(crewCommandService).newCrew(memberId, dto, uploadedUrl);
            verify(eventPublisher, times(0)).publishEvent(any(FileDeleteEvent.class));
        }

        @Test
        @DisplayName("크루 생성 중 예외 발생 시 업로드된 S3 파일을 삭제하는 이벤트를 발행한다.")
        void test3() {
            Long memberId = 1L;
            CrewCreateDto dto = mock(CrewCreateDto.class);
            MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "data".getBytes());
            String uploadedUrl = "https://s3.url/test.png";
            given(crewFileStorageManager.upload(file)).willReturn(uploadedUrl);
            willThrow(CrewBusinessException.NotFound.class)
                    .given(crewCommandService).newCrew(memberId, dto, uploadedUrl);

            assertThatThrownBy(() -> facade.newCrew(memberId, dto, file))
                    .isInstanceOf(CrewBusinessException.class);
            verify(eventPublisher, times(1)).publishEvent(any(FileDeleteEvent.class));
        }
    }

    @Nested
    class updateImage {

        @Test
        @DisplayName("파일이 비어있으면 아무 작업도 수행하지 않는다.")
        void test1() {
            Long memberId = 1L;
            Long crewId = 2L;
            MultipartFile file = mock(MultipartFile.class);
            given(file.isEmpty()).willReturn(true);

            facade.updateImage(memberId, crewId, file);

            verify(crewFileStorageManager, times(0)).upload(file);
        }

        @Test
        @DisplayName("이미지 업데이트에 성공한다.")
        void test2() {
            Long memberId = 1L;
            Long crewId = 2L;
            MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "data".getBytes());
            String uploadedUrl = "https://s3.url/test.png";
            given(crewFileStorageManager.upload(file)).willReturn(uploadedUrl);

            facade.updateImage(memberId, crewId, file);

            verify(crewCommandService).updateImage(memberId, crewId, uploadedUrl);
            verify(eventPublisher, times(0)).publishEvent(any(FileDeleteEvent.class));
        }

        @Test
        @DisplayName("이미지 업데이트 로직 처리 중 예외 발생 시 업로드된 S3 파일을 삭제한다.")
        void test3() {
            Long memberId = 1L;
            Long crewId = 2L;
            MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "data".getBytes());
            String uploadedUrl = "https://s3.url/test.png";
            given(crewFileStorageManager.upload(file)).willReturn(uploadedUrl);
            willThrow(CrewBusinessException.NotFound.class)
                    .given(crewCommandService).updateImage(memberId, crewId, uploadedUrl);

            assertThatThrownBy(() -> facade.updateImage(memberId, crewId, file))
                    .isInstanceOf(CrewBusinessException.class);
            verify(eventPublisher, times(1)).publishEvent(any(FileDeleteEvent.class));
        }
    }
}
