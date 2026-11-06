import { useState } from "react";
import { api, clearAuth, setAuth } from "./api.js";

const vazio = {
    nome: "",
    rg: "",
    cpf: "",
    endereco: "",
    profissao: "",
    rendimentos: [],
};
export default function Login({ onLogin }) {
  const [cadastro, setCadastro] = useState(false);
  const [f, setF] = useState({ login: '', senha: '', papel: 'CLIENTE', codigoAgente: '', cliente: vazio });
  const [erro, setErro] = useState('');
  const set = (k, v) => setF({ ...f, [k]: v });
  const setC = (k, v) => setF({ ...f, cliente: { ...f.cliente, [k]: v } });
  const entrar = async () => {
    setErro(''); setAuth(f.login, f.senha);
    try { onLogin(await api('/auth/me')); } catch { clearAuth(); setErro('Não foi possível entrar. Confira seu usuário e senha.'); }
  };
  const cadastrar = async () => {
    setErro('');
    try {
      await api('/auth/register', 'POST', {
        ...f,
        cliente: f.papel === 'CLIENTE' ? f.cliente : null,
        codigoAgente: f.papel === 'CLIENTE' ? null : f.codigoAgente,
      });
      await entrar();
    }
    catch (e) { setErro(e.message); }
  };
  return <div className="login-page">
    <section className="login-art">
      <div><div className="brand"><span className="brand-mark">L</span><span>LuCrar<small>MOBILITY</small></span></div><h1>O caminho certo começa aqui.</h1><p>Gerencie pedidos, acompanhe análises e transforme planos em viagens.</p></div>
      <div className="login-car" aria-hidden="true">🚘</div>
    </section>
    <section className="login-panel"><form className="login-form" onSubmit={e => { e.preventDefault(); cadastro ? cadastrar() : entrar(); }}>
      <h2>{cadastro ? 'Crie sua conta' : 'Bem-vindo de volta'}</h2><p>{cadastro ? 'Preencha seus dados para começar.' : 'Acesse sua área de mobilidade.'}</p>
      <div className="login-fields">
        <label>Usuário<input autoComplete="username" placeholder="Seu usuário" value={f.login} onChange={e => set('login', e.target.value)} required /></label>
        <label>Senha<input autoComplete={cadastro ? 'new-password' : 'current-password'} placeholder={cadastro ? 'Mínimo de 8 caracteres' : 'Sua senha'} type="password" minLength={cadastro ? 8 : undefined} maxLength={cadastro ? 72 : undefined} value={f.senha} onChange={e => set('senha', e.target.value)} required /></label>
        {cadastro && <>
          <label>Tipo de conta<select value={f.papel} onChange={e => set('papel', e.target.value)}><option value="CLIENTE">Cliente</option><option value="EMPRESA">Agente - Empresa</option><option value="BANCO">Agente - Banco</option></select></label>
          {f.papel === 'CLIENTE' && <div className="register-fields">{[['nome','Nome completo'],['rg','RG (opcional)'],['cpf','CPF'],['endereco','Endereço'],['profissao','Profissão']].map(([k,label]) => <input key={k} aria-label={label} placeholder={label} value={f.cliente[k]} onChange={e => setC(k, e.target.value)} required={k !== 'rg'} />)}</div>}
          {f.papel !== 'CLIENTE' && <label>Chave de cadastro de agente<input autoComplete="off" placeholder="Chave fornecida pelo administrador" value={f.codigoAgente} onChange={e => set('codigoAgente', e.target.value)} required /></label>}
        </>}
        {erro && <p className="erro" role="alert">{erro}</p>}
        <button type="submit">{cadastro ? 'Criar conta' : 'Entrar'} <span aria-hidden="true">→</span></button>
      </div>
      <div className="login-divider">ou</div>
      <button type="button" className="login-toggle" onClick={() => { setCadastro(!cadastro); setErro(''); }}>{cadastro ? 'Já tem uma conta? Entrar' : 'Ainda não tem conta? Criar agora'}</button>
    </form></section>
  </div>;
}
