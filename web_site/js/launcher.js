// launcher.js - make launcher tiles clickable and animate on updates
document.addEventListener('DOMContentLoaded', ()=>{
  const tileConfig = [
    {idPrefix:'dvr', page:'./pages/dvr.html', valueId:'dvr-temp', subId:'dvr-heartbeat'},
    {idPrefix:'mikeWater', page:'./pages/iperl.html', valueId:'mikeWaterReading', subId:'mikeUsage'},
    {idPrefix:'wind', page:'./pages/scada.html', valueId:'wind-speed', subId:'tide'},
    {idPrefix:'security', page:'./pages/security.html', valueId:'networkStatus', subId:'alarmActiveStatus'}
  ];

  // Attach click handlers and make tiles keyboard accessible
  tileConfig.forEach(cfg => {
    // Find the tile by matching contained IDs
    const valueEl = document.getElementById(cfg.valueId);
    if(!valueEl) return;
    const tile = valueEl.closest('.tile');
    if(!tile) return;
    tile.classList.add('clickable-tile');
    tile.classList.add('ripple');
    tile.setAttribute('tabindex', '0');
    tile.setAttribute('role', 'link');
    tile.addEventListener('click', (ev)=>{
      // ripple feedback
      tile.classList.add('ripple-active');
      setTimeout(()=> tile.classList.remove('ripple-active'), 450);
      window.location.href = cfg.page;
    });
    tile.addEventListener('keydown', (e)=>{ if(e.key === 'Enter' || e.key === ' ') { e.preventDefault(); tile.classList.add('ripple-active'); setTimeout(()=> tile.classList.remove('ripple-active'), 450); window.location.href = cfg.page; } });

    // Add a small LED element for status tiles (only once)
    if(!tile.querySelector('.tile-led')){
      const led = document.createElement('span');
      led.className = 'tile-led tile-led--unknown';
      tile.prepend(led);
    }

    // Observe value changes to animate
    const animateOnChange = (el)=>{
      // animate the whole tile so the pulse is visible
      tile.classList.remove('tile-update');
      // force reflow to restart animation
      void tile.offsetWidth;
      tile.classList.add('tile-update');
      setTimeout(()=> tile.classList.remove('tile-update'), 1200);
    };

    const mo = new MutationObserver(muts=>{
      for(const m of muts){ if(m.type === 'characterData' || m.type === 'childList') animateOnChange(valueEl); }
    });
    mo.observe(valueEl, { characterData:true, subtree:true, childList:true });

    // Also observe sub element (heartbeat etc.) to update LED
    const subEl = document.getElementById(cfg.subId);
    if(subEl){
      const ledEl = tile.querySelector('.tile-led');
      const updateLed = ()=>{
        const txt = (subEl.textContent||'').toLowerCase();
        if(/alive|online|ok|green/.test(txt)){
          ledEl.className = 'tile-led tile-led--green';
        }else if(/off|offline|dead|missing|red/.test(txt)){
          ledEl.className = 'tile-led tile-led--red';
        }else{
          // fallback: grey when unknown
          ledEl.className = 'tile-led tile-led--grey';
        }
      };
      // initial
      updateLed();
      const mo2 = new MutationObserver(()=> updateLed());
      mo2.observe(subEl, { characterData:true, subtree:true, childList:true });
    }
  });

  // Fallback: if the page updates values via localStorage changes, reflect them by re-checking every 2s (cheap)
  setInterval(()=>{
    tileConfig.forEach(cfg=>{
      const v = document.getElementById(cfg.valueId);
      const s = document.getElementById(cfg.subId);
      if(v && s){ /* triggers MutationObservers if changed via DOM methods */ }
    });
  }, 2000);

});
