import { useEffect, useState } from 'react';
import { api, clearAuth, logado } from './api.js';
import Login from './Login.jsx';
import Pedidos from './Pedidos.jsx';
import Clientes from './Clientes.jsx';

export default function App() {
  const [user, setUser] = useState(null);
  const [aba, setAba] = useState('pedidos');

  // Restaura a sessão (credenciais em sessionStorage) ao recarregar a página
  useEffect(() => {
    if (logado()) api('/auth/me').then(setUser).catch(() => clearAuth());
  }, []);

  if (!user) return <Login onLogin={setUser} />;

  const sair = () => { clearAuth(); setUser(null); };
  return (
    <>
      <header>
        <span>Aluguel de Carros — {user.login} ({user.papel})</span>
        <button onClick={() => setAba('pedidos')}>Pedidos</button>
        <button onClick={() => setAba('clientes')}>{user.papel === 'CLIENTE' ? 'Meu cadastro' : 'Clientes'}</button>
        <button onClick={sair}>Sair</button>
      </header>
      <main>
        {aba === 'pedidos'
          ? <Pedidos user={user} />
          : <Clientes user={user} onExcluidoProprio={sair} />}
      </main>
    </>
  );
}
