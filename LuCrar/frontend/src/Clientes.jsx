import { useEffect, useState } from 'react';
import { api } from './api.js';

const campos = ['nome', 'rg', 'cpf', 'endereco', 'profissao'];

export default function Clientes({ user, onExcluidoProprio }) {
  const [lista, setLista] = useState([]);
  const [edit, setEdit] = useState(null);
  const [erro, setErro] = useState('');
  const carregar = () => api('/clientes').then(setLista).catch(e => setErro(e.message));
  useEffect(() => { carregar(); }, []);

  const salvar = async () => {
    try { await api(`/clientes/${edit.id}`, 'PUT', edit); setEdit(null); carregar(); }
    catch (e) { setErro(e.message); }
  };

  const excluir = async c => {
    if (!confirm('Excluir cliente e seus pedidos?')) return;
    try {
      await api(`/clientes/${c.id}`, 'DELETE');
      if (user.papel === 'CLIENTE') onExcluidoProprio(); else carregar();
    } catch (e) { setErro(e.message); }
  };

  const setRend = (i, k, v) => {
    const r = edit.rendimentos.map((x, j) => (j === i ? { ...x, [k]: v } : x));
    setEdit({ ...edit, rendimentos: r });
  };

  return (
    <div className="card">
      <h2>Clientes</h2>
      {erro && <p className="erro">{erro}</p>}
      <table>
        <thead><tr><th>Nome</th><th>CPF</th><th>Profissão</th><th></th></tr></thead>
        <tbody>
          {lista.map(c => (
            <tr key={c.id}>
              <td>{c.nome}</td><td>{c.cpf}</td><td>{c.profissao}</td>
              <td><button onClick={() => setEdit(c)}>Editar</button><button onClick={() => excluir(c)}>Excluir</button></td>
            </tr>
          ))}
        </tbody>
      </table>
      {edit && (
        <div>
          <h3>Editar #{edit.id}</h3>
          {campos.map(k => (
            <input key={k} placeholder={k} value={edit[k] ?? ''} onChange={e => setEdit({ ...edit, [k]: e.target.value })} />
          ))}
          <h4>Rendimentos (máx. 3)</h4>
          {edit.rendimentos.map((r, i) => (
            <div key={i}>
              <input placeholder="Empregador" value={r.entidadeEmpregadora ?? ''} onChange={e => setRend(i, 'entidadeEmpregadora', e.target.value)} />
              <input placeholder="Valor" type="number" value={r.valor ?? ''} onChange={e => setRend(i, 'valor', e.target.value)} />
            </div>
          ))}
          {edit.rendimentos.length < 3 && (
            <button onClick={() => setEdit({ ...edit, rendimentos: [...edit.rendimentos, { entidadeEmpregadora: '', valor: '' }] })}>
              + Rendimento
            </button>
          )}
          <div><button onClick={salvar}>Salvar</button><button onClick={() => setEdit(null)}>Cancelar</button></div>
        </div>
      )}
    </div>
  );
}
