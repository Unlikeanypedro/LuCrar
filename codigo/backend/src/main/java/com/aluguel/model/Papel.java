package com.aluguel.model;

public enum Papel {
    CLIENTE, EMPRESA, BANCO;

    /** Agentes (empresas e bancos) avaliam pedidos. */
    public boolean isAgente() {
        return this != CLIENTE;
    }
}
