$res1 = Invoke-RestMethod -Uri "http://localhost:8082/api/safety/routes/resolve-location?lat=28.6692&lng=77.4538" -Method Get
Write-Output "Ghaziabad resolve test:"
Write-Output ($res1 | ConvertTo-Json -Depth 3)

$res2 = Invoke-RestMethod -Uri "http://localhost:8082/api/safety/routes/resolve-location?lat=0.0&lng=0.0" -Method Get
Write-Output "Honest fallback test (0,0):"
Write-Output ($res2 | ConvertTo-Json -Depth 3)

$res3 = Invoke-RestMethod -Uri "http://localhost:8082/api/safety/routes/destinations?city=Ghaziabad" -Method Get
Write-Output "Ghaziabad suggestions count: $($res3.Count)"
if ($res3.Count -gt 0) {
    Write-Output "First item: $($res3[0].name) [$($res3[0].category)]"
}
