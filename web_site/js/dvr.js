// dvr.js - sample DVR chart
async function loadDvr(){
  try{
    const resp = await fetch('../data/sample_scada.json');
    const data = await resp.json();
    const ctx = document.getElementById('dvrTempChart').getContext('2d');
    const labels = data.dvr.history.map(h=>new Date(h.ts).toLocaleTimeString());
    const vals = data.dvr.history.map(h=>h.temp);
    new Chart(ctx, { type: 'line', data:{labels, datasets:[{label:'DVR °C', data: vals, borderColor:'#FF5722', fill:false}]}, options:{scales:{y:{beginAtZero:false}}}});
    document.getElementById('dvrHeartbeatLog').textContent = 'Last heartbeat: ' + (data.dvr.heartbeat || '—');
  }catch(e){ console.warn('dvr load failed', e); }
}
window.addEventListener('load', ()=>{ loadDvr(); });

