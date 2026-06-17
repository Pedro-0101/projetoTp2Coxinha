package com.bancacoxinha.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreditoRequest(@NotNull Long clienteId, @NotNull @Positive Integer denominacao) {
}
