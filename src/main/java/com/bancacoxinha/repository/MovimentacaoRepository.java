package com.bancacoxinha.repository;

import com.bancacoxinha.model.Movimentacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {

    List<Movimentacao> findByClienteIdOrderByDataHoraDesc(Long clienteId);
}
