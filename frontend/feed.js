// feed.js — Feed page logic
if (!requireAuth()) throw new Error('Not authenticated');

let feedBeforeScore = 9999999999999;
let feedLoading = false;
let feedExhausted = false;
let currentFilter = 'all';
let myProfile = null;
let searchTimeout = null;

const topics = ['react', 'java', 'kafka', 'devops', 'python', 'systemdesign', 'ml', 'cloud', 'docker', 'rust'];

// ---- INIT ----
async function init() {
  await loadMyProfile();
  renderTopics();
  const params = new URLSearchParams(window.location.search);
  if (params.get('filter') === 'saved') {
    setFeedFilter('saved');
  } else {
    await loadFeed();
  }
  setupInfiniteScroll();
}

async function loadMyProfile() {
  try {
    myProfile = await api.getMyProfile(0, 1);
    const avatar = avatarUrl(myProfile.profilePictureUrl, myProfile.username);
    document.getElementById('navAvatar').src = avatar;
    document.getElementById('sidebarAvatar').src = avatar;
    document.getElementById('sidebarUsername').textContent = myProfile.username;
    document.getElementById('sidebarHandle').textContent = '@' + myProfile.username;
    localStorage.setItem('techilla_profile', JSON.stringify(myProfile));
  } catch {}
}

function renderTopics() {
  const container = document.getElementById('topicLinks');
  topics.slice(0, 6).forEach(t => {
    const btn = document.createElement('button');
    btn.className = 'sidebar-link';
    btn.innerHTML = `<span class="icon">#</span> ${t}`;
    btn.onclick = () => {
      document.querySelectorAll('.sidebar-link').forEach(l => l.classList.remove('active'));
      btn.classList.add('active');
    };
    container.appendChild(btn);
  });
}

// ---- FEED LOADING ----
async function loadFeed(append = false) {
  if (feedLoading || feedExhausted) return;
  feedLoading = true;
  if (!append) {
    document.getElementById('skeletonLoader').style.display = '';
    document.getElementById('feedContent').innerHTML = '';
    document.getElementById('feedContent').appendChild(document.getElementById('skeletonLoader'));
  } else {
    document.getElementById('feedLoader').style.display = 'flex';
  }

  try {
    let reels = [];
    if (currentFilter === 'saved') {
      const res = await api.getSaved(0, 20);
      reels = res.content || res || [];
    } else {
      // Build feed first if needed, then load
      try { await api.buildFeed(); } catch {}
      reels = await api.getFeed(feedBeforeScore, 10);
    }

    document.getElementById('skeletonLoader').style.display = 'none';
    document.getElementById('feedLoader').style.display = 'none';

    if (!reels || reels.length === 0) {
      if (!append) {
        document.getElementById('feedContent').innerHTML = `
          <div class="empty-state">
            <div class="empty-icon">🎬</div>
            <div class="empty-title">No reels yet</div>
            <div class="empty-text">Your feed is empty. Start following people or upload your first reel!</div>
          </div>`;
      } else {
        feedExhausted = true;
        document.getElementById('feedEnd').style.display = 'block';
      }
      return;
    }

    if (reels.length > 0) {
      const last = reels[reels.length - 1];
      feedBeforeScore = last.createdAt ? Number(last.createdAt) : feedBeforeScore - 1;
    }
    if (reels.length < 10) feedExhausted = true;

    const frag = document.createDocumentFragment();
    reels.forEach(r => {
      frag.appendChild(createReelCard(r));
      if (currentFilter !== 'saved') api.markSeen(r.reelId).catch(() => {});
    });

    if (!append) {
      document.getElementById('feedContent').innerHTML = '';
    }
    document.getElementById('feedContent').appendChild(frag);

    if (feedExhausted) document.getElementById('feedEnd').style.display = 'block';

  } catch (err) {
    document.getElementById('skeletonLoader').style.display = 'none';
    document.getElementById('feedLoader').style.display = 'none';
    document.getElementById('feedContent').innerHTML = `
      <div class="empty-state">
        <div class="empty-icon">⚠️</div>
        <div class="empty-title">Could not load feed</div>
        <div class="empty-text">${err.message || 'Please check your connection and try again.'}</div>
        <button class="btn btn-primary mt-2" onclick="refreshFeed()">Try Again</button>
      </div>`;
  } finally {
    feedLoading = false;
  }
}

function setupInfiniteScroll() {
  window.addEventListener('scroll', () => {
    const scrollBottom = document.documentElement.scrollTop + window.innerHeight;
    const docHeight = document.documentElement.scrollHeight;
    if (scrollBottom >= docHeight - 300 && !feedLoading && !feedExhausted) {
      loadFeed(true);
    }
  });
}

function setFeedFilter(filter) {
  currentFilter = filter;
  feedBeforeScore = 9999999999999;
  feedExhausted = false;

  document.querySelectorAll('#sidebarFeed, #sidebarTrending, #sidebarSaved').forEach(el => el.classList.remove('active'));
  if (filter === 'all') { document.getElementById('sidebarFeed').classList.add('active'); document.getElementById('feedTitle').textContent = 'For You'; }
  if (filter === 'trending') { document.getElementById('sidebarTrending').classList.add('active'); document.getElementById('feedTitle').textContent = 'Trending'; }
  if (filter === 'saved') { document.getElementById('sidebarSaved').classList.add('active'); document.getElementById('feedTitle').textContent = 'Saved Reels'; }

  document.getElementById('feedEnd').style.display = 'none';
  loadFeed(false);
}

async function refreshFeed() {
  feedBeforeScore = 9999999999999;
  feedExhausted = false;
  await loadFeed(false);
}

// ---- REEL CARD ----
function createReelCard(reel) {
  const card = document.createElement('div');
  card.className = 'reel-card fade-in';
  card.dataset.reelId = reel.reelId;

  const avatarSrc = avatarUrl(null, reel.username);
  const isMyReel = myProfile && (reel.username === myProfile.username);

  card.innerHTML = `
    <div class="reel-header">
      <img class="reel-avatar" src="${avatarSrc}" alt="${reel.username}" onclick="goToProfile('${reel.username}')" onerror="this.src='${avatarUrl(null, reel.username)}'" />
      <div class="reel-user-info">
        <span class="reel-username" onclick="goToProfile('${reel.username}')">${reel.username}</span>
        <div class="reel-time">${relativeTime(reel.createdAt)}</div>
      </div>
      ${isMyReel ? `<button class="reel-menu-btn" onclick="showReelMenu(event,'${reel.reelId}')">⋮</button>` : ''}
    </div>
    <div class="reel-video-container" onclick="handleVideoClick(this, '${reel.reelId}', '${reel.reelUrl || ''}', '${reel.thumbnailUrl || ''}')">
      ${reel.thumbnailUrl
        ? `<img class="reel-thumbnail" src="${reel.thumbnailUrl}" alt="Reel" loading="lazy" />`
        : `<div class="reel-thumbnail" style="background:linear-gradient(135deg,#0f1520,#0a0f1a);display:flex;align-items:center;justify-content:center;font-size:48px;aspect-ratio:9/16;max-height:500px;">🎬</div>`
      }
      <div class="reel-play-overlay"><div class="play-icon">▶</div></div>
    </div>
    <div class="reel-body">
      <div class="reel-description" id="desc-${reel.reelId}">
        ${truncateDesc(reel.description, reel.reelId)}
      </div>
      <div class="reel-actions">
        <button class="action-btn" id="like-btn-${reel.reelId}" onclick="toggleLike('${reel.reelId}')">
          <span class="action-icon">🤍</span>
          <span class="action-count" id="like-count-${reel.reelId}">—</span>
        </button>
        <button class="action-btn" onclick="toggleComments('${reel.reelId}')">
          <span class="action-icon">💬</span>
          <span class="action-count" id="comment-count-${reel.reelId}">—</span>
        </button>
        <button class="action-btn" id="save-btn-${reel.reelId}" onclick="toggleSave('${reel.reelId}')">
          <span class="action-icon">🔖</span>
          <span class="action-count">Save</span>
        </button>
        <button class="action-btn" onclick="shareReel('${reel.reelId}')" style="margin-left:auto;">
          <span class="action-icon">↗</span>
        </button>
      </div>
    </div>
    <div class="comments-panel" id="comments-${reel.reelId}">
      <div class="comments-list" id="comments-list-${reel.reelId}">
        <div class="loading-overlay" style="padding:20px;"><div class="spinner"></div></div>
      </div>
      <div class="comment-input-area">
        <textarea class="comment-input" id="comment-input-${reel.reelId}" placeholder="Add a comment..." rows="1"
          onkeydown="handleCommentKey(event, '${reel.reelId}')"></textarea>
        <button class="comment-submit" onclick="submitComment('${reel.reelId}')">Post</button>
      </div>
    </div>
  `;

  // Load likes count asynchronously
  api.getLikes(reel.reelId).then(count => {
    const el = document.getElementById(`like-count-${reel.reelId}`);
    if (el) el.textContent = formatCount(count);
  }).catch(() => {});

  return card;
}

function truncateDesc(text, id) {
  if (!text) return '';
  if (text.length <= 120) return escapeHtml(text);
  return `${escapeHtml(text.slice(0, 120))}<span class="read-more" onclick="expandDesc('${id}')">... more</span><span id="full-${id}" style="display:none">${escapeHtml(text.slice(120))}</span>`;
}

function expandDesc(id) {
  document.getElementById(`full-${id}`).style.display = 'inline';
  const rm = document.querySelector(`#desc-${id} .read-more`);
  if (rm) rm.remove();
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

function formatCount(n) {
  if (n >= 1000000) return (n/1000000).toFixed(1) + 'M';
  if (n >= 1000) return (n/1000).toFixed(1) + 'k';
  return String(n);
}

function goToProfile(username) {
  window.location.href = `profile.html?user=${encodeURIComponent(username)}`;
}

// ---- VIDEO HANDLING ----
function handleVideoClick(container, reelId, reelUrl, thumbnailUrl) {
  if (!reelUrl || reelUrl === 'null') {
    toast('Video not yet available — still processing', 'info');
    return;
  }
  openVideoModal(reelUrl);
}

function openVideoModal(url) {
  const modal = document.getElementById('videoModal');
  const video = document.getElementById('modalVideo');
  video.src = url;
  modal.classList.add('show');
  video.play().catch(() => {});
}

function closeVideoModal() {
  const modal = document.getElementById('videoModal');
  const video = document.getElementById('modalVideo');
  video.pause();
  video.src = '';
  modal.classList.remove('show');
}

document.getElementById('videoModal').addEventListener('click', function(e) {
  if (e.target === this) closeVideoModal();
});

// ---- INTERACTIONS ----
async function toggleLike(reelId) {
  try {
    const res = await api.toggleLike(reelId);
    const btn = document.getElementById(`like-btn-${reelId}`);
    const countEl = document.getElementById(`like-count-${reelId}`);
    if (btn) {
      btn.classList.toggle('liked', res.liked);
      btn.querySelector('.action-icon').textContent = res.liked ? '❤️' : '🤍';
    }
    if (countEl) countEl.textContent = formatCount(res.likesCount);
  } catch (err) {
    toast(err.message || 'Failed to like', 'error');
  }
}

async function toggleSave(reelId) {
  try {
    const res = await api.toggleSave(reelId);
    const btn = document.getElementById(`save-btn-${reelId}`);
    if (btn) {
      btn.classList.toggle('saved', res.saved);
      btn.querySelector('.action-icon').textContent = res.saved ? '🔖' : '🔖';
      btn.querySelector('.action-count').textContent = res.saved ? 'Saved' : 'Save';
    }
    toast(res.saved ? 'Saved to your collection!' : 'Removed from saved', 'success');
  } catch (err) {
    toast(err.message || 'Failed', 'error');
  }
}

async function toggleComments(reelId) {
  const panel = document.getElementById(`comments-${reelId}`);
  const isOpen = panel.classList.toggle('show');
  if (isOpen) {
    await loadComments(reelId);
  }
}

async function loadComments(reelId) {
  const list = document.getElementById(`comments-list-${reelId}`);
  list.innerHTML = '<div class="loading-overlay" style="padding:20px;"><div class="spinner"></div></div>';
  try {
    const res = await api.getComments(reelId, 0, 20);
    const comments = res.content || res || [];
    const countEl = document.getElementById(`comment-count-${reelId}`);
    if (countEl) countEl.textContent = formatCount(res.totalElements || comments.length);
    if (comments.length === 0) {
      list.innerHTML = '<div style="padding:20px; text-align:center; color:var(--text-muted); font-size:14px;">No comments yet. Be the first!</div>';
      return;
    }
    list.innerHTML = '';
    comments.forEach(c => list.appendChild(createCommentEl(c, reelId)));
  } catch {
    list.innerHTML = '<div style="padding:16px; text-align:center; color:var(--text-muted);">Failed to load comments</div>';
  }
}

function createCommentEl(c, reelId) {
  const div = document.createElement('div');
  div.className = 'comment-item';
  div.dataset.commentId = c.id;
  const isOwn = myProfile && c.userName === myProfile.username;
  div.innerHTML = `
    <img class="comment-avatar" src="${avatarUrl(null, c.userName)}" alt="" />
    <div class="comment-content">
      <span class="comment-user">${escapeHtml(c.userName)}</span>
      <div class="comment-text">${escapeHtml(c.comment)}</div>
      <div class="comment-time">${relativeTime(c.commentedAt)}</div>
    </div>
    ${isOwn ? `<button class="comment-delete" onclick="deleteComment('${reelId}', ${c.id}, this)" title="Delete">✕</button>` : ''}
  `;
  return div;
}

async function submitComment(reelId) {
  const input = document.getElementById(`comment-input-${reelId}`);
  const text = input.value.trim();
  if (!text) return;
  try {
    const c = await api.addComment(reelId, text);
    input.value = '';
    const list = document.getElementById(`comments-list-${reelId}`);
    const noComments = list.querySelector('div:only-child');
    if (noComments) list.innerHTML = '';
    const el = createCommentEl(c, reelId);
    list.insertBefore(el, list.firstChild);
    const countEl = document.getElementById(`comment-count-${reelId}`);
    if (countEl) {
      const prev = parseInt(countEl.textContent) || 0;
      countEl.textContent = formatCount(prev + 1);
    }
  } catch (err) {
    toast(err.message || 'Failed to post comment', 'error');
  }
}

function handleCommentKey(e, reelId) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    submitComment(reelId);
  }
}

async function deleteComment(reelId, commentId, btn) {
  try {
    await api.deleteComment(reelId, commentId);
    const item = btn.closest('.comment-item');
    item.style.opacity = '0';
    item.style.transform = 'translateX(-10px)';
    item.style.transition = 'all 0.2s ease';
    setTimeout(() => item.remove(), 200);
    toast('Comment deleted', 'success');
  } catch (err) {
    toast(err.message || 'Failed to delete', 'error');
  }
}

function showReelMenu(e, reelId) {
  e.stopPropagation();
  const existing = document.getElementById('reelContextMenu');
  if (existing) existing.remove();
  const menu = document.createElement('div');
  menu.id = 'reelContextMenu';
  menu.style.cssText = `position:fixed;background:var(--bg-card);border:1px solid var(--border);border-radius:var(--radius-md);box-shadow:var(--shadow-lg);z-index:500;min-width:160px;overflow:hidden;`;
  menu.innerHTML = `
    <button onclick="confirmDeleteReel('${reelId}')" style="display:block;width:100%;padding:12px 16px;background:none;border:none;color:var(--error);font-size:14px;font-family:var(--font);cursor:pointer;text-align:left;">🗑 Delete Reel</button>
  `;
  menu.style.left = Math.min(e.clientX, window.innerWidth - 180) + 'px';
  menu.style.top = e.clientY + 'px';
  document.body.appendChild(menu);
  setTimeout(() => document.addEventListener('click', () => menu.remove(), { once: true }), 0);
}

async function confirmDeleteReel(reelId) {
  if (!confirm('Delete this reel? This action cannot be undone.')) return;
  try {
    await api.deleteReel(reelId);
    const card = document.querySelector(`[data-reel-id="${reelId}"]`);
    if (card) { card.style.opacity = '0'; setTimeout(() => card.remove(), 200); }
    toast('Reel deleted', 'success');
  } catch (err) {
    toast(err.message || 'Failed to delete reel', 'error');
  }
}

function shareReel(reelId) {
  const url = `${window.location.origin}/frontend/feed.html?reel=${reelId}`;
  if (navigator.clipboard) {
    navigator.clipboard.writeText(url);
    toast('Link copied to clipboard!', 'success');
  }
}

// ---- SEARCH ----
async function handleSearch(query) {
  clearTimeout(searchTimeout);
  const results = document.getElementById('searchResults');
  if (!query.trim()) { results.classList.remove('show'); return; }
  searchTimeout = setTimeout(async () => {
    try {
      const profiles = await api.searchProfiles(query);
      if (!profiles || profiles.length === 0) { results.classList.remove('show'); return; }
      results.innerHTML = '';
      profiles.forEach(p => {
        const item = document.createElement('a');
        item.className = 'search-result-item';
        item.href = `profile.html?user=${encodeURIComponent(p.userName)}`;
        item.innerHTML = `
          <img class="search-result-avatar" src="${avatarUrl(p.profilePictureUrl, p.userName)}" alt="" />
          <div class="search-result-name">${escapeHtml(p.userName)}</div>
        `;
        results.appendChild(item);
      });
      results.classList.add('show');
    } catch {}
  }, 400);
}

document.addEventListener('click', e => {
  if (!e.target.closest('.navbar-search')) {
    document.getElementById('searchResults').classList.remove('show');
  }
});

// ---- RIGHT SIDEBAR: SUGGESTED USERS ----
async function loadSuggestedUsers() {
  const container = document.getElementById('suggestedUsers');
  // We'll search for some common users - in a real app this would be a dedicated endpoint
  try {
    const profiles = await api.searchProfiles('');
    if (!profiles || profiles.length === 0) {
      container.innerHTML = '<div style="color:var(--text-muted);font-size:13px;padding:8px;">No suggestions yet</div>';
      return;
    }
    container.innerHTML = '';
    profiles.slice(0, 4).forEach(p => {
      if (myProfile && p.userName === myProfile.username) return;
      const div = document.createElement('div');
      div.className = 'suggested-user';
      div.innerHTML = `
        <img class="suggested-avatar" src="${avatarUrl(p.profilePictureUrl, p.userName)}" alt="" />
        <div class="suggested-info">
          <div class="suggested-name">${escapeHtml(p.userName)}</div>
          <div class="suggested-followers">Developer</div>
        </div>
        <button class="follow-mini-btn" id="sug-follow-${p.userName}" onclick="quickFollow('${p.userName}')">Follow</button>
      `;
      container.appendChild(div);
    });
  } catch {
    container.innerHTML = '<div style="color:var(--text-muted);font-size:13px;padding:8px;">Could not load</div>';
  }
}

async function quickFollow(username) {
  const btn = document.getElementById(`sug-follow-${username}`);
  try {
    if (btn.classList.contains('following')) {
      await api.unfollow(username);
      btn.classList.remove('following');
      btn.textContent = 'Follow';
    } else {
      await api.follow(username);
      btn.classList.add('following');
      btn.textContent = 'Following';
    }
  } catch (err) {
    toast(err.message || 'Failed', 'error');
  }
}

// Start
init().then(() => loadSuggestedUsers());
