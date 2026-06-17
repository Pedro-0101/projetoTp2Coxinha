package com.bancacoxinha.factory;

public class CoxinhaQueijo implements Coxinha {

    @Override
    public String getSabor() {
        return "QUEIJO";
    }

    @Override
    public double getPrecoBase() {
        return 6.0;
    }
}
