// security.js - load security sample or dashboard-saved values
async function loadSecuritySample(){
  try{
    const resp = await fetch('../data/sample_security.json');
    const data = await resp.json();
    populateSecurity(data);
  }catch(e){ console.warn('security sample failed', e); }
}

function populateSecurity(d){
  document.getElementById('alarmActiveStatus').textContent = d.alarmActive ? 'Active' : 'Clear';
  document.getElementById('setupPresenceText').textContent = d.presence || 'Unknown';
  document.getElementById('networkStatus').textContent = d.networkStatus || 'Unknown';
  document.getElementById('notificationsList').textContent = (d.notifications && d.notifications.join('\n')) || 'No notifications';
  document.getElementById('zonesList').textContent = (d.zones && d.zones.map(z=>z.name + ': ' + (z.state||'ok')).join('\n')) || 'No zones';
  document.getElementById('secDvrTemp').textContent = (d.dvr && d.dvr.temp ? d.dvr.temp + ' °C' : '-- °C');
  document.getElementById('secDvrHeartbeat').textContent = (d.dvr && d.dvr.heartbeat) || '—';
}

function loadFromDashboard(){
  const rawDvr = localStorage.getItem('dvr_sample');
  const rawIperl = localStorage.getItem('iperl_sample');
  const rawG = localStorage.getItem('geyser_sample');
  const sec = { alarmActive: false, presence: 'At home', networkStatus: 'Unknown', notifications: [], zones: [] };
  if(rawDvr) try{ const d=JSON.parse(rawDvr); sec.dvr={ temp: d.temp, heartbeat: 'alive' }; }catch(e){}
  if(rawG) try{ const g=JSON.parse(rawG); if(g.latestTemp) sec.dvr = sec.dvr || {}; sec.dvr.temp = g.latestTemp; }catch(e){}
  if(rawIperl) try{ const i=JSON.parse(rawIperl); sec.iperl = { latest: i.latest }; }catch(e){}
  populateSecurity(sec);
  document.getElementById('security-status').textContent = 'Loaded from Dashboard';
}

window.addEventListener('load', ()=>{
  loadSecuritySample();
  document.getElementById('btn-load-security-from-dashboard').addEventListener('click', loadFromDashboard);
  document.getElementById('btn-check-network').addEventListener('click', ()=>{
    // quick local check: try fetch proxy health
    fetch(localStorage.getItem('proxy_base') || 'http://localhost:4000' + '/api/health').then(r=>r.json()).then(j=>{ document.getElementById('security-status').textContent = 'Network: ' + (j.ok? 'Proxy reachable':'Error'); }).catch(e=>{ document.getElementById('security-status').textContent = 'Network check failed: ' + e.message; });
  });
});

