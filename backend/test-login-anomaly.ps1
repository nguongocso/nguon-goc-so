# Test script for login anomaly detection
# This script tests failed login attempts and verifies anomaly detection

$baseUrl = 'http://localhost:8080/api/v1'

function WriteSuccess($message) { Write-Host $message -ForegroundColor Green }
function WriteError($message) { Write-Host $message -ForegroundColor Red }
function WriteInfo($message) { Write-Host $message -ForegroundColor Cyan }
function WriteHeader($message) {
    Write-Host "`n================================" -ForegroundColor Yellow
    Write-Host $message -ForegroundColor Yellow
    Write-Host "================================`n" -ForegroundColor Yellow
}

WriteHeader 'TEST 1: Record multiple failed login attempts'
$failureCount = 0
for ($i = 1; $i -le 6; $i++) {
    WriteInfo ("Attempt {0}: Trying to login with wrong password..." -f $i)

    $payload = @{ username = 'admin'; password = 'wrongpassword123' } | ConvertTo-Json -Compress

    try {
        $response = Invoke-WebRequest -Uri "$baseUrl/auth/login" -Method Post -ContentType 'application/json' -Body $payload -ErrorAction Stop
        $body = $response.Content | ConvertFrom-Json
        if ($response.StatusCode -eq 200) {
            WriteSuccess 'Response Status: 200'
            WriteInfo ("Message: {0}" -f $body.message)
        }
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.Value__
        if ($statusCode -eq 400) {
            WriteError 'Failed login (Status 400) - Expected'
            $failureCount++
        }
        else {
            WriteError ("Unexpected status: {0}" -f $statusCode)
        }
    }

    Start-Sleep -Milliseconds 500
}

WriteSuccess ("Total failed login attempts recorded: {0}" -f $failureCount)
WriteInfo 'Waiting 2 seconds before checking anomalies...'
Start-Sleep -Seconds 2

WriteHeader 'TEST 2: Login with correct credentials'
$payload = @{ username = 'admin'; password = 'admin123' } | ConvertTo-Json -Compress

try {
    $response = Invoke-WebRequest -Uri "$baseUrl/auth/login" -Method Post -ContentType 'application/json' -Body $payload -ErrorAction Stop
    $body = $response.Content | ConvertFrom-Json
    $selectionToken = $body.data.selectionToken

    if ($selectionToken) {
        WriteSuccess 'Login successful! Got selection token'
        WriteInfo ("Selection Token: {0}..." -f $selectionToken.Substring(0, 20))
    }
}
catch {
    WriteError ("Login failed: {0}" -f $_)
    exit 1
}

WriteHeader 'TEST 3: Get organizations'
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/auth/organizations" -Method Get -Headers @{ Authorization = "Bearer $selectionToken" }
    $body = $response.Content | ConvertFrom-Json
    $orgId = $body.data[0].organizationId

    WriteSuccess 'Got organizations'
    WriteInfo ("First org ID: {0}" -f $orgId)
}
catch {
    WriteError ("Failed to get organizations: {0}" -f $_)
    exit 1
}

WriteHeader 'TEST 4: Select organization'
$selectPayload = @{ organizationId = $orgId } | ConvertTo-Json -Compress

try {
    $response = Invoke-WebRequest -Uri "$baseUrl/auth/select-organization" -Method Post -ContentType 'application/json' -Headers @{ Authorization = "Bearer $selectionToken" } -Body $selectPayload -ErrorAction Stop
    $body = $response.Content | ConvertFrom-Json
    $accessToken = $body.data.accessToken

    WriteSuccess 'Organization selected! Got access token'
    WriteInfo ("Access Token: {0}..." -f $accessToken.Substring(0, 20))
}
catch {
    WriteError ("Failed to select organization: {0}" -f $_)
    exit 1
}

WriteHeader 'TEST 5: Query login history'
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/auth/security/login-history?page=0&size=20" -Method Get -Headers @{ Authorization = "Bearer $accessToken" }
    $body = $response.Content | ConvertFrom-Json
    $historyItems = $body.data.items
    $totalItems = $body.data.totalElements

    WriteSuccess 'Login history retrieved!'
    WriteInfo ("Total login attempts recorded: {0}" -f $totalItems)
    WriteInfo ("Items on this page: {0}" -f $historyItems.Count)

    WriteInfo 'Recent login attempts:'
    foreach ($item in $historyItems) {
        WriteInfo ("  - {0} | Result: {1} | IP: {2}" -f $item.createdAt, $item.result, $item.ipAddress)
    }
}
catch {
    WriteError ("Failed to query login history: {0}" -f $_)
    WriteError $_.Exception.Message
}

WriteHeader 'TEST 6: Query login anomalies'
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/auth/security/login-anomalies?page=0&size=20" -Method Get -Headers @{ Authorization = "Bearer $accessToken" }
    $body = $response.Content | ConvertFrom-Json
    $anomalyItems = $body.data.items
    $totalAnomalies = $body.data.totalElements

    WriteSuccess 'Login anomalies retrieved!'
    WriteInfo ("Total anomalies detected: {0}" -f $totalAnomalies)

    if ($anomalyItems.Count -gt 0) {
        WriteSuccess 'Anomalies were detected!'
        WriteInfo 'Anomaly details:'
        foreach ($item in $anomalyItems) {
            WriteInfo ("  - Username: {0}" -f $item.username)
            WriteInfo ("    Reason: {0}" -f $item.reasonCode)
            WriteInfo ("    Status: {0}" -f $item.status)
            WriteInfo ("    Attempt Count: {0}" -f $item.attemptCount)
            WriteInfo ("    Detected At: {0}" -f $item.detectedAt)
        }
    }
    else {
        WriteError 'No anomalies detected (expected at least one after 5+ failed logins)'
    }
}
catch {
    WriteError ("Failed to query login anomalies: {0}" -f $_)
    WriteError $_.Exception.Message
}

WriteHeader 'TEST COMPLETED'