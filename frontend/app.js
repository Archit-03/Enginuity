const API_BASE = 'http://localhost:8081/enginuity';

// ---- API CLIENT ----
const api = {
  token: localStorage.getItem('techilla_token') || null,

  headers(extra = {}) {
    const h = { 'Content-Type': 'application/json', ...extra };
    if (this.token) h['Authorization'] = `Bearer ${this.token}`;
    return h;
  },

  async request(method, path, body, isFormData = false) {
    const opts = { method, credentials: 'include' };
    if (isFormData) {
      opts.headers = this.token ? { 'Authorization': `Bearer ${this.token}` } : {};
      opts.body = body;
    } else {
      opts.headers = this.headers();
      if (body) opts.body = JSON.stringify(body);
    }
    const res = await fetch(API_BASE + path, opts);
    if (!res.ok) {
      let err;
      try { err = await res.json(); } catch { err = { message: res.statusText }; }
      throw { status: res.status, message: err.message || err.error || 'Request failed', data: err };
    }
    const ct = res.headers.get('content-type') || '';
    if (ct.includes('application/json')) return res.json();
    return res.text();
  },

  get: (path) => api.request('GET', path),
  post: (path, body) => api.request('POST', path, body),
  patch: (path, body) => api.request('PATCH', path, body),
  put: (path, body) => api.request('PUT', path, body),
  delete: (path) => api.request('DELETE', path),
  postForm: (path, fd) => api.request('POST', path, fd, true),
  putForm: (path, fd) => api.request('PUT', path, fd, true),

  setToken(t) {
    this.token = t;
    if (t) localStorage.setItem('techilla_token', t);
    else localStorage.removeItem('techilla_token');
  },

  // AUTH
  login: (email, password) => api.post('/login', { email, password }),
  register: (email, password) => api.post('/register', { email, password }),
  verifyOtp: (email, otp) => api.post('/verify-otp', { email, otp }),
  resendOtp: (email) => api.post('/resend-otp', { email }),
  sendResetOtp: (email) => api.post('/send-reset-otp', { email }),
  resetPassword: (email, otp, newPassword) => api.post('/reset-password', { email, otp, newPassword }),
  logout: () => api.post('/logout'),

  // PROFILE
  createProfile: (fd) => api.postForm('/profile/create', fd),
  getMyProfile: (page = 0, size = 9) => api.get(`/profile/me?page=${page}&size=${size}`),
  getProfile: (username, page = 0, size = 9) => api.get(`/profile/${username}?page=${page}&size=${size}`),
  editProfile: (data) => api.patch('/profile/edit', data),
  editProfilePic: (fd) => api.putForm('/profile/edit-profilePic', fd),
  deleteProfilePic: () => api.delete('/profile/delete-profilePic'),
  searchProfiles: (q) => api.get(`/profile/search?username=${encodeURIComponent(q)}`),
  checkUsername: (u) => api.get(`/profile/check-username?username=${encodeURIComponent(u)}`),

  // FOLLOW
  follow: (username) => api.post(`/follow/${username}`),
  unfollow: (username) => api.delete(`/unfollow/${username}`),

  // REELS
  uploadReel: (fd) => api.postForm('/reel/upload', fd),
  getReel: (id) => api.get(`/reel/${id}`),
  deleteReel: (id) => api.delete(`/reel/${id}/delete`),

  // FEED
  buildFeed: () => api.post('/feed/build'),
  getFeed: (beforeScore, size = 10) => api.get(`/feed?beforeScore=${beforeScore}&size=${size}`),
  markSeen: (id) => api.post(`/feed/seen/${id}`),

  // INTERACTIONS
  toggleLike: (id) => api.post(`/reels/${id}/like`),
  getLikes: (id) => api.get(`/reels/${id}/likes/count`),
  addComment: (id, comment) => api.post(`/reels/${id}/comments`, { comment }),
  getComments: (id, page = 0, size = 20) => api.get(`/reels/${id}/comments?page=${page}&size=${size}`),
  deleteComment: (reelId, commentId) => api.delete(`/reels/${reelId}/comment/${commentId}`),
  toggleSave: (id) => api.post(`/reels/${id}/save`),
  getSaved: (page = 0, size = 10) => api.get(`/reels/saved?page=${page}&size=${size}`),
};

// ---- AUTH STATE ----
const auth = {
  getUser() {
    try { return JSON.parse(localStorage.getItem('techilla_user') || 'null'); } catch { return null; }
  },
  setUser(u) {
    if (u) localStorage.setItem('techilla_user', JSON.stringify(u));
    else localStorage.removeItem('techilla_user');
  },
  isLoggedIn() { return !!this.getUser() && !!api.token; },
  logout() {
    this.setUser(null);
    api.setToken(null);
    localStorage.removeItem('techilla_profile');
  }
};

// ---- TOAST ----
function toast(message, type = 'info', duration = 3500) {
  let container = document.querySelector('.toast-container');
  if (!container) {
    container = document.createElement('div');
    container.className = 'toast-container';
    document.body.appendChild(container);
  }
  const icons = { success: '✓', error: '✕', info: 'ℹ' };
  const t = document.createElement('div');
  t.className = `toast ${type}`;
  t.innerHTML = `<span class="toast-icon">${icons[type] || icons.info}</span><span class="toast-message">${message}</span>`;
  container.appendChild(t);
  setTimeout(() => {
    t.classList.add('hiding');
    setTimeout(() => t.remove(), 300);
  }, duration);
}

// ---- MODAL HELPERS ----
function openModal(id) {
  const m = document.getElementById(id);
  if (m) m.classList.add('show');
}
function closeModal(id) {
  const m = document.getElementById(id);
  if (m) m.classList.remove('show');
}
function closeAllModals() {
  document.querySelectorAll('.modal-overlay').forEach(m => m.classList.remove('show'));
}
document.addEventListener('keydown', e => { if (e.key === 'Escape') closeAllModals(); });
document.addEventListener('click', e => {
  if (e.target.classList.contains('modal-overlay')) closeAllModals();
});

// ---- RELATIVE TIME ----
function relativeTime(dateStr) {
  if (!dateStr) return '';
  const date = new Date(isNaN(dateStr) ? dateStr : Number(dateStr) * 1000);
  const diff = Date.now() - date.getTime();
  const s = Math.floor(diff / 1000);
  if (s < 60) return 'just now';
  const m = Math.floor(s / 60); if (m < 60) return `${m}m ago`;
  const h = Math.floor(m / 60); if (h < 24) return `${h}h ago`;
  const d = Math.floor(h / 24); if (d < 7) return `${d}d ago`;
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

// ---- AVATAR FALLBACK ----
function avatarUrl(url, name = '?') {
  if (url) return url;
  return `data:image/svg+xml,${encodeURIComponent(`<svg xmlns='http://www.w3.org/2000/svg' width='40' height='40' viewBox='0 0 40 40'><rect fill='%2300d4ff22' width='40' height='40' rx='20'/><text x='50%' y='55%' dominant-baseline='middle' text-anchor='middle' font-size='18' font-family='Arial' font-weight='700' fill='%2300d4ff'>${name.charAt(0).toUpperCase()}</text></svg>`)}`;
}

// ---- REDIRECT IF NOT LOGGED IN ----
function requireAuth() {
  if (!auth.isLoggedIn()) {
    window.location.href = 'index.html';
    return false;
  }
  return true;
}
