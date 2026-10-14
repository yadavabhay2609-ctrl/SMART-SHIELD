/**
 * Smart Shield - Government Form Scanner & AI Understanding (Module 6)
 * Handles client-side file validation, drag-and-drop uploads, temporary in-memory
 * lifecycle, interactive field filtering, and strict textContent XSS defense.
 */

document.addEventListener('DOMContentLoaded', () => {
  // Elements
  const dropzone = document.getElementById('dropzone');
  const fileInput = document.getElementById('fileInput');
  const selectedFileBox = document.getElementById('selectedFileBox');
  const selectedFileName = document.getElementById('selectedFileName');
  const selectedFileSize = document.getElementById('selectedFileSize');
  const fileTypeIcon = document.getElementById('fileTypeIcon');
  const removeFileBtn = document.getElementById('removeFileBtn');
  const instructionInput = document.getElementById('instructionInput');
  const scanBtn = document.getElementById('scanBtn');
  
  // Status banner elements
  const scannerStatusText = document.getElementById('scannerStatusText');
  const scannerBadge = document.getElementById('scannerBadge');
  const scannerIndicator = document.getElementById('scannerIndicator');

  // Progress elements
  const progressBox = document.getElementById('progressBox');
  const progressStage = document.getElementById('progressStage');
  const progressSub = document.getElementById('progressSub');

  // Results elements
  const resultsContainer = document.getElementById('resultsContainer');
  const resDocType = document.getElementById('resDocType');
  const resDocSummary = document.getElementById('resDocSummary');
  const resConfidence = document.getElementById('resConfidence');
  const missingAlert = document.getElementById('missingAlert');
  const missingList = document.getElementById('missingList');
  const warningsAlert = document.getElementById('warningsAlert');
  const warningsList = document.getElementById('warningsList');
  const verificationAlert = document.getElementById('verificationAlert');
  const verificationList = document.getElementById('verificationList');
  const fieldsCount = document.getElementById('fieldsCount');
  const fieldsTbody = document.getElementById('fieldsTbody');
  const filterPills = document.querySelectorAll('.filter-pill');
  const resDisclaimer = document.getElementById('resDisclaimer');

  // User elements
  const userNameEl = document.getElementById('userName');
  const userAvatarEl = document.getElementById('userAvatar');
  const authBtn = document.getElementById('authBtn');
  const authModal = document.getElementById('authModal');
  const authForm = document.getElementById('authForm');
  const authToggleBtn = document.getElementById('authToggleBtn');
  const authError = document.getElementById('authError');
  const authTitle = document.getElementById('authTitle');
  const authSubmitBtn = document.getElementById('authSubmitBtn');
  const authNameRow = document.getElementById('authNameRow');

  // State
  let currentFile = null;
  let isScanning = false;
  let scannedFields = [];
  let currentFilter = 'ALL';
  let isRegistering = false;

  // Maximum file size: 10 MB
  const MAX_FILE_SIZE = 10 * 1024 * 1024;
  const ALLOWED_EXTS = ['.pdf', '.jpg', '.jpeg', '.png', '.webp'];

  // Initialize Session & Header
  initUserSession();

  // Initialize Engine Status
  checkScannerStatus();

  // Dropzone Click Trigger
  dropzone.addEventListener('click', () => {
    if (!isScanning) {
      fileInput.click();
    }
  });

  // Drag and Drop Events
  ['dragenter', 'dragover'].forEach(eventName => {
    dropzone.addEventListener(eventName, (e) => {
      e.preventDefault();
      e.stopPropagation();
      dropzone.classList.add('drag-over');
    });
  });

  ['dragleave', 'drop'].forEach(eventName => {
    dropzone.addEventListener(eventName, (e) => {
      e.preventDefault();
      e.stopPropagation();
      dropzone.classList.remove('drag-over');
    });
  });

  dropzone.addEventListener('drop', (e) => {
    const files = e.dataTransfer.files;
    if (files && files.length > 0) {
      handleSelectedFile(files[0]);
    }
  });

  // File Input Change
  fileInput.addEventListener('change', () => {
    if (fileInput.files && fileInput.files.length > 0) {
      handleSelectedFile(fileInput.files[0]);
    }
  });

  // Remove Selected File
  removeFileBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    resetSelectedFile();
  });

  // Handle Selected File & Client-side Validation
  function handleSelectedFile(file) {
    if (!file) return;

    // 1. Extension check
    const ext = getExtension(file.name);
    if (!ALLOWED_EXTS.includes(ext)) {
      alert(`Unsupported file format (${ext || 'unknown'}). Please upload a PDF, JPEG, PNG, or WEBP document.`);
      return;
    }

    // 2. Size check
    if (file.size > MAX_FILE_SIZE) {
      alert(`File size exceeds 10 MB limit (${(file.size / (1024 * 1024)).toFixed(2)} MB). Please select a smaller document.`);
      return;
    }

    if (file.size === 0) {
      alert("The selected file is empty. Please choose a valid document.");
      return;
    }

    currentFile = file;
    selectedFileName.textContent = file.name;
    selectedFileSize.textContent = `${formatBytes(file.size)} • ${file.type || 'document'}`;
    fileTypeIcon.textContent = ext.replace('.', '').toUpperCase();

    dropzone.style.display = 'none';
    selectedFileBox.style.display = 'flex';
    scanBtn.disabled = false;
  }

  function resetSelectedFile() {
    currentFile = null;
    fileInput.value = '';
    dropzone.style.display = 'block';
    selectedFileBox.style.display = 'none';
    scanBtn.disabled = true;
    resultsContainer.style.display = 'none';
  }

  // Scan Button Click Handler
  scanBtn.addEventListener('click', async () => {
    if (!currentFile || isScanning) return;

    const token = getToken();
    if (!token) {
      showAuthModal("Please sign in as a citizen to scan government documents.");
      return;
    }

    isScanning = true;
    scanBtn.disabled = true;
    resultsContainer.style.display = 'none';
    progressBox.style.display = 'block';

    // Progressive loading messages
    progressStage.textContent = "Preparing document...";
    progressSub.textContent = "Verifying document signature and preparing secure memory buffer...";

    const t1 = setTimeout(() => {
      progressStage.textContent = "Uploading securely...";
      progressSub.textContent = "Streaming document to backend for magic-byte verification...";
    }, 600);

    const t2 = setTimeout(() => {
      progressStage.textContent = "Analyzing form structure...";
      progressSub.textContent = "Inspecting form fields, completeness, and official requirements...";
    }, 1500);

    try {
      const instruction = instructionInput.value.trim();
      const response = await SmartShieldAPI.scanGovernmentForm(currentFile, instruction);

      clearTimeout(t1);
      clearTimeout(t2);
      progressBox.style.display = 'none';

      if (!response.success) {
        if (response.source === 'NOT_CONFIGURED') {
          renderNotConfigured(response, currentFile);
          return;
        }
        alert(response.message || "Document scan could not be completed.");
        return;
      }

      // Render structured results
      renderScanResults(response);

    } catch (err) {
      clearTimeout(t1);
      clearTimeout(t2);
      progressBox.style.display = 'none';
      console.error("Form scan error:", err);

      if (err.status === 401) {
        showAuthModal("Your session has expired. Please sign in again.");
      } else if (err.status === 429) {
        alert("Scan rate limit reached (maximum 5 scans per minute per citizen). Please wait before scanning another document.");
      } else {
        const errorMsg = (err.data && err.data.message) ? err.data.message : err.message;
        alert(`Form scanning error: ${errorMsg}`);
      }
    } finally {
      isScanning = false;
      scanBtn.disabled = false;
    }
  });

  // Render Honest NOT_CONFIGURED State (Phase 13 & 14)
  function renderNotConfigured(data, file) {
    resultsContainer.style.display = 'block';
    resDocType.textContent = "AI Analysis Unavailable";
    resDocSummary.textContent = "AI document analysis is currently unavailable because Gemini is not configured. Document structure and magic bytes were verified, with zero permanent storage.";
    resConfidence.textContent = "0% (Not Configured)";
    resDisclaimer.textContent = data.message || "Notice: Government Form Scanner requires Gemini multimodal configuration. Local security checks passed.";

    missingAlert.style.display = 'none';
    warningsAlert.style.display = 'none';
    verificationAlert.style.display = 'none';

    // Populate verified file metadata only — do not fabricate extracted fields
    const metaItems = [
      { name: "File Name", value: file ? file.name : "Uploaded Document", status: "VERIFIED", type: "METADATA" },
      { name: "File Format", value: file ? getExtension(file.name).toUpperCase().replace('.', '') : "UNKNOWN", status: "VERIFIED", type: "METADATA" },
      { name: "File Size", value: file ? formatBytes(file.size) : "0 Bytes", status: "VERIFIED", type: "METADATA" },
      { name: "Magic-Byte Integrity", value: "Verified by Backend (PDF/Image)", status: "VERIFIED", type: "SECURITY" },
      { name: "Document Storage", value: "Zero Permanent Storage (Transient In-Memory Only)", status: "VERIFIED", type: "SECURITY" },
      { name: "AI Extraction Engine", value: "Google Gemini Multimodal (Unconfigured)", status: "UNCONFIGURED", type: "SYSTEM" }
    ];

    fieldsCount.textContent = `${metaItems.length} Verified Metadata Properties`;
    fieldsTbody.innerHTML = '';
    metaItems.forEach(item => {
      const tr = document.createElement('tr');
      const tdName = document.createElement('td');
      tdName.innerHTML = `<strong>${item.name}</strong><br><small style="color:#64748b;">${item.type}</small>`;
      const tdVal = document.createElement('td');
      tdVal.textContent = item.value;
      const tdStatus = document.createElement('td');
      const badgeClass = item.status === 'VERIFIED' ? 'badge-complete' : 'badge-attention';
      tdStatus.innerHTML = `<span class="field-badge ${badgeClass}">${item.status}</span>`;
      tr.appendChild(tdName);
      tr.appendChild(tdVal);
      tr.appendChild(tdStatus);
      fieldsTbody.appendChild(tr);
    });

    resultsContainer.scrollIntoView({ behavior: 'smooth' });
  }

  // Render Structured Scan Results (Strict textContent XSS Defense)
  function renderScanResults(data) {
    resDocType.textContent = data.documentType || "Government Document";
    resDocSummary.textContent = data.summary || "No document summary provided.";
    resConfidence.textContent = `${data.documentTypeConfidence || 0}% Confidence`;
    if (data.disclaimer) {
      resDisclaimer.textContent = data.disclaimer;
    }

    // Missing Fields Alert
    if (Array.isArray(data.missingFields) && data.missingFields.length > 0) {
      missingList.innerHTML = '';
      data.missingFields.forEach(item => {
        const li = document.createElement('li');
        li.textContent = item;
        missingList.appendChild(li);
      });
      missingAlert.style.display = 'block';
    } else {
      missingAlert.style.display = 'none';
    }

    // Quality Warnings Alert
    if (Array.isArray(data.warnings) && data.warnings.length > 0) {
      warningsList.innerHTML = '';
      data.warnings.forEach(warn => {
        const li = document.createElement('li');
        li.textContent = warn;
        warningsList.appendChild(li);
      });
      warningsAlert.style.display = 'block';
    } else {
      warningsAlert.style.display = 'none';
    }

    // Verification Items Alert
    if (Array.isArray(data.verificationItems) && data.verificationItems.length > 0) {
      verificationList.innerHTML = '';
      data.verificationItems.forEach(v => {
        const li = document.createElement('li');
        li.textContent = v;
        verificationList.appendChild(li);
      });
      verificationAlert.style.display = 'block';
    } else {
      verificationAlert.style.display = 'none';
    }

    // Fields Table
    scannedFields = Array.isArray(data.fields) ? data.fields : [];
    fieldsCount.textContent = scannedFields.length;
    renderFilteredFields('ALL');

    resultsContainer.style.display = 'flex';
    resultsContainer.scrollIntoView({ behavior: 'smooth' });
  }

  // Render Fields Table with Filter Support
  function renderFilteredFields(filter) {
    currentFilter = filter;
    fieldsTbody.innerHTML = '';

    const filtered = scannedFields.filter(f => {
      if (filter === 'ALL') return true;
      if (filter === 'EMPTY') return f.status === 'EMPTY';
      if (filter === 'FILLED') return f.status === 'FILLED';
      if (filter === 'UNCLEAR') return f.status === 'UNCLEAR';
      return true;
    });

    if (filtered.length === 0) {
      const tr = document.createElement('tr');
      const td = document.createElement('td');
      td.colSpan = 5;
      td.style.textAlign = 'center';
      td.style.padding = '24px';
      td.style.color = '#8a94a6';
      td.textContent = `No fields match the filter "${filter}".`;
      tr.appendChild(td);
      fieldsTbody.appendChild(tr);
      return;
    }

    filtered.forEach(field => {
      const tr = document.createElement('tr');

      // 1. Label cell
      const tdLabel = document.createElement('td');
      tdLabel.className = 'field-label-cell';
      const strongLabel = document.createElement('strong');
      strongLabel.textContent = field.label || 'Field';
      const smallName = document.createElement('small');
      smallName.textContent = field.name ? `[${field.name}]` : '';
      tdLabel.appendChild(strongLabel);
      tdLabel.appendChild(smallName);

      if (field.verificationRequired) {
        const verTag = document.createElement('span');
        verTag.className = 'ver-tag';
        verTag.textContent = 'VERIFICATION REQUIRED';
        tdLabel.appendChild(document.createElement('br'));
        tdLabel.appendChild(verTag);
      }

      // 2. Value cell (masked)
      const tdValue = document.createElement('td');
      tdValue.className = 'field-value-cell';
      tdValue.textContent = field.value && field.value.trim() ? field.value : '— (blank)';

      // 3. Status cell
      const tdStatus = document.createElement('td');
      const badge = document.createElement('span');
      const st = (field.status || 'UNCLEAR').toLowerCase();
      badge.className = `status-badge ${st}`;
      badge.textContent = field.status || 'UNCLEAR';
      tdStatus.appendChild(badge);

      // 4. Confidence cell
      const tdConf = document.createElement('td');
      tdConf.textContent = field.confidence != null ? `${field.confidence}%` : '—';

      // 5. Guidance cell
      const tdGuidance = document.createElement('td');
      tdGuidance.className = 'field-guidance-cell';
      tdGuidance.textContent = field.guidance || 'Verify requirement against official form instructions.';

      tr.appendChild(tdLabel);
      tr.appendChild(tdValue);
      tr.appendChild(tdStatus);
      tr.appendChild(tdConf);
      tr.appendChild(tdGuidance);

      fieldsTbody.appendChild(tr);
    });
  }

  // Filter Pills Event Handlers
  filterPills.forEach(pill => {
    pill.addEventListener('click', () => {
      filterPills.forEach(p => p.classList.remove('active'));
      pill.classList.add('active');
      const filter = pill.getAttribute('data-filter');
      renderFilteredFields(filter);
    });
  });

  // Scanner Engine Status Check (GET /api/forms/status)
  async function checkScannerStatus() {
    try {
      const status = await SmartShieldAPI.getFormScannerStatus();
      if (status.configured) {
        scannerIndicator.className = 'ai-status-indicator';
        scannerStatusText.textContent = `Google Gemini Multimodal AI Active (${status.model || 'gemini-1.5-flash'}) • Zero Permanent Storage`;
        scannerBadge.className = 'ai-engine-badge gemini';
        scannerBadge.textContent = 'GEMINI ACTIVE';
      } else {
        scannerIndicator.className = 'ai-status-indicator fallback';
        scannerStatusText.textContent = 'Gemini API key unconfigured. Real form scanning requires configured Gemini credentials.';
        scannerBadge.className = 'ai-engine-badge kb';
        scannerBadge.textContent = 'UNCONFIGURED';
      }
    } catch (err) {
      console.warn("Scanner status check unreachable:", err);
      scannerIndicator.className = 'ai-status-indicator offline';
      scannerStatusText.textContent = 'Scanner Service Offline';
      scannerBadge.className = 'ai-engine-badge';
      scannerBadge.textContent = 'OFFLINE';
    }
  }

  // Format Helper Utilities
  function getExtension(filename) {
    if (!filename || !filename.includes('.')) return '';
    return '.' + filename.split('.').pop().toLowerCase();
  }

  function formatBytes(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  // Init User Session & Navigation
  function initUserSession() {
    const user = getUser();
    const token = getToken();

    if (user && token) {
      if (userNameEl) userNameEl.textContent = user.fullName || 'Citizen';
      if (userAvatarEl) userAvatarEl.textContent = getInitials(user.fullName || 'C');
      if (authBtn) {
        authBtn.innerHTML = '↪ &nbsp; Logout';
        authBtn.onclick = () => {
          clearAuth();
          window.location.replace('../../index.html');
        };
      }
    } else {
      if (userNameEl) userNameEl.textContent = 'Guest Citizen';
      if (userAvatarEl) userAvatarEl.textContent = 'GC';
      if (authBtn) {
        authBtn.innerHTML = '➔ &nbsp; Sign In';
        authBtn.onclick = () => showAuthModal();
      }
    }
  }

  function getInitials(name) {
    if (!name) return 'SS';
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }

  // Citizen Auth Modal Handlers
  function showAuthModal(message) {
    if (authError) {
      if (message) {
        authError.textContent = message;
        authError.style.display = 'block';
      } else {
        authError.style.display = 'none';
      }
    }
    authModal.style.display = 'flex';
  }

  const closeBtns = document.querySelectorAll('[data-close]');
  closeBtns.forEach(b => b.addEventListener('click', () => {
    authModal.style.display = 'none';
  }));

  if (authToggleBtn) {
    authToggleBtn.addEventListener('click', () => {
      isRegistering = !isRegistering;
      authTitle.textContent = isRegistering ? 'Citizen Registration' : 'Citizen Sign In';
      authSubmitBtn.textContent = isRegistering ? 'Register & Sign In' : 'Sign In';
      authToggleBtn.textContent = isRegistering ? 'Have an account? Sign In' : 'Register';
      authNameRow.style.display = isRegistering ? 'block' : 'none';
      authError.style.display = 'none';
    });
  }

  if (authForm) {
    authForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      authError.style.display = 'none';
      const email = document.getElementById('authEmail').value.trim();
      const password = document.getElementById('authPassword').value;
      const fullName = document.getElementById('authFullName') ? document.getElementById('authFullName').value.trim() : '';

      try {
        let authRes;
        if (isRegistering) {
          if (!fullName) {
            authError.textContent = 'Full name is required for registration.';
            authError.style.display = 'block';
            return;
          }
          authRes = await SmartShieldAPI.signup(fullName, email, password);
        } else {
          authRes = await SmartShieldAPI.login(email, password);
        }

        if (authRes && authRes.token) {
          setToken(authRes.token);
          setUser({
            fullName: authRes.fullName || fullName || 'Citizen',
            email: authRes.email || email,
            role: authRes.role || 'CITIZEN'
          });
          authModal.style.display = 'none';
          initUserSession();
        }
      } catch (err) {
        authError.textContent = (err.data && err.data.message) ? err.data.message : err.message;
        authError.style.display = 'block';
      }
    });
  }
});
