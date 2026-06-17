package com.bancacoxinha.command;

import com.bancacoxinha.factory.Coxinha;
import com.bancacoxinha.model.Cliente;
import com.bancacoxinha.model.Movimentacao;
import com.bancacoxinha.model.CedulaPaga;
import com.bancacoxinha.service.CaixaOperacoes;
import com.bancacoxinha.strategy.CalculoPrecoStrategy;

import java.util.ArrayList;
import java.util.List;

public class TrocaCommand implements TransacaoCommand {

    private final CaixaOperacoes caixaOperacoes;
    private final Cliente cliente;
    private final Movimentacao compraOriginal;
    private final Coxinha novoSabor;
    private final CalculoPrecoStrategy estrategia;

    private Movimentacao estornoOriginal;
    private Movimentacao novaCompra;
    private Movimentacao estornoDesfazer;

    public TrocaCommand(CaixaOperacoes caixaOperacoes, Cliente cliente, Movimentacao compraOriginal,
                        Coxinha novoSabor, CalculoPrecoStrategy estrategia) {
        this.caixaOperacoes = caixaOperacoes;
        this.cliente = cliente;
        this.compraOriginal = compraOriginal;
        this.novoSabor = novoSabor;
        this.estrategia = estrategia;
    }

    @Override
    public void executar() {
        List<Integer> notasPagas = cedulasDe(compraOriginal);
        this.estornoOriginal = caixaOperacoes.reverter(compraOriginal.getId());
        this.novaCompra = caixaOperacoes.registrarCompra(cliente, novoSabor, estrategia, notasPagas, false);
    }

    @Override
    public void desfazer() {
        if (novaCompra == null) {
            return;
        }
        this.estornoDesfazer = caixaOperacoes.reverter(novaCompra.getId());
    }

    @Override
    public Movimentacao getResultado() {
        return estornoDesfazer != null ? estornoDesfazer : novaCompra;
    }

    @Override
    public Long getClienteId() {
        return cliente.getId();
    }

    private List<Integer> cedulasDe(com.bancacoxinha.model.Movimentacao movimentacao) {
        List<Integer> cedulas = new ArrayList<>();
        for (CedulaPaga pagamento : movimentacao.getPagamento()) {
            for (int i = 0; i < pagamento.getQuantidade(); i++) {
                cedulas.add(pagamento.getDenominacao());
            }
        }
        return cedulas;
    }

    public Movimentacao getEstorno() {
        return estornoOriginal;
    }

    public Movimentacao getNovaCompra() {
        return novaCompra;
    }
}
