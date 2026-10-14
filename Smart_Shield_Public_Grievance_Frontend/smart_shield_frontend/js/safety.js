/**
 * Smart Shield — Women Safety & Emergency SOS Controller
 * Connects frontend UI to live backend REST APIs.
 * Enforces zero-trust input sanitization, real geolocation, and honest reporting.
 */

// DOM Elements
const authBtn = document.getElementById("authBtn");
const authModal = document.getElementById("authModal");
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

// User Profile Elements
const userAvatar = document.getElementById("userAvatar");
const userName = document.getElementById("userName");
const userRole = document.getElementById("userRole");
const welcomeGreeting = document.getElementById("welcomeGreeting");

// SOS UI Elements
const mainSosBtn = document.getElementById("mainSosBtn");
const triggerSosDirectBtn = document.getElementById("triggerSosDirectBtn");
const sosConfirmModal = document.getElementById("sosConfirmModal");
const cancelSosConfirmBtn = document.getElementById("cancelSosConfirmBtn");
const confirmSosBtn = document.getElementById("confirmSosBtn");
const sosMessageInput = document.getElementById("sosMessageInput");

// Dispatch Overlay Elements
const dispatchOverlay = document.getElementById("dispatchOverlay");
const dispatchTitle = document.getElementById("dispatchTitle");
const dispatchStatusText = document.getElementById("dispatchStatusText");
const dispatchIcon = document.getElementById("dispatchIcon");
const stepLocation = document.getElementById("stepLocation");
const stepRegister = document.getElementById("stepRegister");
const stepNotify = document.getElementById("stepNotify");
const dispatchSuccessActions = document.getElementById("dispatchSuccessActions");
const dispatchErrorActions = document.getElementById("dispatchErrorActions");
const closeDispatchBtn = document.getElementById("closeDispatchBtn");
const retryDispatchBtn = document.getElementById("retryDispatchBtn");
const dismissDispatchBtn = document.getElementById("dismissDispatchBtn");

// Geolocation Banner Elements
const geoDot = document.getElementById("geoDot");
const geoStatusText = document.getElementById("geoStatusText");
const geoSubText = document.getElementById("geoSubText");
const requestGeoBtn = document.getElementById("requestGeoBtn");

// Latest Alert Monitor
const latestAlertPanel = document.getElementById("latestAlertPanel");
const latestAlertStatus = document.getElementById("latestAlertStatus");
const latestAlertTime = document.getElementById("latestAlertTime");
const latestLat = document.getElementById("latestLat");
const latestLng = document.getElementById("latestLng");
const latestAcc = document.getElementById("latestAcc");
const latestNotifStatus = document.getElementById("latestNotifStatus");
const latestNotifDetails = document.getElementById("latestNotifDetails");
const latestMapLink = document.getElementById("latestMapLink");

// Contacts Elements
const contactsContainer = document.getElementById("contactsContainer");
const addContactBtn = document.getElementById("addContactBtn");
const emptyAddContactBtn = document.getElementById("emptyAddContactBtn");
const contactModal = document.getElementById("contactModal");
const contactForm = document.getElementById("contactForm");
const contactEditId = document.getElementById("contactEditId");
const contactName = document.getElementById("contactName");
const contactPhone = document.getElementById("contactPhone");
const contactRelationship = document.getElementById("contactRelationship");
const contactEmail = document.getElementById("contactEmail");
const contactActive = document.getElementById("contactActive");
const contactModalTitle = document.getElementById("contactModalTitle");
const saveContactBtn = document.getElementById("saveContactBtn");

// Telegram Modal Elements
const telegramModal = document.getElementById("telegramModal");
const telegramModalTitle = document.getElementById("telegramModalTitle");
const telegramModalSub = document.getElementById("telegramModalSub");
const telegramConnectingState = document.getElementById("telegramConnectingState");
const telegramConnectedState = document.getElementById("telegramConnectedState");
const telegramBotUsernameDisplay = document.getElementById("telegramBotUsernameDisplay");
const telegramCodeDisplay = document.getElementById("telegramCodeDisplay");
const telegramExpiryCountdown = document.getElementById("telegramExpiryCountdown");
const telegramDeepLinkBtn = document.getElementById("telegramDeepLinkBtn");
const telegramCopyCodeBtn = document.getElementById("telegramCopyCodeBtn");
const telegramPollingStatus = document.getElementById("telegramPollingStatus");
const telegramCheckStatusBtn = document.getElementById("telegramCheckStatusBtn");
const telegramDisconnectBtn = document.getElementById("telegramDisconnectBtn");
let activeTelegramContactId = null;
let telegramPollTimer = null;
let telegramCountdownTimer = null;

// Alerts History Elements
const alertsHistoryContainer = document.getElementById("alertsHistoryContainer");
const refreshAlertsBtn = document.getElementById("refreshAlertsBtn");

// Toast Element
const toast = document.getElementById("toast");

// In-memory data caches
let contactsList = [];
let alertsList = [];
let currentPosition = null;

// ============================================================
// UTILITY FUNCTIONS & SANITIZATION
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

function formatDate(isoStr) {
  if (!isoStr) return "--";
  try {
    const d = new Date(isoStr);
    return d.toLocaleString([], {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit"
    });
  } catch (e) {
    return isoStr;
  }
}

// ============================================================
// USER SESSION & AUTHENTICATION
// ============================================================

function updateAuthUI() {
  const token = getToken();
  const user = getUser();

  if (token && user) {
    userName.textContent = user.fullName || "Citizen";
    userRole.textContent = user.role || "Citizen";
    const initials = (user.fullName || "C")
      .split(" ")
      .map(p => p[0])
      .join("")
      .substring(0, 2)
      .toUpperCase();
    userAvatar.textContent = initials;
    authBtn.innerHTML = "↪ &nbsp; Logout";
    welcomeGreeting.textContent = `Emergency Protection Active: ${user.fullName}`;
  } else {
    userName.textContent = "Guest Citizen";
    userRole.textContent = "Sign in for protection";
    userAvatar.textContent = "G";
    authBtn.innerHTML = "🔑 &nbsp; Sign In";
    welcomeGreeting.textContent = "Women Safety & SOS Center";
  }
}

function checkAuthentication(actionDescription) {
  if (!getToken()) {
    showToast(`Please sign in to ${actionDescription}.`, true);
    openAuthModal(false);
    return false;
  }
  return true;
}

// ============================================================
// BROWSER GEOLOCATION DIAGNOSTICS
// ============================================================

function initGeolocationDiagnostic() {
  if (!("geolocation" in navigator)) {
    geoDot.className = "geo-dot denied";
    geoStatusText.textContent = "Geolocation API Unsupported";
    geoSubText.textContent = "Your browser does not support HTML5 Geolocation. SOS coordinates cannot be captured.";
    return;
  }

  if (navigator.permissions && navigator.permissions.query) {
    navigator.permissions.query({ name: "geolocation" }).then(result => {
      updateGeoState(result.state);
      result.onchange = () => updateGeoState(result.state);
    }).catch(() => {
      geoDot.className = "geo-dot";
      geoStatusText.textContent = "Location Permission Ready";
      geoSubText.textContent = "Browser will prompt for location when SOS is activated.";
    });
  } else {
    geoDot.className = "geo-dot";
    geoStatusText.textContent = "Location Service Ready";
    geoSubText.textContent = "Click 'Verify Location' to test GPS connectivity.";
  }
}

function updateGeoState(state) {
  if (state === "granted") {
    geoDot.className = "geo-dot granted";
    geoStatusText.textContent = "Location Access: Granted";
    geoSubText.textContent = "Browser GPS is authorized. Emergency alerts will include precise coordinates.";
  } else if (state === "denied") {
    geoDot.className = "geo-dot denied";
    geoStatusText.textContent = "Location Access: Denied";
    geoSubText.textContent = "Please click the lock icon in your browser address bar to allow Location access.";
  } else {
    geoDot.className = "geo-dot";
    geoStatusText.textContent = "Location Access: Not Yet Prompted";
    geoSubText.textContent = "Permission will be requested when you trigger an emergency SOS alert.";
  }
}

requestGeoBtn.addEventListener("click", () => {
  geoStatusText.textContent = "Testing GPS Location...";
  geoSubText.textContent = "Requesting current position from device sensors...";

  navigator.geolocation.getCurrentPosition(
    pos => {
      currentPosition = pos;
      geoDot.className = "geo-dot granted";
      geoStatusText.textContent = "GPS Verified Successfully!";
      geoSubText.textContent = `Latitude: ${pos.coords.latitude.toFixed(4)}, Longitude: ${pos.coords.longitude.toFixed(4)} (±${Math.round(pos.coords.accuracy)}m)`;
      showToast("GPS coordinates verified successfully!");
    },
    err => {
      geoDot.className = "geo-dot denied";
      let msg = "Could not obtain location.";
      if (err.code === 1) msg = "Location permission denied by user.";
      else if (err.code === 2) msg = "Device location unavailable.";
      else if (err.code === 3) msg = "Location request timed out.";
      geoStatusText.textContent = "GPS Check Failed";
      geoSubText.textContent = msg;
      showToast(msg, true);
    },
    { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
  );
});

// ============================================================
// EMERGENCY CONTACTS CRUD
// ============================================================

async function loadContacts() {
  if (!getToken()) {
    renderContacts([]);
    return;
  }

  try {
    const data = await SmartShieldAPI.getEmergencyContacts();
    contactsList = Array.isArray(data) ? data : [];
    renderContacts(contactsList);
  } catch (err) {
    console.error("Failed to load contacts:", err);
    contactsContainer.innerHTML = `
      <div class="empty-state">
        <p style="color:#ef4444;margin:0;">Failed to load emergency contacts. ${escapeHtml(err.message)}</p>
      </div>`;
  }
}

function renderContacts(contacts) {
  if (!contacts || contacts.length === 0) {
    contactsContainer.innerHTML = `
      <div class="empty-state">
        <div class="empty-state-icon">👥</div>
        <h4>No Emergency Contacts Added</h4>
        <p>Add trusted family members or friends who should receive immediate emergency alerts.</p>
        <button class="primary-btn" id="emptyAddContactBtn" style="background:#18243a;color:#fff;">
          ＋ Add First Contact
        </button>
      </div>`;
    const emptyBtn = document.getElementById("emptyAddContactBtn");
    if (emptyBtn) {
      emptyBtn.addEventListener("click", () => openContactModal());
    }
    return;
  }

  contactsContainer.innerHTML = contacts.map(c => {
    const initials = escapeHtml(
      (c.name || "C")
        .split(" ")
        .map(p => p[0])
        .join("")
        .substring(0, 2)
        .toUpperCase()
    );
    const activeBadge = c.active
      ? '<span style="font-size:9px;color:#10b981;font-weight:700;">● Active</span>'
      : '<span style="font-size:9px;color:#94a3b8;font-weight:700;">○ Inactive</span>';

    return `
      <div class="contact-card" data-id="${c.id}">
        <div class="contact-info">
          <div class="contact-avatar">${initials}</div>
          <div class="contact-details">
            <h4>
              ${escapeHtml(c.name)}
              <span class="rel-badge">${escapeHtml(c.relationship)}</span>
              ${activeBadge}
            </h4>
            <div class="contact-meta">
              <span>📞 <a href="tel:${escapeHtml(c.phoneNumber)}">${escapeHtml(c.phoneNumber)}</a></span>
              ${c.email ? `<span>✉ <a href="mailto:${escapeHtml(c.email)}">${escapeHtml(c.email)}</a></span>` : ''}
            </div>
            <div class="telegram-meta" style="margin-top:6px;display:flex;align-items:center;gap:8px;">
              <span style="font-size:11px;color:#64748b;">Telegram:</span>
              ${c.telegramConnected
                ? '<span class="telegram-badge-connected">✓ Connected</span>'
                : '<span class="telegram-badge-disconnected">Not Connected</span>'
              }
              ${!c.telegramConnected
                ? `<button class="telegram-connect-btn connect-telegram-btn" data-id="${c.id}"><span>✈</span> Connect Telegram</button>`
                : `<button class="text-btn view-telegram-btn" data-id="${c.id}" style="font-size:10px;padding:0;color:#0284c7;">Manage ⚙</button>`
              }
            </div>
          </div>
        </div>
        <div class="contact-actions">
          <button class="btn-icon edit-contact" data-id="${c.id}" title="Edit Contact">✎</button>
          <button class="btn-icon delete delete-contact" data-id="${c.id}" title="Delete Contact">🗑</button>
        </div>
      </div>
    `;
  }).join("");

  // Attach event listeners to edit and delete buttons
  contactsContainer.querySelectorAll(".edit-contact").forEach(btn => {
    btn.addEventListener("click", () => {
      const id = btn.getAttribute("data-id");
      const contact = contactsList.find(c => String(c.id) === String(id));
      if (contact) openContactModal(contact);
    });
  });

  contactsContainer.querySelectorAll(".delete-contact").forEach(btn => {
    btn.addEventListener("click", () => {
      const id = btn.getAttribute("data-id");
      const contact = contactsList.find(c => String(c.id) === String(id));
      if (contact) confirmDeleteContact(contact);
    });
  });

  // Attach event listeners to Telegram connect/manage buttons
  contactsContainer.querySelectorAll(".connect-telegram-btn, .view-telegram-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      const id = btn.getAttribute("data-id");
      const contact = contactsList.find(c => String(c.id) === String(id));
      if (contact) openTelegramModal(contact);
    });
  });
}

function openContactModal(contact = null) {
  if (!checkAuthentication("manage emergency contacts")) return;

  contactForm.reset();
  if (contact) {
    contactModalTitle.textContent = "Edit Emergency Contact";
    contactEditId.value = contact.id;
    contactName.value = contact.name || "";
    contactPhone.value = contact.phoneNumber || "";
    contactRelationship.value = contact.relationship || "";
    contactEmail.value = contact.email || "";
    contactActive.checked = contact.active !== false;
    saveContactBtn.textContent = "Update Contact →";
  } else {
    contactModalTitle.textContent = "Add Emergency Contact";
    contactEditId.value = "";
    contactActive.checked = true;
    saveContactBtn.textContent = "Save Emergency Contact →";
  }

  contactModal.classList.add("show");
}

async function confirmDeleteContact(contact) {
  if (!confirm(`Are you sure you want to remove "${contact.name}" from your emergency contacts?`)) {
    return;
  }

  try {
    await SmartShieldAPI.deleteEmergencyContact(contact.id);
    showToast("Emergency contact deleted successfully.");
    await loadContacts();
  } catch (err) {
    showToast("Failed to delete contact: " + err.message, true);
  }
}

contactForm.addEventListener("submit", async e => {
  e.preventDefault();
  const id = contactEditId.value;
  const payload = {
    name: contactName.value.trim(),
    phoneNumber: contactPhone.value.trim(),
    relationship: contactRelationship.value,
    email: contactEmail.value.trim() || null,
    active: contactActive.checked
  };

  saveContactBtn.disabled = true;
  saveContactBtn.textContent = "Saving...";

  try {
    if (id) {
      await SmartShieldAPI.updateEmergencyContact(id, payload);
      showToast("Emergency contact updated successfully.");
    } else {
      await SmartShieldAPI.createEmergencyContact(payload);
      showToast("Emergency contact added successfully.");
    }
    contactModal.classList.remove("show");
    await loadContacts();
  } catch (err) {
    showToast("Failed to save contact: " + err.message, true);
  } finally {
    saveContactBtn.disabled = false;
    saveContactBtn.textContent = id ? "Update Contact →" : "Save Emergency Contact →";
  }
});

// ============================================================
// TELEGRAM CONNECTION MODAL HANDLERS
// ============================================================

async function openTelegramModal(contact) {
  if (!checkAuthentication("connect Telegram for emergency alerts")) return;

  activeTelegramContactId = contact.id;
  telegramModalTitle.textContent = `Connect Telegram: ${contact.name}`;
  telegramModal.classList.add("show");

  // Check initial status
  try {
    const status = await SmartShieldAPI.getTelegramStatus(contact.id);
    if (status.telegramConnected) {
      showTelegramConnectedState(status);
    } else {
      const connectData = await SmartShieldAPI.connectTelegram(contact.id);
      showTelegramConnectingState(connectData);
    }
  } catch (err) {
    showToast("Failed to load Telegram connection details: " + err.message, true);
  }
}

function showTelegramConnectedState(data) {
  stopTelegramTimers();
  telegramConnectingState.style.display = "none";
  telegramConnectedState.style.display = "block";
}

function showTelegramConnectingState(data) {
  stopTelegramTimers();
  telegramConnectingState.style.display = "block";
  telegramConnectedState.style.display = "none";

  const code = data.connectionCode || "SH-......";
  telegramCodeDisplay.textContent = code;

  const botUser = data.botUsername || "SmartShieldAlertBot";
  telegramBotUsernameDisplay.textContent = "@" + botUser;

  const deepLink = data.botDeepLink || `https://t.me/${botUser}?start=${code}`;
  telegramDeepLinkBtn.href = deepLink;

  telegramPollingStatus.textContent = "⏳ Waiting for contact to send code...";

  let targetTime = data.expiresAt ? new Date(data.expiresAt).getTime() : (Date.now() + 10 * 60 * 1000);
  updateCountdown(targetTime);
  telegramCountdownTimer = setInterval(() => updateCountdown(targetTime), 1000);

  // Poll status every 3.5s while modal is active
  telegramPollTimer = setInterval(pollTelegramStatus, 3500);
}

function updateCountdown(targetTime) {
  const remaining = Math.max(0, Math.floor((targetTime - Date.now()) / 1000));
  if (remaining <= 0) {
    telegramExpiryCountdown.textContent = "Code expired. Re-open to generate a new code.";
    stopTelegramTimers();
    return;
  }
  const mins = Math.floor(remaining / 60);
  const secs = remaining % 60;
  telegramExpiryCountdown.textContent = `Expires in ${mins}:${secs < 10 ? '0' : ''}${secs}`;
}

async function pollTelegramStatus() {
  if (!activeTelegramContactId || !telegramModal.classList.contains("show")) {
    stopTelegramTimers();
    return;
  }

  try {
    const status = await SmartShieldAPI.getTelegramStatus(activeTelegramContactId);
    if (status.telegramConnected) {
      showTelegramConnectedState(status);
      showToast("✓ Telegram connected successfully!");
      await loadContacts();
    }
  } catch (e) {
    console.debug("Telegram status poll:", e.message);
  }
}

function stopTelegramTimers() {
  if (telegramPollTimer) {
    clearInterval(telegramPollTimer);
    telegramPollTimer = null;
  }
  if (telegramCountdownTimer) {
    clearInterval(telegramCountdownTimer);
    telegramCountdownTimer = null;
  }
}

telegramCopyCodeBtn.addEventListener("click", () => {
  const code = telegramCodeDisplay.textContent.trim();
  if (code && code !== "SH-......") {
    navigator.clipboard.writeText(code).then(() => {
      showToast("Connection code copied to clipboard!");
    }).catch(() => {
      showToast("Code: " + code);
    });
  }
});

telegramCheckStatusBtn.addEventListener("click", () => {
  telegramPollingStatus.textContent = "Checking Telegram updates...";
  pollTelegramStatus();
});

telegramDisconnectBtn.addEventListener("click", async () => {
  if (!activeTelegramContactId) return;
  if (!confirm("Are you sure you want to disconnect Telegram for this contact?")) return;

  try {
    await SmartShieldAPI.disconnectTelegram(activeTelegramContactId);
    showToast("Telegram alerts disconnected.");
    telegramModal.classList.remove("show");
    stopTelegramTimers();
    await loadContacts();
  } catch (err) {
    showToast("Failed to disconnect: " + err.message, true);
  }
});

// ============================================================
// SOS BROADCAST FLOW WITH GEOLOCATION
// ============================================================

function openSosConfirmation() {
  if (!checkAuthentication("trigger an emergency SOS")) return;
  sosConfirmModal.classList.add("show");
}

cancelSosConfirmBtn.addEventListener("click", () => {
  sosConfirmModal.classList.remove("show");
});

mainSosBtn.addEventListener("click", openSosConfirmation);
triggerSosDirectBtn.addEventListener("click", openSosConfirmation);

confirmSosBtn.addEventListener("click", () => {
  sosConfirmModal.classList.remove("show");
  startSosDispatchFlow();
});

function resetDispatchSteps() {
  stepLocation.className = "step-item active";
  stepRegister.className = "step-item";
  stepNotify.className = "step-item";
  dispatchIcon.textContent = "🚨";
  dispatchTitle.textContent = "Dispatching Emergency SOS";
  dispatchStatusText.textContent = "Acquiring current GPS location from browser sensors...";
  dispatchSuccessActions.style.display = "none";
  dispatchErrorActions.style.display = "none";
}

function startSosDispatchFlow() {
  resetDispatchSteps();
  dispatchOverlay.classList.add("show");

  const customMessage = sosMessageInput.value.trim() || "Emergency assistance required immediately";

  if (!("geolocation" in navigator)) {
    handleDispatchFailure("Browser Geolocation is not supported on this device.");
    return;
  }

  // Step 1: Capture Browser Geolocation
  navigator.geolocation.getCurrentPosition(
    position => {
      // Step 1 Success
      stepLocation.className = "step-item done";
      stepRegister.className = "step-item active";
      dispatchStatusText.textContent = `Coordinates acquired: ${position.coords.latitude.toFixed(4)}, ${position.coords.longitude.toFixed(4)}. Registering incident with Smart Shield...`;

      sendSosToBackend({
        latitude: position.coords.latitude,
        longitude: position.coords.longitude,
        accuracy: position.coords.accuracy || null,
        source: "BROWSER",
        message: customMessage
      });
    },
    error => {
      let errorReason = "Location access failed.";
      if (error.code === 1) {
        errorReason = "Location permission denied. Responders cannot locate you without GPS access. Please allow location permissions in your browser.";
      } else if (error.code === 2) {
        errorReason = "GPS position unavailable from your device.";
      } else if (error.code === 3) {
        errorReason = "GPS request timed out.";
      }
      handleDispatchFailure(errorReason);
    },
    { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 }
  );
}

async function sendSosToBackend(sosPayload) {
  try {
    // Step 2: POST /api/safety/sos
    const alert = await SmartShieldAPI.triggerSos(sosPayload);

    // Step 2 Success
    stepRegister.className = "step-item done";
    stepNotify.className = "step-item done";

    dispatchIcon.textContent = "✓";
    dispatchTitle.textContent = "Emergency Alert Dispatched!";

    function formatChannelStatus(st) {
      if (!st) return '<span style="color:#64748b;font-weight:700;">Not configured</span>';
      const s = String(st).toUpperCase();
      if (s === "SENT") return '<span style="color:#10b981;font-weight:700;">Sent</span>';
      if (s === "NOT_CONNECTED") return '<span style="color:#d97706;font-weight:700;">Not connected</span>';
      if (s === "FAILED") return '<span style="color:#ef4444;font-weight:700;">Failed</span>';
      return '<span style="color:#64748b;font-weight:700;">Not configured</span>';
    }

    dispatchStatusText.innerHTML = `
      <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;padding:12px;margin:10px 0;text-align:left;font-size:12px;line-height:1.7;">
        <div style="color:#0f172a;font-weight:800;margin-bottom:4px;">SOS Incident ID #${alert.id}</div>
        <div style="color:#10b981;font-weight:700;margin-bottom:6px;">✓ SOS recorded successfully</div>
        <div>• <strong>Admin monitoring:</strong> <span style="color:#10b981;font-weight:700;">active</span></div>
        <div>• <strong>Email:</strong> ${formatChannelStatus(alert.emailStatus)}</div>
        <div>• <strong>Telegram:</strong> ${formatChannelStatus(alert.telegramStatus)}</div>
      </div>
    `;

    dispatchSuccessActions.style.display = "block";
    showToast("✓ Emergency SOS alert registered successfully!");

    // Refresh monitoring panels
    await loadAlerts();
  } catch (err) {
    console.error("SOS dispatch error:", err);
    stepRegister.className = "step-item";
    handleDispatchFailure(err.message || "Failed to register SOS with server.");
  }
}

function handleDispatchFailure(message) {
  dispatchIcon.textContent = "✕";
  dispatchTitle.textContent = "SOS Dispatch Failed";
  dispatchStatusText.innerHTML = `<span style="color:#ef4444;">${escapeHtml(message)}</span>`;
  dispatchErrorActions.style.display = "block";
  showToast(message, true);
}

closeDispatchBtn.addEventListener("click", () => {
  dispatchOverlay.classList.remove("show");
});

dismissDispatchBtn.addEventListener("click", () => {
  dispatchOverlay.classList.remove("show");
});

retryDispatchBtn.addEventListener("click", () => {
  startSosDispatchFlow();
});

// ============================================================
// PAST SOS ALERTS & MONITORING
// ============================================================

async function loadAlerts() {
  if (!getToken()) {
    renderAlerts([]);
    latestAlertPanel.style.display = "none";
    return;
  }

  try {
    const data = await SmartShieldAPI.getMySosAlerts();
    alertsList = Array.isArray(data) ? data : [];
    renderAlerts(alertsList);

    if (alertsList.length > 0) {
      renderLatestAlert(alertsList[0]);
    } else {
      latestAlertPanel.style.display = "none";
    }
  } catch (err) {
    console.error("Failed to load past SOS alerts:", err);
  }
}

function renderLatestAlert(alert) {
  latestAlertPanel.style.display = "block";
  latestAlertStatus.className = `alert-status-badge ${alert.status}`;
  latestAlertStatus.textContent = alert.status || "TRIGGERED";
  latestAlertTime.textContent = `Triggered: ${formatDate(alert.triggeredAt || alert.createdAt)}`;

  latestLat.textContent = alert.latitude != null ? alert.latitude.toFixed(5) : "--";
  latestLng.textContent = alert.longitude != null ? alert.longitude.toFixed(5) : "--";

  const latestEmailStatus = document.getElementById("latestEmailStatus");
  const latestTelegramStatus = document.getElementById("latestTelegramStatus");
  if (latestEmailStatus) {
    const es = (alert.emailStatus || "NOT_CONFIGURED").toUpperCase();
    latestEmailStatus.className = `notification-badge ${es}`;
    latestEmailStatus.textContent = es === "SENT" ? "SENT" : (es === "FAILED" ? "FAILED" : "NOT CONFIGURED");
  }
  if (latestTelegramStatus) {
    const ts = (alert.telegramStatus || "NOT_CONFIGURED").toUpperCase();
    latestTelegramStatus.className = `notification-badge ${ts}`;
    latestTelegramStatus.textContent = ts === "SENT" ? "SENT" : (ts === "NOT_CONNECTED" ? "NOT CONNECTED" : (ts === "FAILED" ? "FAILED" : "NOT CONFIGURED"));
  }

  latestNotifDetails.textContent = alert.notificationDetails || "Alert registered on central protection server.";

  if (alert.latitude && alert.longitude) {
    latestMapLink.href = `https://www.google.com/maps?q=${alert.latitude},${alert.longitude}`;
    latestMapLink.style.display = "inline-block";
  } else {
    latestMapLink.style.display = "none";
  }
}

function renderAlerts(alerts) {
  if (!alerts || alerts.length === 0) {
    alertsHistoryContainer.innerHTML = `
      <p style="text-align:center;color:#8a94a6;font-size:11px;padding:24px;">No emergency alerts recorded.</p>`;
    return;
  }

  alertsHistoryContainer.innerHTML = alerts.map(a => `
    <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;padding:12px;font-size:11px;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;">
        <strong style="color:#0f172a;">Alert #${a.id}</strong>
        <span class="alert-status-badge ${a.status}">${escapeHtml(a.status)}</span>
      </div>
      <div style="color:#64748b;font-size:10px;display:flex;justify-content:space-between;align-items:center;margin-bottom:4px;">
        <span>${formatDate(a.triggeredAt || a.createdAt)}</span>
        <div style="display:flex;align-items:center;gap:4px;">
          <span style="font-size:9px;color:#64748b;">Email:</span>
          <span class="notification-badge ${a.emailStatus || 'NOT_CONFIGURED'}">${escapeHtml(a.emailStatus || 'NOT_CONFIGURED')}</span>
          <span style="font-size:9px;color:#64748b;margin-left:4px;">TG:</span>
          <span class="notification-badge ${a.telegramStatus || 'NOT_CONFIGURED'}">${escapeHtml(a.telegramStatus || 'NOT_CONFIGURED')}</span>
        </div>
      </div>
      <div style="color:#475569;font-size:10px;">
        Location: <strong>${a.latitude.toFixed(4)}, ${a.longitude.toFixed(4)}</strong>
        <a href="https://www.google.com/maps?q=${a.latitude},${a.longitude}" target="_blank" rel="noopener noreferrer" style="color:#2563eb;margin-left:6px;text-decoration:none;">[Map ↗]</a>
      </div>
      ${a.message ? `<p style="margin:4px 0 0;color:#64748b;font-style:italic;">"${escapeHtml(a.message)}"</p>` : ''}
    </div>
  `).join("");
}

refreshAlertsBtn.addEventListener("click", () => {
  loadAlerts();
  showToast("Alerts refreshed.");
});

// ============================================================
// CITIZEN AUTHENTICATION MODAL
// ============================================================

function openAuthModal(signup = false) {
  isSignUpMode = signup;
  authModal.classList.add("show");
  renderAuthModalState();
}

function renderAuthModalState() {
  if (isSignUpMode) {
    authTitle.textContent = "Create Citizen Account";
    authSub.textContent = "Sign up to configure emergency contacts and SOS protection.";
    authNameRow.style.display = "block";
    authFullName.required = true;
    authSubmitBtn.textContent = "Sign Up →";
    authTogglePrompt.textContent = "Already have an account?";
    authToggleBtn.textContent = "Sign In";
  } else {
    authTitle.textContent = "Citizen Sign In";
    authSub.textContent = "Sign in to access your safety center and emergency contacts.";
    authNameRow.style.display = "none";
    authFullName.required = false;
    authSubmitBtn.textContent = "Sign In →";
    authTogglePrompt.textContent = "Don't have an account?";
    authToggleBtn.textContent = "Sign Up";
  }
}

authToggleBtn.addEventListener("click", () => {
  isSignUpMode = !isSignUpMode;
  renderAuthModalState();
});

authBtn.addEventListener("click", () => {
  if (getToken()) {
    clearAuth();
    window.location.replace('../../index.html');
  } else {
    openAuthModal(false);
  }
});

authForm.addEventListener("submit", async e => {
  e.preventDefault();
  const email = authEmail.value.trim();
  const password = authPassword.value;
  const fullName = authFullName.value.trim();

  authSubmitBtn.disabled = true;
  authSubmitBtn.textContent = "Authenticating...";

  try {
    let response = null;
    if (isSignUpMode) {
      response = await SmartShieldAPI.signup(fullName, email, password);
    } else {
      response = await SmartShieldAPI.login(email, password);
    }

    if (response && response.token) {
      setToken(response.token, true);
      setUser({
        id: response.id,
        fullName: response.fullName,
        email: response.email,
        role: response.role
      }, true);

      authModal.classList.remove("show");
      authForm.reset();
      updateAuthUI();
      await loadContacts();
      await loadAlerts();
      showToast(`Welcome back, ${response.fullName}!`);
    } else {
      throw new Error("Invalid authentication response from server.");
    }
  } catch (err) {
    showToast("Authentication failed: " + err.message, true);
  } finally {
    authSubmitBtn.disabled = false;
    authSubmitBtn.textContent = isSignUpMode ? "Sign Up →" : "Sign In →";
  }
});

// Modal close button delegation
document.querySelectorAll("[data-close]").forEach(btn => {
  btn.addEventListener("click", () => {
    stopTelegramTimers();
    document.querySelectorAll(".modal").forEach(m => m.classList.remove("show"));
  });
});

window.addEventListener("click", e => {
  if (e.target.classList.contains("modal")) {
    stopTelegramTimers();
    e.target.classList.remove("show");
  }
});

addContactBtn.addEventListener("click", () => openContactModal());

// Mobile sidebar toggle
const menuBtn = document.getElementById("menuBtn");
const sidebar = document.getElementById("sidebar");
if (menuBtn && sidebar) {
  menuBtn.addEventListener("click", () => {
    sidebar.classList.toggle("open");
  });
}

// ============================================================
// INITIALIZATION
// ============================================================

document.addEventListener("DOMContentLoaded", () => {
  updateAuthUI();
  initGeolocationDiagnostic();
  loadContacts();
  loadAlerts();

  // If redirected with ?trigger=true, prompt SOS immediately
  const params = new URLSearchParams(window.location.search);
  if (params.get("trigger") === "true") {
    setTimeout(openSosConfirmation, 400);
  }
});
