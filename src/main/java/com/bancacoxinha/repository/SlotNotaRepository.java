package com.bancacoxinha.repository;

import com.bancacoxinha.model.SlotNota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SlotNotaRepository extends JpaRepository<SlotNota, Long> {

    Optional<SlotNota> findByDenominacao(Integer denominacao);

    List<SlotNota> findAllByOrderByDenominacaoAsc();
}
