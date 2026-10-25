import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import { ArrowRight, Check, CircleHelp, Command, Database, LayoutDashboard, LoaderCircle, Package, Pencil, Plus, RefreshCw, Search, ShoppingCart, Users, Warehouse, X } from 'lucide-react';
import { ApiError, getCollection, saveCustomer, saveProduct, type ApiStatus, type Customer, type Product, type Resource, type Order, type Stock } from './api';
import { OrdersPage, StockPage, StatusBadge } from './Operations';
import { availableStock, grams, lowStockProducts } from './format';

type Page = 'overview' | Resource | 'orders' | 'stocks';
type RecordItem = Customer | Product;
const navigation = [
  { page: 'overview' as const, label: 'Übersicht', icon: LayoutDashboard },
  { page: 'customers' as const, label: 'Kunden', icon: Users },
  { page: 'products' as const, label: 'Produkte', icon: Package },
  { page: 'orders' as const, label: 'Aufträge', icon: ShoppingCart },
  { page: 'stocks' as const, label: 'Lagerbestand', icon: Warehouse },
];
const euro = new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' });

export default function App() {
  const [page, setPage] = useState<Page>('overview');
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [stocks, setStocks] = useState<Stock[]>([]);
  const [status, setStatus] = useState<ApiStatus>('checking');
  const [loading, setLoading] = useState(true);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const refresh = useCallback(async (signal?: AbortSignal) => {
    setLoading(true);
    setError('');
    try {
      const [nextCustomers, nextProducts, nextOrders, nextStocks] = await Promise.all([
        getCollection<Customer>('customers', signal), getCollection<Product>('products', signal),
        getCollection<Order>('orders', signal), getCollection<Stock>('stocks', signal),
      ]);
      if (signal?.aborted) return;
      setCustomers(nextCustomers);
      setProducts(nextProducts);
      setOrders(nextOrders);
      setStocks(nextStocks);
      setLoaded(true);
      setStatus('online');
    } catch (failure) {
      if (signal?.aborted) return;
      setStatus(failure instanceof ApiError && failure.status > 0 ? 'online' : 'offline');
      setError(failure instanceof Error ? failure.message : 'Die Daten konnten nicht geladen werden.');
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    void refresh(controller.signal);
    return () => controller.abort();
  }, [refresh]);

  useEffect(() => {
    if (!notice) return;
    const timeout = window.setTimeout(() => setNotice(''), 4000);
    return () => window.clearTimeout(timeout);
  }, [notice]);

  function saved(item: RecordItem) {
    if ('customerID' in item) setCustomers(current => [item, ...current.filter(c => c.customerID !== item.customerID)]);
    else setProducts(current => [item, ...current.filter(p => p.productID !== item.productID)]);
    setStatus('online');
    setNotice('Änderungen gespeichert.');
  }

  const label = navigation.find(item => item.page === page)!.label;
  const lowProducts = lowStockProducts(products, stocks);
  return <div className="shell">
    <aside className="sidebar">
      <div className="brand"><div className="brand-mark"><Command size={20}/></div>mini<span>ERP</span><small>BETA</small></div>
      <div className="workspace"><div className="workspace-avatar">M</div><div><b>Mein Unternehmen</b><small>Lokaler Arbeitsplatz</small></div></div>
      <div className="nav-caption">ARBEITSBEREICH</div>
      <nav aria-label="Hauptnavigation">{navigation.map(({ page: next, label: title, icon: Icon }) =>
        <button key={next} className={`nav-item ${page === next ? 'active' : ''}`} onClick={() => setPage(next)} aria-current={page === next ? 'page' : undefined}>
          <Icon size={18}/><span>{title}</span>
        </button>)}</nav>
      <div className="sidebar-bottom"><Database size={17}/><div><b>Deine Daten bleiben lokal</b><small>MiniERP · SQLite</small></div></div>
    </aside>
    <main className="main">
      <header className="topbar"><div className="crumb">Arbeitsbereich <span>/</span> <b>{label}</b></div><div className="top-actions">
        <div className={`connection ${status}`}><span/>{status === 'online' ? 'Backend verbunden' : status === 'checking' ? 'Verbinde…' : 'Backend nicht erreichbar'}</div>
        <button className="secondary-button" onClick={() => void refresh()} disabled={loading}><RefreshCw size={14} className={loading ? 'spin' : ''}/>Aktualisieren</button>
      </div></header>
      <div className="content">
        {error && <div className="message error" role="alert">{error}<button onClick={() => void refresh()} disabled={loading}>Erneut versuchen</button></div>}
        {notice && <div className="toast" role="status"><Check size={16}/>{notice}</div>}
        {!loaded && !loading ? <div className="panel empty"><Database size={28}/><b>Die Daten konnten noch nicht geladen werden.</b><p>Starte die API und wähle anschließend „Aktualisieren“.</p></div> : page === 'overview' ? <>
          <div className="welcome-row"><div><div className="eyebrow">{new Intl.DateTimeFormat('de-DE', { dateStyle: 'full', timeZone: 'Europe/Berlin' }).format(new Date())}</div><h1>Alles im Überblick.</h1><p>Stammdaten, Aufträge und Lager an einem Ort.</p></div><div className="page-symbol"><LayoutDashboard size={25}/></div></div>
          <section className="metrics" aria-label="Kennzahlen">
            <Metric title="Aktive Kunden" count={customers.length} loading={loading} unavailable={status !== 'online' && !customers.length} icon="customers"/>
            <Metric title="Aktive Produkte" count={products.length} loading={loading} unavailable={status !== 'online' && !products.length} icon="products"/>
            <Metric title="Offene Aufträge" count={orders.filter(order => !['DELIVERED', 'CANCELLED'].includes(order.orderStatus)).length} loading={loading} unavailable={status !== 'online' && !orders.length} icon="orders"/>
            <Metric title="Niedriger Bestand" count={lowProducts.length} loading={loading} unavailable={status !== 'online'} icon="stocks"/>
          </section>
          <section className="overview-grid">
            <OverviewList title="Deine Kunden" description="Kontakte und Kundendaten verwalten" items={customers} loading={loading} onOpen={() => setPage('customers')}/>
            <OverviewList title="Deine Produkte" description="Artikel, Preise und Mindestbestände pflegen" items={products} loading={loading} onOpen={() => setPage('products')}/>
          </section>
          <section className="overview-grid">
            <section className="panel"><div className="panel-heading"><div><h2>Letzte Aufträge</h2><p>Die neuesten Vorgänge im Verkauf</p></div><button className="text-link" onClick={() => setPage('orders')}>Öffnen <ArrowRight size={15}/></button></div>{loading ? <div className="empty compact">Daten werden geladen…</div> : !orders.length ? <div className="empty compact">Noch keine Aufträge vorhanden.</div> : [...orders].sort((a, b) => b.orderID - a.orderID).slice(0, 4).map(order => <button className="overview-row" key={order.orderID} onClick={() => setPage('orders')}><span className="record-avatar"><ShoppingCart size={15}/></span><span><b>Auftrag #{String(order.orderID).padStart(4, '0')}</b><small>{euro.format(Number(order.totalPrice))}</small></span><StatusBadge status={order.orderStatus}/></button>)}</section>
            <section className="panel"><div className="panel-heading"><div><h2>Lager im Blick</h2><p>Verfügbarkeit unter dem Mindestbestand</p></div><button className="text-link" onClick={() => setPage('stocks')}>Öffnen <ArrowRight size={15}/></button></div>{loading ? <div className="empty compact">Daten werden geladen…</div> : !lowProducts.length ? <div className="empty compact"><Check size={22}/>Alle Mindestbestände sind gedeckt.</div> : lowProducts.slice(0, 4).map(product => <button className="overview-row" key={product.productID} onClick={() => setPage('stocks')}><span className="record-avatar"><Warehouse size={15}/></span><span><b>{product.name}</b><small>{availableStock(product.productID, stocks)} verfügbar · mindestens {product.minStockThreshold} Stk.</small></span><ArrowRight size={14}/></button>)}</section>
          </section>
          <div className="next-step"><CircleHelp size={20}/><div><b>Bestände und Reservierungen manuell pflegen.</b><p>Die Lagerverwaltung zeigt verfügbare Mengen über alle Lagerplätze. Aufträge ändern den Bestand derzeit nicht automatisch.</p></div></div>
        </> : page === 'orders' ? <OrdersPage orders={orders} customers={customers} products={products} loading={loading} onCustomers={() => setPage('customers')} onProducts={() => setPage('products')} onSaved={order => { setOrders(current => [order, ...current.filter(item => item.orderID !== order.orderID)]); setNotice('Auftrag gespeichert.'); }}/>
          : page === 'stocks' ? <StockPage stocks={stocks} products={products} loading={loading} onProducts={() => setPage('products')} onSaved={stock => { setStocks(current => [stock, ...current.filter(item => item.stockID !== stock.stockID)]); setNotice('Lagerbestand gespeichert.'); }}/>
          : <RecordsPage key={page} kind={page} items={page === 'customers' ? customers : products} loading={loading} onSaved={saved}/>}
        <footer><span><Database size={13}/> Lokale Datenbank</span><span>MiniERP · 0.1.0</span></footer>
      </div>
    </main>
  </div>;
}

function Metric({ title, count, loading, unavailable, icon }: { title: string; count: number; loading: boolean; unavailable: boolean; icon: Exclude<Page, 'overview'> }) {
  const Icon = icon === 'customers' ? Users : icon === 'products' ? Package : icon === 'orders' ? ShoppingCart : Warehouse;
  return <article className="metric"><div className="metric-top"><span>{title}</span><div className={`metric-icon ${icon}`}><Icon size={19}/></div></div><div className="metric-value">{loading ? '…' : unavailable ? '—' : count}</div><div className="metric-foot">Aktuelle Datensätze aus dem Backend</div></article>;
}

function OverviewList({ title, description, items, loading, onOpen }: { title: string; description: string; items: RecordItem[]; loading: boolean; onOpen: () => void }) {
  return <section className="panel"><div className="panel-heading"><div><h2>{title}</h2><p>{description}</p></div><button className="text-link" onClick={onOpen}>Öffnen <ArrowRight size={15}/></button></div>
    {loading ? <div className="empty compact"><LoaderCircle className="spin" size={22}/>Daten werden geladen…</div> : !items.length ? <div className="empty compact">Noch keine Einträge.<button className="text-link" onClick={onOpen}>Ersten Eintrag anlegen <ArrowRight size={14}/></button></div> : <div className="overview-list">{items.slice(0, 5).map(item => <button className="overview-row" onClick={onOpen} key={'customerID' in item ? item.customerID : item.productID}><span className="record-avatar">{item.name.slice(0, 2).toUpperCase()}</span><span><b>{item.name}</b><small>{'email' in item ? item.email : euro.format(Number(item.price))}</small></span><ArrowRight size={14}/></button>)}</div>}
  </section>;
}

function RecordsPage({ kind, items, loading, onSaved }: { kind: Resource; items: RecordItem[]; loading: boolean; onSaved: (item: RecordItem) => void }) {
  const [query, setQuery] = useState('');
  const [editing, setEditing] = useState<RecordItem | null | undefined>(undefined);
  const customer = kind === 'customers';
  const title = customer ? 'Kunden' : 'Produkte';
  const singular = customer ? 'Kunde' : 'Produkt';
  const filtered = items.filter(item => `${item.name} ${'email' in item ? item.email : item.description ?? ''}`.toLocaleLowerCase('de').includes(query.toLocaleLowerCase('de')));
  return <>
    <div className="welcome-row"><div><div className="eyebrow">STAMMDATEN</div><h1>{title}</h1><p>{customer ? 'Alle Kontakte und Kundendaten an einem Ort.' : 'Dein Sortiment, übersichtlich und aktuell.'}</p></div><button className="primary-button" onClick={() => setEditing(null)}><Plus size={16}/> {customer ? 'Neuer Kunde' : 'Neues Produkt'}</button></div>
    <section className="panel records-panel"><div className="list-toolbar"><div className="search"><Search size={16}/><input aria-label={`${title} durchsuchen`} placeholder={`${title} durchsuchen…`} value={query} onChange={event => setQuery(event.target.value)}/>{query && <button aria-label="Suche löschen" onClick={() => setQuery('')}><X size={14}/></button>}</div><span>{filtered.length} {filtered.length === 1 ? singular : title}</span></div>
      {loading ? <div className="empty"><LoaderCircle className="spin" size={24}/>Daten werden geladen…</div> : !filtered.length ? <div className="empty">{customer ? <Users size={28}/> : <Package size={28}/>}<b>{query ? 'Keine passenden Einträge' : `Noch keine ${title} vorhanden`}</b><p>{query ? 'Versuche einen anderen Suchbegriff.' : `Lege jetzt ${customer ? 'deinen ersten Kunden' : 'dein erstes Produkt'} an.`}</p>{!query && <button className="secondary-button" onClick={() => setEditing(null)}><Plus size={15}/>Eintrag anlegen</button>}</div> : <div className="table-wrap"><table><thead><tr><th>{customer ? 'KUNDE' : 'PRODUKT'}</th><th>{customer ? 'E-MAIL' : 'PREIS'}</th><th>{customer ? 'TELEFON' : 'GEWICHT (g)'}</th><th>{customer ? 'ADRESSE' : 'MINDESTBESTAND'}</th><th><span className="sr-only">Aktionen</span></th></tr></thead><tbody>{filtered.map(item => {
        const id = 'customerID' in item ? item.customerID : item.productID;
        return <tr key={id}><td><div className="record-name"><span className="record-avatar">{item.name.slice(0, 2).toUpperCase()}</span><div><b>{item.name}</b><small>#{id}</small></div></div></td><td>{'email' in item ? item.email : euro.format(Number(item.price))}</td><td>{'phone' in item ? item.phone : grams(item.weight)}</td><td>{'address' in item ? item.address : `${item.minStockThreshold} Stk.`}</td><td className="right"><button className="edit-button" aria-label={`${item.name} bearbeiten`} onClick={() => setEditing(item)}><Pencil size={14}/><span>Bearbeiten</span></button></td></tr>;
      })}</tbody></table></div>}
    </section>
    {editing !== undefined && <RecordEditor kind={kind} record={editing} onClose={() => setEditing(undefined)} onSaved={item => { onSaved(item); setEditing(undefined); }}/ >}
  </>;
}

function RecordEditor({ kind, record, onClose, onSaved }: { kind: Resource; record: RecordItem | null; onClose: () => void; onSaved: (item: RecordItem) => void }) {
  const dialog = useRef<HTMLDialogElement>(null);
  const customer = kind === 'customers';
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [fields, setFields] = useState<Record<string, string>>((): Record<string, string> => {
    if (customer) {
      const item = record as Customer | null;
      return { name: item?.name ?? '', address: item?.address ?? '', email: item?.email ?? '', phone: item?.phone ?? '', birthdate: item?.birthdate ?? '' };
    }
    const item = record as Product | null;
    return { name: item?.name ?? '', description: item?.description ?? '', price: String(item?.price ?? ''), weight: String(item?.weight ?? ''), minStockThreshold: String(item?.minStockThreshold ?? 0) };
  });
  useEffect(() => { dialog.current?.showModal(); }, []);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError('');
    try {
      let saved: RecordItem;
      if (customer) saved = await saveCustomer({ name: fields.name, address: fields.address, email: fields.email, phone: fields.phone, birthdate: fields.birthdate || null }, record && 'customerID' in record ? record.customerID : undefined);
      else saved = await saveProduct({ name: fields.name, description: fields.description || null, price: fields.price, weight: fields.weight, minStockThreshold: Number(fields.minStockThreshold) }, record && 'productID' in record ? record.productID : undefined);
      onSaved(saved);
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'Speichern fehlgeschlagen.');
      setBusy(false);
    }
  }

  function field(name: string, label: string, type = 'text', required = true, step?: string) {
    return <label className="form-field" htmlFor={`field-${name}`} key={name}><span>{label}{!required && <small>optional</small>}</span><input id={`field-${name}`} name={name} type={type} required={required} step={step} min={type === 'number' ? '0' : undefined} value={fields[name]} onChange={event => setFields(current => ({ ...current, [name]: event.target.value }))} disabled={busy}/></label>;
  }
  return <dialog ref={dialog} className="editor" aria-labelledby="editor-title" onCancel={event => { event.preventDefault(); if (!busy) onClose(); }}>
    <form onSubmit={event => void submit(event)}><div className="editor-heading"><div><div className="eyebrow">{record ? 'STAMMDATEN BEARBEITEN' : 'STAMMDATEN ANLEGEN'}</div><h2 id="editor-title">{customer ? record ? 'Kunde bearbeiten' : 'Neuer Kunde' : record ? 'Produkt bearbeiten' : 'Neues Produkt'}</h2></div><button type="button" className="icon-button" aria-label="Schließen" disabled={busy} onClick={onClose}><X size={20}/></button></div>
      <div className="editor-fields">{field('name', 'Name')}{customer ? <>{field('email', 'E-Mail', 'email')}{field('phone', 'Telefon', 'tel')}{field('address', 'Adresse')}{field('birthdate', 'Geburtsdatum', 'date', false)}</> : <>{field('description', 'Beschreibung', 'text', false)}<div className="field-pair">{field('price', 'Preis (€)', 'number', true, '0.01')}{field('weight', 'Gewicht (g)', 'number', true, 'any')}</div>{field('minStockThreshold', 'Mindestbestand (Stück)', 'number', true, '1')}</>}</div>
      {error && <div className="message error" role="alert">{error}</div>}
      <div className="editor-footer"><button type="button" className="secondary-button" disabled={busy} onClick={onClose}>Abbrechen</button><button type="submit" className="primary-button" disabled={busy}>{busy ? <LoaderCircle className="spin" size={15}/> : <Check size={15}/>} {busy ? 'Wird gespeichert…' : 'Speichern'}</button></div>
    </form>
  </dialog>;
}
