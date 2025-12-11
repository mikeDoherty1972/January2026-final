# Export web_site to website_public and sanitize likely secrets
param()
$src = "web_site"
$dst = "website_public"
if(-Not (Test-Path $src)){
    Write-Host "Source folder '$src' not found. Aborting."; exit 1
}
if(Test-Path $dst){ Write-Host "Removing existing $dst"; Remove-Item -Recurse -Force $dst }
New-Item -ItemType Directory -Path $dst | Out-Null

# File extensions to treat as binary (skip content scanning)
$binaryExt = @('.png','.jpg','.jpeg','.gif','.ico','.zip','.gz','.tar','.jar','.keystore','.p12','.png','.bin','.exe')

# Patterns to detect sensitive keys (case-insensitive)
$secretFieldNames = @('apikey','api_key','api-key','apiKey','secret','client_secret','password','passwd','token','access_token','refresh_token','stormglass','google-services','firebase','serviceAccount')
# Generic words to flag in content
$flagWords = @('APIKEY','API_KEY','API-KEY','SECRET','CLIENT_SECRET','PASSWORD','TOKEN','ACCESS_TOKEN','REFRESH_TOKEN','STORMGLASS','GOOGLE','FIREBASE','SERVICE_ACCOUNT')

$report = @()

function IsBinary([string]$path){
    $ext = [IO.Path]::GetExtension($path)
    return $binaryExt -contains $ext.ToLower()
}

Get-ChildItem -Path $src -Recurse -File | ForEach-Object {
    $rel = $_.FullName.Substring((Get-Location).Path.Length + 1)
    $rel = $rel.Substring($src.Length+1)
    $targetPath = Join-Path $dst $rel
    $targetDir = Split-Path $targetPath -Parent
    if(-not (Test-Path $targetDir)){ New-Item -ItemType Directory -Path $targetDir -Force | Out-Null }

    if(IsBinary($_.FullName)){
        # copy binary as-is
        Copy-Item -Path $_.FullName -Destination $targetPath -Force
    } else {
        # read text and scan for secret field names
        $text = Get-Content -Raw -Encoding UTF8 $_.FullName
        $found = @()
        foreach($field in $secretFieldNames){
            # look for JSON-style "field": "value"
            $regexJson = [regex]::new('"' + [regex]::Escape($field) + '"\s*:\s*"([^"]{4,})"', 'IgnoreCase')
            foreach($m in $regexJson.Matches($text)){
                $found += @{ type='json_field'; field=$field; value=$m.Groups[1].Value }
                # mask value
                $text = $text -replace [regex]::Escape($m.Value), '"' + $field + '": "REDACTED"'
            }
            # look for key=VALUE or field=VALUE or field: VALUE (simple forms)
            $regexKvp = [regex]::new('(?i)\b' + [regex]::Escape($field) + '\b\s*[=:]\s*(["\']?)([^"\'\s,;]{4,})\1')
            foreach($m in $regexKvp.Matches($text)){
                $found += @{ type='kvp'; field=$field; value=$m.Groups[2].Value }
                # replace the value with REDACTED (preserve quoting)
                $quote = $m.Groups[1].Value
                if($quote -eq ''){ $quote = '"' }
                $orig = $m.Value
                $replacement = $orig -replace [regex]::Escape($m.Groups[2].Value), 'REDACTED'
                $text = $text -replace [regex]::Escape($m.Value), [regex]::Escape($replacement)
            }
        }
        # Generic flag: look for long-looking keys (UUIDs/hex) near words
        $longKeyRegex = [regex]::new('([A-Za-z0-9_-]{20,})')
        foreach($m in $longKeyRegex.Matches($text)){
            $snippet = $text.Substring([math]::Max(0,$m.Index-40), [math]::Min(80, $text.Length - $m.Index + 40))
            foreach($w in $flagWords){ if($snippet.IndexOf($w, [System.StringComparison]::InvariantCultureIgnoreCase) -ge 0){ $found += @{ type='longkey'; field=$w; value=$m.Value } ; break } }
        }

        if($found.Count -gt 0){
            $report += @{ file = $rel; findings = $found }
            # write sanitized file
            Set-Content -Path $targetPath -Value $text -Encoding UTF8
        } else {
            # copy original text file
            Copy-Item -Path $_.FullName -Destination $targetPath -Force
        }
    }
}

# Save a report
$reportPath = Join-Path $dst "export_report.txt"
$sb = New-Object System.Text.StringBuilder
$sb.AppendLine("Export report for website_public generated at $(Get-Date)") | Out-Null
if($report.Count -eq 0){ $sb.AppendLine("No potential secrets or masked values detected.") | Out-Null } else {
    foreach($r in $report){
        $sb.AppendLine("File: $($r.file)") | Out-Null
        foreach($f in $r.findings){
            $sb.AppendLine("  - type=$($f.type) field=$($f.field) value_preview=$($f.value.Substring(0,[math]::Min(40,$f.value.Length)))") | Out-Null
        }
    }
}
Set-Content -Path $reportPath -Value $sb.ToString() -Encoding UTF8
Write-Host "Export complete. Public copy at '$dst'. Report: $reportPath"
if($report.Count -gt 0){ Write-Host "WARNING: potential secrets were found and replaced with REDACTED in the public copy. Check $reportPath and verify further manual sanitization as needed." }
else { Write-Host "No obvious secrets found. Still review $dst before publishing." }

