package kr.mafoo.recap.controller;

import java.util.Arrays;
import java.util.function.Function;
import kr.mafoo.recap.controller.dto.request.RecapCreateRequest;
import kr.mafoo.recap.controller.dto.response.RecapResponse;
import kr.mafoo.recap.service.RecapService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecapController implements Function<RecapCreateRequest, RecapResponse> {

    private final RecapService recapService;

    @Override
    public RecapResponse apply(RecapCreateRequest request) {
        return new RecapResponse(
            recapService.generateCombinedVideo(Arrays.stream(request.fileUrls()).toList())
        );
    }
}

