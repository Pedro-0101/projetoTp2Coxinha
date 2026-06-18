package com.bancacoxinha.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TrocaRequest(
        @NotNull Long clienteId,
        @NotNull Long movimentacaoId,
        @NotBlank String saborAntigo,
        @NotBlank String novoSabor,
        boolean promocional) {
}
