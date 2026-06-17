package com.bancacoxinha.dto;

import java.math.BigDecimal;

public record CreditoResponse(Long movimentacaoId, Integer denominacao, Integer quantidadeSlot, BigDecimal saldo) {
}
