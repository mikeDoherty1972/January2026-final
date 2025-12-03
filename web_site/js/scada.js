// scada.js - populate SCADA dashboard from sample data and optional proxy fetches
async function loadSample(){
  try{
    const resp = await fetch('../data/sample_scada.json');
    const data = await resp.json();
    populateScada(data);
  }catch(e){ console.warn('sample load failed', e); }
}

function populateScada(d){
  // Geyser
  document.getElementById('waterTempValue').textContent = (d.geyser.temp || '--') + ' °C';
  document.getElementById('waterTempStatus').textContent = d.geyser.status || '';
  document.getElementById('waterPressureValue').textContent = (d.geyser.pressure || '--') + ' Bar';
  document.getElementById('waterPressureStatus').textContent = d.geyser.pressureStatus || '';

  // DVR
  document.getElementById('dvrTemp').textContent = (d.dvr.temp || '--') + ' °C';
  document.getElementById('dvrHeartbeatStatus').textContent = d.dvr.heartbeat || '—';

  // Weather
  document.getElementById('indoorTemp').textContent = (d.weather.indoor || '--') + ' °C';
  document.getElementById('outdoorTemp').textContent = (d.weather.outdoor || '--') + ' °C';
  document.getElementById('windSpeed').textContent = (d.weather.windSpeed || '--') + ' m/s';
  document.getElementById('windDirText').textContent = (d.weather.windDir || '--') + '°';
  document.getElementById('sunInfo').textContent = d.weather.sunrise + ' / ' + d.weather.sunset;
  document.getElementById('tideInfo').textContent = d.weather.tide || 'Tide: N/A';

  // Power
  document.getElementById('currentAmps').textContent = (d.power.amps || '--') + ' A';
  document.getElementById('currentPower').textContent = (d.power.kW || '--') + ' kW';
  document.getElementById('dailyPower').textContent = (d.power.daily || '--') + ' kWh';

  // Charts: DVR sample
  const ctx = document.getElementById('dvrChart').getContext('2d');
  const labels = d.dvr.history.map(p=>new Date(p.ts).toLocaleTimeString());
  const vals = d.dvr.history.map(p=>p.temp);
  if(window._dvrChart) window._dvrChart.destroy();
  window._dvrChart = new Chart(ctx, { type: 'line', data:{labels, datasets:[{label:'DVR °C', data: vals, borderColor:'#ff6384', fill:false}]}, options:{scales:{y:{beginAtZero:false}}}});
}

async function fetchDvrCsvViaProxy(url){
  const status = document.getElementById('dvr-sheet-status');
  status.textContent = 'Fetching...';
  try{
    const rows = await fetchCsvRows(url);
    if(!rows || !rows.length){ status.textContent = 'No rows returned'; return; }
    const history = rows.map(r=>{
      const keys = Object.keys(r);
      const tsKey = keys.find(k=>/date|time|ts|timestamp/i.test(k)) || keys[0];
      const valKey = keys.find(k=>/temp|temperature|dvr/i.test(k)) || keys[1] || keys[0];
      let ts = Date.parse(r[tsKey]); if(Number.isNaN(ts)) ts = Date.now();
      const temp = Number(r[valKey]) || 0;
      return { ts, temp };
    });
    const latestTemp = history.length ? history[history.length-1].temp : null;
    // Save DVR sample so DVR page can load it
    try{ localStorage.setItem('dvr_sample', JSON.stringify({ temp: latestTemp, history })); }catch(e){ console.warn('dvr sample save failed', e); }
    populateScada({ geyser:{}, dvr:{ temp: latestTemp, heartbeat: 'alive', history }, weather:{}, power:{} });
    status.textContent = 'Loaded ' + rows.length + ' rows';
  }catch(e){ status.textContent = 'Error: ' + e.message; }
}

async function fetchStormglass(lat, lng){
  const status = document.getElementById('sg-status');
  status.textContent = 'Fetching...';
  try{
    const resp = await fetch(PROXY_BASE + '/api/stormglass?lat='+encodeURIComponent(lat)+'&lng='+encodeURIComponent(lng));
    if(!resp.ok) throw new Error('stormglass fetch failed: ' + resp.status);
    const json = await resp.json();
    // pick first hour's values if present
    const h = json.data && json.data.hours && json.data.hours[0];
    if(h){
      const wind = h.windSpeed && h.windSpeed.sg ? h.windSpeed.sg[0] : null;
      const tide = h.tideHeight && h.tideHeight.sg ? h.tideHeight.sg[0] : null;
      document.getElementById('windSpeed').textContent = wind != null ? wind + ' m/s' : document.getElementById('windSpeed').textContent;
      document.getElementById('tideInfo').textContent = tide != null ? (tide + ' m') : document.getElementById('tideInfo').textContent;
      status.textContent = 'Stormglass data loaded';
    }else{
      status.textContent = 'No data returned';
    }
  }catch(e){ status.textContent = 'Error: ' + e.message; }
}

// Proxy base (change if deployed elsewhere)
const PROXY_BASE = localStorage.getItem('proxy_base') || 'http://localhost:4000';
let _autoRefreshId = null;

function saveConfig(){
  const cfg = {
    dvr: document.getElementById('cfg-dvr').value.trim(),
    iperl: document.getElementById('cfg-iperl').value.trim(),
    geyser: document.getElementById('cfg-geyser').value.trim(),
    power: document.getElementById('cfg-power').value.trim(),
    autorefresh: document.getElementById('cfg-autorefresh').checked,
    usePublic: !!document.getElementById('cfg-use-public') && document.getElementById('cfg-use-public').checked,
    sheetId: (document.getElementById('cfg-sheet-id') && document.getElementById('cfg-sheet-id').value.trim()) || '',
    dvrTab: (document.getElementById('cfg-dvr-tab') && document.getElementById('cfg-dvr-tab').value.trim()) || '',
    iperlTab: (document.getElementById('cfg-iperl-tab') && document.getElementById('cfg-iperl-tab').value.trim()) || '',
    geyserTab: (document.getElementById('cfg-geyser-tab') && document.getElementById('cfg-geyser-tab').value.trim()) || '',
    powerTab: (document.getElementById('cfg-power-tab') && document.getElementById('cfg-power-tab').value.trim()) || ''
  };
  localStorage.setItem('scada_cfg', JSON.stringify(cfg));
  document.getElementById('cfg-status').textContent = 'Config saved';
  applyAutoRefresh(cfg.autorefresh);
}

function loadConfigToUI(){
  const raw = localStorage.getItem('scada_cfg');
  if(!raw) return;
  try{
    const cfg = JSON.parse(raw);
    document.getElementById('cfg-dvr').value = cfg.dvr || '';
    document.getElementById('cfg-iperl').value = cfg.iperl || '';
    document.getElementById('cfg-geyser').value = cfg.geyser || '';
    document.getElementById('cfg-power').value = cfg.power || '';
    // new fields
    if(document.getElementById('cfg-use-public')) document.getElementById('cfg-use-public').checked = !!cfg.usePublic;
    if(document.getElementById('cfg-sheet-id')) document.getElementById('cfg-sheet-id').value = cfg.sheetId || '';
    if(document.getElementById('cfg-dvr-tab')) document.getElementById('cfg-dvr-tab').value = cfg.dvrTab || '';
    if(document.getElementById('cfg-iperl-tab')) document.getElementById('cfg-iperl-tab').value = cfg.iperlTab || '';
    if(document.getElementById('cfg-geyser-tab')) document.getElementById('cfg-geyser-tab').value = cfg.geyserTab || '';
    if(document.getElementById('cfg-power-tab')) document.getElementById('cfg-power-tab').value = cfg.powerTab || '';
    document.getElementById('cfg-autorefresh').checked = !!cfg.autorefresh;
    if(cfg.autorefresh) applyAutoRefresh(true);
  }catch(e){ console.warn('cfg load failed', e); }
}

// Fetch public Google Sheet using gviz/tq JSON (works for publicly published sheets)
async function fetchPublicSheet(spreadsheetId, sheetName){
  if(!spreadsheetId) throw new Error('missing spreadsheetId');
  // build gviz URL
  let url = `https://docs.google.com/spreadsheets/d/${encodeURIComponent(spreadsheetId)}/gviz/tq?tqx=out:json`;
  if(sheetName){
    // if sheetName is numeric (gid), use &gid=... otherwise use &sheet=...
    if(/^\d+$/.test(String(sheetName).trim())){
      url += '&gid=' + encodeURIComponent(String(sheetName).trim());
    }else{
      url += '&sheet=' + encodeURIComponent(sheetName);
    }
  }

  // helper to parse CSV into rows of objects
  function parseCsvToRows(csvText){
    // simple CSV parser that handles quoted fields
    const lines = [];
    let cur = '';
    let inQuotes = false;
    for(let i=0;i<csvText.length;i++){
      const ch = csvText[i];
      const nxt = csvText[i+1];
      if(ch === '"'){
        if(inQuotes && nxt === '"'){
          cur += '"'; // escaped quote
          i++; continue;
        }
        inQuotes = !inQuotes;
        continue;
      }
      if(ch === '\n' && !inQuotes){
        lines.push(cur);
        cur = '';
        continue;
      }
      cur += ch;
    }
    if(cur.length) lines.push(cur);
    const rows = lines.map(l=>{
      const cols = [];
      let val = '';
      let q = false;
      for(let i=0;i<l.length;i++){
        const ch = l[i];
        const nxt = l[i+1];
        if(ch === '"'){
          if(q && nxt === '"'){ val += '"'; i++; continue; }
          q = !q; continue;
        }
        if(ch === ',' && !q){ cols.push(val); val=''; continue; }
        val += ch;
      }
      cols.push(val);
      return cols;
    }).filter(r=> r.length>0);
    if(rows.length === 0) return [];
    const headers = rows[0].map(h=> (h||'').trim());
    const out = rows.slice(1).map(r=>{ const obj = {}; headers.forEach((h,i)=> obj[h || ('col'+i)] = (r[i] !== undefined ? r[i] : '')); return obj; });
    return out;
  }

  try{
    const resp = await fetch(url);
    if(!resp.ok) throw new Error('public sheet fetch failed: ' + resp.status);
    const text = await resp.text();
    // attempt to parse gviz wrapper
    const m = text.match(/google\.visualization\.Query\.setResponse\(([\s\S]+)\);?/);
    if(m){
      const json = JSON.parse(m[1]);
      const table = json.table;
      const cols = (table.cols || []).map(c=> c.label || c.id || '');
      const rows = (table.rows || []).map(r=>{
        const obj = {};
        cols.forEach((h,i)=>{ const cell = r.c[i]; obj[h] = (cell && cell.v !== undefined) ? cell.v : ''; });
        return obj;
      });
      return rows; // array of objects with header keys
    }
    // If gviz wrapper wasn't present, fall back to CSV export URL
    // Determine gid if sheetName looks numeric
    let gidParam = '';
    if(sheetName && /^\d+$/.test(String(sheetName).trim())) gidParam = String(sheetName).trim();
    // Try CSV export URL using gid if available else without gid
    const csvUrl = `https://docs.google.com/spreadsheets/d/${encodeURIComponent(spreadsheetId)}/export?format=csv${gidParam?('&gid='+encodeURIComponent(gidParam)) : ''}`;
    const resp2 = await fetch(csvUrl);
    if(!resp2.ok) throw new Error('public sheet CSV fetch failed: ' + resp2.status);
    const csvText = await resp2.text();
    const parsed = parseCsvToRows(csvText);
    return parsed;
  }catch(e){
    // rethrow with useful message
    throw new Error('fetchPublicSheet error: ' + e.message);
  }
}

// Helper: when rows are objects keyed by headers, get value by numeric column index
function getRowValueByIndex(rowObj, index){
  if(!rowObj) return undefined;
  const keys = Object.keys(rowObj);
  if(index < 0 || index >= keys.length) return undefined;
  return rowObj[keys[index]];
}

// Known sheet id from Android app
const ANDROID_SHEET_ID = '1_GuECfJE0nFSGMS6-prrWAfLCR4gBPa3Qf_zzaKzAAA';

// Wrappers to fetch and populate from a public sheet
async function fetchDvrFromPublic(spreadsheetId, tabName){
  const status = document.getElementById('dvr-sheet-status');
  status.textContent = 'Fetching public sheet...';
  try{
    const rows = await fetchPublicSheet(spreadsheetId, tabName || undefined);
    if(!rows || !rows.length){ status.textContent = 'No rows returned'; return; }

    // Special-case Android DVR sheet: gid=2109322930 -> columns: [0]=dvrTemp, [1]=timestamp
    let history = [];
    if(spreadsheetId === ANDROID_SHEET_ID && String(tabName).trim() === '2109322930'){
      for(const r of rows){
        const temp = Number(getRowValueByIndex(r,0)) || Number(r[Object.keys(r)[0]]) || 0;
        const tsRaw = (getRowValueByIndex(r,1) || '').toString();
        let ts = Date.parse(tsRaw);
        if(Number.isNaN(ts)) ts = Date.now();
        history.push({ ts, temp });
      }
    } else {
      // fallback generic parsing
      history = rows.map(r=>{
        const keys = Object.keys(r);
        const tsKey = keys.find(k=>/date|time|ts|timestamp/i.test(k)) || keys[0];
        const valKey = keys.find(k=>/temp|temperature|dvr/i.test(k)) || keys[1] || keys[0];
        let ts = Date.parse(r[tsKey]); if(Number.isNaN(ts)) ts = Date.now();
        const temp = Number(r[valKey]) || 0;
        return { ts, temp };
      });
    }

    const latestTemp = history.length ? history[history.length-1].temp : null;
    try{ localStorage.setItem('dvr_sample', JSON.stringify({ temp: latestTemp, history })); }catch(e){ console.warn('dvr sample save failed', e); }
    populateScada({ geyser:{}, dvr:{ temp: latestTemp, heartbeat: 'alive', history }, weather:{}, power:{} });
    status.textContent = 'Loaded ' + rows.length + ' rows (public)';
  }catch(e){ status.textContent = 'Error: ' + e.message; }
}

async function fetchIperlFromPublic(spreadsheetId, tabName){
  const status = document.getElementById('cfg-status');
  try{
    const rows = await fetchPublicSheet(spreadsheetId, tabName || undefined);
    if(!rows || !rows.length){ status.textContent = 'IPERL: no rows'; return; }

    // Special-case Android main sheet (gid=0): timestamp at column A (index 0), water value at column K (index 10)
    if(spreadsheetId === ANDROID_SHEET_ID && String(tabName).trim() === '0'){
      const history = [];
      for(const r of rows){
        const tsRaw = (getRowValueByIndex(r,0) || '').toString();
        const valRaw = (getRowValueByIndex(r,10) || '').toString();
        const ts = Date.parse(tsRaw) || Date.now();
        const value = Number(valRaw) || 0;
        history.push({ ts, value });
      }
      const latest = history.length ? history[history.length-1].value : null;
      localStorage.setItem('iperl_sample', JSON.stringify({ latest, history }));
      status.textContent = 'IPERL: loaded ' + rows.length + ' rows (public, mapped columns A & K)';
      return;
    }

    // fallback generic parsing
    const sample = rows[0];
    const keys = Object.keys(sample);
    const valKey = keys.find(k=>/value|reading|meter|usage|total|litres|l/i.test(k)) || keys.find(k=>/\d/.test(sample[k]) || !isNaN(Number(sample[k]))) || keys[0];
    const tsKey = keys.find(k=>/date|time|ts|timestamp/i.test(k)) || keys[0];
    const history = rows.map(r=>({ ts: Date.parse(r[tsKey]) || Date.now(), value: Number(r[valKey]) || 0 }));
    const latest = history.length ? history[history.length-1].value : null;
    localStorage.setItem('iperl_sample', JSON.stringify({ latest, history }));
    status.textContent = 'IPERL: loaded ' + rows.length + ' rows (public)';
  }catch(e){ status.textContent = 'IPERL error: ' + e.message; }
}

async function fetchGeyserFromPublic(spreadsheetId, tabName){
  const status = document.getElementById('cfg-status');
  try{
    const rows = await fetchPublicSheet(spreadsheetId, tabName || undefined);
    if(!rows.length){ status.textContent = 'Geyser: no rows'; return; }
    const sample = rows[0];
    const keys = Object.keys(sample);
    const tempKey = keys.find(k=>/temp|temperature/i.test(k)) || keys[0];
    const pressureKey = keys.find(k=>/press|bar|psi/i.test(k)) || null;
    const tsKey = keys.find(k=>/date|time|ts|timestamp/i.test(k)) || keys[0];
    const history = rows.map(r=>({ ts: Date.parse(r[tsKey]) || Date.now(), temp: Number(r[tempKey]) || 0 }));
    const latestTemp = history.length ? history[history.length-1].temp : null;
    const latestPressure = pressureKey ? Number(rows[rows.length-1][pressureKey]) : null;
    try{ localStorage.setItem('geyser_sample', JSON.stringify({ latestTemp, latestPressure, history })); }catch(e){ console.warn('geyser sample save failed', e); }
    document.getElementById('waterTempValue').textContent = latestTemp !== null ? latestTemp + ' °C' : document.getElementById('waterTempValue').textContent;
    document.getElementById('waterPressureValue').textContent = latestPressure !== null ? latestPressure + ' Bar' : document.getElementById('waterPressureValue').textContent;
    status.textContent = 'Geyser: loaded ' + rows.length + ' rows (public)';
  }catch(e){ status.textContent = 'Geyser error: ' + e.message; }
}

async function fetchPowerFromPublic(spreadsheetId, tabName){
  const status = document.getElementById('cfg-status');
  try{
    const rows = await fetchPublicSheet(spreadsheetId, tabName || undefined);
    if(!rows.length){ status.textContent = 'Power: no rows'; return; }
    const sample = rows[0];
    const keys = Object.keys(sample);
    const ampsKey = keys.find(k=>/amp|current/i.test(k)) || null;
    const kWKey = keys.find(k=>/kw|power|kW/i.test(k)) || null;
    const dailyKey = keys.find(k=>/daily|kwh|wh|energy/i.test(k)) || null;
    const last = rows[rows.length-1];
    const ampsVal = ampsKey ? (Number(last[ampsKey]) || null) : null;
    const kWVal = kWKey ? (Number(last[kWKey]) || null) : null;
    const dailyVal = dailyKey ? (Number(last[dailyKey]) || null) : null;
    try{ localStorage.setItem('power_sample', JSON.stringify({ ampsVal, kWVal, dailyVal, lastRow: last })); }catch(e){ console.warn('power sample save failed', e); }
    if(ampsKey) document.getElementById('currentAmps').textContent = (ampsVal !== null ? ampsVal : '--') + ' A';
    if(kWKey) document.getElementById('currentPower').textContent = (kWVal !== null ? kWVal : '--') + ' kW';
    if(dailyKey) document.getElementById('dailyPower').textContent = (dailyVal !== null ? dailyVal : '--') + ' kWh';
    status.textContent = 'Power: loaded ' + rows.length + ' rows (public)';
  }catch(e){ status.textContent = 'Power error: ' + e.message; }
}

async function fetchAll(){
  const cfgRaw = localStorage.getItem('scada_cfg');
  if(!cfgRaw) return;
  let cfg = null;
  try{ cfg = JSON.parse(cfgRaw); }catch(e){ console.warn('invalid cfg'); return; }
  const tasks = [];
  if(cfg.usePublic && cfg.sheetId){
    if(cfg.dvrTab) tasks.push(fetchDvrFromPublic(cfg.sheetId, cfg.dvrTab));
    else if(cfg.dvr) tasks.push(fetchDvrCsvViaProxy(cfg.dvr));
    if(cfg.iperlTab) tasks.push(fetchIperlFromPublic(cfg.sheetId, cfg.iperlTab));
    else if(cfg.iperl) tasks.push(fetchAndPopulateIperl(cfg.iperl));
    if(cfg.geyserTab) tasks.push(fetchGeyserFromPublic(cfg.sheetId, cfg.geyserTab));
    else if(cfg.geyser) tasks.push(fetchAndPopulateGeyser(cfg.geyser));
    if(cfg.powerTab) tasks.push(fetchPowerFromPublic(cfg.sheetId, cfg.powerTab));
    else if(cfg.power) tasks.push(fetchAndPopulatePower(cfg.power));
  }else{
    if(cfg.dvr) tasks.push(fetchDvrCsvViaProxy(cfg.dvr));
    if(cfg.iperl) tasks.push(fetchAndPopulateIperl(cfg.iperl));
    if(cfg.geyser) tasks.push(fetchAndPopulateGeyser(cfg.geyser));
    if(cfg.power) tasks.push(fetchAndPopulatePower(cfg.power));
  }
  // run DVR first then others
  try{ await Promise.all(tasks); document.getElementById('cfg-status').textContent = 'Last refresh: ' + new Date().toLocaleTimeString(); }catch(e){ document.getElementById('cfg-status').textContent = 'Refresh error: ' + e.message; }
}

// Build or update the main All Trends chart from samples saved in localStorage
function buildAllTrendsChart(){
  const dvrRaw = localStorage.getItem('dvr_sample');
  const geyserRaw = localStorage.getItem('geyser_sample');
  const iperlRaw = localStorage.getItem('iperl_sample');
  const powerRaw = localStorage.getItem('power_sample');
  let labels = [];
  let dvrTemps = [];
  if(dvrRaw){
    try{
      const d = JSON.parse(dvrRaw);
      const hist = d.history || [];
      labels = hist.map(p=> new Date(p.ts).toLocaleTimeString());
      dvrTemps = hist.map(p=> p.temp);
    }catch(e){ console.warn('dvr_sample parse', e); }
  }
  // fallback to sample data if nothing
  if(!labels.length){
    try{
      const sample = (window.__sample_scada_cache ||= null);
      if(!sample){ /* try to load file synchronously not possible; rely on existing populateScada to seed */ }
    }catch(e){}
  }

  // prepare datasets
  const datasets = [];
  if(dvrTemps.length){ datasets.push({ label: 'DVR °C', data: dvrTemps, borderColor:'#ff6384', fill:false, tension:0.2 }); }

  // geyser
  if(geyserRaw){
    try{
      const g = JSON.parse(geyserRaw);
      const gh = g.history || [];
      const vals = gh.map(p=> p.temp);
      if(vals.length){
        // align to labels length: if different, pad or slice
        let data = vals;
        if(labels.length && vals.length !== labels.length){
          // if labels longer, extend vals with last value; if shorter, pad
          if(vals.length < labels.length){ const last = vals[vals.length-1] || null; data = vals.concat(Array(labels.length - vals.length).fill(last)); }
          else if(vals.length > labels.length){ data = vals.slice(vals.length - labels.length); }
        }
        datasets.push({ label: 'Geyser °C', data, borderColor:'#3F51B5', fill:false, tension:0.2 });
      }
    }catch(e){ console.warn('geyser parse', e); }
  }

  // humidity/wind from iperl or sample: we attempt to use iperl history as generic series
  if(iperlRaw){
    try{
      const ip = JSON.parse(iperlRaw);
      const ih = ip.history || [];
      const vals = ih.map(p=> p.value);
      if(vals.length){
        let data = vals;
        if(labels.length && vals.length !== labels.length){ if(vals.length < labels.length){ const last = vals[vals.length-1] || null; data = vals.concat(Array(labels.length - vals.length).fill(last)); } else if(vals.length > labels.length){ data = vals.slice(vals.length - labels.length); } }
        datasets.push({ label: 'IPERL', data, borderColor:'#00BCD4', fill:false, tension:0.2 });
      }
    }catch(e){ console.warn('iperl parse', e); }
  }

  // power sample: show kW or amps as another dataset
  if(powerRaw){
    try{
      const p = JSON.parse(powerRaw);
      const last = p.lastRow || {};
      const numericKeys = Object.keys(last).filter(k=> !isNaN(Number(last[k])) );
      const sampleVal = numericKeys.length ? Number(last[numericKeys[0]]) : null;
      if(sampleVal !== null){
        const data = labels.map(()=> sampleVal);
        datasets.push({ label: 'Power (sample)', data, borderColor:'#4CAF50', fill:false, tension:0.2 });
      }
    }catch(e){ console.warn('power parse', e); }
  }

  const ctx = document.getElementById('allTrendsChart');
  if(!ctx) return;
  // get 2D context
  const ctx2d = ctx.getContext('2d');
  if(window._allTrendsChart) window._allTrendsChart.destroy();
  window._allTrendsChart = new Chart(ctx2d, {
    type: 'line',
    data: { labels: labels.length ? labels : ['...'], datasets },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: 'index', intersect: false },
      plugins: { legend: { display: true } },
      scales: { y: { beginAtZero: false } }
    }
  });
}

// Update left-side gauges and bars from localStorage samples
function updateGaugesFromSamples(){
  try{
    const dvrRaw = localStorage.getItem('dvr_sample');
    if(dvrRaw){ const d = JSON.parse(dvrRaw); if(d.temp!==undefined) document.getElementById('secDvrTemp')?.textContent = d.temp + ' °C'; if(d.temp!==undefined) document.getElementById('dvrTemp')?.textContent = d.temp + ' °C'; }
    const geyserRaw = localStorage.getItem('geyser_sample');
    if(geyserRaw){ const g = JSON.parse(geyserRaw); if(g.latestTemp!==undefined) document.getElementById('waterTempValue')?.textContent = g.latestTemp + ' °C'; if(g.latestPressure!==undefined) document.getElementById('waterPressureValue')?.textContent = g.latestPressure + ' Bar'; }
    const iperlRaw = localStorage.getItem('iperl_sample');
    if(iperlRaw){ const ip = JSON.parse(iperlRaw); if(ip.latest!==undefined) document.getElementById('mikeWaterReading')?.textContent = ip.latest + ' L'; }
    const powerRaw = localStorage.getItem('power_sample');
    if(powerRaw){ const p = JSON.parse(powerRaw); if(p.ampsVal!==undefined) document.getElementById('currentAmps')?.textContent = (p.ampsVal!==null? p.ampsVal : '--') + ' A'; if(p.kWVal!==undefined) document.getElementById('currentPower')?.textContent = (p.kWVal!==null? p.kWVal : '--') + ' kW'; }
    // wind and pressure: try sample_scada backup
    // if sample data available via populateScada, update wind text from DOM id windSpeed
    const windText = document.getElementById('windSpeed')?.textContent || document.getElementById('windSpeedText')?.textContent;
    if(windText) document.getElementById('windSpeedText') && (document.getElementById('windSpeedText').textContent = windText);
    // Update bars widths from power_sample if present
    try{
      const p = JSON.parse(localStorage.getItem('power_sample')||'{}');
      if(p.ampsVal!==undefined){ const pct = Math.max(0, Math.min(100, (p.ampsVal||0)*10)); const el = document.querySelector('.bar-fill.green'); if(el) el.style.width = pct + '%'; }
      if(p.kWVal!==undefined){ const pct = Math.max(0, Math.min(100, (p.kWVal||0)*10)); const el = document.querySelector('.bar-fill.aqua'); if(el) el.style.width = pct + '%'; }
    }catch(e){}
  }catch(e){ console.warn('updateGaugesFromSamples', e); }
}

// Hook into populateScada and fetch wrappers so UI updates immediately
const _origPopulateScada = populateScada;
function populateScadaAndUpdate(d){
  _origPopulateScada(d);
  try{ buildAllTrendsChart(); updateGaugesFromSamples(); }catch(e){ console.warn('update visuals failed', e); }
}
// replace calls
window.populateScada = populateScadaAndUpdate;

// Ensure update after public fetch wrappers
const _origFetchDvrFromPublic = fetchDvrFromPublic;
fetchDvrFromPublic = async function(spreadsheetId, tabName){ await _origFetchDvrFromPublic(spreadsheetId, tabName); buildAllTrendsChart(); updateGaugesFromSamples(); };
const _origFetchGeyserFromPublic = fetchGeyserFromPublic;
fetchGeyserFromPublic = async function(spreadsheetId, tabName){ await _origFetchGeyserFromPublic(spreadsheetId, tabName); buildAllTrendsChart(); updateGaugesFromSamples(); };
const _origFetchIperlFromPublic = fetchIperlFromPublic;
fetchIperlFromPublic = async function(spreadsheetId, tabName){ await _origFetchIperlFromPublic(spreadsheetId, tabName); buildAllTrendsChart(); updateGaugesFromSamples(); };
const _origFetchPowerFromPublic = fetchPowerFromPublic;
fetchPowerFromPublic = async function(spreadsheetId, tabName){ await _origFetchPowerFromPublic(spreadsheetId, tabName); buildAllTrendsChart(); updateGaugesFromSamples(); };

// Also ensure CSV/proxy fetchers call the update helpers after fetching
const _origFetchDvrCsvViaProxy = fetchDvrCsvViaProxy;
fetchDvrCsvViaProxy = async function(url){ await _origFetchDvrCsvViaProxy(url); buildAllTrendsChart(); updateGaugesFromSamples(); };
const _origFetchAndPopulateGeyser = fetchAndPopulateGeyser;
fetchAndPopulateGeyser = async function(url){ await _origFetchAndPopulateGeyser(url); buildAllTrendsChart(); updateGaugesFromSamples(); };
const _origFetchAndPopulateIperl = fetchAndPopulateIperl;
fetchAndPopulateIperl = async function(url){ await _origFetchAndPopulateIperl(url); buildAllTrendsChart(); updateGaugesFromSamples(); };
const _origFetchAndPopulatePower = fetchAndPopulatePower;
fetchAndPopulatePower = async function(url){ await _origFetchAndPopulatePower(url); buildAllTrendsChart(); updateGaugesFromSamples(); };

// Wire config UI actions
window.addEventListener('load', ()=>{
  // existing loadSample and buttons
  loadSample();
  document.getElementById('btn-fetch-dvr').addEventListener('click', ()=>{
    const url = document.getElementById('dvr-sheet-url').value.trim();
    if(!url) return alert('Enter DVR sheet URL');
    fetchDvrCsvViaProxy(url);
  });
  document.getElementById('btn-fetch-sg').addEventListener('click', ()=>{
    const lat = document.getElementById('sg-lat').value.trim();
    const lng = document.getElementById('sg-lng').value.trim();
    if(!lat || !lng) return alert('Enter lat and lng');
    fetchStormglass(lat,lng);
  });

  // config UI
  loadConfigToUI();
  document.getElementById('cfg-save').addEventListener('click', saveConfig);
  document.getElementById('cfg-refresh-now').addEventListener('click', ()=>{ fetchAll(); });
  document.getElementById('cfg-autorefresh').addEventListener('change', (e)=>{ applyAutoRefresh(e.target.checked); });
});

// If no user config exists, prefill with the sheet ID and tabs used by the Android app
(function prefillDefaultSheetConfig(){
  try{
    const existing = localStorage.getItem('scada_cfg');
    if(existing) return;
    const defaultCfg = {
      usePublic: true,
      // spreadsheetId from Android app
      sheetId: '1_GuECfJE0nFSGMS6-prrWAfLCR4gBPa3Qf_zzaKzAAA',
      // Prefill tab/gid defaults taken from Android app (updated iperlTab to Daq-data gid)
      dvrTab: '2109322930',
      iperlTab: '1710292753',
      geyserTab: '',
      powerTab: '',
      dvr: '', iperl: '', geyser: '', power: '',
      autorefresh: false
    };
    localStorage.setItem('scada_cfg', JSON.stringify(defaultCfg));
    console.info('Prefilled default SCADA sheet config from Android app');
  }catch(e){ console.warn('prefillDefaultSheetConfig failed', e); }
})();

// After page load, automatically trigger fetch if sheetId is present
window.addEventListener('load', ()=>{
  // existing load handlers will run; after a short delay, trigger fetchAll if public sheet configured
  setTimeout(()=>{
    try{
      const cfg = JSON.parse(localStorage.getItem('scada_cfg') || '{}');
      if(cfg && cfg.usePublic && cfg.sheetId){
        // run a fetch cycle to populate UI
        fetchAll();
      }
    }catch(e){ }
  }, 800);
});
