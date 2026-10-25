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
export type Collection = Resource | 'orders' | 'stocks';
export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
export type Order = {
  orderID: number; customerID: number; orderDateTime: string;
  discount: string | number; tax: string | number; totalPrice: string | number; orderStatus: OrderStatus;
};
export type OrderItem = { orderItemID: number; orderID: number; productID: number; quantity: number; unitPrice: string | number };
export type OrderDetail = { order: Order; items: OrderItem[] };
export type OrderInput = { customerID: number; discount: string; tax: string; items: { productID: number; quantity: number }[] };
export type StockInput = { productID: number; location: string; quantity: number; maxStock: number; reserved: number };
export type Stock = StockInput & { stockID: number; active: boolean };

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

export function getCollection<T>(resource: Collection, signal?: AbortSignal): Promise<T[]> {
  return request<T[]>(resource, { signal });
}

export function getOrder(id: number, signal?: AbortSignal): Promise<OrderDetail> {
  return request<OrderDetail>(`orders/${id}`, { signal });
}

export function createOrder(data: OrderInput): Promise<OrderDetail> {
  return request<OrderDetail>('orders', { method: 'POST', body: JSON.stringify(data) });
}

export function updateOrderStatus(id: number, status: OrderStatus): Promise<Order> {
  return request<Order>(`orders/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) });
}

export function saveStock(data: StockInput, id?: number): Promise<Stock> {
  return request<Stock>(id ? `stocks/${id}` : 'stocks', { method: id ? 'PUT' : 'POST', body: JSON.stringify(data) });
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
