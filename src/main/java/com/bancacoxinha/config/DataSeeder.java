package com.bancacoxinha.config;

import com.bancacoxinha.model.Cliente;
import com.bancacoxinha.model.SlotNota;
import com.bancacoxinha.repository.ClienteRepository;
import com.bancacoxinha.repository.SlotNotaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ClienteRepository clienteRepository;
    private final SlotNotaRepository slotNotaRepository;

    public DataSeeder(ClienteRepository clienteRepository, SlotNotaRepository slotNotaRepository) {
        this.clienteRepository = clienteRepository;
        this.slotNotaRepository = slotNotaRepository;
    }

    @Override
    @SuppressWarnings("null")
    public void run(String... args) {
        clienteRepository.save(new Cliente("Cliente Demo", "cliente", valor(0)));
        clienteRepository.save(new Cliente("Cliente VIP", "vip", valor(50)));

        slotNotaRepository.saveAll(List.of(
                new SlotNota(2, 20),
                new SlotNota(5, 20),
                new SlotNota(10, 10),
                new SlotNota(20, 10),
                new SlotNota(50, 5),
                new SlotNota(100, 3),
                new SlotNota(200, 2)
        ));
    }

    private BigDecimal valor(int reais) {
        return BigDecimal.valueOf(reais).setScale(2);
    }
}
