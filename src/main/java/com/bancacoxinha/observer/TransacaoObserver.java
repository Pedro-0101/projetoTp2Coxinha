package com.bancacoxinha.observer;

import com.bancacoxinha.command.TransacaoCommand;

/**
 * Observador (padrao Observer) notificado pelo {@code RegistroTransacoes}
 * sempre que uma transacao e executada ou desfeita.
 *
 * <p>Os metodos sao {@code default} vazios para que cada observador
 * implemente apenas os eventos de seu interesse.</p>
 */
public interface TransacaoObserver {

    default void aoExecutar(TransacaoCommand comando) {
    }

    default void aoDesfazer(TransacaoCommand comando) {
    }
}
