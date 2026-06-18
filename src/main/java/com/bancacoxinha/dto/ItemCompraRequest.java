package com.bancacoxinha.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ItemCompraRequest(
        @NotBlank String sabor,
        @Positive int quantidade) {
}
