package revi1337.onsquad.crew.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import revi1337.onsquad.common.error.ErrorCode;

@Getter
@AllArgsConstructor
public enum CrewErrorCode implements ErrorCode {

    INVALID_DETAIL_LENGTH(400, "CR001", "크루 상세정보의 길이는 %d 자 이상 %d 자 입니다."),
    INVALID_HASHTAGS_SIZE(400, "CR002", "해시태그의 최대 개수는 %d 개 입니다."),
    INVALID_INTRODUCE_LENGTH(400, "CR003", "크루 소개는 %d 자 이상 %d 자 입니다."),
    INVALID_NAME_LENGTH(400, "CR004", "크루명의 길이는 %d 자 이상 %d 자 입니다."),

    NOTFOUND_CREW(404, "CR005", "%s 크루 게시글이 존재하지 않습니다."),
    ALREADY_EXISTS(400, "CR006", "%s 크루가 이미 존재하여 크루를 개설할 수 없습니다."),
    CANNOT_JOIN(404, "CR007", "%s 크루가 존재하지 않아 가입신청을 할 수 없습니다."),
    ALREADY_JOIN(400, "CR008", "이미 %s 크루에 가입된 사용자입니다."),
    ALREADY_REQUEST(400, "CR009", "%s 크루에 가입신청을 했지만 요청 수락 전 상태입니다."),
    INVALID_PUBLISHER(400, "CR010", "%s 크루 작성자와 일치하지 않습니다.");

    private final int status;
    private final String code;
    private final String description;

}
