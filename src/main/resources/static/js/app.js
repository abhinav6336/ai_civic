/**
 * Civic Issue Reporting Platform - Simple, Mobile-First Application Controller
 */

document.addEventListener('DOMContentLoaded', () => {
  App.init();
});

const App = {
  currentView: 'home',
  wizardStep: 1,
  selectedCategory: 'ROADS',
  photoDataUrl: null,
  latitude: null,
  longitude: null,
  address: '',
  departments: [],
  map: null,
  mapMarker: null,
  selectedAdminIssue: null,
  adminFilterStatus: '',
  adminFilterDept: '',

  async init() {
    this.setupNavigation();
    this.setupWizardEvents();
    this.setupAdminEvents();
    await this.loadDepartments();
    this.checkSavedSession();
  },

  // ============================================================================
  // NAVIGATION & VIEW SWITCHING
  // ============================================================================
  setupNavigation() {
    // Logo Click -> Home
    document.getElementById('nav-logo').addEventListener('click', (e) => {
      e.preventDefault();
      this.switchView('home');
    });

    // Nav Buttons
    document.querySelectorAll('[data-view]').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.preventDefault();
        const view = btn.getAttribute('data-view');
        this.switchView(view);
      });
    });

    // Mobile Admin Nav Toggle
    const adminToggle = document.getElementById('btn-admin-mobile-menu');
    if (adminToggle) {
      adminToggle.addEventListener('click', () => {
        document.querySelector('.admin-sidebar').classList.toggle('mobile-open');
      });
    }
  },

  switchView(viewName) {
    this.currentView = viewName;

    // Update nav active states
    document.querySelectorAll('.nav-btn').forEach(btn => {
      btn.classList.toggle('active', btn.getAttribute('data-view') === viewName);
    });

    // Show active section
    document.querySelectorAll('.view-section').forEach(sec => {
      sec.classList.toggle('active', sec.id === `view-${viewName}`);
    });

    // View specific triggers
    if (viewName === 'report') {
      this.stopAdminPolling();
      this.resetWizard();
    } else if (viewName === 'my-complaints') {
      this.stopAdminPolling();
      this.loadMyComplaints();
    } else if (viewName === 'admin-dashboard') {
      this.checkAdminAuthAndLoad();
      this.startAdminPolling();
    } else {
      this.stopAdminPolling();
    }
  },

  startAdminPolling() {
    this.stopAdminPolling();
    this.adminPollTimer = setInterval(() => {
      if (this.currentView === 'admin-dashboard') {
        this.loadAdminStats();
        const activePane = document.querySelector('.admin-tab-pane:not([style*="none"])');
        if (!activePane || activePane.id === 'admin-tab-overview') {
          this.loadAdminComplaintsTable();
        }
      }
    }, 8000);
  },

  stopAdminPolling() {
    if (this.adminPollTimer) {
      clearInterval(this.adminPollTimer);
      this.adminPollTimer = null;
    }
  },

  // ============================================================================
  // 5-STEP REPORT ISSUE WIZARD
  // ============================================================================
  setupWizardEvents() {
    // Step 1: Category Tile Click
    document.querySelectorAll('.category-tile').forEach(tile => {
      tile.addEventListener('click', () => {
        document.querySelectorAll('.category-tile').forEach(t => t.classList.remove('selected'));
        tile.classList.add('selected');
        this.selectedCategory = tile.getAttribute('data-category');
        // Smoothly auto-advance to Step 2 for frictionless flow
        setTimeout(() => this.goToStep(2), 150);
      });
    });

    // Step 2: Photo Upload & Preview
    const photoInput = document.getElementById('photo-input');
    const photoBox = document.getElementById('photo-upload-box');
    
    if (photoBox && photoInput) {
      photoBox.addEventListener('click', () => photoInput.click());
      photoInput.addEventListener('change', (e) => this.handlePhotoSelection(e));
    }

    // Step 3: Location / Map
    const geoBtn = document.getElementById('btn-current-location');
    if (geoBtn) {
      geoBtn.addEventListener('click', () => this.detectCurrentLocation());
    }

    const addressInput = document.getElementById('manual-address-input');
    if (addressInput) {
      addressInput.addEventListener('input', (e) => {
        this.address = e.target.value;
      });
    }

    // Wizard Next & Prev Buttons
    document.querySelectorAll('[data-wizard-next]').forEach(btn => {
      btn.addEventListener('click', () => {
        const nextStep = parseInt(btn.getAttribute('data-wizard-next'));
        this.goToStep(nextStep);
      });
    });

    document.querySelectorAll('[data-wizard-prev]').forEach(btn => {
      btn.addEventListener('click', () => {
        const prevStep = parseInt(btn.getAttribute('data-wizard-prev'));
        this.goToStep(prevStep);
      });
    });

    // Step 4: Real-time AI category suggestion
    const descInput = document.getElementById('report-desc-input');
    const suggestBox = document.getElementById('ai-smart-suggest-box');
    let aiTimer = null;
    if (descInput && suggestBox) {
      descInput.addEventListener('input', () => {
        clearTimeout(aiTimer);
        const text = descInput.value.trim();
        if (text.length < 8) {
          suggestBox.style.display = 'none';
          return;
        }
        aiTimer = setTimeout(async () => {
          try {
            const res = await CivicApi.analyzeAi({
              description: text,
              category: this.selectedCategory,
              latitude: this.latitude,
              longitude: this.longitude,
              address: this.address
            });
            if (res.success && res.data && res.data.confidenceScore >= 0.70 && res.data.predictedCategory !== 'OTHER') {
              const cat = res.data.predictedCategory;
              const catLabel = res.data.predictedCategoryDisplayName || cat;
              suggestBox.style.display = 'block';
              suggestBox.innerHTML = `
                <div class="ai-suggestion-pill" onclick="App.applySuggestedCategory('${cat}')">
                  <span>🤖 AI Suggestion:</span> <b>${this.escapeHtml(catLabel)}</b> (${(res.data.confidenceScore * 100).toFixed(0)}%)
                  <span style="text-decoration: underline; font-size:0.75rem; margin-left:0.25rem;">(Click to apply)</span>
                </div>
              `;
            } else {
              suggestBox.style.display = 'none';
            }
          } catch (e) {
            suggestBox.style.display = 'none';
          }
        }, 400);
      });
    }

    // Final Submit Button
    const submitBtn = document.getElementById('btn-submit-report');
    if (submitBtn) {
      submitBtn.addEventListener('click', () => this.submitReport());
    }

    // Track Complaint Search Box
    const trackBtn = document.getElementById('btn-track-search');
    const trackInput = document.getElementById('track-search-input');
    if (trackBtn && trackInput) {
      trackBtn.addEventListener('click', () => {
        const id = trackInput.value.trim();
        if (id) this.trackComplaint(id);
      });
      trackInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          trackBtn.click();
        }
      });
    }
  },

  applySuggestedCategory(cat) {
    this.selectedCategory = cat;
    document.querySelectorAll('.category-tile').forEach(tile => {
      tile.classList.toggle('selected', tile.getAttribute('data-category') === cat);
    });
    const suggestBox = document.getElementById('ai-smart-suggest-box');
    if (suggestBox) {
      suggestBox.innerHTML = `<span style="font-size:0.8rem; color:var(--success); font-weight:600;">✓ Category updated to ${cat}</span>`;
      setTimeout(() => { suggestBox.style.display = 'none'; }, 2000);
    }
  },

  resetWizard() {
    this.goToStep(1);
    this.selectedPhotoFile = null;
    const suggestBox = document.getElementById('ai-smart-suggest-box');
    if (suggestBox) suggestBox.style.display = 'none';
    this.photoDataUrl = null;
    this.latitude = null;
    this.longitude = null;
    this.address = '';
    
    const photoPreview = document.getElementById('photo-preview-box');
    if (photoPreview) photoPreview.style.display = 'none';
    
    const photoInput = document.getElementById('photo-input');
    if (photoInput) photoInput.value = '';

    const addressInput = document.getElementById('manual-address-input');
    if (addressInput) addressInput.value = '';

    const descInput = document.getElementById('report-desc-input');
    if (descInput) descInput.value = '';

    document.querySelectorAll('.category-tile').forEach(t => {
      t.classList.toggle('selected', t.getAttribute('data-category') === 'ROADS');
    });
    this.selectedCategory = 'ROADS';
  },

  goToStep(stepNumber) {
    this.wizardStep = stepNumber;

    // Update progress text
    const ind = document.getElementById('wizard-step-indicator');
    if (ind) ind.textContent = `Step ${stepNumber} of 5`;

    // Show active step pane
    document.querySelectorAll('.step-pane').forEach(p => {
      p.classList.toggle('active', p.id === `step-${stepNumber}`);
    });

    // Step-specific initialization
    if (stepNumber === 3) {
      this.initLocationMap();
    } else if (stepNumber === 5) {
      this.renderSummary();
    }

    window.scrollTo({ top: 120, behavior: 'smooth' });
  },

  handlePhotoSelection(e) {
    const file = e.target.files[0];
    if (!file) return;

    if (!file.type.match(/image\/(jpeg|jpg|png|webp)/i)) {
      this.showToast('Please select a JPG or PNG photo.', true);
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      this.showToast('File size must be under 10MB.', true);
      return;
    }

    this.selectedPhotoFile = file;

    const reader = new FileReader();
    reader.onload = (event) => {
      this.photoDataUrl = event.target.result;
      const previewImg = document.getElementById('photo-preview-img');
      const previewBox = document.getElementById('photo-preview-box');
      if (previewImg && previewBox) {
        previewImg.src = this.photoDataUrl;
        previewBox.style.display = 'block';
      }
      this.showToast('Photo selected successfully!');
    };
    reader.readAsDataURL(file);
  },

  initLocationMap() {
    setTimeout(() => {
      const mapDiv = document.getElementById('map');
      if (!mapDiv || typeof L === 'undefined') return;

      const defaultLat = this.latitude || 25.4358; // Default Prayagraj / Urban reference
      const defaultLng = this.longitude || 81.8463;

      if (!this.map) {
        this.map = L.map('map').setView([defaultLat, defaultLng], 14);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
          maxZoom: 19,
          attribution: '© OpenStreetMap'
        }).addTo(this.map);

        this.mapMarker = L.marker([defaultLat, defaultLng], { draggable: true }).addTo(this.map);

        this.map.on('click', (e) => {
          this.setLocation(e.latlng.lat, e.latlng.lng);
        });

        this.mapMarker.on('dragend', (e) => {
          const pos = e.target.getLatLng();
          this.setLocation(pos.lat, pos.lng);
        });
      } else {
        this.map.invalidateSize();
        if (this.latitude && this.longitude) {
          this.map.setView([this.latitude, this.longitude], 15);
          this.mapMarker.setLatLng([this.latitude, this.longitude]);
        }
      }
    }, 150);
  },

  detectCurrentLocation() {
    const geoBtn = document.getElementById('btn-current-location');
    if (!navigator.geolocation) {
      this.showToast('Geolocation not supported by browser.', true);
      return;
    }

    geoBtn.disabled = true;
    geoBtn.textContent = '📍 Acquiring GPS Location...';

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const lat = pos.coords.latitude;
        const lng = pos.coords.longitude;
        this.setLocation(lat, lng, 'Current GPS Location');
        geoBtn.textContent = '✅ Location Captured';
        geoBtn.disabled = false;
        this.showToast('Location captured successfully!');
      },
      (err) => {
        geoBtn.textContent = '📍 Use My Current Location';
        geoBtn.disabled = false;
        this.showToast('Could not fetch location. Please enter street name manually.', true);
      },
      { timeout: 10000, enableHighAccuracy: true }
    );
  },

  setLocation(lat, lng, addressHint) {
    this.latitude = lat;
    this.longitude = lng;

    if (this.map && this.mapMarker) {
      this.map.setView([lat, lng], 15);
      this.mapMarker.setLatLng([lat, lng]);
    }

    const addrInput = document.getElementById('manual-address-input');
    if (addrInput && !addrInput.value) {
      addrInput.value = addressHint || `Near (${lat.toFixed(4)}, ${lng.toFixed(4)})`;
      this.address = addrInput.value;
    }
  },

  renderSummary() {
    const catMap = {
      'ROADS': '🛣️ Road',
      'ELECTRICITY': '💡 Electricity',
      'GARBAGE_SANITATION': '🗑️ Garbage',
      'WATER': '🚰 Water',
      'DRAINAGE': '🌊 Drainage',
      'OTHER': '🏙️ Other'
    };

    document.getElementById('summary-issue-type').textContent = catMap[this.selectedCategory] || this.selectedCategory;
    
    // Photo
    const photoSummary = document.getElementById('summary-photo');
    if (this.photoDataUrl) {
      photoSummary.innerHTML = `<img src="${this.photoDataUrl}" style="width: 50px; height: 50px; border-radius: 6px; object-fit: cover;">`;
    } else {
      photoSummary.textContent = 'No photo attached';
    }

    // Location
    const locText = this.address || (this.latitude ? `GPS (${this.latitude.toFixed(4)}, ${this.longitude.toFixed(4)})` : 'Not specified');
    document.getElementById('summary-location').textContent = locText;

    // Description
    const desc = document.getElementById('report-desc-input')?.value.trim() || 'No description provided';
    document.getElementById('summary-desc').textContent = desc;
  },

  async submitReport() {
    const btn = document.getElementById('btn-submit-report');
    btn.disabled = true;
    btn.textContent = 'Submitting Report...';

    const desc = document.getElementById('report-desc-input')?.value.trim() || '';
    const name = document.getElementById('report-citizen-name')?.value.trim() || 'Citizen';
    const phone = document.getElementById('report-citizen-phone')?.value.trim() || '';

    // Build Multipart FormData payload
    const formData = new FormData();
    formData.append('category', this.selectedCategory);
    if (desc) formData.append('description', desc);
    if (this.address) formData.append('address', this.address);
    if (this.latitude) formData.append('latitude', this.latitude);
    if (this.longitude) formData.append('longitude', this.longitude);
    if (name) formData.append('citizenName', name);
    if (phone) formData.append('citizenPhone', phone);
    
    // Attach binary image file if selected
    if (this.selectedPhotoFile) {
      formData.append('image', this.selectedPhotoFile);
    }

    try {
      const res = await CivicApi.submitIssue(formData);
      if (res.success && res.data) {
        const issue = res.data;
        
        // Save to citizen local storage list
        this.saveComplaintLocally(issue);

        // Show Confirmation
        document.getElementById('confirm-tracking-id').textContent = issue.trackingNumber;
        this.switchView('confirmation');
      }
    } catch (err) {
      this.showToast(`Error: ${err.message}`, true);
    } finally {
      btn.disabled = false;
      btn.textContent = 'SUBMIT REPORT';
    }
  },

  saveComplaintLocally(issue) {
    let list = [];
    try {
      list = JSON.parse(localStorage.getItem('civic_my_reports') || '[]');
    } catch {}
    list.unshift({
      id: issue.id,
      trackingNumber: issue.trackingNumber,
      category: issue.category,
      address: issue.address,
      createdAt: issue.createdAt,
      status: issue.status
    });
    localStorage.setItem('civic_my_reports', JSON.stringify(list.slice(0, 30)));
  },

  // ============================================================================
  // 4. CITIZEN COMPLAINT TRACKING & "MY COMPLAINTS"
  // ============================================================================
  async trackComplaint(trackingNumber) {
    try {
      const res = await CivicApi.trackIssue(trackingNumber);
      if (res.success && res.data) {
        this.renderComplaintDetailsModal(res.data, false);
      }
    } catch (err) {
      this.showToast(`Complaint not found: ${err.message}`, true);
    }
  },

  async loadMyComplaints() {
    const listContainer = document.getElementById('my-complaints-list');
    if (!listContainer) return;

    listContainer.innerHTML = '<div style="text-align:center; padding: 2rem; color: var(--text-muted);">Loading your complaints...</div>';

    try {
      // Get all from backend or local stored IDs
      const res = await CivicApi.getIssues();
      if (res.success && res.data) {
        const complaints = res.data;
        this.renderCitizenComplaintsList(complaints);
      }
    } catch (err) {
      // Fallback to local storage if offline
      const local = JSON.parse(localStorage.getItem('civic_my_reports') || '[]');
      this.renderCitizenComplaintsList(local);
    }
  },

  renderCitizenComplaintsList(complaints) {
    const listContainer = document.getElementById('my-complaints-list');
    if (!complaints || complaints.length === 0) {
      listContainer.innerHTML = `
        <div style="text-align: center; padding: 3rem 1rem; background: white; border-radius: var(--radius-lg); border: 1px dashed var(--border);">
          <div style="font-size: 2.5rem; margin-bottom: 0.5rem;">📋</div>
          <h3 style="font-size: 1.15rem; margin-bottom: 0.25rem;">No Complaints Found</h3>
          <p style="font-size: 0.9rem; color: var(--text-muted); margin-bottom: 1.25rem;">You haven't reported any civic problems yet.</p>
          <button class="btn btn-primary" onclick="App.switchView('report')">Report an Issue</button>
        </div>
      `;
      return;
    }

    listContainer.innerHTML = complaints.map(c => `
      <div class="complaint-card" onclick="App.trackComplaint('${c.trackingNumber}')">
        <div class="complaint-card-header">
          <span style="font-family: monospace; font-weight: 700; font-size: 0.95rem; color: var(--primary);">${c.trackingNumber}</span>
          <div style="display:flex; align-items:center; gap:0.4rem;">
            ${this.getPriorityBadgeHtml(c.priority, c.priorityLabel)}
            ${this.getStatusBadgeHtml(c.status)}
          </div>
        </div>
        <h3 style="font-size: 1.1rem; font-weight: 700; margin-bottom: 0.25rem;">${this.escapeHtml(c.title || c.category)}</h3>
        <p style="font-size: 0.875rem; color: var(--text-muted); margin-bottom: 0.75rem;">📍 ${this.escapeHtml(c.address || 'Location provided')}</p>
        
        <!-- Plain Visual Progress Stepper for Citizen -->
        ${this.getCitizenStepperHtml(c.status)}

        <div style="display:flex; justify-content:space-between; font-size: 0.8rem; color: var(--text-light); margin-top: 0.5rem; border-top: 1px solid var(--border-light); padding-top: 0.5rem;">
          <span>Reported: ${new Date(c.createdAt).toLocaleDateString()}</span>
          <span style="font-weight: 600; color: var(--primary);">View Details &rarr;</span>
        </div>
      </div>
    `).join('');
  },

  getCitizenStepperHtml(status) {
    const steps = [
      { key: 'REPORTED', label: 'Submitted' },
      { key: 'AI_CLASSIFIED', label: 'Under Review' },
      { key: 'ASSIGNED', label: 'Assigned' },
      { key: 'IN_PROGRESS', label: 'In Progress' },
      { key: 'RESOLVED', label: 'Resolved' }
    ];

    const order = ['REPORTED', 'AI_CLASSIFIED', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED'];
    let currentIndex = order.indexOf(status);
    if (status === 'REJECTED') currentIndex = -1;

    return `
      <div class="status-stepper">
        ${steps.map((s, idx) => {
          let nodeClass = '';
          if (idx < currentIndex) nodeClass = 'completed';
          else if (idx === currentIndex) nodeClass = 'active';
          
          return `
            <div class="stepper-node ${nodeClass}">
              <div class="stepper-dot">${idx < currentIndex ? '✓' : (idx + 1)}</div>
              <span class="stepper-label">${s.label}</span>
            </div>
          `;
        }).join('')}
      </div>
    `;
  },

  getStatusBadgeHtml(status) {
    const map = {
      'REPORTED': { cls: 'badge-submitted', text: 'Submitted' },
      'AI_CLASSIFIED': { cls: 'badge-review', text: 'Under Review' },
      'ASSIGNED': { cls: 'badge-assigned', text: 'Assigned' },
      'IN_PROGRESS': { cls: 'badge-progress', text: 'In Progress' },
      'RESOLVED': { cls: 'badge-resolved', text: 'Resolved' },
      'REJECTED': { cls: 'badge-rejected', text: 'Rejected' }
    };
    const b = map[status] || { cls: 'badge-submitted', text: status };
    return `<span class="badge ${b.cls}">${b.text}</span>`;
  },

  getPriorityBadgeHtml(priority, label) {
    if (!priority) return '';
    const p = String(priority).toUpperCase();
    const lbl = label || priority;
    let cls = 'badge-priority-medium';
    if (p === 'CRITICAL') cls = 'badge-priority-critical';
    else if (p === 'HIGH') cls = 'badge-priority-high';
    else if (p === 'LOW') cls = 'badge-priority-low';
    return `<span class="badge-priority ${cls}">${this.escapeHtml(lbl)}</span>`;
  },

  renderComplaintDetailsModal(issue, isAdmin = false) {
    this.selectedAdminIssue = issue;

    document.getElementById('modal-issue-id').textContent = issue.trackingNumber;
    document.getElementById('modal-status-pill').innerHTML = this.getStatusBadgeHtml(issue.status);
    document.getElementById('modal-issue-title').textContent = issue.title;
    document.getElementById('modal-issue-desc').textContent = issue.description || 'No description entered.';
    document.getElementById('modal-issue-location').textContent = issue.address || (issue.latitude ? `${issue.latitude}, ${issue.longitude}` : 'Not provided');
    document.getElementById('modal-issue-date').textContent = new Date(issue.createdAt).toLocaleString();

    // Photo Display
    const photoBox = document.getElementById('modal-issue-photo-box');
    const photoImg = document.getElementById('modal-issue-photo-img');
    if (issue.imageUrl && issue.imageUrl.trim() !== '') {
      photoImg.src = issue.imageUrl;
      photoBox.style.display = 'block';
    } else {
      photoBox.style.display = 'none';
    }

    // AI Classification & Triage (Clean & Minimal)
    const aiBox = document.getElementById('modal-ai-box');
    const priorityHtml = this.getPriorityBadgeHtml(issue.priority, issue.priorityLabel);
    const etaText = issue.estimatedResolutionHours ? `⏱️ Est. SLA: <b>~${issue.estimatedResolutionHours} hrs</b>` : '';
    const dupNotice = issue.isDuplicate ? `<div style="margin-top:0.4rem; color:#92400e; font-size:0.85rem; background:#fef3c7; padding:0.4rem 0.6rem; border-radius:var(--radius-sm); border:1px solid #fde68a;">⚠️ <b>Duplicate Detected:</b> Matched with active ticket <code>${this.escapeHtml(issue.duplicateOfTrackingNumber || '')}</code></div>` : '';

    if (isAdmin) {
      aiBox.style.display = 'block';
      aiBox.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.35rem; flex-wrap: wrap; gap: 0.5rem;">
          <div><strong>🤖 AI Triage & Risk Assessment:</strong></div>
          <div style="display:flex; align-items:center; gap:0.4rem;">${priorityHtml} ${issue.urgencyScore ? `<span style="font-size: 0.8rem; font-weight:700; color: var(--text-muted);">(Score: ${issue.urgencyScore}/100)</span>` : ''}</div>
        </div>
        <div style="font-size: 0.88rem; line-height: 1.5;">
          Category: <b>${this.escapeHtml(issue.aiSuggestedCategory || issue.categoryDisplayName || issue.category || 'General')}</b> 
          ${issue.aiConfidence ? `| Confidence: <b>${(issue.aiConfidence * 100).toFixed(0)}%</b>` : ''}
          ${etaText ? ` | ${etaText}` : ''}
        </div>
        ${dupNotice}
      `;
    } else if (issue.aiSuggestedCategory || issue.priority) {
      aiBox.style.display = 'block';
      aiBox.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.5rem;">
          <div><strong>🤖 Detected Issue:</strong> ${this.escapeHtml(issue.aiSuggestedCategory || issue.categoryDisplayName || issue.category || '')}</div>
          <div>${priorityHtml}</div>
        </div>
        ${etaText ? `<div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 0.25rem;">${etaText}</div>` : ''}
        ${dupNotice}
      `;
    } else {
      aiBox.style.display = 'none';
    }

    // Admin Controls visibility
    const adminControls = document.getElementById('modal-admin-actions');
    if (adminControls) {
      adminControls.style.display = isAdmin ? 'block' : 'none';
      if (isAdmin) {
        document.getElementById('admin-action-status-select').value = issue.status;
        this.populateDepartmentSelect('admin-action-dept-select', issue.assignedDepartmentId);
      }
    }

    // Open Modal
    document.getElementById('complaint-modal').classList.add('open');
  },

  // ============================================================================
  // 5. ADMIN PORTAL & DASHBOARD
  // ============================================================================
  setupAdminEvents() {
    // Admin Login Form
    const loginForm = document.getElementById('admin-login-form');
    if (loginForm) {
      loginForm.addEventListener('submit', (e) => this.handleAdminLogin(e));
    }

    // Admin Logout
    const logoutBtn = document.getElementById('btn-admin-logout');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', (e) => {
        e.preventDefault();
        CivicApi.logoutAdmin();
        this.showToast('Logged out successfully.');
        this.switchView('home');
      });
    }

    // Admin Tabs
    document.querySelectorAll('[data-admin-tab]').forEach(tabBtn => {
      tabBtn.addEventListener('click', (e) => {
        e.preventDefault();
        document.querySelectorAll('[data-admin-tab]').forEach(b => b.classList.remove('active'));
        tabBtn.classList.add('active');
        const tab = tabBtn.getAttribute('data-admin-tab');
        this.switchAdminTab(tab);
      });
    });

    // Admin Complaints Filter
    const statusFilter = document.getElementById('admin-filter-status');
    const deptFilter = document.getElementById('admin-filter-dept');
    const searchFilter = document.getElementById('admin-filter-search');

    if (statusFilter) statusFilter.addEventListener('change', () => this.loadAdminComplaintsTable());
    if (deptFilter) deptFilter.addEventListener('change', () => this.loadAdminComplaintsTable());
    if (searchFilter) {
      let timer = null;
      searchFilter.addEventListener('input', () => {
        clearTimeout(timer);
        timer = setTimeout(() => this.loadAdminComplaintsTable(), 300);
      });
    }

    // Admin Modal Action Buttons
    const updateStatusBtn = document.getElementById('btn-admin-apply-status');
    if (updateStatusBtn) {
      updateStatusBtn.addEventListener('click', () => this.handleAdminStatusUpdate());
    }

    const assignDeptBtn = document.getElementById('btn-admin-apply-dept');
    if (assignDeptBtn) {
      assignDeptBtn.addEventListener('click', () => this.handleAdminDeptAssign());
    }

    const markResolvedBtn = document.getElementById('btn-admin-mark-resolved');
    if (markResolvedBtn) {
      markResolvedBtn.addEventListener('click', () => {
        document.getElementById('admin-action-status-select').value = 'RESOLVED';
        this.handleAdminStatusUpdate();
      });
    }

    // Close Modal Button
    document.querySelectorAll('.modal-close, .modal-overlay').forEach(el => {
      el.addEventListener('click', (e) => {
        if (e.target === el) {
          document.querySelectorAll('.modal-overlay').forEach(m => m.classList.remove('open'));
        }
      });
    });
  },

  async handleAdminLogin(e) {
    e.preventDefault();
    const email = document.getElementById('admin-email').value.trim();
    const password = document.getElementById('admin-password').value.trim();
    const btn = document.getElementById('btn-login-submit');

    btn.disabled = true;
    btn.textContent = 'Verifying Credentials...';

    try {
      const res = await CivicApi.loginAdmin(email, password);
      if (res.success) {
        this.showToast('Login successful!');
        this.switchView('admin-dashboard');
      }
    } catch (err) {
      this.showToast(`Login failed: ${err.message}`, true);
    } finally {
      btn.disabled = false;
      btn.textContent = 'Login to Admin Portal';
    }
  },

  checkSavedSession() {
    const userStr = localStorage.getItem('civic_user');
    if (userStr) {
      try {
        const user = JSON.parse(userStr);
        if (user && (user.role === 'ADMIN' || user.role === 'OFFICER')) {
          // Admin session available
        }
      } catch {}
    }
  },

  async checkAdminAuthAndLoad() {
    const token = CivicApi.getAuthToken();
    if (!token) {
      this.switchView('admin-login');
      return;
    }

    await this.loadAdminStats();
    await this.loadAdminComplaintsTable();
  },

  switchAdminTab(tabName) {
    document.querySelectorAll('.admin-tab-pane').forEach(pane => {
      pane.style.display = pane.id === `admin-tab-${tabName}` ? 'block' : 'none';
    });

    if (tabName === 'insights') {
      this.loadCivicInsights();
    } else if (tabName === 'departments') {
      this.renderAdminDepartmentsTab();
    } else if (tabName === 'complaints' || tabName === 'overview') {
      this.loadAdminComplaintsTable();
    }
  },

  // ============================================================================
  // 5. ADMIN OPERATIONS & COMPLAINTS MANAGEMENT
  // ============================================================================
  async loadAdminStats() {
    try {
      const res = await CivicApi.getStats();
      if (res.success && res.data) {
        const d = res.data;
        document.getElementById('adm-stat-total').textContent = d.totalComplaints || 0;
        document.getElementById('adm-stat-new').textContent = d.newComplaints || 0;
        document.getElementById('adm-stat-review').textContent = d.underReviewComplaints || 0;
        document.getElementById('adm-stat-progress').textContent = d.inProgressComplaints || 0;
        document.getElementById('adm-stat-resolved').textContent = d.resolvedComplaints || 0;
      }
    } catch (err) {
      console.warn('Failed to load admin stats', err);
    }
  },

  async loadAdminComplaintsTable() {
    const tableBody = document.getElementById('admin-complaints-tbody');
    if (!tableBody) return;

    const status = document.getElementById('admin-filter-status')?.value || '';
    const deptId = document.getElementById('admin-filter-dept')?.value || '';
    const search = document.getElementById('admin-filter-search')?.value || '';

    try {
      const res = await CivicApi.getIssues({ status, departmentId: deptId, search });
      if (res.success && res.data) {
        this.renderAdminTableRows(res.data);
      }
    } catch (err) {
      tableBody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding: 2rem; color: var(--danger);">Failed to load: ${err.message}</td></tr>`;
    }
  },

  renderAdminTableRows(issues) {
    const tbody = document.getElementById('admin-complaints-tbody');
    if (!tbody) return;

    if (!issues || issues.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; padding: 2rem; color: var(--text-muted);">No complaints found matching criteria.</td></tr>';
      return;
    }

    tbody.innerHTML = issues.map(i => `
      <tr>
        <td style="font-family: monospace; font-weight: 700; color: var(--primary);">${this.escapeHtml(i.trackingNumber)}</td>
        <td>
          <div style="display:flex; align-items:center; gap:0.4rem; flex-wrap:wrap;">
            <b>${this.escapeHtml(i.title || i.category)}</b>
            ${this.getPriorityBadgeHtml(i.priority, i.priorityLabel)}
            ${i.isDuplicate ? '<span class="badge-duplicate">🔁 Dup</span>' : ''}
          </div>
        </td>
        <td>${this.escapeHtml(i.address || 'Location provided')}</td>
        <td>${i.assignedDepartmentName ? this.escapeHtml(i.assignedDepartmentName) : '<span style="color:var(--text-light)">Unassigned</span>'}</td>
        <td>${this.getStatusBadgeHtml(i.status)}</td>
        <td>${new Date(i.createdAt).toLocaleDateString([], {month:'short', day:'numeric'})}</td>
        <td>
          <button class="btn btn-secondary" style="padding: 0.35rem 0.75rem; font-size: 0.85rem; min-height: 32px;" onclick="App.openAdminComplaintModal(${i.id})">
            View
          </button>
        </td>
      </tr>
    `).join('');
  },

  async openAdminComplaintModal(issueId) {
    try {
      const res = await CivicApi.getIssueById(issueId);
      if (res.success && res.data) {
        this.renderComplaintDetailsModal(res.data, true);
      }
    } catch (err) {
      this.showToast(`Failed to open complaint: ${err.message}`, true);
    }
  },

  async handleAdminStatusUpdate() {
    if (!this.selectedAdminIssue) return;
    const newStatus = document.getElementById('admin-action-status-select').value;
    const notes = document.getElementById('admin-action-notes').value.trim();

    try {
      const res = await CivicApi.updateStatus(this.selectedAdminIssue.id, newStatus, notes, null);
      if (res.success) {
        this.showToast('Complaint status updated successfully!');
        this.selectedAdminIssue = res.data;
        document.getElementById('complaint-modal').classList.remove('open');
        this.loadAdminStats();
        this.loadAdminComplaintsTable();
      }
    } catch (err) {
      this.showToast(`Update error: ${err.message}`, true);
    }
  },

  async handleAdminDeptAssign() {
    if (!this.selectedAdminIssue) return;
    const deptId = document.getElementById('admin-action-dept-select').value;
    const notes = document.getElementById('admin-action-notes').value.trim();

    if (!deptId) {
      this.showToast('Please select a department to assign.', true);
      return;
    }

    try {
      const res = await CivicApi.assignDepartment(this.selectedAdminIssue.id, deptId, null, notes, null);
      if (res.success) {
        this.showToast('Department assigned successfully!');
        this.selectedAdminIssue = res.data;
        document.getElementById('complaint-modal').classList.remove('open');
        this.loadAdminStats();
        this.loadAdminComplaintsTable();
      }
    } catch (err) {
      this.showToast(`Assignment error: ${err.message}`, true);
    }
  },

  async loadDepartments() {
    try {
      const res = await CivicApi.getDepartments();
      if (res.success && res.data) {
        this.departments = res.data;
        this.populateDepartmentSelect('admin-filter-dept');
        this.populateDepartmentSelect('admin-action-dept-select');
      }
    } catch (err) {
      console.error('Failed to load departments:', err);
    }
  },

  populateDepartmentSelect(selectId, selectedId) {
    const sel = document.getElementById(selectId);
    if (!sel) return;
    sel.innerHTML = '<option value="">All Departments</option>';
    this.departments.forEach(d => {
      const isSel = selectedId && selectedId === d.id ? 'selected' : '';
      sel.innerHTML += `<option value="${d.id}" ${isSel}>${d.name}</option>`;
    });
  },

  // ============================================================================
  // 6. AI-ASSISTED CIVIC INSIGHTS & ANALYTICS
  // ============================================================================
  async loadCivicInsights() {
    try {
      const res = await CivicApi.getCivicInsights();
      if (res.success && res.data) {
        const insights = res.data;
        this.renderCivicInsightsView(insights);
      }
    } catch (err) {
      this.showToast(`Failed to load insights: ${err.message}`, true);
    }
  },

  renderCivicInsightsView(insights) {
    const obs = insights.observedData;
    const algo = insights.algorithmicInsights;

    // 1. Top KPI Summary Numbers
    const totalEl = document.getElementById('ins-stat-total');
    const resRateEl = document.getElementById('ins-stat-resolution-rate');
    const clustersEl = document.getElementById('ins-stat-clusters');
    const bottlenecksEl = document.getElementById('ins-stat-bottlenecks');

    if (totalEl) totalEl.textContent = obs.totalComplaints;
    if (resRateEl) resRateEl.textContent = `${obs.overallResolutionRate}%`;
    if (clustersEl) clustersEl.textContent = algo.spatialClusters ? algo.spatialClusters.length : 0;
    if (bottlenecksEl) {
      const bCount = (algo.workloadBottlenecks || []).filter(b => b.bottleneckSeverity !== 'NORMAL').length;
      bottlenecksEl.textContent = bCount;
    }

    // 2. Render Observed Category Bars
    this.renderObservedCategories(obs.categoryBreakdown, obs.totalComplaints);

    // 3. Render Observed Department Workloads
    this.renderObservedDepartments(obs.departmentWorkloads);

    // 4. Render Frequent Locations
    this.renderObservedLocations(obs.frequentLocations);

    // 5. Render Spatial Clusters & Map
    this.renderSpatialClustersAndMap(algo.spatialClusters);

    // 6. Render Recurring Patterns
    this.renderRecurringPatterns(algo.recurringPatterns);

    // 7. Render Workload Bottlenecks
    this.renderWorkloadBottlenecks(algo.workloadBottlenecks);

    // 8. Render Unresolved Patterns
    this.renderUnresolvedPatterns(algo.unresolvedPatterns);

    // 9. Render Strategic Recommendations
    this.renderRecommendations(algo.recommendations);
  },

  renderObservedCategories(categories, total) {
    const container = document.getElementById('ins-observed-categories');
    if (!container) return;

    if (!categories || categories.length === 0 || total === 0) {
      container.innerHTML = '<p style="font-size:0.85rem; color:var(--text-muted);">No historical complaint data available.</p>';
      return;
    }

    container.innerHTML = categories.map(cat => {
      let fillClass = 'primary';
      if (cat.category === 'ROADS') fillClass = 'warning';
      else if (cat.category === 'WATER') fillClass = 'primary';
      else if (cat.category === 'GARBAGE_SANITATION') fillClass = 'success';
      else if (cat.category === 'ELECTRICITY') fillClass = 'purple';

      return `
        <div class="bar-chart-row">
          <div class="bar-chart-label-row">
            <span>${this.escapeHtml(cat.displayName)}</span>
            <span style="color: var(--text-muted);">${cat.count} (${cat.percentage}%)</span>
          </div>
          <div class="bar-chart-track">
            <div class="bar-chart-fill ${fillClass}" style="width: ${Math.max(cat.percentage, 4)}%;"></div>
          </div>
        </div>
      `;
    }).join('');
  },

  renderObservedDepartments(departments) {
    const container = document.getElementById('ins-observed-departments');
    if (!container) return;

    if (!departments || departments.length === 0) {
      container.innerHTML = '<p style="font-size:0.85rem; color:var(--text-muted);">No department workload data available.</p>';
      return;
    }

    container.innerHTML = departments.map(d => `
      <div style="background: var(--surface-alt); border-radius: var(--radius-md); padding: 0.75rem 1rem; margin-bottom: 0.6rem;">
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom: 0.35rem;">
          <strong style="font-size: 0.9rem;">${this.escapeHtml(d.departmentName)}</strong>
          <span style="font-size: 0.75rem; font-weight: 700; color: var(--text-muted);">${d.totalCount} Total</span>
        </div>
        <div style="display:flex; gap: 0.75rem; font-size: 0.8rem; margin-bottom: 0.4rem; flex-wrap: wrap;">
          <span style="color: var(--warning); font-weight: 600;">⏳ Pending: ${d.pendingCount}</span>
          <span style="color: var(--primary); font-weight: 600;">⚙️ In Progress: ${d.inProgressCount}</span>
          <span style="color: var(--success); font-weight: 600;">✅ Resolved: ${d.resolvedCount}</span>
        </div>
        <div class="bar-chart-track" style="height: 6px;">
          <div class="bar-chart-fill success" style="width: ${d.resolutionRate}%;"></div>
        </div>
        <div style="display:flex; justify-content:space-between; font-size: 0.7rem; color: var(--text-light); margin-top: 0.2rem;">
          <span>Resolution: ${d.resolutionRate}%</span>
          <span>Workload Share: ${d.workloadSharePercent}%</span>
        </div>
      </div>
    `).join('');
  },

  renderObservedLocations(locations) {
    const container = document.getElementById('ins-observed-locations');
    if (!container) return;

    if (!locations || locations.length === 0) {
      container.innerHTML = '<p style="font-size:0.85rem; color:var(--text-muted);">No frequent location records.</p>';
      return;
    }

    container.innerHTML = locations.map((loc, idx) => `
      <div style="display:flex; justify-content:space-between; align-items:center; padding: 0.5rem 0; border-bottom: 1px solid var(--border-light); font-size: 0.85rem;">
        <div style="display:flex; align-items:center; gap: 0.5rem;">
          <span style="font-weight: 800; color: var(--text-light); width: 18px;">#${idx + 1}</span>
          <div>
            <div style="font-weight: 600;">${this.escapeHtml(loc.location)}</div>
            <div style="font-size: 0.75rem; color: var(--text-muted);">Dominant: ${loc.primaryCategory}</div>
          </div>
        </div>
        <div style="text-align: right;">
          <span style="font-weight: 700; color: var(--primary);">${loc.totalCount} reports</span>
          <div style="font-size: 0.75rem; color: var(--warning);">${loc.pendingCount} open</div>
        </div>
      </div>
    `).join('');
  },

  renderSpatialClustersAndMap(clusters) {
    const listContainer = document.getElementById('ins-clusters-list');
    const mapEl = document.getElementById('insights-cluster-map');

    if (!clusters || clusters.length === 0) {
      if (listContainer) listContainer.innerHTML = '<p style="font-size:0.85rem; color:var(--text-muted);">No geographic clusters detected.</p>';
      return;
    }

    // 1. Render Cluster Cards List
    if (listContainer) {
      listContainer.innerHTML = clusters.map(c => {
        let tagClass = 'low';
        if (c.riskLevel === 'HIGH') tagClass = 'high';
        else if (c.riskLevel === 'MEDIUM') tagClass = 'medium';

        return `
          <div class="cluster-list-item">
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom: 0.35rem;">
              <strong style="font-size: 0.9rem;">${this.escapeHtml(c.clusterName)}</strong>
              <span class="risk-tag ${tagClass}">${c.riskLevel} RISK</span>
            </div>
            <p style="font-size: 0.8rem; color: var(--text-main); margin-bottom: 0.4rem;">${this.escapeHtml(c.summary)}</p>
            <div style="display:flex; justify-content:space-between; font-size: 0.75rem; color: var(--text-muted);">
              <span>Dominant: <b>${c.dominantCategory}</b></span>
              <span>Tickets: ${c.trackingNumbers ? c.trackingNumbers.slice(0, 3).join(', ') : ''}</span>
            </div>
          </div>
        `;
      }).join('');
    }

    // 2. Render Interactive Leaflet Map
    if (mapEl && typeof L !== 'undefined') {
      if (this.insightsMap) {
        this.insightsMap.remove();
        this.insightsMap = null;
      }

      // Initialize map centered at first cluster centroid
      const first = clusters[0];
      const initialLat = first.latitude || 37.7749;
      const initialLon = first.longitude || -122.4194;

      this.insightsMap = L.map('insights-cluster-map', {
        zoomControl: true,
        attributionControl: false
      }).setView([initialLat, initialLon], 13);

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 18
      }).addTo(this.insightsMap);

      const bounds = [];

      clusters.forEach(c => {
        if (!c.latitude || !c.longitude) return;
        const latLng = [c.latitude, c.longitude];
        bounds.push(latLng);

        let circleColor = '#16a34a';
        if (c.riskLevel === 'HIGH') circleColor = '#dc2626';
        else if (c.riskLevel === 'MEDIUM') circleColor = '#d97706';

        const circle = L.circle(latLng, {
          color: circleColor,
          fillColor: circleColor,
          fillOpacity: 0.35,
          radius: 200 + (c.complaintCount * 100)
        }).addTo(this.insightsMap);

        const marker = L.circleMarker(latLng, {
          color: circleColor,
          fillColor: circleColor,
          fillOpacity: 0.9,
          radius: 8
        }).addTo(this.insightsMap);

        const popupContent = `
          <div style="font-family: sans-serif; min-width: 180px;">
            <b style="color: ${circleColor};">${this.escapeHtml(c.clusterName)}</b><br>
            <span style="font-size: 0.85rem;"><b>${c.complaintCount} Complaints</b></span><br>
            <span style="font-size: 0.8rem; color: #555;">Dominant: ${c.dominantCategory}</span><br>
            <span style="font-size: 0.75rem; color: #777;">Risk Level: <b>${c.riskLevel}</b></span>
          </div>
        `;
        marker.bindPopup(popupContent);
        circle.bindPopup(popupContent);
      });

      if (bounds.length > 0) {
        this.insightsMap.fitBounds(bounds, { padding: [30, 30], maxZoom: 14 });
      }

      setTimeout(() => {
        if (this.insightsMap) this.insightsMap.invalidateSize();
      }, 300);
    }
  },

  renderRecurringPatterns(patterns) {
    const container = document.getElementById('ins-recurring-patterns');
    if (!container) return;

    if (!patterns || patterns.length === 0) {
      container.innerHTML = '<p style="font-size:0.85rem; color:var(--text-muted);">No recurring chronic patterns detected in historical records.</p>';
      return;
    }

    container.innerHTML = patterns.map(p => `
      <div style="background: var(--surface-alt); border-left: 3px solid var(--warning); border-radius: 0 var(--radius-md) var(--radius-md) 0; padding: 0.85rem 1rem; margin-bottom: 0.75rem;">
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom: 0.3rem;">
          <strong style="font-size: 0.9rem;">📍 ${this.escapeHtml(p.location)}</strong>
          <span style="font-size: 0.75rem; font-weight: 700; color: var(--warning);">${p.occurrences} Recurring</span>
        </div>
        <p style="font-size: 0.85rem; color: var(--text-main); line-height: 1.4; margin-bottom: 0.4rem;">
          <b>Diagnosis:</b> ${this.escapeHtml(p.diagnosis)}
        </p>
        <p style="font-size: 0.8rem; color: var(--primary); font-weight: 600;">
          💡 <b>Action:</b> ${this.escapeHtml(p.recommendation)}
        </p>
      </div>
    `).join('');
  },

  renderWorkloadBottlenecks(bottlenecks) {
    const container = document.getElementById('ins-workload-bottlenecks');
    if (!container) return;

    if (!bottlenecks || bottlenecks.length === 0) {
      container.innerHTML = '<p style="font-size:0.85rem; color:var(--text-muted);">All municipal departments operating within normal capacity.</p>';
      return;
    }

    container.innerHTML = bottlenecks.map(b => {
      let tagClass = 'low';
      if (b.bottleneckSeverity === 'CRITICAL') tagClass = 'high';
      else if (b.bottleneckSeverity === 'ELEVATED') tagClass = 'medium';

      return `
        <div style="border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 0.85rem 1rem; margin-bottom: 0.6rem; background: var(--surface);">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom: 0.35rem;">
            <strong style="font-size: 0.9rem;">${this.escapeHtml(b.departmentName)}</strong>
            <span class="risk-tag ${tagClass}">${b.bottleneckSeverity}</span>
          </div>
          <div style="display:flex; justify-content:space-between; font-size: 0.8rem; color: var(--text-muted); margin-bottom: 0.35rem;">
            <span>Pending Backlog: <b>${b.pendingBacklog} tickets</b></span>
            <span>Pressure Index: <b>${b.workloadPressureIndex}x</b></span>
          </div>
          <p style="font-size: 0.8rem; color: var(--text-main); line-height: 1.35;">
            ${this.escapeHtml(b.recommendation)}
          </p>
        </div>
      `;
    }).join('');
  },

  renderUnresolvedPatterns(unresolved) {
    const container = document.getElementById('ins-unresolved-patterns');
    if (!container) return;

    if (!unresolved || unresolved.length === 0) {
      container.innerHTML = '<p style="font-size:0.85rem; color:var(--text-muted);">No unresolved queue stagnation detected.</p>';
      return;
    }

    container.innerHTML = unresolved.map(u => `
      <div style="padding: 0.75rem; border-radius: var(--radius-md); background: var(--surface-alt); margin-bottom: 0.6rem; font-size: 0.85rem;">
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom: 0.25rem;">
          <strong>⏱️ ${this.escapeHtml(u.title)}</strong>
          <span style="font-weight: 700; color: var(--danger); font-size: 0.8rem;">${u.affectedCount} Affected</span>
        </div>
        <p style="color: var(--text-muted); font-size: 0.8rem; margin-bottom: 0.25rem;">${this.escapeHtml(u.description)}</p>
        <p style="color: var(--primary); font-size: 0.8rem; font-weight: 600;">Suggested Action: ${this.escapeHtml(u.actionSuggested)}</p>
      </div>
    `).join('');
  },

  renderRecommendations(recs) {
    const container = document.getElementById('ins-recommendations-list');
    if (!container) return;

    if (!recs || recs.length === 0) {
      container.innerHTML = '<p style="font-size:0.85rem; color:var(--text-muted);">No critical recommendations at this time.</p>';
      return;
    }

    container.innerHTML = recs.map(r => {
      let priorityClass = 'medium';
      if (r.priority === 'URGENT') priorityClass = 'urgent';
      else if (r.priority === 'HIGH') priorityClass = 'high';

      return `
        <div class="rec-card ${priorityClass}">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom: 0.35rem;">
            <strong style="font-size: 0.95rem;">${this.escapeHtml(r.title)}</strong>
            <span class="rec-priority-badge ${priorityClass}">${r.priority} PRIORITY</span>
          </div>
          <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 0.35rem;">
            <b>Rationale:</b> ${this.escapeHtml(r.rationale)}
          </p>
          <div style="background: var(--surface-alt); padding: 0.5rem 0.75rem; border-radius: var(--radius-sm); font-size: 0.85rem; color: var(--text-main);">
            <b>Recommended Municipal Action:</b> ${this.escapeHtml(r.recommendedAction)}
          </div>
        </div>
      `;
    }).join('');
  },

  renderAdminDepartmentsTab() {
    const list = document.getElementById('admin-departments-list');
    if (!list) return;

    list.innerHTML = this.departments.map(d => `
      <div class="complaint-card" style="cursor: default;">
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <h3 style="font-size: 1.15rem; font-weight: 700;">${d.name}</h3>
          <span class="badge badge-submitted">${d.code}</span>
        </div>
        <p style="font-size: 0.9rem; color: var(--text-muted); margin: 0.5rem 0;">${d.description}</p>
        <div style="font-size: 0.85rem; color: var(--text-light); border-top: 1px solid var(--border-light); padding-top: 0.5rem; display:flex; gap: 1.5rem;">
          <span>📧 ${d.contactEmail}</span>
          <span>📞 ${d.contactPhone}</span>
        </div>
      </div>
    `).join('');
  },

  // Helpers
  escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  },

  showToast(message, isError = false) {
    const container = document.getElementById('toast-box');
    const toast = document.createElement('div');
    toast.className = 'toast-msg';
    if (isError) toast.style.backgroundColor = 'var(--danger)';
    toast.textContent = message;
    container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      setTimeout(() => toast.remove(), 250);
    }, 3500);
  }
};
