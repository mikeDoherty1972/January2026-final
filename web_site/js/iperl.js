// iperl.js - populate water meter page from sample data and optional proxy
async function loadIperlSample(){
  try{
    const resp = await fetch('../data/sample_iperl.json');
    const data = await resp.json();
    populateIperl(data);
  }catch(e){ console.warn('iperl sample load failed', e); }
}

function populateIperl(d){
  document.getElementById('mikeWaterReading').textContent = (d.latest || '--') + ' L';
  document.getElementById('mikeUsage').textContent = (d.day || '--') + ' / ' + (d.month || '--') + ' / ' + (d.total || '--');

  const ctx = document.getElementById('mikeWaterChart').getContext('2d');
  const labels = d.history.map(h=>new Date(h.ts).toLocaleDateString());
  const vals = d.history.map(h=>h.value);
  if(window._mikeChart) window._mikeChart.destroy();
  window._mikeChart = new Chart(ctx, { type: 'line', data:{labels, datasets:[{label:'Water (L)', data: vals, borderColor:'#2196F3', fill:false}]}, options:{scales:{y:{beginAtZero:true}}}});
}

async function fetchSheetViaProxy(url){
  const status = document.getElementById('sheet-status');
  status.textContent = 'Fetching...';
  try{
    const resp = await fetch('http://localhost:4000/api/sheets?url=' + encodeURIComponent(url));
    if(!resp.ok) throw new Error('fetch failed: ' + resp.status);
    const json = await resp.json();
    const rows = json.data;
    const history = rows.map(r=>{
      const keys = Object.keys(r);
      const tsKey = keys.find(k=>/date|time|ts|timestamp/i.test(k)) || keys[0];
      const valKey = keys.find(k=>/value|reading|meter|usage|total|litres|l/i.test(k)) || keys[1] || keys[0];
      let ts = Date.parse(r[tsKey]);
      if(Number.isNaN(ts)) ts = Date.now();
      const value = Number(r[valKey]) || 0;
      return { ts, value };
    });
    const latest = history.length ? history[history.length-1].value : null;
    populateIperl({ latest, day: null, month: null, total: null, history });
    status.textContent = 'Loaded ' + rows.length + ' rows';
  }catch(e){ status.textContent = 'Error: ' + e.message; }
}

function loadFromDashboard(){
  const raw = localStorage.getItem('iperl_sample');
  if(!raw) return alert('No IPERL sample found in Dashboard. Enable auto-refresh or fetch from Sheets.');
  try{
    const obj = JSON.parse(raw);
    populateIperl({ latest: obj.latest, day:null, month:null, total:null, history: obj.history });
    document.getElementById('sheet-status').textContent = 'Loaded from Dashboard';
  }catch(e){ alert('Failed to load from dashboard: ' + e.message); }
}

window.addEventListener('load', ()=>{
  loadIperlSample();
  document.getElementById('btn-fetch-sheet').addEventListener('click', ()=>{
    const url = document.getElementById('sheet-url').value.trim();
    if(!url) return alert('Enter sheet CSV export url');
    fetchSheetViaProxy(url);
  });
  document.getElementById('btn-load-from-dashboard').addEventListener('click', ()=>{ loadFromDashboard(); });
});
