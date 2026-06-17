package com.bancacoxinha.command;

import com.bancacoxinha.model.Movimentacao;

public interface TransacaoCommand {

    void executar();

    void desfazer();

    Movimentacao getResultado();

    Long getClienteId();
}
