package kr.mafoo.recap.service;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class LocalMemoryService {

    public Mono<String> downloadFile(String fileUrl, String downloadPath) {
        return Mono.fromCallable(() -> {
            try {
                FileUtils.copyURLToFile(new URL(fileUrl), new File(downloadPath));
                return downloadPath;
            } catch (IOException e) {
                throw new RuntimeException("Failed to download image for recap: " + fileUrl, e);
            }
        });
    }

    public Mono<Void> deleteFileBulk(String path, String identifier) {
        return Mono.fromCallable(() -> new File(path).listFiles())
            .flatMapMany(files -> {
                if (files == null) {
                    throw new RuntimeException("Failed to retrieve file list from the specified path: " + path);
                }
                return Flux.fromArray(files);
            })
            .filter(file -> file.getName().contains(identifier) && file.isFile())
            .flatMap(file -> Mono.fromCallable(() -> {
                        boolean deleted = file.delete();
                        if (!deleted) {
                            throw new RuntimeException("Failed to delete file: " + file.getAbsolutePath());
                        }
                        return deleted;
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .onErrorResume(e -> Mono.empty())
                    .then()
            )
            .then()
            .onErrorResume(e -> {
                throw new RuntimeException("Error occurred while processing files in the directory: " + path, e);
            });
    }
}
