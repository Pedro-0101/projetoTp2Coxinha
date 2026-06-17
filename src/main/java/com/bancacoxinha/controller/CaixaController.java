package com.bancacoxinha.controller;

import com.bancacoxinha.dto.AbastecimentoRequest;
import com.bancacoxinha.dto.CompraRequest;
import com.bancacoxinha.dto.CompraResponse;
import com.bancacoxinha.dto.CreditoRequest;
import com.bancacoxinha.dto.CreditoResponse;
import com.bancacoxinha.dto.DesfazerRequest;
import com.bancacoxinha.dto.MovimentacaoResponse;
import com.bancacoxinha.dto.SaborResponse;
import com.bancacoxinha.dto.SlotResponse;
import com.bancacoxinha.dto.TrocaRequest;
import com.bancacoxinha.dto.TrocaResponse;
import com.bancacoxinha.facade.CaixaEletronicoFacade;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/caixa", produces = "application/json")
public class CaixaController {

    private final CaixaEletronicoFacade facade;

    public CaixaController(CaixaEletronicoFacade facade) {
        this.facade = facade;
    }

    @PostMapping(value = "/credito", consumes = "application/json")
    public CreditoResponse inserirCredito(@Valid @RequestBody CreditoRequest request) {
        return facade.inserirCredito(request);
    }

    @PostMapping(value = "/compra", consumes = "application/json")
    public CompraResponse comprar(@Valid @RequestBody CompraRequest request) {
        return facade.comprar(request);
    }

    @PostMapping(value = "/troca", consumes = "application/json")
    public TrocaResponse trocar(@Valid @RequestBody TrocaRequest request) {
        return facade.trocar(request);
    }

    @PostMapping("/desfazer")
    public MovimentacaoResponse desfazerUltima(@RequestBody(required = false) DesfazerRequest request) {
        List<Long> clienteIds = request != null ? request.clienteIdsOuVazio() : List.of();
        return facade.desfazerUltimaTransacao(clienteIds);
    }

    @PostMapping(value = "/abastecer", consumes = "application/json")
    public SlotResponse abastecer(@Valid @RequestBody AbastecimentoRequest request) {
        return facade.abastecerCaixa(request);
    }

    @GetMapping("/slots")
    public List<SlotResponse> slots() {
        return facade.slots();
    }

    @GetMapping("/sabores")
    public List<SaborResponse> sabores() {
        return facade.sabores();
    }
}
