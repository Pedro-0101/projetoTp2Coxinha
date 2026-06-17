package com.bancacoxinha.exception;

public class TransacaoImpossivelException extends RuntimeException {

    public static final String MENSAGEM_PADRAO = "Transação impossível: falta de cédulas específicas";

    public TransacaoImpossivelException() {
        super(MENSAGEM_PADRAO);
    }
}
