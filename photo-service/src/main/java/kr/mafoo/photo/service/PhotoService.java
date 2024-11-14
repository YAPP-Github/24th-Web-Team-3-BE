package kr.mafoo.photo.service;

import kr.mafoo.photo.domain.AlbumEntity;
import kr.mafoo.photo.domain.BrandType;
import kr.mafoo.photo.domain.PhotoEntity;
import kr.mafoo.photo.exception.PhotoDisplayIndexIsSameException;
import kr.mafoo.photo.exception.PhotoDisplayIndexNotValidException;
import kr.mafoo.photo.exception.PhotoNotFoundException;
import kr.mafoo.photo.repository.PhotoRepository;
import kr.mafoo.photo.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.LimitedDataBufferList;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@RequiredArgsConstructor
@Service
public class PhotoService {
    private final PhotoRepository photoRepository;

    private final AlbumService albumService;
    private final QrService qrService;
    private final ObjectStorageService objectStorageService;

    @Transactional
    public Mono<PhotoEntity> createNewPhotoByQrUrl(String qrUrl, String requestMemberId) {
        return qrService
                .getFileFromQrUrl(qrUrl)
                .flatMap(fileDto -> objectStorageService.uploadFile(fileDto.fileByte())
                        .flatMap(photoUrl -> createNewPhoto(photoUrl, fileDto.type(), requestMemberId))
                );
    }

    @Transactional
    public Flux<PhotoEntity> createNewPhotoFileUrls(String[] fileUrls, String albumId, String requestMemberId) {
        return albumService.findByAlbumId(albumId, requestMemberId)
                .flatMapMany(albumEntity -> {
                    AtomicInteger displayIndex = new AtomicInteger(albumEntity.getPhotoCount());

                    return Flux.fromArray(fileUrls)
                            .concatMap(fileUrl ->
                                    createNewPhotoFileUrl(fileUrl, BrandType.EXTERNAL, albumId, displayIndex.getAndIncrement(), requestMemberId)
                            );
                });
    }

    private Mono<PhotoEntity> createNewPhotoFileUrl(String fileUrl, BrandType type, String albumId, Integer displayIndex, String requestMemberId) {
        return objectStorageService.setObjectPublicRead(fileUrl)
                .flatMap(fileLink -> {
                    PhotoEntity photoEntity = PhotoEntity.newPhoto(IdGenerator.generate(), fileLink, type, albumId, displayIndex, requestMemberId);
                    return albumService.increaseAlbumPhotoCount(albumId, 1, requestMemberId)
                            .then(photoRepository.save(photoEntity));
                });
    }

    private Mono<PhotoEntity> createNewPhoto(String photoUrl, BrandType type, String requestMemberId) {
        PhotoEntity photoEntity = PhotoEntity.newPhoto(IdGenerator.generate(), photoUrl, type, null, 0, requestMemberId);
        return photoRepository.save(photoEntity);
    }

    @Transactional
    public Flux<PhotoEntity> uploadPhoto(Flux<FilePart> files, String requestMemberId) {
        return files
                .parallel()
                .flatMap(filePart ->
                        filePart.content()
                                .collect(() -> new LimitedDataBufferList(-31), LimitedDataBufferList::add)
                                .filter(list -> !list.isEmpty())
                                .map(list -> list.get(0).factory().join(list))
                                .doOnDiscard(DataBuffer.class, DataBufferUtils::release)
                                .map(dataBuffer -> {
                                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                    dataBuffer.read(bytes);
                                    DataBufferUtils.release(dataBuffer);
                                    return bytes;
                                })
                                .flatMap(bytes -> objectStorageService.uploadFile(bytes)
                                        .flatMap(photoUrl -> {
                                            PhotoEntity photoEntity = PhotoEntity.newPhoto(IdGenerator.generate(), photoUrl, BrandType.EXTERNAL, null, 0, requestMemberId);
                                            return photoRepository.save(photoEntity);
                                        }))
                                .subscribeOn(Schedulers.boundedElastic())

                ).sequential();
    }

//     @Transactional(readOnly = true)
//     public Flux<PhotoEntity> findAllByAlbumId(String albumId, String requestMemberId) {
//         return albumService.checkAlbumReadPermission(albumId, requestMemberId)
//                 .flatMapMany(albumEntity -> handleFindAllByAlbumId(albumEntity.getId()));
//     }

//     private Flux<PhotoEntity> handleFindAllByAlbumId(String albumId) {
//         return photoRepository.findAllByAlbumIdOrderByDisplayIndexDesc(albumId);

    public Flux<PhotoEntity> findAllByAlbumId(String albumId, String requestMemberId, String sort) {
        String sortMethod = (sort == null) ? "CUSTOM" : sort.toUpperCase();

        return albumService.findByAlbumId(albumId, requestMemberId)
                .thenMany(
                        switch (sortMethod) {
                            case "ASC" -> photoRepository.findAllByAlbumIdOrderByCreatedAtAsc(albumId);
                            case "DESC" -> photoRepository.findAllByAlbumIdOrderByCreatedAtDesc(albumId);
                            default -> photoRepository.findAllByAlbumIdOrderByDisplayIndexDesc(albumId);
                        }
                );
    }

    public Mono<PhotoEntity> findByPhotoId(String photoId, String requestMemberId) {
        return photoRepository
                .findById(photoId)
                .switchIfEmpty(Mono.error(new PhotoNotFoundException()))
//                 .flatMap(photoEntity -> albumService
//                         .checkAlbumFullAccessPermission(photoEntity.getAlbumId(), requestMemberId)
//                         .flatMap(albumEntity -> handleDeletePhotoById(photoEntity)));
//     }

//     private Mono<Void> handleDeletePhotoById(PhotoEntity photoEntity) {
//         return albumService.decreaseAlbumPhotoCount(photoEntity.getAlbumId())
//                 .then(photoRepository.popDisplayIndexGreaterThan(photoEntity.getAlbumId(), photoEntity.getDisplayIndex()))
//                 .then(photoRepository.deleteById(photoEntity.getPhotoId()));

                .flatMap(photoEntity -> {
                    if (!photoEntity.hasOwnerMemberId()) {
                        return photoRepository.save(photoEntity.updateOwnerMemberId(requestMemberId));
                    }
                    else if (!photoEntity.getOwnerMemberId().equals(requestMemberId)) {
                        // 내 사진이 아니면 그냥 없는 사진 처리
                        return Mono.error(new PhotoNotFoundException());
                    } else {
                        return Mono.just(photoEntity);
                    }
                });
    }

    @Transactional
    public Mono<Void> deletePhotoById(String photoId, String requestMemberId) {
        return findByPhotoId(photoId, requestMemberId)
                .flatMap(photoEntity ->
                        albumService.decreaseAlbumPhotoCount(photoEntity.getAlbumId(), 1, requestMemberId)
                                .then(photoRepository.popDisplayIndexGreaterThan(photoEntity.getAlbumId(), photoEntity.getDisplayIndex()))
                                .then(photoRepository.deleteById(photoId))
                );
    }

    @Transactional
    public Flux<PhotoEntity> updatePhotoBulkAlbumId(String[] photoIds, String albumId, String requestMemberId) {
        return Flux.fromArray(photoIds)
                .concatMap(photoId -> this.updatePhotoAlbumId(photoId, albumId, requestMemberId));
    }

    @Transactional
    public Mono<PhotoEntity> updatePhotoAlbumId(String photoId, String albumId, String requestMemberId) {
//         return photoRepository
//                 .findById(photoId)
//                 .switchIfEmpty(Mono.error(new PhotoNotFoundException()))
//                 .flatMap(photoEntity -> {

//                     if (photoEntity.getAlbumId() == null) {
//                         if (!photoEntity.hasOwnerMemberId()) {
//                             photoEntity.updateOwnerMemberId(requestMemberId);
//                         }
                      
//                         return albumService.checkAlbumFullAccessPermission(albumId, requestMemberId)
//                                 .flatMap(albumEntity -> photoRepository.save(photoEntity.updateAlbumId(albumId)));
//                     }

//                     return albumService.checkAlbumFullAccessPermission(photoEntity.getAlbumId(), requestMemberId)
//                             .flatMap(previousAlbum -> albumService.checkAlbumFullAccessPermission(albumId, requestMemberId)
//                                     .flatMap(newAlbum -> handleUpdatePhotoAlbumId(photoEntity, newAlbum)));
//                 });
        return findByPhotoId(photoId, requestMemberId)
                .flatMap(photoEntity ->
                        albumService.findByAlbumId(albumId, requestMemberId)
                                .flatMap(albumEntity ->
                                        albumService.decreaseAlbumPhotoCount(photoEntity.getAlbumId(), 1, requestMemberId)
                                                .then(photoRepository.popDisplayIndexGreaterThan(photoEntity.getAlbumId(), photoEntity.getDisplayIndex()))
                                                .then(albumService.increaseAlbumPhotoCount(albumId, 1, requestMemberId))
                                                .then(photoRepository.save(
                                                        photoEntity
                                                                .updateAlbumId(albumId)
                                                                .updateDisplayIndex(albumEntity.getPhotoCount())
                                                ))
                                )
                );
    }

    private Mono<PhotoEntity> handleUpdatePhotoAlbumId(PhotoEntity photoEntity, AlbumEntity albumEntity) {
        return albumService.decreaseAlbumPhotoCount(photoEntity.getAlbumId())
                .then(photoRepository.popDisplayIndexGreaterThan(photoEntity.getAlbumId(), photoEntity.getDisplayIndex()))
                .then(albumService.increaseAlbumPhotoCount(albumEntity.getId()))
                .then(photoRepository.save(
                        photoEntity.updateAlbumId(albumEntity.getId()).updateDisplayIndex(albumEntity.getPhotoCount())
                ));
    }

    @Transactional
    public Mono<PhotoEntity> updatePhotoDisplayIndex(String photoId, Integer newIndex, String requestMemberId) {
//         return photoRepository
//                 .findById(photoId)
//                 .switchIfEmpty(Mono.error(new PhotoNotFoundException()))
//                 .flatMap(photoEntity -> albumService.checkAlbumFullAccessPermission(photoEntity.getAlbumId(), requestMemberId)
//                         .flatMap(albumEntity -> {
//                             int targetIndex = albumEntity.getPhotoCount() - newIndex - 1;

//                             if (photoEntity.getDisplayIndex().equals(targetIndex)) {
//                                 return Mono.error(new PhotoDisplayIndexIsSameException());
//                             }

//                             if (targetIndex < 0 || targetIndex >= albumEntity.getPhotoCount()) {
//                                 return Mono.error(new PhotoDisplayIndexNotValidException());
//                             }

//                             if (photoEntity.getDisplayIndex() < targetIndex) {
//                                 return photoRepository
//                                         .popDisplayIndexBetween(photoEntity.getAlbumId(), photoEntity.getDisplayIndex() + 1, targetIndex)
//                                         .then(photoRepository.save(photoEntity.updateDisplayIndex(targetIndex)));
//                             } else {
//                                 return photoRepository
//                                         .pushDisplayIndexBetween(photoEntity.getAlbumId(), targetIndex, photoEntity.getDisplayIndex() - 1)
//                                         .then(photoRepository.save(photoEntity.updateDisplayIndex(targetIndex)));
//                             }
//                         }));
        return findByPhotoId(photoId, requestMemberId)
                .flatMap(photoEntity ->
                        albumService.findByAlbumId(photoEntity.getAlbumId(), requestMemberId)
                                .flatMap(albumEntity -> {
                                    int targetIndex = albumEntity.getPhotoCount() - newIndex - 1;

                                    if (photoEntity.getDisplayIndex().equals(targetIndex)) {
                                        return Mono.error(new PhotoDisplayIndexIsSameException());
                                    }

                                    if (targetIndex < 0 || targetIndex >= albumEntity.getPhotoCount()) {
                                        return Mono.error(new PhotoDisplayIndexNotValidException());
                                    }

                                    if (photoEntity.getDisplayIndex() < targetIndex) {
                                        return photoRepository
                                                .popDisplayIndexBetween(photoEntity.getAlbumId(), photoEntity.getDisplayIndex() + 1, targetIndex)
                                                .then(photoRepository.save(photoEntity.updateDisplayIndex(targetIndex)));
                                    } else {
                                        return photoRepository
                                                .pushDisplayIndexBetween(photoEntity.getAlbumId(), targetIndex, photoEntity.getDisplayIndex() - 1)
                                                .then(photoRepository.save(photoEntity.updateDisplayIndex(targetIndex)));
                                    }
                                })
                );
    }

}
