package com.bancacoxinha.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "movimentacao")
public class Movimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimentacao tipoMovimentacao;

    @Column(precision = 12, scale = 2)
    private BigDecimal valorNota;

    private String sabor;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    private Long movimentacaoOrigemId;

    @Column(nullable = false)
    private boolean estornada = false;

    @OneToMany(mappedBy = "movimentacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrocoDetalhe> troco = new ArrayList<>();

    @OneToMany(mappedBy = "movimentacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CedulaPaga> pagamento = new ArrayList<>();

    protected Movimentacao() {
    }

    public Movimentacao(Cliente cliente, TipoMovimentacao tipoMovimentacao, BigDecimal valorNota,
                        String sabor, BigDecimal valor) {
        this.dataHora = LocalDateTime.now();
        this.cliente = cliente;
        this.tipoMovimentacao = tipoMovimentacao;
        this.valorNota = valorNota;
        this.sabor = sabor;
        this.valor = valor;
    }

    public void adicionarTroco(int denominacao, int quantidade) {
        this.troco.add(new TrocoDetalhe(denominacao, quantidade, this));
    }

    public void adicionarPagamento(int denominacao, int quantidade) {
        this.pagamento.add(new CedulaPaga(denominacao, quantidade, this));
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public TipoMovimentacao getTipoMovimentacao() {
        return tipoMovimentacao;
    }

    public BigDecimal getValorNota() {
        return valorNota;
    }

    public String getSabor() {
        return sabor;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public Long getMovimentacaoOrigemId() {
        return movimentacaoOrigemId;
    }

    public void setMovimentacaoOrigemId(Long movimentacaoOrigemId) {
        this.movimentacaoOrigemId = movimentacaoOrigemId;
    }

    public boolean isEstornada() {
        return estornada;
    }

    public void marcarEstornada() {
        this.estornada = true;
    }

    public List<TrocoDetalhe> getTroco() {
        return troco;
    }

    public List<CedulaPaga> getPagamento() {
        return pagamento;
    }
}
