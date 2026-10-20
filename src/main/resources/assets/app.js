/* SMHY dashboard UI. Reads dashboard.json (schema 1) generated next to this page.
   Hours and windows are the active provider's rated forecast. SMHI, Open-Meteo and
   Holfuy stay "kommande" and never get invented numbers. */
(function () {
  const DIRS = ["N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"];
  const SV = {N:"N", NNE:"NNO", NE:"NO", ENE:"ONO", E:"O", ESE:"OSO", SE:"SO", SSE:"SSO", S:"S", SSW:"SSV", SW:"SV", WSW:"VSV", W:"V", WNW:"VNV", NW:"NV", NNW:"NNV"};
  const SC = {1:"#c4151c", 2:"#e0241b", 3:"#ef4b1d", 4:"#f37a12", 5:"#f5a300", 6:"#e3bf00", 7:"#b5c800", 8:"#7cc22a", 9:"#3fae3d", 10:"#139a48"};
  const LIGHT = new Set([6, 7]);
  const WORD = s => s >= 9 ? "Utmärkt" : s >= 7 ? "Bra" : s >= 5 ? "Okej" : s >= 3 ? "Marginellt" : "Dåligt";
  const WR = [[0, "#f2fbff"], [2, "#c9f0fb"], [3, "#9fe6f3"], [4, "#7fdcc0"], [5, "#8fe08a"], [6, "#c5ea6a"], [7, "#f0e45c"], [8, "#f7c04a"], [9, "#f59a42"], [10, "#ef6a4a"], [12, "#e0457b"]];
  const windCol = v => { let c = WR[0][1]; for (const [t, col] of WR) if (v >= t) c = col; return c; };
  const X0 = 10.9, Y1 = 69.2, K = Math.cos(62 * Math.PI / 180), SS = 31.7919;
  const NUDGE = {granholmstoppen:[2, -4], lovsta:[-4, 1], uttran:[1, 5], areskutan:[4, -1], morvikshummeln:[-4, 2]};
  const LBL = {granholmstoppen:["end", -8, 11], lovsta:["end", -8, 2], uttran:["start", 7, 8], areskutan:["start", 8, -3], morvikshummeln:["start", 8, 7], salanda:["end", -8, -6]};
  const SWE = window.SMHY_SWE || "";

  let D = null;
  let DAYS = [];
  let SITES = {};
  let ORDER = [];
  let WINDOWS = [];
  let favs = new Set();
  const S = {day: null, view: window.SMHY_VIEW || "vecka", metric: "score", sel: {site: null, date: null}};
  let cmdIdx = 0;

  const pad = n => String(n).padStart(2, "0");
  const esc = s => String(s ?? "").replace(/[&<>"']/g, c => ({"&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"}[c]));
  const fmt = n => {
    if (n == null || Number.isNaN(+n)) return "–";
    const x = Math.round(+n * 10) / 10;
    return Number.isInteger(x) ? String(x) : String(x);
  };
  const dayBy = date => DAYS.find(d => d.date === date);
  const hm = s => {
    if (!s || !s.includes(":")) return null;
    const [h, m] = s.split(":").map(Number);
    return h + m / 60;
  };
  const dname = d => {
    if (d == null || Number.isNaN(+d)) return null;
    return DIRS[Math.round((((+d % 360) + 360) % 360) / 22.5) % 16];
  };
  const launchOf = id => SITES[id] && SITES[id].launch ? SITES[id].launch : {good: [], ok: []};
  const dirFit = (id, deg) => {
    const name = dname(deg);
    if (!name) return "no";
    const launch = launchOf(id);
    if ((launch.good || []).includes(name)) return "good";
    if ((launch.ok || []).includes(name)) return "ok";
    return "no";
  };
  const windFit = w => {
    const launch = launchOf(w.site);
    if (w.wind == null || launch.windMin == null || launch.windMax == null) return "ok";
    if (w.wind < launch.windMin) return "low";
    if (w.wind > launch.windMax) return "high";
    return "ok";
  };
  const sunFor = (site, date) => (site.sun || []).find(s => s.date === date) || null;
  const hourRec = (id, date, hour) => {
    const site = SITES[id];
    if (!site) return null;
    return (site.hours || []).find(h => h.date === date && h.hour === hour) || null;
  };
  const sites = () => {
    let ids = ORDER.slice();
    if (S.view === "region") ids = ids.filter(id => SITES[id].region === D.regionView);
    if (S.view === "favoriter") ids = ids.filter(id => favs.has(id));
    return ids.sort((a, b) => (favs.has(b) ? 1 : 0) - (favs.has(a) ? 1 : 0));
  };
  const get = (id, date) => {
    let best = null;
    for (const w of WINDOWS) {
      if (w.site !== id || w.date !== date) continue;
      if (!best || w.score > best.score || (w.score === best.score && w.durationHours > best.durationHours)) best = w;
    }
    return best;
  };
  const gustSpread = w => w.gust == null ? 3 : w.gust - (w.wind || 0);
  const dayWins = date => {
    const ids = new Set(sites());
    const best = new Map();
    for (const w of WINDOWS) {
      if (w.date !== date || !ids.has(w.site)) continue;
      const cur = best.get(w.site);
      if (!cur || w.score > cur.score || (w.score === cur.score && w.durationHours > cur.durationHours)) best.set(w.site, w);
    }
    return [...best.values()].sort((a, b) => b.score - a.score || gustSpread(a) - gustSpread(b) || b.durationHours - a.durationHours);
  };
  const nextDay = date => DAYS.find(d => d.date > date && dayWins(d.date).length) || null;
  const confDots = n => [1, 2, 3].map(i => `<i class="${i <= n ? "on" : ""}"></i>`).join("");
  const arrow = (deg, size = 14, col = "currentColor") => `<svg width="${size}" height="${size}" viewBox="0 0 24 24" style="transform:rotate(${+deg || 0}deg)" aria-hidden="true"><path fill="${col}" d="M11.53 3l-.941 12.857L7 15l5.001 6L17 15l-3.587.857L12.471 3h-.941z"/></svg>`;

  function compass(id, deg, size) {
    const launch = launchOf(id);
    const c = size / 2, R = c - 2, r0 = R * 0.55;
    let svg = `<svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" aria-hidden="true">`;
    DIRS.forEach((d, i) => {
      const a0 = (i * 22.5 - 11.25 - 90) * Math.PI / 180, a1 = (i * 22.5 + 11.25 - 90) * Math.PI / 180;
      const p = (rr, a) => `${(c + rr * Math.cos(a)).toFixed(2)},${(c + rr * Math.sin(a)).toFixed(2)}`;
      const f = (launch.good || []).includes(d) ? "#3fae3d" : (launch.ok || []).includes(d) ? "#f2c14e" : "var(--surface2)";
      svg += `<path d="M${p(r0, a0)}L${p(R, a0)}A${R},${R} 0 0 1 ${p(R, a1)}L${p(r0, a1)}A${r0},${r0} 0 0 0 ${p(r0, a0)}Z" fill="${f}" stroke="var(--surface)" stroke-width="1.2"/>`;
    });
    [["N", 0], ["O", 90], ["S", 180], ["V", 270]].forEach(([t, a]) => {
      const rr = r0 - 10, ra = (a - 90) * Math.PI / 180;
      svg += `<text x="${c + rr * Math.cos(ra)}" y="${c + rr * Math.sin(ra) + 3.5}" text-anchor="middle" font-size="10" font-weight="600" fill="var(--t3)" font-family="Geist,sans-serif">${t}</text>`;
    });
    if (deg != null) {
      svg += `<g transform="rotate(${deg} ${c} ${c})"><line x1="${c}" y1="${c - R + 3}" x2="${c}" y2="${c + R * 0.18}" stroke="var(--t1)" stroke-width="2.6" stroke-linecap="round"/><path d="M${c - R * 0.15},${c + R * 0.05}L${c},${c + R * 0.34}L${c + R * 0.15},${c + R * 0.05}Z" fill="var(--t1)"/></g>`;
    }
    return svg + "</svg>";
  }

  function windowBar(w, site) {
    const sn = sunFor(site, w.date);
    const X = h => Math.max(0, Math.min(100, (h - 4) / 18 * 100));
    const rise = sn && sn.rise ? hm(sn.rise) : null;
    const set = sn && sn.set ? hm(sn.set) : null;
    const dl = rise != null && set != null ? `<span class="dl" style="left:${X(rise)}%;width:${Math.max(0, X(set) - X(rise))}%"></span>` : "";
    const title = sn && sn.rise ? `Dagsljus ${sn.rise}–${sn.set}` : "Dagsljus okänt";
    return `<div class="wbar" title="${esc(title)}">${dl}<span class="fw" style="left:${X(w.fromHour)}%;width:${Math.max(3, X(w.toHour + 1) - X(w.fromHour))}%;background:${SC[w.score] || SC[1]}"></span></div>`;
  }

  function summary(ws, day) {
    if (!ws.length) {
      const next = nextDay(day.date);
      return next
        ? `Ingen av platserna uppfyller vind- och riktningskraven. Närmaste flygbara dag: <b>${esc(next.weekdayLong)} ${next.dayOfMonth} ${esc(next.month)}</b>.`
        : "Ingen av platserna uppfyller vind- och riktningskraven de kommande dagarna.";
    }
    const b = ws[0];
    const site = SITES[b.site];
    const name = dname(b.direction);
    const gust = b.gust == null ? "byar saknas" : `byar ${fmt(b.gust)} m/s`;
    const favWins = ws.filter(w => favs.has(w.site));
    let text = `<b>${esc(site.name)}</b>, betyg <b>${b.score}</b>, fönster ${pad(b.fromHour)}–${pad(b.toHour)}. Vind <b>${fmt(b.wind)} m/s</b>`;
    text += name ? ` från ${SV[name]} (${Math.round(b.direction)}°)` : "";
    text += `, ${gust}. ${ws.length} ${ws.length === 1 ? "plats flygbar" : "platser flygbara"}.`;
    text += favWins.length
      ? ` Favoriter: ${favWins.map(w => `<b>${esc(SITES[w.site].name)} ${w.score}</b>`).join(", ")}.`
      : " Inga favoriter flygbara.";
    if (day.leadDays > 0 && ws.every(w => w.gust == null)) text += " Byar saknas så här långt fram.";
    return text + ` <span class="badge ex">AI-sammanfattning · kommande</span>`;
  }

  function renderHero() {
    const day = dayBy(S.day);
    if (!day) return;
    const ws = dayWins(S.day);
    const b = ws[0];
    document.getElementById("heroDay").textContent = `${day.today ? "Idag · " : ""}${day.weekdayLong} ${day.dayOfMonth} ${day.month}`;
    document.getElementById("heroConf").innerHTML = confDots(day.confidence);
    document.getElementById("heroConfT").textContent = day.confidenceLabel;
    const title = document.getElementById("heroTitle");
    if (!b) {
      title.textContent = `Inget flygbart fönster ${day.weekdayLong}`;
    } else {
      title.innerHTML = `<span class="sc" style="background:${SC[b.score]}">${b.score}</span> ${esc(SITES[b.site].name)} <span class="muted" style="font-weight:500">${pad(b.fromHour)}–${pad(b.toHour)}</span>`;
    }
    document.getElementById("heroSum").innerHTML = summary(ws, day);
    const fw = ws.filter(w => favs.has(w.site)).length;
    const visible = sites().length;
    document.getElementById("kpis").innerHTML = [
      ["Platser", `${ws.length}<small> / ${visible}</small>`],
      ["Favoriter", `${fw}<small> / ${favs.size}</small>`],
      ["Bästa fönster", b ? `${b.durationHours}<small> h</small>` : "–"]
    ].map(([l, v]) => `<div class="card kpi"><div class="cap">${l}</div><div class="v num">${v}</div></div>`).join("");
  }

  function renderScrub() {
    document.getElementById("scrub").innerHTML = DAYS.map(d => {
      const ws = dayWins(d.date);
      const top = ws.length ? ws[0].score : 0;
      const dim = S.view === "helg" && !d.weekend;
      return `<button type="button" class="dchip ${d.weekend ? "weekend" : ""}" aria-pressed="${S.day === d.date}" data-day="${d.date}" style="${dim ? "opacity:.35" : ""}">
        <div class="w">${d.today ? "Idag" : esc(d.weekday)}</div><div class="d num">${d.dayOfMonth}</div>
        <div class="s" style="${top ? `background:${SC[top]}` : ""}"></div>
        <div class="n num">${ws.length ? `${top} · ${ws.length} st` : "–"}</div>
        <div style="margin-top:3px"><span class="conf" title="${esc(d.confidenceLabel)} · beräknad">${confDots(d.confidence)}</span></div></button>`;
    }).join("");
  }

  function renderMap() {
    const d = S.day;
    const day = dayBy(d);
    const top = dayWins(d)[0];
    let svg = `<svg viewBox="2 150 135 296" role="img" aria-label="Karta över platserna"><path class="land" d="${SWE}"/>`;
    sites().forEach(id => {
      const p = SITES[id];
      if (p.latitude == null || p.longitude == null) return;
      const w = get(id, d);
      const n = NUDGE[id] || [0, 0];
      const x = (p.longitude - X0) * K * SS + n[0];
      const y = (Y1 - p.latitude) * SS + n[1];
      const L = LBL[id] || ["start", 8, 2.5];
      const sel = S.sel.site === id;
      const showLabel = sel || favs.has(id) || (top && top.site === id);
      const tip = w
        ? `${p.name} · ${w.score}/10 · ${pad(w.fromHour)}–${pad(w.toHour)} · ${dname(w.direction) ? SV[dname(w.direction)] : "–"} ${fmt(w.wind)} m/s`
        : `${p.name} · inget fönster`;
      svg += `<g class="pin ${w ? "" : "none"} ${sel ? "sel" : ""}" data-open="${id}|${w ? d : ""}" data-tip="${esc(tip)}">
        ${sel ? `<circle class="ring" cx="${x}" cy="${y}" r="7.6"/>` : ""}
        <circle cx="${x}" cy="${y}" r="${w ? 5.4 : 3.4}" ${w ? `fill="${SC[w.score]}"` : ""}/>
        ${w ? `<text class="cnt" x="${x}" y="${y + 2.1}" text-anchor="middle">${w.score}</text>` : ""}
        ${showLabel ? `<text x="${x + L[1]}" y="${y + L[2]}" text-anchor="${L[0]}">${favs.has(id) ? "★ " : ""}${esc(p.name)}</text>` : ""}
      </g>`;
    });
    document.getElementById("map").innerHTML = svg + "</svg>";
    document.getElementById("mapDay").textContent = day ? `${day.weekday} ${day.dayOfMonth} ${day.month}` : "";
  }

  function renderRows() {
    const day = dayBy(S.day);
    const ws = dayWins(S.day);
    document.getElementById("listTitle").textContent = day ? `Flygbart ${day.weekday} ${day.dayOfMonth} ${day.month}` : "Flygbart";
    const next = nextDay(S.day);
    document.getElementById("rows").innerHTML = ws.length ? ws.map(w => {
      const s = SITES[w.site];
      const fit = dirFit(w.site, w.direction);
      const wf = windFit(w);
      const name = dname(w.direction);
      return `<button type="button" class="row" data-open="${w.site}|${w.date}" aria-current="${S.sel.site === w.site && S.sel.date === w.date}">
        <span class="chip num" style="background:${SC[w.score]};${LIGHT.has(w.score) ? "color:#1f2a10" : ""}">${w.score}</span>
        <span style="min-width:0"><span class="rname">${esc(s.name)}${favs.has(w.site) ? '<span class="star">★</span>' : ""}</span>
          <span class="rsub num">${pad(w.fromHour)}–${pad(w.toHour)} · ${esc(s.regionName)}${fit !== "good" ? ` · <span class="meh">${fit === "ok" ? "snett" : "fel riktning"}</span>` : ""}${wf !== "ok" ? ` · <span class="meh">${wf === "high" ? "starkt" : "svagt"}</span>` : ""}</span>
          ${windowBar(w, s)}</span>
        <span><span class="wind num">${w.direction == null ? "" : arrow(w.direction)}${fmt(w.wind)}${w.gust != null ? `<span class="g">(${fmt(w.gust)})</span>` : ""}</span><div class="wdir">${name ? SV[name] : "–"} · m/s</div></span>
      </button>`;
    }).join("") : `<div style="padding:28px 14px;text-align:center;color:var(--t3)">Inget flygbart fönster ${day ? esc(day.weekdayLong) : ""}.${next ? `<br><button type="button" class="badge" style="margin-top:8px" data-day="${next.date}">Gå till ${esc(next.weekday)} ${next.dayOfMonth} →</button>` : ""}</div>`;
  }

  function cellFor(id, date) {
    const w = get(id, date);
    if (!w) return "<td></td>";
    const day = dayBy(date);
    const name = dname(w.direction);
    const tip = `${SITES[id].name} · ${day ? day.weekday : ""} ${day ? day.dayOfMonth : ""} · ${pad(w.fromHour)}–${pad(w.toHour)} · ${w.score}/10 · ${name ? SV[name] : "–"} ${fmt(w.wind)}${w.gust != null ? ` (${fmt(w.gust)})` : ""} m/s · ${fmt(w.temp)}° · molnbas ${w.cloudBase == null ? "–" : "≈" + w.cloudBase + " m, beräknad"}`;
    let bg, txt, lt = false;
    if (S.metric === "score") { bg = SC[w.score]; txt = w.score; lt = LIGHT.has(w.score); }
    else if (S.metric === "wind") {
      bg = w.wind == null ? "var(--surface2)" : windCol(w.wind);
      lt = true;
      txt = `${fmt(w.wind)}${w.gust != null ? `<span class="sub">${fmt(w.gust)}</span>` : ""}${w.direction == null ? "" : arrow(w.direction, 11, "#16202a")}`;
    } else if (S.metric === "temp") {
      const t = w.temp;
      if (t == null) return `<td data-open="${id}|${date}"></td>`;
      bg = t <= 0 ? "#9cc3f5" : t <= 4 ? "#c4dcf7" : t <= 8 ? "#f6e3b5" : t <= 10 ? "#f7c98f" : "#f3a46f";
      lt = true;
      txt = `${fmt(t)}°`;
    } else {
      const c = w.cloudBase;
      if (c == null) return `<td data-open="${id}|${date}" data-tip="${esc(tip)}">–</td>`;
      bg = c < 200 ? "#d4d4d8" : c < 400 ? "#bcd7f2" : c < 700 ? "#8fbdf0" : "#5a9ae8";
      lt = c < 700;
      txt = c;
    }
    return `<td class="v num ${lt ? "lt" : ""}" style="background:${bg}" data-open="${id}|${date}" data-tip="${esc(tip)}">${txt}</td>`;
  }

  function renderMatrix() {
    let html = `<thead><tr><th></th>${DAYS.map(d => `<th class="${d.date === S.day ? "sel" : ""} ${d.weekend ? "we" : ""}">${d.today ? "idag" : esc(d.weekday)}<br><span class="num">${d.dayOfMonth}</span></th>`).join("")}</tr></thead><tbody>`;
    sites().forEach(id => {
      html += `<tr><th>${favs.has(id) ? '<span class="star">★</span> ' : ""}${esc(SITES[id].name)}</th>${DAYS.map(d => cellFor(id, d.date)).join("")}</tr>`;
    });
    document.getElementById("mx").innerHTML = html + "</tbody>";
    const lg = document.getElementById("legend");
    if (S.metric === "score") lg.innerHTML = `Betyg <span class="ramp">${Object.values(SC).map(c => `<i style="background:${c}"></i>`).join("")}</span> 1–10 <span class="spacer"></span> Tomt = inget flygbart fönster`;
    else if (S.metric === "wind") lg.innerHTML = `Medelvind m/s <span class="ramp">${WR.map(([, c]) => `<i style="background:${c}"></i>`).join("")}</span> 0 → 12+ · liten siffra = byar när källan har dem`;
    else if (S.metric === "temp") lg.innerHTML = "Temperatur i fönstret, °C";
    else lg.innerHTML = "Molnbas ≈ (temp − daggpunkt) × 125 m över start · beräknad. Tomt tal saknas.";
  }

  function renderFavs() {
    const ids = [...favs].filter(id => SITES[id]);
    const html = ids.map(id => {
      const upcoming = WINDOWS.filter(w => w.site === id && w.date >= S.day).sort((a, b) => a.date.localeCompare(b.date) || b.score - a.score);
      const n = upcoming[0] || null;
      const s = SITES[id];
      const day = n ? dayBy(n.date) : null;
      return `<button type="button" class="fav" data-open="${id}|${n ? n.date : ""}">
        <span><span class="rname">${esc(s.name)}</span><span class="rsub">${n && day ? `Nästa: <b style="color:var(--t1);font-weight:600">${esc(day.weekday)} ${day.dayOfMonth} ${esc(day.month)} ${pad(n.fromHour)}–${pad(n.toHour)}</b> · ${n.score}/10` : "Inget fönster kommande dagar"}</span></span>
        <span>${n && n.direction != null ? `<span class="wind num">${arrow(n.direction)}${fmt(n.wind)}</span>` : ""}</span>
        <span class="spark">${DAYS.map(d => { const w = get(id, d.date); return `<i class="${d.date === S.day ? "sel" : ""}" style="${w ? `background:${SC[w.score]}` : ""}"></i>`; }).join("")}</span>
        <span class="spark-l">${DAYS.map(d => `<span>${esc(d.weekday.slice(0, 1).toUpperCase())}</span>`).join("")}</span>
      </button>`;
    }).join("");
    document.getElementById("favs").innerHTML = html || `<div style="padding:16px;color:var(--t3)">Inga favoriter – tryck ☆ på en plats.</div>`;
  }

  function renderSidebar() {
    const item = id => {
      const w = get(id, S.day);
      return `<button type="button" class="sb-item" data-open="${id}|${w ? S.day : ""}" aria-current="${S.sel.site === id}">
        <span class="dot" style="${w ? `background:${SC[w.score]}` : ""}"></span>${esc(SITES[id].name)}<span class="r num muted">${w ? w.score : "–"}</span></button>`;
    };
    document.getElementById("sbFav").innerHTML = [...favs].filter(id => SITES[id]).map(item).join("");
    document.getElementById("sbAll").innerHTML = ORDER.filter(id => !favs.has(id)).map(item).join("");
    const day = dayBy(S.day);
    document.getElementById("sbDay").textContent = day ? `${day.weekday} ${day.dayOfMonth}` : "";
    document.getElementById("cntVecka").textContent = WINDOWS.length;
    document.getElementById("cntHelg").textContent = WINDOWS.filter(w => { const d = dayBy(w.date); return d && d.weekend; }).length;
    document.getElementById("cntFav").textContent = favs.size;
    document.querySelectorAll("[data-view]").forEach(b => b.setAttribute("aria-current", b.dataset.view === S.view));
  }

  function hourColumns(id, date) {
    const cols = [];
    for (let h = 4; h <= 22; h++) cols.push({h, rec: hourRec(id, date, h)});
    return cols;
  }

  function renderInspector() {
    const el = document.getElementById("inspector");
    const id = S.sel.site;
    const s = id && SITES[id];
    if (!s) { el.innerHTML = ""; return; }
    const launch = launchOf(id);
    const dirs = (launch.good || []).map(d => SV[d] || d).join(", ");
    const head = `<div class="ins-h"><button type="button" class="iconbtn back" data-close aria-label="Tillbaka"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 18l-6-6 6-6"/></svg></button>
      <div style="min-width:0;flex:1"><h2>${esc(s.name)}</h2><div class="sub">${esc(s.regionName)} · ${s.altitude} m ö.h.${dirs ? ` · start ${esc(dirs)}` : ""}</div></div>
      <button type="button" class="iconbtn" data-fav="${id}" aria-pressed="${favs.has(id)}" title="Favorit" style="color:${favs.has(id) ? "#e5a50a" : "var(--t3)"}">${favs.has(id) ? "★" : "☆"}</button></div>`;
    let w = S.sel.date ? get(id, S.sel.date) : null;
    if (!w) w = WINDOWS.find(x => x.site === id && x.date >= S.day) || WINDOWS.find(x => x.site === id);
    if (!w) {
      el.innerHTML = head + `<div class="ins-b"><div class="card card-b muted">Inget flygbart fönster de kommande 10 dagarna.</div></div>`;
      return;
    }
    S.sel.date = w.date;
    const day = dayBy(w.date);
    const sn = sunFor(s, w.date);
    const fit = dirFit(id, w.direction);
    const wf = windFit(w);
    const name = dname(w.direction);
    const byDate = new Map();
    WINDOWS.filter(x => x.site === id).forEach(x => {
      const cur = byDate.get(x.date);
      if (!cur || x.score > cur.score) byDate.set(x.date, x);
    });
    const tabs = [...byDate.values()].sort((a, b) => a.date.localeCompare(b.date));
    const cols = hourColumns(id, w.date);
    const inWindow = h => h >= w.fromHour && h <= w.toHour;
    const daylight = h => {
      const rec = hourRec(id, w.date, h);
      if (rec) return rec.daylight;
      if (!sn || !sn.rise || !sn.set) return false;
      const t = h * 60, [rh, rm] = sn.rise.split(":").map(Number), [sh, sm] = sn.set.split(":").map(Number);
      return t >= rh * 60 + rm && t < sh * 60 + sm;
    };
    const repeated = cols.some(c => c.rec && c.rec.repeated);
    const outside = sn && sn.rise && sn.set && (w.fromHour < hm(sn.rise) - 0.02 || w.toHour >= hm(sn.set));
    const live = (D.sources || []).find(src => src.status === "live");
    const coming = (D.sources || []).filter(src => src.status !== "live");
    const gustLine = w.gust != null
      ? `Byar ${fmt(w.gust)}${launch.gustMax != null ? ` / max ${fmt(launch.gustMax)} m/s` : " m/s"}`
      : "Byar saknas";
    const ideal = launch.windMin != null && launch.windMax != null ? `${fmt(launch.windMin)}–${fmt(launch.windMax)}` : "–";
    el.innerHTML = head + `<div class="ins-b">
      <div class="daytabs">${tabs.map(o => {
        const od = dayBy(o.date);
        return `<button type="button" data-open="${id}|${o.date}" aria-pressed="${o.date === w.date}"><i style="background:${SC[o.score]}">${o.score}</i>${od ? esc(od.weekday) : ""} ${od ? od.dayOfMonth : ""}</button>`;
      }).join("")}</div>
      <div class="card card-b">
        <div class="big"><div class="score num" style="background:${SC[w.score]}">${w.score}</div>
          <div style="flex:1;min-width:0"><div class="lbl">${WORD(w.score)}</div>
          <div class="muted num" style="font-size:12.5px">${day ? esc(day.weekdayLong) : ""} ${day ? day.dayOfMonth : ""} ${day ? esc(day.month) : ""} · fönster ${pad(w.fromHour)}–${pad(w.toHour)}</div>
          <div class="muted" style="font-size:12px;margin-top:3px" title="Beräknad ur prognoslängd, inte modellspridning"><span class="conf">${day ? confDots(day.confidence) : ""}</span> ${day ? esc(day.confidenceLabel) : ""}</div></div></div>
        <div style="margin-top:12px">${windowBar(w, s)}<div class="axis num"><span>04</span><span>☀ ${sn && sn.rise ? esc(sn.rise) + "–" + esc(sn.set) : "–"}</span><span>22</span></div></div>
        ${outside ? `<div class="note">Fönstret ${pad(w.fromHour)}–${pad(w.toHour)} sträcker sig utanför dagsljus (${esc(sn.rise)}–${esc(sn.set)}).</div>` : ""}
      </div>
      <div class="card"><div class="card-h"><span class="cap">Vind mot start</span></div><div class="card-b">
        <div class="cmp">${compass(id, w.direction, 124)}<div class="cmp-t">
          <div class="num" style="font-size:24px;font-weight:650;letter-spacing:-.02em">${fmt(w.wind)}${w.gust != null ? `<span class="muted" style="font-size:15px;font-weight:500"> (${fmt(w.gust)})</span>` : ""} <span class="muted" style="font-size:12px;font-weight:500">m/s</span></div>
          <div>Från ${name ? SV[name] : "–"}${w.direction != null ? ` · ${Math.round(w.direction)}°` : ""}</div>
          <div class="${fit === "good" ? "ok" : "meh"}">${fit === "good" ? "✓ Rakt på start" : fit === "ok" ? "⚠ Snett mot start" : "✕ Fel riktning"}</div>
          <div class="${wf === "ok" ? "ok" : "meh"}">${wf === "ok" ? "✓ Lagom styrka" : `⚠ ${wf === "high" ? "Över" : "Under"} ideal`} <span class="muted">(${ideal})</span></div>
          <div class="muted" style="font-size:12px">${gustLine}</div>
        </div></div>
        <div class="key"><span><i style="background:#3fae3d"></i>Bra</span><span><i style="background:#f2c14e"></i>Godkänd</span><span><i style="background:var(--surface2);border:1px solid var(--line2)"></i>Ej flygbar</span><span>Pil = vindens väg</span></div>
      </div></div>
      <div class="card"><div class="card-h"><span class="cap">Timme för timme</span></div><div class="card-b"><div class="ht"><table class="num">
        <tr class="h"><th>Tid</th>${cols.map(c => `<td class="${daylight(c.h) ? "day" : ""} ${inWindow(c.h) ? "fw" : ""} ${c.rec && c.rec.repeated ? "rep" : ""}" ${c.rec && c.rec.repeated ? 'title="Upprepad från föregående tidssteg"' : ""}>${pad(c.h)}</td>`).join("")}</tr>
        <tr class="r"><th>Betyg</th>${cols.map(c => {
          const sc = c.rec && c.rec.score;
          return `<td>${sc ? `<span style="background:${SC[sc]}${LIGHT.has(sc) ? ";color:#1f2a10" : ""}">${sc}</span>` : `<span style="background:var(--surface2);color:var(--t4)">–</span>`}</td>`;
        }).join("")}</tr>
        <tr><th>Vind</th>${cols.map(c => windTd(c.rec && c.rec.wind, inWindow(c.h))).join("")}</tr>
        <tr><th>Byar</th>${cols.map(c => windTd(c.rec && c.rec.gust, inWindow(c.h))).join("")}</tr>
        <tr><th>Riktning</th>${cols.map(c => {
          const dir = c.rec && c.rec.direction;
          if (dir == null) return `<td class="${inWindow(c.h) ? "" : "out"}">–</td>`;
          const f = dirFit(id, dir);
          return `<td class="${inWindow(c.h) ? "" : "out"}">${arrow(dir, 14, f === "good" ? "var(--good)" : f === "ok" ? "#d39b00" : "var(--t3)")}</td>`;
        }).join("")}</tr>
        <tr><th>Temp</th>${cols.map(c => `<td class="${inWindow(c.h) ? "" : "out"}" style="font-weight:500">${c.rec && c.rec.temp != null ? fmt(c.rec.temp) + "°" : "–"}</td>`).join("")}</tr>
      </table></div>
      <div class="note">Timmar ${pad(w.fromHour)}–${pad(w.toHour)} ingår i fönstret. Gult = dagsljus. Färgskala för vind som Windguru/Windy. Saknade värden visas som –.${repeated ? " Kursiva timmar är upprepade från föregående tidssteg, samma sätt som betygsmotorn fyller luckor." : ""}</div></div></div>
      <div class="card"><div class="card-h"><span class="cap">Förhållanden i fönstret</span></div><div class="card-b"><div class="stats num">
        <div class="stat"><div class="cap">Temp</div><div class="v">${fmt(w.temp)}°</div></div>
        <div class="stat"><div class="cap">Daggpunkt</div><div class="v">${fmt(w.dew)}°</div></div>
        <div class="stat"><div class="cap">Molnbas ≈</div><div class="v">${w.cloudBase == null ? "–" : w.cloudBase}<small> m</small></div></div>
        <div class="stat"><div class="cap">Nederbörd</div><div class="v">${fmt(w.precipMin)}–${fmt(w.precipMax)}<small> mm</small></div></div>
        <div class="stat"><div class="cap">Regnrisk</div><div class="v">${fmt(w.precipProb)}<small> %</small></div></div>
        <div class="stat"><div class="cap">Tryck</div><div class="v">${fmt(w.pressure)}<small> hPa</small></div></div>
      </div><div class="note">Luftfuktighet ${fmt(w.humidity)} %. ${w.cloudBaseCalculated ? "Molnbas är beräknad ur temp − daggpunkt (≈125 m/°C) över startplatsen." : "Molnbas kunde inte beräknas."}</div></div></div>
      <div class="card"><div class="card-h"><span class="cap">Källor</span><span class="badge ex">Övriga källor · kommande</span></div><div class="card-b">
        <div class="srcs">${(D.sources || []).map(src => src.status === "live"
          ? `<span class="src on"><span class="badge live">live</span>${esc(src.name)}</span>`
          : `<span class="src off">${esc(src.id === "holfuy" && s.holfuy ? src.name + " #" + s.holfuy : src.name)} · kommande</span>`).join("")}</div>
        <table class="st num"><thead><tr><th>Källa</th><th>Vind</th><th>Byar</th><th>Riktn.</th><th>Betyg</th></tr></thead><tbody>
          <tr><td>${esc(live ? live.name : D.activeSource)}</td><td>${fmt(w.wind)}</td><td>${fmt(w.gust)}</td><td>${name ? SV[name] : "–"}</td><td>${w.score}</td></tr>
          ${coming.map(src => `<tr><td>${esc(src.name)}</td><td class="coming">kommande</td><td class="coming">kommande</td><td class="coming">kommande</td><td class="coming">kommande</td></tr>`).join("")}
        </tbody></table>
        <div class="note">Fler prognoskällor kan kopplas in senare. Kommande källor har inga värden.</div>
      </div></div>
    </div>`;
  }

  function windTd(v, on) {
    if (v == null) return `<td class="${on ? "" : "out"}">–</td>`;
    return `<td class="wc ${on ? "" : "out"}" style="background:${windCol(v)}">${fmt(v)}</td>`;
  }

  function renderChrome() {
    const live = (D.sources || []).find(s => s.status === "live");
    const liveName = live ? live.name : (D.activeSource || "");
    const titles = {vecka: "Vecka", helg: "Helg", region: D.regionName || "Region", favoriter: "Favoriter"};
    document.getElementById("tbTitle").textContent = titles[S.view] || "Vecka";
    document.getElementById("tbRange").textContent = `${D.rangeLabel || ""} · ${ORDER.length} platser`;
    document.getElementById("tbSource").innerHTML = `Källa: <b style="color:var(--t1);font-weight:600">${esc(liveName)}</b>`;
    document.getElementById("mUpdated").textContent = `● ${D.updatedTime || ""}`;
    document.getElementById("regionLabel").textContent = D.regionName || "Region";
    document.getElementById("sourceBadges").innerHTML = (D.sources || []).map(s => s.status === "live"
      ? `<span class="badge live">● ${esc(s.name)}</span>`
      : `<span class="badge">${esc(s.name)}</span>`).join("");
    document.getElementById("updatedLine").innerHTML = `Uppdaterad ${esc(D.updatedTime)} · ${esc(D.updatedDate)}<br>En tjänst från <a href="https://hangflyg.se" style="color:inherit">Stockholms Hängflygklubb</a>`;
    const yr = (D.sources || []).find(s => s.id === "yr");
    document.getElementById("foot").innerHTML = `Väderdata från <a href="${esc((yr && yr.url) || "https://www.yr.no")}">${esc(liveName)}</a>${yr && yr.license ? ` (${esc(yr.license)})` : ""}, hämtad ${esc(D.updatedDate)} ${esc(D.updatedTime)}. Flygfönster är trimmade till lokal soluppgång och solnedgång. Molnbas är beräknad ur temperatur minus daggpunkt. Säkerhet är en prognoslängdsindikator, inte modellspridning.<br>SMHI, Open-Meteo och Holfuy är markerade <b>kommande</b> och har inga värden.`;
  }

  function renderAll() {
    renderChrome();
    renderHero();
    renderScrub();
    renderMap();
    renderRows();
    renderMatrix();
    renderFavs();
    renderSidebar();
    renderInspector();
  }

  const isDesk = () => matchMedia("(min-width:1100px)").matches;
  function open(site, date) {
    if (!SITES[site]) return;
    S.sel = {site, date: date || null};
    renderInspector();
    renderRows();
    renderMap();
    renderSidebar();
    if (!isDesk()) {
      const panel = document.getElementById("inspector");
      panel.classList.add("open");
      panel.scrollTop = 0;
    }
  }
  function setDay(date) {
    if (!dayBy(date)) return;
    S.day = date;
    renderAll();
  }
  function setView(view) {
    S.view = view;
    if (view === "helg") {
      const current = dayBy(S.day);
      if (!current || !current.weekend) {
        const weekend = DAYS.find(d => d.weekend);
        if (weekend) S.day = weekend.date;
      }
    }
    const visible = sites();
    if (S.sel.site && !visible.includes(S.sel.site) && visible.length) {
      const wins = dayWins(S.day);
      S.sel = wins.length ? {site: wins[0].site, date: wins[0].date} : {site: visible[0], date: S.day};
    }
    renderAll();
  }

  document.addEventListener("click", e => {
    const t = e.target.closest("[data-open],[data-day],[data-fav],[data-close],[data-metric],[data-view],[data-cmdk],[data-theme-toggle],[data-go],#cmdkBg");
    if (!t) return;
    if (t.dataset.themeToggle !== undefined) {
      const root = document.documentElement;
      root.dataset.theme = root.dataset.theme === "dark" ? "light" : "dark";
      localStorage.setItem("smhy-theme", root.dataset.theme);
      return;
    }
    if (!D) return;
    if (t.id === "cmdkBg") { if (e.target === t) closeCmdk(); return; }
    if (t.dataset.open !== undefined) {
      const [site, date] = t.dataset.open.split("|");
      open(site, date);
      closeCmdk();
      return;
    }
    if (t.dataset.day) { setDay(t.dataset.day); return; }
    if (t.dataset.fav) {
      const id = t.dataset.fav;
      favs.has(id) ? favs.delete(id) : favs.add(id);
      localStorage.setItem("smhy2-favs", JSON.stringify([...favs]));
      renderAll();
      return;
    }
    if (t.dataset.close !== undefined) { document.getElementById("inspector").classList.remove("open"); return; }
    if (t.dataset.metric) {
      S.metric = t.dataset.metric;
      document.querySelectorAll("#seg button").forEach(b => b.setAttribute("aria-pressed", String(b === t)));
      renderMatrix();
      return;
    }
    if (t.dataset.view) { setView(t.dataset.view); return; }
    if (t.dataset.cmdk !== undefined) { openCmdk(); return; }
    if (t.dataset.go) {
      document.querySelectorAll(".bnav button").forEach(b => b.setAttribute("aria-current", String(b === t)));
      if (t.dataset.go === "top") scrollTo({top: 0, behavior: "smooth"});
      else document.getElementById(t.dataset.go).scrollIntoView({behavior: "smooth", block: "start"});
    }
  });

  const tip = document.getElementById("tip");
  addEventListener("scroll", () => tip.classList.remove("on"), true);
  document.addEventListener("mousemove", e => {
    const t = e.target.closest("[data-tip]");
    if (!t) { tip.classList.remove("on"); return; }
    tip.textContent = t.dataset.tip;
    tip.classList.add("on");
    tip.style.left = Math.min(innerWidth - 250, e.clientX + 12) + "px";
    tip.style.top = (e.clientY + 14) + "px";
  });

  function cmdItems(q) {
    q = q.toLowerCase();
    return ORDER.filter(id => (SITES[id].name + " " + (SITES[id].regionName || "")).toLowerCase().includes(q));
  }
  function renderCmdk() {
    const q = document.getElementById("cmdkIn").value;
    const its = cmdItems(q);
    cmdIdx = Math.min(cmdIdx, Math.max(0, its.length - 1));
    document.getElementById("cmdkList").innerHTML = its.map((id, i) => {
      const n = WINDOWS.find(w => w.site === id && w.date >= S.day);
      const day = n ? dayBy(n.date) : null;
      return `<li><button type="button" class="${i === cmdIdx ? "act" : ""}" data-open="${id}|${n ? n.date : ""}">${n ? `<span class="chip sm" style="background:${SC[n.score]}">${n.score}</span>` : `<span class="chip sm" style="background:var(--surface2);color:var(--t4)">–</span>`}<span><b style="font-weight:600">${esc(SITES[id].name)}</b> <span class="muted">${esc(SITES[id].regionName)}</span></span><span class="r num">${n && day ? `${esc(day.weekday)} ${day.dayOfMonth} ${pad(n.fromHour)}–${pad(n.toHour)}` : "inget fönster"}</span></button></li>`;
    }).join("") || `<li style="padding:12px;color:var(--t3)">Ingen plats hittades</li>`;
  }
  function openCmdk() {
    document.getElementById("cmdkBg").classList.add("on");
    const input = document.getElementById("cmdkIn");
    input.value = "";
    cmdIdx = 0;
    renderCmdk();
    setTimeout(() => input.focus(), 10);
  }
  function closeCmdk() { document.getElementById("cmdkBg").classList.remove("on"); }
  document.getElementById("cmdkIn").addEventListener("input", () => { cmdIdx = 0; if (D) renderCmdk(); });
  document.addEventListener("keydown", e => {
    tip.classList.remove("on");
    if (!D) return;
    const cm = document.getElementById("cmdkBg").classList.contains("on");
    if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") { e.preventDefault(); cm ? closeCmdk() : openCmdk(); return; }
    if (e.key === "Escape") { closeCmdk(); document.getElementById("inspector").classList.remove("open"); return; }
    if (cm) {
      const its = cmdItems(document.getElementById("cmdkIn").value);
      if (e.key === "ArrowDown") { cmdIdx = Math.min(its.length - 1, cmdIdx + 1); renderCmdk(); e.preventDefault(); }
      if (e.key === "ArrowUp") { cmdIdx = Math.max(0, cmdIdx - 1); renderCmdk(); e.preventDefault(); }
      if (e.key === "Enter" && its[cmdIdx]) {
        const id = its[cmdIdx];
        const n = WINDOWS.find(w => w.site === id && w.date >= S.day);
        open(id, n ? n.date : "");
        closeCmdk();
      }
      return;
    }
    if (e.target.tagName === "INPUT") return;
    const i = DAYS.findIndex(d => d.date === S.day);
    if (e.key === "ArrowRight" && i < DAYS.length - 1) setDay(DAYS[i + 1].date);
    if (e.key === "ArrowLeft" && i > 0) setDay(DAYS[i - 1].date);
  });

  function boot(data) {
    D = data;
    DAYS = data.days || [];
    SITES = Object.fromEntries((data.sites || []).map(s => [s.id, s]));
    ORDER = (data.sites || []).map(s => s.id);
    WINDOWS = [];
    (data.sites || []).forEach(site => (site.windows || []).forEach(w => WINDOWS.push(Object.assign({site: site.id}, w))));
    let stored = null;
    try { stored = JSON.parse(localStorage.getItem("smhy2-favs") || "null"); } catch (e) { stored = null; }
    const defaults = Array.isArray(stored) && stored.length ? stored : (data.defaultFavorites || []);
    favs = new Set(defaults.filter(id => SITES[id]));
    S.view = window.SMHY_VIEW || "vecka";
    S.day = DAYS.length ? DAYS[0].date : null;
    if (S.view === "helg") {
      const weekend = DAYS.find(d => d.weekend);
      if (weekend) S.day = weekend.date;
    }
    const wins = S.day ? dayWins(S.day) : [];
    const visible = sites();
    S.sel = wins.length ? {site: wins[0].site, date: wins[0].date} : {site: visible[0] || ORDER[0] || null, date: S.day};
    renderAll();
  }

  fetch("dashboard.json")
    .then(r => { if (!r.ok) throw new Error(String(r.status)); return r.json(); })
    .then(boot)
    .catch(() => {
      document.getElementById("heroTitle").textContent = "Kunde inte läsa dashboard.json";
      document.getElementById("heroSum").textContent = "Öppna sidan via en webbserver i samma katalog som rapporten.";
    });
})();
