package com.bancacoxinha.factory;

import com.bancacoxinha.exception.RegraNegocioException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CoxinhaFactory {

    public List<Coxinha> saboresDisponiveis() {
        return List.of(new CoxinhaFrango(), new CoxinhaCatupiry(), new CoxinhaCarne(), new CoxinhaQueijo());
    }

    public Coxinha criar(String sabor) {
        if (sabor == null) {
            throw new RegraNegocioException("Sabor nao informado");
        }
        return switch (sabor.trim().toUpperCase()) {
            case "FRANGO" -> new CoxinhaFrango();
            case "CATUPIRY" -> new CoxinhaCatupiry();
            case "CARNE" -> new CoxinhaCarne();
            case "QUEIJO" -> new CoxinhaQueijo();
            default -> throw new RegraNegocioException("Sabor invalido: " + sabor);
        };
    }
}
