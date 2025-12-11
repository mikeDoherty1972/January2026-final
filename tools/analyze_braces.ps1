$file = 'app\src\main\java\com\security\app\ScadaActivity.kt'
$lines = Get-Content $file
$nest = 0
$firstZero = $null
for($i=0; $i -lt $lines.Count; $i++){
    $line = $lines[$i]
    $opens = ($line.ToCharArray() | Where-Object { $_ -eq '{' }).Count
    $closes = ($line.ToCharArray() | Where-Object { $_ -eq '}' }).Count
    $prev = $nest
    $nest += $opens - $closes
    if($prev -gt 0 -and $nest -eq 0 -and $firstZero -eq $null){ $firstZero = $i+1 }
}
Write-Host "total_lines=$($lines.Count) final_nesting=$nest firstZero=$firstZero"
if($firstZero){ $start = [Math]::Max(1, $firstZero-10); $end = [Math]::Min($lines.Count, $firstZero+10); for($j=$start; $j -le $end; $j++){ Write-Host "{0}: {1}" -f $j, $lines[$j-1] } } else { Write-Host 'No premature zero found' }

