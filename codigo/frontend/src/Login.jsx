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
  const [f, setF] = useState({ login: '', senha: '', papel: 'CLIENTE', codigoAgente: '', cliente: vazio, agente: { nome: '', cnpj: '' } });
  const [erro, setErro] = useState('');
  const set = (k, v) => setF({ ...f, [k]: v });
  const setC = (k, v) => setF({ ...f, cliente: { ...f.cliente, [k]: v } });
  const setRendimento = (index, key, value) => setF({
    ...f,
    cliente: {
      ...f.cliente,
      rendimentos: f.cliente.rendimentos.map((item, i) => i === index
        ? { ...item, [key]: key === 'valor' && value !== '' ? Number(value) : value }
        : item),
    },
  });
  const setA = (k, v) => setF({ ...f, agente: { ...f.agente, [k]: v } });
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
        agente: f.papel === 'CLIENTE' ? null : f.agente,
      });
      setAuth(f.login, f.senha);
      try {
        onLogin(await api('/auth/me'));
      } catch {
        clearAuth();
        setErro('Cadastro salvo, mas o servidor recusou o login. Atualize o backend para aplicar a migração do perfil de agente e tente entrar novamente.');
      }
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
          {f.papel === 'CLIENTE' && <>
            <div className="register-fields">{[['nome','Nome completo'],['rg','RG (opcional)'],['cpf','CPF'],['endereco','Endereço'],['profissao','Profissão']].map(([k,label]) => <input key={k} aria-label={label} placeholder={label} value={f.cliente[k]} onChange={e => setC(k, e.target.value)} required={k !== 'rg'} />)}</div>
            <div className="register-income">
              <strong>Empregadores e rendimentos (opcional, até 3)</strong>
              {f.cliente.rendimentos.map((r, i) => <div className="register-income-row" key={i}>
                <input aria-label={`Empregador ${i + 1}`} placeholder="Entidade empregadora" value={r.entidadeEmpregadora} onChange={e => setRendimento(i, 'entidadeEmpregadora', e.target.value)} required />
                <input aria-label={`Rendimento ${i + 1}`} type="number" min="0" step="0.01" placeholder="Rendimento (R$)" value={r.valor} onChange={e => setRendimento(i, 'valor', e.target.value)} required />
                <button type="button" className="secondary" onClick={() => setC('rendimentos', f.cliente.rendimentos.filter((_, j) => j !== i))}>Remover</button>
              </div>)}
              {f.cliente.rendimentos.length < 3 && <button type="button" className="secondary" onClick={() => setC('rendimentos', [...f.cliente.rendimentos, { entidadeEmpregadora: '', valor: '' }])}>+ Adicionar rendimento</button>}
            </div>
          </>}
          {f.papel !== 'CLIENTE' && <>
            <label>Nome da empresa ou banco<input autoComplete="organization" placeholder="Razão social" value={f.agente.nome} onChange={e => setA('nome', e.target.value)} required /></label>
            <label>CNPJ<input inputMode="numeric" autoComplete="off" placeholder="14 números, sem pontuação" maxLength={14} pattern="[0-9]{14}" value={f.agente.cnpj} onChange={e => setA('cnpj', e.target.value.replace(/\D/g, ''))} required /></label>
            <label>Chave de cadastro de agente<input autoComplete="off" placeholder="Chave configurada pelo administrador" value={f.codigoAgente} onChange={e => set('codigoAgente', e.target.value)} required /></label>
            <p className="login-hint">Para testar, use as contas prontas na tela de entrada. Cadastros novos de empresa e banco precisam da chave definida pelo administrador.</p>
          </>}
        </>}
        {erro && <p className="erro" role="alert">{erro}</p>}
        <button type="submit">{cadastro ? 'Criar conta' : 'Entrar'} <span aria-hidden="true">→</span></button>
      </div>
      <div className="login-divider">ou</div>
      {!cadastro && <p className="login-hint demo-logins"><strong>Acessos de demonstração (ambiente local)</strong><br />Empresa: <code>empresa-teste</code> / <code>EMPRESA</code><br />Banco: <code>banco-teste</code> / <code>BANCO</code></p>}
      <button type="button" className="login-toggle" onClick={() => { setCadastro(!cadastro); setErro(''); }}>{cadastro ? 'Já tem uma conta? Entrar' : 'Ainda não tem conta? Criar agora'}</button>
    </form></section>
  </div>;
}
