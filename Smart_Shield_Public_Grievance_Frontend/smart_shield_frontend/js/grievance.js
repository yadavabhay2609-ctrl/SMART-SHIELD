/**
 * Smart Shield - Grievance Citizen Frontend Controller
 * Connects UI elements to live backend REST APIs.
 */

// DOM Elements
const complaintModal = document.getElementById("complaintModal");
const detailsModal = document.getElementById("detailsModal");
const authModal = document.getElementById("authModal");
const toast = document.getElementById("toast");

const complaintForm = document.getElementById("complaintForm");
const complaintTable = document.getElementById("complaintTable");
const searchInput = document.getElementById("searchInput");
const statusFilter = document.getElementById("statusFilter");

// Stats Elements
const totalCountEl = document.getElementById("totalCount");
const progressCountEl = document.getElementById("progressCount");
const resolvedCountEl = document.getElementById("resolvedCount");
const priorityCountEl = document.getElementById("priorityCount");

// Profile / Header Elements
const welcomeGreeting = document.getElementById("welcomeGreeting");
const userAvatar = document.getElementById("userAvatar");
const userName = document.getElementById("userName");
const userRole = document.getElementById("userRole");
const authBtn = document.getElementById("authBtn");

// Details Elements
let activeTrackingId = null;
const detailsTrackingId = document.getElementById("detailsTrackingId");
const detailsTitle = document.getElementById("detailsTitle");
const detailsSubmitted = document.getElementById("detailsSubmitted");
const detailsStatus = document.getElementById("detailsStatus");
const detailsPriority = document.getElementById("detailsPriority");
const detailsCategory = document.getElementById("detailsCategory");
const detailsLocation = document.getElementById("detailsLocation");
const detailsDescription = document.getElementById("detailsDescription");
const historyList = document.getElementById("historyList");
const evidenceList = document.getElementById("evidenceList");
const attachEvidenceForm = document.getElementById("attachEvidenceForm");
const attachEvidenceFile = document.getElementById("attachEvidenceFile");

// Auth Modal Elements
const authForm = document.getElementById("authForm");
const authTitle = document.getElementById("authTitle");
const authPill = document.getElementById("authPill");
const authSub = document.getElementById("authSub");
const authNameRow = document.getElementById("authNameRow");
const authFullName = document.getElementById("authFullName");
const authEmail = document.getElementById("authEmail");
const authPassword = document.getElementById("authPassword");
const authSubmitBtn = document.getElementById("authSubmitBtn");
const authToggleBtn = document.getElementById("authToggleBtn");
const authTogglePrompt = document.getElementById("authTogglePrompt");
let isSignUpMode = false;

// In-memory cache of current citizen's grievances
let loadedGrievances = [];

// ============================================================
// UTILITY FUNCTIONS
// ============================================================

function escapeHtml(s) {
  if (s == null) return "";
  return String(s).replace(/[&<>"']/g, c => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#039;"
  }[c]));
}

function showToast(msg, isError = false) {
  toast.textContent = msg;
  toast.style.background = isError ? "#b91c1c" : "#17243b";
  toast.classList.add("show");
  setTimeout(() => toast.classList.remove("show"), 3500);
}

function formatCategory(cat) {
  if (!cat) return "";
  const map = {
    PUBLIC_SAFETY: "Public Safety",
    ROADS: "Roads",
    SANITATION: "Sanitation",
    WATER_SUPPLY: "Water Supply",
    ELECTRICITY: "Electricity",
    CORRUPTION: "Corruption"
  };
  return map[cat] || cat;
}

function formatPriority(pri) {
  if (!pri) return "Medium";
  const map = {
    LOW: "Low",
    MEDIUM: "Medium",
    HIGH: "High",
    CRITICAL: "Critical"
  };
  return map[pri] || pri;
}

function getPriorityClass(pri) {
  if (!pri) return "medium";
  const p = pri.toUpperCase();
  if (p === "HIGH" || p === "CRITICAL") return "high";
  if (p === "LOW") return "low";
  return "medium";
}

function formatStatus(status) {
  if (!status) return "Pending";
  const map = {
    PENDING: "Pending",
    IN_REVIEW: "In Progress",
    RESOLVED: "Resolved",
    ESCALATED: "Escalated"
  };
  return map[status] || status;
}

function getStatusClass(status) {
  if (!status) return "pending";
  const s = status.toUpperCase();
  if (s === "IN_REVIEW") return "progress";
  if (s === "RESOLVED") return "resolved";
  if (s === "ESCALATED") return "escalated";
  return "pending";
}

function formatDate(dateStr) {
  if (!dateStr) return "";
  try {
    const d = new Date(dateStr);
    return d.toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" });
  } catch (e) {
    return dateStr;
  }
}

function formatDateTime(dateStr) {
  if (!dateStr) return "";
  try {
    const d = new Date(dateStr);
    return d.toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" }) +
      " at " + d.toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit" });
  } catch (e) {
    return dateStr;
  }
}

function formatBytes(bytes) {
  if (!bytes || bytes === 0) return "0 Bytes";
  const k = 1024;
  const sizes = ["Bytes", "KB", "MB", "GB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + " " + sizes[i];
}

// ============================================================
// MODAL CONTROLS
// ============================================================

let pendingOpenComplaint = false;

const openComplaint = () => {
  if (!getToken()) {
    pendingOpenComplaint = true;
    showToast("Please sign in first to submit a complaint.", true);
    openAuthModal(false);
    return;
  }
  complaintModal.classList.add("show");
};

const openComplaintBtn = document.getElementById("openComplaint");
if (openComplaintBtn) openComplaintBtn.addEventListener("click", openComplaint);

const navRaiseComplaint = document.getElementById("navRaiseComplaint");
if (navRaiseComplaint) {
  navRaiseComplaint.addEventListener("click", (e) => {
    e.preventDefault();
    openComplaint();
  });
}

document.querySelectorAll("[data-close]").forEach(btn => {
  btn.addEventListener("click", () => {
    document.querySelectorAll(".modal").forEach(m => m.classList.remove("show"));
  });
});

[complaintModal, detailsModal, authModal].forEach(modal => {
  if (modal) {
    modal.addEventListener("click", e => {
      if (e.target === modal) modal.classList.remove("show");
    });
  }
});

// ============================================================
// AUTHENTICATION & SESSION HANDLING
// ============================================================

function updateAuthUI() {
  const token = getToken();
  const user = getUser();

  if (token && user) {
    const firstName = (user.fullName || "Citizen").split(" ")[0];
    if (welcomeGreeting) welcomeGreeting.textContent = `Welcome back, ${escapeHtml(firstName)} 👋`;
    if (userName) userName.textContent = escapeHtml(user.fullName || "Citizen");
    if (userRole) userRole.textContent = escapeHtml(user.role || "Citizen");
    if (userAvatar) {
      const initials = (user.fullName || "C")
        .split(" ")
        .map(n => n[0])
        .join("")
        .substring(0, 2)
        .toUpperCase();
      userAvatar.textContent = initials;
    }
    if (authBtn) {
      authBtn.innerHTML = "↪ &nbsp; Logout";
      authBtn.title = "Click to sign out";
    }
  } else {
    if (welcomeGreeting) welcomeGreeting.textContent = "Welcome to Smart Shield 👋";
    if (userName) userName.textContent = "Guest Citizen";
    if (userRole) userRole.textContent = "Not signed in";
    if (userAvatar) userAvatar.textContent = "--";
    if (authBtn) {
      authBtn.innerHTML = "↪ &nbsp; Sign In";
      authBtn.title = "Click to sign in";
    }
  }
}

function openAuthModal(signUp = false) {
  isSignUpMode = signUp;
  if (isSignUpMode) {
    authTitle.textContent = "Create Citizen Account";
    authPill.textContent = "NEW CITIZEN REGISTRATION";
    authSub.textContent = "Join Smart Shield to report issues and track resolutions.";
    authNameRow.style.display = "block";
    authFullName.required = true;
    authSubmitBtn.textContent = "Create Account →";
    authTogglePrompt.textContent = "Already have an account?";
    authToggleBtn.textContent = "Sign In";
  } else {
    authTitle.textContent = "Citizen Sign In";
    authPill.textContent = "CITIZEN ACCESS";
    authSub.textContent = "Sign in to submit and track your civic grievances.";
    authNameRow.style.display = "none";
    authFullName.required = false;
    authSubmitBtn.textContent = "Sign In →";
    authTogglePrompt.textContent = "Don't have an account?";
    authToggleBtn.textContent = "Sign Up";
  }
  authModal.classList.add("show");
}

authToggleBtn.addEventListener("click", () => {
  openAuthModal(!isSignUpMode);
});

if (authBtn) {
  authBtn.addEventListener("click", () => {
    if (getToken()) {
      clearAuth();
      window.location.replace('../../index.html');
    } else {
      openAuthModal(false);
    }
  });
}

authForm.addEventListener("submit", async e => {
  e.preventDefault();
  const email = authEmail.value.trim();
  const password = authPassword.value;
  const fullName = authFullName.value.trim();

  authSubmitBtn.disabled = true;
  authSubmitBtn.textContent = "Connecting...";

  try {
    let res;
    if (isSignUpMode) {
      if (!fullName) {
        showToast("Full name is required", true);
        authSubmitBtn.disabled = false;
        authSubmitBtn.textContent = "Create Account →";
        return;
      }
      res = await SmartShieldAPI.signup(fullName, email, password);
      showToast("✓ Account created and signed in!");
    } else {
      res = await SmartShieldAPI.login(email, password);
      showToast("✓ Signed in successfully!");
    }

    if (res && res.token) {
      setToken(res.token);
      setUser(res);
      updateAuthUI();
      authModal.classList.remove("show");
      authForm.reset();
      loadGrievances();
      if (pendingOpenComplaint) {
        pendingOpenComplaint = false;
        setTimeout(() => openComplaint(), 300);
      }
    }
  } catch (err) {
    showToast(err.message || "Authentication failed", true);
  } finally {
    authSubmitBtn.disabled = false;
    authSubmitBtn.textContent = isSignUpMode ? "Create Account →" : "Sign In →";
  }
});

// Hook for API 401 callback
window.onAuthExpired = () => {
  clearAuth();
  updateAuthUI();
  showToast("Your session has expired. Please sign in again.", true);
  openAuthModal(false);
};

// ============================================================
// GRIEVANCES DATA LOADING & RENDERING
// ============================================================

async function loadGrievances() {
  if (!getToken()) {
    complaintTable.innerHTML = `
      <tr>
        <td colspan="5" style="text-align:center;padding:28px;color:#8a94a6;">
          Please <a href="#" id="tableSignInLink" style="color:#2563eb;font-weight:700;">sign in</a> to view your submitted grievances.
        </td>
      </tr>
    `;
    const link = document.getElementById("tableSignInLink");
    if (link) link.addEventListener("click", e => { e.preventDefault(); openAuthModal(false); });
    updateStats([]);
    return;
  }

  complaintTable.innerHTML = `<tr><td colspan="5" style="text-align:center;padding:24px;color:#8a94a6;">Loading grievances...</td></tr>`;

  try {
    const list = await SmartShieldAPI.getMyGrievances();
    loadedGrievances = Array.isArray(list) ? list : [];
    renderTable(loadedGrievances);
    updateStats(loadedGrievances);
  } catch (err) {
    console.error("Failed to load grievances:", err);
    complaintTable.innerHTML = `<tr><td colspan="5" style="text-align:center;padding:24px;color:#dc2626;">Failed to load grievances: ${escapeHtml(err.message)}</td></tr>`;
  }
}

function renderTable(grievances) {
  if (!grievances || grievances.length === 0) {
    complaintTable.innerHTML = `
      <tr>
        <td colspan="5" style="text-align:center;padding:32px;color:#8a94a6;">
          No grievances submitted yet. Click <strong>"＋ Raise a Complaint"</strong> to submit your first issue.
        </td>
      </tr>
    `;
    return;
  }

  complaintTable.innerHTML = "";
  grievances.forEach(g => {
    const tr = document.createElement("tr");
    const searchTerms = `${g.trackingId} ${g.title} ${formatCategory(g.category)} ${g.location}`.toLowerCase();
    tr.dataset.search = searchTerms;
    tr.dataset.status = formatStatus(g.status);
    tr.dataset.trackingId = g.trackingId;

    const sClass = getStatusClass(g.status);
    const pClass = getPriorityClass(g.priority);

    tr.innerHTML = `
      <td>
        <b>${escapeHtml(g.trackingId)}</b>
        <span>${escapeHtml(g.title)}</span>
      </td>
      <td>${escapeHtml(formatCategory(g.category))}</td>
      <td>${escapeHtml(formatDate(g.createdAt))}</td>
      <td><span class="status ${sClass}">${escapeHtml(formatStatus(g.status))}</span></td>
      <td><span class="priority ${pClass}">${escapeHtml(formatPriority(g.priority))}</span></td>
    `;

    tr.addEventListener("click", () => {
      openGrievanceDetails(g.trackingId);
    });

    complaintTable.appendChild(tr);
  });

  filterRows();
}

function updateStats(grievances) {
  const list = grievances || [];
  const total = list.length;
  const inProgress = list.filter(g => g.status === "IN_REVIEW").length;
  const resolved = list.filter(g => g.status === "RESOLVED").length;
  const highPriority = list.filter(g => g.priority === "HIGH" || g.priority === "CRITICAL").length;

  if (totalCountEl) totalCountEl.textContent = String(total).padStart(2, "0");
  if (progressCountEl) progressCountEl.textContent = String(inProgress).padStart(2, "0");
  if (resolvedCountEl) resolvedCountEl.textContent = String(resolved).padStart(2, "0");
  if (priorityCountEl) priorityCountEl.textContent = String(highPriority).padStart(2, "0");
}

// ============================================================
// SEARCH & FILTER
// ============================================================

function filterRows() {
  const q = searchInput.value.toLowerCase().trim();
  const f = statusFilter.value;
  document.querySelectorAll("#complaintTable tr").forEach(r => {
    if (!r.dataset.search) return; // skip header or empty state
    const matchesSearch = !q || r.dataset.search.includes(q);
    const matchesStatus = f === "all" || r.dataset.status === f;
    r.style.display = matchesSearch && matchesStatus ? "" : "none";
  });
}

searchInput.addEventListener("input", filterRows);
statusFilter.addEventListener("change", filterRows);

searchInput.addEventListener("keydown", async e => {
  if (e.key === "Enter") {
    const val = searchInput.value.trim().toUpperCase();
    if (val.startsWith("SS-")) {
      e.preventDefault();
      await openGrievanceDetails(val);
    }
  }
});

document.getElementById("viewAll").addEventListener("click", () => {
  searchInput.value = "";
  statusFilter.value = "all";
  filterRows();
  document.getElementById("complaints").scrollIntoView({ behavior: "smooth" });
});

// ============================================================
// GRIEVANCE SUBMISSION
// ============================================================

complaintForm.addEventListener("submit", async e => {
  e.preventDefault();

  if (!getToken()) {
    showToast("Please sign in before submitting a complaint.", true);
    openAuthModal(false);
    return;
  }

  const category = document.getElementById("category").value;
  const prioritySelect = document.getElementById("priority");
  const priority = prioritySelect ? prioritySelect.value : "MEDIUM";
  const title = document.getElementById("title").value.trim();
  const location = document.getElementById("location").value.trim();
  const description = document.getElementById("description").value.trim();
  const evidenceInput = document.getElementById("evidence");
  const evidenceFile = (evidenceInput && evidenceInput.files) ? evidenceInput.files[0] : null;

  if (!category || !title || !location || !description) {
    showToast("Please fill in all required fields.", true);
    return;
  }

  const submitBtn = document.getElementById("submitComplaintBtn") || complaintForm.querySelector("button[type=submit]");
  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.textContent = "Submitting to Backend...";
  }

  try {
    const grievancePayload = {
      category,
      priority,
      title,
      location,
      description
    };

    const createdGrievance = await SmartShieldAPI.createGrievance(grievancePayload);
    const trackingId = createdGrievance.trackingId;

    // Upload evidence if selected
    if (evidenceFile) {
      try {
        if (submitBtn) submitBtn.textContent = "Uploading Evidence...";
        await SmartShieldAPI.uploadEvidence(trackingId, evidenceFile);
      } catch (uploadErr) {
        console.warn("Evidence upload failed:", uploadErr);
        showToast(`Complaint filed (${trackingId}), but evidence upload failed: ${uploadErr.message}`, true);
      }
    }

    complaintForm.reset();
    complaintModal.classList.remove("show");
    showToast(`✓ Complaint submitted successfully! Tracking ID: ${trackingId}`);

    // Refresh list from backend
    await loadGrievances();

    // Optionally auto-open the newly created grievance details
    setTimeout(() => {
      openGrievanceDetails(trackingId);
    }, 500);

  } catch (err) {
    console.error("Grievance submission error:", err);
    showToast(`Submission failed: ${err.message}`, true);
  } finally {
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.textContent = "Submit Complaint →";
    }
  }
});

// ============================================================
// DETAILS, STATUS HISTORY & EVIDENCE METADATA
// ============================================================

async function openGrievanceDetails(trackingId) {
  if (!getToken()) {
    showToast("Please sign in to view grievance details.", true);
    openAuthModal(false);
    return;
  }

  try {
    activeTrackingId = trackingId;
    detailsTrackingId.textContent = trackingId;
    detailsTitle.textContent = "Loading details...";
    detailsSubmitted.textContent = "Fetching latest state...";
    historyList.innerHTML = `<p style="color:#8a94a6;font-size:10px;margin:0;">Loading status history...</p>`;
    evidenceList.innerHTML = `<p style="color:#8a94a6;font-size:10px;margin:0;">Loading evidence metadata...</p>`;
    detailsModal.classList.add("show");

    // Fetch in parallel
    const [grievance, history, evidence] = await Promise.all([
      SmartShieldAPI.getGrievanceByTrackingId(trackingId),
      SmartShieldAPI.getGrievanceHistory(trackingId).catch(() => []),
      SmartShieldAPI.getEvidenceMetadata(trackingId).catch(() => [])
    ]);

    // Populate grievance info
    detailsTitle.textContent = grievance.title || "Grievance Details";
    detailsSubmitted.textContent = `Submitted on ${formatDateTime(grievance.createdAt)}`;
    detailsCategory.textContent = formatCategory(grievance.category);
    detailsLocation.textContent = grievance.location || "--";
    detailsDescription.textContent = grievance.description || "No description provided.";

    const sClass = getStatusClass(grievance.status);
    const pClass = getPriorityClass(grievance.priority);
    detailsStatus.innerHTML = `<span class="status ${sClass}">${escapeHtml(formatStatus(grievance.status))}</span>`;
    detailsPriority.innerHTML = `<span class="priority ${pClass}">${escapeHtml(formatPriority(grievance.priority))}</span>`;

    // Populate status history
    renderStatusHistory(history);

    // Populate evidence metadata
    renderEvidenceList(evidence);

  } catch (err) {
    console.error("Failed to fetch grievance details:", err);
    detailsModal.classList.remove("show");
    showToast(`Could not load grievance ${trackingId}: ${err.message}`, true);
  }
}

function renderStatusHistory(history) {
  if (!history || history.length === 0) {
    historyList.innerHTML = `<p style="color:#8a94a6;font-size:10px;margin:0;">No status updates recorded yet.</p>`;
    return;
  }

  historyList.innerHTML = "";
  // Sort or display chronologically
  history.forEach((h, index) => {
    const item = document.createElement("div");
    item.className = "timeline-item";

    const oldStr = h.oldStatus ? formatStatus(h.oldStatus) : "Initial";
    const newStr = formatStatus(h.newStatus);
    const byStr = h.changedBy || "SYSTEM";
    const dateStr = formatDateTime(h.changedAt);
    const remarksStr = h.remarks ? escapeHtml(h.remarks) : "No remarks";

    item.innerHTML = `
      <div class="timeline-header">
        <strong>${escapeHtml(oldStr)} → <span class="status ${getStatusClass(h.newStatus)}">${escapeHtml(newStr)}</span></strong>
        <span>${escapeHtml(dateStr)}</span>
      </div>
      <div class="timeline-body">
        <div><strong>Updated by:</strong> ${escapeHtml(byStr)}</div>
        <div style="margin-top:2px;"><strong>Remarks:</strong> ${remarksStr}</div>
      </div>
    `;
    historyList.appendChild(item);
  });
}

function renderEvidenceList(evidence) {
  if (!evidence || evidence.length === 0) {
    evidenceList.innerHTML = `<p style="color:#8a94a6;font-size:10px;margin:0;">No evidence files attached to this grievance.</p>`;
    return;
  }

  evidenceList.innerHTML = "";
  evidence.forEach(ev => {
    const item = document.createElement("div");
    item.className = "evidence-item";

    // Safe metadata only - never display internal filePath or storedFileName
    const name = escapeHtml(ev.originalFileName || "File");
    const mime = escapeHtml(ev.contentType || "Unknown");
    const size = formatBytes(ev.fileSize);
    const uploaded = formatDateTime(ev.uploadedAt);

    item.innerHTML = `
      <div>
        <strong>📎 ${name}</strong>
        <div style="font-size:9px;color:#8a94a6;margin-top:2px;">${mime} • ${size} • Uploaded ${uploaded}</div>
      </div>
      <span class="pill" style="font-size:8px;background:#f1f5f9;color:#475569;">Verified</span>
    `;
    evidenceList.appendChild(item);
  });
}

// Attach additional evidence from inside Details modal
if (attachEvidenceForm) {
  attachEvidenceForm.addEventListener("submit", async e => {
    e.preventDefault();
    if (!activeTrackingId) return;

    const file = attachEvidenceFile.files[0];
    if (!file) {
      showToast("Please select a file to attach.", true);
      return;
    }

    const btn = document.getElementById("attachEvidenceBtn");
    if (btn) {
      btn.disabled = true;
      btn.textContent = "Uploading...";
    }

    try {
      await SmartShieldAPI.uploadEvidence(activeTrackingId, file);
      showToast("✓ Evidence attached successfully!");
      attachEvidenceForm.reset();

      // Refresh evidence metadata list
      const updatedEvidence = await SmartShieldAPI.getEvidenceMetadata(activeTrackingId);
      renderEvidenceList(updatedEvidence);

    } catch (err) {
      console.error("Evidence upload failed:", err);
      showToast(`Upload failed: ${err.message}`, true);
    } finally {
      if (btn) {
        btn.disabled = false;
        btn.textContent = "Attach Evidence";
      }
    }
  });
}

// Side Action Demonstrations
document.getElementById("sosBtn").addEventListener("click", () => {
  showToast("Emergency SOS: Integrated with Smart Shield SOS module.");
});
document.getElementById("assistantBtn").addEventListener("click", () => {
  showToast("AI Legal Assistant: Available in the main Smart Shield portal.");
});

// Sidebar Mobile Toggle
const sidebar = document.getElementById("sidebar");
const menuBtn = document.getElementById("menuBtn");
if (menuBtn && sidebar) {
  menuBtn.addEventListener("click", () => sidebar.classList.toggle("open"));
  document.querySelectorAll(".nav-item").forEach(a => {
    a.addEventListener("click", () => sidebar.classList.remove("open"));
  });
}

// ============================================================
// INITIALIZATION
// ============================================================

function handleHashRouting() {
  const hash = window.location.hash;
  const search = window.location.search;
  if (hash === "#raise" || search.includes("action=raise")) {
    openComplaint();
  } else if (hash === "#login" || search.includes("auth=login")) {
    if (!getToken()) {
      openAuthModal(false);
    }
  }
}

document.addEventListener("DOMContentLoaded", () => {
  updateAuthUI();
  loadGrievances();
  handleHashRouting();
});

window.addEventListener("hashchange", handleHashRouting);
