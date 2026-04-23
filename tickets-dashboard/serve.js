#!/usr/bin/env node
/**
 * Tickets Dashboard — localhost:7575
 * Kanban | List | Roadmap | Analytics views with drag-drop + detail panel
 * Usage: node serve.js [--env /path/to/.env]
 */

const http    = require('http')
const https   = require('https')
const fs      = require('fs')
const path    = require('path')
const { execSync } = require('child_process')

const PORT = 7575

// --- Load .env ---
function loadEnv(filePath) {
  if (!filePath || !fs.existsSync(filePath)) return {}
  const env = {}
  fs.readFileSync(filePath, 'utf-8').split('\n').forEach(line => {
    const m = line.match(/^([^#=\s][^=]*)=(.*)$/)
    if (m) env[m[1].trim()] = m[2].trim()
  })
  return env
}

const argIdx = process.argv.indexOf('--env')
let envPath = argIdx !== -1 ? process.argv[argIdx + 1] : null
if (!envPath) {
  const candidates = [
    path.join(__dirname, '../../../workspaces/mbs/reels-downloader/server-layer/.env'),
    path.join(process.cwd(), 'server-layer/.env'),
    path.join(process.cwd(), '.env'),
  ]
  envPath = candidates.find(p => fs.existsSync(p)) || null
}

const env = loadEnv(envPath)
const SUPABASE_URL = env.SUPABASE_URL || process.env.SUPABASE_URL || ''
const SUPABASE_KEY = env.SUPABASE_ANON_KEY || process.env.SUPABASE_ANON_KEY || ''
const SERVICE_KEY  = env.SUPABASE_SERVICE_ROLE_KEY || process.env.SUPABASE_SERVICE_ROLE_KEY || SUPABASE_KEY
const TABLE        = 'product_tickets'
const PROJECT_NAME = env.PROJECT_NAME || new URL(SUPABASE_URL || 'http://unknown').hostname.split('.')[0]

console.log(`🎫 Tickets Dashboard`)
console.log(`   Project:  ${PROJECT_NAME}`)
console.log(`   Supabase: ${SUPABASE_URL || '⚠️  not configured'}`)
console.log(`   Server:   http://localhost:${PORT}`)
console.log(``)

// --- Views ---
const kanban  = require('./views/kanban')
const list    = require('./views/list')
const roadmap = require('./views/roadmap')
const analytics = require('./views/analytics')
const panel   = require('./views/detail-panel')
const tmpl    = require('./shared/page-template')

// --- HTML page ---
function buildPage(activeView) {
  const body = `
  <div class="header">
    <div class="header-brand">
      <span class="logo">🎫</span>
      <span class="title">Tickets</span>
      <span class="product">${PROJECT_NAME}</span>
    </div>
    <div class="header-stats">
      <div class="header-stat total"><div class="val" id="st-total">—</div><div class="lbl">Total</div></div>
      <div class="header-stat open"><div class="val" id="st-open">—</div><div class="lbl">Open</div></div>
      <div class="header-stat resolved"><div class="val" id="st-resolved">—</div><div class="lbl">Done</div></div>
    </div>
    <input class="header-search" id="search-input" placeholder="Search..." oninput="onSearch()">
    <button class="refresh-btn" onclick="loadTickets()">↻ Refresh</button>
  </div>

  <div class="view-tabs">
    <button class="view-tab ${activeView==='kanban'?'active':''}" onclick="switchView('kanban')">🗂 Kanban</button>
    <button class="view-tab ${activeView==='list'?'active':''}" onclick="switchView('list')">📋 List</button>
    <button class="view-tab ${activeView==='roadmap'?'active':''}" onclick="switchView('roadmap')">🗺️ Roadmap</button>
    <button class="view-tab ${activeView==='analytics'?'active':''}" onclick="switchView('analytics')">📊 Analytics</button>
  </div>

  <div id="main-content"></div>

  <div class="panel-overlay" id="panel-overlay" onclick="closeDetail()"></div>
  <div class="detail-panel" id="detail-panel"></div>
  `

  const scripts = `
  // Shared state
  let allTickets = []
  let currentView = '${activeView}'
  let searchQuery = ''

  function switchView(v) {
    currentView = v
    document.querySelectorAll('.view-tab').forEach(t => t.classList.toggle('active', t.textContent.toLowerCase().includes(v.substring(0,4))))
    renderView()
    history.replaceState(null,'','/?view=' + v)
  }

  function onSearch() {
    searchQuery = document.getElementById('search-input').value.toLowerCase()
    renderView()
  }

  function filtered() {
    if (!searchQuery) return allTickets
    return allTickets.filter(t =>
      (t.title||'').toLowerCase().includes(searchQuery) ||
      (t.description||'').toLowerCase().includes(searchQuery)
    )
  }

  function renderView() {
    const tickets = filtered()
    const el = document.getElementById('main-content')
    if (currentView === 'kanban')    el.innerHTML = kanbanRender(tickets)
    else if (currentView === 'list') el.innerHTML = listRender(tickets)
    else if (currentView === 'roadmap') el.innerHTML = roadmapRender(tickets)
    else if (currentView === 'analytics') el.innerHTML = analyticsRender(tickets)
  }

  async function loadTickets() {
    const btn = document.querySelector('.refresh-btn')
    if (btn) { btn.textContent = '↻ ...'; btn.disabled = true }
    try {
      const r = await fetch('/api/tickets')
      allTickets = await r.json()
      // Update header stats
      document.getElementById('st-total').textContent = allTickets.length
      const open = allTickets.filter(t => ['pending','in_review','in_progress','planned'].includes(t.status)).length
      document.getElementById('st-open').textContent = open
      const done = allTickets.filter(t => ['resolved','completed'].includes(t.status)).length
      document.getElementById('st-resolved').textContent = done
      renderView()
    } catch(e) {
      document.getElementById('main-content').innerHTML = '<div style="text-align:center;padding:60px;color:var(--danger)">⚠️ ' + e.message + '</div>'
    } finally {
      if (btn) { btn.textContent = '↻ Refresh'; btn.disabled = false }
    }
  }

  // Inline view renderers (server pre-baked as JS functions)
  ${kanban.scripts}
  ${panel.scripts}

  // Boot
  loadTickets()
  // Auto-refresh every 60s
  setInterval(loadTickets, 60000)
  `

  return tmpl({
    title: `Tickets — ${PROJECT_NAME}`,
    body: body + kanban.css + panel.css + list.css + roadmap.css + analytics.css,
    scripts
  })
}

// --- Supabase helpers ---
function supabaseFetch(path, method = 'GET', body = null) {
  return new Promise((resolve, reject) => {
    const u = new URL(SUPABASE_URL + path)
    const bodyBuf = body ? Buffer.from(JSON.stringify(body)) : null
    const opts = {
      method,
      hostname: u.hostname,
      path: u.pathname + u.search,
      headers: {
        'apikey': SERVICE_KEY,
        'Authorization': `Bearer ${SERVICE_KEY}`,
        'Content-Type': 'application/json',
        'Prefer': method === 'PATCH' ? 'return=minimal' : undefined,
        ...(bodyBuf ? { 'Content-Length': bodyBuf.length } : {}),
      }
    }
    // clean undefined headers
    Object.keys(opts.headers).forEach(k => opts.headers[k] === undefined && delete opts.headers[k])
    const req = https.request(opts, r => {
      let data = ''
      r.on('data', d => data += d)
      r.on('end', () => {
        try { resolve(data ? JSON.parse(data) : {}) }
        catch(_) { resolve(data) }
      })
    })
    req.on('error', reject)
    if (bodyBuf) req.write(bodyBuf)
    req.end()
  })
}

// --- Server ---
const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://localhost:${PORT}`)
  const activeView = url.searchParams.get('view') || 'kanban'

  // GET /api/tickets
  if (req.method === 'GET' && url.pathname === '/api/tickets') {
    try {
      const data = await supabaseFetch('/rest/v1/product_tickets?order=upvotes.desc,created_at.desc&select=*')
      // Client-side rendering needs the view functions — inject them
      res.writeHead(200, { 'Content-Type': 'application/json' })
      res.end(JSON.stringify(Array.isArray(data) ? data : []))
    } catch(e) {
      res.writeHead(500); res.end(JSON.stringify({ error: e.message }))
    }
    return
  }

  // GET /api/views/:name — return rendered HTML fragment + scripts for client
  if (req.method === 'GET' && url.pathname.startsWith('/api/views/')) {
    const viewName = url.pathname.split('/').pop()
    const views = { kanban, list, roadmap, analytics }
    const v = views[viewName]
    if (!v) { res.writeHead(404); res.end('not found'); return }
    // Fetch tickets and render server-side
    try {
      const data = await supabaseFetch('/rest/v1/product_tickets?order=upvotes.desc,created_at.desc&select=*')
      const tickets = Array.isArray(data) ? data : []
      res.writeHead(200, { 'Content-Type': 'application/json' })
      res.end(JSON.stringify({ html: v.render(tickets), scripts: v.scripts }))
    } catch(e) {
      res.writeHead(500); res.end(JSON.stringify({ error: e.message }))
    }
    return
  }

  // PATCH /api/tickets/:id
  if (req.method === 'PATCH' && url.pathname.startsWith('/api/tickets/')) {
    const id = url.pathname.split('/').pop()
    let body = ''
    req.on('data', d => body += d)
    req.on('end', async () => {
      try {
        await supabaseFetch(`/rest/v1/product_tickets?id=eq.${id}`, 'PATCH', JSON.parse(body))
        res.writeHead(200); res.end(JSON.stringify({ ok: true }))
      } catch(e) {
        res.writeHead(500); res.end(JSON.stringify({ error: e.message }))
      }
    })
    return
  }

  // Serve main HTML (with inline view renderers)
  if (req.method === 'GET' && (url.pathname === '/' || url.pathname === '/index.html')) {
    // Fetch tickets server-side for initial render
    let tickets = []
    try { tickets = await supabaseFetch('/rest/v1/product_tickets?order=upvotes.desc,created_at.desc&select=*') } catch(_) {}
    if (!Array.isArray(tickets)) tickets = []

    // Inject server-side rendered views as JS functions the client calls
    const views = { kanban, list, roadmap, analytics }
    const clientViewFns = Object.entries(views).map(([name, v]) => {
      return `function ${name}Render(tickets) { ${injectViewRender(v)} }`
    }).join('\n\n')

    function injectViewRender(v) {
      // Convert server render() to a client-callable function
      // We serialize the logic inline
      const src = v.render.toString()
      // Return the body of render()
      const match = src.match(/\{([\s\S]*)\}$/)
      return match ? match[1] : 'return ""'
    }

    const html = buildPageWithData(activeView, tickets, clientViewFns)
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' })
    res.end(html)
    return
  }

  res.writeHead(404); res.end()
})

function buildPageWithData(activeView, tickets, clientViewFns) {
  const body = `
  <div class="header">
    <div class="header-brand">
      <span class="logo">🎫</span>
      <span class="title">Tickets</span>
      <span class="product">${PROJECT_NAME}</span>
    </div>
    <div class="header-stats">
      <div class="header-stat total"><div class="val" id="st-total">${tickets.length}</div><div class="lbl">Total</div></div>
      <div class="header-stat open"><div class="val" id="st-open">${tickets.filter(t=>['pending','in_review','in_progress','planned'].includes(t.status)).length}</div><div class="lbl">Open</div></div>
      <div class="header-stat resolved"><div class="val" id="st-resolved">${tickets.filter(t=>['resolved','completed'].includes(t.status)).length}</div><div class="lbl">Done</div></div>
    </div>
    <input class="header-search" id="search-input" placeholder="Search tickets..." oninput="onSearch()">
    <button class="refresh-btn" onclick="loadTickets()">↻ Refresh</button>
  </div>

  <div class="view-tabs">
    <button class="view-tab ${activeView==='kanban'?'active':''}" onclick="switchView('kanban')">🗂 Kanban</button>
    <button class="view-tab ${activeView==='list'?'active':''}" onclick="switchView('list')">📋 List</button>
    <button class="view-tab ${activeView==='roadmap'?'active':''}" onclick="switchView('roadmap')">🗺️ Roadmap</button>
    <button class="view-tab ${activeView==='analytics'?'active':''}" onclick="switchView('analytics')">📊 Analytics</button>
  </div>

  <div id="main-content"></div>
  <div class="panel-overlay" id="panel-overlay" onclick="closeDetail()"></div>
  <div class="detail-panel" id="detail-panel"></div>
  `

  const scripts = `
  let allTickets = ${JSON.stringify(tickets)};
  let currentView = '${activeView}';
  let searchQuery = '';

  function switchView(v) {
    currentView = v;
    document.querySelectorAll('.view-tab').forEach(t => {
      const names = { kanban:'kanban', list:'list', roadmap:'roadmap', analytics:'analytics' };
      t.classList.toggle('active', t.textContent.toLowerCase().includes(v.substring(0,4)));
    });
    renderView();
    history.replaceState(null,'','/?view='+v);
  }

  function onSearch() {
    searchQuery = document.getElementById('search-input').value.toLowerCase();
    renderView();
  }

  function filtered() {
    if (!searchQuery) return allTickets;
    return allTickets.filter(t =>
      (t.title||'').toLowerCase().includes(searchQuery) ||
      (t.description||'').toLowerCase().includes(searchQuery)
    );
  }

  function renderView() {
    const tickets = filtered();
    const el = document.getElementById('main-content');
    if (currentView === 'kanban') el.innerHTML = kanbanRender(tickets);
    else if (currentView === 'list') el.innerHTML = listRender(tickets);
    else if (currentView === 'roadmap') el.innerHTML = roadmapRender(tickets);
    else if (currentView === 'analytics') el.innerHTML = analyticsRender(tickets);
  }

  async function loadTickets() {
    const btn = document.querySelector('.refresh-btn');
    if (btn) { btn.textContent = '↻ ...'; btn.disabled = true; }
    try {
      const r = await fetch('/api/tickets');
      allTickets = await r.json();
      document.getElementById('st-total').textContent = allTickets.length;
      const open = allTickets.filter(t => ['pending','in_review','in_progress','planned'].includes(t.status)).length;
      document.getElementById('st-open').textContent = open;
      const done = allTickets.filter(t => ['resolved','completed'].includes(t.status)).length;
      document.getElementById('st-resolved').textContent = done;
      renderView();
    } catch(e) {
      document.getElementById('main-content').innerHTML = '<div style="text-align:center;padding:60px;color:var(--danger)">⚠️ '+e.message+'</div>';
    } finally {
      if (btn) { btn.textContent = '↻ Refresh'; btn.disabled = false; }
    }
  }

  // --- Kanban render ---
  function kanbanRender(tickets) {
    const COLS = [
      {id:'pending',label:'Pending',color:'#8b90b0'},
      {id:'in_review',label:'In Review',color:'#f5a623'},
      {id:'planned',label:'Planned',color:'#00d4aa'},
      {id:'in_progress',label:'In Progress',color:'#6c63ff'},
      {id:'resolved',label:'Done',color:'#00c896'},
      {id:'closed',label:'Closed',color:'#ff5a5f'},
    ];
    const byStatus = {};
    COLS.forEach(c => byStatus[c.id] = []);
    tickets.forEach(t => { const s = t.status||'pending'; if(byStatus[s]) byStatus[s].push(t); else byStatus.pending.push(t); });
    return '<div class="kanban-wrap"><div class="kanban-board">' + COLS.map(col => {
      const cards = byStatus[col.id];
      const cardHtml = cards.length === 0 ? '<div class="kanban-empty">Drop here</div>' :
        cards.map(t => {
          const title = t.title && t.title.startsWith('http') ? '🔗 ' + t.title.split('/').slice(-2).join('/') : (t.title||'(no title)');
          return '<div class="kanban-card'+(t.admin_response?' has-reply':'')+'" draggable="true" data-id="'+t.id+'" onclick="openDetail(this.dataset.id)" ondragstart="onDragStart(event,this.dataset.id)" ondragend="onDragEnd(event)"><div class="card-emoji">'+(TYPE_EMOJI[t.ticket_type]||'📋')+'</div><div class="card-title">'+escHtml(title)+'</div><div class="card-meta"><span class="card-votes">⬆ '+(t.upvotes||0)+'</span><span class="card-prio '+(t.priority||'low')+'">'+(t.priority||'low')+'</span></div></div>';
        }).join('');
      return '<div class="kanban-col"><div class="kanban-col-header" style="background:'+col.color+'22;color:'+col.color+';border-top:2px solid '+col.color+';">'+col.label+'<span class="count">'+cards.length+'</span></div><div class="kanban-cards" data-status="'+col.id+'" ondragover="onDragOver(event)" ondragleave="onDragLeave(event)" ondrop="onDrop(event,event.currentTarget.dataset.status)">'+cardHtml+'</div></div>';
    }).join('') + '</div></div>';
  }

  // --- List render ---
  function listRender(tickets) {
    if (!tickets.length) return '<div style="text-align:center;padding:60px;color:var(--text3)">No tickets found</div>';
    return '<div class="list-wrap"><table class="list-table"><thead><tr><th></th><th>Title</th><th>Status</th><th>Priority</th><th>Votes</th><th>Category</th><th>Created</th><th>Response</th></tr></thead><tbody>'+
      tickets.map(t => {
        const title = t.title && t.title.startsWith('http') ? '🔗 '+t.title.split('/').slice(-2).join('/') : (t.title||'(no title)');
        return '<tr data-id="'+t.id+'" onclick="openDetail(this.dataset.id)"><td>'+(TYPE_EMOJI[t.ticket_type]||'📋')+'</td><td class="col-title"><span class="title-text">'+escHtml(title)+'</span></td><td><span class="badge status-'+(t.status||'pending')+'">'+(STATUS_LABEL[t.status]||t.status)+'</span></td><td><span class="badge prio-'+(t.priority||'low')+'">'+(t.priority||'low')+'</span></td><td class="col-votes">⬆ '+(t.upvotes||0)+'</td><td style="color:var(--text3);font-size:12px">'+escHtml(t.category||'')+'</td><td style="color:var(--text3);font-size:12px;white-space:nowrap">'+fmt(t.created_at)+'</td><td>'+(t.admin_response?'<span class="has-reply">✅</span>':'<span style="color:var(--text3)">—</span>')+'</td></tr>';
      }).join('')+'</tbody></table></div>';
  }

  // --- Roadmap render ---
  function roadmapRender(tickets) {
    const shipped = tickets.filter(t=>['resolved','completed','closed'].includes(t.status)).sort((a,b)=>b.upvotes-a.upvotes);
    const inProg  = tickets.filter(t=>t.status==='in_progress').sort((a,b)=>b.upvotes-a.upvotes);
    const planned = tickets.filter(t=>['planned','in_review'].includes(t.status)).sort((a,b)=>b.upvotes-a.upvotes);
    const pending = tickets.filter(t=>t.status==='pending').sort((a,b)=>b.upvotes-a.upvotes).slice(0,5);
    function ri(t) {
      const title = t.title&&t.title.startsWith('http')?'🔗 '+t.title.split('/').slice(-2).join('/'):(t.title||'(no title)');
      return '<div class="roadmap-item" data-id="'+t.id+'" onclick="openDetail(this.dataset.id)"><span class="ri-emoji">'+(TYPE_EMOJI[t.ticket_type]||'📋')+'</span><span class="ri-title">'+escHtml(title)+'</span><span class="badge status-'+t.status+'">'+(STATUS_LABEL[t.status]||t.status)+'</span><span class="ri-votes">⬆ '+(t.upvotes||0)+'</span><span class="ri-date">'+fmt(t.updated_at||t.created_at)+'</span></div>';
    }
    function sec(icon, label, items, color) {
      if (!items.length) return '';
      return '<div class="roadmap-section"><div class="roadmap-section-title" style="color:'+color+'">'+icon+' '+label+' ('+items.length+')</div>'+items.map(ri).join('')+'</div>';
    }
    return '<div class="roadmap-wrap">'+sec('🔄','In Progress',inProg,'#6c63ff')+sec('📋','Planned',planned,'#00d4aa')+sec('💡','Top Requested',pending,'#f5a623')+sec('✅','Shipped',shipped,'#00c896')+'</div>';
  }

  // --- Analytics render ---
  function analyticsRender(tickets) {
    const total = tickets.length;
    if (!total) return '<div style="text-align:center;padding:60px;color:var(--text3)">No data yet</div>';
    const statuses = {pending:0,in_review:0,planned:0,in_progress:0,resolved:0,completed:0,closed:0};
    const types = {feature_request:0,bug_report:0,contact_support:0,roadmap_item:0};
    let totalVotes=0,withReply=0;
    tickets.forEach(t=>{if(statuses[t.status]!==undefined)statuses[t.status]++;if(types[t.ticket_type]!==undefined)types[t.ticket_type]++;totalVotes+=(t.upvotes||0);if(t.admin_response)withReply++;});
    const open=(statuses.pending+statuses.in_review+statuses.in_progress+statuses.planned);
    const rRate=total?Math.round(withReply/total*100):0;
    const topV=[...tickets].sort((a,b)=>(b.upvotes||0)-(a.upvotes||0)).slice(0,5);
    const maxV=topV[0]?.upvotes||1;
    const SC={'#8b90b0':'pending','#f5a623':'in_review','#00d4aa':'planned','#6c63ff':'in_progress','#00c896':'resolved','#00c896':'completed','#ff5a5f':'closed'};
    const SCOL={pending:'#8b90b0',in_review:'#f5a623',planned:'#00d4aa',in_progress:'#6c63ff',resolved:'#00c896',completed:'#00c896',closed:'#ff5a5f'};
    const TCOL={feature_request:'#6c63ff',bug_report:'#ff5a5f',contact_support:'#f5a623',roadmap_item:'#00d4aa'};
    function bar(label,count,max,color){const p=max?Math.round(count/max*100):0;return '<div class="bar-row"><span class="bar-label">'+label+'</span><div class="bar-track"><div class="bar-fill" style="width:'+p+'%;background:'+color+'"></div></div><span class="bar-val">'+count+'</span></div>';}
    const sBars=Object.entries(statuses).filter(([,n])=>n>0).map(([s,n])=>bar(STATUS_LABEL[s]||s,n,total,SCOL[s]||'#6c63ff')).join('');
    const tBars=Object.entries(types).filter(([,n])=>n>0).map(([t,n])=>bar(t.replace('_',' '),n,total,TCOL[t]||'#6c63ff')).join('');
    const tvRows=topV.map((t,i)=>{const title=t.title&&t.title.startsWith('http')?'🔗 '+t.title.split('/').slice(-2).join('/'):(t.title||'(no title)');return '<div class="top-voted-item"><span class="rank">#'+(i+1)+'</span><span class="tv-title">'+escHtml(title)+'</span><div style="flex:1;height:4px;background:var(--surface2);border-radius:2px;overflow:hidden;margin:0 10px"><div style="width:'+Math.round((t.upvotes||0)/maxV*100)+'%;height:100%;background:var(--accent);border-radius:2px"></div></div><span class="tv-votes">⬆ '+(t.upvotes||0)+'</span></div>';}).join('');
    return '<div class="analytics-wrap"><div class="analytics-card"><h3>Overview</h3><div class="stat-grid"><div class="stat-box"><div class="sv" style="color:var(--accent)">'+total+'</div><div class="sl">Total</div></div><div class="stat-box"><div class="sv" style="color:var(--warn)">'+open+'</div><div class="sl">Open</div></div><div class="stat-box"><div class="sv" style="color:var(--success)">'+(statuses.resolved+statuses.completed)+'</div><div class="sl">Resolved</div></div><div class="stat-box"><div class="sv" style="color:var(--accent2)">'+rRate+'%</div><div class="sl">Reply Rate</div></div></div></div><div class="analytics-card"><h3>By Type</h3>'+tBars+'</div><div class="analytics-card"><h3>By Status</h3>'+sBars+'</div><div class="analytics-card"><h3>Totals</h3><div class="stat-grid"><div class="stat-box"><div class="sv" style="color:var(--accent)">'+totalVotes+'</div><div class="sl">Total Votes</div></div><div class="stat-box"><div class="sv" style="color:var(--success)">'+withReply+'</div><div class="sl">Replied</div></div><div class="stat-box"><div class="sv" style="color:var(--text2)">'+(total-withReply)+'</div><div class="sl">No Reply</div></div><div class="stat-box"><div class="sv" style="color:var(--accent2)">'+Math.round(totalVotes/(total||1)*10)/10+'</div><div class="sl">Avg Votes</div></div></div></div><div class="analytics-card full"><h3>Top Voted</h3>'+tvRows+'</div></div>';
  }

  ${kanban.scripts}
  ${panel.scripts}

  // Initial render
  renderView();
  // Auto-refresh every 60s
  setInterval(loadTickets, 60000);
  `

  return require('./shared/page-template')({
    title: `Tickets — ${PROJECT_NAME}`,
    body: body + kanban.css + panel.css + `
      ${list.css}
      <style>
      .list-wrap { padding: 20px 24px 40px; }
      .list-table { width: 100%; border-collapse: collapse; }
      .list-table th { text-align: left; font-size: 11px; text-transform: uppercase; letter-spacing: .5px; color: var(--text3); padding: 10px 14px; border-bottom: 1px solid var(--border); user-select: none; white-space: nowrap; }
      .list-table td { padding: 12px 14px; border-bottom: 1px solid var(--border); font-size: 13px; vertical-align: middle; }
      .list-table tr:hover td { background: var(--surface2); cursor: pointer; }
      .list-table .col-title { max-width: 300px; }
      .list-table .title-text { font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 300px; display: block; }
      .list-table .col-votes { text-align: center; color: var(--accent); font-weight: 700; }
      .has-reply { color: var(--success); font-size: 12px; }
      .roadmap-wrap { padding: 24px; max-width: 860px; }
      .roadmap-section { margin-bottom: 28px; }
      .roadmap-section-title { font-size: 13px; font-weight: 700; text-transform: uppercase; letter-spacing: .5px; margin-bottom: 12px; display: flex; align-items: center; gap: 8px; }
      .roadmap-section-title::after { content: ''; flex: 1; height: 1px; background: var(--border); }
      .roadmap-item { background: var(--surface); border: 1px solid var(--border); border-radius: 10px; padding: 14px 18px; margin-bottom: 8px; display: flex; align-items: center; gap: 14px; cursor: pointer; transition: border-color .2s; }
      .roadmap-item:hover { border-color: var(--accent); }
      .ri-emoji { font-size: 18px; }
      .ri-title { font-size: 14px; font-weight: 500; flex: 1; }
      .ri-votes { font-size: 13px; color: var(--accent); font-weight: 700; }
      .ri-date { font-size: 11px; color: var(--text3); }
      .analytics-wrap { padding: 24px; display: grid; grid-template-columns: 1fr 1fr; gap: 20px; max-width: 1000px; }
      .analytics-card { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; padding: 20px; }
      .analytics-card.full { grid-column: 1 / -1; }
      .analytics-card h3 { font-size: 13px; font-weight: 700; text-transform: uppercase; letter-spacing: .5px; color: var(--text2); margin-bottom: 16px; }
      .bar-row { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
      .bar-row .bar-label { font-size: 12px; color: var(--text2); width: 100px; flex-shrink: 0; }
      .bar-row .bar-track { flex: 1; height: 8px; background: var(--surface2); border-radius: 4px; overflow: hidden; }
      .bar-row .bar-fill { height: 100%; border-radius: 4px; }
      .bar-row .bar-val { font-size: 12px; color: var(--text2); width: 30px; text-align: right; }
      .top-voted-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px solid var(--border); }
      .top-voted-item:last-child { border-bottom: none; }
      .rank { font-size: 13px; font-weight: 700; color: var(--text3); width: 20px; }
      .tv-title { flex: 1; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
      .tv-votes { color: var(--accent); font-weight: 700; font-size: 13px; }
      .stat-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
      .stat-box { background: var(--surface2); border-radius: 8px; padding: 14px; text-align: center; }
      .stat-box .sv { font-size: 28px; font-weight: 800; }
      .stat-box .sl { font-size: 11px; color: var(--text2); margin-top: 2px; text-transform: uppercase; letter-spacing: .5px; }
      </style>
    `,
    scripts
  })
}

server.listen(PORT, () => {
  console.log(`✅ Dashboard ready → http://localhost:${PORT}`)
  try { execSync(`open http://localhost:${PORT}`) } catch(_) {}
})
process.on('SIGINT', () => { console.log('\n👋 Bye'); process.exit(0) })
