package com.bancacoxinha.dto;

import java.math.BigDecimal;

public record ItemResponse(
        String sabor,
        int quantidade,
        BigDecimal precoUnitario) {
}
