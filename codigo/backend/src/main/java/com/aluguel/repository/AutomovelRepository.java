package com.aluguel.repository;

import com.aluguel.model.Automovel;
import com.aluguel.model.Usuario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutomovelRepository extends JpaRepository<Automovel, Long> {
    boolean existsByPlacaIgnoreCase(String placa);

    List<Automovel> findByProprietarioUsuario(Usuario usuario);
}
