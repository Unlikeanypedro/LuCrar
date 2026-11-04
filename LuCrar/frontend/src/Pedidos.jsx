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
      {!agente && (
        <div className="card">
          <h2>{editId ? `Modificar pedido #${editId}` : 'Novo pedido de aluguel'}</h2>
          <select value={f.automovelId} onChange={e => setF({ ...f, automovelId: e.target.value })}>
            <option value="">Automóvel...</option>
            {carros.map(c => <option key={c.id} value={c.id}>{c.marca} {c.modelo} {c.ano} ({c.placa})</option>)}
          </select>
          <input type="date" value={f.dataInicio} onChange={e => setF({ ...f, dataInicio: e.target.value })} />
          <input type="date" value={f.dataFim} onChange={e => setF({ ...f, dataFim: e.target.value })} />
          <button onClick={salvar}>{editId ? 'Salvar' : 'Criar'}</button>
          {editId && <button onClick={() => { setEditId(null); setF(novo); }}>Cancelar edição</button>}
        </div>
      )}
      <div className="card">
        <h2>Pedidos</h2>
        {erro && <p className="erro">{erro}</p>}
        <table>
          <thead><tr><th>#</th>{agente && <th>Cliente</th>}<th>Carro</th><th>Período</th><th>Status</th><th>Crédito</th><th></th></tr></thead>
          <tbody>
            {pedidos.map(p => (
              <tr key={p.id}>
                <td>{p.id}</td>
                {agente && <td>{p.cliente.nome}</td>}
                <td>{p.automovel.marca} {p.automovel.modelo}</td>
                <td>{p.dataInicio} → {p.dataFim}</td>
                <td>{p.status}</td>
                <td>{p.credito ? `R$ ${p.credito.valor} (${p.credito.banco.login})` : '-'}</td>
                <td>
                  {!agente && p.status === 'PENDENTE' && <button onClick={() => editar(p)}>Modificar</button>}
                  {!agente && !['CANCELADO', 'CONTRATADO'].includes(p.status) &&
                    <button onClick={() => acao(() => api(`/pedidos/${p.id}`, 'DELETE'))}>Cancelar</button>}
                  {agente && p.status === 'PENDENTE' && <>
                    <button onClick={() => aprovar(p)}>Aprovar</button>
                    <button onClick={() => acao(() => api(`/pedidos/${p.id}/avaliacao`, 'POST', { aprovado: false }))}>Rejeitar</button>
                  </>}
                  {agente && p.status === 'APROVADO' &&
                    <button onClick={() => acao(() => api(`/pedidos/${p.id}/contrato`, 'POST'))}>Executar contrato</button>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
