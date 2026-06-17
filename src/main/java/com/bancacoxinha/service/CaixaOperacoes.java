package com.bancacoxinha.service;

import com.bancacoxinha.exception.RegraNegocioException;
import com.bancacoxinha.factory.Coxinha;
import com.bancacoxinha.model.CedulaPaga;
import com.bancacoxinha.model.Cliente;
import com.bancacoxinha.model.Movimentacao;
import com.bancacoxinha.model.SlotNota;
import com.bancacoxinha.model.TipoMovimentacao;
import com.bancacoxinha.model.TrocoDetalhe;
import com.bancacoxinha.repository.ClienteRepository;
import com.bancacoxinha.repository.MovimentacaoRepository;
import com.bancacoxinha.repository.SlotNotaRepository;
import com.bancacoxinha.strategy.CalculoPrecoStrategy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CaixaOperacoes {

    private final ClienteRepository clienteRepository;
    private final SlotNotaRepository slotNotaRepository;
    private final MovimentacaoRepository movimentacaoRepository;
    private final CalculadoraTroco calculadoraTroco;

    public CaixaOperacoes(ClienteRepository clienteRepository,
                          SlotNotaRepository slotNotaRepository,
                          MovimentacaoRepository movimentacaoRepository,
                          CalculadoraTroco calculadoraTroco) {
        this.clienteRepository = clienteRepository;
        this.slotNotaRepository = slotNotaRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.calculadoraTroco = calculadoraTroco;
    }

    public Movimentacao registrarCredito(Cliente cliente, int denominacao) {
        SlotNota slot = buscarSlot(denominacao);
        slot.adicionar(1);
        BigDecimal valor = reais(denominacao);
        cliente.creditar(valor);

        clienteRepository.save(cliente);
        slotNotaRepository.save(slot);

        Movimentacao movimentacao = new Movimentacao(cliente, TipoMovimentacao.ENTRADA, valor, null, valor);
        return movimentacaoRepository.save(movimentacao);
    }

    public Movimentacao registrarCompra(Cliente cliente, Coxinha coxinha, CalculoPrecoStrategy estrategia,
                                        List<Integer> notasPagas, boolean exigirTrocoExato) {
        if (notasPagas == null || notasPagas.isEmpty()) {
            throw new RegraNegocioException("Informe ao menos uma cedula para pagamento");
        }
        Map<Integer, Integer> cedulasPagas = new LinkedHashMap<>();
        for (Integer cedula : notasPagas) {
            buscarSlot(cedula);
            cedulasPagas.merge(cedula, 1, Integer::sum);
        }

        BigDecimal preco = reais(estrategia.calcular(coxinha.getPrecoBase()));
        int totalPago = notasPagas.stream().mapToInt(Integer::intValue).sum();
        BigDecimal valorPago = reais(totalPago);

        if (valorPago.compareTo(preco) < 0) {
            throw new RegraNegocioException("As cedulas inseridas (R$ " + totalPago
                    + ") nao cobrem o preco da coxinha de " + coxinha.getSabor() + " (R$ " + preco.toPlainString() + ")");
        }

        cedulasPagas.forEach((denominacao, quantidade) -> buscarSlot(denominacao).adicionar(quantidade));

        int troco = totalPago - preco.intValueExact();
        Map<Integer, Integer> notasTroco = Map.of();
        if (troco > 0) {
            if (exigirTrocoExato) {
                notasTroco = calculadoraTroco.calcularTroco(troco, estoqueDisponivel());
                removerNotasDoCaixa(notasTroco);
            } else {
                ResultadoTroco resultado = calculadoraTroco.devolverTroco(troco, estoqueDisponivel());
                notasTroco = resultado.notas();
                removerNotasDoCaixa(notasTroco);
                if (resultado.restanteEmCredito() > 0) {
                    cliente.creditar(reais(resultado.restanteEmCredito()));
                }
            }
        }

        clienteRepository.save(cliente);
        slotNotaRepository.saveAll(slotNotaRepository.findAll());

        Movimentacao movimentacao = new Movimentacao(cliente, TipoMovimentacao.SAIDA, valorPago, coxinha.getSabor(), preco);
        cedulasPagas.forEach(movimentacao::adicionarPagamento);
        notasTroco.forEach(movimentacao::adicionarTroco);
        return movimentacaoRepository.save(movimentacao);
    }

    public Movimentacao reverter(Long movimentacaoId) {
        Movimentacao original = movimentacaoRepository.findById(movimentacaoId)
                .orElseThrow(() -> new RegraNegocioException("Movimentacao nao encontrada: " + movimentacaoId));
        if (original.getTipoMovimentacao() != TipoMovimentacao.SAIDA) {
            throw new RegraNegocioException("Apenas movimentacoes de compra podem ser estornadas");
        }
        if (original.isEstornada()) {
            throw new RegraNegocioException("Movimentacao " + original.getId() + " ja foi estornada");
        }

        Cliente cliente = original.getCliente();
        BigDecimal notaPaga = original.getValorNota();
        BigDecimal preco = original.getValor();
        BigDecimal totalTroco = totalTroco(original.getTroco());

        cliente.creditar(preco.add(totalTroco).subtract(notaPaga));
        for (CedulaPaga cedula : original.getPagamento()) {
            buscarSlot(cedula.getDenominacao()).remover(cedula.getQuantidade());
        }
        for (TrocoDetalhe detalhe : original.getTroco()) {
            buscarSlot(detalhe.getDenominacao()).adicionar(detalhe.getQuantidade());
        }

        original.marcarEstornada();
        clienteRepository.save(cliente);
        slotNotaRepository.saveAll(slotNotaRepository.findAll());
        movimentacaoRepository.save(original);

        Movimentacao estorno = new Movimentacao(cliente, TipoMovimentacao.ESTORNO, notaPaga, original.getSabor(), preco);
        estorno.setMovimentacaoOrigemId(original.getId());
        return movimentacaoRepository.save(estorno);
    }

    public SlotNota abastecer(int denominacao, int quantidade) {
        if (quantidade <= 0) {
            throw new RegraNegocioException("Quantidade de cedulas deve ser positiva");
        }
        SlotNota slot = buscarSlot(denominacao);
        slot.adicionar(quantidade);
        return slotNotaRepository.save(slot);
    }

    private void removerNotasDoCaixa(Map<Integer, Integer> notas) {
        notas.forEach((denominacao, quantidade) -> buscarSlot(denominacao).remover(quantidade));
    }

    private Map<Integer, Integer> estoqueDisponivel() {
        Map<Integer, Integer> estoque = new HashMap<>();
        for (SlotNota slot : slotNotaRepository.findAll()) {
            estoque.put(slot.getDenominacao(), slot.getQuantidade());
        }
        return estoque;
    }

    private BigDecimal totalTroco(List<TrocoDetalhe> troco) {
        BigDecimal total = BigDecimal.ZERO;
        for (TrocoDetalhe detalhe : troco) {
            total = total.add(reais(detalhe.getDenominacao() * detalhe.getQuantidade()));
        }
        return total;
    }

    private SlotNota buscarSlot(int denominacao) {
        return slotNotaRepository.findByDenominacao(denominacao)
                .orElseThrow(() -> new RegraNegocioException("Cedula invalida: R$ " + denominacao));
    }

    private BigDecimal reais(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal reais(int valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }
}
