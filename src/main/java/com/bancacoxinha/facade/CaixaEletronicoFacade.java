package com.bancacoxinha.facade;

import com.bancacoxinha.command.CompraCommand;
import com.bancacoxinha.command.RegistroTransacoes;
import com.bancacoxinha.command.TrocaCommand;
import com.bancacoxinha.dto.AbastecimentoRequest;
import com.bancacoxinha.dto.ClienteResponse;
import com.bancacoxinha.dto.CompraRequest;
import com.bancacoxinha.dto.CompraResponse;
import com.bancacoxinha.dto.CreditoRequest;
import com.bancacoxinha.dto.CreditoResponse;
import com.bancacoxinha.dto.ItemCompraRequest;
import com.bancacoxinha.dto.ItemResponse;
import com.bancacoxinha.dto.LoginRequest;
import com.bancacoxinha.dto.MovimentacaoResponse;
import com.bancacoxinha.dto.SaborResponse;
import com.bancacoxinha.dto.SlotResponse;
import com.bancacoxinha.dto.TrocaRequest;
import com.bancacoxinha.dto.TrocaResponse;
import com.bancacoxinha.dto.TrocoItem;
import com.bancacoxinha.exception.RecursoNaoEncontradoException;
import com.bancacoxinha.factory.Coxinha;
import com.bancacoxinha.factory.CoxinhaFactory;
import com.bancacoxinha.factory.ItemCoxinha;
import com.bancacoxinha.model.Cliente;
import com.bancacoxinha.model.ItemCompra;
import com.bancacoxinha.model.Movimentacao;
import com.bancacoxinha.model.SlotNota;
import com.bancacoxinha.repository.MovimentacaoRepository;
import com.bancacoxinha.repository.SlotNotaRepository;
import com.bancacoxinha.service.CaixaOperacoes;
import com.bancacoxinha.service.ClienteService;
import com.bancacoxinha.strategy.CalculoPrecoStrategy;
import com.bancacoxinha.strategy.PrecoPadrao;
import com.bancacoxinha.strategy.PrecoPromocional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CaixaEletronicoFacade {

    private final ClienteService clienteService;
    private final MovimentacaoRepository movimentacaoRepository;
    private final SlotNotaRepository slotNotaRepository;
    private final CoxinhaFactory coxinhaFactory;
    private final CaixaOperacoes caixaOperacoes;
    private final RegistroTransacoes registroTransacoes;

    public CaixaEletronicoFacade(ClienteService clienteService,
                                 MovimentacaoRepository movimentacaoRepository,
                                 SlotNotaRepository slotNotaRepository,
                                 CoxinhaFactory coxinhaFactory,
                                 CaixaOperacoes caixaOperacoes,
                                 RegistroTransacoes registroTransacoes) {
        this.clienteService = clienteService;
        this.movimentacaoRepository = movimentacaoRepository;
        this.slotNotaRepository = slotNotaRepository;
        this.coxinhaFactory = coxinhaFactory;
        this.caixaOperacoes = caixaOperacoes;
        this.registroTransacoes = registroTransacoes;
    }

    @Transactional(readOnly = true)
    public ClienteResponse login(LoginRequest request) {
        Cliente cliente = clienteService.buscarPorLogin(request.login());
        return paraClienteResponse(cliente);
    }

    @Transactional
    public CreditoResponse inserirCredito(CreditoRequest request) {
        Cliente cliente = buscarCliente(request.clienteId());
        Movimentacao movimentacao = caixaOperacoes.registrarCredito(cliente, request.denominacao());
        SlotNota slot = slotNotaRepository.findByDenominacao(request.denominacao()).orElseThrow();
        return new CreditoResponse(movimentacao.getId(), slot.getDenominacao(), slot.getQuantidade(), cliente.getSaldo());
    }

    @Transactional
    public CompraResponse comprar(CompraRequest request) {
        Cliente cliente = buscarCliente(request.clienteId());
        CalculoPrecoStrategy estrategia = resolverEstrategia(request.promocional());

        List<ItemCoxinha> itens = request.itens().stream()
                .map(item -> new ItemCoxinha(coxinhaFactory.criar(item.sabor()), item.quantidade()))
                .toList();

        CompraCommand comando = new CompraCommand(caixaOperacoes, cliente, itens, estrategia,
                request.notasPagas(), request.trocoExato());
        registroTransacoes.executar(comando);

        Movimentacao movimentacao = comando.getCompra();
        return new CompraResponse(
                movimentacao.getId(),
                paraItensResponse(movimentacao),
                movimentacao.getValor(),
                paraPagamentoItens(movimentacao),
                paraTrocoItens(movimentacao),
                trocoEmCredito(movimentacao),
                cliente.getSaldo());
    }

    @Transactional
    public TrocaResponse trocar(TrocaRequest request) {
        Cliente cliente = buscarCliente(request.clienteId());
        Movimentacao original = movimentacaoRepository.findById(request.movimentacaoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Movimentacao nao encontrada: " + request.movimentacaoId()));
        CalculoPrecoStrategy estrategia = resolverEstrategia(request.promocional());

        List<ItemCoxinha> novosItens = original.getItens().stream()
                .map(item -> {
                    String sabor = item.getSabor().equalsIgnoreCase(request.saborAntigo())
                            ? request.novoSabor()
                            : item.getSabor();
                    return new ItemCoxinha(coxinhaFactory.criar(sabor), item.getQuantidade());
                })
                .toList();

        TrocaCommand comando = new TrocaCommand(caixaOperacoes, cliente, original, novosItens, estrategia);
        registroTransacoes.executar(comando);

        Movimentacao novaCompra = comando.getNovaCompra();
        return new TrocaResponse(
                comando.getEstorno().getId(),
                novaCompra.getId(),
                paraItensResponse(novaCompra),
                novaCompra.getValor(),
                paraTrocoItens(novaCompra),
                trocoEmCredito(novaCompra),
                cliente.getSaldo());
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoResponse> extrato(Long clienteId) {
        buscarCliente(clienteId);
        return movimentacaoRepository.findByClienteIdOrderByDataHoraDesc(clienteId).stream()
                .map(this::paraMovimentacaoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> slots() {
        return slotNotaRepository.findAllByOrderByDenominacaoAsc().stream()
                .map(slot -> new SlotResponse(slot.getDenominacao(), slot.getQuantidade()))
                .toList();
    }

    public List<SaborResponse> sabores() {
        return coxinhaFactory.saboresDisponiveis().stream()
                .map(coxinha -> new SaborResponse(coxinha.getSabor(), BigDecimal.valueOf(coxinha.getPrecoBase())))
                .toList();
    }

    @Transactional
    public MovimentacaoResponse desfazerUltimaTransacao(List<Long> clienteIds) {
        Movimentacao estorno = registroTransacoes.desfazerUltima(clienteIds).getResultado();
        return paraMovimentacaoResponse(estorno);
    }

    @Transactional
    public SlotResponse abastecerCaixa(AbastecimentoRequest request) {
        SlotNota slot = caixaOperacoes.abastecer(request.denominacao(), request.quantidade());
        return new SlotResponse(slot.getDenominacao(), slot.getQuantidade());
    }

    private CalculoPrecoStrategy resolverEstrategia(boolean promocional) {
        return promocional ? new PrecoPromocional() : new PrecoPadrao();
    }

    private Cliente buscarCliente(Long clienteId) {
        return clienteService.buscarPorId(clienteId);
    }

    private ClienteResponse paraClienteResponse(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getNome(), cliente.getLogin(), cliente.getSaldo());
    }

    private BigDecimal trocoEmCredito(Movimentacao movimentacao) {
        BigDecimal somaTroco = movimentacao.getTroco().stream()
                .map(detalhe -> BigDecimal.valueOf((long) detalhe.getDenominacao() * detalhe.getQuantidade()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return movimentacao.getValorNota()
                .subtract(movimentacao.getValor())
                .subtract(somaTroco)
                .setScale(2);
    }

    private List<TrocoItem> paraTrocoItens(Movimentacao movimentacao) {
        return movimentacao.getTroco().stream()
                .map(detalhe -> new TrocoItem(detalhe.getDenominacao(), detalhe.getQuantidade()))
                .toList();
    }

    private List<ItemResponse> paraItensResponse(Movimentacao movimentacao) {
        return movimentacao.getItens().stream()
                .map(item -> new ItemResponse(item.getSabor(), item.getQuantidade(), item.getPrecoUnitario()))
                .toList();
    }

    private MovimentacaoResponse paraMovimentacaoResponse(Movimentacao movimentacao) {
        return new MovimentacaoResponse(
                movimentacao.getId(),
                movimentacao.getDataHora(),
                movimentacao.getTipoMovimentacao().name(),
                movimentacao.getValorNota(),
                movimentacao.getSabor(),
                movimentacao.getQuantidade(),
                movimentacao.getValor(),
                paraPagamentoItens(movimentacao),
                paraTrocoItens(movimentacao),
                movimentacao.getMovimentacaoOrigemId(),
                paraItensResponse(movimentacao));
    }

    private List<TrocoItem> paraPagamentoItens(Movimentacao movimentacao) {
        return movimentacao.getPagamento().stream()
                .map(cedula -> new TrocoItem(cedula.getDenominacao(), cedula.getQuantidade()))
                .toList();
    }
}
