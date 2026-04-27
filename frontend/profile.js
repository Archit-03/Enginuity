// profile.js — Profile page logic
if (!requireAuth()) throw new Error('Not authenticated');

const params = new URLSearchParams(window.location.search);
const targetUser = params.get('user');
let profileData = null;
let myProfileData = null;
let currentPage = 0;
let totalPages = 0;
let editTags = { skill: [], interest: [] };
let searchTimeout = null;

async function init() {
  await loadMyProfile();
  if (targetUser && targetUser !== myProfileData?.username) {
    await loadOtherProfile(targetUser);
  } else {
    await loadMyOwnProfile();
  }
}

async function loadMyProfile() {
  try {
    myProfileData = await api.getMyProfile(0, 1);
    const avatar = avatarUrl(myProfileData.profilePictureUrl, myProfileData.username);
    document.getElementById('navAvatar').src = avatar;
    document.getElementById('sidebarAvatar').src = avatar;
    document.getElementById('sidebarUsername').textContent = myProfileData.username;
    document.getElementById('sidebarHandle').textContent = '@' + myProfileData.username;
  } catch {}
}

async function loadMyOwnProfile(page = 0) {
  try {
    profileData = await api.getMyProfile(page, 9);
    currentPage = profileData.page;
    totalPages = profileData.totalPages;
    renderProfile(profileData, true);
  } catch (err) {
    renderProfileError(err.message);
  }
}

async function loadOtherProfile(username, page = 0) {
  try {
    profileData = await api.getProfile(username, page, 9);
    currentPage = profileData.page;
    totalPages = profileData.totalPages;
    renderProfile(profileData, profileData.myProfile);
  } catch (err) {
    if (err.status === 404 || err.message?.includes('not exist')) {
      renderProfileNotFound(username);
    } else {
      renderProfileError(err.message);
    }
  }
}

function renderProfile(p, isMe) {
  document.title = `${p.username} — Techilla`;
  const wrapper = document.getElementById('profileWrapper');
  const avatarSrc = avatarUrl(p.profilePictureUrl, p.username);

  wrapper.innerHTML = `
    <div class="profile-hero fade-in">
      <div class="profile-banner"></div>
      <div class="profile-info-row">
        <div class="profile-avatar-wrap">
          ${isMe
            ? `<img class="profile-avatar" src="${avatarSrc}" alt="${p.username}" onclick="openModal('editPicModal')" style="cursor:pointer;" title="Change photo" />`
            : `<img class="profile-avatar" src="${avatarSrc}" alt="${p.username}" />`
          }
        </div>
        <div class="profile-meta">
          <div class="profile-name">${escapeHtml(p.username)}</div>
          <div class="profile-handle">@${escapeHtml(p.username)}</div>
          ${p.bio ? `<div class="profile-bio">${escapeHtml(p.bio)}</div>` : ''}
          <div class="profile-stats">
            <div class="stat-item">
              <div class="stat-value">${(p.reels || []).length}</div>
              <div class="stat-label">Reels</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">${p.followerCount || 0}</div>
              <div class="stat-label">Followers</div>
            </div>
          </div>
          ${p.githubUrl ? `<a href="${p.githubUrl}" class="github-link" target="_blank" rel="noopener" style="margin-top:12px;display:inline-flex;">🔗 GitHub</a>` : ''}
        </div>
        <div class="profile-actions">
          ${isMe
            ? `<button class="btn btn-secondary" onclick="openEditModal()">✏️ Edit Profile</button>`
            : `<button class="btn btn-primary" id="followBtn" onclick="handleFollow('${p.username}')">
                ${p.followedByCurrentUser ? '✓ Following' : '+ Follow'}
               </button>`
          }
        </div>
      </div>

      ${(p.skills?.length || p.interests?.length) ? `
        <div style="padding: 0 32px 24px;">
          <div class="profile-tags">
            ${(p.skills || []).map(s => `<span class="tag skill">${escapeHtml(s)}</span>`).join('')}
            ${(p.interests || []).map(i => `<span class="tag interest">#${escapeHtml(i)}</span>`).join('')}
          </div>
        </div>
      ` : ''}

      <div class="profile-tabs">
        <button class="profile-tab active" onclick="switchTab('reels', this)">🎬 Reels</button>
        ${isMe ? `<button class="profile-tab" onclick="switchTab('saved', this)">🔖 Saved</button>` : ''}
        <button class="profile-tab" onclick="switchTab('about', this)">👤 About</button>
      </div>
    </div>

    <!-- REELS TAB -->
    <div class="tab-content active" id="tab-reels">
      <div class="profile-reels-grid" id="reelGrid">
        ${renderReelThumbs(p.reels || [])}
      </div>
      ${totalPages > 1 ? `
        <div style="display:flex;justify-content:center;gap:12px;padding:24px;">
          <button class="btn btn-secondary btn-sm" onclick="loadPage(${currentPage - 1})" ${currentPage === 0 ? 'disabled' : ''}>← Prev</button>
          <span style="display:flex;align-items:center;color:var(--text-muted);font-size:14px;">Page ${currentPage + 1} of ${totalPages}</span>
          <button class="btn btn-secondary btn-sm" onclick="loadPage(${currentPage + 1})" ${currentPage >= totalPages - 1 ? 'disabled' : ''}>Next →</button>
        </div>
      ` : ''}
    </div>

    <!-- SAVED TAB (only for own profile) -->
    ${isMe ? `
    <div class="tab-content" id="tab-saved">
      <div class="profile-reels-grid" id="savedGrid">
        <div class="reel-grid-empty"><div class="spinner spinner-lg"></div></div>
      </div>
    </div>
    ` : ''}

    <!-- ABOUT TAB -->
    <div class="tab-content" id="tab-about">
      <div style="background:var(--bg-card);border:1px solid var(--border);border-radius:var(--radius-lg);padding:28px;margin-top:4px;">
        <h3 style="font-size:16px;font-weight:700;margin-bottom:16px;">About @${escapeHtml(p.username)}</h3>
        ${p.bio ? `<p style="color:var(--text-secondary);line-height:1.7;margin-bottom:20px;">${escapeHtml(p.bio)}</p>` : '<p style="color:var(--text-muted);">No bio yet.</p>'}
        ${p.skills?.length ? `
          <div style="margin-bottom:16px;">
            <div style="font-size:12px;font-weight:700;color:var(--text-muted);text-transform:uppercase;letter-spacing:1px;margin-bottom:8px;">Skills</div>
            <div class="profile-tags">${(p.skills).map(s => `<span class="tag skill">${escapeHtml(s)}</span>`).join('')}</div>
          </div>
        ` : ''}
        ${p.interests?.length ? `
          <div style="margin-bottom:16px;">
            <div style="font-size:12px;font-weight:700;color:var(--text-muted);text-transform:uppercase;letter-spacing:1px;margin-bottom:8px;">Interests</div>
            <div class="profile-tags">${(p.interests).map(i => `<span class="tag interest">#${escapeHtml(i)}</span>`).join('')}</div>
          </div>
        ` : ''}
        ${p.githubUrl ? `
          <div>
            <div style="font-size:12px;font-weight:700;color:var(--text-muted);text-transform:uppercase;letter-spacing:1px;margin-bottom:8px;">Links</div>
            <a href="${p.githubUrl}" class="github-link" target="_blank" rel="noopener">🔗 GitHub Profile</a>
          </div>
        ` : ''}
      </div>
    </div>
  `;

  // Update follow button state
  if (!isMe) {
    const btn = document.getElementById('followBtn');
    if (btn) {
      btn.classList.toggle('btn-secondary', p.followedByCurrentUser);
      btn.classList.toggle('btn-primary', !p.followedByCurrentUser);
    }
  }

  // Render right sidebar
  renderProfileSidebar(p, isMe);
}

function renderReelThumbs(reels) {
  if (!reels || reels.length === 0) {
    return `<div class="reel-grid-empty">
      <div style="font-size:48px;opacity:0.3;margin-bottom:12px;">🎬</div>
      <div style="font-size:16px;font-weight:600;color:var(--text-secondary);">No reels yet</div>
    </div>`;
  }
  return reels.map(r => `
    <div class="profile-reel-thumb" onclick="openReelView('${r.reelUrl || ''}', '${r.reelId}')">
      ${r.thumbnailUrl
        ? `<img src="${r.thumbnailUrl}" alt="${escapeHtml(r.description || '')}" loading="lazy" />`
        : `<div style="width:100%;height:100%;background:linear-gradient(135deg,#0f1520,#0a0f1a);display:flex;align-items:center;justify-content:center;font-size:36px;">🎬</div>`
      }
      <div class="overlay"><span>▶</span></div>
    </div>
  `).join('');
}

function renderProfileSidebar(p, isMe) {
  const container = document.getElementById('profileSidebarContent');
  container.innerHTML = `
    <div class="sidebar-card">
      <div class="sidebar-card-title">Stats</div>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
        <div style="text-align:center;padding:12px;background:var(--bg-secondary);border-radius:var(--radius-sm);">
          <div style="font-size:22px;font-weight:800;">${(p.reels || []).length}</div>
          <div style="font-size:12px;color:var(--text-muted);">Reels</div>
        </div>
        <div style="text-align:center;padding:12px;background:var(--bg-secondary);border-radius:var(--radius-sm);">
          <div style="font-size:22px;font-weight:800;">${p.followerCount || 0}</div>
          <div style="font-size:12px;color:var(--text-muted);">Followers</div>
        </div>
      </div>
    </div>
    ${!isMe ? `
    <div class="sidebar-card">
      <button class="btn ${p.followedByCurrentUser ? 'btn-secondary' : 'btn-primary'} w-full" id="sidebarFollowBtn" onclick="handleFollow('${p.username}')">
        ${p.followedByCurrentUser ? '✓ Following' : '+ Follow'}
      </button>
    </div>
    ` : `
    <div class="sidebar-card">
      <div class="sidebar-card-title">Manage</div>
      <div style="display:flex;flex-direction:column;gap:8px;">
        <button class="btn btn-secondary btn-sm w-full" onclick="openEditModal()">✏️ Edit Profile</button>
        <button class="btn btn-ghost btn-sm w-full" onclick="handleLogout()">↩ Sign Out</button>
      </div>
    </div>
    `}
  `;
}

function switchTab(tabName, btn) {
  document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
  document.querySelectorAll('.profile-tab').forEach(t => t.classList.remove('active'));
  const tab = document.getElementById(`tab-${tabName}`);
  if (tab) tab.classList.add('active');
  btn.classList.add('active');
  if (tabName === 'saved') loadSavedReels();
}

async function loadSavedReels() {
  const grid = document.getElementById('savedGrid');
  if (!grid) return;
  try {
    const res = await api.getSaved(0, 12);
    const reels = res.content || res || [];
    grid.innerHTML = renderReelThumbs(reels);
  } catch {
    grid.innerHTML = '<div class="reel-grid-empty">Could not load saved reels</div>';
  }
}

async function loadPage(page) {
  if (page < 0 || page >= totalPages) return;
  window.scrollTo(0, 0);
  if (targetUser && targetUser !== myProfileData?.username) {
    await loadOtherProfile(targetUser, page);
  } else {
    await loadMyOwnProfile(page);
  }
}

// ---- FOLLOW ----
async function handleFollow(username) {
  const btns = [document.getElementById('followBtn'), document.getElementById('sidebarFollowBtn')].filter(Boolean);
  const isFollowing = profileData?.followedByCurrentUser;
  try {
    if (isFollowing) {
      await api.unfollow(username);
      profileData.followedByCurrentUser = false;
      profileData.followerCount = Math.max(0, (profileData.followerCount || 1) - 1);
      btns.forEach(b => { b.textContent = '+ Follow'; b.className = 'btn btn-primary'; if(b.id==='sidebarFollowBtn') b.className += ' w-full'; });
      toast(`Unfollowed @${username}`, 'info');
    } else {
      await api.follow(username);
      profileData.followedByCurrentUser = true;
      profileData.followerCount = (profileData.followerCount || 0) + 1;
      btns.forEach(b => { b.textContent = '✓ Following'; b.className = 'btn btn-secondary'; if(b.id==='sidebarFollowBtn') b.className += ' w-full'; });
      toast(`Following @${username}!`, 'success');
    }
  } catch (err) {
    toast(err.message || 'Action failed', 'error');
  }
}

// ---- EDIT PROFILE ----
function openEditModal() {
  if (!profileData) return;
  document.getElementById('editUsername').value = profileData.username || '';
  document.getElementById('editBio').value = profileData.bio || '';
  document.getElementById('editGithub').value = profileData.githubUrl || '';
  editTags.skill = [...(profileData.skills || [])];
  editTags.interest = [...(profileData.interests || [])];
  renderEditTags('skill');
  renderEditTags('interest');
  openModal('editProfileModal');
}

function handleEditTag(e, type) {
  if (e.key === 'Enter' || e.key === ',') {
    e.preventDefault();
    const input = document.getElementById(type === 'skill' ? 'editSkillInput' : 'editInterestInput');
    const val = input.value.trim().replace(/,/g,'');
    if (val && !editTags[type].includes(val)) {
      editTags[type].push(val);
      renderEditTags(type);
    }
    input.value = '';
  }
}

function renderEditTags(type) {
  const container = document.getElementById(type === 'skill' ? 'editSkillTags' : 'editInterestTags');
  container.innerHTML = '';
  editTags[type].forEach((tag, i) => {
    const span = document.createElement('span');
    span.className = 'tag-item';
    span.innerHTML = `${escapeHtml(tag)}<button class="tag-remove" onclick="removeEditTag('${type}',${i})">×</button>`;
    container.appendChild(span);
  });
}

function removeEditTag(type, i) {
  editTags[type].splice(i, 1);
  renderEditTags(type);
}

async function handleEditProfile(e) {
  e.preventDefault();
  const btn = document.getElementById('saveProfileBtn');
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span> Saving...';
  try {
    const data = {};
    const u = document.getElementById('editUsername').value.trim();
    const b = document.getElementById('editBio').value.trim();
    const g = document.getElementById('editGithub').value.trim();
    if (u) data.username = u;
    if (b) data.bio = b;
    if (g) data.githubUrl = g;
    data.skills = editTags.skill;
    data.interests = editTags.interest;
    const updated = await api.editProfile(data);
    profileData = { ...profileData, ...updated };
    closeModal('editProfileModal');
    toast('Profile updated!', 'success');
    renderProfile(profileData, true);
  } catch (err) {
    toast(err.message || 'Failed to update profile', 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = 'Save Changes';
  }
}

// ---- PROFILE PIC ----
function previewNewPic(input) {
  const file = input.files[0];
  if (!file) return;
  const url = URL.createObjectURL(file);
  document.getElementById('newPicImg').src = url;
  document.getElementById('newPicImg').style.display = 'block';
  document.getElementById('newPicPlaceholder').style.display = 'none';
}

async function handleUpdatePic() {
  const input = document.getElementById('newPicInput');
  if (!input.files[0]) { toast('Please select a photo', 'error'); return; }
  const fd = new FormData();
  fd.append('image', input.files[0]);
  try {
    await api.editProfilePic(fd);
    toast('Profile photo updated!', 'success');
    closeModal('editPicModal');
    await loadMyOwnProfile(currentPage);
  } catch (err) {
    toast(err.message || 'Failed to update photo', 'error');
  }
}

async function handleDeletePic() {
  if (!confirm('Remove your profile photo?')) return;
  try {
    await api.deleteProfilePic();
    toast('Profile photo removed', 'success');
    closeModal('editPicModal');
    await loadMyOwnProfile(currentPage);
  } catch (err) {
    toast(err.message || 'Failed', 'error');
  }
}

// ---- VIDEO ----
function openReelView(url, reelId) {
  if (!url || url === 'null') {
    toast('Video not yet available — still processing', 'info');
    return;
  }
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

// ---- SEARCH ----
async function handleProfileSearch(query) {
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
        item.innerHTML = `<img class="search-result-avatar" src="${avatarUrl(p.profilePictureUrl, p.userName)}" alt="" /><div class="search-result-name">${escapeHtml(p.userName)}</div>`;
        results.appendChild(item);
      });
      results.classList.add('show');
    } catch {}
  }, 400);
}

document.addEventListener('click', e => {
  if (!e.target.closest('.navbar-search')) document.getElementById('searchResults').classList.remove('show');
});

// ---- MISC ----
function renderProfileError(msg) {
  document.getElementById('profileWrapper').innerHTML = `
    <div class="empty-state">
      <div class="empty-icon">⚠️</div>
      <div class="empty-title">Could not load profile</div>
      <div class="empty-text">${msg || 'An error occurred.'}</div>
      <button class="btn btn-primary mt-2" onclick="location.reload()">Retry</button>
    </div>`;
}

function renderProfileNotFound(username) {
  document.getElementById('profileWrapper').innerHTML = `
    <div class="empty-state">
      <div class="empty-icon">👤</div>
      <div class="empty-title">@${escapeHtml(username)} not found</div>
      <div class="empty-text">This profile doesn't exist or may have been removed.</div>
      <a href="feed.html" class="btn btn-primary mt-2">← Back to Feed</a>
    </div>`;
}

async function handleLogout() {
  try { await api.logout(); } catch {}
  auth.logout();
  window.location.href = 'index.html';
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

init();
