package com.bancacoxinha.observer;

import com.bancacoxinha.command.TransacaoCommand;
import com.bancacoxinha.model.Movimentacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Observador concreto que registra em log toda transacao executada ou desfeita.
 * Por ser apenas um efeito colateral de registro, nao altera nenhum calculo,
 * saldo ou resposta do sistema.
 */
@Component
public class LogTransacaoObserver implements TransacaoObserver {

    private static final Logger log = LoggerFactory.getLogger(LogTransacaoObserver.class);

    @Override
    public void aoExecutar(TransacaoCommand comando) {
        Movimentacao resultado = comando.getResultado();
        log.info("Transacao executada | cliente={} | tipo={} | movimentacaoId={}",
                comando.getClienteId(),
                resultado != null ? resultado.getTipoMovimentacao() : "-",
                resultado != null ? resultado.getId() : "-");
    }

    @Override
    public void aoDesfazer(TransacaoCommand comando) {
        Movimentacao resultado = comando.getResultado();
        log.info("Transacao desfeita | cliente={} | movimentacaoId={}",
                comando.getClienteId(),
                resultado != null ? resultado.getId() : "-");
    }
}
