param(
    [string]$ApiBaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

Write-Host "Resetting RankStream demo data at $ApiBaseUrl ..."
$response = Invoke-RestMethod -Method Post -Uri "$ApiBaseUrl/api/platform/demo/reset"

Write-Host "Demo user: $($response.demoUserId)"
Write-Host "Users created: $($response.usersCreated)"
Write-Host "Content created: $($response.contentCreated)"
Write-Host "Events created: $($response.eventsCreated)"
Write-Host $response.message
