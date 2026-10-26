import { useEffect, useRef, useState, type ReactNode, type FormEvent } from 'react';
import { ArrowRight, Check, ClipboardList, LoaderCircle, PackagePlus, Pencil, Plus, Search, Trash2, Warehouse, X } from 'lucide-react';
import { createOrder, getOrder, saveStock, updateOrderStatus, type Customer, type Order, type OrderDetail, type OrderStatus, type Product, type Stock, type StockSelection } from './api';
import { AllocationPicker } from './AllocationPicker';
import { availableStock, lowStockProducts, money, orderPreview, percentToFraction, statusLabels } from './format';

export function StatusBadge({ status }: { status: OrderStatus }) {
  return <span className={`status-badge ${status.toLowerCase()}`}><i/>{statusLabels[status]}</span>;
}

function Modal({ title, eyebrow, busy, onClose, children, wide = false }: { title: string; eyebrow: string; busy?: boolean; onClose: () => void; children: ReactNode; wide?: boolean }) {
  const dialog = useRef<HTMLDialogElement>(null);
  useEffect(() => { dialog.current?.showModal(); }, []);
  return <dialog ref={dialog} className={`editor ${wide ? 'wide-editor' : ''}`} aria-labelledby="operation-title" onCancel={event => { event.preventDefault(); if (!busy) onClose(); }}>
    <div className="modal-body"><div className="editor-heading"><div><div className="eyebrow">{eyebrow}</div><h2 id="operation-title">{title}</h2></div><button type="button" className="icon-button" aria-label="Schließen" onClick={onClose} disabled={busy}><X size={20}/></button></div>{children}</div>
  </dialog>;
}

function SaveFooter({ busy, onClose, label = 'Speichern' }: { busy: boolean; onClose: () => void; label?: string }) {
  return <div className="editor-footer"><button type="button" className="secondary-button" onClick={onClose} disabled={busy}>Abbrechen</button><button className="primary-button" type="submit" disabled={busy}>{busy ? <LoaderCircle className="spin" size={15}/> : <Check size={15}/>} {busy ? 'Wird gespeichert…' : label}</button></div>;
}

function ListSearch({ value, onChange, label, children }: { value: string; onChange: (value: string) => void; label: string; children: ReactNode }) {
  return <div className="list-toolbar"><div className="search"><Search size={16}/><input aria-label={label} placeholder={label + '…'} value={value} onChange={event => onChange(event.target.value)}/>{value && <button aria-label="Suche löschen" onClick={() => onChange('')}><X size={14}/></button>}</div>{children}</div>;
}

export function OrdersPage({ orders, customers, products, loading, onSaved, onCustomers, onProducts }: { orders: Order[]; customers: Customer[]; products: Product[]; loading: boolean; onSaved: (order: Order) => void; onCustomers: () => void; onProducts: () => void }) {
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState('all');
  const [creating, setCreating] = useState(false);
  const [selected, setSelected] = useState<number | null>(null);
  const customerName = (id: number) => customers.find(customer => customer.customerID === id)?.name ?? `Kunde #${id}`;
  const filtered = [...orders].sort((a, b) => b.orderID - a.orderID).filter(order =>
    (filter === 'all' || order.orderStatus === filter) && `${order.orderID} ${customerName(order.customerID)}`.toLocaleLowerCase('de').includes(query.toLocaleLowerCase('de')));
  const ready = customers.length > 0 && products.length > 0;
  return <>
    <div className="welcome-row"><div><div className="eyebrow">VERKAUF</div><h1>Aufträge</h1><p>Von der ersten Position bis zur Lieferung.</p></div><button className="primary-button" disabled={loading || !ready} onClick={() => setCreating(true)}><Plus size={16}/>Neuer Auftrag</button></div>
    {!loading && !ready && <div className="next-step prerequisites"><ClipboardList size={22}/><div><b>Für einen Auftrag brauchst du Kunden und Produkte.</b><p>{!customers.length && <button className="text-link" onClick={onCustomers}>Kunden anlegen <ArrowRight size={13}/></button>}{!products.length && <button className="text-link" onClick={onProducts}>Produkte anlegen <ArrowRight size={13}/></button>}</p></div></div>}
    <section className="panel records-panel"><ListSearch value={query} onChange={setQuery} label="Aufträge durchsuchen"><select className="filter-select" aria-label="Auftragsstatus filtern" value={filter} onChange={event => setFilter(event.target.value)}><option value="all">Alle Status</option>{Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></ListSearch>
      {loading ? <div className="empty"><LoaderCircle className="spin" size={24}/>Aufträge werden geladen…</div> : !filtered.length ? <div className="empty"><ClipboardList size={28}/><b>{orders.length ? 'Keine passenden Aufträge' : 'Noch keine Aufträge vorhanden'}</b><p>{orders.length ? 'Passe die Suche oder den Statusfilter an.' : 'Lege deinen ersten Auftrag mit den vorhandenen Stammdaten an.'}</p></div> : <div className="table-wrap"><table><thead><tr><th>AUFTRAG</th><th>KUNDE</th><th>DATUM</th><th>STATUS</th><th className="right">GESAMT</th><th><span className="sr-only">Details</span></th></tr></thead><tbody>{filtered.map(order => <tr key={order.orderID}><td><button className="text-link record-link" onClick={() => setSelected(order.orderID)}>#{String(order.orderID).padStart(4, '0')}</button></td><td>{customerName(order.customerID)}</td><td>{new Intl.DateTimeFormat('de-DE', { dateStyle: 'medium' }).format(new Date(order.orderDateTime))}</td><td><StatusBadge status={order.orderStatus}/></td><td className="right amount">{money(order.totalPrice)}</td><td className="right"><button className="edit-button" aria-label={`Auftrag ${order.orderID} öffnen`} onClick={() => setSelected(order.orderID)}><ArrowRight size={16}/></button></td></tr>)}</tbody></table></div>}
    </section>
    {creating && <OrderEditor customers={customers} products={products} onClose={() => setCreating(false)} onSaved={detail => { onSaved(detail.order); setCreating(false); setSelected(detail.order.orderID); }}/ >}
    {selected !== null && <OrderDetails key={selected} id={selected} customers={customers} products={products} onClose={() => setSelected(null)} onSaved={onSaved}/ >}
  </>;
}

function OrderEditor({ customers, products, onClose, onSaved }: { customers: Customer[]; products: Product[]; onClose: () => void; onSaved: (order: OrderDetail) => void }) {
  const [customerID, setCustomerID] = useState(String(customers[0]?.customerID ?? ''));
  const [discount, setDiscount] = useState('0');
  const [tax, setTax] = useState('0');
  const [lines, setLines] = useState([{ key: 1, productID: String(products[0]?.productID ?? ''), quantity: '1' }]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const nextKey = useRef(2);
  const preview = orderPreview(lines.map(line => ({ price: products.find(product => String(product.productID) === line.productID)?.price ?? '', quantity: Number(line.quantity) })), discount, tax);
  function updateLine(key: number, field: 'productID' | 'quantity', value: string) { setLines(current => current.map(line => line.key === key ? { ...line, [field]: value } : line)); }
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (busy) return;
    setBusy(true); setError('');
    try {
      const order = await createOrder({ customerID: Number(customerID), discount: percentToFraction(discount), tax: percentToFraction(tax), items: lines.map(line => ({ productID: Number(line.productID), quantity: Number(line.quantity) })) });
      onSaved(order);
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'Auftrag konnte nicht angelegt werden.'); setBusy(false); }
  }
  return <Modal title="Neuer Auftrag" eyebrow="VERKAUF" busy={busy} onClose={onClose} wide><form className="operation-form" onSubmit={event => void submit(event)}>
    <label className="form-field"><span>Kunde</span><select aria-label="Kunde" required value={customerID} onChange={event => setCustomerID(event.target.value)} disabled={busy}>{customers.map(customer => <option key={customer.customerID} value={customer.customerID}>{customer.name}</option>)}</select></label>
    <div className="line-heading"><h3>Positionen</h3><span>Preise aus dem Produktkatalog</span></div>
    <div className="order-lines">{lines.map((line, index) => {
      const product = products.find(product => String(product.productID) === line.productID);
      return <div className="order-line" key={line.key}><label className="form-field"><span>Artikel {index + 1}</span><select aria-label={`Artikel ${index + 1}`} required value={line.productID} onChange={event => updateLine(line.key, 'productID', event.target.value)} disabled={busy}>{products.map(product => <option key={product.productID} value={product.productID}>{product.name}</option>)}</select></label><label className="form-field quantity-field"><span>Menge</span><input aria-label={`Menge ${index + 1}`} type="number" min="1" max="2147483647" step="1" required value={line.quantity} onChange={event => updateLine(line.key, 'quantity', event.target.value)} disabled={busy}/></label><div className="line-price"><small>Einzelpreis</small><b>{product ? money(product.price) : '—'}</b></div><button className="icon-button remove-line" aria-label={`Position ${index + 1} entfernen`} type="button" disabled={busy || lines.length === 1} onClick={() => setLines(current => current.filter(item => item.key !== line.key))}><Trash2 size={16}/></button></div>;
    })}</div>
    <button className="text-link add-line" type="button" disabled={busy} onClick={() => setLines(current => [...current, { key: nextKey.current++, productID: String(products[0]?.productID ?? ''), quantity: '1' }])}><Plus size={15}/>Position hinzufügen</button>
    <div className="field-pair order-rates"><label className="form-field"><span>Rabatt (%)</span><input aria-label="Rabatt (%)" type="number" min="0" max="100" step="1" required value={discount} disabled={busy} onChange={event => setDiscount(event.target.value)}/></label><label className="form-field"><span>Steuer (%)</span><input aria-label="Steuer (%)" type="number" min="0" step="1" required value={tax} disabled={busy} onChange={event => setTax(event.target.value)}/></label></div>
    <div className="order-total"><span>Gesamt nach Rabatt und Steuer</span><strong>{preview ?? 'Wird beim Speichern berechnet'}</strong></div><p className="form-hint">Beim Bestätigen wählst du Lagerplätze für die Reservierung. Beim Versand wird die zugeordnete Ware abgebucht.</p>
    {error && <div className="message error" role="alert">{error}</div>}<SaveFooter busy={busy} onClose={onClose} label="Auftrag anlegen"/>
  </form></Modal>;
}

const transitions: Record<OrderStatus, { status: OrderStatus; label: string }[]> = {
  PENDING: [{ status: 'CONFIRMED', label: 'Bestätigen' }, { status: 'CANCELLED', label: 'Stornieren' }],
  CONFIRMED: [{ status: 'SHIPPED', label: 'Als versendet markieren' }, { status: 'CANCELLED', label: 'Stornieren' }],
  SHIPPED: [{ status: 'DELIVERED', label: 'Als geliefert markieren' }], DELIVERED: [], CANCELLED: [],
};

function OrderDetails({ id, customers, products, onClose, onSaved }: { id: number; customers: Customer[]; products: Product[]; onClose: () => void; onSaved: (order: Order) => void }) {
  const [detail, setDetail] = useState<OrderDetail | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [retry, setRetry] = useState(0);
  const [confirmCancel, setConfirmCancel] = useState(false);
  const [picking, setPicking] = useState(false);
  useEffect(() => {
    const controller = new AbortController();
    setError('');
    getOrder(id, controller.signal).then(setDetail).catch(failure => { if (!controller.signal.aborted) setError(failure instanceof Error ? failure.message : 'Auftrag konnte nicht geladen werden.'); });
    return () => controller.abort();
  }, [id, retry]);
  async function setStatus(status: OrderStatus, selection?: StockSelection[]) {
    if (busy || !detail) return;
    setBusy(true); setError('');
    let committed = false;
    try {
      const saved = await updateOrderStatus(id, status, selection);
      committed = true;
      setDetail({ ...detail, order: saved }); onSaved(saved); setConfirmCancel(false); setPicking(false);
      setDetail(await getOrder(id));
    }
    catch (failure) { setError(committed ? 'Status gespeichert. Die Details konnten nicht neu geladen werden; bitte den Auftrag erneut öffnen.' : failure instanceof Error ? failure.message : 'Statusänderung fehlgeschlagen.'); }
    finally { setBusy(false); }
  }
  return <Modal title={`Auftrag #${String(id).padStart(4, '0')}`} eyebrow="AUFTRAGSDETAILS" busy={busy} onClose={onClose} wide>
    {error && !picking && <div className="message error" role="alert">{error}{!detail && <button onClick={() => setRetry(value => value + 1)}>Erneut versuchen</button>}</div>}
    {!detail ? !error && <div className="empty compact"><LoaderCircle className="spin" size={22}/>Auftrag wird geladen…</div> : <>
      <div className="order-meta"><div><small>Kunde</small><b>{customers.find(customer => customer.customerID === detail.order.customerID)?.name ?? `Kunde #${detail.order.customerID}`}</b></div><div><small>Erstellt (Berlin)</small><b>{new Intl.DateTimeFormat('de-DE', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(detail.order.orderDateTime))}</b></div><StatusBadge status={detail.order.orderStatus}/></div>
      <div className="table-wrap detail-table"><table><thead><tr><th>ARTIKEL</th><th className="right">MENGE</th><th className="right">EINZELPREIS</th></tr></thead><tbody>{detail.items.map(item => <tr key={item.orderItemID}><td>{products.find(product => product.productID === item.productID)?.name ?? `Artikel #${item.productID}`}</td><td className="right">{item.quantity} Stk.</td><td className="right">{money(item.unitPrice)}</td></tr>)}</tbody></table></div>
      <div className="rate-summary"><span>Rabatt: {new Intl.NumberFormat('de-DE', { style: 'percent', maximumFractionDigits: 2 }).format(Number(detail.order.discount))}</span><span>Steuer: {new Intl.NumberFormat('de-DE', { style: 'percent', maximumFractionDigits: 2 }).format(Number(detail.order.tax))}</span></div><div className="order-total"><span>Gesamtbetrag</span><strong>{money(detail.order.totalPrice)}</strong></div>
      {detail.allocations.length > 0 && <section className="allocation-history"><h3>Lagerzuordnung</h3><div className="table-wrap detail-table"><table><thead><tr><th>ARTIKEL</th><th>LAGERPLATZ</th><th className="right">MENGE</th><th>STATUS</th></tr></thead><tbody>{detail.allocations.map(row => <tr key={row.allocationID}><td>{products.find(product => product.productID === row.productID)?.name ?? `Artikel #${row.productID}`}</td><td><span className="location-code">{row.location}</span></td><td className="right">{row.quantity} Stk.</td><td><span className={`allocation-state ${row.state.toLowerCase()}`}>{row.state === 'RESERVED' ? 'Reserviert' : row.state === 'SHIPPED' ? 'Ausgebucht' : 'Freigegeben'}</span></td></tr>)}</tbody></table></div></section>}
      {detail.order.orderStatus === 'CONFIRMED' && !detail.allocations.length && <p className="legacy-allocation-note">Für diesen bestätigten Auftrag fehlen Lagerreservierungen. Bitte vor dem Versand die Lagerzuordnung nachholen.</p>}
      {picking ? <AllocationPicker detail={detail} products={products} busy={busy} error={error} onClose={() => { setPicking(false); setError(''); }} onConfirm={selection => void setStatus('CONFIRMED', selection)}/>
        : confirmCancel ? <div className="cancel-confirm"><p>Diesen Auftrag verbindlich stornieren und seine Reservierungen freigeben?</p><button className="secondary-button" disabled={busy} onClick={() => setConfirmCancel(false)}>Zurück</button><button className="danger-button" disabled={busy} onClick={() => void setStatus('CANCELLED')}>Ja, stornieren</button></div>
        : <div className="status-actions">{detail.order.orderStatus === 'CONFIRMED' && !detail.allocations.length && <button className="primary-button" disabled={busy} onClick={() => { setError(''); setPicking(true); }}>Lagerzuordnung nachholen</button>}{transitions[detail.order.orderStatus].filter(action => !(action.status === 'SHIPPED' && !detail.allocations.length)).map(action => <button key={action.status} className={action.status === 'CANCELLED' ? 'secondary-button' : 'primary-button'} disabled={busy} onClick={() => action.status === 'CANCELLED' ? setConfirmCancel(true) : action.status === 'CONFIRMED' ? (setError(''), setPicking(true)) : void setStatus(action.status)}>{busy && <LoaderCircle className="spin" size={14}/>} {action.label}</button>)}</div>}
    </>}
  </Modal>;
}

export function StockPage({ stocks, products, loading, onSaved, onProducts }: { stocks: Stock[]; products: Product[]; loading: boolean; onSaved: (stock: Stock) => void; onProducts: () => void }) {
  const [query, setQuery] = useState('');
  const [onlyLow, setOnlyLow] = useState(false);
  const [editing, setEditing] = useState<Stock | null | undefined>(undefined);
  const low = lowStockProducts(products, stocks);
  const name = (id: number) => products.find(product => product.productID === id)?.name ?? `Artikel #${id}`;
  const filtered = [...stocks].sort((a, b) => a.location.localeCompare(b.location) || a.productID - b.productID).filter(stock =>
    (!onlyLow || low.some(product => product.productID === stock.productID)) && `${name(stock.productID)} ${stock.location}`.toLocaleLowerCase('de').includes(query.toLocaleLowerCase('de')));
  return <>
    <div className="welcome-row"><div><div className="eyebrow">LAGERVERWALTUNG</div><h1>Lagerbestand</h1><p>Bestände, Reservierungen und Lagerplätze im Blick.</p></div><button className="primary-button" onClick={() => setEditing(null)} disabled={loading || !products.length}><Plus size={16}/>Bestand anlegen</button></div>
    {!loading && !products.length && <div className="next-step prerequisites"><PackagePlus size={22}/><div><b>Lege zuerst einen Artikel an.</b><p><button className="text-link" onClick={onProducts}>Produkte öffnen <ArrowRight size={13}/></button></p></div></div>}
    {!loading && low.length > 0 && <div className="stock-warning"><div><b>{low.length} Artikel unter Mindestbestand</b><p>Verfügbar = Bestand − reserviert, über alle aktiven Lagerplätze summiert.</p></div><div className="shortage-list">{low.map(product => <span key={product.productID}>{product.name}<b>{availableStock(product.productID, stocks)} / {product.minStockThreshold} Stk.</b></span>)}</div></div>}
    <section className="panel records-panel"><ListSearch value={query} onChange={setQuery} label="Artikel oder Lagerplatz suchen"><label className="check-filter"><input type="checkbox" checked={onlyLow} onChange={event => setOnlyLow(event.target.checked)}/>Nur niedrige Bestände</label></ListSearch>
      {loading ? <div className="empty"><LoaderCircle className="spin" size={24}/>Bestände werden geladen…</div> : !filtered.length ? <div className="empty"><Warehouse size={28}/><b>{stocks.length ? 'Keine passenden Bestände' : 'Noch keine Lagerbestände vorhanden'}</b><p>{onlyLow ? 'Artikel ohne Lagerplatz erscheinen im Hinweis oben.' : stocks.length ? 'Passe die Suche an.' : 'Lege einen Bestand mit Artikel und Lagerplatz an.'}</p></div> : <div className="table-wrap"><table><thead><tr><th>ARTIKEL</th><th>LAGERPLATZ</th><th className="right">BESTAND</th><th className="right">RESERVIERT</th><th className="right">VERFÜGBAR</th><th className="right">KAPAZITÄT</th><th><span className="sr-only">Bearbeiten</span></th></tr></thead><tbody>{filtered.map(stock => <tr key={stock.stockID}><td><div className="stock-product"><b>{name(stock.productID)}</b>{low.some(product => product.productID === stock.productID) && <span>Unter Mindestbestand</span>}</div></td><td><span className="location-code">{stock.location}</span></td><td className="right">{stock.quantity}</td><td className="right"><div>{stock.reserved}</div><small className="reservation-breakdown">Aufträge {stock.orderReserved ?? 0} · Manuell {stock.manualReserved ?? stock.reserved}</small></td><td className="right amount">{stock.quantity - stock.reserved}</td><td className="right">{stock.maxStock}</td><td className="right"><button className="edit-button" aria-label={`${name(stock.productID)} an ${stock.location} bearbeiten`} onClick={() => setEditing(stock)}><Pencil size={14}/></button></td></tr>)}</tbody></table></div>}
    </section><p className="form-hint stock-note">Auftragsreservierungen werden beim Bestätigen angelegt, beim Versand ausgebucht und bei Stornierung freigegeben. Manuelle Reservierungen bleiben erhalten.</p>
    {editing !== undefined && <StockEditor products={products} record={editing} onClose={() => setEditing(undefined)} onSaved={stock => { onSaved(stock); setEditing(undefined); }}/ >}
  </>;
}

function StockEditor({ products, record, onClose, onSaved }: { products: Product[]; record: Stock | null; onClose: () => void; onSaved: (stock: Stock) => void }) {
  const [productID, setProductID] = useState(String(record?.productID ?? products[0]?.productID ?? ''));
  const [location, setLocation] = useState(record?.location ?? '');
  const [quantity, setQuantity] = useState(String(record?.quantity ?? 0));
  const [reserved, setReserved] = useState(String(record?.reserved ?? 0));
  const [capacity, setCapacity] = useState(record ? String(record.maxStock) : '');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (busy) return;
    setBusy(true); setError('');
    try { onSaved(await saveStock({ productID: Number(productID), location, quantity: Number(quantity), maxStock: Number(capacity), reserved: Number(reserved) }, record?.stockID)); }
    catch (failure) { setError(failure instanceof Error ? failure.message : 'Bestand konnte nicht gespeichert werden.'); setBusy(false); }
  }
  return <Modal title={record ? 'Bestand bearbeiten' : 'Bestand anlegen'} eyebrow="LAGERVERWALTUNG" busy={busy} onClose={onClose}><form className="operation-form" onSubmit={event => void submit(event)}><div className="editor-fields">
    <label className="form-field"><span>Artikel</span><select aria-label="Artikel" value={productID} onChange={event => setProductID(event.target.value)} disabled={busy || !!record} required>{record && !products.some(product => product.productID === record.productID) && <option value={record.productID}>Artikel #{record.productID}</option>}{products.map(product => <option key={product.productID} value={product.productID}>{product.name}</option>)}</select></label>
    <label className="form-field"><span>Lagerplatz</span><input aria-label="Lagerplatz" value={location} onChange={event => setLocation(event.target.value.toUpperCase())} required pattern="[A-Z]-[0-9]{2}-[0-9]{2}" placeholder="A-01-02" maxLength={7} disabled={busy || (record?.orderReserved ?? 0) > 0}/><small className="form-hint">Format: A-01-02</small></label>
    <div className="field-pair"><label className="form-field"><span>Bestand (Stück)</span><input aria-label="Bestand (Stück)" type="number" min="0" max={capacity || '2147483647'} step="1" required value={quantity} disabled={busy} onChange={event => setQuantity(event.target.value)}/></label><label className="form-field"><span>Reserviert gesamt (Stück)</span><input aria-label="Reserviert gesamt (Stück)" type="number" min={record?.orderReserved ?? 0} max={quantity || '0'} step="1" required value={reserved} disabled={busy} onChange={event => setReserved(event.target.value)}/></label></div>
    <label className="form-field"><span>Kapazität (Stück)</span><input aria-label="Kapazität (Stück)" type="number" min="1" max="2147483647" step="1" required value={capacity} disabled={busy} onChange={event => setCapacity(event.target.value)}/></label><div className="availability">Verfügbar <b>{Number(quantity) - Number(reserved)} Stk.</b></div><p className="form-hint">Davon durch Aufträge gebunden: {record?.orderReserved ?? 0} Stk. Diese Mindestreservierung und ihr Lagerplatz sind geschützt.</p>
    </div>{error && <div className="message error" role="alert">{error}</div>}<SaveFooter busy={busy} onClose={onClose}/></form></Modal>;
}
