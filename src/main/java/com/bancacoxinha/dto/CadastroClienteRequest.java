package com.bancacoxinha.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CadastroClienteRequest(
        @NotBlank String nome,
        @NotBlank String login,
        @PositiveOrZero BigDecimal saldoInicial) {
}
