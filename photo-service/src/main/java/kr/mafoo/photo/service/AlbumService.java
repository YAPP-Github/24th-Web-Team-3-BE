package kr.mafoo.photo.service;

import kr.mafoo.photo.domain.AlbumEntity;
import kr.mafoo.photo.domain.AlbumType;
import kr.mafoo.photo.repository.AlbumRepository;
import kr.mafoo.photo.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Service
public class AlbumService {
    private final AlbumRepository albumRepository;

    public Mono<AlbumEntity> createNewAlbum(String ownerMemberId, String albumName, AlbumType albumType) {
        AlbumEntity albumEntity = AlbumEntity.newAlbum(IdGenerator.generate(), albumName, albumType, ownerMemberId);
        return albumRepository.save(albumEntity);
    }

    public Flux<AlbumEntity> findAllByOwnerMemberId(String ownerMemberId) {
        return albumRepository.findAllByOwnerMemberId(ownerMemberId);
    }

    public Mono<Void> deleteAlbumById(String albumId, String requestMemberId) {
        return albumRepository
                .findById(albumId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("앨범을 찾을 수 없습니다.")))
                .flatMap(albumEntity -> {
                    if(!albumEntity.getOwnerMemberId().equals(requestMemberId)) {
                        return Mono.error(new IllegalArgumentException("앨범을 삭제할 권한이 없습니다."));
                    } else {
                        return albumRepository.deleteById(albumId);
                    }
                });
    }

    public Mono<AlbumEntity> updateAlbumName(String albumId, String albumName, String requestMemberId) {
        return albumRepository
                .findById(albumId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("앨범을 찾을 수 없습니다.")))
                .flatMap(albumEntity -> {
                    if(!albumEntity.getOwnerMemberId().equals(requestMemberId)) {
                        return Mono.error(new IllegalArgumentException("앨범을 삭제할 권한이 없습니다."));
                    } else {
                        return albumRepository.save(albumEntity.updateName(albumName));
                    }
                });
    }

    public Mono<AlbumEntity> updateAlbumType(String albumId, AlbumType albumType, String requestMemberId) {
        return albumRepository
                .findById(albumId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("앨범을 찾을 수 없습니다.")))
                .flatMap(albumEntity -> {
                    if(!albumEntity.getOwnerMemberId().equals(requestMemberId)) {
                        return Mono.error(new IllegalArgumentException("앨범을 삭제할 권한이 없습니다."));
                    } else {
                        return albumRepository.save(albumEntity.updateType(albumType));
                    }
                });
    }
}
