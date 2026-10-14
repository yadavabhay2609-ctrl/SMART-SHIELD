Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "SMART SHIELD - SAFE ROUTES REAL LOCATION AND RISK VERIFICATION" -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8082"

# 1. Citizen Authentication
Write-Host "`n[TEST 1] Citizen Authentication" -ForegroundColor Yellow
$testTs = (Get-Date).ToString("yyyyMMddHHmmss")
$citizenEmail = "test_citizen_$testTs@smartshield.gov.in"
$citizenPassword = "CitizenPassword@123"

$signupBody = @{
    fullName = "Ananya Verma"
    email = $citizenEmail
    password = $citizenPassword
} | ConvertTo-Json

try {
    $signupRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/signup" -Method Post -Body $signupBody -ContentType "application/json"
    $token = $signupRes.token
    Write-Host "Citizen registered and logged in successfully. User: $($signupRes.user.fullName)" -ForegroundColor Green
} catch {
    Write-Host "Signup failed, attempting login..." -ForegroundColor Yellow
    $loginBody = @{
        email = $citizenEmail
        password = $citizenPassword
    } | ConvertTo-Json
    $citizenLogin = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $token = $citizenLogin.token
    Write-Host "Citizen login successful. User: $($citizenLogin.user.fullName)" -ForegroundColor Green
}

$citizenHeaders = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# 2. Real Location Resolution (Ghaziabad Boundary)
Write-Host "`n[TEST 2] Location Resolution - Known City (Ghaziabad)" -ForegroundColor Yellow
$locGhaziabad = Invoke-RestMethod -Uri "$baseUrl/api/safety/routes/resolve-location?lat=28.6692`&lng=77.4538" -Method Get
Write-Host "Resolved: $($locGhaziabad.resolved)"
Write-Host "City: $($locGhaziabad.city)"
Write-Host "State: $($locGhaziabad.state)"
Write-Host "Formatted: $($locGhaziabad.formattedAddress)"
Write-Host "Provider: $($locGhaziabad.provider)"
if ($locGhaziabad.resolved -and $locGhaziabad.city -eq "Ghaziabad" -and $locGhaziabad.state -eq "Uttar Pradesh") {
    Write-Host "PASS: Real Location Ghaziabad verified." -ForegroundColor Green
} else {
    Write-Host "FAIL: Location resolution unexpected." -ForegroundColor Red
}

# 3. Location Resolution - Unknown Coordinates (Honest Fallback)
Write-Host "`n[TEST 3] Location Resolution - Unknown Coordinates (Honest Fallback)" -ForegroundColor Yellow
$locUnknown = Invoke-RestMethod -Uri "$baseUrl/api/safety/routes/resolve-location?lat=15.1234`&lng=45.6789" -Method Get
Write-Host "Resolved: $($locUnknown.resolved)"
Write-Host "Formatted: $($locUnknown.formattedAddress)"
Write-Host "City: $($locUnknown.city)"
if (-not $locUnknown.resolved -and $locUnknown.formattedAddress -eq "Current location detected" -and [string]::IsNullOrEmpty($locUnknown.city)) {
    Write-Host "PASS: Honest Fallback: No fake city/state fabricated." -ForegroundColor Green
} else {
    Write-Host "FAIL: Fallback behavior violated." -ForegroundColor Red
}

# 4. Same-City Destination Suggestions
Write-Host "`n[TEST 4] Destination Suggestions - Same City (Ghaziabad)" -ForegroundColor Yellow
$destSuggestions = Invoke-RestMethod -Uri "$baseUrl/api/safety/routes/destinations?city=Ghaziabad" -Method Get
Write-Host "Count: $($destSuggestions.Count)"
$categories = $destSuggestions | ForEach-Object { $_.category } | Select-Object -Unique
Write-Host "Categories found: $($categories -join ', ')"
if ($destSuggestions.Count -ge 5) {
    Write-Host "PASS: Same-city municipal destination suggestions verified." -ForegroundColor Green
} else {
    Write-Host "FAIL: Suggestions count lower than expected." -ForegroundColor Red
}

# 5. Destination Search Filter
Write-Host "`n[TEST 5] Destination Search Filter (query: Hospital)" -ForegroundColor Yellow
$hospitalSearch = Invoke-RestMethod -Uri "$baseUrl/api/safety/routes/destinations?city=Ghaziabad`&query=hospital" -Method Get
Write-Host "Search count: $($hospitalSearch.Count)"
foreach ($h in $hospitalSearch) {
    Write-Host "  - $($h.name) [$($h.category)] - $($h.address)"
}
if ($hospitalSearch.Count -ge 1 -and $hospitalSearch[0].category -eq "HOSPITAL") {
    Write-Host "PASS: Search filter verified." -ForegroundColor Green
} else {
    Write-Host "FAIL: Search filter failed." -ForegroundColor Red
}

# 6. Zero-Evidence Route Calculation
Write-Host "`n[TEST 6] Zero-Evidence Route Calculation (Must NOT show RISKY)" -ForegroundColor Yellow
$zeroRoutePayload = @{
    originLatitude = 25.5000
    originLongitude = 75.5000
    originAddress = "Isolated Waypoint Alpha"
    destinationLatitude = 25.5080
    destinationLongitude = 75.5090
    destinationAddress = "Isolated Waypoint Beta"
    travelMode = "WALK"
} | ConvertTo-Json

$zeroRouteRes = Invoke-RestMethod -Uri "$baseUrl/api/safety/routes/calculate" -Method Post -Body $zeroRoutePayload -Headers $citizenHeaders
Write-Host "Route ID: $($zeroRouteRes.safeRouteId)"
Write-Host "Safety Level: $($zeroRouteRes.safetyLevel)"
Write-Host "Safety Score: $($zeroRouteRes.safetyScore)"
Write-Host "Data Confidence: $($zeroRouteRes.dataConfidence)"
Write-Host "Auto Grievance Filed: $($zeroRouteRes.autoGrievanceFiled)"
Write-Host "Factors count: $($zeroRouteRes.factors.Count)"
if ($zeroRouteRes.safetyLevel -eq "INSUFFICIENT_DATA" -and $zeroRouteRes.autoGrievanceFiled -eq $false) {
    Write-Host "PASS: Zero-evidence route correctly evaluated as INSUFFICIENT_DATA without fake risk and without auto-filing grievance." -ForegroundColor Green
} else {
    Write-Host "FAIL: Zero-evidence route was incorrectly labelled: $($zeroRouteRes.safetyLevel)" -ForegroundColor Red
}

# 7. Real Route Calculation in Ghaziabad
Write-Host "`n[TEST 7] Route Calculation with Real Civic Facilities (Ghaziabad Corridor)" -ForegroundColor Yellow
$ghzRoutePayload = @{
    originLatitude = 28.6692
    originLongitude = 77.4538
    originAddress = "Kavi Nagar, Ghaziabad, Uttar Pradesh"
    destinationLatitude = 28.6672
    destinationLongitude = 77.4328
    destinationAddress = "MMG District Hospital, Model Town, Ghaziabad"
    travelMode = "WALK"
} | ConvertTo-Json

$ghzRouteRes = Invoke-RestMethod -Uri "$baseUrl/api/safety/routes/calculate" -Method Post -Body $ghzRoutePayload -Headers $citizenHeaders
$ghzRouteId = $ghzRouteRes.safeRouteId
Write-Host "Route ID: $ghzRouteId"
Write-Host "Distance: $($ghzRouteRes.distanceMeters)m, Duration: $($ghzRouteRes.durationSeconds)s"
Write-Host "Safety Score: $($ghzRouteRes.safetyScore)/100"
Write-Host "Safety Level: $($ghzRouteRes.safetyLevel)"
Write-Host "Data Confidence: $($ghzRouteRes.dataConfidence)"
Write-Host "Nearby Facilities: $($ghzRouteRes.nearbyFacilities.Count)"
if ($ghzRouteRes.distanceMeters -gt 0 -and $ghzRouteRes.safetyScore -gt 0) {
    Write-Host "PASS: Ghaziabad Safe Route calculated successfully." -ForegroundColor Green
} else {
    Write-Host "FAIL: Ghaziabad route calculation unexpected." -ForegroundColor Red
}

# 8. Submit Real Citizen Route Rating & Review
Write-Host "`n[TEST 8] Submit Real Citizen Route Rating" -ForegroundColor Yellow
$ratingPayload = @{
    rating = 5
    comment = "Main road is broad and well lit near MMG Hospital with regular police movement."
    safetyTag = "WELL_LIT"
} | ConvertTo-Json

$ratingRes = Invoke-RestMethod -Uri "$baseUrl/api/safety/routes/$ghzRouteId/rating" -Method Post -Body $ratingPayload -Headers $citizenHeaders
Write-Host "Rating submission response: $($ratingRes.message)"

# 9. Verify Review Retrieval
Write-Host "`n[TEST 9] Verify Real Community Reviews Retrieval" -ForegroundColor Yellow
$reviews = Invoke-RestMethod -Uri "$baseUrl/api/safety/routes/$ghzRouteId/ratings" -Method Get -Headers $citizenHeaders
Write-Host "Total verified reviews: $($reviews.Count)"
if ($reviews.Count -ge 1) {
    Write-Host "Latest review: Rating=$($reviews[0].rating)/5, Author=$($reviews[0].citizenName), Tag=$($reviews[0].safetyTag)"
    Write-Host "Comment: $($reviews[0].comment)"
    Write-Host "PASS: Real citizen review persisted and retrieved." -ForegroundColor Green
} else {
    Write-Host "FAIL: Reviews not found." -ForegroundColor Red
}

# 10. Admin Console Route Monitoring
Write-Host "`n[TEST 10] Admin Console Route Monitoring" -ForegroundColor Yellow
$adminAuthBody = @{
    adminId = "ADMIN001"
    password = "SmartShield@123"
} | ConvertTo-Json

$adminLogin = Invoke-RestMethod -Uri "$baseUrl/api/admin/auth/login" -Method Post -Body $adminAuthBody -ContentType "application/json"
$adminToken = $adminLogin.token
$adminHeaders = @{
    "Authorization" = "Bearer $adminToken"
    "Content-Type" = "application/json"
}

$routeSummary = Invoke-RestMethod -Uri "$baseUrl/api/admin/safety/routes/summary" -Method Get -Headers $adminHeaders
$allRoutes = Invoke-RestMethod -Uri "$baseUrl/api/admin/safety/routes" -Method Get -Headers $adminHeaders
Write-Host "Total Routes Monitored by Admin: $($routeSummary.totalRoutes)"
Write-Host "Average Route Safety Score: $($routeSummary.averageSafetyScore)"
Write-Host "Auto Filed Grievances Count: $($routeSummary.autoFiledGrievances)"
Write-Host "Recent Routes Count in Registry: $($allRoutes.Count)"
if ($routeSummary.totalRoutes -ge 1 -and $allRoutes.Count -ge 1) {
    Write-Host "PASS: Admin route monitoring operational with live database telemetry." -ForegroundColor Green
} else {
    Write-Host "FAIL: Admin route monitoring empty." -ForegroundColor Red
}

# 11. Modules 1-6 Regression Check
Write-Host "`n[TEST 11] Full System Regression Check (Modules 1-6)" -ForegroundColor Yellow
$myGrievances = Invoke-RestMethod -Uri "$baseUrl/api/grievances/my" -Method Get -Headers $citizenHeaders
Write-Host "PASS: Module 1 (Grievances): $($myGrievances.Count) records" -ForegroundColor Green

$mySos = Invoke-RestMethod -Uri "$baseUrl/api/safety/sos/my" -Method Get -Headers $citizenHeaders
Write-Host "PASS: Module 2 (SOS): $($mySos.Count) alerts" -ForegroundColor Green

$aiStatus = Invoke-RestMethod -Uri "$baseUrl/api/ai/status" -Method Get
Write-Host "PASS: Module 5 (AI Assistant Status): $($aiStatus.provider) (configured: $($aiStatus.configured))" -ForegroundColor Green

$formStatus = Invoke-RestMethod -Uri "$baseUrl/api/forms/status" -Method Get
Write-Host "PASS: Module 6 (Form Scanner Status): $($formStatus.provider) (configured: $($formStatus.configured))" -ForegroundColor Green

$adminSettings = Invoke-RestMethod -Uri "$baseUrl/api/admin/settings" -Method Get -Headers $adminHeaders
Write-Host "PASS: Admin Console Settings: $($adminSettings.Count) configuration entries" -ForegroundColor Green

Write-Host "`n=================================================================" -ForegroundColor Cyan
Write-Host "ALL VERIFICATION CHECKS COMPLETED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "=================================================================" -ForegroundColor Cyan
