// Genera las dos versiones de la app a partir de web/template.html:
//   dist/torre-abierta.html      -> versión web (Leaflet desde CDN)
//   android/assets/index.html    -> versión empaquetada en el APK (Leaflet incluido)
// Uso: npm install && node web/build.js
const fs = require("fs");
const path = require("path");
const topo = require("topojson-client");

const root = path.join(__dirname, "..");
const mod = p => path.join(root, "node_modules", p);

// Costas del mundo (Natural Earth 1:50m) simplificadas, para el mapa de respaldo sin conexión.
const t = JSON.parse(fs.readFileSync(mod("world-atlas/countries-50m.json"), "utf8"));
const land = topo.feature(t, t.objects.land);
const r = v => Math.round(v * 50) / 50;
function ring(c) {
  const o = [];
  for (const p of c) {
    const q = [r(p[0]), r(p[1])], l = o[o.length - 1];
    if (!l || l[0] !== q[0] || l[1] !== q[1]) o.push(q);
  }
  return o.length >= 4 ? o : null;
}
function geom(g) {
  if (g.type === "Polygon") { const rs = g.coordinates.map(ring).filter(Boolean); return rs.length ? [rs] : []; }
  if (g.type === "MultiPolygon") return g.coordinates.map(p => p.map(ring).filter(Boolean)).filter(p => p.length);
  return [];
}
const feats = land.type === "FeatureCollection" ? land.features : [land];
const world = { type: "Feature", properties: {}, geometry: { type: "MultiPolygon", coordinates: feats.flatMap(f => geom(f.geometry)) } };

const css = fs.readFileSync(mod("leaflet/dist/leaflet.css"), "utf8");
const ljs = fs.readFileSync(mod("leaflet/dist/leaflet.js"), "utf8");
const base = fs.readFileSync(path.join(__dirname, "template.html"), "utf8")
  .replace("/*__LEAFLET_CSS__*/", () => css)
  .replace("/*__WORLD__*/null", () => JSON.stringify(world));

const web = base.replace("<!--__LEAFLET_JS__-->", '<script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js"></script>');
fs.mkdirSync(path.join(root, "dist"), { recursive: true });
fs.writeFileSync(path.join(root, "dist", "torre-abierta.html"), web);

const app = '<!doctype html><html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">'
  + '<style>[hidden]{display:none!important}body{margin:0}img{max-width:100%}</style></head><body>'
  + base.replace("<!--__LEAFLET_JS__-->", () => "<script>" + ljs.replace(/<\/script/gi, "<\\/script") + "</script>")
  + "</body></html>";
fs.mkdirSync(path.join(root, "android", "assets"), { recursive: true });
fs.writeFileSync(path.join(root, "android", "assets", "index.html"), app);

console.log("Listo: dist/torre-abierta.html y android/assets/index.html");
