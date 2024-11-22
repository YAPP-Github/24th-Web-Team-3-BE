package kr.mafoo.user.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "친구 맺기 요청")
public record MakeFriendRequest(
        @Schema(description = "초대 ID", example = "test")
        String invitationId
) {
}
