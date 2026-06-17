package com.bancacoxinha.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String login;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldo;

    protected Cliente() {
    }

    public Cliente(String nome, String login, BigDecimal saldo) {
        this.nome = nome;
        this.login = login;
        this.saldo = saldo;
    }

    public void creditar(BigDecimal valor) {
        this.saldo = this.saldo.add(valor);
    }

    public void debitar(BigDecimal valor) {
        this.saldo = this.saldo.subtract(valor);
    }

    public boolean possuiSaldo(BigDecimal valor) {
        return this.saldo.compareTo(valor) >= 0;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getLogin() {
        return login;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }
}
