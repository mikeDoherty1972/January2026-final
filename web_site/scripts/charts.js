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
    "door": null
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
        const zoneKey = zoneName.toLowerCase().replace(' ','');
        card.href = cameraUrls[zoneKey] || '#';
        card.target = '_blank';
        card.className = `card zone-card ${cardClass}`;
        card.innerHTML = `<h3>${zoneName}</h3><div class="zone-status">${statusText}</div>`;
        grid.appendChild(card);
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
