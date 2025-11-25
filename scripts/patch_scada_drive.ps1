$path = 'c:\website\home_app_novemberD_2025\app\src\main\java\com\security\app\ScadaActivity.kt'
$txt = Get-Content -Raw $path
$old = 'try { fetchLightsStatusOnce() } catch (_: Exception) {}'
if ($txt.Contains($old)) {
  $new = $old + "`n        // Ensure lights backend selection is applied now that Drive is initialized. If the user`n        // selected 'drive' as the backend, this will start the drive polling job.`n        try { applyLightsBackendSelection() } catch (_: Exception) {}"
  $txt = $txt.Replace($old, $new)
  Set-Content -Path $path -Value $txt -Encoding UTF8
  Write-Output 'patched'
} else {
  Write-Output 'pattern not found'
  exit 1
}

