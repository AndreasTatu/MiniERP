import type { Product, Stock } from './api';

export const money = (value: string | number) => new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(Number(value));
export const grams = (value: string | number) => `${new Intl.NumberFormat('de-DE', { maximumFractionDigits: 6 }).format(Number(value))} g`;
export const statusLabels = { PENDING: 'Offen', CONFIRMED: 'Bestätigt', SHIPPED: 'Versendet', DELIVERED: 'Geliefert', CANCELLED: 'Storniert' };

export function availableStock(productID: number, stocks: Stock[]) {
  return stocks.filter(stock => stock.active && stock.productID === productID).reduce((sum, stock) => sum + stock.quantity - stock.reserved, 0);
}

export function lowStockProducts(products: Product[], stocks: Stock[]) {
  return products.filter(product => product.active && availableStock(product.productID, stocks) < product.minStockThreshold);
}

// Use decimal integers for the preview, applying HALF_UP only to the final amount.
// The server always calculates and persists the authoritative total and catalogue prices.
function decimal(value: string | number): { value: bigint; scale: number } | null {
  const text = String(value);
  if (!/^\d+(\.\d+)?$/.test(text) || text.length > 32) return null;
  const [integer, fraction = ''] = text.split('.');
  return { value: BigInt(integer + fraction), scale: fraction.length };
}

export function orderPreview(lines: { price: string | number; quantity: number }[], discountPercent: string, taxPercent: string): string | null {
  const discount = decimal(discountPercent), tax = decimal(taxPercent);
  if (!discount || !tax) return null;
  let subtotal = 0n, scale = 0;
  for (const line of lines) {
    const price = decimal(line.price);
    if (!price || !Number.isInteger(line.quantity) || line.quantity < 1) return null;
    const nextScale = Math.max(scale, price.scale);
    subtotal = subtotal * 10n ** BigInt(nextScale - scale)
      + price.value * BigInt(line.quantity) * 10n ** BigInt(nextScale - price.scale);
    scale = nextScale;
  }
  const discountUnit = 10n ** BigInt(discount.scale + 2), taxUnit = 10n ** BigInt(tax.scale + 2);
  if (discount.value > discountUnit) return null;
  const numerator = subtotal * (discountUnit - discount.value) * (taxUnit + tax.value) * 100n;
  const denominator = 10n ** BigInt(scale) * discountUnit * taxUnit;
  const cents = (numerator * 2n + denominator) / (denominator * 2n);
  return `${new Intl.NumberFormat('de-DE').format(cents / 100n)},${String(cents % 100n).padStart(2, '0')} €`;
}

export function percentToFraction(percent: string): string {
  const parsed = decimal(percent);
  if (!parsed) throw new Error('Bitte einen gültigen Prozentwert eingeben.');
  const digits = parsed.value.toString().padStart(parsed.scale + 3, '0');
  return `${digits.slice(0, -(parsed.scale + 2))}.${digits.slice(-(parsed.scale + 2))}`;
}
