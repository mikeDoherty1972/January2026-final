<#
PowerShell helper to create a timestamped GitHub repo and push the contents of the current folder.
Usage:
  - Open PowerShell in the web_site folder and run:
      $env:GITHUB_PAT = 'YOUR_PAT'
      .\deploy_create_github_repo.ps1 -Owner 'your-github-username' -Public

Notes:
- Requires Git installed and available in PATH.
- The script will create a temp folder, copy current files, init a repo and push to GitHub.
- It will enable GitHub Pages for branch 'main' at '/'.
#>
param(
    [Parameter(Mandatory=$true)][string]$Owner,
    [switch]$Public = $true,
    [string]$RepoNamePrefix = 'home_site'
)

if(-not $env:GITHUB_PAT){ Write-Error 'Please set $env:GITHUB_PAT to a GitHub Personal Access Token with public_repo or repo scope before running.'; exit 1 }

$timestamp = Get-Date -Format 'yyyyMMdd_HHmm'
$repoName = "$RepoNamePrefix`_$timestamp"
$src = Get-Location
$dest = Join-Path -Path $env:TEMP -ChildPath $repoName

Write-Host "Preparing repo $repoName at $dest"
Remove-Item -Recurse -Force $dest -ErrorAction SilentlyContinue
Copy-Item -Path $src\* -Destination $dest -Recurse -Force

Set-Location $dest
# initialize git
git init
git checkout -b main
git add .
git commit -m "Initial web site commit from local project on $timestamp"

# create repo via GitHub API
$body = @{ name = $repoName; description = "Static web site exported from local Home SCADA project"; private = -not $Public } | ConvertTo-Json
$headers = @{ Authorization = "token $env:GITHUB_PAT"; "User-Agent" = "powershell" }
$resp = Invoke-RestMethod -Method Post -Uri "https://api.github.com/user/repos" -Headers $headers -Body $body -ContentType "application/json"
Write-Host "Created remote repo: $($resp.full_name)"

# push to remote using non-interactive URL
$remoteUrlWithToken = "https://$($env:GITHUB_PAT)@github.com/$Owner/$repoName.git"
git remote add origin $remoteUrlWithToken
git push -u origin main

# remove token-containing remote and add clean remote
git remote remove origin
git remote add origin "https://github.com/$Owner/$repoName.git"

# enable Pages
$pagesBody = @{ source = @{ branch = 'main'; path = '/' } } | ConvertTo-Json
Invoke-RestMethod -Method PUT -Uri "https://api.github.com/repos/$Owner/$repoName/pages" -Headers $headers -Body $pagesBody -ContentType "application/json"

# Query pages URL
$pages = Invoke-RestMethod -Method Get -Uri "https://api.github.com/repos/$Owner/$repoName/pages" -Headers $headers
Write-Host "Pages status: $($pages.status) - $($pages.html_url)"

Write-Host "Deploy complete. Repo: https://github.com/$Owner/$repoName  Pages: $($pages.html_url)"

# cleanup: unset PAT from environment in this script's session
Remove-Item Env:\GITHUB_PAT -ErrorAction SilentlyContinue

Write-Host 'Done.'

