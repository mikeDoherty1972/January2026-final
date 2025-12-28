# Clean up DecemberD workspace: keep Android app + Python bridge; remove website and misc patch/log/temp files
# Run in an elevated PowerShell if any files are locked; adjust as needed.

$root = "C:\website\home_App_DecemberD_2025"

Write-Host "Cleaning: $root" -ForegroundColor Cyan

# 1) Remove website folders entirely
$toRemoveDirs = @(
    "web_site",
    "website_public",
    "obsolete_removed"
)

foreach ($d in $toRemoveDirs) {
    $path = Join-Path $root $d
    if (Test-Path $path) {
        Write-Host "Removing directory: $path" -ForegroundColor Yellow
        Remove-Item -Recurse -Force $path -ErrorAction SilentlyContinue
    }
}

# 2) Remove top-level files not needed by Android app or Python bridge
$toRemoveFiles = @(
    "index_fetch.html",
    "remote_index.html",
    "remote_index_b64.txt",
    "remote_styles.css",
    "scada_fetch.html",
    "init_fetch.js",
    "app.js",
    "light-controller.patch",
    "scada-drive-dual-control.patch",
    "drive-backend-apply.patch",
    "drive-backend-apply2.patch",
    "repo_tree.txt",
    "objects_with_sizes.txt",
    "mike_remote_refs.txt",
    "sheet_dvr.json",
    "sheet_gid0.json",
    "temp_dvr_gid.csv",
    "build_debug_log.txt",
    "build_full_debug.log",
    "build_jt_log.txt",
    "build_kotlin_debug.log",
    "build_kotlin_output.txt",
    "build_log.txt",
    "commit_single_output.txt",
    "commit_status.txt",
    'h --set-upstream mike_remote backup-working-`u25`date`u25`'
)

foreach ($f in $toRemoveFiles) {
    $path = Join-Path $root $f
    if (Test-Path $path) {
        Write-Host "Removing file: $path" -ForegroundColor Yellow
        Remove-Item -Force $path -ErrorAction SilentlyContinue
    }
}

# 3) Keep Android + bridge essentials; optional clean of Gradle caches inside project build
$keepDirs = @(
    "app",
    "gradle",
    "gradle\wrapper",
    "scripts"
)

# 4) Optional: prune app/build intermediates to shrink workspace
$appBuild = Join-Path $root "app\build"
if (Test-Path $appBuild) {
    Write-Host "Pruning app/build intermediates" -ForegroundColor Yellow
    Remove-Item -Recurse -Force $appBuild -ErrorAction SilentlyContinue
}

# 5) Report remaining content (quick tree)
Write-Host "Remaining top-level items:" -ForegroundColor Cyan
Get-ChildItem -LiteralPath $root | Select-Object Name, Mode, Length, LastWriteTime | Format-Table -AutoSize

Write-Host "Cleanup complete." -ForegroundColor Green
