// upload.js — Upload page logic
if (!requireAuth()) throw new Error('Not authenticated');

let selectedFile = null;

// ---- INIT ----
async function initUpload() {
  try {
    const profile = await api.getMyProfile(0, 1);
    const avatar = avatarUrl(profile.profilePictureUrl, profile.username);
    document.getElementById('navAvatar').src = avatar;
  } catch {}
  setupDragDrop();
}

// ---- DRAG & DROP ----
function setupDragDrop() {
  const dz = document.getElementById('dropZone');
  dz.addEventListener('dragover', e => { e.preventDefault(); dz.classList.add('dragover'); });
  dz.addEventListener('dragleave', () => dz.classList.remove('dragover'));
  dz.addEventListener('drop', e => {
    e.preventDefault();
    dz.classList.remove('dragover');
    const file = e.dataTransfer.files[0];
    if (file) processFile(file);
  });
}

function handleFileSelect(input) {
  const file = input.files[0];
  if (file) processFile(file);
}

function processFile(file) {
  // Basic validation before even trying
  if (file.type !== 'video/mp4') {
    toast('Only MP4 videos are supported', 'error');
    return;
  }
  const maxSize = 45 * 1024 * 1024;
  if (file.size > maxSize) {
    toast('File size exceeds 45 MB limit', 'error');
    return;
  }

  selectedFile = file;
  const url = URL.createObjectURL(file);
  const preview = document.getElementById('videoPreview');
  const wrap = document.getElementById('videoPreviewWrap');
  const dz = document.getElementById('dropZone');
  const meta = document.getElementById('videoMeta');

  preview.src = url;
  wrap.classList.add('show');
  dz.style.display = 'none';
  meta.style.display = 'block';

  document.getElementById('metaName').textContent = file.name.length > 30 ? file.name.slice(0, 27) + '...' : file.name;
  document.getElementById('metaSize').textContent = formatFileSize(file.size);

  // Get video duration
  preview.onloadedmetadata = () => {
    const dur = preview.duration;
    const dEl = document.getElementById('metaDuration');
    if (isFinite(dur)) {
      dEl.textContent = `${Math.floor(dur)}s`;
      if (dur < 30 || dur > 60) {
        dEl.style.color = 'var(--error)';
        dEl.textContent += ' ⚠️ (must be 30–60s)';
        document.getElementById('step1Btn').disabled = true;
        toast('Video must be between 30 and 60 seconds', 'error');
        return;
      }
    }
    dEl.style.color = 'var(--success)';
    document.getElementById('step1Btn').disabled = false;
  };
}

function formatFileSize(bytes) {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function removeVideo() {
  selectedFile = null;
  const preview = document.getElementById('videoPreview');
  preview.src = '';
  document.getElementById('videoPreviewWrap').classList.remove('show');
  document.getElementById('dropZone').style.display = '';
  document.getElementById('videoMeta').style.display = 'none';
  document.getElementById('step1Btn').disabled = true;
  document.getElementById('videoFileInput').value = '';
}

// ---- STEPS ----
function goToStep2() {
  if (!selectedFile) { toast('Please select a video file', 'error'); return; }
  document.getElementById('step1').classList.remove('active');
  document.getElementById('step2').classList.add('active');
}

function goToStep1() {
  document.getElementById('step2').classList.remove('active');
  document.getElementById('step1').classList.add('active');
}

function updateCharCount(textarea) {
  document.getElementById('charCount').textContent = textarea.value.length;
}

// ---- UPLOAD ----
async function handleUpload() {
  const description = document.getElementById('reelDescription').value.trim();
  if (!description) { toast('Please add a description for your reel', 'error'); return; }
  if (!selectedFile) { toast('No video file selected', 'error'); return; }

  const btn = document.getElementById('uploadBtn');
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span> Submitting...';

  // Switch to step 3
  document.getElementById('step2').classList.remove('active');
  document.getElementById('step3').classList.add('active');
  document.getElementById('processingView').style.display = '';
  document.getElementById('successView').style.display = 'none';
  document.getElementById('errorView').style.display = 'none';

  // Animate progress
  animateProcessing();

  try {
    const fd = new FormData();
    fd.append('reel', JSON.stringify({ description }));
    fd.append('file', selectedFile);

    await api.uploadReel(fd);

    // Show success
    document.getElementById('processingView').style.display = 'none';
    document.getElementById('successView').style.display = 'block';
    toast('Reel submitted successfully!', 'success');
  } catch (err) {
    document.getElementById('processingView').style.display = 'none';
    document.getElementById('errorView').style.display = 'block';
    document.getElementById('errorMsg').textContent = err.message || 'Upload failed. Please try again.';
    toast(err.message || 'Upload failed', 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = '🚀 Submit Reel';
  }
}

function animateProcessing() {
  const steps = ['proc1', 'proc2', 'proc3', 'proc4'];
  const delays = [0, 600, 1200, 1800];
  steps.forEach((id, i) => {
    setTimeout(() => {
      const el = document.getElementById(id);
      if (!el) return;
      el.classList.remove('active');
      el.classList.add('done');
      if (i + 1 < steps.length) document.getElementById(steps[i + 1]).classList.add('active');
    }, delays[i]);
  });

  // Progress bar
  let pct = 0;
  const interval = setInterval(() => {
    pct = Math.min(pct + 3, 95);
    const bar = document.getElementById('uploadProgress');
    if (bar) bar.style.width = pct + '%';
    if (pct >= 95) clearInterval(interval);
  }, 80);
}

function resetUpload() {
  selectedFile = null;
  document.getElementById('step3').classList.remove('active');
  document.getElementById('step1').classList.add('active');
  document.getElementById('videoPreview').src = '';
  document.getElementById('videoPreviewWrap').classList.remove('show');
  document.getElementById('dropZone').style.display = '';
  document.getElementById('videoMeta').style.display = 'none';
  document.getElementById('step1Btn').disabled = true;
  document.getElementById('videoFileInput').value = '';
  document.getElementById('reelDescription').value = '';
  document.getElementById('charCount').textContent = '0';
  document.getElementById('uploadProgress').style.width = '0%';
  ['proc1','proc2','proc3','proc4'].forEach(id => {
    const el = document.getElementById(id);
    if (el) { el.classList.remove('done', 'active'); }
  });
  document.getElementById('proc1').classList.add('active');
}

initUpload();
