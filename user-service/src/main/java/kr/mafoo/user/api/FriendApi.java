package kr.mafoo.user.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.mafoo.user.annotation.RequestMemberId;
import kr.mafoo.user.controller.dto.request.BreakFriendRequest;
import kr.mafoo.user.controller.dto.request.MakeFriendRequest;
import kr.mafoo.user.controller.dto.response.FriendInvitationResponse;
import kr.mafoo.user.controller.dto.response.MemberResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Tag(name = "친구 맺기 API", description = "내(사용자의) 친구를 관리하는 API")
@Validated
@RequestMapping("/v1/friends")
public interface FriendApi {
    @Operation(summary = "내 친구 목록 조회", description = "내 친구 목록을 조회합니다.")
    @GetMapping
    Flux<MemberResponse> getFriendList(
            @RequestMemberId @Parameter(hidden = true) String memberId
    );

    @Operation(summary = "친구 끊기", description = "친구를 끊어요")
    @PostMapping("/break-friend")
    Mono<Void> breakFriend(
            @RequestMemberId @Parameter(hidden = true) String memberId,
            @RequestBody BreakFriendRequest breakFriendRequest
    );

    @Operation(summary = "친구 맺기", description = "친구를 맺어요")
    @PostMapping("/make-friend")
    Mono<Void> makeFriend(
            @RequestMemberId @Parameter(hidden = true) String memberId,
            @RequestBody MakeFriendRequest makeFriendRequest
    );

    @Operation(summary = "친구 초대 id 발급하기", description = "친구 초대 id를 만들어요")
    @PostMapping("/invitation-id")
    Mono<FriendInvitationResponse> createFriendInvitationId(
            @RequestMemberId @Parameter(hidden = true) String memberId
    );
}
