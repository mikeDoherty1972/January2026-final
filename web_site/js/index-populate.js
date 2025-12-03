// index-populate.js - populate launcher tiles from localStorage samples if present
(function(){
  function safeGet(key){ try{ return JSON.parse(localStorage.getItem(key)); }catch(e){ return null; } }

  function populate(){
    const dvr = safeGet('dvr_sample');
    if(dvr && (dvr.temp !== undefined || (dvr.latestTemp !== undefined))){
      const tempVal = dvr.temp !== undefined ? dvr.temp : (dvr.latestTemp !== undefined ? dvr.latestTemp : null);
      const el = document.getElementById('dvr-temp');
      if(el) el.textContent = (tempVal!==null? (Number(tempVal).toFixed(1)+' °C') : '— °C');
      const hb = document.getElementById('dvr-heartbeat'); if(hb) hb.textContent = dvr.heartbeat || dvr.status || 'alive';
    }
    const iperl = safeGet('iperl_sample');
    if(iperl){
      const el = document.getElementById('mikeWaterReading'); if(el) el.textContent = (iperl.latest!==undefined? (String(iperl.latest)) : '—');
      const sub = document.getElementById('mikeUsage'); if(sub) sub.textContent = (iperl.latest!==undefined? (iperl.latest+' L') : '—');
    }
    const weather = safeGet('weather_sample');
    if(weather){ const w = document.getElementById('wind-speed'); if(w) w.textContent = (weather.windSpeed||'—') + ' m/s'; const t = document.getElementById('tide'); if(t) t.textContent = (weather.tide||'—') + ' m'; }

    // Last update support - read from scada_cfg or a last_update key if present
    try{
      const lastEl = document.getElementById('last-update');
      const cfg = safeGet('scada_cfg');
      if(lastEl){
        const lu = safeGet('last_update') || (cfg && cfg.last_update) || null;
        if(lu) lastEl.textContent = 'Last Update: ' + new Date(lu).toLocaleTimeString();
        else lastEl.textContent = 'Last Update: --:--:--';
      }
    }catch(e){}

    const power = safeGet('power_sample');
    // network / security - try to read from dashboard config if present
    try{
      const secEl = document.getElementById('networkStatus');
      const alarmEl = document.getElementById('alarmActiveStatus');
      // if there is a value in localStorage (security_status) use it
      const ss = localStorage.getItem('security_status');
      if(ss && secEl) secEl.textContent = ss;
      // also show a simple alarm count if stored
      const ac = localStorage.getItem('security_active_zones');
      if(ac && alarmEl) alarmEl.textContent = (Number(ac) > 0) ? `🔴 ${ac} Active` : `🟢 ${ac || '0'} Active`;
    }catch(e){}
  }

  document.addEventListener('DOMContentLoaded', ()=>{
    populate();
    // watch localStorage via poll (storage event doesn't fire in same tab reliably)
    setInterval(populate, 1500);
  });
})();
