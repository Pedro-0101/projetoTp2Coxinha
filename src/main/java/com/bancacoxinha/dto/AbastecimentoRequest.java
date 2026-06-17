package com.bancacoxinha.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AbastecimentoRequest(
        @NotNull @Positive Integer denominacao,
        @NotNull @Positive Integer quantidade) {
}
