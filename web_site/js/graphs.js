// graphs.js - small helpers for creating charts consistently
function makeLineChart(ctx, labels, data, options){
  return new Chart(ctx, { type:'line', data:{labels, datasets:[{label:options.label||'', data, borderColor:options.color||'#2196F3', fill:false}]}, options:{scales:{y:{beginAtZero:options.beginAtZero||false}}, plugins:{legend:{display:options.showLegend||false}}}});
}

