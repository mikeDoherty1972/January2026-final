# Copy web_site -> website_public, then sanitize copied files in-place and produce a report
$src = 'web_site'
$dst = 'website_public'
if(-not (Test-Path $src)) { Write-Host "Source '$src' not found. Aborting."; exit 1 }
if(Test-Path $dst) { Write-Host "Removing existing $dst"; Remove-Item -Recurse -Force $dst }
Write-Host "Copying $src -> $dst (this may take a moment)"
Copy-Item -Path $src -Destination $dst -Recurse -Force

# Define binary extensions to skip
$binaryExt = @('.png','.jpg','.jpeg','.gif','.ico','.zip','.gz','.tar','.jar','.keystore','.p12','.bin','.exe','.woff','.woff2','.ttf')
$secretFieldNames = @('apikey','api_key','api-key','apiKey','secret','client_secret','password','passwd','token','access_token','refresh_token','stormglass','google-services','firebase','serviceAccount')
$flagWords = @('APIKEY','API_KEY','API-KEY','SECRET','CLIENT_SECRET','PASSWORD','TOKEN','ACCESS_TOKEN','REFRESH_TOKEN','STORMGLASS','GOOGLE','FIREBASE','SERVICE_ACCOUNT')

$report = @()

function IsBinary($path) {
    $ext = [IO.Path]::GetExtension($path)
    return $binaryExt -contains $ext.ToLower()
}

Write-Host "Sanitizing files under $dst"
$files = Get-ChildItem -Path $dst -Recurse -File
foreach($file in $files){
    try{
        $rel = $file.FullName.Substring((Get-Location).Path.Length + 1)
        $rel = $rel.Substring($dst.Length+1)
    } catch { $rel = $file.Name }
    if(IsBinary($file.FullName)) { continue }
    try{
        $text = Get-Content -Raw -Encoding UTF8 $file.FullName
    } catch { continue }
    $found = @()
    foreach($field in $secretFieldNames){
        # JSON style: "field": "value"
        $jsonPattern = '"' + [regex]::Escape($field) + '"\s*:\s*"([^"\\]{4,})"'
        $regexJson = [regex]::new($jsonPattern, 'IgnoreCase')
        $matches = $regexJson.Matches($text)
        foreach($m in $matches){
            $found += @{ type='json_field'; field=$field; value=$m.Groups[1].Value }
        }
        if($matches.Count -gt 0){
            # mask all JSON occurrences for this field
            $text = $regexJson.Replace($text, '"' + $field + '": "REDACTED"')
        }

        # KVP style: field = value  or field: value  (simple token after separator)
        $kvpPattern = '(?i)\b' + [regex]::Escape($field) + '\b\s*[=:]\s*([^\s,;]{4,})'
        $regexKvp = [regex]::new($kvpPattern)
        $matches2 = $regexKvp.Matches($text)
        foreach($m in $matches2){
            $found += @{ type='kvp'; field=$field; value=$m.Groups[1].Value }
        }
        if($matches2.Count -gt 0){
            # Replace using MatchEvaluator to preserve surrounding syntax
            $text = $regexKvp.Replace($text, [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $m.Value -replace [regex]::Escape($m.Groups[1].Value), 'REDACTED' })
        }
    }

    # Generic long key near flag words
    $longKeyRegex = [regex]::new('([A-Za-z0-9_-]{20,})')
    $longMatches = $longKeyRegex.Matches($text)
    foreach($m in $longMatches){
        $idx = $m.Index
        $start = [math]::Max(0, $idx-40)
        $len = [math]::Min(80, $text.Length - $start)
        $snippet = $text.Substring($start, $len)
        foreach($w in $flagWords){ if($snippet.IndexOf($w, [System.StringComparison]::InvariantCultureIgnoreCase) -ge 0){ $found += @{ type='longkey'; field=$w; value=$m.Value }; break } }
    }

    if($found.Count -gt 0){
        $report += @{ file = $rel; findings = $found }
        try{ Set-Content -Path $file.FullName -Value $text -Encoding UTF8 } catch { Write-Host "Failed writing sanitized file $rel" }
    }
}

# Write report
$reportPath = Join-Path $dst 'export_report.txt'
$sb = New-Object System.Text.StringBuilder
$sb.AppendLine("Export report for website_public generated at $(Get-Date)") | Out-Null
if($report.Count -eq 0) { $sb.AppendLine('No potential secrets were detected.') | Out-Null } else {
    foreach($r in $report){
        $sb.AppendLine("File: $($r.file)") | Out-Null
        foreach($f in $r.findings){
            $valPreview = $f.value
            if($valPreview.Length -gt 40){ $valPreview = $valPreview.Substring(0,40) + '...' }
            $sb.AppendLine("  - type=$($f.type) field=$($f.field) value_preview=$valPreview") | Out-Null
        }
    }
}
Set-Content -Path $reportPath -Value $sb.ToString() -Encoding UTF8
Write-Host "Export complete. Public copy at '$dst'. Report: $reportPath"
if($report.Count -gt 0){ Write-Host 'WARNING: potential secrets were found and replaced with REDACTED. Inspect export_report.txt and sanitized files.' }
else { Write-Host 'No obvious secrets found. Still review before publishing.' }
