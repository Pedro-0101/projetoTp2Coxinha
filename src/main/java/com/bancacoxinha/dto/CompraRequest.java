package com.bancacoxinha.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CompraRequest(
        @NotNull Long clienteId,
        @NotBlank String sabor,
        @NotEmpty List<@NotNull @Positive Integer> notasPagas,
        boolean promocional,
        boolean trocoExato,
        @Positive Integer quantidade) {

    public CompraRequest {
        if (quantidade == null || quantidade < 1) {
            quantidade = 1;
        }
    }
}
