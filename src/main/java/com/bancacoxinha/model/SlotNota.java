package com.bancacoxinha.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "slot_nota")
public class SlotNota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer denominacao;

    @Column(nullable = false)
    private Integer quantidade;

    protected SlotNota() {
    }

    public SlotNota(Integer denominacao, Integer quantidade) {
        this.denominacao = denominacao;
        this.quantidade = quantidade;
    }

    public void adicionar(int unidades) {
        this.quantidade += unidades;
    }

    public void remover(int unidades) {
        if (unidades > this.quantidade) {
            throw new IllegalStateException("Quantidade insuficiente no slot " + denominacao);
        }
        this.quantidade -= unidades;
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
}
