package revi1337.onsquad.history.presentation;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import revi1337.onsquad.auth.support.Authenticate;
import revi1337.onsquad.auth.support.CurrentMember;
import revi1337.onsquad.common.dto.PageResponse;
import revi1337.onsquad.common.dto.RestResponse;
import revi1337.onsquad.common.presentation.support.AdaptivePageable;
import revi1337.onsquad.history.application.HistoryQueryService;
import revi1337.onsquad.history.application.response.HistoryResponse;
import revi1337.onsquad.history.domain.HistoryType;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryQueryService historyQueryService;

    @GetMapping("/members/me/histories")
    public ResponseEntity<RestResponse<PageResponse<HistoryResponse>>> fetchHistories(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) HistoryType type,
            @AdaptivePageable(defaultSort = "recordedAt") Pageable pageable,
            @Authenticate CurrentMember currentMember
    ) {
        PageResponse<HistoryResponse> response = historyQueryService.fetchHistories(currentMember.id(), from, to, type, pageable);

        return ResponseEntity.ok(RestResponse.success(response));
    }
}
