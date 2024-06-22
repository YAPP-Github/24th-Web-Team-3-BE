package kr.mafoo.user.service;

import kr.mafoo.user.domain.MemberEntity;
import kr.mafoo.user.exception.MemberNotFoundException;
import kr.mafoo.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Service
public class MemberService {
    private final MemberRepository memberRepository;

    public Mono<Void> quitMemberByMemberId(String memberId) {
        return memberRepository.deleteMemberById(memberId);
    }

    public Mono<MemberEntity> getMemberByMemberId(String memberId) {
        return memberRepository
                .findById(memberId)
                .switchIfEmpty(Mono.error(new MemberNotFoundException()));
    }

}
