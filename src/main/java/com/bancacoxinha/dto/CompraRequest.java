package com.bancacoxinha.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CompraRequest(
        @NotNull Long clienteId,
        @NotEmpty List<@NotNull ItemCompraRequest> itens,
        @NotEmpty List<@NotNull @jakarta.validation.constraints.Positive Integer> notasPagas,
        boolean promocional,
        boolean trocoExato) {
}
