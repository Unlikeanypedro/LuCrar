package com.aluguel.repository;

import com.aluguel.model.Cliente;
import com.aluguel.model.PedidoAluguel;
import com.aluguel.model.StatusPedido;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<PedidoAluguel, Long> {
    List<PedidoAluguel> findByCliente(Cliente cliente);

    void deleteByCliente(Cliente cliente);

    @Query("""
        select count(p) from PedidoAluguel p
        where p.automovel.id = :automovelId
          and p.status in :statuses
          and p.dataInicio < :dataFim
          and p.dataFim > :dataInicio
        """)
    long contarConflitosDePeriodo(
        @Param("automovelId") Long automovelId,
        @Param("statuses") List<StatusPedido> statuses,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    @Query("""
        select count(p) from PedidoAluguel p
        where p.automovel.id = :automovelId
          and p.status in :statuses
          and p.dataInicio < :dataFim
          and p.dataFim > :dataInicio
          and p.id <> :pedidoId
        """)
    long contarConflitosDePeriodoExcetoPedido(
        @Param("automovelId") Long automovelId,
        @Param("statuses") List<StatusPedido> statuses,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim,
        @Param("pedidoId") Long pedidoId
    );
}
