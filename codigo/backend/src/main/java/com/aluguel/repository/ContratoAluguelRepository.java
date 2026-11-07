package com.aluguel.repository;

import com.aluguel.model.ContratoAluguel;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContratoAluguelRepository extends JpaRepository<ContratoAluguel, Long> {
    Optional<ContratoAluguel> findByPedidoId(Long pedidoId);
}
