package com.xiuxian.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record GenerateRequest(
        @NotBlank @Size(max = 200) String topic,
        String difficulty,
        @Min(1) @Max(20) Integer count
) {
}
