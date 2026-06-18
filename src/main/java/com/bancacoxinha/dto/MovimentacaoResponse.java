package com.bancacoxinha.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MovimentacaoResponse(
        Long id,
        LocalDateTime dataHora,
        String tipoMovimentacao,
        BigDecimal valorNota,
        String sabor,
        int quantidade,
        BigDecimal valor,
        List<TrocoItem> pagamento,
        List<TrocoItem> troco,
        Long movimentacaoOrigemId,
        List<ItemResponse> itens) {
}
