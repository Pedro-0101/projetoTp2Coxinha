package com.bancacoxinha.factory;

public class CoxinhaCarne implements Coxinha {

    @Override
    public String getSabor() {
        return "CARNE";
    }

    @Override
    public double getPrecoBase() {
        return 9.0;
    }
}
