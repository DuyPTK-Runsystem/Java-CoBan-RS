# Script running full system tests with Postman Collection & Newman
param(
    [string]$BaseUrl = "http://localhost:8081",
    [string]$CollectionPath = "document/postman/Java-CoBan.postman_collection.json"
)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " Running Postman Automated Test Suite    " -ForegroundColor Cyan
Write-Host " Target URL: $BaseUrl                    " -ForegroundColor Cyan
Write-Host " Collection: $CollectionPath             " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# Check if newman is available
$newmanCmd = Get-Command newman -ErrorAction SilentlyContinue
if (-not $newmanCmd) {
    Write-Host "Newman is not installed globally. Running via npx newman..." -ForegroundColor Yellow
    npx newman run "$CollectionPath" --env-var "baseUrl=$BaseUrl" --reporters cli
}
else {
    newman run "$CollectionPath" --env-var "baseUrl=$BaseUrl" --reporters cli
}

