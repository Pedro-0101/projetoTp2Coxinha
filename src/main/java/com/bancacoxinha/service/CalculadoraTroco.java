package com.bancacoxinha.service;

import com.bancacoxinha.exception.TransacaoImpossivelException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CalculadoraTroco {

    private static final int[] DENOMINACOES_DECRESCENTE = {200, 100, 50, 20, 10, 5, 2};

    public Map<Integer, Integer> calcularTroco(int troco, Map<Integer, Integer> disponiveis) {
        if (troco < 0) {
            throw new TransacaoImpossivelException();
        }
        Map<Integer, Integer> notas = tentarCompor(troco, disponiveis);
        if (notas == null) {
            throw new TransacaoImpossivelException();
        }
        return notas;
    }

    public ResultadoTroco devolverTroco(int troco, Map<Integer, Integer> disponiveis) {
        int alvo = Math.max(troco, 0);
        for (int valor = alvo; valor >= 0; valor--) {
            Map<Integer, Integer> notas = tentarCompor(valor, disponiveis);
            if (notas != null) {
                return new ResultadoTroco(notas, valor, alvo - valor);
            }
        }
        return new ResultadoTroco(new LinkedHashMap<>(), 0, alvo);
    }

    private Map<Integer, Integer> tentarCompor(int troco, Map<Integer, Integer> disponiveis) {
        if (troco == 0) {
            return new LinkedHashMap<>();
        }
        Map<Integer, Integer> estoque = new HashMap<>(disponiveis);
        Map<Integer, Integer> resultado = new LinkedHashMap<>();
        return compor(troco, 0, estoque, resultado) ? resultado : null;
    }

    private boolean compor(int restante, int indice, Map<Integer, Integer> estoque, Map<Integer, Integer> resultado) {
        if (restante == 0) {
            return true;
        }
        if (indice >= DENOMINACOES_DECRESCENTE.length) {
            return false;
        }

        int denominacao = DENOMINACOES_DECRESCENTE[indice];
        int disponivel = estoque.getOrDefault(denominacao, 0);
        int maximo = Math.min(restante / denominacao, disponivel);

        for (int quantidade = maximo; quantidade >= 0; quantidade--) {
            if (quantidade > 0) {
                resultado.put(denominacao, quantidade);
            } else {
                resultado.remove(denominacao);
            }
            if (compor(restante - denominacao * quantidade, indice + 1, estoque, resultado)) {
                return true;
            }
        }
        resultado.remove(denominacao);
        return false;
    }
}
