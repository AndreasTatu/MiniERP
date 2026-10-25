const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? 'http://127.0.0.1:8080/api').replace(/\/$/, '');

export type ApiStatus = 'checking' | 'online' | 'offline';
export type CustomerInput = {
  name: string;
  address: string;
  birthdate: string | null;
  email: string;
  phone: string;
};
export type Customer = CustomerInput & { customerID: number; active: boolean };
export type ProductInput = {
  name: string;
  description: string | null;
  price: string | number;
  weight: string | number;
  minStockThreshold: number;
};
export type Product = ProductInput & { productID: number; active: boolean };
export type Resource = 'customers' | 'products';

export class ApiError extends Error {
  constructor(message: string, public status: number) { super(message); }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}/${path}`, {
      ...options,
      headers: { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers },
      signal: options.signal ?? AbortSignal.timeout(10_000),
    });
  } catch (error) {
    if (options.signal?.aborted) throw error;
    throw new ApiError('Das Backend ist nicht erreichbar. Bitte starte die MiniERP-API und versuche es erneut.', 0);
  }
  const data = await response.json();
  if (!response.ok) throw new ApiError(data.error ?? `Die Anfrage ist fehlgeschlagen (${response.status}).`, response.status);
  return data as T;
}

export function getCollection<T>(resource: Resource, signal?: AbortSignal): Promise<T[]> {
  return request<T[]>(resource, { signal });
}

export function saveCustomer(data: CustomerInput, id?: number): Promise<Customer> {
  return request<Customer>(id ? `customers/${id}` : 'customers', {
    method: id ? 'PUT' : 'POST', body: JSON.stringify(data),
  });
}

export function saveProduct(data: ProductInput, id?: number): Promise<Product> {
  return request<Product>(id ? `products/${id}` : 'products', {
    method: id ? 'PUT' : 'POST', body: JSON.stringify(data),
  });
}
