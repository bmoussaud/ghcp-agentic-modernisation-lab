# Order Service Frozen Behavior Baseline

**Task:** `t1-baseline`  
**Captured:** 2026-09-30 UTC  
**Status:** FROZEN after validation  
**Scope:** Existing regression suite plus application boot/API smoke behavior

This document is the behavior contract for post-migration verification. Every
case below passed against the pre-change snapshot. Files in this `test-cases/`
directory must not be changed, moved, deleted, or extended without an explicit
re-freeze cycle.

## Baseline environment and evidence

- Application: `com.contoso.demo:order-service:1.0.0`
- Spring Boot: `2.7.18`
- Runtime used: Microsoft OpenJDK `17.0.16`
- Maven used: `3.9.16`
- Database: in-memory H2 at `jdbc:h2:mem:orders;DB_CLOSE_DELAY=-1`
- Existing-suite command: `mvn test`
- Existing-suite result: **20 run, 20 passed, 0 failures, 0 errors, 0 skipped**
- Build result: **BUILD SUCCESS**
- Test completion time: `2026-09-30T09:16:11Z`
- Boot command: `mvn spring-boot:run`
- Boot evidence: embedded Tomcat initialized and started on HTTP port `8080`;
  `OrderServiceApplication` reported started in `4.671` seconds.
- Boot smoke request: `GET http://127.0.0.1:8080/api/orders`
- Boot smoke result: **HTTP 200** with the three seeded orders described in
  `BR-021`.

## General test rules

- Use an isolated in-memory database for persistence and full-context cases.
- Do not depend on identifiers or timestamps being equal to the observed
  baseline values; assert that generated identifiers/timestamps are present
  where applicable.
- Decimal comparisons are exact and retain the values shown below.
- HTTP cases use JSON requests and responses unless stated otherwise.
- Service unit cases isolate `OrderService` with a mocked `OrderRepository`.
- Controller slice cases isolate `OrderController` with a mocked
  `OrderService`.
- No external Azure resource or secret is required by this baseline.
- No external fixture files are required; all inputs are specified inline.

## Frozen cases

### Repository behavior

#### BR-001 — Find orders by customer

- **Origin:** `OrderRepositoryTest.findByCustomerReturnsOnlyMatchingOrders`
- **Given:** Persist orders `erin/5.00`, `erin/7.00`, and `frank/9.00`.
- **When:** Query the repository for customer `erin`.
- **Then:** Exactly two orders are returned.

#### BR-002 — Constructor-created order receives persistence defaults

- **Origin:** `OrderRepositoryTest.createdAtIsPopulatedOnSave`
- **Given:** An order constructed as `grace/3.50`.
- **When:** Save the order.
- **Then:** Customer is `grace`, `createdAt` is non-null, and status is
  `PENDING`.

#### BR-003 — REST-style order receives a creation timestamp

- **Origin:** `OrderRepositoryTest.createdAtIsPopulatedForRestStyleOrderOnSave`
- **Given:** A no-argument order whose customer is `harry` and amount is
  `11.25`.
- **When:** Save and flush the order.
- **Then:** `createdAt` is non-null.

#### BR-004 — Explicit status is persisted

- **Origin:** `OrderRepositoryTest.statusIsPersisted`
- **Given:** An order `helen/18.25` with status `COMPLETED`.
- **When:** Save and flush it, then reload it by generated ID.
- **Then:** The reloaded status is `COMPLETED`.

### Service behavior

#### BR-005 — Full-context customer total

- **Origin:** `OrderServiceTest.totalForCustomerSumsAmounts`
- **Given:** Persist orders for `charlie` with amounts `10.00` and `15.50`.
- **When:** Request the total for `charlie` from `OrderService`.
- **Then:** The total is exactly `25.50`.

#### BR-006 — Empty customer total is zero

- **Origin:** `OrderServiceUnitTest.totalForCustomerReturnsZeroWhenNoOrders`
- **Given:** The repository returns no orders for `nobody`.
- **When:** Request the total for `nobody`.
- **Then:** The total equals `BigDecimal.ZERO`.

#### BR-007 — Customer total sums every amount

- **Origin:** `OrderServiceUnitTest.totalForCustomerSumsAllAmounts`
- **Given:** The repository returns `alice/120.50` and `alice/80.00`.
- **When:** Request the total for `alice`.
- **Then:** The total is exactly `200.50`.

#### BR-008 — Find by ID delegates and returns the order

- **Origin:** `OrderServiceUnitTest.findByIdDelegatesToRepository`
- **Given:** Repository ID `1` resolves to `bob/42.99`.
- **When:** Find order ID `1`.
- **Then:** A result is present and its customer is `bob`.

#### BR-009 — Create forces PENDING and saves

- **Origin:** `OrderServiceUnitTest.createPersistsOrder`
- **Given:** An order `carol/10.00` whose incoming status is `COMPLETED`.
- **When:** Create the order through the service.
- **Then:** Its status is changed to `PENDING` and the repository saves it.

#### BR-010 — Status update persists the requested status

- **Origin:** `OrderServiceUnitTest.updateStatusPersistsTheRequestedStatus`
- **Given:** Repository ID `4` resolves to `dave/15.00`.
- **When:** Update ID `4` to `PROCESSING`.
- **Then:** A result is present, its status is `PROCESSING`, and it is saved.

#### BR-011 — Missing status update does not save

- **Origin:** `OrderServiceUnitTest.updateStatusReturnsEmptyWhenOrderIsMissing`
- **Given:** Repository ID `99` does not exist.
- **When:** Update ID `99` to `COMPLETED`.
- **Then:** The result is empty and no order is saved.

### HTTP controller behavior

#### BR-012 — List endpoint returns all supplied orders

- **Origin:** `OrderControllerTest.listReturnsAllOrders`
- **Given:** The service supplies `alice/120.50` and `bob/42.99`.
- **When:** `GET /api/orders`.
- **Then:** Response status is `200`, the JSON array length is `2`, and the
  first customer's value is `alice`.

#### BR-013 — Missing order returns 404

- **Origin:** `OrderControllerTest.getByIdReturns404WhenMissing`
- **Given:** Service ID `99` does not exist.
- **When:** `GET /api/orders/99`.
- **Then:** Response status is `404`.

#### BR-014 — Customer total endpoint returns a decimal value

- **Origin:** `OrderControllerTest.totalForCustomerReturnsValue`
- **Given:** The service total for `alice` is `200.50`.
- **When:** `GET /api/orders/customer/alice/total`.
- **Then:** Response status is `200` and the body is `200.50`.

#### BR-015 — Valid create returns 201

- **Origin:** `OrderControllerTest.createReturns201`
- **Given:** JSON `{"customer":"dave","amount":15.00}` and a service result
  `dave/15.00`.
- **When:** `POST /api/orders` with content type `application/json`.
- **Then:** Response status is `201` and response customer is `dave`.

#### BR-016 — Empty create payload is rejected

- **Origin:** `OrderControllerTest.createRejectsInvalidPayload`
- **Given:** JSON `{}`.
- **When:** `POST /api/orders` with content type `application/json`.
- **Then:** Response status is `400`.

#### BR-017 — Blank customer and zero amount are rejected

- **Origin:**
  `OrderControllerTest.createRejectsBlankCustomerAndNonPositiveAmount`
- **Given:** JSON `{"customer":"   ","amount":0}`.
- **When:** `POST /api/orders` with content type `application/json`.
- **Then:** Response status is `400`.

#### BR-018 — Valid status update returns the updated order

- **Origin:** `OrderControllerTest.updateStatusReturnsUpdatedOrder`
- **Given:** Updating service ID `4` to `PROCESSING` returns `dave/15.00` with
  status `PROCESSING`.
- **When:** `PATCH /api/orders/4/status?status=PROCESSING`.
- **Then:** Response status is `200` and JSON status is `PROCESSING`.

#### BR-019 — Missing status update returns 404

- **Origin:** `OrderControllerTest.updateStatusReturns404WhenOrderIsMissing`
- **Given:** Updating service ID `99` to `COMPLETED` returns no order.
- **When:** `PATCH /api/orders/99/status?status=COMPLETED`.
- **Then:** Response status is `404`.

#### BR-020 — Unknown status is rejected

- **Origin:** `OrderControllerTest.updateStatusRejectsUnknownStatus`
- **Given:** Status text `UNKNOWN`.
- **When:** `PATCH /api/orders/1/status?status=UNKNOWN`.
- **Then:** Response status is `400`.

### Application boot behavior

#### BR-021 — Application boots and serves seeded orders

- **Origin:** Pre-change `mvn spring-boot:run` smoke check.
- **Given:** The application starts with its default configuration and empty
  in-memory H2 database.
- **When:** Startup completes and `GET /api/orders` is requested.
- **Then:**
  - Startup succeeds without an active profile.
  - Embedded HTTP server listens on configured port `8080`.
  - Response status is `200`.
  - Response is a JSON array containing exactly three seeded orders:
    `alice/120.50/PENDING`, `alice/80.00/PROCESSING`, and
    `bob/42.99/COMPLETED`.
  - Every returned order has a generated ID and non-null `createdAt`.

## Validation accounting

| Evidence group | Cases | Passed | Failed |
|---|---:|---:|---:|
| Existing JUnit suite | 20 | 20 | 0 |
| Application boot/API smoke | 1 | 1 | 0 |
| **Frozen contract total** | **21** | **21** | **0** |

