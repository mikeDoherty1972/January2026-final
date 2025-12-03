// initializer to auto-configure dashboard to use the Android public sheet and start fetching
(function(){
  const ANDROID_SHEET_ID = '1_GuECfJE0nFSGMS6-prrWAfLCR4gBPa3Qf_zzaKzAAA';
  const DEFAULTS = {
    usePublic: true,
    sheetId: ANDROID_SHEET_ID,
    dvrTab: '2109322930',
    iperlTab: '0',
    geyserTab: '',
    powerTab: '',
    autorefresh: true
  };

  function applyDefaults(){
    try{
      const raw = localStorage.getItem('scada_cfg');
      let cfg = raw ? JSON.parse(raw) : {};
      let changed = false;
      for(const k of Object.keys(DEFAULTS)){
        if(cfg[k] === undefined || cfg[k] === null || cfg[k] === ''){ cfg[k] = DEFAULTS[k]; changed = true; }
      }
      if(changed) localStorage.setItem('scada_cfg', JSON.stringify(cfg));
      // reflect in UI if present
      try{ if(document.getElementById('cfg-sheet-id')) document.getElementById('cfg-sheet-id').value = cfg.sheetId || ''; }catch(e){}
      try{ if(document.getElementById('cfg-dvr-tab')) document.getElementById('cfg-dvr-tab').value = cfg.dvrTab || ''; }catch(e){}
      try{ if(document.getElementById('cfg-iperl-tab')) document.getElementById('cfg-iperl-tab').value = cfg.iperlTab || ''; }catch(e){}
      try{ if(document.getElementById('cfg-use-public')) document.getElementById('cfg-use-public').checked = !!cfg.usePublic; }catch(e){}
      return cfg;
    }catch(e){ console.warn('init-dashboard applyDefaults failed', e); return null; }
  }

  document.addEventListener('DOMContentLoaded', async ()=>{
    // apply defaults, then call fetchAll if function available
    const cfg = applyDefaults();
    if(typeof fetchAll === 'function'){
      try{ await fetchAll(); }catch(e){ console.warn('fetchAll failed', e); }
      // if autorefresh enabled, set interval
      try{
        if(cfg && cfg.autorefresh){
          setInterval(()=>{ try{ fetchAll(); }catch(e){ console.warn('periodic fetchAll failed', e); } }, 60*1000);
        }
      }catch(e){}
    }
  });
})();

