import { useEffect, useMemo, useState } from 'react';
import { api } from './api.js';
import './catalogo.css';

const proprietarios = {
  CLIENTE: 'Cliente',
  EMPRESA: 'Empresa',
  BANCO: 'Banco',
};

export default function Catalogo({ user, onSolicitar }) {
  const [carros, setCarros] = useState([]);
  const [busca, setBusca] = useState('');
  const [filtroProprietario, setFiltroProprietario] = useState('TODOS');
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');

  useEffect(() => {
    let ativo = true;
    api('/automoveis')
      .then((dados) => {
        if (ativo) setCarros(Array.isArray(dados) ? dados : []);
      })
      .catch((e) => {
        if (ativo) setErro(e.message || 'Não foi possível carregar os automóveis.');
      })
      .finally(() => {
        if (ativo) setCarregando(false);
      });
    return () => { ativo = false; };
  }, []);

  const carrosFiltrados = useMemo(() => {
    const termo = busca.trim().toLocaleLowerCase('pt-BR');
    return carros.filter((carro) => {
      const correspondeTexto = !termo || [carro.marca, carro.modelo, carro.ano, carro.matricula, carro.placa]
        .some((valor) => String(valor ?? '').toLocaleLowerCase('pt-BR').includes(termo));
      const correspondeProprietario = filtroProprietario === 'TODOS' || carro.proprietario === filtroProprietario;
      return correspondeTexto && correspondeProprietario;
    });
  }, [busca, carros, filtroProprietario]);

  const formatarDiaria = (valor) => {
    if (valor == null || valor === '') return 'Consulte a diária';
    return `${Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })} / dia`;
  };

  return (
    <section className="catalog-page" aria-labelledby="catalog-title">
      <div className="catalog-heading">
        <div>
          <span className="catalog-eyebrow">FROTA LUCRAR</span>
          <h1 id="catalog-title">Catálogo de veículos</h1>
          <p>Encontre o automóvel ideal para a sua próxima jornada.</p>
        </div>
        <div className="catalog-count"><strong>{carrosFiltrados.length}</strong><span>veículos</span></div>
      </div>

      <div className="catalog-toolbar">
        <label className="catalog-search">
          <span aria-hidden="true">⌕</span>
          <input
            type="search"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            placeholder="Buscar por marca, modelo, ano ou placa"
            aria-label="Buscar veículos"
          />
        </label>
        <label className="catalog-filter">
          <span>Proprietário</span>
          <select value={filtroProprietario} onChange={(e) => setFiltroProprietario(e.target.value)}>
            <option value="TODOS">Todos</option>
            <option value="EMPRESA">Empresa</option>
            <option value="BANCO">Banco</option>
            <option value="CLIENTE">Cliente</option>
          </select>
        </label>
      </div>

      {erro && <div className="erro catalog-error" role="alert">{erro}</div>}
      {carregando && <div className="catalog-state" role="status">Carregando veículos...</div>}
      {!carregando && !erro && carrosFiltrados.length === 0 && (
        <div className="catalog-state">Nenhum veículo encontrado para esses filtros.</div>
      )}

      {!carregando && carrosFiltrados.length > 0 && (
        <div className="catalog-grid">
          {carrosFiltrados.map((carro) => {
            const tipoProprietario = proprietarios[carro.proprietario] || 'Não informado';
            return (
              <article className="vehicle-card" key={carro.id}>
                <div className="vehicle-card-content">
                  <div className="vehicle-meta"><span>{carro.ano || 'Ano não informado'}</span><span>Matrícula {carro.matricula || '—'}</span></div>
                  <h2>{carro.marca} <span>{carro.modelo}</span></h2>
                  <p className="vehicle-plate">Placa {carro.placa || 'não informada'}</p>
                  <div className="vehicle-owner">
                    <span className="owner-icon" aria-hidden="true">⌂</span>
                    <span>Proprietário <strong>{tipoProprietario}</strong></span>
                  </div>
                  <div className="vehicle-card-footer">
                    <strong className="vehicle-price">{formatarDiaria(carro.valorDiaria)}</strong>
                    {user?.papel === 'CLIENTE' && (
                      <button type="button" onClick={() => onSolicitar?.(carro.id)}>Solicitar <span aria-hidden="true">→</span></button>
                    )}
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
}
