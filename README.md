# LogiFleet Fleet Operations Platform

Spring Boot microservices for commercial fleet tracking, dispatch, and maintenance.

## Services

| Service | Port | Responsibility |
|---|---:|---|
| Eureka Server | 8761 | Service registry |
| Auth Service | 8081 | Register, login, JWT issuing |
| Vehicle Service | 8082 | Vehicles, locations, availability |
| Maintenance Service | 8083 | Maintenance work orders and repair history |
| Trip Service | 8084 | Trip lifecycle and vehicle/maintenance coordination |
| API Gateway | 8080 | Single entry point and load-balanced routing |

All business services share one MySQL database. JPA creates and updates its tables automatically at startup.

## Execute the project

### Prerequisites

Install Java 21, Maven 3.9+, and MySQL 8. Make sure MySQL is running on port `3306` and its `root` password is `2007`. If your password is different, edit `spring.datasource.password` in the Auth, Vehicle, Trip, and Maintenance Service `application.properties` files.

### Create the database

In MySQL Workbench or the MySQL command line, run:

```sql
CREATE DATABASE IF NOT EXISTS logifleet_db;
```

The services automatically create the `users`, `vehicle`, `trip`, and `maintenance` tables when they start.

### Build and run

Open a terminal in the project root and run this once after any code change:

```powershell
mvn clean package
```

Then open six terminals in the project root and start the applications in this order:

```powershell
mvn -pl eureka-server spring-boot:run
mvn -pl auth-service spring-boot:run
mvn -pl vehicle-service spring-boot:run
mvn -pl maintenance-service spring-boot:run
mvn -pl trip-service spring-boot:run
mvn -pl api-gateway spring-boot:run
```

Wait until each terminal reports `Started ...Application` before starting the next one. Open Eureka at `http://localhost:8761` and confirm all six services are registered. All application requests should use the API Gateway at `http://localhost:8080`.

Alternatively, with Docker Desktop installed, run `docker compose up --build` from the project folder. It starts MySQL and creates `logifleet_db` automatically.

## Postman: create and use users

### 1. Register a user

In Postman, create a `POST` request:

```text
http://localhost:8080/auth/register
```

Set the `Content-Type` header to `application/json`, choose **Body → raw → JSON**, and send:

```json
{
  "username": "admin",
  "password": "Admin@123",
  "role": "ADMIN"
}
```

The response contains a JWT token. Copy the value of `token`. You can register additional users by changing `username`; valid example roles are `ADMIN` and `DISPATCHER`.

### 2. Log in later

Use another `POST` request:

```text
http://localhost:8080/auth/login
```

```json
{
  "username": "admin",
  "password": "Admin@123"
}
```

Copy `token` from the response.

### 3. Authorize Postman requests

For every Vehicle, Trip, or Maintenance request, open the **Authorization** tab in Postman, choose **Bearer Token**, and paste the JWT token. Requests without the token return HTTP `401 Unauthorized`.

## Quick API demo

Create a vehicle with a `POST` request to `http://localhost:8080/vehicles` and this JSON body:

```json
{
  "registrationNumber": "KA01AB1234",
  "type": "TRUCK",
  "status": "AVAILABLE",
  "location": "Bengaluru"
}
```

Assign a trip with a `POST` request to `http://localhost:8080/trips`:

```json
{
  "vehicleId": 1,
  "driverId": "DRV-101",
  "origin": "Bengaluru",
  "destination": "Chennai"
}
```

Update a trip with a `PATCH` request:

```text
http://localhost:8080/trips/1/status?status=IN_PROGRESS
```

## PowerShell quick demo

Register and log in (the initial user is made an ADMIN):

```powershell
$body = '{"username":"admin","password":"Admin@123","role":"ADMIN"}'
Invoke-RestMethod http://localhost:8080/auth/register -Method Post -ContentType application/json -Body $body
$login = Invoke-RestMethod http://localhost:8080/auth/login -Method Post -ContentType application/json -Body $body
$headers = @{ Authorization = "Bearer $($login.token)" }
```

Create a vehicle, assign a trip, then update the trip status:

```powershell
$vehicle = Invoke-RestMethod http://localhost:8080/vehicles -Method Post -Headers $headers -ContentType application/json -Body '{"registrationNumber":"KA01AB1234","type":"TRUCK","status":"AVAILABLE","location":"Bengaluru"}'
$trip = Invoke-RestMethod http://localhost:8080/trips -Method Post -Headers $headers -ContentType application/json -Body "{\"vehicleId\":$($vehicle.id),\"driverId\":\"DRV-101\",\"origin\":\"Bengaluru\",\"destination\":\"Chennai\"}"
Invoke-RestMethod "http://localhost:8080/trips/$($trip.id)/status?status=IN_PROGRESS" -Method Patch -Headers $headers
```

Routes: `/auth/**`, `/vehicles/**`, `/trips/**`, `/maintenance/**`. All except auth require `Authorization: Bearer <token>`.

## MySQL database details

Database: `logifleet_db`

Tables created by the services: `users`, `vehicle`, `trip`, and `maintenance`. On a fresh database, Vehicle, Trip, and Maintenance Services add sample records automatically; the Auth Service creates users only through `/auth/register`.

For a local MySQL installation, the configured values are host `localhost`, port `3306`, username `root`, password `2007`. Update the `spring.datasource.username` and `spring.datasource.password` properties in each business service if your MySQL credentials differ. Run `database/init-databases.sql` once if the database user cannot create schemas automatically.

## Verification checklist

1. Register and log in; calls without a token return HTTP 401.
2. Create an `AVAILABLE` vehicle and assign it to a trip; its status changes to `ON_TRIP`.
3. Start and complete the trip; its status becomes `COMPLETED` and the vehicle becomes `AVAILABLE`.
4. Create an `OPEN` maintenance record and try to assign that vehicle; Trip Service returns HTTP 409.
5. Complete the maintenance order and assign the vehicle again; the assignment succeeds.
