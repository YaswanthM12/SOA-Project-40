Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " LOGIFLEET PLATFORM - END-TO-END VERIFICATION TEST SUITE " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080"
$timestamp = Get-Date -Format "HHmmss"
$testUsername = "test_dispatcher_$timestamp"
$testPassword = "Password@123"

# 1. Unauthenticated Access Test (Should return 401)
Write-Host "`n[TEST 1] Testing JWT Protection on unauthenticated request..." -ForegroundColor Yellow
try {
    $unauth = Invoke-WebRequest -Uri "$baseUrl/vehicles" -Method Get -ErrorAction Stop
    Write-Host " FAIL: Expected 401 Unauthorized, but got $($unauth.StatusCode)" -ForegroundColor Red
    exit 1
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 401) {
        Write-Host " PASS: Unauthenticated access blocked with HTTP 401 Unauthorized" -ForegroundColor Green
    } else {
        Write-Host " FAIL: Unexpected status code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
        exit 1
    }
}

# 2. User Registration
Write-Host "`n[TEST 2] Registering user '$testUsername' with role 'DISPATCHER'..." -ForegroundColor Yellow
$regBody = @{
    username = $testUsername
    password = $testPassword
    role = "DISPATCHER"
} | ConvertTo-Json

$regResponse = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method Post -ContentType "application/json" -Body $regBody
if ($regResponse.token) {
    Write-Host " PASS: User registered successfully. JWT received." -ForegroundColor Green
} else {
    Write-Host " FAIL: Registration did not return a token" -ForegroundColor Red
    exit 1
}

# 3. User Login
Write-Host "`n[TEST 3] Logging in as '$testUsername'..." -ForegroundColor Yellow
$loginBody = @{
    username = $testUsername
    password = $testPassword
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
$jwt = $loginResponse.token
if ($jwt) {
    Write-Host " PASS: Login successful. JWT token length: $($jwt.Length)" -ForegroundColor Green
} else {
    Write-Host " FAIL: Login did not return a token" -ForegroundColor Red
    exit 1
}

$headers = @{
    Authorization = "Bearer $jwt"
    "Content-Type" = "application/json"
}

# 4. Authenticated Request (List vehicles)
Write-Host "`n[TEST 4] Fetching vehicle list with JWT Bearer Token..." -ForegroundColor Yellow
$vehicles = Invoke-RestMethod -Uri "$baseUrl/vehicles" -Method Get -Headers $headers
Write-Host " PASS: Successfully retrieved $($vehicles.Count) existing vehicle(s)" -ForegroundColor Green

# 5. Create Vehicle
$regNum = "KA05LF$timestamp"
Write-Host "`n[TEST 5] Creating new commercial vehicle '$regNum'..." -ForegroundColor Yellow
$newVehicleBody = @{
    registrationNumber = $regNum
    type = "HEAVY_TRUCK"
    status = "AVAILABLE"
    location = "Bengaluru Logistics Hub"
} | ConvertTo-Json

$createdVehicle = Invoke-RestMethod -Uri "$baseUrl/vehicles" -Method Post -Headers $headers -Body $newVehicleBody
$vId = $createdVehicle.id
if ($vId -and $createdVehicle.status -eq "AVAILABLE") {
    Write-Host " PASS: Vehicle created with ID=$vId, Status=$($createdVehicle.status)" -ForegroundColor Green
} else {
    Write-Host " FAIL: Vehicle creation failed" -ForegroundColor Red
    exit 1
}

# 6. Telematics Location Update
Write-Host "`n[TEST 6] Updating live telematics location for Vehicle ID=$vId..." -ForegroundColor Yellow
$locUpdated = Invoke-RestMethod -Uri "$baseUrl/vehicles/$vId/location?location=Hosur+Highway+Checkpoint" -Method Patch -Headers $headers
if ($locUpdated.location -eq "Hosur Highway Checkpoint") {
    Write-Host " PASS: Telematics location updated to '$($locUpdated.location)'" -ForegroundColor Green
} else {
    Write-Host " FAIL: Location update failed" -ForegroundColor Red
    exit 1
}

# 7. Trip Dispatch (Inter-service: Trip -> Vehicle)
Write-Host "`n[TEST 7] Dispatching trip for Vehicle ID=$vId..." -ForegroundColor Yellow
$tripBody = @{
    vehicleId = $vId
    driverId = "DRV-888"
    origin = "Bengaluru Logistics Hub"
    destination = "Chennai Port"
} | ConvertTo-Json

$assignedTrip = Invoke-RestMethod -Uri "$baseUrl/trips" -Method Post -Headers $headers -Body $tripBody
$tripId = $assignedTrip.id
if ($tripId -and $assignedTrip.tripStatus -eq "ASSIGNED") {
    Write-Host " PASS: Trip assigned with ID=$tripId, Status=$($assignedTrip.tripStatus)" -ForegroundColor Green
} else {
    Write-Host " FAIL: Trip assignment failed" -ForegroundColor Red
    exit 1
}

# Verify Vehicle status transitioned to ON_TRIP
$vCheck = Invoke-RestMethod -Uri "$baseUrl/vehicles/$vId" -Method Get -Headers $headers
if ($vCheck.status -eq "ON_TRIP") {
    Write-Host " PASS: Vehicle state automatically transitioned to 'ON_TRIP'" -ForegroundColor Green
} else {
    Write-Host " FAIL: Vehicle state is '$($vCheck.status)', expected 'ON_TRIP'" -ForegroundColor Red
    exit 1
}

# 8. Vehicle Availability Conflict Check (Assigning same vehicle should fail 409)
Write-Host "`n[TEST 8] Testing conflict prevention: Assigning second trip to same vehicle..." -ForegroundColor Yellow
try {
    $conflictTrip = Invoke-RestMethod -Uri "$baseUrl/trips" -Method Post -Headers $headers -Body $tripBody -ErrorAction Stop
    Write-Host " FAIL: Expected HTTP 409 Conflict, but request succeeded" -ForegroundColor Red
    exit 1
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 409) {
        Write-Host " PASS: Conflict prevented with HTTP 409: Vehicle is not available" -ForegroundColor Green
    } else {
        Write-Host " FAIL: Unexpected response: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
        exit 1
    }
}

# 9. Preventive Maintenance Scheduling
$regNumMaint = "KA05MT$timestamp"
Write-Host "`n[TEST 9] Creating Vehicle '$regNumMaint' and scheduling open maintenance..." -ForegroundColor Yellow
$vMaintBody = @{
    registrationNumber = $regNumMaint
    type = "DELIVERY_VAN"
    status = "AVAILABLE"
    location = "Mysuru Service Center"
} | ConvertTo-Json
$vMaint = Invoke-RestMethod -Uri "$baseUrl/vehicles" -Method Post -Headers $headers -Body $vMaintBody
$vMaintId = $vMaint.id

$maintBody = @{
    vehicleId = $vMaintId
    issue = "Transmission fluid check and brake overhaul"
    status = "OPEN"
    notes = "Preventive 50,000 km maintenance"
} | ConvertTo-Json
$maintRecord = Invoke-RestMethod -Uri "$baseUrl/maintenance" -Method Post -Headers $headers -Body $maintBody
$maintId = $maintRecord.id
if ($maintId -and $maintRecord.status -eq "OPEN") {
    Write-Host " PASS: Maintenance work-order created with ID=$maintId, Status=$($maintRecord.status)" -ForegroundColor Green
} else {
    Write-Host " FAIL: Maintenance creation failed" -ForegroundColor Red
    exit 1
}

# 10. Inter-service Conflict: Trip assignment blocked when vehicle has open maintenance
Write-Host "`n[TEST 10] Testing dispatch block on vehicle with open maintenance..." -ForegroundColor Yellow
$maintTripBody = @{
    vehicleId = $vMaintId
    driverId = "DRV-999"
    origin = "Mysuru Service Center"
    destination = "Hubballi"
} | ConvertTo-Json

try {
    $blockedTrip = Invoke-RestMethod -Uri "$baseUrl/trips" -Method Post -Headers $headers -Body $maintTripBody -ErrorAction Stop
    Write-Host " FAIL: Expected HTTP 409 Conflict, but trip was assigned!" -ForegroundColor Red
    exit 1
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 409) {
        Write-Host " PASS: Dispatch blocked with HTTP 409: Vehicle has open maintenance work orders" -ForegroundColor Green
    } else {
        Write-Host " FAIL: Unexpected response: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
        exit 1
    }
}

# 11. Complete Maintenance Work Order
Write-Host "`n[TEST 11] Completing maintenance work order ID=$maintId..." -ForegroundColor Yellow
$completedMaint = Invoke-RestMethod -Uri "$baseUrl/maintenance/$maintId/status?status=COMPLETED&notes=Transmission+serviced+successfully" -Method Patch -Headers $headers
if ($completedMaint.status -eq "COMPLETED") {
    Write-Host " PASS: Maintenance work order completed" -ForegroundColor Green
} else {
    Write-Host " FAIL: Maintenance completion failed" -ForegroundColor Red
    exit 1
}

# 12. Trip Lifecycle Transitions: IN_PROGRESS -> COMPLETED
Write-Host "`n[TEST 12] Progressing Trip ID=$tripId from ASSIGNED to IN_PROGRESS..." -ForegroundColor Yellow
$tripInProgress = Invoke-RestMethod -Uri "$baseUrl/trips/$tripId/status?status=IN_PROGRESS" -Method Patch -Headers $headers
if ($tripInProgress.tripStatus -eq "IN_PROGRESS" -and $tripInProgress.startedAt) {
    Write-Host " PASS: Trip started, startedAt=$($tripInProgress.startedAt)" -ForegroundColor Green
} else {
    Write-Host " FAIL: Trip start transition failed" -ForegroundColor Red
    exit 1
}

Write-Host "`n[TEST 13] Completing Trip ID=$tripId..." -ForegroundColor Yellow
$tripCompleted = Invoke-RestMethod -Uri "$baseUrl/trips/$tripId/status?status=COMPLETED" -Method Patch -Headers $headers
if ($tripCompleted.tripStatus -eq "COMPLETED" -and $tripCompleted.completedAt) {
    Write-Host " PASS: Trip marked COMPLETED, completedAt=$($tripCompleted.completedAt)" -ForegroundColor Green
} else {
    Write-Host " FAIL: Trip complete transition failed" -ForegroundColor Red
    exit 1
}

# Verify vehicle released back to AVAILABLE
$vFinal = Invoke-RestMethod -Uri "$baseUrl/vehicles/$vId" -Method Get -Headers $headers
if ($vFinal.status -eq "AVAILABLE") {
    Write-Host " PASS: Vehicle ID=$vId automatically released back to 'AVAILABLE'" -ForegroundColor Green
} else {
    Write-Host " FAIL: Vehicle status is '$($vFinal.status)', expected 'AVAILABLE'" -ForegroundColor Red
    exit 1
}

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host " ALL 13 END-TO-END SYSTEM VERIFICATION TESTS PASSED!     " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
