import { useState } from 'react';
import { api, clearAuth, setAuth } from './api.js';

const vazio = { nome: '', rg: '', cpf: '', endereco: '', profissao: '', rendimentos: [] };

export default function Login({ onLogin }) {
  const [cadastro, setCadastro] = useState(false);
  const [f, setF] = useState({ login: '', senha: '', papel: 'CLIENTE', cliente: vazio });
  const [erro, setErro] = useState('');
  const set = (k, v) => setF({ ...f, [k]: v });
  const setC = (k, v) => setF({ ...f, cliente: { ...f.cliente, [k]: v } });

  const entrar = async () => {
    setAuth(f.login, f.senha);
    try { onLogin(await api('/auth/me')); }
    catch { clearAuth(); setErro('Usuário ou senha inválidos'); }
  };

  const cadastrar = async () => {
    try {
      await api('/auth/register', 'POST', { ...f, cliente: f.papel === 'CLIENTE' ? f.cliente : null });
      await entrar();
    } catch (e) { setErro(e.message); }
  };

  return (
    <main>
      <div className="card">
        <h2>{cadastro ? 'Cadastro' : 'Login'}</h2>
        <input placeholder="Usuário" value={f.login} onChange={e => set('login', e.target.value)} />
        <input placeholder="Senha" type="password" value={f.senha} onChange={e => set('senha', e.target.value)} />
        {cadastro && (
          <>
            <select value={f.papel} onChange={e => set('papel', e.target.value)}>
              <option value="CLIENTE">Cliente</option>
              <option value="EMPRESA">Agente - Empresa</option>
              <option value="BANCO">Agente - Banco</option>
            </select>
            {f.papel === 'CLIENTE' && ['nome', 'rg', 'cpf', 'endereco', 'profissao'].map(k => (
              <input key={k} placeholder={k} value={f.cliente[k]} onChange={e => setC(k, e.target.value)} />
            ))}
          </>
        )}
        <div>
          <button onClick={cadastro ? cadastrar : entrar}>{cadastro ? 'Cadastrar' : 'Entrar'}</button>
          <button onClick={() => { setCadastro(!cadastro); setErro(''); }}>
            {cadastro ? 'Já tenho conta' : 'Criar conta'}
          </button>
        </div>
        {erro && <p className="erro">{erro}</p>}
      </div>
    </main>
  );
}
