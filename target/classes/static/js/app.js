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
      this.resetWizard();
    } else if (viewName === 'my-complaints') {
      this.loadMyComplaints();
    } else if (viewName === 'admin-dashboard') {
      this.checkAdminAuthAndLoad();
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

  resetWizard() {
    this.goToStep(1);
    this.selectedPhotoFile = null;
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
          ${this.getStatusBadgeHtml(c.status)}
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
    const currentIndex = order.indexOf(status);

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

    // AI Classification (Plain Language for Citizen, Detailed for Admin)
    const aiBox = document.getElementById('modal-ai-box');
    if (isAdmin) {
      aiBox.style.display = 'block';
      aiBox.innerHTML = `
        <strong>🤖 AI Classification Details:</strong><br>
        Detected: <b>${issue.aiSuggestedCategory || issue.category}</b> | Confidence: <b>${issue.aiConfidence ? (issue.aiConfidence * 100).toFixed(0) + '%' : 'Verified'}</b>
      `;
    } else if (issue.aiSuggestedCategory) {
      aiBox.style.display = 'block';
      aiBox.innerHTML = `<strong>Detected issue:</strong> ${issue.aiSuggestedCategory || issue.category}`;
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

    // Admin Sidebar Tabs
    document.querySelectorAll('.admin-menu-link[data-admin-tab]').forEach(tab => {
      tab.addEventListener('click', (e) => {
        e.preventDefault();
        document.querySelectorAll('.admin-menu-link').forEach(t => t.classList.remove('active'));
        tab.classList.add('active');
        const tabName = tab.getAttribute('data-admin-tab');
        this.switchAdminTab(tabName);
      });
    });

    // Admin Filter Controls
    const statusFilter = document.getElementById('admin-filter-status');
    const deptFilter = document.getElementById('admin-filter-dept');
    const searchFilter = document.getElementById('admin-filter-search');

    if (statusFilter) statusFilter.addEventListener('change', () => this.loadAdminComplaintsTable());
    if (deptFilter) deptFilter.addEventListener('change', () => this.loadAdminComplaintsTable());
    if (searchFilter) {
      let timer;
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

    if (tabName === 'departments') {
      this.renderAdminDepartmentsTab();
    } else if (tabName === 'complaints') {
      this.loadAdminComplaintsTable();
    }
  },

  async loadAdminStats() {
    try {
      const res = await CivicApi.getStats();
      if (res.success && res.data) {
        const d = res.data;
        document.getElementById('adm-stat-total').textContent = d.totalIssues;
        document.getElementById('adm-stat-new').textContent = d.reportedIssues;
        document.getElementById('adm-stat-review').textContent = d.aiClassifiedIssues;
        document.getElementById('adm-stat-progress').textContent = d.inProgressIssues;
        document.getElementById('adm-stat-resolved').textContent = d.resolvedIssues;
      }
    } catch (err) {
      console.error('Stats load failed:', err);
    }
  },

  async loadAdminComplaintsTable() {
    const tableBody = document.getElementById('admin-complaints-tbody');
    if (!tableBody) return;

    tableBody.innerHTML = '<tr><td colspan="7" style="text-align:center; padding: 2rem;">Loading complaints...</td></tr>';

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
        <td style="font-family: monospace; font-weight: 700; color: var(--primary);">${i.trackingNumber}</td>
        <td><b>${this.escapeHtml(i.title || i.category)}</b></td>
        <td>${this.escapeHtml(i.address || 'N/A')}</td>
        <td>${i.assignedDepartmentName || '<span style="color:var(--text-light)">Unassigned</span>'}</td>
        <td>${this.getStatusBadgeHtml(i.status)}</td>
        <td>${new Date(i.createdAt).toLocaleDateString([], {month:'short', day:'numeric'})}</td>
        <td>
          <button class="btn btn-secondary" style="padding: 0.4rem 0.8rem; font-size: 0.85rem; min-height: 36px;" onclick="App.openAdminComplaintModal(${i.id})">
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
