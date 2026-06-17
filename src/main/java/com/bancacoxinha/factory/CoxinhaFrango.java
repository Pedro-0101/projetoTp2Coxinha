package com.bancacoxinha.factory;

public class CoxinhaFrango implements Coxinha {

    @Override
    public String getSabor() {
        return "FRANGO";
    }

    @Override
    public double getPrecoBase() {
        return 8.0;
    }
}
