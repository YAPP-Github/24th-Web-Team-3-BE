package kr.mafoo.user.repository;

import kr.mafoo.user.domain.FriendEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface FriendRepository extends R2dbcRepository<FriendEntity, String> {
    Flux<FriendEntity> findAllByFromMemberId(String fromMemberId);
    Flux<FriendEntity> findAllByToMemberId(String toMemberId);
}
