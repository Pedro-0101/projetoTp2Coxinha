package com.bancacoxinha.controller;

import com.bancacoxinha.dto.MovimentacaoResponse;
import com.bancacoxinha.facade.CaixaEletronicoFacade;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/extrato", produces = "application/json")
public class ExtratoController {

    private final CaixaEletronicoFacade facade;

    public ExtratoController(CaixaEletronicoFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/{clienteId}")
    public List<MovimentacaoResponse> extrato(@PathVariable Long clienteId) {
        return facade.extrato(clienteId);
    }
}
