package com.aluguel.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class Rendimento {

    @NotBlank
    private String entidadeEmpregadora;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal valor;
}
