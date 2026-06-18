package com.bancacoxinha.service;

import com.bancacoxinha.exception.RegraNegocioException;
import com.bancacoxinha.factory.ItemCoxinha;
import com.bancacoxinha.model.CedulaPaga;
import com.bancacoxinha.model.Cliente;
import com.bancacoxinha.model.ItemCompra;
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
import java.util.ArrayList;
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

        Movimentacao movimentacao = new Movimentacao(cliente, TipoMovimentacao.ENTRADA, valor, null, valor, 1);
        return movimentacaoRepository.save(movimentacao);
    }

    public Movimentacao registrarCompra(Cliente cliente, List<ItemCoxinha> itens, CalculoPrecoStrategy estrategia,
                                        List<Integer> notasPagas, boolean exigirTrocoExato) {
        if (notasPagas == null || notasPagas.isEmpty()) {
            throw new RegraNegocioException("Informe ao menos uma cedula para pagamento");
        }
        if (itens == null || itens.isEmpty()) {
            throw new RegraNegocioException("Informe ao menos um item para compra");
        }

        Map<Integer, Integer> cedulasPagas = new LinkedHashMap<>();
        for (Integer cedula : notasPagas) {
            buscarSlot(cedula);
            cedulasPagas.merge(cedula, 1, Integer::sum);
        }

        List<ItemCompra> itensCompra = new ArrayList<>();
        BigDecimal precoTotal = BigDecimal.ZERO;
        for (ItemCoxinha item : itens) {
            BigDecimal precoUnitario = reais(estrategia.calcular(item.coxinha().getPrecoBase()));
            BigDecimal subtotal = precoUnitario.multiply(reais(item.quantidade()));
            precoTotal = precoTotal.add(subtotal);
            itensCompra.add(new ItemCompra(null, item.coxinha().getSabor(), item.quantidade(), precoUnitario));
        }

        int totalPago = notasPagas.stream().mapToInt(Integer::intValue).sum();
        BigDecimal valorPago = reais(totalPago);

        if (valorPago.compareTo(precoTotal) < 0) {
            throw new RegraNegocioException("As cedulas inseridas (R$ " + totalPago
                    + ") nao cobrem o preco total (R$ " + precoTotal.toPlainString() + ")");
        }

        cedulasPagas.forEach((denominacao, qtd) -> buscarSlot(denominacao).adicionar(qtd));

        int troco = totalPago - precoTotal.intValueExact();
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

        Movimentacao movimentacao = new Movimentacao(cliente, TipoMovimentacao.SAIDA, valorPago, precoTotal, itensCompra);
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

        List<ItemCompra> itensEstorno = original.getItens().stream()
                .map(item -> new ItemCompra(null, item.getSabor(), item.getQuantidade(), item.getPrecoUnitario()))
                .toList();

        Movimentacao estorno = new Movimentacao(cliente, TipoMovimentacao.ESTORNO, notaPaga, preco, itensEstorno);
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
