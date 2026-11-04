package com.aluguel.repository;

import com.aluguel.model.Cliente;
import com.aluguel.model.PedidoAluguel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<PedidoAluguel, Long> {
    List<PedidoAluguel> findByCliente(Cliente cliente);

    void deleteByCliente(Cliente cliente);
}
