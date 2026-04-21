#!/usr/bin/env node
// =============================================================================
// Ticket Dashboard — localhost:7575
// Kanban board + detail panel + analytics for cmp-user-tickets
//
// Usage: node tickets-dashboard/serve.js
// Env:   SHARED_SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY (from .env)
// =============================================================================

const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.TICKETS_PORT || 7575;

// ── Load .env ────────────────────────────────────────────────────────────
function loadEnv() {
  const envPaths = [
    path.resolve(__dirname, '..', '.env'),
    path.resolve(__dirname, '..', '..', '.env'),
    path.resolve(__dirname, '..', '..', '..', '.env'),
    path.resolve(__dirname, '..', '..', '..', '..', '.env'),
    path.resolve(__dirname, '..', 'server-layer', '.env'),
  ];
  for (const p of envPaths) {
    if (fs.existsSync(p)) {
      const lines = fs.readFileSync(p, 'utf8').split('\n');
      for (const line of lines) {
        const match = line.match(/^([^#=]+)=(.*)$/);
        if (match) process.env[match[1].trim()] = match[2].trim();
      }
      console.log(`Loaded env from: ${p}`);
      return;
    }
  }
  console.error('No .env found. Set SHARED_SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY');
  process.exit(1);
}
loadEnv();

const SUPABASE_URL = process.env.SHARED_SUPABASE_URL;
const SERVICE_KEY = process.env.SUPABASE_SERVICE_ROLE_KEY;

if (!SUPABASE_URL || !SERVICE_KEY) {
  console.error('Missing SHARED_SUPABASE_URL or SUPABASE_SERVICE_ROLE_KEY in .env');
  process.exit(1);
}

// ── Supabase API ─────────────────────────────────────────────────────────
async function supaFetch(endpoint, options = {}) {
  const url = `${SUPABASE_URL}/rest/v1/${endpoint}`;
  const res = await fetch(url, {
    ...options,
    headers: {
      'apikey': SERVICE_KEY,
      'Authorization': `Bearer ${SERVICE_KEY}`,
      'Content-Type': 'application/json',
      'Prefer': 'return=representation',
      ...options.headers,
    },
  });
  return res.json();
}

async function getTickets(productType) {
  const filter = productType && productType !== 'all' ? `&product_type=eq.${productType}` : '';
  return supaFetch(`user_tickets?order=upvotes.desc&select=*${filter}`);
}

async function getProductTypes() {
  const tickets = await supaFetch('user_tickets?select=product_type');
  return [...new Set(tickets.map(t => t.product_type))].sort();
}

async function updateTicket(id, data) {
  return supaFetch(`user_tickets?id=eq.${id}`, {
    method: 'PATCH',
    body: JSON.stringify(data),
  });
}

// ── HTML Rendering ───────────────────────────────────────────────────────
const STATUS_COLS = ['pending', 'in_review', 'planned', 'in_progress', 'resolved'];
const STATUS_LABELS = {
  pending: 'Pending', in_review: 'In Review', planned: 'Planned',
  in_progress: 'In Progress', resolved: 'Resolved', completed: 'Completed', closed: 'Closed'
};
const TYPE_EMOJI = { feature_request: '💡', bug_report: '🐛', contact_support: '📩', roadmap_item: '🗺️' };
const PRIORITY_COLORS = { low: '#22c55e', medium: '#eab308', high: '#ef4444', critical: '#a855f7' };

function renderPage(tickets, products, activeProduct) {
  const stats = {
    total: tickets.length,
    pending: tickets.filter(t => t.status === 'pending').length,
    in_progress: tickets.filter(t => t.status === 'in_progress').length,
    resolved: tickets.filter(t => ['resolved', 'completed'].includes(t.status)).length,
    needs_reply: tickets.filter(t => !t.admin_response && !t.is_private).length,
  };

  const kanbanCols = STATUS_COLS.map(status => {
    const col_tickets = tickets.filter(t => t.status === status && !t.is_private);
    return `
      <div class="kanban-col" data-status="${status}">
        <div class="col-header">
          <span>${STATUS_LABELS[status]}</span>
          <span class="badge">${col_tickets.length}</span>
        </div>
        <div class="col-cards" ondrop="drop(event,'${status}')" ondragover="event.preventDefault()">
          ${col_tickets.map(t => renderCard(t)).join('')}
        </div>
      </div>`;
  }).join('');

  const topVoted = [...tickets].filter(t => !t.is_private).sort((a, b) => b.upvotes - a.upvotes).slice(0, 5);

  return `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Ticket Dashboard</title>
<style>
  * { margin:0; padding:0; box-sizing:border-box; }
  body { font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif; background:#0f172a; color:#e2e8f0; }
  .header { background:#1e293b; padding:16px 24px; display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #334155; }
  .header h1 { font-size:20px; color:#f8fafc; }
  .stats { display:flex; gap:24px; }
  .stat { text-align:center; }
  .stat-val { font-size:24px; font-weight:700; color:#60a5fa; }
  .stat-label { font-size:11px; color:#94a3b8; text-transform:uppercase; }
  .tabs { background:#1e293b; padding:0 24px; display:flex; gap:4px; border-bottom:1px solid #334155; }
  .tab { padding:10px 20px; cursor:pointer; color:#94a3b8; border-bottom:2px solid transparent; font-size:14px; }
  .tab.active { color:#60a5fa; border-bottom-color:#60a5fa; }
  .tab:hover { color:#f8fafc; }
  .view { display:none; padding:20px 24px; }
  .view.active { display:block; }
  .kanban { display:flex; gap:16px; overflow-x:auto; min-height:calc(100vh - 160px); }
  .kanban-col { flex:1; min-width:220px; background:#1e293b; border-radius:12px; padding:12px; }
  .col-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; font-weight:600; font-size:14px; color:#cbd5e1; }
  .badge { background:#334155; padding:2px 8px; border-radius:10px; font-size:12px; }
  .col-cards { display:flex; flex-direction:column; gap:10px; min-height:100px; }
  .card { background:#0f172a; border:1px solid #334155; border-radius:10px; padding:14px; cursor:pointer; transition:border-color .15s; }
  .card:hover { border-color:#60a5fa; }
  .card[draggable=true] { cursor:grab; }
  .card-title { font-size:14px; font-weight:600; color:#f1f5f9; margin-bottom:6px; display:flex; gap:6px; align-items:center; }
  .card-desc { font-size:12px; color:#94a3b8; margin-bottom:8px; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden; }
  .card-meta { display:flex; gap:8px; align-items:center; flex-wrap:wrap; }
  .chip { padding:2px 8px; border-radius:6px; font-size:11px; font-weight:500; }
  .chip-priority { border:1px solid; }
  .chip-votes { background:#1e3a5f; color:#60a5fa; }
  .chip-replied { background:#166534; color:#4ade80; }
  .detail-overlay { display:none; position:fixed; top:0; right:0; bottom:0; width:480px; background:#1e293b; border-left:1px solid #334155; overflow-y:auto; z-index:100; box-shadow:-4px 0 20px rgba(0,0,0,.5); }
  .detail-overlay.open { display:block; }
  .detail-header { padding:16px 20px; border-bottom:1px solid #334155; display:flex; justify-content:space-between; align-items:center; }
  .detail-body { padding:20px; }
  .detail-body h3 { font-size:16px; margin-bottom:12px; color:#f8fafc; }
  .detail-body p { font-size:14px; color:#cbd5e1; line-height:1.6; margin-bottom:16px; }
  .detail-body label { font-size:12px; color:#94a3b8; display:block; margin-bottom:4px; text-transform:uppercase; }
  .detail-body select, .detail-body textarea { width:100%; padding:10px; border:1px solid #334155; border-radius:8px; background:#0f172a; color:#e2e8f0; font-size:14px; margin-bottom:12px; }
  .detail-body textarea { min-height:80px; resize:vertical; }
  .btn { padding:8px 16px; border-radius:8px; border:none; cursor:pointer; font-size:13px; font-weight:600; }
  .btn-primary { background:#3b82f6; color:#fff; }
  .btn-primary:hover { background:#2563eb; }
  .btn-close { background:#334155; color:#e2e8f0; }
  .btn-close:hover { background:#475569; }
  .btn-group { display:flex; gap:8px; margin-top:12px; }
  .analytics { display:grid; grid-template-columns:1fr 1fr; gap:20px; }
  .analytics-card { background:#1e293b; border-radius:12px; padding:20px; }
  .analytics-card h3 { font-size:14px; color:#94a3b8; margin-bottom:12px; }
  .bar-row { display:flex; align-items:center; gap:8px; margin-bottom:8px; }
  .bar-label { width:100px; font-size:13px; color:#cbd5e1; }
  .bar-fill { height:20px; border-radius:4px; background:#3b82f6; transition:width .3s; }
  .bar-val { font-size:12px; color:#94a3b8; }
  .roadmap-section { margin-bottom:24px; }
  .roadmap-section h3 { font-size:16px; color:#94a3b8; margin-bottom:12px; padding-bottom:8px; border-bottom:1px solid #334155; }
  .toast { position:fixed; bottom:20px; right:20px; background:#166534; color:#fff; padding:12px 20px; border-radius:8px; font-size:14px; display:none; z-index:200; }
</style>
</head>
<body>
  <div class="header">
    <div style="display:flex;align-items:center;gap:16px">
      <h1>🎫 Ticket Dashboard</h1>
      <select onchange="location.href='/?product='+this.value" style="background:#0f172a;color:#60a5fa;border:1px solid #334155;border-radius:8px;padding:6px 12px;font-size:14px;cursor:pointer">
        <option value="all" ${activeProduct === 'all' ? 'selected' : ''}>All Products</option>
        ${products.map(p => `<option value="${p}" ${activeProduct === p ? 'selected' : ''}>${p.replace(/_/g, ' ')}</option>`).join('')}
      </select>
    </div>
    <div class="stats">
      <div class="stat"><div class="stat-val">${stats.total}</div><div class="stat-label">Total</div></div>
      <div class="stat"><div class="stat-val">${stats.pending}</div><div class="stat-label">Pending</div></div>
      <div class="stat"><div class="stat-val">${stats.in_progress}</div><div class="stat-label">Active</div></div>
      <div class="stat"><div class="stat-val">${stats.resolved}</div><div class="stat-label">Resolved</div></div>
      <div class="stat"><div class="stat-val">${stats.needs_reply}</div><div class="stat-label">Needs Reply</div></div>
    </div>
  </div>

  <div class="tabs">
    <div class="tab active" onclick="switchView('kanban')">Kanban</div>
    <div class="tab" onclick="switchView('roadmap')">Roadmap</div>
    <div class="tab" onclick="switchView('analytics')">Analytics</div>
  </div>

  <div id="view-kanban" class="view active">
    <div class="kanban">${kanbanCols}</div>
  </div>

  <div id="view-roadmap" class="view">
    <div class="roadmap-section">
      <h3>🚧 In Progress</h3>
      ${tickets.filter(t => t.status === 'in_progress' && !t.is_private).map(t => renderCard(t)).join('') || '<p style="color:#64748b">No tickets in progress</p>'}
    </div>
    <div class="roadmap-section">
      <h3>📋 Planned</h3>
      ${tickets.filter(t => t.status === 'planned' && !t.is_private).map(t => renderCard(t)).join('') || '<p style="color:#64748b">No planned tickets</p>'}
    </div>
    <div class="roadmap-section">
      <h3>✅ Recently Shipped</h3>
      ${tickets.filter(t => ['resolved', 'completed'].includes(t.status) && !t.is_private).map(t => renderCard(t)).join('') || '<p style="color:#64748b">No shipped tickets</p>'}
    </div>
  </div>

  <div id="view-analytics" class="view">
    <div class="analytics">
      <div class="analytics-card">
        <h3>Top Voted</h3>
        ${topVoted.map(t => `<div class="bar-row">
          <span class="bar-label">${(t.title || '').slice(0, 20)}</span>
          <div class="bar-fill" style="width:${Math.min(t.upvotes * 15, 200)}px"></div>
          <span class="bar-val">⬆${t.upvotes}</span>
        </div>`).join('')}
      </div>
      <div class="analytics-card">
        <h3>By Status</h3>
        ${Object.entries(STATUS_LABELS).map(([k, v]) => {
          const count = tickets.filter(t => t.status === k).length;
          return count ? `<div class="bar-row">
            <span class="bar-label">${v}</span>
            <div class="bar-fill" style="width:${count * 30}px;background:${k === 'resolved' ? '#22c55e' : k === 'in_progress' ? '#eab308' : '#3b82f6'}"></div>
            <span class="bar-val">${count}</span>
          </div>` : '';
        }).join('')}
      </div>
      <div class="analytics-card">
        <h3>By Type</h3>
        ${['feature_request', 'bug_report', 'contact_support'].map(type => {
          const count = tickets.filter(t => t.ticket_type === type).length;
          return `<div class="bar-row">
            <span class="bar-label">${TYPE_EMOJI[type]} ${type.replace('_', ' ')}</span>
            <div class="bar-fill" style="width:${count * 40}px"></div>
            <span class="bar-val">${count}</span>
          </div>`;
        }).join('')}
      </div>
      <div class="analytics-card">
        <h3>Summary</h3>
        <p style="color:#cbd5e1;font-size:14px;line-height:1.8">
          Total tickets: <strong>${stats.total}</strong><br>
          Response rate: <strong>${stats.total ? Math.round((stats.total - stats.needs_reply) / stats.total * 100) : 0}%</strong><br>
          Needs reply: <strong style="color:${stats.needs_reply ? '#ef4444' : '#22c55e'}">${stats.needs_reply}</strong>
        </p>
      </div>
    </div>
  </div>

  <div id="detail-panel" class="detail-overlay">
    <div class="detail-header">
      <h2 style="font-size:16px" id="detail-title"></h2>
      <button class="btn btn-close" onclick="closeDetail()">✕</button>
    </div>
    <div class="detail-body">
      <p id="detail-desc"></p>
      <label>Status</label>
      <select id="detail-status" onchange="updateField('status',this.value)">
        ${Object.entries(STATUS_LABELS).map(([k, v]) => `<option value="${k}">${v}</option>`).join('')}
      </select>
      <label>Priority</label>
      <select id="detail-priority" onchange="updateField('priority',this.value)">
        <option value="low">🟢 Low</option>
        <option value="medium">🟡 Medium</option>
        <option value="high">🔴 High</option>
        <option value="critical">🟣 Critical</option>
      </select>
      <label>Admin Response</label>
      <textarea id="detail-response" placeholder="Type your reply..."></textarea>
      <div class="btn-group">
        <button class="btn btn-primary" onclick="sendReply()">Send Reply</button>
        <button class="btn btn-primary" style="background:#166534" onclick="updateField('status','resolved')">Resolve</button>
      </div>
      <div id="detail-resolution" style="margin-top:16px"></div>
      <div style="margin-top:16px">
        <label>Copy for Claude Code</label>
        <code id="detail-claude-cmd" style="display:block;background:#0f172a;padding:8px;border-radius:6px;font-size:12px;color:#60a5fa;cursor:pointer;word-break:break-all" onclick="navigator.clipboard.writeText(this.textContent)"></code>
      </div>
    </div>
  </div>

  <div id="toast" class="toast"></div>

<script>
const TICKETS = ${JSON.stringify(tickets)};
let currentTicketId = null;

function switchView(view) {
  document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  document.getElementById('view-' + view).classList.add('active');
  event.target.classList.add('active');
}

function openDetail(id) {
  const t = TICKETS.find(x => x.id === id);
  if (!t) return;
  currentTicketId = id;
  document.getElementById('detail-title').textContent = (({'feature_request':'💡','bug_report':'🐛','contact_support':'📩'}[t.ticket_type]||'📝') + ' ' + t.title);
  document.getElementById('detail-desc').textContent = t.description;
  document.getElementById('detail-status').value = t.status;
  document.getElementById('detail-priority').value = t.priority || 'medium';
  document.getElementById('detail-response').value = t.admin_response || '';
  document.getElementById('detail-resolution').innerHTML = t.resolution ? '<label>Resolution</label><p style="color:#4ade80">' + t.resolution + '</p>' : '';
  document.getElementById('detail-claude-cmd').textContent = '/tickets reply ' + id.slice(0, 8) + ' ""';
  document.getElementById('detail-panel').classList.add('open');
}

function closeDetail() {
  document.getElementById('detail-panel').classList.remove('open');
  currentTicketId = null;
}

async function updateField(field, value) {
  if (!currentTicketId) return;
  const body = { [field]: value };
  if (field === 'status' || field === 'admin_response') body.responded_at = new Date().toISOString();
  const res = await fetch('/api/update', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ id: currentTicketId, ...body })
  });
  if (res.ok) {
    showToast(field + ' updated!');
    setTimeout(() => location.reload(), 500);
  }
}

async function sendReply() {
  const msg = document.getElementById('detail-response').value;
  if (!msg || !currentTicketId) return;
  await updateField('admin_response', msg);
}

function drag(ev, id) { ev.dataTransfer.setData('text/plain', id); }
function drop(ev, status) {
  ev.preventDefault();
  const id = ev.dataTransfer.getData('text/plain');
  currentTicketId = id;
  updateField('status', status);
}

function showToast(msg) {
  const t = document.getElementById('toast');
  t.textContent = '✅ ' + msg;
  t.style.display = 'block';
  setTimeout(() => t.style.display = 'none', 2000);
}
</script>
</body>
</html>`;
}

function renderCard(t) {
  const emoji = TYPE_EMOJI[t.ticket_type] || '📝';
  const prioColor = PRIORITY_COLORS[t.priority] || '#eab308';
  return `<div class="card" draggable="true" ondragstart="drag(event,'${t.id}')" onclick="openDetail('${t.id}')">
    <div class="card-title">${emoji} ${(t.title || '').slice(0, 35)}${(t.title||'').length > 35 ? '...' : ''}</div>
    <div class="card-desc">${(t.description || '').slice(0, 80)}</div>
    <div class="card-meta">
      <span class="chip chip-votes">⬆${t.upvotes}</span>
      <span class="chip chip-priority" style="border-color:${prioColor};color:${prioColor}">${t.priority || 'med'}</span>
      ${t.admin_response ? '<span class="chip chip-replied">replied</span>' : ''}
    </div>
  </div>`;
}

// ── HTTP Server ──────────────────────────────────────────────────────────
const server = http.createServer(async (req, res) => {
  if (req.method === 'POST' && req.url === '/api/update') {
    let body = '';
    req.on('data', c => body += c);
    req.on('end', async () => {
      try {
        const data = JSON.parse(body);
        const { id, ...fields } = data;
        await updateTicket(id, fields);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ ok: true }));
      } catch (e) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: e.message }));
      }
    });
    return;
  }

  // Default: render dashboard
  try {
    const url = new URL(req.url, `http://localhost:${PORT}`);
    const activeProduct = url.searchParams.get('product') || 'all';
    const [tickets, products] = await Promise.all([getTickets(activeProduct), getProductTypes()]);
    const html = renderPage(tickets, products, activeProduct);
    res.writeHead(200, { 'Content-Type': 'text/html' });
    res.end(html);
  } catch (e) {
    res.writeHead(500, { 'Content-Type': 'text/plain' });
    res.end('Error: ' + e.message);
  }
});

server.listen(PORT, () => {
  console.log(`\n🎫 Ticket Dashboard running at http://localhost:${PORT}\n`);
});
