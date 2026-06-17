package com.bancacoxinha.strategy;

public class PrecoPromocional implements CalculoPrecoStrategy {

    private static final double DESCONTO = 2.0;
    private static final double PRECO_MINIMO = 2.0;

    @Override
    public Double calcular(Double base) {
        double comDesconto = base - DESCONTO;
        return Math.max(comDesconto, PRECO_MINIMO);
    }
}
