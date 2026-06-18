package com.bancacoxinha.service;

import com.bancacoxinha.exception.RegraNegocioException;
import com.bancacoxinha.exception.TransacaoImpossivelException;
import com.bancacoxinha.factory.CoxinhaFactory;
import com.bancacoxinha.factory.ItemCoxinha;
import com.bancacoxinha.model.Cliente;
import com.bancacoxinha.model.Movimentacao;
import com.bancacoxinha.model.SlotNota;
import com.bancacoxinha.model.TipoMovimentacao;
import com.bancacoxinha.repository.ClienteRepository;
import com.bancacoxinha.repository.SlotNotaRepository;
import com.bancacoxinha.strategy.PrecoPadrao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class CaixaOperacoesIntegrationTest {

    @Autowired
    private CaixaOperacoes caixaOperacoes;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private SlotNotaRepository slotNotaRepository;
    @Autowired
    private CoxinhaFactory coxinhaFactory;

    private Cliente cliente() {
        return clienteRepository.findByLogin("cliente").orElseThrow();
    }

    private int quantidadeSlot(int denominacao) {
        return slotNotaRepository.findByDenominacao(denominacao).orElseThrow().getQuantidade();
    }

    private List<ItemCoxinha> item(String sabor, int quantidade) {
        return List.of(new ItemCoxinha(coxinhaFactory.criar(sabor), quantidade));
    }

    @Test
    void compraComCedulaQueCobreOPrecoDevolveTrocoEmCedulas() {
        Cliente cliente = cliente();
        int slot10Antes = quantidadeSlot(10);
        int slot2Antes = quantidadeSlot(2);
        int slot20Antes = quantidadeSlot(20);

        Movimentacao mov = caixaOperacoes.registrarCompra(cliente, item("FRANGO", 1), new PrecoPadrao(), List.of(20), false);

        assertEquals(TipoMovimentacao.SAIDA, mov.getTipoMovimentacao());
        assertEquals(12, mov.getTroco().stream().mapToInt(t -> t.getDenominacao() * t.getQuantidade()).sum());
        assertEquals(0, cliente.getSaldo().compareTo(BigDecimal.ZERO));
        assertEquals(slot20Antes + 1, quantidadeSlot(20));
        assertEquals(slot10Antes - 1, quantidadeSlot(10));
        assertEquals(slot2Antes - 1, quantidadeSlot(2));
    }

    @Test
    void compraComMultiplasCedulasSomaOTotalERegistraOPagamento() {
        Cliente cliente = cliente();
        int slot5Antes = quantidadeSlot(5);
        int slot2Antes = quantidadeSlot(2);

        // CARNE custa 9; paga com 5 + 2 + 2 = 9 (exato, sem troco)
        Movimentacao mov = caixaOperacoes.registrarCompra(cliente, item("CARNE", 1), new PrecoPadrao(), List.of(5, 2, 2), false);

        assertEquals(0, mov.getValorNota().compareTo(new BigDecimal("9.00")));
        assertTrue(mov.getTroco().isEmpty());
        assertEquals(9, mov.getPagamento().stream().mapToInt(p -> p.getDenominacao() * p.getQuantidade()).sum());
        assertEquals(slot5Antes + 1, quantidadeSlot(5));
        assertEquals(slot2Antes + 2, quantidadeSlot(2));
        assertEquals(0, cliente.getSaldo().compareTo(BigDecimal.ZERO));
    }

    @Test
    void estornoDeCompraComMultiplasCedulasRestauraEstoque() {
        Cliente cliente = cliente();
        int slot5Antes = quantidadeSlot(5);
        int slot2Antes = quantidadeSlot(2);

        Movimentacao compra = caixaOperacoes.registrarCompra(cliente, item("CARNE", 1), new PrecoPadrao(), List.of(5, 2, 2), false);
        caixaOperacoes.reverter(compra.getId());

        assertEquals(slot5Antes, quantidadeSlot(5));
        assertEquals(slot2Antes, quantidadeSlot(2));
    }

    @Test
    void compraComCedulaMenorQueOPrecoFalha() {
        Cliente cliente = cliente();
        assertThrows(RegraNegocioException.class,
                () -> caixaOperacoes.registrarCompra(cliente, item("CATUPIRY", 1), new PrecoPadrao(), List.of(5), false));
    }

    @Test
    void trocoSemCedulasViraSaldoDoCliente() {
        Cliente cliente = cliente();
        SlotNota slotDois = slotNotaRepository.findByDenominacao(2).orElseThrow();
        slotDois.remover(slotDois.getQuantidade());
        slotNotaRepository.save(slotDois);

        Movimentacao mov = caixaOperacoes.registrarCompra(cliente, item("FRANGO", 1), new PrecoPadrao(), List.of(10), false);

        assertTrue(mov.getTroco().isEmpty());
        assertEquals(0, cliente.getSaldo().compareTo(new BigDecimal("2.00")));
    }

    @Test
    void trocoExatoImpossivelLancaTransacaoImpossivel() {
        Cliente cliente = cliente();
        SlotNota slotDois = slotNotaRepository.findByDenominacao(2).orElseThrow();
        slotDois.remover(slotDois.getQuantidade());
        slotNotaRepository.save(slotDois);

        assertThrows(TransacaoImpossivelException.class,
                () -> caixaOperacoes.registrarCompra(cliente, item("FRANGO", 1), new PrecoPadrao(), List.of(10), true));
    }

    @Test
    void estornoReverteSaldoEEstoque() {
        Cliente cliente = cliente();
        int slot20Antes = quantidadeSlot(20);
        int slot10Antes = quantidadeSlot(10);
        int slot2Antes = quantidadeSlot(2);
        BigDecimal saldoAntes = cliente.getSaldo();

        Movimentacao compra = caixaOperacoes.registrarCompra(cliente, item("FRANGO", 1), new PrecoPadrao(), List.of(20), false);
        Movimentacao estorno = caixaOperacoes.reverter(compra.getId());

        assertEquals(TipoMovimentacao.ESTORNO, estorno.getTipoMovimentacao());
        assertEquals(compra.getId(), estorno.getMovimentacaoOrigemId());
        assertTrue(compra.isEstornada());
        assertEquals(slot20Antes, quantidadeSlot(20));
        assertEquals(slot10Antes, quantidadeSlot(10));
        assertEquals(slot2Antes, quantidadeSlot(2));
        assertEquals(0, cliente.getSaldo().compareTo(saldoAntes));
    }

    @Test
    void estornoDuplicadoFalha() {
        Cliente cliente = cliente();
        Movimentacao compra = caixaOperacoes.registrarCompra(cliente, item("FRANGO", 1), new PrecoPadrao(), List.of(20), false);
        caixaOperacoes.reverter(compra.getId());
        assertThrows(RegraNegocioException.class, () -> caixaOperacoes.reverter(compra.getId()));
    }

    @Test
    void compraComMultiplosSaboresCalculaPrecoTotal() {
        Cliente cliente = cliente();
        // FRANGO (8) x2 + CATUPIRY (10) x1 = 26
        List<ItemCoxinha> itens = List.of(
                new ItemCoxinha(coxinhaFactory.criar("FRANGO"), 2),
                new ItemCoxinha(coxinhaFactory.criar("CATUPIRY"), 1));
        int slot20Antes = quantidadeSlot(20);
        int slot10Antes = quantidadeSlot(10);

        Movimentacao mov = caixaOperacoes.registrarCompra(cliente, itens, new PrecoPadrao(), List.of(20, 10), false);

        assertEquals(0, mov.getValorNota().compareTo(new BigDecimal("30.00")));
        assertEquals(0, mov.getValor().compareTo(new BigDecimal("26.00")));
        assertEquals(4, mov.getTroco().stream().mapToInt(t -> t.getDenominacao() * t.getQuantidade()).sum());
        assertEquals(2, mov.getItens().size());
        assertEquals("FRANGO", mov.getItens().get(0).getSabor());
        assertEquals(2, mov.getItens().get(0).getQuantidade());
        assertEquals("CATUPIRY", mov.getItens().get(1).getSabor());
        assertEquals(1, mov.getItens().get(1).getQuantidade());
        assertEquals(3, mov.getQuantidade());
        assertEquals(slot20Antes + 1, quantidadeSlot(20));
        assertEquals(slot10Antes + 1, quantidadeSlot(10));
    }
}
