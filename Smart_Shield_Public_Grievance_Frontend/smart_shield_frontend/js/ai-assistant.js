/**
 * Smart Shield - AI Civic + Legal Assistant (Module 5)
 * Handles conversational queries, local KB fallback, bounded history,
 * honest engine status, and strict XSS defense via textContent.
 */

document.addEventListener('DOMContentLoaded', () => {
  // Elements
  const chatMessages = document.getElementById('chatMessages');
  const chatForm = document.getElementById('chatForm');
  const chatInput = document.getElementById('chatInput');
  const sendBtn = document.getElementById('sendBtn');
  const charCounter = document.getElementById('charCounter');
  const clearChatBtn = document.getElementById('clearChatBtn');
  const promptChips = document.querySelectorAll('.prompt-chip');
  
  // Status banner elements
  const aiStatusText = document.getElementById('aiStatusText');
  const aiEngineBadge = document.getElementById('aiEngineBadge');
  const statusIndicator = document.getElementById('statusIndicator');
  const sidebarAiStatus = document.getElementById('sidebarAiStatus');

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

  // Conversation history (bounded to last 6 turns)
  let conversationHistory = [];
  let isRegistering = false;
  let isGenerating = false;

  // Initialize Session & Header
  initUserSession();

  // Initialize Engine Status
  checkAiEngineStatus();

  // Auto-resize textarea & character counter
  chatInput.addEventListener('input', () => {
    const len = chatInput.value.length;
    charCounter.textContent = `${len} / 3000`;
    
    // Auto grow up to max 140px
    chatInput.style.height = 'auto';
    chatInput.style.height = Math.min(chatInput.scrollHeight, 140) + 'px';
  });

  // Handle Enter key (Shift+Enter for newline, Enter to submit)
  chatInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      if (!isGenerating && chatInput.value.trim().length > 0) {
        chatForm.dispatchEvent(new Event('submit'));
      }
    }
  });

  // Handle Quick Prompt Chips
  promptChips.forEach(chip => {
    chip.addEventListener('click', () => {
      const prompt = chip.getAttribute('data-prompt');
      if (prompt && !isGenerating) {
        chatInput.value = prompt;
        chatInput.dispatchEvent(new Event('input'));
        chatForm.dispatchEvent(new Event('submit'));
      }
    });
  });

  // Clear Conversation
  clearChatBtn.addEventListener('click', () => {
    conversationHistory = [];
    chatMessages.innerHTML = '';
    
    // Re-render initial welcome message
    appendAssistantMessage({
      answer: "Conversation cleared. How else may I assist you with civic grievances, legal processes, or Smart Shield safety tools?",
      source: "LOCAL_KB",
      disclaimer: "Civic guidance and general legal information only. Not professional legal advice.",
      suggestedActions: ["Draft Grievance", "Safe Routes", "Emergency Center"]
    });
  });

  // Submit Chat Message
  chatForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const query = chatInput.value.trim();
    if (!query || isGenerating) return;

    // Check citizen authentication
    const token = getToken();
    if (!token) {
      showAuthModal("Please sign in as a citizen to consult the AI Assistant.");
      return;
    }

    // 1. Render Citizen User Bubble
    appendUserMessage(query);

    // 2. Clear input
    chatInput.value = '';
    chatInput.style.height = '52px';
    charCounter.textContent = '0 / 3000';

    // 3. Show Loading Indicator
    isGenerating = true;
    sendBtn.disabled = true;
    const typingIndicator = showTypingIndicator();

    try {
      // 4. Send API request with bounded history
      const payload = {
        message: query,
        history: conversationHistory
      };

      const response = await SmartShieldAPI.sendAiChatMessage(payload);

      // Remove typing indicator
      typingIndicator.remove();

      // 5. Append Assistant Message (strictly safe textContent)
      appendAssistantMessage(response);

      // 6. Update Bounded In-Memory History (max 6 turns)
      conversationHistory.push({ role: 'user', content: query });
      conversationHistory.push({ role: 'assistant', content: response.answer || '' });
      if (conversationHistory.length > 6) {
        conversationHistory = conversationHistory.slice(conversationHistory.length - 6);
      }

    } catch (err) {
      typingIndicator.remove();
      console.error("AI Assistant request error:", err);

      if (err.status === 401) {
        showAuthModal("Your session has expired. Please sign in again.");
      } else if (err.status === 429) {
        appendAssistantMessage({
          answer: "Rate limit reached (maximum 10 queries per minute per citizen). Please wait a moment before sending another query.",
          source: "RATE_LIMITED",
          disclaimer: "Rate limit applied for server safety and cost governance.",
          suggestedActions: []
        });
      } else {
        const errorMsg = (err.data && err.data.message) ? err.data.message : err.message;
        appendAssistantMessage({
          answer: `Unable to complete request: ${errorMsg}. Smart Shield verified civic information remains available on the grievances and safety portals.`,
          source: "UNAVAILABLE",
          disclaimer: "Service notice.",
          suggestedActions: ["My Grievances", "Safe Routes", "Emergency Center"]
        });
      }
    } finally {
      isGenerating = false;
      sendBtn.disabled = false;
      chatInput.focus();
    }
  });

  // Append User Message to Chat DOM (Strict XSS Defense)
  function appendUserMessage(text) {
    const row = document.createElement('div');
    row.className = 'message-row user';

    const avatar = document.createElement('div');
    avatar.className = 'msg-avatar user';
    const user = getUser();
    avatar.textContent = user && user.fullName ? getInitials(user.fullName) : 'ME';

    const bubble = document.createElement('div');
    bubble.className = 'message-bubble';

    const content = document.createElement('div');
    content.className = 'msg-content';
    content.textContent = text; // Safe textContent prevents any script injection

    bubble.appendChild(content);
    row.appendChild(avatar);
    row.appendChild(bubble);

    chatMessages.appendChild(row);
    scrollToBottom();
  }

  // Append Assistant Message to Chat DOM (Strict XSS Defense)
  function appendAssistantMessage(data) {
    const row = document.createElement('div');
    row.className = 'message-row assistant';

    const avatar = document.createElement('div');
    avatar.className = 'msg-avatar bot';
    avatar.textContent = '✦';

    const bubble = document.createElement('div');
    bubble.className = 'message-bubble';

    const isEmergency = data.source === 'EMERGENCY_OVERRIDE';
    if (isEmergency) {
      bubble.classList.add('emergency');
    }

    // Message Content
    const content = document.createElement('div');
    content.className = 'msg-content';
    content.textContent = data.answer || "No response provided.";
    bubble.appendChild(content);

    // Suggested Action Buttons (if any)
    if (Array.isArray(data.suggestedActions) && data.suggestedActions.length > 0) {
      const actionsList = document.createElement('div');
      actionsList.className = 'msg-actions-list';

      data.suggestedActions.forEach(actionText => {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'action-pill-btn';
        btn.textContent = actionText;

        if (actionText.includes('112') || actionText.toLowerCase().includes('emergency') || actionText.toLowerCase().includes('sos')) {
          btn.classList.add('emergency-btn');
        }

        btn.addEventListener('click', () => {
          handleActionPillClick(actionText);
        });

        actionsList.appendChild(btn);
      });

      bubble.appendChild(actionsList);
    }

    // Metadata Bar
    const metaBar = document.createElement('div');
    metaBar.className = 'msg-meta-bar';

    const disclaimerSpan = document.createElement('span');
    disclaimerSpan.textContent = data.disclaimer || "Civic information only. Not professional legal advice.";

    const sourceTag = document.createElement('span');
    sourceTag.className = 'msg-source-tag';
    
    if (data.source === 'GEMINI') {
      sourceTag.classList.add('gemini');
      sourceTag.textContent = 'GEMINI AI';
    } else if (data.source === 'EMERGENCY_OVERRIDE') {
      sourceTag.classList.add('emergency');
      sourceTag.textContent = 'EMERGENCY PROTOCOL';
    } else {
      sourceTag.classList.add('local-kb');
      sourceTag.textContent = 'VERIFIED CIVIC KB';
    }

    metaBar.appendChild(disclaimerSpan);
    metaBar.appendChild(sourceTag);
    bubble.appendChild(metaBar);

    row.appendChild(avatar);
    row.appendChild(bubble);

    chatMessages.appendChild(row);
    scrollToBottom();
  }

  // Handle Suggested Action Pill Clicks
  function handleActionPillClick(action) {
    const actLower = action.toLowerCase();
    if (actLower.includes('call 112') || actLower === '112') {
      window.location.href = 'tel:112';
    } else if (actLower.includes('sos') || actLower.includes('emergency')) {
      window.location.href = 'safety.html';
    } else if (actLower.includes('draft') || actLower.includes('raise') || actLower.includes('file')) {
      window.location.href = 'grievance.html#raise';
    } else if (actLower.includes('my grievances') || actLower.includes('tracking')) {
      window.location.href = 'grievance.html#complaints';
    } else if (actLower.includes('safe route') || actLower.includes('route')) {
      window.location.href = 'safe-routes.html';
    } else {
      // Put action in chat input
      chatInput.value = action;
      chatInput.dispatchEvent(new Event('input'));
    }
  }

  // Show Typing Indicator
  function showTypingIndicator() {
    const row = document.createElement('div');
    row.className = 'message-row assistant';
    row.id = 'typingIndicatorRow';

    const avatar = document.createElement('div');
    avatar.className = 'msg-avatar bot';
    avatar.textContent = '✦';

    const indicator = document.createElement('div');
    indicator.className = 'typing-indicator';
    indicator.innerHTML = '<span class="typing-dot"></span><span class="typing-dot"></span><span class="typing-dot"></span>';

    row.appendChild(avatar);
    row.appendChild(indicator);
    chatMessages.appendChild(row);
    scrollToBottom();
    return row;
  }

  function scrollToBottom() {
    chatMessages.scrollTop = chatMessages.scrollHeight;
  }

  // Engine Status Check (GET /api/ai/status)
  async function checkAiEngineStatus() {
    try {
      const status = await SmartShieldAPI.getAiStatus();
      if (status.configured) {
        statusIndicator.className = 'ai-status-indicator';
        aiStatusText.textContent = `Google Gemini Engine Active (${status.model || 'gemini-1.5-flash'})`;
        aiEngineBadge.className = 'ai-engine-badge gemini';
        aiEngineBadge.textContent = 'GEMINI ACTIVE';
        if (sidebarAiStatus) sidebarAiStatus.textContent = 'Gemini AI Active';
      } else {
        statusIndicator.className = 'ai-status-indicator fallback';
        aiStatusText.textContent = 'Smart Shield Verified Civic & Legal Knowledge Base Active';
        aiEngineBadge.className = 'ai-engine-badge kb';
        aiEngineBadge.textContent = 'LOCAL KB ACTIVE';
        if (sidebarAiStatus) sidebarAiStatus.textContent = 'Local KB Active';
      }
    } catch (err) {
      console.warn("AI Status check unreachable:", err);
      statusIndicator.className = 'ai-status-indicator offline';
      aiStatusText.textContent = 'AI Service Connecting / Offline';
      aiEngineBadge.className = 'ai-engine-badge';
      aiEngineBadge.textContent = 'OFFLINE';
      if (sidebarAiStatus) sidebarAiStatus.textContent = 'Offline';
    }
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

  // Auth Modal Management
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
