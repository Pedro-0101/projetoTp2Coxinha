package com.bancacoxinha.strategy;

public class PrecoPadrao implements CalculoPrecoStrategy {

    @Override
    public Double calcular(Double base) {
        return base;
    }
}
