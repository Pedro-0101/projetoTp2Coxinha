package com.bancacoxinha.dto;

import java.math.BigDecimal;
import java.util.List;

public record CompraResponse(
        Long movimentacaoId,
        String sabor,
        int quantidade,
        BigDecimal preco,
        List<TrocoItem> pagamento,
        List<TrocoItem> troco,
        BigDecimal trocoEmCredito,
        BigDecimal saldo) {
}
