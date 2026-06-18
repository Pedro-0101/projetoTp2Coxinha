package com.bancacoxinha.dto;

import java.math.BigDecimal;
import java.util.List;

public record TrocaResponse(
        Long estornoId,
        Long novaMovimentacaoId,
        List<ItemResponse> itens,
        BigDecimal preco,
        List<TrocoItem> troco,
        BigDecimal trocoEmCredito,
        BigDecimal saldo) {
}
