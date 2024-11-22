package kr.mafoo.user.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "친구 끊기 요청")
public record BreakFriendRequest(
        @Schema(description = "대상 친구 ID", example = "test")
        String memberId
) {
}
