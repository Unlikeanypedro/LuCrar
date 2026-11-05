import { useEffect, useState } from 'react';
import { api, clearAuth, logado } from './api.js';
import Login from './Login.jsx';
import Pedidos from './Pedidos.jsx';
import Clientes from './Clientes.jsx';

export default function App() {
  const [user, setUser] = useState(null);
  const [aba, setAba] = useState('pedidos');
  useEffect(() => { if (logado()) api('/auth/me').then(setUser).catch(() => clearAuth()); }, []);
  if (!user) return <Login onLogin={setUser} />;

  const sair = () => { clearAuth(); setUser(null); };
  const inicial = user.login?.trim()?.[0]?.toUpperCase() || 'U';
  return <div className="app-shell">
    <aside className="sidebar">
      <div className="brand"><span className="brand-mark">L</span><span>LuCrar<small>MOBILITY</small></span></div>
      <div><p className="nav-label">Workspace</p><nav>
        <button className={`nav-button ${aba === 'pedidos' ? 'active' : ''}`} onClick={() => setAba('pedidos')}><span className="nav-icon">▤</span>Pedidos</button>
        <button className={`nav-button ${aba === 'clientes' ? 'active' : ''}`} onClick={() => setAba('clientes')}><span className="nav-icon">♙</span>{user.papel === 'CLIENTE' ? 'Meu cadastro' : 'Clientes'}</button>
      </nav></div>
      <div className="sidebar-foot"><strong>Mobilidade sem complicação</strong>Acompanhe solicitações e contratos em um só lugar.</div>
    </aside>
    <section className="workspace">
      <header className="topbar"><div className="topbar-kicker">Painel de gestão</div><div className="user-chip"><span className="avatar">{inicial}</span><span>{user.login}<small className="muted" style={{display:'block',fontWeight:500}}>{user.papel}</small></span><button className="logout" onClick={sair}>Sair ↗</button></div></header>
      <main className="content">{aba === 'pedidos' ? <Pedidos user={user} /> : <Clientes user={user} onExcluidoProprio={sair} />}</main>
    </section>
  </div>;
}
