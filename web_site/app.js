// Client-side prototype wiring for the web UI
// Adds ability to load an app-export JSON (file or paste) and populate DVR/IPERL values

// Default simulated DVR data (used as fallback)
let dvrData = [25.6, 25.8, 26.0, 25.9, 25.7, 25.6];
let dvrTimestamps = dvrData.map((_,i)=>Date.now()-((dvrData.length-i)*60000));

function initDvrChart(){
  const ctx = document.getElementById('dvr-chart').getContext('2d');
  window.dvrChart = new Chart(ctx,{type:'line',data:{labels:dvrTimestamps.map(t=>new Date(t).toLocaleTimeString()),datasets:[{label:'DVR Temp (°C)',data:dvrData,borderColor:'#ff6384',fill:false}]},options:{scales:{y:{beginAtZero:false}}}});
  updateDvrUI();
}

function updateDvrUI(){
  const latest = dvrData.length ? dvrData[dvrData.length-1] : null;
  document.getElementById('dvr-temp').textContent = latest!==null ? latest.toFixed(1) + ' °C' : '— °C';
  // Heartbeat: if last sample within 2 mins assume alive
  const lastTs = dvrTimestamps.length ? dvrTimestamps[dvrTimestamps.length-1] : 0;
  const alive = Date.now() - lastTs < (2*60*1000);
  document.getElementById('dvr-heartbeat').textContent = alive ? 'alive' : 'offline';
  if(window.dvrChart){
    window.dvrChart.data.labels = dvrTimestamps.map(t=>new Date(t).toLocaleTimeString());
    window.dvrChart.data.datasets[0].data = dvrData;
    window.dvrChart.update();
  }
}

function clearData(){
  dvrData = [];
  dvrTimestamps = [];
  updateDvrUI();
  document.getElementById('mikeWaterReading').textContent = '—';
  document.getElementById('mikeUsage').textContent = '— / — / —';
}

function loadAppExportJson(obj){
  // Attempt to find DVR readings and IPERL/water usage in the app-export JSON structure
  // We'll look for common keys. This is heuristic: adjust if your app-export shape differs.

  // DVR: look for a path like data.dvr.readings or dvrTemperatures
  let dvrTemps = null;
  try{
    if(obj.dvr && Array.isArray(obj.dvr.readings)){
      dvrTemps = obj.dvr.readings.map(r=>Number(r.temp ?? r.temperature ?? r));
      // timestamps if present
      dvrTimestamps = obj.dvr.readings.map(r=> r.ts ? new Date(r.ts).getTime() : Date.now());
    } else if(Array.isArray(obj.dvrTemperatures)){
      dvrTemps = obj.dvrTemperatures.map(Number);
      dvrTimestamps = dvrTemps.map((_,i)=>Date.now()-((dvrTemps.length-i)*60000));
    }
  }catch(e){ console.warn('dvr parse error', e); }

  if(dvrTemps && dvrTemps.length){
    dvrData = dvrTemps.filter(n=>!Number.isNaN(n));
    if(!dvrTimestamps || dvrTimestamps.length !== dvrData.length) dvrTimestamps = dvrData.map((_,i)=>Date.now()-((dvrData.length-i)*60000));
    updateDvrUI();
  }

  // IPERL/water usage: look for fields totalUsage/dayUsage/monthUsage/latestReading
  try{
    let latestReading = null;
    if(obj.iperl && obj.iperl.latestReading) latestReading = obj.iperl.latestReading;
    if(obj.waterMeter && obj.waterMeter.latest) latestReading = obj.waterMeter.latest;
    if(latestReading == null && obj.latestMeterReading) latestReading = obj.latestMeterReading;

    let total=null, day=null, month=null;
    if(obj.iperl && obj.iperl.stats){
      total = obj.iperl.stats.totalUsage;
      day = obj.iperl.stats.dayUsage;
      month = obj.iperl.stats.monthUsage;
    }
    if(!total && obj.totalUsage) total = obj.totalUsage;

    if(latestReading != null){
      document.getElementById('mikeWaterReading').textContent = latestReading + ' L';
    }
    if(total != null || day != null || month != null){
      document.getElementById('mikeUsage').textContent = (day!=null?day+' L':'—') + ' / ' + (month!=null?month+' L':'—') + ' / ' + (total!=null?total+' L':'—');
    }
  }catch(e){ console.warn('iperl parse', e); }
}

// File input handling
document.addEventListener('DOMContentLoaded',()=>{
  initDvrChart();

  const fileInput = document.getElementById('data-file');
  fileInput.addEventListener('change', (ev)=>{
    const f = ev.target.files && ev.target.files[0];
    if(!f) return;
    const reader = new FileReader();
    reader.onload = (e)=>{
      try{
        const obj = JSON.parse(e.target.result);
        loadAppExportJson(obj);
      }catch(err){ alert('Failed to parse JSON: '+err); }
    };
    reader.readAsText(f);
  });

  document.getElementById('btn-load-paste').addEventListener('click', ()=>{
    const txt = document.getElementById('paste-json').value.trim();
    if(!txt) return alert('Paste JSON above');
    try{
      const obj = JSON.parse(txt);
      loadAppExportJson(obj);
    }catch(e){ alert('Invalid JSON: '+e); }
  });

  document.getElementById('btn-clear-data').addEventListener('click', ()=>{ clearData(); });

  // Stormglass fetch: naive direct attempt (may fail due to CORS)
  document.getElementById('btn-sg-fetch').addEventListener('click', async ()=>{
    const key = document.getElementById('sg-key').value.trim();
    if(!key) return alert('Provide Stormglass API key');
    try{
      const lat=53.0, lon=-6.0; // example - you can change in code
      const params = 'lat='+lat+'&lng='+lon+'&params=windSpeed,tideHeight&source=sg';
      const resp = await fetch('https://api.stormglass.io/v2/weather/point?'+params, { headers: { 'Authorization': key }});
      if(!resp.ok) throw new Error(resp.status+' '+resp.statusText);
      const data = await resp.json();
      // crude extraction
      const wind = data.hours && data.hours[0] && data.hours[0].windSpeed && data.hours[0].windSpeed.sg && data.hours[0].windSpeed.sg[0] ? data.hours[0].windSpeed.sg[0] : null;
      const tide = data.hours && data.hours[0] && data.hours[0].tideHeight && data.hours[0].tideHeight.sg && data.hours[0].tideHeight.sg[0] ? data.hours[0].tideHeight.sg[0] : null;
      if(wind!=null) document.getElementById('wind-speed').textContent = wind + ' m/s';
      if(tide!=null) document.getElementById('tide').textContent = tide + ' m';
    }catch(e){ alert('Stormglass fetch failed: '+e); }
  });

});

