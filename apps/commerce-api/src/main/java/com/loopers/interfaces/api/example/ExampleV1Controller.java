package com.loopers.interfaces.api.example;

import com.loopers.application.example.ExampleFacade;
import com.loopers.application.example.ExampleInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/examples")
// 예시 API 컨트롤러 (레거시 참고용)
public class ExampleV1Controller implements ExampleV1ApiSpec {

    private final ExampleFacade exampleFacade;

    // id로 예시 조회
    @GetMapping("/{exampleId}")
    @Override
    public ApiResponse<ExampleV1Dto.ExampleResponse> getExample(
        @PathVariable(value = "exampleId") Long exampleId
    ) {
        ExampleInfo info = exampleFacade.getExample(exampleId);
        ExampleV1Dto.ExampleResponse response = ExampleV1Dto.ExampleResponse.from(info);
        return ApiResponse.success(response);
    }
}
