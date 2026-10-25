import React, { useEffect, useState } from 'react';
import { Activity, ArrowDownLeft, ArrowUpRight, Bell, Boxes, ChevronDown, CircleHelp, Command, LayoutDashboard, Package, Search, Settings2, ShoppingCart, Users, Warehouse } from 'lucide-react';
import { checkApi, getDashboardCounts, type ApiStatus } from './api';

const nav = [
  { label: 'Übersicht', icon: LayoutDashboard, active: true },
  { label: 'Aufträge', icon: ShoppingCart },
  { label: 'Kunden', icon: Users },
  { label: 'Produkte', icon: Package },
  { label: 'Lagerbestand', icon: Warehouse },
];

function App() {
  const [apiStatus, setApiStatus] = useState<ApiStatus>('checking');
  const [counts, setCounts] = useState<{ customers: number; products: number } | null>(null);
  const [dataError, setDataError] = useState(false);
  useEffect(() => {
    let active = true;
    checkApi().then(status => {
      if (!active) return;
      setApiStatus(status);
      if (status === 'online') getDashboardCounts()
        .then(data => { if (active) setCounts(data); })
        .catch(() => { if (active) setDataError(true); });
    });
    return () => { active = false; };
  }, []);

  return <div className="shell">
    <aside className="sidebar">
      <div className="brand"><div className="brand-mark"><Command size={19}/></div><span>mini<span className="brand-light">ERP</span></span><span className="version">BETA</span></div>
      <div className="workspace"><div className="workspace-avatar">N</div><div className="workspace-copy"><b>Nordlicht Handel</b><small>Unternehmen</small></div><ChevronDown size={15}/></div>
      <div className="nav-caption">ARBEITSBEREICH</div>
      <nav>{nav.map(({label, icon: Icon, active}) => <a className={`nav-item ${active ? 'active' : ''}`} href="#" key={label}><Icon size={17}/><span>{label}</span>{label === 'Aufträge' && <em>8</em>}</a>)}</nav>
      <div className="sidebar-bottom"><a className="nav-item" href="#"><Settings2 size={17}/><span>Einstellungen</span></a><a className="nav-item" href="#"><CircleHelp size={17}/><span>Hilfe & Support</span></a><div className="user-card"><div className="user-avatar">AM</div><div className="workspace-copy"><b>Alex Meyer</b><small>Administrator</small></div><ChevronDown size={15}/></div></div>
    </aside>
    <main className="main">
      <header className="topbar"><div className="crumb">Arbeitsbereich <span>/</span> <b>Übersicht</b></div><div className="top-actions"><div className="search"><Search size={15}/><span>Suchen…</span><kbd>⌘ K</kbd></div><button className="icon-button" aria-label="Benachrichtigungen"><Bell size={17}/><i/></button><div className={`connection ${apiStatus}`}><span/>{apiStatus === 'online' ? 'Backend verbunden' : apiStatus === 'checking' ? 'Verbinde…' : 'Backend nicht erreichbar'}</div></div></header>
      <div className="content">
        <div className="welcome-row"><div><div className="eyebrow">SONNTAG, 25. OKTOBER 2026</div><h1>Guten Morgen, Alex <span>☀️</span></h1><p>Hier ist die aktuelle Übersicht für dein Unternehmen.</p></div><button className="primary-button"><span>＋</span> Neuer Auftrag</button></div>
        {dataError && <div className="data-error">Die API ist erreichbar, aber Kunden- und Produktdaten konnten nicht geladen werden.</div>}
        <section className="metrics">
          <Metric title="Umsatz (Monat)" value="24.580,00 €" delta="12,8 %" up note="zum Vormonat" icon={ArrowUpRight} color="blue"/>
          <Metric title="Offene Aufträge" value="18" delta="3 neu" note="seit letzter Woche" icon={ShoppingCart} color="violet"/>
          <Metric title="Aktive Kunden" value={counts ? String(counts.customers) : apiStatus === 'offline' ? '—' : '…'} delta="Live" note="aus dem Backend" icon={Users} color="green"/>
          <Metric title="Aktive Produkte" value={counts ? String(counts.products) : apiStatus === 'offline' ? '—' : '…'} delta="Live" note="aus dem Backend" icon={Boxes} color="amber"/>
        </section>
        <section className="middle-grid">
          <div className="panel revenue-panel"><div className="panel-heading"><div><h2>Umsatzentwicklung</h2><p>Demo-Daten · Deine Einnahmen im Zeitverlauf</p></div><button className="select-button">Letzte 7 Tage <ChevronDown size={14}/></button></div><div className="chart-legend"><span className="legend-dot"/> Umsatz <b>8.420 €</b><span className="legend-muted">+ 8,4 % zur Vorwoche</span></div><div className="chart"><div className="y-labels"><span>4.000 €</span><span>3.000 €</span><span>2.000 €</span><span>1.000 €</span><span>0 €</span></div><div className="plot"><div className="gridline g1"/><div className="gridline g2"/><div className="gridline g3"/><div className="gridline g4"/><svg viewBox="0 0 700 205" preserveAspectRatio="none" aria-label="Umsatzdiagramm"><defs><linearGradient id="fill" x1="0" x2="0" y1="0" y2="1"><stop offset="0" stopColor="#5b6ee1" stopOpacity=".22"/><stop offset="1" stopColor="#5b6ee1" stopOpacity="0"/></linearGradient></defs><path d="M0 164 C40 150 55 155 90 136 S145 128 180 139 S230 121 270 111 S330 121 360 98 S415 105 450 79 S510 92 540 63 S590 81 620 48 S670 54 700 25 L700 205 L0 205Z" fill="url(#fill)"/><path d="M0 164 C40 150 55 155 90 136 S145 128 180 139 S230 121 270 111 S330 121 360 98 S415 105 450 79 S510 92 540 63 S590 81 620 48 S670 54 700 25" fill="none" stroke="#5b6ee1" strokeWidth="3" vectorEffect="non-scaling-stroke"/></svg><div className="x-labels"><span>Mo, 19.</span><span>Di, 20.</span><span>Mi, 21.</span><span>Do, 22.</span><span>Fr, 23.</span><span>Sa, 24.</span><span>So, 25.</span></div></div></div></div>
          <div className="panel stock-panel"><div className="panel-heading"><div><h2>Bestand im Blick</h2><p>Demo-Daten · Artikel mit niedrigem Bestand</p></div><button className="text-link">Alle ansehen <span>→</span></button></div><div className="stock-list"><StockRow initials="KB" name="Kaffeebohnen Bio" sku="ART-0042" count="4" tone="red"/><StockRow initials="TM" name="Teemischung Minze" sku="ART-0018" count="8" tone="amber"/><StockRow initials="GL" name="Glasflasche 500ml" sku="ART-0061" count="12" tone="amber"/><StockRow initials="VP" name="Versandpaket S" sku="ART-0026" count="16" tone="green"/></div></div>
        </section>
        <section className="panel orders-panel"><div className="panel-heading"><div><h2>Letzte Aufträge</h2><p>Demo-Daten · Die neuesten Bestellungen in deinem Shop</p></div><button className="text-link">Alle Aufträge <span>→</span></button></div><div className="table-wrap"><table><thead><tr><th>AUFTRAG</th><th>KUNDE</th><th>DATUM</th><th>STATUS</th><th className="right">BETRAG</th></tr></thead><tbody><OrderRow id="#AU-2026-084" name="Mara Schneider" initials="MS" date="Heute, 10:42" status="In Bearbeitung" amount="248,50 €"/><OrderRow id="#AU-2026-083" name="Lukas Weber" initials="LW" date="Heute, 09:18" status="Versendet" amount="86,00 €"/><OrderRow id="#AU-2026-082" name="Julia Fischer" initials="JF" date="Gestern, 16:34" status="Abgeschlossen" amount="1.240,00 €"/><OrderRow id="#AU-2026-081" name="Leon Hoffmann" initials="LH" date="Gestern, 14:02" status="In Bearbeitung" amount="172,90 €"/></tbody></table></div></section>
        <footer><span><Activity size={13}/> Alle Systeme betriebsbereit</span><span>MiniERP · Version 0.1.0</span></footer>
      </div>
    </main>
  </div>;
}

function Metric({title, value, delta, note, icon: Icon, color, up}: {title:string;value:string;delta:string;note:string;icon:typeof ArrowUpRight;color:string;up?:boolean}) { return <article className="metric"><div className="metric-top"><span>{title}</span><div className={`metric-icon ${color}`}><Icon size={17}/></div></div><div className="metric-value">{value}</div><div className="metric-foot"><span className={up ? 'positive' : 'neutral'}>{up && <ArrowUpRight size={13}/>} {delta}</span><span>{note}</span></div></article>; }
function StockRow({initials,name,sku,count,tone}:{initials:string;name:string;sku:string;count:string;tone:string}) { return <div className="stock-row"><div className="product-avatar">{initials}</div><div className="stock-name"><b>{name}</b><small>{sku}</small></div><span className={`stock-count ${tone}`}><ArrowDownLeft size={12}/>{count} Stk.</span></div>; }
function OrderRow({id,name,initials,date,status,amount}:{id:string;name:string;initials:string;date:string;status:string;amount:string}) { const c=status==='Versendet'?'shipped':status==='Abgeschlossen'?'done':'pending'; return <tr><td className="order-id">{id}</td><td><div className="customer-cell"><span className="customer-avatar">{initials}</span>{name}</div></td><td className="muted-cell">{date}</td><td><span className={`status ${c}`}><i/>{status}</span></td><td className="right amount">{amount}</td></tr>; }

export default App;
