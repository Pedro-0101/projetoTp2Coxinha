package com.bancacoxinha.controller;

import com.bancacoxinha.dto.ClienteResponse;
import com.bancacoxinha.dto.LoginRequest;
import com.bancacoxinha.facade.CaixaEletronicoFacade;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/auth", produces = "application/json", consumes = "application/json")
public class AuthController {

    private final CaixaEletronicoFacade facade;

    public AuthController(CaixaEletronicoFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/login")
    public ClienteResponse login(@Valid @RequestBody LoginRequest request) {
        return facade.login(request);
    }
}
