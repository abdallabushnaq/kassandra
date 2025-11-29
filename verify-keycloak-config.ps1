# Script to verify Keycloak token configuration
# Run this after starting your test to check if the token lifespan is correctly configured

Write-Host "=== Keycloak Token Configuration Verification ===" -ForegroundColor Cyan
Write-Host ""

# Check if realm configuration file exists and has the token settings
$realmFile = "E:\github\kassandra\src\test\resources\keycloak\project-hub-realm.json"

if (Test-Path $realmFile) {
    Write-Host "OK: Realm configuration file found" -ForegroundColor Green

    $content = Get-Content $realmFile -Raw | ConvertFrom-Json

    if ($content.accessTokenLifespan) {
        $hours = $content.accessTokenLifespan / 3600
        Write-Host "OK: Access Token Lifespan: $($content.accessTokenLifespan)s ($hours hour)" -ForegroundColor Green
    } else {
        Write-Host "ERROR: Access Token Lifespan NOT SET (will use Keycloak default of 5 minutes)" -ForegroundColor Red
    }

    if ($content.ssoSessionMaxLifespan) {
        $hours = $content.ssoSessionMaxLifespan / 3600
        Write-Host "OK: SSO Session Max Lifespan: $($content.ssoSessionMaxLifespan)s ($hours hours)" -ForegroundColor Green
    } else {
        Write-Host "WARN: SSO Session Max Lifespan NOT SET" -ForegroundColor Yellow
    }

    if ($content.ssoSessionIdleTimeout) {
        $hours = $content.ssoSessionIdleTimeout / 3600
        Write-Host "OK: SSO Session Idle Timeout: $($content.ssoSessionIdleTimeout)s ($hours hour)" -ForegroundColor Green
    } else {
        Write-Host "WARN: SSO Session Idle Timeout NOT SET" -ForegroundColor Yellow
    }
} else {
    Write-Host "ERROR: Realm configuration file not found!" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== Next Steps ===" -ForegroundColor Cyan
Write-Host "1. If you have a REUSED Keycloak container, you need to stop it:"
Write-Host "   docker ps | Select-String keycloak"
Write-Host "   docker stop <container-id>"
Write-Host ""
Write-Host "2. Run your test:"
Write-Host "   mvn test -Dtest=UserProfileIntroductionVideo"
Write-Host ""
Write-Host "3. Watch for token expiration logs in the output:"
Write-Host "   - Look for: 'Access token for user X is valid for Y more seconds'"
Write-Host "   - Look for: 'Access token has EXPIRED' (should not appear)"
Write-Host ""
Write-Host "4. If still getting 401 errors, check the logs for:"
Write-Host "   - 'REST API call failed with status: 401 UNAUTHORIZED'"
Write-Host "   - Token expiration messages"
Write-Host ""

