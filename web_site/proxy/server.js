const express = require('express');
const cors = require('cors');
const fetch = require('node-fetch');
const csvParse = require('csv-parse/sync');
const { google } = require('googleapis');
const app = express();
const port = process.env.PORT || 4000;

app.use(cors());
app.use(express.json());

// Simple in-memory cache
const cache = new Map();
function setCache(key, value, ttlSeconds=300){
  cache.set(key, { value, exp: Date.now() + ttlSeconds*1000 });
}
function getCache(key){
  const v = cache.get(key);
  if(!v) return null;
  if(Date.now() > v.exp){ cache.delete(key); return null; }
  return v.value;
}

app.get('/api/health', (req, res) => res.json({ ok: true, time: new Date().toISOString() }));

// GET /api/sheets?url=<export-csv-url>
app.get('/api/sheets', async (req, res) => {
  const url = req.query.url;
  if(!url) return res.status(400).json({ error: 'missing url query param' });
  try{
    const key = 'sheets:' + url;
    const cached = getCache(key);
    if(cached) return res.json({ cached:true, data: cached });
    const resp = await fetch(url);
    if(!resp.ok) return res.status(502).json({ error: 'bad upstream', status: resp.status });
    const text = await resp.text();
    const records = csvParse.parse(text, { columns: true, skip_empty_lines: true });
    setCache(key, records, 300);
    res.json({ cached:false, data: records });
  }catch(e){ res.status(500).json({ error: e.message }); }
});

// GET /api/sheets_private?spreadsheetId=&range=
// Uses a Google service account JSON provided in env var GOOGLE_SERVICE_ACCOUNT_JSON
app.get('/api/sheets_private', async (req, res) => {
  const spreadsheetId = req.query.spreadsheetId;
  const range = req.query.range || undefined; // e.g. 'Sheet1!A:Z' or 'A:Z'
  if(!spreadsheetId) return res.status(400).json({ error: 'missing spreadsheetId query param' });

  const saEnv = process.env.GOOGLE_SERVICE_ACCOUNT_JSON;
  if(!saEnv) return res.status(500).json({ error: 'server missing GOOGLE_SERVICE_ACCOUNT_JSON env var' });

  try{
    // support either base64-encoded JSON or raw JSON
    let saJson = null;
    try{ saJson = JSON.parse(Buffer.from(saEnv, 'base64').toString('utf8')); }catch(e){
      try{ saJson = JSON.parse(saEnv); }catch(e2){ return res.status(500).json({ error: 'invalid GOOGLE_SERVICE_ACCOUNT_JSON format' }); }
    }

    const cacheKey = `sheets_private:${spreadsheetId}:${range||''}`;
    const cached = getCache(cacheKey);
    if(cached) return res.json({ cached:true, data: cached });

    const jwtClient = new google.auth.JWT(
      saJson.client_email,
      null,
      saJson.private_key,
      ['https://www.googleapis.com/auth/spreadsheets.readonly']
    );
    await jwtClient.authorize();
    const sheets = google.sheets({ version: 'v4', auth: jwtClient });
    const response = await sheets.spreadsheets.values.get({ spreadsheetId, range });
    const values = response.data.values || [];
    if(values.length === 0){ setCache(cacheKey, [], 60); return res.json({ cached:false, data: [] }); }
    const headers = values[0];
    const rows = values.slice(1).map(r=>{ const obj = {}; headers.forEach((h,i)=> obj[h]= r[i] !== undefined ? r[i] : ''); return obj; });
    setCache(cacheKey, rows, 60);
    res.json({ cached:false, data: rows });
  }catch(e){
    console.error('sheets_private error', e);
    res.status(500).json({ error: e.message });
  }
});

// GET /api/stormglass?lat=&lng=&key=ENV_KEY_NAME
// key is the name of an env variable that holds the Stormglass API key
app.get('/api/stormglass', async (req, res) => {
  const lat = req.query.lat;
  const lng = req.query.lng;
  const envKey = req.query.key || 'STORMGLASS_API_KEY';
  const apiKey = process.env[envKey];
  if(!apiKey) return res.status(500).json({ error: 'server missing Stormglass API key env var: ' + envKey });
  if(!lat || !lng) return res.status(400).json({ error: 'missing lat or lng' });
  try{
    const cacheKey = `sg:${lat}:${lng}`;
    const cached = getCache(cacheKey);
    if(cached) return res.json({ cached:true, data: cached });
    const params = `lat=${lat}&lng=${lng}&params=tideHeight,windSpeed,windDirection&source=sg`;
    const resp = await fetch('https://api.stormglass.io/v2/weather/point?' + params, { headers: { 'Authorization': apiKey } });
    if(!resp.ok) return res.status(502).json({ error: 'stormglass error', status: resp.status });
    const data = await resp.json();
    setCache(cacheKey, data, 60*10); // 10 minutes
    res.json({ cached:false, data });
  }catch(e){ res.status(500).json({ error: e.message }); }
});

app.listen(port, ()=>console.log('Proxy listening on port', port));
