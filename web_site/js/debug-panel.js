// debug-panel.js - small UI to manually test public sheet fetches and show responses
(function(){
  function el(html){ const d = document.createElement('div'); d.innerHTML = html; return d.firstElementChild; }
  document.addEventListener('DOMContentLoaded', ()=>{
    const panel = el('<div id="debug-panel" style="position:fixed;right:12px;bottom:120px;width:420px;max-width:90vw;background:#fff;border-radius:8px;padding:8px;box-shadow:0 6px 20px rgba(0,0,0,0.12);z-index:9999;font-family:inherit;font-size:13px"></div>');
    panel.innerHTML = '<div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:6px"><strong>Debug</strong><button id="dbg-close" style="background:transparent;border:0;cursor:pointer">✕</button></div>' +
      '<div style="display:flex;gap:6px;margin-bottom:6px"><button id="dbg-fetch-main" class="btn">Fetch main (gid=0)</button><button id="dbg-fetch-dvr" class="btn">Fetch DVR (gid=2109322930)</button><button id="dbg-apply" class="btn">Apply to UI</button></div>' +
      '<div id="dbg-cfg" style="font-size:12px;color:#444;margin-bottom:6px"></div>' +
      '<div id="dbg-status" style="height:220px;overflow:auto;border:1px solid #eee;padding:6px;background:#f9fafb"></div>';
    document.body.appendChild(panel);
    document.getElementById('dbg-close').addEventListener('click', ()=> panel.remove());
    const status = document.getElementById('dbg-status');
    const cfgEl = document.getElementById('dbg-cfg');
    const show = (t)=>{ const p = document.createElement('pre'); p.style.whiteSpace='pre-wrap'; p.style.fontSize='12px'; p.textContent = t; status.prepend(p); };

    function showCfg(){ try{ cfgEl.textContent = 'scada_cfg: ' + localStorage.getItem('scada_cfg'); }catch(e){ cfgEl.textContent = 'scada_cfg: (err)'; } }
    showCfg();

    // Reusable mapping function: convert fetched rows into localStorage samples
    function applyRows(rows){
      if(!rows || !rows.length) return;'use strict';
      try{
        let headerRow = rows[0];
        let dataRows = rows.slice(1);
        const headerVals = Object.values(headerRow).map(v=> String(v||'').toLowerCase());
        const looksLikeHeader = headerVals.some(v=> /time|temp|water|dvr|wind|geyser|amps|kw|tide/i.test(v));
        if(!looksLikeHeader){ headerRow = {}; const first = rows[0]; Object.keys(first).forEach((k,i)=> headerRow[k]=k); dataRows = rows; }
        const colMap = {}; Object.keys(headerRow).forEach(k=>{ const v = String(headerRow[k]||'').trim().toLowerCase(); colMap[k]=v; });
        const findKey = (re)=> Object.keys(colMap).find(k=> colMap[k] && re.test(colMap[k]));
        const tsKey = findKey(/time|date|timestamp|thetime/);
        const dvrKey = findKey(/dvr.*temp|dvr temp|dvr|n/);
        const waterKey = findKey(/\bwater\b|total usage|usage|meter|k/);
        const geyserKey = findKey(/geyser|geyser temp|geyser.*temp|f/);
        const windKey = findKey(/wind speed|wind/);
        const windDirKey = findKey(/wind direction|wind dir|wind direction|m/);
        const ampsKey = findKey(/amp|amps|current|i/);
        const kWKey = findKey(/kw|kwh|power|kw daily|daily|h/);

        // Build DVR history
        const history = dataRows.map(r=>{
          const tsRaw = (tsKey && r[tsKey]) || r[Object.keys(r)[0]] || '';
          let ts = Date.parse(tsRaw); if(Number.isNaN(ts)) ts = Date.now();
          const temp = Number((dvrKey && r[dvrKey]) || Object.values(r).find((v,i)=>/dvr/i.test(colMap[Object.keys(r)[i]])) || 0) || 0;
          return { ts, temp };
        });
        const latestDvr = history.length ? history[history.length-1].temp : null;
        localStorage.setItem('dvr_sample', JSON.stringify({ temp: latestDvr, heartbeat: 'alive', history }));

        // IPERL / water
        if(waterKey){ const ipHistory = dataRows.map(r=>({ ts: Date.parse(r[tsKey])||Date.now(), value: Number(r[waterKey])||0 })); const latest = ipHistory.length ? ipHistory[ipHistory.length-1].value : null; localStorage.setItem('iperl_sample', JSON.stringify({ latest, history: ipHistory })); }

        // Geyser
        if(geyserKey){ const gHistory = dataRows.map(r=>({ ts: Date.parse(r[tsKey])||Date.now(), temp: Number(r[geyserKey])||0 })); const latestTemp = gHistory.length ? gHistory[gHistory.length-1].temp : null; localStorage.setItem('geyser_sample', JSON.stringify({ latestTemp, history: gHistory })); }

        // Power
        const lastRow = dataRows.length ? dataRows[dataRows.length-1] : null;
        if(lastRow){ const ampsVal = ampsKey ? (Number(lastRow[ampsKey])||null) : null; const kWVal = kWKey ? (Number(lastRow[kWKey])||null) : null; localStorage.setItem('power_sample', JSON.stringify({ ampsVal, kWVal, lastRow })); }

        // Weather
        if(windKey || windDirKey){ const last = dataRows.length ? dataRows[dataRows.length-1] : null; const windSpeed = last && windKey ? Number(last[windKey]) : null; const windDir = last && windDirKey ? Number(last[windDirKey]) : null; localStorage.setItem('weather_sample', JSON.stringify({ windSpeed, windDir })); }

        // last_update
        const lastTs = history.length ? history[history.length-1].ts : Date.now(); localStorage.setItem('last_update', lastTs);
        show('Auto-applied rows to UI localStorage');
        showCfg();
      }catch(e){ show('applyRows failed: ' + (e.message||e)); }
    }

    document.getElementById('dbg-fetch-main').addEventListener('click', async ()=>{
      show('Fetching main gviz...');
      try{
        // prefer the global fetchPublicSheet if available
        const sheetId = (localStorage.getItem('scada_cfg') ? JSON.parse(localStorage.getItem('scada_cfg')).sheetId : null) || '';
        const fetcher = (typeof window.fetchPublicSheet === 'function') ? window.fetchPublicSheet : fetchPublicSheetLocal;
        const rows = await fetcher(sheetId || '', '0');
        panel._lastRows = rows;
        // auto-apply so UI updates immediately
        try{ applyRows(rows); }catch(e){ show('Auto-apply failed: '+(e.message||e)); }
        show('Rows parsed: ' + (rows ? rows.length : 'none'));
        show(JSON.stringify(rows && rows.slice(0,4), null, 2));
        showCfg();
      }catch(e){ show('Error: ' + e.message || e); }
    });
    document.getElementById('dbg-fetch-dvr').addEventListener('click', async ()=>{
      show('Fetching DVR gviz...');
      try{
        const cfg = localStorage.getItem('scada_cfg') ? JSON.parse(localStorage.getItem('scada_cfg')) : null;
        const sid = cfg && cfg.sheetId ? cfg.sheetId : '';
        const fetcher = (typeof window.fetchPublicSheet === 'function') ? window.fetchPublicSheet : fetchPublicSheetLocal;
        const rows = await fetcher(sid, '2109322930');
        // save last fetch result for apply action
        panel._lastRows = rows;
        // auto-apply the fetched DVR rows
        try{ applyRows(rows); }catch(e){ show('Auto-apply failed: '+(e.message||e)); }
        show('Rows parsed: ' + (rows ? rows.length : 'none'));
        show(JSON.stringify(rows && rows.slice(0,6), null, 2));
        showCfg();
      }catch(e){ show('Error: ' + (e.message || e)); }
    });

    // Apply button - map the last fetched rows into localStorage samples used by the UI
    document.getElementById('dbg-apply').addEventListener('click', ()=>{
      const rows = panel._lastRows;
      if(!rows || !rows.length){ show('No fetched rows to apply - fetch first'); return; }
      try{ applyRows(rows); }catch(e){ show('Apply failed: ' + (e.message||e)); }
     });

    // Local fallback: replicate fetchPublicSheet behavior (gviz JSON -> CSV fallback)
    async function fetchPublicSheetLocal(spreadsheetId, sheetName){
      if(!spreadsheetId) throw new Error('missing spreadsheetId');
      let url = `https://docs.google.com/spreadsheets/d/${encodeURIComponent(spreadsheetId)}/gviz/tq?tqx=out:json`;
      if(sheetName){ if(/^\d+$/.test(String(sheetName).trim())) url += '&gid=' + encodeURIComponent(String(sheetName).trim()); else url += '&sheet=' + encodeURIComponent(sheetName); }
      // try gviz wrapper
      try{
        const resp = await fetch(url);
        if(!resp.ok) throw new Error('public sheet fetch failed: ' + resp.status);
        const text = await resp.text();
        const m = text.match(/google\.visualization\.Query\.setResponse\((([\s\S]+))\);?/);
        if(m){ const json = JSON.parse(m[1]); const table = json.table; const cols = (table.cols||[]).map(c=> c.label || c.id || ''); const rows = (table.rows||[]).map(r=>{ const obj={}; cols.forEach((h,i)=>{ const cell = r.c[i]; obj[h] = (cell && cell.v !== undefined) ? cell.v : ''; }); return obj; }); return rows; }
      }catch(e){ /* ignore and try CSV fallback */ }
      // CSV fallback
      try{
        let gidParam = '';
        if(sheetName && /^\d+$/.test(String(sheetName).trim())) gidParam = String(sheetName).trim();
        const csvUrl = `https://docs.google.com/spreadsheets/d/${encodeURIComponent(spreadsheetId)}/export?format=csv${gidParam?('&gid='+encodeURIComponent(gidParam)) : ''}`;
        const resp2 = await fetch(csvUrl);
        if(!resp2.ok) throw new Error('public sheet CSV fetch failed: ' + resp2.status);
        const csvText = await resp2.text();
        // simple CSV parser
        const lines = csvText.split(/\r?\n/).filter(l=>l.trim().length>0);
        const rows = lines.map(l=>{
          // naive split - handles quoted with commas minimally
          const cols = []; let cur=''; let q=false;
          for(let i=0;i<l.length;i++){ const ch=l[i]; const nxt=l[i+1]; if(ch==='"'){ if(q && nxt==='"'){ cur+='"'; i++; continue; } q=!q; continue; } if(ch===',' && !q){ cols.push(cur); cur=''; continue; } cur+=ch; } cols.push(cur);
          return cols;
        });
        if(!rows.length) return [];
        const headers = rows[0].map(h=> (h||'').trim());
        const out = rows.slice(1).map(r=>{ const obj={}; headers.forEach((h,i)=> obj[h || ('col'+i)] = (r[i] !== undefined ? r[i] : '')); return obj; });
        return out;
      }catch(e){ throw new Error('fetchPublicSheetLocal error: ' + (e.message || e)); }
    }

    // Expose a small toggle via console to remove panel
    window.__debugPanel = { remove: ()=> panel.remove(), panel };
  });
})();
