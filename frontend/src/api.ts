const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '/api').replace(/\/$/, '');

export type Role = 'ADMIN' | 'SALES' | 'WAREHOUSE';
export type Permission = 'CUSTOMERS_READ' | 'CUSTOMERS_WRITE' | 'PRODUCTS_READ' | 'PRODUCTS_WRITE' | 'ORDERS_READ' | 'ORDERS_CREATE' | 'CONFIRM' | 'SHIP' | 'DELIVER' | 'CANCEL_OPEN' | 'CANCEL_CONFIRMED' | 'STOCKS_READ' | 'STOCKS_WRITE' | 'USERS_MANAGE';
export type Session = { username: string; role: Role; permissions: Permission[] };
export type Account = { username: string; role: Role; active: boolean };
export const roleNames: Record<Role, string> = { ADMIN: 'Admin', SALES: 'Vertrieb', WAREHOUSE: 'Lager' };
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
export type OrderAllocation = { allocationID: number; orderID: number; stockID: number; productID: number; location: string; quantity: number; state: 'RESERVED' | 'SHIPPED' | 'RELEASED' };
export type StockSelection = { stockID: number; quantity: number };
export type OrderDetail = { order: Order; items: OrderItem[]; allocations: OrderAllocation[]; recipientName: string; recipientAddress: string };
export type OrderInput = { customerID: number; discount: string; tax: string; items: { productID: number; quantity: number }[] };
export type StockInput = { productID: number; location: string; quantity: number; maxStock: number; reserved: number };
export type Stock = StockInput & { stockID: number; active: boolean; orderReserved: number; manualReserved: number; revision: number };

export class ApiError extends Error {
  constructor(message: string, public status: number) { super(message); }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}/${path}`, {
      ...options,
      credentials: 'include',
      headers: { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers },
      signal: options.signal ?? AbortSignal.timeout(10_000),
    });
  } catch (error) {
    if (options.signal?.aborted) throw error;
    throw new ApiError('Das Backend ist nicht erreichbar. Bitte starte die MiniERP-API und versuche es erneut.', 0);
  }
  if (response.status === 401 && path !== 'login' && path !== 'session') window.dispatchEvent(new Event('minierp:expired'));
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

export function updateOrderStatus(id: number, status: OrderStatus, allocations?: StockSelection[]): Promise<Order> {
  return request<Order>(`orders/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status, ...(allocations ? { allocations } : {}) }) });
}

export function saveStock(data: StockInput & { revision?: number }, id?: number): Promise<Stock> {
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

export const getSession = () => request<Session>('session');
export const login = (username: string, password: string) =>
  request<Session>('login', { method: 'POST', body: JSON.stringify({ username, password }) });
export const logout = () => request('logout', { method: 'POST', body: '{}' });
export const getAccounts = () => request<Account[]>('users');
export const createAccount = (username: string, password: string, role: Role) =>
  request<Account>('users', { method: 'POST', body: JSON.stringify({ username, password, role }) });
export const activateAccount = (username: string, active: boolean) =>
  request('users/' + encodeURIComponent(username), { method: 'PATCH', body: JSON.stringify({ active }) });
export const getStock = (id: number) => request<Stock>('stocks/' + id);
