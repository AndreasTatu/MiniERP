const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

export type ApiStatus = 'checking' | 'online' | 'offline';
export type Customer = { customerID: number; name: string; email: string; active: boolean };
export type Product = { productID: number; name: string; price: number; active: boolean };

export async function checkApi(): Promise<ApiStatus> {
  try {
    const response = await fetch(`${API_BASE_URL}/health`, { signal: AbortSignal.timeout(2500) });
    return response.ok ? 'online' : 'offline';
  } catch {
    return 'offline';
  }
}

export async function getCollection<T>(resource: string): Promise<T[]> {
  const response = await fetch(`${API_BASE_URL}/${resource}`);
  if (!response.ok) throw new Error(`API-Fehler (${response.status})`);
  return response.json() as Promise<T[]>;
}

export async function getDashboardCounts(): Promise<{ customers: number; products: number }> {
  const [customers, products] = await Promise.all([
    getCollection<Customer>('customers'),
    getCollection<Product>('products'),
  ]);
  return { customers: customers.length, products: products.length };
}
