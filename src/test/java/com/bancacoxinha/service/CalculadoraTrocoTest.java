package com.bancacoxinha.service;

import com.bancacoxinha.exception.TransacaoImpossivelException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculadoraTrocoTest {

    private final CalculadoraTroco calculadora = new CalculadoraTroco();

    @Test
    void trocoZeroNaoUsaCedulas() {
        assertTrue(calculadora.calcularTroco(0, estoqueCompleto()).isEmpty());
    }

    @Test
    void trocoParUsaMaioresCedulasPrimeiro() {
        Map<Integer, Integer> resultado = calculadora.calcularTroco(12, estoqueCompleto());
        assertEquals(1, resultado.get(10));
        assertEquals(1, resultado.get(2));
        assertEquals(12, soma(resultado));
    }

    @Test
    void trocoImparUsaUmaCedulaDeCinco() {
        Map<Integer, Integer> resultado = calculadora.calcularTroco(11, estoqueCompleto());
        assertEquals(1, resultado.get(5));
        assertEquals(11, soma(resultado));
    }

    @Test
    void trocoImparSemCedulaDeCincoFalha() {
        Map<Integer, Integer> estoque = estoqueCompleto();
        estoque.put(5, 0);
        assertThrows(TransacaoImpossivelException.class, () -> calculadora.calcularTroco(11, estoque));
    }

    @Test
    void trocoDeCemUsaVinteCedulasDeCincoQuandoSoHaCincos() {
        Map<Integer, Integer> estoque = new HashMap<>();
        estoque.put(5, 20);
        Map<Integer, Integer> resultado = calculadora.calcularTroco(100, estoque);
        assertEquals(20, resultado.get(5));
        assertEquals(100, soma(resultado));
    }

    @Test
    void trocoParUsaDuasCedulasDeCincoQuandoNecessario() {
        Map<Integer, Integer> estoque = new HashMap<>();
        estoque.put(5, 10);
        Map<Integer, Integer> resultado = calculadora.calcularTroco(10, estoque);
        assertEquals(2, resultado.get(5));
        assertEquals(10, soma(resultado));
    }

    @Test
    void trocoSeisNaoFicaPresoNaCedulaDeCinco() {
        Map<Integer, Integer> resultado = calculadora.calcularTroco(6, estoqueCompleto());
        assertEquals(3, resultado.get(2));
        assertEquals(6, soma(resultado));
    }

    @Test
    void trocoRespeitaQuantidadeDisponivelEUsaCedulasMenores() {
        Map<Integer, Integer> estoque = new HashMap<>();
        estoque.put(20, 1);
        estoque.put(2, 50);
        Map<Integer, Integer> resultado = calculadora.calcularTroco(30, estoque);
        assertEquals(30, soma(resultado));
        assertTrue(resultado.getOrDefault(20, 0) <= 1);
    }

    @Test
    void trocoComposeValorAltoComCombinacaoMista() {
        Map<Integer, Integer> resultado = calculadora.calcularTroco(238, estoqueCompleto());
        assertEquals(238, soma(resultado));
    }

    @Test
    void trocoSemCedulasSuficientesFalha() {
        Map<Integer, Integer> estoque = new HashMap<>();
        estoque.put(2, 1);
        assertThrows(TransacaoImpossivelException.class, () -> calculadora.calcularTroco(8, estoque));
    }

    @Test
    void trocoImparMenorQueCincoFalha() {
        assertThrows(TransacaoImpossivelException.class, () -> calculadora.calcularTroco(1, estoqueCompleto()));
        assertThrows(TransacaoImpossivelException.class, () -> calculadora.calcularTroco(3, estoqueCompleto()));
    }

    @Test
    void trocoCaixaVazioFalha() {
        assertThrows(TransacaoImpossivelException.class, () -> calculadora.calcularTroco(2, new HashMap<>()));
    }

    @Test
    void devolverTrocoExatoNaoGeraCredito() {
        ResultadoTroco resultado = calculadora.devolverTroco(12, estoqueCompleto());
        assertEquals(12, resultado.valorDevolvido());
        assertEquals(0, resultado.restanteEmCredito());
        assertEquals(12, soma(resultado.notas()));
    }

    @Test
    void devolverTrocoSemCedulaDeDoisCreditaORestante() {
        Map<Integer, Integer> estoque = estoqueCompleto();
        estoque.put(2, 0);
        ResultadoTroco resultado = calculadora.devolverTroco(2, estoque);
        assertTrue(resultado.notas().isEmpty());
        assertEquals(0, resultado.valorDevolvido());
        assertEquals(2, resultado.restanteEmCredito());
    }

    @Test
    void devolverTrocoDevolveOMaximoPossivelECreditaResto() {
        ResultadoTroco resultado = calculadora.devolverTroco(3, estoqueCompleto());
        assertEquals(2, resultado.valorDevolvido());
        assertEquals(1, resultado.restanteEmCredito());
        assertEquals(2, soma(resultado.notas()));
    }

    @Test
    void devolverTrocoLimitadoPeloEstoqueCreditaDiferenca() {
        Map<Integer, Integer> estoque = new HashMap<>();
        estoque.put(10, 1);
        ResultadoTroco resultado = calculadora.devolverTroco(12, estoque);
        assertEquals(10, resultado.valorDevolvido());
        assertEquals(2, resultado.restanteEmCredito());
        assertEquals(1, resultado.notas().get(10));
    }

    private int soma(Map<Integer, Integer> notas) {
        return notas.entrySet().stream().mapToInt(e -> e.getKey() * e.getValue()).sum();
    }

    private Map<Integer, Integer> estoqueCompleto() {
        Map<Integer, Integer> estoque = new HashMap<>();
        estoque.put(2, 20);
        estoque.put(5, 20);
        estoque.put(10, 10);
        estoque.put(20, 10);
        estoque.put(50, 5);
        estoque.put(100, 3);
        estoque.put(200, 2);
        return estoque;
    }
}
