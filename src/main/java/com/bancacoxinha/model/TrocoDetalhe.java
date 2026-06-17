package com.bancacoxinha.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "troco_detalhe")
public class TrocoDetalhe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer denominacao;

    @Column(nullable = false)
    private Integer quantidade;

    @ManyToOne
    @JoinColumn(name = "movimentacao_id", nullable = false)
    private Movimentacao movimentacao;

    protected TrocoDetalhe() {
    }

    public TrocoDetalhe(Integer denominacao, Integer quantidade, Movimentacao movimentacao) {
        this.denominacao = denominacao;
        this.quantidade = quantidade;
        this.movimentacao = movimentacao;
    }

    public Long getId() {
        return id;
    }

    public Integer getDenominacao() {
        return denominacao;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public Movimentacao getMovimentacao() {
        return movimentacao;
    }
}
