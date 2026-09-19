package com.localstream.app.server

object WebAssets {

    val INDEX_HTML = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>LocalStream Web Portal</title>
    <link rel="stylesheet" href="/style.css">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
</head>
<body>
    <div class="app-container">
        <!-- Header -->
        <header class="navbar">
            <div class="brand">
                <div class="logo-badge">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                        <polygon points="5 3 19 12 5 21 5 3"></polygon>
                    </svg>
                </div>
                <div class="brand-text">
                    <h1>LocalStream</h1>
                    <span class="status-pill"><span class="pulse"></span> Online</span>
                </div>
            </div>

            <div class="search-bar">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="11" cy="11" r="8"></circle>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                </svg>
                <input type="text" id="searchInput" placeholder="Search media files, videos, tracks...">
            </div>

            <div class="header-actions">
                <button class="icon-btn" id="refreshBtn" title="Refresh list">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M23 4v6h-6"></path>
                        <path d="M1 20v-6h6"></path>
                        <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
                    </svg>
                </button>
            </div>
        </header>

        <!-- Categories & Controls -->
        <div class="toolbar">
            <div class="category-chips" id="categoryChips">
                <button class="chip active" data-type="ALL">All Files</button>
                <button class="chip" data-type="VIDEO">🎬 Videos</button>
                <button class="chip" data-type="AUDIO">🎵 Music</button>
                <button class="chip" data-type="IMAGE">🖼️ Images</button>
                <button class="chip" data-type="DOCUMENT">📄 Documents</button>
            </div>
            <div class="meta-info">
                <span id="fileCountBadge">0 files available</span>
            </div>
        </div>

        <!-- Media Grid -->
        <main class="content-area">
            <div id="fileGrid" class="media-grid"></div>
            <div id="emptyState" class="empty-state" style="display: none;">
                <div class="empty-icon">📁</div>
                <h3>No Media Files Found</h3>
                <p>Ensure files are scanned and indexed in the LocalStream Android app.</p>
            </div>
        </main>

        <!-- Media Player Modal -->
        <div id="playerModal" class="modal">
            <div class="modal-content">
                <div class="modal-header">
                    <h3 id="modalTitle">Now Playing</h3>
                    <button class="close-btn" id="closeModalBtn">&times;</button>
                </div>
                <div class="player-wrapper" id="playerContainer">
                    <!-- Dynamic video/audio element injected here -->
                </div>
            </div>
        </div>
    </div>
    <script src="/app.js"></script>
</body>
</html>
""".trimIndent()

    val STYLE_CSS = """
:root {
    --bg-base: #090713;
    --bg-surface: #130f24;
    --bg-card: #1c1635;
    --bg-card-hover: #261f47;
    --primary: #a78bfa;
    --primary-glow: rgba(167, 139, 250, 0.25);
    --secondary: #8b5cf6;
    --accent-cyan: #38bdf8;
    --text-primary: #f8fafc;
    --text-secondary: #94a3b8;
    --border-subtle: #2d2452;
    --radius-lg: 16px;
    --radius-md: 10px;
}

* {
    box-sizing: border-box;
    margin: 0;
    padding: 0;
}

body {
    font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
    background-color: var(--bg-base);
    color: var(--text-primary);
    min-height: 100vh;
    overflow-x: hidden;
}

.app-container {
    max-width: 1300px;
    margin: 0 auto;
    padding: 20px 24px;
}

.navbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    padding: 16px 24px;
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-lg);
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.4);
    margin-bottom: 24px;
}

.brand {
    display: flex;
    align-items: center;
    gap: 14px;
}

.logo-badge {
    width: 44px;
    height: 44px;
    border-radius: 12px;
    background: linear-gradient(135deg, #7c3aed, #a855f7);
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    box-shadow: 0 0 20px var(--primary-glow);
}

.brand-text h1 {
    font-size: 1.25rem;
    font-weight: 700;
    letter-spacing: -0.5px;
    background: linear-gradient(90deg, #ffffff, #c4b5fd);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
}

.status-pill {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 0.75rem;
    font-weight: 600;
    color: #4ade80;
}

.pulse {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #4ade80;
    box-shadow: 0 0 8px #4ade80;
}

.search-bar {
    flex: 1;
    max-width: 500px;
    display: flex;
    align-items: center;
    gap: 10px;
    background: var(--bg-base);
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-md);
    padding: 10px 16px;
    transition: border-color 0.2s;
}

.search-bar:focus-within {
    border-color: var(--primary);
}

.search-bar svg {
    color: var(--text-secondary);
}

.search-bar input {
    background: transparent;
    border: none;
    outline: none;
    color: var(--text-primary);
    width: 100%;
    font-size: 0.95rem;
}

.icon-btn {
    background: var(--bg-card);
    border: 1px solid var(--border-subtle);
    color: var(--text-primary);
    padding: 10px;
    border-radius: var(--radius-md);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;
}

.icon-btn:hover {
    background: var(--bg-card-hover);
    border-color: var(--primary);
}

.toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 14px;
    margin-bottom: 24px;
}

.category-chips {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
}

.chip {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    color: var(--text-secondary);
    padding: 8px 16px;
    border-radius: 20px;
    font-size: 0.85rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
}

.chip.active, .chip:hover {
    background: var(--primary);
    color: #090713;
    border-color: var(--primary);
    box-shadow: 0 0 16px var(--primary-glow);
}

.meta-info {
    font-size: 0.85rem;
    color: var(--text-secondary);
}

.media-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 18px;
}

.file-card {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-lg);
    padding: 16px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    transition: transform 0.2s, border-color 0.2s, box-shadow 0.2s;
}

.file-card:hover {
    transform: translateY(-3px);
    border-color: var(--primary);
    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.5);
}

.card-top {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    margin-bottom: 14px;
}

.type-badge {
    width: 44px;
    height: 44px;
    border-radius: 10px;
    background: var(--bg-card);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 1.3rem;
    flex-shrink: 0;
}

.file-info {
    overflow: hidden;
}

.file-name {
    font-weight: 600;
    font-size: 0.95rem;
    color: var(--text-primary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    margin-bottom: 4px;
}

.file-meta {
    font-size: 0.8rem;
    color: var(--text-secondary);
}

.card-actions {
    display: flex;
    gap: 10px;
    margin-top: 8px;
}

.btn-play {
    flex: 1;
    background: linear-gradient(135deg, #8b5cf6, #7c3aed);
    color: white;
    border: none;
    border-radius: var(--radius-md);
    padding: 9px 14px;
    font-weight: 600;
    font-size: 0.85rem;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    transition: opacity 0.2s;
}

.btn-play:hover {
    opacity: 0.9;
}

.btn-download {
    background: var(--bg-card);
    color: var(--text-primary);
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-md);
    padding: 9px 14px;
    font-weight: 600;
    font-size: 0.85rem;
    cursor: pointer;
    text-decoration: none;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: border-color 0.2s;
}

.btn-download:hover {
    border-color: var(--primary);
}

.empty-state {
    text-align: center;
    padding: 60px 20px;
    background: var(--bg-surface);
    border-radius: var(--radius-lg);
    border: 1px dashed var(--border-subtle);
}

.empty-icon {
    font-size: 3rem;
    margin-bottom: 12px;
}

.empty-state h3 {
    margin-bottom: 6px;
    font-size: 1.2rem;
}

.empty-state p {
    color: var(--text-secondary);
    font-size: 0.9rem;
}

/* Modal */
.modal {
    display: none;
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background: rgba(4, 3, 10, 0.85);
    backdrop-filter: blur(8px);
    z-index: 1000;
    align-items: center;
    justify-content: center;
    padding: 20px;
}

.modal.active {
    display: flex;
}

.modal-content {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-lg);
    width: 100%;
    max-width: 860px;
    padding: 20px;
    box-shadow: 0 20px 50px rgba(0, 0, 0, 0.8);
}

.modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
}

.close-btn {
    background: transparent;
    border: none;
    color: var(--text-secondary);
    font-size: 1.8rem;
    cursor: pointer;
    line-height: 1;
}

.player-wrapper video, .player-wrapper audio {
    width: 100%;
    border-radius: var(--radius-md);
    background: #000;
    max-height: 70vh;
}
""".trimIndent()

    val APP_JS = """
let allFiles = [];
let currentCategory = 'ALL';

document.addEventListener('DOMContentLoaded', () => {
    fetchFiles();

    document.getElementById('searchInput').addEventListener('input', (e) => {
        filterFiles();
    });

    document.getElementById('refreshBtn').addEventListener('click', () => {
        fetchFiles();
    });

    document.querySelectorAll('.chip').forEach(chip => {
        chip.addEventListener('click', () => {
            document.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
            chip.classList.add('active');
            currentCategory = chip.dataset.type;
            filterFiles();
        });
    });

    document.getElementById('closeModalBtn').addEventListener('click', closeModal);
    document.getElementById('playerModal').addEventListener('click', (e) => {
        if (e.target.id === 'playerModal') closeModal();
    });
});

async function fetchFiles() {
    try {
        const response = await fetch('/api/files');
        if (!response.ok) throw new Error('Failed to fetch media');
        allFiles = await response.json();
        filterFiles();
    } catch (e) {
        console.error('Error fetching files:', e);
        document.getElementById('emptyState').style.display = 'block';
    }
}

function filterFiles() {
    const query = document.getElementById('searchInput').value.toLowerCase().trim();
    const filtered = allFiles.filter(file => {
        const matchesCategory = currentCategory === 'ALL' || file.mediaType === currentCategory;
        const matchesSearch = file.name.toLowerCase().includes(query) || (file.folder && file.folder.toLowerCase().includes(query));
        return matchesCategory && matchesSearch;
    });

    renderFiles(filtered);
}

function renderFiles(files) {
    const grid = document.getElementById('fileGrid');
    const emptyState = document.getElementById('emptyState');
    const countBadge = document.getElementById('fileCountBadge');

    grid.innerHTML = '';
    countBadge.textContent = files.length + ' files available';

    if (files.length === 0) {
        emptyState.style.display = 'block';
        return;
    }

    emptyState.style.display = 'none';

    files.forEach(file => {
        const card = document.createElement('div');
        card.className = 'file-card';

        const icon = getIconForType(file.mediaType);
        const canStream = file.mediaType === 'VIDEO' || file.mediaType === 'AUDIO' || file.mediaType === 'IMAGE';

        card.innerHTML = `
            <div class="card-top">
                <div class="type-badge">${'$'}{icon}</div>
                <div class="file-info">
                    <div class="file-name" title="${'$'}{escapeHtml(file.name)}">${'$'}{escapeHtml(file.name)}</div>
                    <div class="file-meta">${'$'}{file.formattedSize || ''} • ${'$'}{file.folder || 'Internal'}</div>
                </div>
            </div>
            <div class="card-actions">
                ${'$'}{canStream ? `<button class="btn-play" onclick="openPlayer(${'$'}{file.id}, '${'$'}{file.mediaType}', '${'$'}{encodeURIComponent(file.name)}')">Play / View</button>` : ''}
                <a class="btn-download" href="/api/download?id=${'$'}{file.id}" download="${'$'}{escapeHtml(file.name)}">Download</a>
            </div>
        `;
        grid.appendChild(card);
    });
}

function getIconForType(type) {
    switch (type) {
        case 'VIDEO': return '🎬';
        case 'AUDIO': return '🎵';
        case 'IMAGE': return '🖼️';
        case 'DOCUMENT': return '📄';
        default: return '📁';
    }
}

function escapeHtml(str) {
    return (str || '').replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function openPlayer(id, type, encodedName) {
    const modal = document.getElementById('playerModal');
    const container = document.getElementById('playerContainer');
    const title = document.getElementById('modalTitle');
    const name = decodeURIComponent(encodedName);
    title.textContent = name;

    const streamUrl = `/api/stream?id=` + id;

    if (type === 'VIDEO') {
        container.innerHTML = `<video controls autoplay playsinline src="${'$'}{streamUrl}"></video>`;
    } else if (type === 'AUDIO') {
        container.innerHTML = `<audio controls autoplay controlsList="nodownload" src="${'$'}{streamUrl}"></audio>`;
    } else if (type === 'IMAGE') {
        container.innerHTML = `<img src="${'$'}{streamUrl}" style="max-width:100%; max-height:70vh; border-radius:10px; display:block; margin:auto;" />`;
    }

    modal.classList.add('active');
}

function closeModal() {
    const modal = document.getElementById('playerModal');
    const container = document.getElementById('playerContainer');
    container.innerHTML = '';
    modal.classList.remove('active');
}
""".trimIndent()
}
