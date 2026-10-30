# LogiFleet Fleet Operations Platform
## SOA Review 2 Rubric Assessment

**Assessment date:** 30 October 2026  
**Assessment basis:** Source code, project documentation, automated tests, and Maven validation available in the workspace. Scores below are estimates against the supplied rubric, not official marks.

## Project overview

LogiFleet is a Java 21 / Spring Boot microservices application for fleet operations. Its Maven modules comprise a Eureka service registry, an API Gateway, an authentication service, and separate Vehicle, Trip, and Maintenance services. The business services use Spring Data JPA and MySQL. Trip assignment coordinates with Vehicle and Maintenance services through service-discovered clients.

## Rubric assessment

| Rubric area | Estimated score | Evidence and assessment |
|---|---:|---|
| Microservice implementation, service discovery, and configuration | **8/10** | Six separate Maven modules are registered in the parent build. Business services configure Eureka clients, and Gateway routes use service IDs. This is a credible modular microservice implementation. Runtime service registration was not demonstrated in this assessment, so the top band is not yet evidenced. |
| JWT authentication and security | **6/10** | Auth issues signed, expiring JWTs and stores passwords using BCrypt. Gateway and business-service filters validate token signatures; business endpoints require authentication and apply role checks. The public registration endpoint accepts a requested role, so a caller can request `ADMIN`; roles are also currently granted the same access to the protected controllers. This limits the strength of the authorization model. |
| API Gateway and service integration | **7/10** | Gateway routes auth, vehicle, trip, and maintenance paths and validates JWTs for non-auth routes. Trip assignment checks vehicle availability and open maintenance before assigning, and forwards the caller’s authorization header to service clients. Full live routing and service-to-service behavior still need demonstration. |
| Testing, validation, and demonstration | **5/10** | Five focused unit tests cover auth registration/login, the public auth filter bypass, trip assignment, and a trip status transition. The full Maven test command passed. There are no end-to-end or integration tests in the current evidence, and the live demo could not be run because Docker was unavailable and database-backed services were not started. |
| MOOCs completion status | **Not assessable** | Certificates and LMS submission proof are external evidence and are not present in the source-code workspace. Attach valid certificates and submission evidence separately; no completion claim is made here. |

**Technical subtotal:** **26/40** across the four assessable implementation criteria.  
**MOOC criterion:** pending external evidence; excluded from the technical subtotal.

## Implemented behavior

- User registration and login return signed JWTs; passwords are BCrypt-encoded.
- JWTs expire, and protected service endpoints reject absent or invalid tokens.
- Vehicle, Trip, and Maintenance APIs support their core CRUD/status workflows.
- Trip assignment checks that the vehicle is available and has no incomplete maintenance order; successful assignment marks the vehicle `ON_TRIP`.
- Completing or cancelling a trip returns its vehicle to `AVAILABLE`.
- Gateway routes external requests to the registered service instances.

## Verification performed

- `.\mvnw -q -pl eureka-server,auth-service,vehicle-service,maintenance-service,trip-service,api-gateway test` — **passed** after the latest auth-filter correction.
- `.\mvnw -q -pl eureka-server,auth-service,vehicle-service,maintenance-service,trip-service,api-gateway -DskipTests package` — **passed** before that correction. The later test run recompiled the modules.
- The test run emitted Mockito/JDK dynamic-agent warnings; these did not fail the tests.
- Docker was unavailable in the assessment environment. Consequently, container startup, Eureka registration, database connectivity, and the end-to-end API flow remain **unverified at runtime**.

## Remaining work to target the top rubric band

1. **Authorization:** prevent public registration from creating administrators; define which operations require `ADMIN` versus `DISPATCHER`; add tests for denied roles and invalid/expired tokens.
2. **Integration validation:** add automated tests for Gateway routing, authentication through the Gateway, and Trip-to-Vehicle/Maintenance interactions. Run the services with MySQL and capture successful and negative API scenarios.
3. **Configuration and deployment:** keep secrets and database credentials outside source-controlled defaults; verify container health/readiness and startup dependencies before the demonstration.
4. **MOOC evidence:** attach the required certificates and LMS submission proof; this cannot be established from application code.

## Demonstration evidence checklist

- Eureka dashboard showing all six applications registered.
- Successful registration and login, plus an unauthenticated protected request returning HTTP 401.
- Vehicle creation and trip assignment, showing the vehicle transitions to `ON_TRIP`.
- Maintenance-blocked assignment returning HTTP 409, followed by successful assignment after maintenance completion.
- Trip completion returning the vehicle to `AVAILABLE`.
- Test output and separate MOOC certificates/LMS proof.
