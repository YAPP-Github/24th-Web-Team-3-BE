package kr.mafoo.user.service;

import kr.mafoo.user.domain.FriendEntity;
import kr.mafoo.user.domain.FriendInvitationEntity;
import kr.mafoo.user.domain.MemberEntity;
import kr.mafoo.user.exception.CannotBeFriendMyselfException;
import kr.mafoo.user.exception.InvitationNotValidException;
import kr.mafoo.user.repository.FriendInvitationRepository;
import kr.mafoo.user.repository.FriendRepository;
import kr.mafoo.user.repository.MemberRepository;
import kr.mafoo.user.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class FriendService {
    private final FriendRepository friendRepository;
    private final FriendInvitationRepository friendInvitationRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Mono<FriendEntity> makeFriend(String fromMemberId, String invitationId) {
        return friendInvitationRepository.findById(invitationId)
                .switchIfEmpty(Mono.error(new InvitationNotValidException()))
                .flatMap(invitationEntity -> {
                    if(fromMemberId.equals(invitationEntity.getFromMemberId())) {
                        return Mono.error(new CannotBeFriendMyselfException());
                    }
                    FriendEntity friendEntity = FriendEntity.newFriend(
                            IdGenerator.generate(), fromMemberId, invitationEntity.getFromMemberId()
                    );
                    return friendRepository.save(
                            friendEntity
                    );
                });
    }

    @Transactional(readOnly = true)
    public Flux<MemberEntity> getFriends(String memberId) {
        return friendRepository
                .findAllByFromMemberId(memberId)
                .map(FriendEntity::getToMemberId)
                .concatWith(
                        friendRepository
                                .findAllByToMemberId(memberId)
                                .map(FriendEntity::getFromMemberId)
                )
                .collectList()
                .flatMapMany(memberRepository::findAllByIdIn);
    }

    @Transactional
    public Mono<Void> breakFriend(String myMemberId, String friendMemberId) {
        return friendRepository
                .findAllByFromMemberId(myMemberId)
                .filter(friendEntity -> friendEntity.getToMemberId().equals(friendMemberId))
                .switchIfEmpty(
                        friendRepository
                                .findAllByToMemberId(myMemberId)
                                .filter(friendEntity -> friendEntity.getFromMemberId().equals(friendMemberId))
                )
                .next()
                .flatMap(friendEntity -> friendRepository.deleteById(friendEntity.getId()));
    }

    @Transactional
    public Mono<FriendInvitationEntity> generateInvitationId(String myMemberId) {
        return generateUniqueInvitationId()
                .flatMap(invitationId -> {
                    FriendInvitationEntity friendInvitationEntity = FriendInvitationEntity.newInvitation(
                            invitationId, myMemberId
                    );
                    return friendInvitationRepository.save(
                            friendInvitationEntity
                    );
                });
    }

    private Mono<String> generateUniqueInvitationId() {
        return Mono.fromCallable(() -> IdGenerator.generateAlphanumericString(8))
                .flatMap(id -> friendInvitationRepository.existsById(id)
                        .flatMap(exists -> exists
                                ? Mono.defer(this::generateUniqueInvitationId) // ID가 존재하면 재귀적으로 호출
                                : Mono.just(id)));
    }
}
