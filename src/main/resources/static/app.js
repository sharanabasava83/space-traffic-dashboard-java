/* app.js — Orbital Traffic Control frontend
   Renders a rotating Earth with live satellite positions, and binds
   the telemetry table + alerts strip to a WebSocket feed from the backend. */

// ---------------------------------------------------------------
// 3D scene setup
// ---------------------------------------------------------------
const canvas = document.getElementById('globe-canvas');
const viewport = document.querySelector('.viewport-panel');

const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(50, 1, 0.1, 1000);
camera.position.set(0, 4, 14);

const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true });

function resizeRenderer() {
  const w = viewport.clientWidth;
  const h = viewport.clientHeight;
  renderer.setSize(w, h, false);
  camera.aspect = w / h;
  camera.updateProjectionMatrix();
}
window.addEventListener('resize', resizeRenderer);

const controls = new THREE.OrbitControls(camera, renderer.domElement);
controls.enableDamping = true;
controls.dampingFactor = 0.08;
controls.minDistance = 7;
controls.maxDistance = 30;
controls.autoRotate = true;
controls.autoRotateSpeed = 0.4;

// Lighting
scene.add(new THREE.AmbientLight(0x445566, 1.2));
const sun = new THREE.DirectionalLight(0x00e5ff, 0.6);
sun.position.set(5, 3, 5);
scene.add(sun);

// Earth: solid sphere + wireframe grid overlay for a HUD look
const EARTH_RADIUS = 5;

const earth = new THREE.Mesh(
  new THREE.SphereGeometry(EARTH_RADIUS, 48, 48),
  new THREE.MeshPhongMaterial({ color: 0x0c2540, shininess: 8 })
);
scene.add(earth);

const earthGrid = new THREE.Mesh(
  new THREE.SphereGeometry(EARTH_RADIUS * 1.001, 24, 24),
  new THREE.MeshBasicMaterial({ color: 0x00e5ff, wireframe: true, transparent: true, opacity: 0.12 })
);
scene.add(earthGrid);

// Faint outer atmosphere glow
const atmosphere = new THREE.Mesh(
  new THREE.SphereGeometry(EARTH_RADIUS * 1.04, 32, 32),
  new THREE.MeshBasicMaterial({ color: 0x00e5ff, transparent: true, opacity: 0.05, side: THREE.BackSide })
);
scene.add(atmosphere);

// Stars background
function buildStarfield() {
  const count = 900;
  const positions = new Float32Array(count * 3);
  for (let i = 0; i < count; i++) {
    const r = 80 + Math.random() * 60;
    const theta = Math.random() * Math.PI * 2;
    const phi = Math.acos((Math.random() * 2) - 1);
    positions[i * 3] = r * Math.sin(phi) * Math.cos(theta);
    positions[i * 3 + 1] = r * Math.sin(phi) * Math.sin(theta);
    positions[i * 3 + 2] = r * Math.cos(phi);
  }
  const geo = new THREE.BufferGeometry();
  geo.setAttribute('position', new THREE.BufferAttribute(positions, 3));
  const mat = new THREE.PointsMaterial({ color: 0x7d8aa0, size: 0.35 });
  scene.add(new THREE.Points(geo, mat));
}
buildStarfield();

// ---------------------------------------------------------------
// Satellite markers
// ---------------------------------------------------------------
const EARTH_RADIUS_KM = 6371.0;
const ALT_EXAGGERATION = 1.8; // visually exaggerate altitude so LEO sats are visible above the surface

const satMeshes = new Map(); // name -> THREE.Mesh

function latLonAltToVector3(lat, lon, altitudeKm) {
  const radiusUnits = EARTH_RADIUS + (altitudeKm / EARTH_RADIUS_KM) * EARTH_RADIUS * ALT_EXAGGERATION;
  const phi = (90 - lat) * (Math.PI / 180);
  const theta = (lon + 180) * (Math.PI / 180);
  const x = -radiusUnits * Math.sin(phi) * Math.cos(theta);
  const z = radiusUnits * Math.sin(phi) * Math.sin(theta);
  const y = radiusUnits * Math.cos(phi);
  return new THREE.Vector3(x, y, z);
}

function getOrCreateSatMesh(name) {
  if (satMeshes.has(name)) return satMeshes.get(name);
  const geo = new THREE.SphereGeometry(0.07, 8, 8);
  const mat = new THREE.MeshBasicMaterial({ color: 0x00e5ff });
  const mesh = new THREE.Mesh(geo, mat);
  scene.add(mesh);
  satMeshes.set(name, mesh);
  return mesh;
}

const RISK_COLOR = {
  critical: 0xff3b5c,
  high: 0xffb020,
  medium: 0x00e5ff,
};

function applyState(state) {
  const riskByName = {};
  state.alerts.forEach(a => {
    riskByName[a.satellite1] = a.risk_level;
    riskByName[a.satellite2] = a.risk_level;
  });

  state.satellites.forEach(sat => {
    const mesh = getOrCreateSatMesh(sat.name);
    const pos = latLonAltToVector3(sat.lat, sat.lon, sat.altitude_km);
    mesh.position.copy(pos);
    const risk = riskByName[sat.name];
    mesh.material.color.setHex(risk ? RISK_COLOR[risk] || 0xff3b5c : 0x00e5ff);
    mesh.scale.setScalar(risk ? 1.8 : 1);
  });

  updateTelemetryTable(state.satellites, riskByName);
  updateAlertsList(state.alerts);
  updateStatusChips(state);
}

// ---------------------------------------------------------------
// UI binding
// ---------------------------------------------------------------
const tableBody = document.getElementById('sat-table-body');
const alertsList = document.getElementById('alerts-list');
const satCountEl = document.getElementById('sat-count');
const alertCountEl = document.getElementById('alert-count');
const alertChip = document.getElementById('alert-chip');

function updateTelemetryTable(satellites, riskByName) {
  const sorted = [...satellites].sort((a, b) => a.name.localeCompare(b.name));
  tableBody.innerHTML = sorted.map(sat => `
    <tr class="${riskByName[sat.name] ? 'at-risk' : ''}">
      <td class="sat-name">${sat.name}</td>
      <td>${sat.altitude_km.toFixed(0)}</td>
      <td>${sat.velocity_km_s.toFixed(2)}</td>
    </tr>
  `).join('');
}

function updateAlertsList(alerts) {
  if (!alerts.length) {
    alertsList.innerHTML = '<div class="alert-empty">No active collision risks detected.</div>';
    return;
  }
  alertsList.innerHTML = alerts.map(a => `
    <div class="alert-card ${a.risk_level}">
      <div class="pair">${a.satellite1} &harr; ${a.satellite2}</div>
      <div class="meta">${a.distance_km.toFixed(1)} km &middot; ${a.risk_level.toUpperCase()} risk</div>
    </div>
  `).join('');
}

function updateStatusChips(state) {
  satCountEl.textContent = state.satellite_count;
  alertCountEl.textContent = state.alert_count;
  alertChip.classList.toggle('has-alerts', state.alert_count > 0);
}

// ---------------------------------------------------------------
// WebSocket connection (with auto-reconnect)
// ---------------------------------------------------------------
const connDot = document.getElementById('conn-dot');
const connText = document.getElementById('conn-text');

function setConnState(state) {
  connDot.classList.remove('connected', 'error');
  if (state === 'connected') {
    connDot.classList.add('connected');
    connText.textContent = 'LIVE';
  } else if (state === 'error') {
    connDot.classList.add('error');
    connText.textContent = 'RECONNECTING';
  } else {
    connText.textContent = 'CONNECTING';
  }
}

function connectSocket() {
  const proto = location.protocol === 'https:' ? 'wss://' : 'ws://';
  const ws = new WebSocket(proto + location.host + '/ws/tracking');

  ws.onopen = () => setConnState('connected');
  ws.onmessage = (evt) => applyState(JSON.parse(evt.data));
  ws.onerror = () => setConnState('error');
  ws.onclose = () => {
    setConnState('error');
    setTimeout(connectSocket, 2500);
  };
}
connectSocket();

// ---------------------------------------------------------------
// Clock + render loop
// ---------------------------------------------------------------
const clockEl = document.getElementById('sim-clock');
setInterval(() => {
  clockEl.textContent = new Date().toUTCString().split(' ')[4] + ' UTC';
}, 1000);

function animate() {
  requestAnimationFrame(animate);
  controls.update();
  renderer.render(scene, camera);
}
resizeRenderer();
animate();
