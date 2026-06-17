package com.bancacoxinha.dto;

import java.math.BigDecimal;

public record ClienteResponse(Long id, String nome, String login, BigDecimal saldo) {
}
