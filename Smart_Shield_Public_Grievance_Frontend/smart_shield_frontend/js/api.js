/**
 * Smart Shield - Authenticated API & Session Helper
 * Manages citizen JWT authentication and connects frontend to backend APIs.
 */

const API_BASE_URL = 'http://localhost:8082';
const TOKEN_KEY = 'smartshield_citizen_token';
const USER_KEY = 'smartshield_citizen_user';
const ADMIN_TOKEN_KEY = 'smartshield_admin_token';
const ADMIN_USER_KEY = 'smartshield_admin_user';

function getToken() {
  try {
    return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY) || null;
  } catch (e) {
    return null;
  }
}

function setToken(token, persist = true) {
  try {
    if (persist) {
      localStorage.setItem(TOKEN_KEY, token);
    } else {
      sessionStorage.setItem(TOKEN_KEY, token);
    }
  } catch (e) {
    console.error("Storage error:", e);
  }
}

function clearAuth() {
  try {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
  } catch (e) {
    console.error("Storage clear error:", e);
  }
}

function getUser() {
  try {
    const raw = localStorage.getItem(USER_KEY) || sessionStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch (e) {
    return null;
  }
}

function setUser(user, persist = true) {
  try {
    const raw = JSON.stringify(user);
    if (persist) {
      localStorage.setItem(USER_KEY, raw);
    } else {
      sessionStorage.setItem(USER_KEY, raw);
    }
  } catch (e) {
    console.error("User storage error:", e);
  }
}

// Admin / Developer Authentication Helpers (Strictly separated from Citizen state)
function getAdminToken() {
  try {
    return localStorage.getItem(ADMIN_TOKEN_KEY) || sessionStorage.getItem(ADMIN_TOKEN_KEY) || null;
  } catch (e) {
    return null;
  }
}

function setAdminToken(token, persist = true) {
  try {
    if (persist) {
      localStorage.setItem(ADMIN_TOKEN_KEY, token);
    } else {
      sessionStorage.setItem(ADMIN_TOKEN_KEY, token);
    }
  } catch (e) {
    console.error("Admin storage error:", e);
  }
}

function clearAdminAuth() {
  try {
    localStorage.removeItem(ADMIN_TOKEN_KEY);
    localStorage.removeItem(ADMIN_USER_KEY);
    sessionStorage.removeItem(ADMIN_TOKEN_KEY);
    sessionStorage.removeItem(ADMIN_USER_KEY);
  } catch (e) {
    console.error("Admin storage clear error:", e);
  }
}

async function apiFetch(endpoint, options = {}) {
  const url = `${API_BASE_URL}${endpoint}`;
  const headers = Object.assign({}, options.headers || {});

  // Enforce zero-trust token separation: admin endpoints require admin token, citizen endpoints require citizen token
  const isAdminEndpoint = endpoint.startsWith('/api/admin/');
  const token = isAdminEndpoint ? getAdminToken() : getToken();
  if (token && !headers['Authorization']) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  if (!headers['Content-Type'] && !(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(url, {
    ...options,
    headers
  });

  if (response.status === 401) {
    console.warn("API 401: Unauthorized access to " + endpoint);
    if (isAdminEndpoint) {
      if (typeof onAdminAuthExpired === 'function') {
        onAdminAuthExpired();
      }
      throw new Error("Admin authentication required or session expired.");
    } else {
      if (typeof onAuthExpired === 'function') {
        onAuthExpired();
      }
      throw new Error("Authentication required or session expired.");
    }
  }

  let data = null;
  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    try {
      data = await response.json();
    } catch (err) {
      data = null;
    }
  } else {
    data = await response.text();
  }

  if (!response.ok) {
    let errorMessage = (data && data.message) ? data.message : `Request failed with status ${response.status}`;
    if (data && data.validationErrors) {
      const details = Object.entries(data.validationErrors).map(([f, m]) => `${f}: ${m}`).join(', ');
      errorMessage += ` (${details})`;
    }
    const error = new Error(errorMessage);
    error.status = response.status;
    error.data = data;
    throw error;
  }

  return data;
}

async function apiUpload(endpoint, file) {
  const url = `${API_BASE_URL}${endpoint}`;
  const token = getToken();

  const formData = new FormData();
  formData.append('file', file);

  const headers = {};
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  // IMPORTANT: Do NOT set Content-Type header so browser sets multipart boundary

  const response = await fetch(url, {
    method: 'POST',
    headers,
    body: formData
  });

  if (response.status === 401) {
    if (typeof onAuthExpired === 'function') {
      onAuthExpired();
    }
    throw new Error("Authentication required or session expired.");
  }

  let data = null;
  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    try {
      data = await response.json();
    } catch (err) {
      data = null;
    }
  } else {
    data = await response.text();
  }

  if (!response.ok) {
    const errorMessage = (data && data.message) ? data.message : `Upload failed with status ${response.status}`;
    const error = new Error(errorMessage);
    error.status = response.status;
    error.data = data;
    throw error;
  }

  return data;
}

// Higher level API functions
const SmartShieldAPI = {
  // Citizen Auth
  signup: (fullName, email, password) =>
    apiFetch('/api/auth/signup', {
      method: 'POST',
      body: JSON.stringify({ fullName, email, password })
    }),

  login: (email, password) =>
    apiFetch('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    }),

  // Grievance Operations
  createGrievance: (grievance) =>
    apiFetch('/api/grievances', {
      method: 'POST',
      body: JSON.stringify(grievance)
    }),

  getMyGrievances: () =>
    apiFetch('/api/grievances/my', {
      method: 'GET'
    }),

  getGrievanceByTrackingId: (trackingId) =>
    apiFetch(`/api/grievances/${encodeURIComponent(trackingId)}`, {
      method: 'GET'
    }),

  getGrievanceHistory: (trackingId) =>
    apiFetch(`/api/grievances/${encodeURIComponent(trackingId)}/history`, {
      method: 'GET'
    }),

  uploadEvidence: (trackingId, file) =>
    apiUpload(`/api/grievances/${encodeURIComponent(trackingId)}/evidence`, file),

  getEvidenceMetadata: (trackingId) =>
    apiFetch(`/api/grievances/${encodeURIComponent(trackingId)}/evidence`, {
      method: 'GET'
    }),

  // Women Safety & Emergency Contacts (Phase 3)
  getEmergencyContacts: () =>
    apiFetch('/api/safety/emergency-contacts', {
      method: 'GET'
    }),

  createEmergencyContact: (contact) =>
    apiFetch('/api/safety/emergency-contacts', {
      method: 'POST',
      body: JSON.stringify(contact)
    }),

  updateEmergencyContact: (id, contact) =>
    apiFetch(`/api/safety/emergency-contacts/${encodeURIComponent(id)}`, {
      method: 'PUT',
      body: JSON.stringify(contact)
    }),

  deleteEmergencyContact: (id) =>
    apiFetch(`/api/safety/emergency-contacts/${encodeURIComponent(id)}`, {
      method: 'DELETE'
    }),

  connectTelegram: (id) =>
    apiFetch(`/api/safety/emergency-contacts/${encodeURIComponent(id)}/telegram/connect`, {
      method: 'POST'
    }),

  getTelegramStatus: (id) =>
    apiFetch(`/api/safety/emergency-contacts/${encodeURIComponent(id)}/telegram/status`, {
      method: 'GET'
    }),

  verifyTelegramConnection: (id, code, chatId) =>
    apiFetch(`/api/safety/emergency-contacts/${encodeURIComponent(id)}/telegram/verify`, {
      method: 'POST',
      body: JSON.stringify({ code, chatId })
    }),

  disconnectTelegram: (id) =>
    apiFetch(`/api/safety/emergency-contacts/${encodeURIComponent(id)}/telegram/disconnect`, {
      method: 'POST'
    }),

  triggerSos: (sosData) =>
    apiFetch('/api/safety/sos', {
      method: 'POST',
      body: JSON.stringify(sosData)
    }),

  getMySosAlerts: () =>
    apiFetch('/api/safety/sos/my', {
      method: 'GET'
    }),

  getSosAlertById: (id) =>
    apiFetch(`/api/safety/sos/${encodeURIComponent(id)}`, {
      method: 'GET'
    }),

  // Safe Routes (Module 4)
  getSafeRouteConfig: () =>
    apiFetch('/api/safety/routes/config', {
      method: 'GET'
    }),

  resolveLocation: (lat, lng) =>
    apiFetch(`/api/safety/routes/resolve-location?lat=${encodeURIComponent(lat)}&lng=${encodeURIComponent(lng)}`, {
      method: 'GET'
    }),

  getDestinationSuggestions: (city, query, lat, lng) => {
    let url = `/api/safety/routes/destinations?city=${encodeURIComponent(city || '')}&query=${encodeURIComponent(query || '')}`;
    if (lat != null && lng != null) {
      url += `&lat=${encodeURIComponent(lat)}&lng=${encodeURIComponent(lng)}`;
    }
    return apiFetch(url, { method: 'GET' });
  },

  calculateSafeRoute: (routeData) =>
    apiFetch('/api/safety/routes/calculate', {
      method: 'POST',
      body: JSON.stringify(routeData)
    }),

  getSafeRoute: (safeRouteId) =>
    apiFetch(`/api/safety/routes/${encodeURIComponent(safeRouteId)}`, {
      method: 'GET'
    }),

  submitRouteRating: (safeRouteId, ratingData) =>
    apiFetch(`/api/safety/routes/${encodeURIComponent(safeRouteId)}/rating`, {
      method: 'POST',
      body: JSON.stringify(ratingData)
    }),

  getRouteRatings: (safeRouteId) =>
    apiFetch(`/api/safety/routes/${encodeURIComponent(safeRouteId)}/ratings`, {
      method: 'GET'
    }),

  // Gemini AI Civic + Legal Assistant (Module 5)
  getAiStatus: () =>
    apiFetch('/api/ai/status', {
      method: 'GET'
    }),

  sendAiChatMessage: (chatData) =>
    apiFetch('/api/ai/chat', {
      method: 'POST',
      body: JSON.stringify(chatData)
    }),

  // Government Form Scanner (Module 6)
  getFormScannerStatus: () =>
    apiFetch('/api/forms/status', {
      method: 'GET'
    }),

  scanGovernmentForm: async (file, instruction = '') => {
    const url = `${API_BASE_URL}/api/forms/scan`;
    const token = getToken();

    const formData = new FormData();
    formData.append('file', file);
    if (instruction && instruction.trim()) {
      formData.append('instruction', instruction.trim());
    }

    const headers = {};
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(url, {
      method: 'POST',
      headers,
      body: formData
    });

    if (response.status === 401) {
      if (typeof onAuthExpired === 'function') {
        onAuthExpired();
      }
      throw new Error("Authentication required or session expired.");
    }

    let data = null;
    const contentType = response.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
      try {
        data = await response.json();
      } catch (err) {
        data = null;
      }
    } else {
      data = await response.text();
    }

    if (!response.ok) {
      const errorMessage = (data && data.message) ? data.message : `Form scan failed with status ${response.status}`;
      const error = new Error(errorMessage);
      error.status = response.status;
      error.data = data;
      throw error;
    }

    return data;
  },

  // Unified Logout
  logout: (redirectUrl = '../../index.html') => {
    clearAuth();
    if (redirectUrl) {
      window.location.replace(redirectUrl);
    }
  },

  // Admin / Developer Operations
  adminLogin: (adminId, password) =>
    apiFetch('/api/admin/auth/login', {
      method: 'POST',
      body: JSON.stringify({ adminId, password })
    }),

  getAdminGrievances: () =>
    apiFetch('/api/admin/grievances', {
      method: 'GET'
    }),

  getAdminSosAlerts: () =>
    apiFetch('/api/admin/safety/sos', {
      method: 'GET'
    }),

  getAdminRouteSummary: () =>
    apiFetch('/api/admin/safety/routes/summary', {
      method: 'GET'
    }),

  getAdminSettings: () =>
    apiFetch('/api/admin/settings', {
      method: 'GET'
    }),

  updateAdminSettings: (settings) =>
    apiFetch('/api/admin/settings', {
      method: 'PUT',
      body: JSON.stringify(settings)
    }),

  // Health
  checkHealth: () =>
    apiFetch('/api/health', {
      method: 'GET'
    })
};


