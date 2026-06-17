package com.bancacoxinha.factory;

public class CoxinhaCatupiry implements Coxinha {

    @Override
    public String getSabor() {
        return "CATUPIRY";
    }

    @Override
    public double getPrecoBase() {
        return 10.0;
    }
}
