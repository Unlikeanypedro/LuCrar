let auth = sessionStorage.getItem('auth') || '';

export function setAuth(login, senha) {
  auth = 'Basic ' + btoa(unescape(encodeURIComponent(`${login}:${senha}`)));
  sessionStorage.setItem('auth', auth);
}

export function clearAuth() {
  auth = '';
  sessionStorage.removeItem('auth');
}

export const logado = () => !!auth;

export async function api(path, method = 'GET', body) {
  const r = await fetch('/api' + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(auth && { Authorization: auth }) },
    body: body && JSON.stringify(body),
  });
  if (!r.ok) {
    const e = await r.json().catch(() => ({}));
    throw new Error(e.message || e.error || `Erro ${r.status}`);
  }
  return r.status === 204 ? null : r.json();
}
