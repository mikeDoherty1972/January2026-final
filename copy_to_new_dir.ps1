# PowerShell script to copy app to new directory
$sourceDir = "C:\website\home_app_novemberD_2025"
$destDir = "C:\website\home_app_DecemberA_2025"

Write-Host "Copying app from $sourceDir to $destDir..."

# Create destination directory if it doesn't exist
if (!(Test-Path $destDir)) {
    New-Item -ItemType Directory -Path $destDir -Force
}

# Copy all files and directories recursively
Copy-Item -Path "$sourceDir\*" -Destination $destDir -Recurse -Force

Write-Host "Copy completed successfully!"
Write-Host "New app location: $destDir"
