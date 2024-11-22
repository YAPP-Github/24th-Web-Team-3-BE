package kr.mafoo.user.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "친구 초대 ID 응답")
public record FriendInvitationResponse(
        @Schema(description = "초대 ID", example = "test")
        String invitationId
) {
}
