package kr.mafoo.recap.config;

import java.io.IOException;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FFmpegConfig {

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @Bean
    public FFmpegExecutor ffMpegExecutor() throws IOException {
        FFmpeg ffmpeg = new FFmpeg(ffmpegPath);
        return new FFmpegExecutor(ffmpeg);
    }
}
