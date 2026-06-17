package com.bancacoxinha.controller;

import com.bancacoxinha.dto.CadastroClienteRequest;
import com.bancacoxinha.dto.ClienteResponse;
import com.bancacoxinha.model.Cliente;
import com.bancacoxinha.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/clientes", produces = "application/json")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping(consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse cadastrar(@Valid @RequestBody CadastroClienteRequest request) {
        Cliente cliente = clienteService.cadastrar(request.nome(), request.login(), request.saldoInicial());
        return paraResponse(cliente);
    }

    @GetMapping
    public List<ClienteResponse> listar() {
        return clienteService.listar().stream().map(this::paraResponse).toList();
    }

    @GetMapping("/{id}")
    public ClienteResponse buscar(@PathVariable Long id) {
        return paraResponse(clienteService.buscarPorId(id));
    }

    private ClienteResponse paraResponse(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getNome(), cliente.getLogin(), cliente.getSaldo());
    }
}
