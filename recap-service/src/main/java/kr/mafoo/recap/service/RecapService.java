package kr.mafoo.recap.service;

import java.util.UUID;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecapService {

    @Value("${recap.path}")
    private String tmpPath;

    private final ObjectStorageService objectStorageService;
    private final LocalMemoryService localMemoryService;
    private final FFmpegExecutor ffmpegExecutor;

    public String generateCombinedVideo(List<String> photoUrls) {

        String recapId = UUID.randomUUID().toString();

        return calculateFrameRate(photoUrls.size())
            .flatMap(frameRate -> downloadFilesForRecap(photoUrls, recapId)
                .collectList()
                .then(combineVideo(String.valueOf(frameRate), recapId))
            )
            .flatMap(objectStorageService::uploadFile)
            .flatMap(videoUrl ->
                localMemoryService.deleteFileBulk(tmpPath, recapId)
                    .thenReturn(videoUrl)
            )
            .block();
    }

    private Mono<Double> calculateFrameRate(int photoCount) {
        if (photoCount < 2 || photoCount > 10) {
            return Mono.error(new IllegalArgumentException("리캡의 사진 수가 허용 범위를 벗어났습니다."));
        }
        return Mono.just(photoCount / 5.0);
    }

    private Flux<String> downloadFilesForRecap(List<String> photoUrls, String recapId) {
        return Flux.fromIterable(photoUrls)
            .flatMap(fileUrl -> {
                int index = photoUrls.indexOf(fileUrl);
                return objectStorageService.setObjectPublicRead(fileUrl)
                    .flatMap(publicUrl ->
                        localMemoryService.downloadFile(publicUrl, getPhotoFilePath(recapId, index + 1, publicUrl))
                            .doFinally(signal ->
                                objectStorageService.removeFile(publicUrl).subscribe()
                            )
                    );
            });
    }

    private Mono<String> combineVideo(String frameRate, String recapId) {
        return Mono.fromCallable(() -> {
            try {
                String recapVideoPath = getVideoFilePath(recapId);

                FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(getPhotoFilePath(recapId))
                    .addExtraArgs("-framerate", frameRate)

                    .addOutput(recapVideoPath)
                    .addExtraArgs("-t", "5")
                    .done();

                ffmpegExecutor.createJob(builder).run();

                return recapVideoPath;
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate recap video", e);
            }
        });
    }

    private String getPhotoFilePath(String recapId) {
        return String.format("%s/%s_photo_%%02d.png", tmpPath, recapId);
    }

    private String getPhotoFilePath(String recapId, int index, String publicUrl) {
        return String.format("%s/%s_photo_%02d.%s", tmpPath, recapId, index, extractFileType(publicUrl));
    }

    private String extractFileType(String fileUrl) {
        return fileUrl.substring(fileUrl.lastIndexOf(".") + 1);
    }

    private String getVideoFilePath(String recapId) {
        return String.format("%s/%s_video.mp4", tmpPath, recapId);
    }
}
