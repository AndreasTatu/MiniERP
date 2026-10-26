import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Check, LoaderCircle, RefreshCw } from 'lucide-react';
import { getCollection, type OrderDetail, type Product, type Stock, type StockSelection } from './api';

export function AllocationPicker({ detail, products, busy, error, onConfirm, onClose }: {
  detail: OrderDetail; products: Product[]; busy: boolean; error: string;
  onConfirm: (selection: StockSelection[]) => void; onClose: () => void;
}) {
  const [stocks, setStocks] = useState<Stock[]>([]);
  const [chosen, setChosen] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const load = useCallback(async (signal?: AbortSignal) => {
    setLoading(true); setLoadError('');
    try { const current = await getCollection<Stock>('stocks', signal); if (!signal?.aborted) setStocks(current); }
    catch (failure) { if (!signal?.aborted) setLoadError(failure instanceof Error ? failure.message : 'Bestände konnten nicht geladen werden.'); }
    finally { if (!signal?.aborted) setLoading(false); }
  }, []);
  useEffect(() => { const controller = new AbortController(); void load(controller.signal); return () => controller.abort(); }, [load]);

  const demand = new Map<number, number>();
  detail.items.forEach(item => demand.set(item.productID, (demand.get(item.productID) ?? 0) + item.quantity));
  const candidates = [...stocks].filter(stock => demand.has(stock.productID)).sort((a, b) => a.location.localeCompare(b.location) || a.stockID - b.stockID);
  const entries = candidates.map(stock => ({ stockID: stock.stockID, quantity: Number(chosen[stock.stockID] ?? 0) }));
  const validRows = entries.every(row => Number.isInteger(row.quantity) && row.quantity >= 0
    && row.quantity <= candidates.find(stock => stock.stockID === row.stockID)!.quantity - candidates.find(stock => stock.stockID === row.stockID)!.reserved);
  const selectedFor = (productID: number) => candidates.filter(stock => stock.productID === productID).reduce((sum, stock) => sum + Number(chosen[stock.stockID] ?? 0), 0);
  const complete = validRows && [...demand].every(([productID, needed]) => selectedFor(productID) === needed);

  function submit(event: FormEvent) {
    event.preventDefault();
    if (!busy && !loading && !loadError && complete) onConfirm(entries.filter(row => row.quantity > 0));
  }
  return <form className="operation-form allocation-picker" onSubmit={submit}>
    <div className="allocation-heading"><div><h3>Lagerplätze auswählen</h3><p>Verteile die benötigte Menge je Artikel auf die gewünschten Lagerplätze.</p></div><button type="button" className="secondary-button" disabled={busy || loading} onClick={() => void load()}><RefreshCw size={13} className={loading ? 'spin' : ''}/>Neu laden</button></div>
    {loadError && <div className="message error" role="alert">{loadError}</div>}
    {loading ? <div className="empty compact"><LoaderCircle className="spin" size={20}/>Verfügbare Bestände werden geladen…</div> : [...demand].map(([productID, needed]) => {
      const selected = selectedFor(productID);
      const locations = candidates.filter(stock => stock.productID === productID);
      return <section className="allocation-product" key={productID}><div className="allocation-product-heading"><b>{products.find(product => product.productID === productID)?.name ?? `Artikel #${productID}`}</b><span className={selected === needed ? 'allocation-complete' : ''}>{selected} / {needed} Stk. ausgewählt</span></div>
        {!locations.length ? <p className="form-hint">Kein aktiver Lagerbestand vorhanden. Bitte zuerst einen passenden Bestand anlegen.</p> : locations.map(stock => {
          const available = stock.quantity - stock.reserved;
          return <label className="allocation-row" key={stock.stockID}><span className="location-code">{stock.location}</span><span>{available} Stk. verfügbar</span><input type="number" aria-label={`Menge ${stock.location} für ${products.find(product => product.productID === productID)?.name ?? `Artikel ${productID}`}`} min="0" max={available} step="1" required value={chosen[stock.stockID] ?? '0'} disabled={busy || (available <= 0 && !Number(chosen[stock.stockID]))} onChange={event => setChosen(current => ({ ...current, [stock.stockID]: event.target.value }))}/></label>;
        })}
      </section>;
    })}
    {error && <div className="message error" role="alert">{error}</div>}
    <div className="editor-footer"><button type="button" className="secondary-button" disabled={busy} onClick={onClose}>Zurück</button><button type="submit" className="primary-button" disabled={busy || loading || !!loadError || !complete}>{busy ? <LoaderCircle size={14} className="spin"/> : <Check size={14}/>} {detail.order.orderStatus === 'CONFIRMED' ? 'Reservierung speichern' : 'Reservieren und bestätigen'}</button></div>
  </form>;
}
