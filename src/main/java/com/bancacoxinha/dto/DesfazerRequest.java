package com.bancacoxinha.dto;

import java.util.List;

public record DesfazerRequest(List<Long> clienteIds) {

    public List<Long> clienteIdsOuVazio() {
        return clienteIds != null ? clienteIds : List.of();
    }
}
