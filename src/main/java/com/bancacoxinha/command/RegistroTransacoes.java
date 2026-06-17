package com.bancacoxinha.command;

import com.bancacoxinha.exception.RegraNegocioException;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Component
public class RegistroTransacoes {

    private final Deque<TransacaoCommand> historico = new ArrayDeque<>();

    public synchronized void executar(TransacaoCommand comando) {
        comando.executar();
        historico.push(comando);
    }

    public synchronized TransacaoCommand desfazerUltima(Collection<Long> clienteIds) {
        TransacaoCommand alvo = (clienteIds == null || clienteIds.isEmpty())
                ? removerUltimaGlobal()
                : removerUltimaDosClientes(clienteIds);
        alvo.desfazer();
        return alvo;
    }

    public synchronized int operacoesRegistradas() {
        return historico.size();
    }

    private TransacaoCommand removerUltimaGlobal() {
        if (historico.isEmpty()) {
            throw new RegraNegocioException("Nenhuma transacao para desfazer");
        }
        return historico.pop();
    }

    private TransacaoCommand removerUltimaDosClientes(Collection<Long> clienteIds) {
        Set<Long> filtro = new HashSet<>(clienteIds);
        Iterator<TransacaoCommand> iterator = historico.iterator();
        while (iterator.hasNext()) {
            TransacaoCommand comando = iterator.next();
            if (filtro.contains(comando.getClienteId())) {
                iterator.remove();
                return comando;
            }
        }
        throw new RegraNegocioException("Nenhuma transacao para desfazer para os clientes informados");
    }
}
