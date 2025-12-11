const SHEET_ID = '1_GuECfJE0nFSGMS6-prrWAfLCR4gBPa3Qf_zzaKzAAA';
const CSV_URL = `https://docs.google.com/spreadsheets/d/${SHEET_ID}/export?format=csv&gid=0`;

const firebaseConfig = {
  apiKey: "AIzaSyB1mlojJKCRKKIl7wLZEKs87ajKmS4S3fY",
  authDomain: "security-33809.firebaseapp.com",
  projectId: "security-33809",
  storageBucket: "security-33809.appspot.com",
  messagingSenderId: "741577311231",
  appId: "1:741577311231:web:188bdcbbc1f96ed9585dca",
  measurementId: "G-Z1YBS7D0MR"
};

const cameraUrls = {
    "garage": "https://drive.google.com/uc?export=download&id=1i7yQEZAIEwjB1aYfN0jKUwVdOKhL2uBr",
    "garageside": "https://drive.google.com/uc?export=download&id=1mZTC_6Nb4thZAQvH4bafJVwO6p8yB6KH",
    "south": "https://drive.google.com/uc?export=download&id=1Fw9dvmwaWkw8RjCi18Zr_fUdXzN5MApw",
    "back": "https://drive.google.com/uc?export=download&id=1D-RC21Z5p9atX1xTle31uDlA1dgqAAJ-",
    "north": "https://drive.google.com/uc?export=download&id=1uPhu1YBPCTVtDog8a7TdTOt8zI2x3s_L",
    "front": "https://drive.google.com/uc?export=download&id=1aGcQTvdpYNsmUNOLN-4TFruxk7MTu-wg",
    "door": "https://drive.google.com/uc?export=view&id=1Ngq-uxaXuoHFySX-ynU1vwafeRh5InU5"
};

let securityActivityCache = [];
let chartInstances = {};

document.addEventListener('DOMContentLoaded', () => {
  if (typeof firebase !== 'undefined' && firebaseConfig.apiKey) {
    try {
        if (firebase.apps.length === 0) firebase.initializeApp(firebaseConfig);
    } catch (e) { console.error("Firebase initialization failed:", e); }
  }

  if (document.getElementById('live-weather-value')) fetchDataForDashboard();
  if (document.getElementById('current-power-chart')) fetchDataAndCreatePowerCharts();
  if (document.getElementById('temperature-chart')) fetchDataAndCreateWeatherCharts();
  if (document.getElementById('mike-water-chart')) fetchDataAndCreateWaterChart();
  if (document.getElementById('csv-data')) fetchDataForDebug();
  if (document.getElementById('zones-grid')) listenForSecurityUpdates();
});

function listenForSecurityUpdates() {
    if (typeof firebase === 'undefined' || !firebase.apps.length) return;
    const db = firebase.firestore();
    const docRef = db.collection("security sensors").doc("live_status");
    docRef.onSnapshot((doc) => {
        if (doc.exists) {
            const data = doc.data();
            if(document.getElementById('zones-grid')) updateSecurityPage(data);
            if(document.getElementById('security-summary-value')) updateDashboardSecurity(data);
        }
    });
}

function ensureCameraModal() {
    if (document.getElementById('camera-modal')) return;
    const modal = document.createElement('div');
    modal.id = 'camera-modal';
    modal.style.cssText = 'position:fixed;left:0;top:0;width:100%;height:100%;background:rgba(0,0,0,0.75);display:flex;align-items:center;justify-content:center;z-index:9999;visibility:hidden;opacity:0;transition:opacity .18s ease';
    modal.innerHTML = `
      <div id="camera-modal-inner" style="max-width:95%;max-height:95%;background:transparent;border-radius:6px;overflow:auto;">
         <img id="camera-modal-img" src="" alt="Camera" style="max-width:100%;max-height:100%;display:none;border-radius:4px;box-shadow:0 8px 24px rgba(0,0,0,0.6);background:#222" />
         <iframe id="camera-modal-iframe" src="" frameborder="0" style="width:90vw;height:80vh;display:none;border-radius:6px;box-shadow:0 8px 24px rgba(0,0,0,0.6);background:#000"></iframe>
         <div id="camera-modal-caption" style="color:#fff;text-align:center;margin-top:8px;font-weight:500"></div>
      </div>
    `;
    modal.addEventListener('click', (e) => {
        const inner = document.getElementById('camera-modal-inner');
        if (!inner.contains(e.target)) hideCameraModal();
    });
    document.body.appendChild(modal);
    // close on Esc
    window.addEventListener('keydown', (e) => { if (e.key === 'Escape') hideCameraModal(); });
}

// Return Drive preview iframe URL for a given Drive URL or id
function drivePreviewUrl(url) {
    try {
        if (!url) return null;
        // If it's already a preview URL
        if (url.indexOf('drive.google.com') === -1) return null;
        // extract id
        const m = url.match(/\/d\/([-_A-Za-z0-9]+)(?:\/|\/view|$)/);
        if (m && m[1]) return `https://drive.google.com/file/d/${m[1]}/preview`;
        const idMatch = url.match(/[?&]id=([-_A-Za-z0-9]+)/);
        if (idMatch && idMatch[1]) return `https://drive.google.com/file/d/${idMatch[1]}/preview`;
        // if uc?id=ID
        const ucMatch = url.match(/id=([-_A-Za-z0-9]+)/);
        if (ucMatch && ucMatch[1]) return `https://drive.google.com/file/d/${ucMatch[1]}/preview`;
        return null;
    } catch (e) { console.warn('drivePreviewUrl failed', e); return null; }
}

function showCameraModal(url, caption) {
    try {
        ensureCameraModal();
        const modal = document.getElementById('camera-modal');
        const img = document.getElementById('camera-modal-img');
        const iframe = document.getElementById('camera-modal-iframe');
        const cap = document.getElementById('camera-modal-caption');
        const nurl = normalizeDriveUrl(url) || url;
        // If the URL is a Drive URL, prefer the Drive preview iframe to avoid download behavior
        const dp = drivePreviewUrl(nurl);
        if (dp) {
            // show iframe
            img.style.display = 'none';
            iframe.style.display = 'block';
            iframe.src = dp;
        } else {
            // show image tag
            iframe.style.display = 'none';
            iframe.src = 'about:blank';
            img.style.display = 'block';
            img.src = nurl;
        }
        cap.textContent = caption || '';
        modal.style.visibility = 'visible';
        modal.style.opacity = '1';
    } catch (e) { console.warn('showCameraModal failed', e); window.open(url, '_blank'); }
}

function hideCameraModal(){
    const modal = document.getElementById('camera-modal');
    if(!modal) return;
    // clear iframe src to stop playback
    const iframe = document.getElementById('camera-modal-iframe');
    if (iframe) iframe.src = 'about:blank';
    const img = document.getElementById('camera-modal-img');
    if (img) img.src = '';
    modal.style.opacity='0'; setTimeout(()=>{ if(modal) modal.style.visibility='hidden'; }, 200);
}

function updateSecurityPage(data) {
    const zones = {
        'Front': ['front_motion', 'front_sensor'], 'Garage': ['garage_motion', 'garage_sensor'], 'Garage Side': ['garage_side_motion', 'garage_side_sensor'],
        'North': ['north_motion', 'north_sensor'], 'South': ['south_motion', 'south_sensor'], 'Back': ['back_motion', 'back_sensor'], 'Door': ['door_motion', 'door_sensor']
    };
    const grid = document.getElementById('zones-grid');
    grid.innerHTML = '';
    let activeZones = 0;
    const newActivities = [];

    for (const [zoneName, sensorKeys] of Object.entries(zones)) {
        const sensor1 = sensorValueToBoolean(data[sensorKeys[0]]);
        const sensor2 = sensorValueToBoolean(data[sensorKeys[1]]);
        let statusText = '🟢 Clear', cardClass = 'status-clear';

        if (sensor1 && sensor2) {
            statusText = '🔴 BREACH!', cardClass = 'status-breach', activeZones++;
            newActivities.push(`${new Date().toLocaleTimeString()}: ${zoneName} - DUAL SENSOR BREACH`);
        } else if (sensor1 || sensor2) {
            statusText = '🟡 Motion', cardClass = 'status-motion';
            newActivities.push(`${new Date().toLocaleTimeString()}: ${zoneName} - ${sensor1 ? sensorKeys[0] : sensorKeys[1]} detected`);
        }
        
        const card = document.createElement('a');
        const zoneKey = zoneName.toLowerCase().replace(' ', '');
        // avoid direct navigation/download: use '#' as href and store camera URL in data attribute
        const camUrl = cameraUrls[zoneKey] || null;
        card.href = '#';
        card.dataset.camUrl = camUrl || '';
        card.className = `card zone-card ${cardClass}`;
        card.innerHTML = `<h3>${zoneName}</h3><div class="zone-status">${statusText}</div>`;
        grid.appendChild(card);

        // wire click to show modal when a camera URL exists
        (function(zName, url){
            if(url){
                card.addEventListener('click', function(ev){ ev.preventDefault(); showCameraModal(url, zName); });
            } else {
                card.addEventListener('click', function(ev){ ev.preventDefault(); alert('No camera configured for ' + zName); });
            }
        })(zoneName, camUrl);
    }

    document.getElementById('system-status-text').innerHTML = activeZones > 0 ? `🔴 ${activeZones} Zone(s) Active - ALERT!` : '🟢 All Zones Armed - System Online';

    if (newActivities.length > 0) {
        securityActivityCache.unshift(...newActivities.reverse());
        if (securityActivityCache.length > 20) securityActivityCache.length = 20;
    }
    
    document.getElementById('recent-activity-text').innerHTML = securityActivityCache.length > 0 ? securityActivityCache.slice(0, 5).join('<br>') : 'No recent activity';
}

function updateDashboardSecurity(data) {
    let activeZones = 0;
    const zones = ['front','garage','garageside','north','south','back','door'];
    for (const zone of zones) {
        if (sensorValueToBoolean(data[`${zone}_motion`]) && sensorValueToBoolean(data[`${zone}_sensor`])) activeZones++;
    }
    const summaryDiv = document.getElementById('security-summary-value');
    summaryDiv.textContent = activeZones > 0 ? "BREACH!" : "System Armed";
    summaryDiv.className = `dashboard-card-value ${activeZones > 0 ? 'status-breach' : 'status-clear'}`;
}

function sensorValueToBoolean(value) { return value === true || value === 1 || value === '1' || String(value).toLowerCase() === 'true'; }

async function fetchCsvData() {
    const response = await fetch(CSV_URL);
    if (!response.ok) throw new Error(`Failed to fetch CSV: ${response.statusText}`);
    return parseCsvData(await response.text());
}

async function fetchDataForDashboard() {
    try {
        const readings = await fetchCsvData();
        if (readings.length > 0) {
            const lastReading = readings[readings.length - 1];
            document.getElementById('live-weather-value').textContent = `${resolveField(lastReading, ['temp out'])}°C`;
            document.getElementById('humidity-value').textContent = `Humidity: ${resolveField(lastReading, ['humidity'])}%`;
            document.getElementById('wind-speed-value').textContent = `${resolveField(lastReading, ['wind speed'])} km/h`;
            document.getElementById('power-usage-value').textContent = `${resolveField(lastReading, ['kw'])} kW`;
            document.getElementById('power-amps-value').textContent = resolveField(lastReading, ['amps']);
            document.getElementById('power-daily-value').textContent = resolveField(lastReading, ['kw daily']);
            document.getElementById('water-meter-value').textContent = resolveField(lastReading, ['water']);
            document.getElementById('water-daily-value').textContent = resolveField(lastReading, ['water daily']);
            document.getElementById('water-total-value').textContent = resolveField(lastReading, ['water']); // Assuming 'water' is the total

            const windDirection = parseFloat(resolveField(lastReading, ['wind direction'])) + 180;
            document.getElementById('wind-barb').style.transform = `rotate(${windDirection}deg)`;
        }
    } catch (error) { console.error("Error fetching data for dashboard:", error); }
    listenForSecurityUpdates();
}

async function fetchDataForDebug() {
    const response = await fetch(CSV_URL); document.getElementById('csv-data').innerText = await response.text();
}

async function fetchDataAndCreatePowerCharts() {
  try {
    const readings = await fetchCsvData();
    if (readings.length > 0) createPowerCharts(readings);
  } catch (error) { console.error('Error creating power charts:', error); }
}

async function fetchDataAndCreateWeatherCharts() {
  try {
    const readings = await fetchCsvData();
    if (readings.length > 0) createWeatherCharts(readings);
  } catch (error) { console.error('Error creating weather charts:', error); }
}

async function fetchDataAndCreateWaterChart() {
  try {
     const response = await fetch(CSV_URL), csvData = await response.text(), readings = parseWaterCsvData(csvData);
    if (readings.length > 0) createWaterChart(readings);
  } catch (error) { console.error('Error creating water chart:', error); }
}

function resolveField(reading, headerNames) {
    for (const name of headerNames) {
        const key = name.toLowerCase().replace(/\s/g, '');
        if (reading.hasOwnProperty(key)) return reading[key];
    }
    return '';
}

function parseCsvLine(line) {
    const fields = []; let currentField = ''; let inQuotes = false;
    for (let i = 0; i < line.length; i++) {
        const char = line[i];
        if (char === '"') inQuotes = !inQuotes;
        else if (char === ',' && !inQuotes) { fields.push(currentField); currentField = ''; }
        else currentField += char;
    }
    fields.push(currentField);
    return fields;
}

function parseCsvData(csvData) {
  const lines = csvData.split('\n').filter(line => line.trim() !== '');
  if (lines.length <= 1) return [];
  const header = parseCsvLine(lines[0]).map(h => h.trim().toLowerCase().replace(/\s/g, ''));
  return lines.slice(1).map(line => {
    const values = parseCsvLine(line), reading = {};
    header.forEach((h, i) => { reading[h] = values[i] ? values[i].trim() : ''; });
    return reading;
  });
}

function parseWaterCsvData(csvData) {
  const lines = csvData.split('\n').filter(line => line.trim() !== '');
  if (lines.length <= 1) return [];
  return lines.slice(1).map(line => {
    const values = parseCsvLine(line);
    return { timestamp: values[0] ? values[0].trim() : '', meterReading: values[10] ? parseFloat(values[10].trim()) : 0 };
  }).filter(r => r.timestamp);
}

function createPowerCharts(readings) {
  const timestamps = readings.map(r => resolveField(r, ['thetime']));
  createChart('current-power-chart', timestamps, [{ label: 'Power (kW)', data: readings.map(r => parseFloat(resolveField(r, ['kw']))), borderColor: '#00796b' }]);
  createChart('current-amps-chart', timestamps, [{ label: 'Current (A)', data: readings.map(r => parseFloat(resolveField(r, ['amps']))), borderColor: '#00796b' }]);
  createChart('daily-total-chart', timestamps, [{ label: 'Daily (kWh)', data: readings.map(r => parseFloat(resolveField(r, ['kwdaily']))), borderColor: '#00796b' }]);
}

function createWeatherCharts(readings) {
    const timestamps = readings.map(r => resolveField(r, ['thetime']));
    createChart('temperature-chart', timestamps, [
        { label: 'Indoor °C', data: readings.map(r => parseFloat(resolveField(r, ['temp in']))), borderColor: '#e53935' },
        { label: 'Outdoor °C', data: readings.map(r => parseFloat(resolveField(r, ['temp out']))), borderColor: '#1e88e5' }
    ]);
    createChart('humidity-chart', timestamps, [{ label: 'Humidity %', data: readings.map(r => parseFloat(resolveField(r, ['humidity']))), borderColor: '#1e88e5' }]);
    createChart('wind-chart', timestamps, [{ label: 'Wind km/h', data: readings.map(r => parseFloat(resolveField(r, ['wind speed']))), borderColor: '#43a047' }]);
    createChart('wind-direction-chart', timestamps, [{ label: 'Wind Dir °', data: readings.map(r => parseFloat(resolveField(r, ['winddirection']))), borderColor: '#8e24aa' }]);
}

function createWaterChart(readings) {
  createChart('mike-water-chart', readings.map(r => r.timestamp), [{ label: 'Meter Reading', data: readings.map(r => r.meterReading), borderColor: '#1e88e5' }]);
}

function createChart(canvasId, labels, datasets) {
  if (chartInstances[canvasId]) chartInstances[canvasId].destroy();
  const finalDatasets = datasets.map(ds => ({ ...ds, borderWidth: 2, fill: false }));
  chartInstances[canvasId] = new Chart(document.getElementById(canvasId).getContext('2d'), {
    type: 'line',
    data: { labels: labels, datasets: finalDatasets },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: datasets.length > 1, labels: { color: '#333' } } },
      scales: {
        x: { ticks: { color: '#333' }, grid: { color: '#ddd' } },
        y: { ticks: { color: '#333' }, grid: { color: '#ddd' } }
      }
    }
  });
}
