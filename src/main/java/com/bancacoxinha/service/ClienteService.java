package com.bancacoxinha.service;

import com.bancacoxinha.exception.RecursoNaoEncontradoException;
import com.bancacoxinha.exception.RegraNegocioException;
import com.bancacoxinha.model.Cliente;
import com.bancacoxinha.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public Cliente cadastrar(String nome, String login, BigDecimal saldoInicial) {
        clienteRepository.findByLogin(login).ifPresent(existente -> {
            throw new RegraNegocioException("Ja existe um cliente com o login: " + login);
        });
        BigDecimal saldo = (saldoInicial != null ? saldoInicial : BigDecimal.ZERO).setScale(2);
        return clienteRepository.save(new Cliente(nome, login, saldo));
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorLogin(String login) {
        return clienteRepository.findByLogin(login)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado: " + login));
    }
}
