package revi1337.onsquad.squad_comment.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import revi1337.onsquad.auth.support.Authenticate;
import revi1337.onsquad.auth.support.CurrentMember;
import revi1337.onsquad.common.dto.PageResponse;
import revi1337.onsquad.common.dto.RestResponse;
import revi1337.onsquad.common.presentation.support.AdaptivePageable;
import revi1337.onsquad.squad_comment.application.SquadCommentCommandService;
import revi1337.onsquad.squad_comment.application.SquadCommentQueryService;
import revi1337.onsquad.squad_comment.application.response.SquadCommentResponse;
import revi1337.onsquad.squad_comment.presentation.request.CommentCreateRequest;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SquadCommentController {

    private final SquadCommentCommandService squadCommentCommandService;
    private final SquadCommentQueryService squadCommentQueryService;

    @PostMapping("/squads/{squadId}/comments")
    public ResponseEntity<RestResponse<Void>> add(
            @PathVariable Long squadId,
            @Valid @RequestBody CommentCreateRequest request,
            @Authenticate CurrentMember currentMember
    ) {
        squadCommentCommandService.add(currentMember.id(), squadId, request.content());

        return ResponseEntity.ok().body(RestResponse.created());
    }

    @PostMapping("/squads/{squadId}/replies/{parentId}")
    public ResponseEntity<RestResponse<Void>> addReply(
            @PathVariable Long squadId,
            @PathVariable Long parentId,
            @Valid @RequestBody CommentCreateRequest request,
            @Authenticate CurrentMember currentMember
    ) {
        squadCommentCommandService.addReply(currentMember.id(), squadId, parentId, request.content());

        return ResponseEntity.ok().body(RestResponse.created());
    }

    @GetMapping("/squads/{squadId}/comments")
    public ResponseEntity<RestResponse<PageResponse<SquadCommentResponse>>> fetchInitialComments(
            @PathVariable Long squadId,
            @AdaptivePageable(defaultSort = "createdAt") Pageable pageable,
            @Authenticate CurrentMember currentMember
    ) {
        PageResponse<SquadCommentResponse> response = squadCommentQueryService.fetchInitialComments(currentMember.id(), squadId, pageable);

        return ResponseEntity.ok().body(RestResponse.success(response));
    }

    @GetMapping("/squads/{squadId}/comments/{parentId}/replies")
    public ResponseEntity<RestResponse<PageResponse<SquadCommentResponse>>> fetchMoreChildren(
            @PathVariable Long squadId,
            @PathVariable Long parentId,
            @AdaptivePageable(defaultSort = "createdAt") Pageable pageable,
            @Authenticate CurrentMember currentMember
    ) {
        PageResponse<SquadCommentResponse> response = squadCommentQueryService.fetchMoreChildren(currentMember.id(), squadId, parentId, pageable);

        return ResponseEntity.ok().body(RestResponse.success(response));
    }

    @PatchMapping("/squads/{squadId}/comments/{commentId}")
    public ResponseEntity<RestResponse<Void>> updateComment(
            @PathVariable Long squadId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentCreateRequest request,
            @Authenticate CurrentMember currentMember
    ) {
        squadCommentCommandService.update(currentMember.id(), squadId, commentId, request.content());

        return ResponseEntity.ok().body(RestResponse.noContent());
    }

    @DeleteMapping("/squads/{squadId}/comments/{commentId}")
    public ResponseEntity<RestResponse<Void>> deleteComment(
            @PathVariable Long squadId,
            @PathVariable Long commentId,
            @Authenticate CurrentMember currentMember
    ) {
        squadCommentCommandService.delete(currentMember.id(), squadId, commentId);

        return ResponseEntity.ok().body(RestResponse.noContent());
    }
}
