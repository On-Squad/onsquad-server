package revi1337.onsquad.squad_comment.domain;

import static revi1337.onsquad.squad_comment.error.SquadCommentErrorCode.NOTFOUND_COMMENT;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import revi1337.onsquad.squad_comment.domain.dto.SquadCommentDomainDto;
import revi1337.onsquad.squad_comment.error.exception.SquadCommentBusinessException;

public interface SquadCommentRepository {

    SquadComment save(SquadComment crewComment);

    Optional<SquadComment> findById(Long id);

    List<SquadCommentDomainDto> findAllWithMemberBySquadId(Long squadId);

    List<SquadCommentDomainDto> fetchPageableParentCommentsWithLimitChildren(Long squadId, Pageable pageable,
                                                                             int childSize);

    List<SquadCommentDomainDto> findChildComments(Long squadId, Long parentId, Pageable pageable);

    default SquadComment getById(Long id) {
        return findById(id)
                .orElseThrow(() -> new SquadCommentBusinessException.NotFoundById(NOTFOUND_COMMENT, id));
    }
}
