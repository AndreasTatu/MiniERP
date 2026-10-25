# MiniERP

MiniERP currently contains a Java 17/Maven persistence and business layer and an initial React/Vite frontend. The frontend lives in `frontend/`; the Java sources remain under `src/main/java`.

## Current backend

- SQLite JDBC persistence with DAO implementations for customers, products, stock and orders.
- Service interfaces and implementations for those areas. Customer/product/stock CRUD and order reads/status updates are represented in the existing layers.
- There is no Spring runtime. The API uses the JDK HTTP server. `OrderServiceImpl.createOrder` currently validates input but does not complete total calculation or call persistence.
- On startup, `SchemaInitializer` creates any missing SQLite tables. The default database URL is `jdbc:sqlite:miniERP.db`; override it with `MINIERP_DB_URL`.
- The API listens on port 8080 (override with `MINIERP_PORT`) and currently provides `GET /api/health`, `GET /api/customers`, and `GET /api/products`. The customer and product routes return active records as JSON.

## Frontend

```sh
cd frontend
npm install
npm run dev
```

The dashboard reads active customer and product counts from the API. Revenue, stock and order widgets remain marked sample data. To point it elsewhere, copy `.env.example` to `.env.local` and set `VITE_API_BASE_URL`.

Run the Java API from the IDE by starting `com.minierp.api.ApiServer`. The API requires Java 17 or newer.

## Suggested next steps

1. Add a schema/migration initializer and move the SQLite URL out of `DatabaseConnection` into configuration.
2. Finish and verify the order creation workflow.
3. Add JSON request/response DTOs and API routes for products, customers, stock and orders, with consistent validation and error responses.
4. Replace the dashboard sample content with API-backed summaries, then add list/detail/create/edit flows per module.
