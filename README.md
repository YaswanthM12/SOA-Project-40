# LogiFleet Fleet Operations Platform

LogiFleet is a Spring Boot microservices application for fleet tracking, trip dispatch, and maintenance management.

## Services

| Service | Port | Responsibility |
|---|---:|---|
| Eureka Server | 8761 | Service registry |
| Auth Service | 8081 | User registration, login, and JWT issuing |
| Vehicle Service | 8082 | Vehicle records, locations, and availability |
| Maintenance Service | 8083 | Maintenance work orders |
| Trip Service | 8084 | Trip lifecycle and vehicle/maintenance coordination |
| API Gateway | 8080 | Client entry point and service-discovered routing |

Business services use MySQL and register with Eureka. Client requests should normally go through the API Gateway at `http://localhost:8080`.

## Prerequisites

For running services directly on Windows, install:

- Java 21
- MySQL 8.x, running on port `3306`
- Postman (for the API walkthrough)

Maven is included through the Maven Wrapper (`mvnw.cmd`); a separate Maven installation is not required.

Alternatively, install Docker Desktop with Docker Compose to run the containerized setup.

## Configure and run locally

### 1. Create the database

Create the database using MySQL Workbench or a MySQL command prompt:

```sql
CREATE DATABASE IF NOT EXISTS logifleet_db;
```

The services create or update their tables at startup. Make sure the MySQL username and password configured in the four business-service `application.properties` files match your local MySQL account. Do not commit real credentials to source control.

### 2. Build and test

Open PowerShell in the project root and run:

```powershell
.\mvnw.cmd clean verify
```

To package without running tests:

```powershell
.\mvnw.cmd clean package -DskipTests
```

### 3. Start the services

Open a separate PowerShell terminal in the project root for each command. Start Eureka first, then the business services, and the Gateway last. Wait for each service to log `Started ...Application` before moving to the next terminal.

**Terminal 1 — Eureka:**

```powershell
.\mvnw.cmd -pl eureka-server spring-boot:run
```

**Terminal 2 — Auth:**

```powershell
.\mvnw.cmd -pl auth-service spring-boot:run
```

**Terminal 3 — Vehicle:**

```powershell
.\mvnw.cmd -pl vehicle-service spring-boot:run
```

**Terminal 4 — Maintenance:**

```powershell
.\mvnw.cmd -pl maintenance-service spring-boot:run
```

**Terminal 5 — Trip:**

```powershell
.\mvnw.cmd -pl trip-service spring-boot:run
```

**Terminal 6 — API Gateway:**

```powershell
.\mvnw.cmd -pl api-gateway spring-boot:run
```

Open [Eureka](http://localhost:8761) and confirm the services register. The Eureka Server itself does not register as a client. Use `http://localhost:8080` as the base URL for API calls.

### Docker Compose alternative

From the project root, run:

```powershell
docker compose up --build
```

This starts MySQL and the application containers. On shutdown, use `Ctrl+C`; to remove the containers while preserving the database volume, run `docker compose down`.

## Postman walkthrough

This walkthrough uses the Gateway and demonstrates registration/login, JWT authorization, vehicle creation, maintenance checks, and trip status changes.

### 1. Create a Postman environment

Create an environment with these variables:

| Variable | Initial value |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `token` | *(leave empty)* |
| `vehicleId` | *(leave empty)* |
| `tripId` | *(leave empty)* |
| `maintenanceId` | *(leave empty)* |

For JSON requests, set `Content-Type: application/json`. Use **Authorization → Bearer Token** for protected requests and set the token to `{{token}}`. Do not include the word `Bearer` in the token field; Postman adds it.

### 2. Register a demo user

- Method: `POST`
- URL: `{{baseUrl}}/auth/register`
- Body: **raw → JSON**

```json
{
  "username": "admin",
  "password": "Admin@123",
  "role": "ADMIN"
}
```

Expected response: `201 Created`, containing `token`, `username`, and `role`. In the **Tests** tab, save the token:

```javascript
const body = pm.response.json();
pm.environment.set("token", body.token);
```

Registration returns `409 Conflict` if that username already exists. In that case, use the login request below.

> This role is shown for a local classroom demonstration. Do not expose public self-registration with administrator role assignment in a deployed system.

### 3. Log in (if the user already exists)

- Method: `POST`
- URL: `{{baseUrl}}/auth/login`
- Body: **raw → JSON**

```json
{
  "username": "admin",
  "password": "Admin@123"
}
```

Expected response: `200 OK`. Use the same Tests-tab token-saving script from registration.

### 4. Check that protected routes require authentication

Send a request without authorization:

- Method: `GET`
- URL: `{{baseUrl}}/vehicles`
- Authorization: **No Auth**

Expected response: `401 Unauthorized`. For the remaining requests, set **Authorization → Bearer Token** to `{{token}}`.

### 5. Create a vehicle

- Method: `POST`
- URL: `{{baseUrl}}/vehicles`
- Authorization: Bearer Token `{{token}}`
- Body: **raw → JSON**

```json
{
  "registrationNumber": "KA01AB1234",
  "type": "TRUCK",
  "status": "AVAILABLE",
  "location": "Bengaluru"
}
```

Expected response: `201 Created`. Save its generated ID in the Tests tab:

```javascript
pm.environment.set("vehicleId", pm.response.json().id);
```

If the registration number is already present, use a different value or select an available vehicle with `GET {{baseUrl}}/vehicles`.

### 6. Create an open maintenance order

- Method: `POST`
- URL: `{{baseUrl}}/maintenance`
- Authorization: Bearer Token `{{token}}`
- Body: **raw → JSON**

```json
  {
    "vehicleId": {{vehicleId}},
    "issue": "Routine inspection",
    "status": "OPEN",
    "notes": "Created for the API demo"
  }
```

Expected response: `201 Created`. Save the generated order ID:

```javascript
pm.environment.set("maintenanceId", pm.response.json().id);
```

### 7. Confirm that maintenance blocks trip assignment

- Method: `POST`
- URL: `{{baseUrl}}/trips`
- Authorization: Bearer Token `{{token}}`
- Body: **raw → JSON**

```json
{
  "vehicleId": {{vehicleId}},
  "driverId": "DRV-101",
  "origin": "Bengaluru",
  "destination": "Chennai"
}
```

Expected response: `409 Conflict` because the vehicle has an open maintenance order.

### 8. Complete the maintenance order

- Method: `PATCH`
- URL: `{{baseUrl}}/maintenance/{{maintenanceId}}/status?status=COMPLETED&notes=Inspection%20complete`
- In Postman, paste the URL without Markdown backticks. For maintenance order `2`, use `http://localhost:8080/maintenance/2/status?status=COMPLETED&notes=Inspection%20complete`. Select **Body → none**; `status` and `notes` are query parameters.
- Authorization: Bearer Token `{{token}}`

Expected response: `200 OK`, with the order status set to `COMPLETED`.

### 9. Assign a trip

Resend the `POST {{baseUrl}}/trips` request from step 7.

Expected response: `201 Created`, with trip status `ASSIGNED`. Save the returned trip ID:

```javascript
pm.environment.set("tripId", pm.response.json().id);
```

The vehicle is now marked `ON_TRIP`.

### 10. Update the trip lifecycle

Start the trip:

- Method: `PATCH`
- URL: `{{baseUrl}}/trips/{{tripId}}/status?status=IN_PROGRESS`
- In Postman, use the URL without Markdown backticks. For trip `4`, enter `http://localhost:8080/trips/4/status?status=IN_PROGRESS` exactly; set **Body → none**. The status is a query parameter, not a JSON body.
- Authorization: Bearer Token `{{token}}`

Expected response: `200 OK`, with status `IN_PROGRESS`.

Complete the trip by sending:

- Method: `PATCH`
- URL: `{{baseUrl}}/trips/{{tripId}}/status?status=COMPLETED`
- Authorization: Bearer Token `{{token}}`

Expected response: `200 OK`, with status `COMPLETED`; the vehicle returns to `AVAILABLE`.

### Useful read requests

All require the Bearer Token:

| Method | URL | Purpose |
|---|---|---|
| `GET` | `{{baseUrl}}/vehicles` | List vehicles |
| `GET` | `{{baseUrl}}/vehicles/{{vehicleId}}` | Get a vehicle |
| `GET` | `{{baseUrl}}/maintenance` | List maintenance orders |
| `GET` | `{{baseUrl}}/maintenance/vehicle/{{vehicleId}}/clear` | Check whether a vehicle has open maintenance |
| `GET` | `{{baseUrl}}/trips` | List trips |
| `GET` | `{{baseUrl}}/trips/{{tripId}}` | Get a trip |

## API route summary

| Route | Destination | Authentication |
|---|---|---|
| `/auth/**` | Auth Service | Registration/login are public |
| `/vehicles/**` | Vehicle Service | JWT required |
| `/maintenance/**` | Maintenance Service | JWT required |
| `/trips/**` | Trip Service | JWT required |

The Gateway rejects missing or invalid JWTs on protected routes with `401 Unauthorized`. Business services also validate JWTs directly.

## Database notes

Database name: `logifleet_db`. The services create or update the `users`, `vehicle`, `trip`, and `maintenance` tables at startup. Vehicle, Trip, and Maintenance services may insert sample records when their tables are empty.

The Trip table's lifecycle fields (`assigned_at`, `started_at`, and `completed_at`) are Java `LocalDateTime` values stored as MySQL date-time values. MySQL Workbench displays them in the format `YYYY-MM-DD HH:MM:SS.ffffff` (for example, `2026-09-13 11:39:51.099780`); the six digits after the decimal are fractional seconds. A `NULL` value means that lifecycle timestamp has not been set yet. The API serializes the same value as an ISO date-time string, typically with a `T` between date and time (for example, `2026-09-13T11:39:51.099780`).

The Maintenance table uses `DATE` values for `opened_on` and `completed_on`, so MySQL Workbench displays them as `YYYY-MM-DD` (for example, `2026-09-08`). A `NULL` `completed_on` means the maintenance order has not been completed. The API represents these fields as ISO date strings in the same `YYYY-MM-DD` format.

For local development, configure the same MySQL host, port, username, and password in each business service's `application.properties`. Never commit real passwords or production secrets.

## Verification checklist

1. All five client applications appear in Eureka.
2. Registration/login returns a token; a protected request without a token returns `401`.
3. An open maintenance order blocks trip assignment with `409`.
4. After completing maintenance, trip assignment succeeds and the vehicle becomes `ON_TRIP`.
5. Completing the trip sets its status to `COMPLETED` and the vehicle to `AVAILABLE`.
6. `.\mvnw.cmd clean verify` completes successfully.
