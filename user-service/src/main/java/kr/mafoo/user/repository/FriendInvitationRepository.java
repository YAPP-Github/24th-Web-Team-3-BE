package kr.mafoo.user.repository;

import kr.mafoo.user.domain.FriendInvitationEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FriendInvitationRepository extends R2dbcRepository<FriendInvitationEntity, String> {
}
