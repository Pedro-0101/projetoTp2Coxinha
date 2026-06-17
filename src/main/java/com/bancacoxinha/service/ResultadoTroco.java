package com.bancacoxinha.service;

import java.util.Map;

public record ResultadoTroco(Map<Integer, Integer> notas, int valorDevolvido, int restanteEmCredito) {
}
