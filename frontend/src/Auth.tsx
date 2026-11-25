import { createContext, useContext, useEffect, useState, type FormEvent, type ReactNode } from 'react';
import { ApiError, activateAccount, createAccount, getAccounts, getSession, login, roleNames, type Account, type Role, type Session } from './api';

const AuthContext = createContext<Session | null>(null);
export function useSession() {
  const session = useContext(AuthContext);
  if (!session) throw new Error('Anmeldung erforderlich.');
  return session;
}

export function AuthGate({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const [busy, setBusy] = useState(true);
  const [error, setError] = useState('');
  useEffect(() => {
    let mounted = true;
    const clear = () => { setSession(null); setError(''); };
    window.addEventListener('minierp:expired', clear);
    getSession().then(user => { if (mounted) setSession(user); }).catch(failure => {
      if (mounted && !(failure instanceof ApiError && failure.status === 401)) setError(failure.message);
    }).finally(() => { if (mounted) setBusy(false); });
    return () => { mounted = false; window.removeEventListener('minierp:expired', clear); };
  }, []);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const fields = new FormData(event.currentTarget);
    setBusy(true); setError('');
    try { setSession(await login(String(fields.get('username')), String(fields.get('password')))); }
    catch (failure) { setError(failure instanceof Error ? failure.message : 'Anmeldung fehlgeschlagen.'); }
    finally { setBusy(false); }
  }
  if (session) return <AuthContext.Provider value={session}>{children}</AuthContext.Provider>;
  return <main className="login-screen"><form className="panel account-form" onSubmit={submit}>
    <div className="eyebrow">MiniERP</div><h1>Anmelden</h1>
    <label className="form-field">Benutzername<input name="username" required autoComplete="username" autoCapitalize="none"/></label>
    <label className="form-field">Passwort<input name="password" type="password" required autoComplete="current-password"/></label>
    {error && <div role="alert" className="message error">{error}</div>}
    <button className="primary-button" disabled={busy}>Anmelden</button>
  </form></main>;
}

export function AccountsPage() {
  const session = useSession();
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(true);
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    let mounted = true;
    setBusy(true); setError('');
    getAccounts().then(rows => { if (mounted) setAccounts(rows); }).catch(failure => { if (mounted) setError(failure.message); })
      .finally(() => { if (mounted) setBusy(false); });
    return () => { mounted = false; };
  }, [attempt]);

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const fields = new FormData(form);
    setBusy(true); setError('');
    try {
      const account = await createAccount(String(fields.get('username')), String(fields.get('password')), fields.get('role') as Role);
      setAccounts(rows => [...rows, account]); form.reset();
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'Anlegen fehlgeschlagen.'); }
    finally { setBusy(false); }
  }
  async function toggle(account: Account) {
    setBusy(true); setError('');
    try {
      await activateAccount(account.username, !account.active);
      setAccounts(rows => rows.map(row => row.username === account.username ? { ...row, active: !row.active } : row));
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'Speichern fehlgeschlagen.'); }
    finally { setBusy(false); }
  }
  return <><h1>Benutzerkonten</h1>
    {error && <div className="message error" role="alert">{error}<button onClick={() => setAttempt(n => n + 1)}>Neu laden</button></div>}
    <section className="panel"><div className="table-wrap"><table><thead><tr><th>Benutzername</th><th>Rolle</th><th>Status</th><th>Aktion</th></tr></thead>
      <tbody>{accounts.map(account => <tr key={account.username}><td>{account.username}</td><td>{roleNames[account.role]}</td><td>{account.active ? 'Aktiv' : 'Gesperrt'}</td>
        <td>{account.username !== session.username && <button className="secondary-button" disabled={busy} onClick={() => void toggle(account)}>{account.active ? 'Sperren' : 'Freigeben'}</button>}</td></tr>)}</tbody></table></div></section>
    <form className="panel account-form" onSubmit={create}><h2>Konto anlegen</h2>
      <label className="form-field">Benutzername<input name="username" required minLength={3} maxLength={64} autoComplete="off"/></label>
      <label className="form-field">Passwort (mindestens 12 Zeichen)<input name="password" type="password" required minLength={12} maxLength={256} autoComplete="new-password"/></label>
      <label className="form-field">Rolle<select name="role" defaultValue="SALES">{Object.entries(roleNames).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
      <button className="primary-button" disabled={busy}>Konto anlegen</button>
    </form></>;
}
