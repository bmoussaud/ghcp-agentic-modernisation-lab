# Order Service

A small Spring Boot REST API for managing customer orders, paired with a lightweight React (Vite) operations workspace. It is used as a demo application for **application code modernization** (Java upgrade, CVE remediation, and Azure migration scenarios).

## Tech Stack

| Layer      | Technology                                  |
| ---------- | ------------------------------------------- |
| Language   | Java 25                                      |
| Framework  | Spring Boot 3.5.16 (Web, Data JPA, Validation) |
| Database   | H2 (in-memory)                               |
| Build      | Maven                                         |
| Frontend   | React 18 + Vite 5                            |

## Project Structure

```
pom.xml                     # Maven build for the backend
src/main/java/...           # Spring Boot application code
  OrderServiceApplication   # Application entry point
  model/Order               # JPA entity + simple status enum
  repository/OrderRepository# Spring Data JPA repository
  service/OrderService      # Business logic
  web/OrderController        # REST endpoints
  config/                   # Web config + data seeding
src/main/resources/         # application.properties
src/test/java/...           # Unit and web tests
frontend/                   # React + Vite UI
```

## Prerequisites

- JDK 25 and Maven 3.9+
- Node.js 18+ (for the frontend)

## Running the Backend

```pwsh
mvn spring-boot:run
```

The API starts on **http://localhost:8080**. An in-memory H2 database is seeded with sample orders on startup, and the H2 console is available at http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:orders`, user `sa`). The datasource password is read from `SPRING_DATASOURCE_PASSWORD`; its empty default preserves the passwordless local H2 baseline. Production environments must inject the required secret at runtime rather than storing it in source control or an image.

Check the following url **http://localhost:8080/api/orders**

To build a runnable JAR:

```pwsh
mvn clean package
java -jar target/order-service-1.0.0.jar
```

To build and run the Java 25 container:

```pwsh
docker build -t order-service:local .
docker run --rm -p 8080:8080 -e SPRING_DATASOURCE_PASSWORD order-service:local
```

The image runs as a non-root user, listens through the published port, and does not contain a datasource password.

## Running the Frontend

```pwsh
cd frontend
npm install
npm run dev or npm run dev -- --host=0.0.0.0 --port=5173 if running in the devcontainer
```

The UI runs on **http://localhost:5173** and proxies `/api` requests to the backend on port 8080.

## Debugging in an IDE

Install the frontend dependencies once before using either IDE configuration:

```pwsh
cd frontend
npm install
```

Stop any manually started processes on ports `8080` and `5173` before launching an IDE configuration.

### VS Code

1. Open the repository root in VS Code and install the recommended **Extension Pack for Java** when prompted.
2. Open **Run and Debug**.
3. Select **Order Service: Full Stack** and press `F5`.

The full-stack configuration starts Vite, launches the Spring Boot application with the Java debugger attached, and opens Chrome at http://127.0.0.1:5173 after the backend is ready. Use **Order Service: Backend** or **Order Service: Frontend** to debug only one layer. If Vite remains active after debugging, run **Tasks: Terminate Task** and select `Order Service: Start Frontend`.

### IntelliJ IDEA

1. Open the repository root and import `app/Java - Spring Boot/Order Service/pom.xml` as a Maven project.
2. Select an installed JDK for the `order-service` module and configure the project Node.js interpreter. The npm run configuration requires IntelliJ's JavaScript and TypeScript plugin.
3. Select **Order Service Full Stack** from the run configuration menu and choose **Debug**.

The shared IntelliJ compound configuration starts **Order Service Backend** as a debuggable Java application and **Order Service Frontend** as the Vite npm process. The frontend configuration opens http://127.0.0.1:5173 automatically. The individual backend and frontend configurations are also available.

## API Reference

Base path: `/api/orders`

| Method | Endpoint                              | Description                              |
| ------ | ------------------------------------- | ---------------------------------------- |
| GET    | `/api/orders`                         | List all orders                          |
| GET    | `/api/orders/{id}`                    | Get a single order by ID (404 if absent) |
| GET    | `/api/orders/customer/{customer}/total` | Sum of order amounts for a customer    |
| POST   | `/api/orders`                         | Create a new order                       |
| PATCH  | `/api/orders/{id}/status?status={value}` | Update an order status                |

### Order model

```json
{
  "id": 1,
  "customer": "alice",
  "amount": 120.50,
  "createdAt": "2026-06-09T12:00:00.000+00:00",
  "status": "PENDING"
}
```

`customer` must not be blank and `amount` must be at least `0.01`. New orders always start as `PENDING`. Supported statuses are `PENDING`, `PROCESSING`, and `COMPLETED`; this demo deliberately uses direct status updates rather than a workflow engine or transition history.

### Example requests

```pwsh
# List orders
curl http://localhost:8080/api/orders

# Create an order
curl -X POST http://localhost:8080/api/orders `
  -H "Content-Type: application/json" `
  -d '{ "customer": "carol", "amount": 99.99 }'

# Total spend for a customer
curl http://localhost:8080/api/orders/customer/alice/total

# Update an order status
curl -X PATCH "http://localhost:8080/api/orders/1/status?status=PROCESSING"
```

## Frontend Workspace

The single-page workspace keeps all demo data in the existing orders API and adds:

- summary metrics calculated in the browser
- customer or exact order-ID search
- status filtering and date/amount sorting
- inline status updates
- create-order validation and customer-total lookup
- responsive layouts, native dialog behavior, keyboard focus states, and text-based status indicators

These capabilities are intentionally client-side for the small seeded dataset. No charting, component, state-management, or API-client libraries are added.

## Testing

```pwsh
mvn test

cd frontend
npm test
npm run build
```

Backend tests cover the repository, service, validation, and web controller layers. Frontend tests use Node's built-in test runner for search, filtering, sorting, and summary calculations.

## AI-assisted modernization

The backend now targets Java 25 and Spring Boot 3.5.16. For subsequent Azure Container Apps work,
start with [`AGENTS.md`](AGENTS.md) and [`constitution.md`](constitution.md), then follow the task
dependencies and gates in [`plan.md`](plan.md). The validated evidence and limitations are in
[`assessment/assessment.md`](assessment/assessment.md).

## Intentional Scope

The current demo stays compact so its modernization path remains easy to inspect. Authentication, customer/product/line-item models, status history, server-side querying and pagination, analytics endpoints, charts, bulk actions, exports, and notifications remain out of scope. Java, Spring Boot, and vulnerable dependency upgrades, plus durable PostgreSQL storage and Azure Container Apps deployment, are planned in `plan.md`; they have not yet been implemented.