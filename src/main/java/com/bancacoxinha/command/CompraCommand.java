package com.bancacoxinha.command;

import com.bancacoxinha.factory.Coxinha;
import com.bancacoxinha.model.Cliente;
import com.bancacoxinha.model.Movimentacao;
import com.bancacoxinha.service.CaixaOperacoes;
import com.bancacoxinha.strategy.CalculoPrecoStrategy;

import java.util.List;

public class CompraCommand implements TransacaoCommand {

    private final CaixaOperacoes caixaOperacoes;
    private final Cliente cliente;
    private final Coxinha coxinha;
    private final CalculoPrecoStrategy estrategia;
    private final List<Integer> notasPagas;
    private final boolean exigirTrocoExato;
    private final int quantidade;

    private Movimentacao compra;
    private Movimentacao estorno;

    public CompraCommand(CaixaOperacoes caixaOperacoes, Cliente cliente, Coxinha coxinha,
                         CalculoPrecoStrategy estrategia, List<Integer> notasPagas, boolean exigirTrocoExato,
                         int quantidade) {
        this.caixaOperacoes = caixaOperacoes;
        this.cliente = cliente;
        this.coxinha = coxinha;
        this.estrategia = estrategia;
        this.notasPagas = notasPagas;
        this.exigirTrocoExato = exigirTrocoExato;
        this.quantidade = quantidade;
    }

    @Override
    public void executar() {
        this.compra = caixaOperacoes.registrarCompra(cliente, coxinha, estrategia, notasPagas, exigirTrocoExato, quantidade);
    }

    @Override
    public void desfazer() {
        if (compra == null) {
            return;
        }
        this.estorno = caixaOperacoes.reverter(compra.getId());
    }

    @Override
    public Movimentacao getResultado() {
        return estorno != null ? estorno : compra;
    }

    @Override
    public Long getClienteId() {
        return cliente.getId();
    }

    public Movimentacao getCompra() {
        return compra;
    }
}
