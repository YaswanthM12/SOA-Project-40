$ports = @(8080, 8081, 8082, 8083, 8084, 8761)
foreach ($port in $ports) {
    $connections = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($connections) {
        $pids = $connections | Select-Object -ExpandProperty OwningProcess -Unique
        foreach ($procId in $pids) {
            Write-Host "Stopping process $procId on port $port..." -ForegroundColor Yellow
            Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
        }
    }
}
Write-Host "All LogiFleet service processes stopped." -ForegroundColor Green
