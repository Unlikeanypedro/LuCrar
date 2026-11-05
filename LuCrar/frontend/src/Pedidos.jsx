import { useEffect, useState } from 'react';
import { api } from './api.js';

const novo = { automovelId: '', dataInicio: '', dataFim: '' };

export default function Pedidos({ user }) {
  const [pedidos, setPedidos] = useState([]);
  const [carros, setCarros] = useState([]);
  const [f, setF] = useState(novo);
  const [editId, setEditId] = useState(null);
  const [erro, setErro] = useState('');
  const agente = user.papel !== 'CLIENTE';
  const pendentes = pedidos.filter(p => p.status === 'PENDENTE').length;
  const ativos = pedidos.filter(p => !['CANCELADO', 'CONTRATADO', 'REJEITADO'].includes(p.status)).length;

  const carregar = () => api('/pedidos').then(setPedidos).catch(e => setErro(e.message));
  useEffect(() => { carregar(); api('/automoveis').then(setCarros); }, []);

  // Executa uma ação da API e recarrega a lista
  const acao = async (fn) => {
    try { setErro(''); await fn(); carregar(); } catch (e) { setErro(e.message); }
  };

  const salvar = () => acao(async () => {
    const body = { ...f, automovelId: Number(f.automovelId) };
    await (editId ? api(`/pedidos/${editId}`, 'PUT', body) : api('/pedidos', 'POST', body));
    setF(novo); setEditId(null);
  });

  const editar = p => {
    setEditId(p.id);
    setF({ automovelId: p.automovel.id, dataInicio: p.dataInicio, dataFim: p.dataFim });
  };

  const aprovar = p => {
    const credito = user.papel === 'BANCO' ? prompt('Valor do crédito (opcional):') : null;
    acao(() => api(`/pedidos/${p.id}/avaliacao`, 'POST', { aprovado: true, valorCredito: credito ? Number(credito) : null }));
  };

  return (
    <>
      <div className="page-heading"><div><h1>{agente ? 'Pedidos recebidos' : 'Meus pedidos'}</h1><p>{agente ? 'Analise solicitações e acompanhe contratos.' : 'Acompanhe suas solicitações de aluguel.'}</p></div></div>
      <div className="stats-grid">
        <div className="stat-card"><span><i className="stat-dot" />Pedidos ativos</span><strong>{ativos}</strong></div>
        <div className="stat-card"><span><i className="stat-dot" style={{background:'#e6bf58'}} />Aguardando análise</span><strong>{pendentes}</strong></div>
        <div className="stat-card"><span><i className="stat-dot" style={{background:'#7a9ce0'}} />Total de pedidos</span><strong>{pedidos.length}</strong></div>
      </div>
      {!agente && (
        <div className="card">
          <h2>{editId ? `Modificar pedido #${editId}` : 'Solicitar um aluguel'}</h2>
          <div className="form-grid">
          <select value={f.automovelId} onChange={e => setF({ ...f, automovelId: e.target.value })}>
            <option value="">Automóvel...</option>
            {carros.map(c => <option key={c.id} value={c.id}>{c.marca} {c.modelo} {c.ano} ({c.placa})</option>)}
          </select>
          <input type="date" value={f.dataInicio} onChange={e => setF({ ...f, dataInicio: e.target.value })} />
          <input type="date" value={f.dataFim} onChange={e => setF({ ...f, dataFim: e.target.value })} />
          <button onClick={salvar}>{editId ? 'Salvar alterações' : 'Enviar pedido'}</button>
          {editId && <button className="secondary" onClick={() => { setEditId(null); setF(novo); }}>Cancelar</button>}
          </div>
        </div>
      )}
      <div className="card">
        <h2>Solicitações recentes</h2>
        {erro && <p className="erro">{erro}</p>}
        <div className="table-wrap"><table>
          <thead><tr><th>#</th>{agente && <th>Cliente</th>}<th>Carro</th><th>Período</th><th>Status</th><th>Crédito</th><th></th></tr></thead>
          <tbody>
            {pedidos.map(p => (
              <tr key={p.id}>
                <td className="muted">#{String(p.id).padStart(4, '0')}</td>
                {agente && <td className="row-title">{p.cliente.nome}</td>}
                <td><span className="row-title">{p.automovel.marca} {p.automovel.modelo}</span><span className="muted" style={{display:'block'}}>{p.automovel.ano} · {p.automovel.placa}</span></td>
                <td>{p.dataInicio} <span className="muted">→</span> {p.dataFim}</td>
                <td><span className={`badge badge-${String(p.status).toLowerCase()}`}>{p.status}</span></td>
                <td>{p.credito ? `R$ ${p.credito.valor} (${p.credito.banco.login})` : '-'}</td>
                <td><div className="actions">
                  {!agente && p.status === 'PENDENTE' && <button className="secondary" onClick={() => editar(p)}>Editar</button>}
                  {!agente && !['CANCELADO', 'CONTRATADO'].includes(p.status) &&
                    <button className="danger" onClick={() => acao(() => api(`/pedidos/${p.id}`, 'DELETE'))}>Cancelar</button>}
                  {agente && p.status === 'PENDENTE' && <>
                    <button onClick={() => aprovar(p)}>Aprovar</button>
                    <button className="danger" onClick={() => acao(() => api(`/pedidos/${p.id}/avaliacao`, 'POST', { aprovado: false }))}>Rejeitar</button>
                  </>}
                  {agente && p.status === 'APROVADO' &&
                    <button onClick={() => acao(() => api(`/pedidos/${p.id}/contrato`, 'POST'))}>Executar</button>}
                </div></td>
              </tr>
            ))}
          </tbody>
        </table>{pedidos.length === 0 && <div className="empty-state">Ainda não há pedidos por aqui. Suas solicitações aparecerão nesta lista.</div>}</div>
      </div>
    </>
  );
}
