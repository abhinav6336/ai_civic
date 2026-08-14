/**
 * Civic Reporting Platform - Main Application Logic (Vanilla JavaScript)
 */

document.addEventListener('DOMContentLoaded', () => {
  App.init();
});

const App = {
  departments: [],
  currentTab: 'report',
  selectedIssue: null,

  async init() {
    this.setupTabs();
    this.setupEventListeners();
    await this.loadDepartments();
    await this.loadStats();
    await this.loadIssues();
  },

  setupTabs() {
    const tabBtns = document.querySelectorAll('.nav-tab-btn');
    tabBtns.forEach(btn => {
      btn.addEventListener('click', (e) => {
        const tabId = btn.getAttribute('data-tab');
        this.switchTab(tabId);
      });
    });
  },

  switchTab(tabId) {
    this.currentTab = tabId;
    document.querySelectorAll('.nav-tab-btn').forEach(btn => {
      btn.classList.toggle('active', btn.getAttribute('data-tab') === tabId);
    });
    document.querySelectorAll('.tab-pane').forEach(pane => {
      pane.classList.toggle('active', pane.id === `pane-${tabId}`);
    });

    if (tabId === 'tracker') {
      this.loadIssues();
    } else if (tabId === 'departments') {
      this.renderDepartmentsList();
    }
  },

  setupEventListeners() {
    // Form submission
    const form = document.getElementById('issue-form');
    if (form) {
      form.addEventListener('submit', (e) => this.handleIssueSubmit(e));
    }

    // Geolocation button
    const geoBtn = document.getElementById('btn-get-location');
    if (geoBtn) {
      geoBtn.addEventListener('click', () => this.detectLocation());
    }

    // Quick search from hero
    const quickSearchBtn = document.getElementById('btn-quick-search');
    const quickSearchInput = document.getElementById('quick-search-input');
    if (quickSearchBtn && quickSearchInput) {
      quickSearchBtn.addEventListener('click', () => {
        const code = quickSearchInput.value.trim();
        if (code) {
          this.trackByCode(code);
        }
      });
      quickSearchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          quickSearchBtn.click();
        }
      });
    }

    // Filter controls in tracker
    const statusFilter = document.getElementById('filter-status');
    const categoryFilter = document.getElementById('filter-category');
    const deptFilter = document.getElementById('filter-department');
    const searchFilter = document.getElementById('filter-search');

    [statusFilter, categoryFilter, deptFilter].forEach(el => {
      if (el) el.addEventListener('change', () => this.loadIssues());
    });

    if (searchFilter) {
      let debounceTimer;
      searchFilter.addEventListener('input', () => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => this.loadIssues(), 350);
      });
    }

    // Modal Close
    const closeBtn = document.getElementById('modal-close');
    const backdrop = document.getElementById('issue-modal');
    if (closeBtn && backdrop) {
      closeBtn.addEventListener('click', () => backdrop.classList.remove('open'));
      backdrop.addEventListener('click', (e) => {
        if (e.target === backdrop) backdrop.classList.remove('open');
      });
    }

    // Status update in modal
    const updateStatusBtn = document.getElementById('btn-modal-update-status');
    if (updateStatusBtn) {
      updateStatusBtn.addEventListener('click', () => this.handleModalStatusUpdate());
    }

    // Department assignment in modal
    const assignDeptBtn = document.getElementById('btn-modal-assign-dept');
    if (assignDeptBtn) {
      assignDeptBtn.addEventListener('click', () => this.handleModalDepartmentAssign());
    }
  },

  async loadDepartments() {
    try {
      const res = await CivicApi.getDepartments();
      if (res.success) {
        this.departments = res.data;
        this.populateDepartmentDropdowns();
      }
    } catch (err) {
      console.error('Failed to load departments:', err);
    }
  },

  populateDepartmentDropdowns() {
    const filterDept = document.getElementById('filter-department');
    const modalDept = document.getElementById('modal-assign-dept-select');

    if (filterDept) {
      filterDept.innerHTML = '<option value="">All Departments</option>';
      this.departments.forEach(d => {
        filterDept.innerHTML += `<option value="${d.id}">${d.name}</option>`;
      });
    }

    if (modalDept) {
      modalDept.innerHTML = '<option value="">Select Department...</option>';
      this.departments.forEach(d => {
        modalDept.innerHTML += `<option value="${d.id}">${d.name}</option>`;
      });
    }
  },

  async loadStats() {
    try {
      const res = await CivicApi.getDashboardStats();
      if (res.success && res.data) {
        const d = res.data;
        document.getElementById('stat-total').textContent = d.totalIssues;
        document.getElementById('stat-progress').textContent = d.inProgressIssues;
        document.getElementById('stat-resolved').textContent = d.resolvedIssues;
        document.getElementById('stat-ai').textContent = d.aiClassifiedIssues;
      }
    } catch (err) {
      console.error('Failed to load stats:', err);
    }
  },

  async loadIssues() {
    const grid = document.getElementById('issues-grid');
    if (!grid) return;

    grid.innerHTML = '<div style="grid-column: 1/-1; text-align:center; padding: 2rem; color: var(--color-text-muted);">Loading issues...</div>';

    const status = document.getElementById('filter-status')?.value;
    const category = document.getElementById('filter-category')?.value;
    const departmentId = document.getElementById('filter-department')?.value;
    const search = document.getElementById('filter-search')?.value;

    try {
      const res = await CivicApi.getIssues({ status, category, departmentId, search });
      if (res.success) {
        this.renderIssuesGrid(res.data);
      }
    } catch (err) {
      grid.innerHTML = `<div style="grid-column: 1/-1; text-align:center; padding: 2rem; color: #dc2626;">Failed to load issues: ${err.message}</div>`;
    }
  },

  renderIssuesGrid(issues) {
    const grid = document.getElementById('issues-grid');
    if (!issues || issues.length === 0) {
      grid.innerHTML = `
        <div style="grid-column: 1/-1; text-align: center; padding: 3rem 1rem; background: white; border-radius: var(--radius-lg); border: 1px dashed var(--color-border);">
          <div style="font-size: 2rem; margin-bottom: 0.5rem;">🔍</div>
          <h3 style="font-size: 1.1rem; color: var(--color-text-main); margin-bottom: 0.25rem;">No Civic Issues Found</h3>
          <p style="font-size: 0.85rem; color: var(--color-text-muted);">No reports match your current search or filter criteria.</p>
        </div>`;
      return;
    }

    grid.innerHTML = issues.map(issue => `
      <div class="issue-card" onclick="App.openIssueDetails(${issue.id})">
        <div>
          <div class="issue-card-top">
            <span class="tracking-code">${issue.trackingNumber}</span>
            <span class="badge badge-${issue.status}">${issue.statusLabel || issue.status}</span>
          </div>
          <h3 class="issue-card-title">${this.escapeHtml(issue.title)}</h3>
          <p class="issue-card-desc">${this.escapeHtml(issue.description)}</p>
        </div>

        <div class="issue-card-meta">
          <span class="meta-item">📁 ${issue.categoryDisplayName || issue.category}</span>
          <span class="meta-item">🏛️ ${issue.assignedDepartmentName || 'Pending Routing'}</span>
          ${issue.aiConfidence ? `<span class="meta-item" style="color: #6d28d9;">🤖 ${(issue.aiConfidence * 100).toFixed(0)}% AI</span>` : ''}
        </div>
      </div>
    `).join('');
  },

  async openIssueDetails(id) {
    try {
      const res = await CivicApi.getIssueById(id);
      if (res.success && res.data) {
        this.selectedIssue = res.data;
        this.renderModalContent(res.data);
        document.getElementById('issue-modal').classList.add('open');
      }
    } catch (err) {
      this.showToast(`Error: ${err.message}`, true);
    }
  },

  async trackByCode(trackingNumber) {
    try {
      const res = await CivicApi.getIssueByTracking(trackingNumber);
      if (res.success && res.data) {
        this.openIssueDetails(res.data.id);
      }
    } catch (err) {
      this.showToast(`Tracking lookup failed: ${err.message}`, true);
    }
  },

  renderModalContent(issue) {
    document.getElementById('modal-tracking-no').textContent = issue.trackingNumber;
    document.getElementById('modal-status-badge').className = `badge badge-${issue.status}`;
    document.getElementById('modal-status-badge').textContent = issue.statusLabel || issue.status;
    document.getElementById('modal-title').textContent = issue.title;
    document.getElementById('modal-desc').textContent = issue.description;

    document.getElementById('modal-meta-category').textContent = issue.categoryDisplayName || issue.category;
    document.getElementById('modal-meta-dept').textContent = issue.assignedDepartmentName || 'Unassigned';
    document.getElementById('modal-meta-officer').textContent = issue.assignedOfficerName || 'Not assigned';
    document.getElementById('modal-meta-reporter').textContent = `${issue.citizenName || 'Citizen'} (${issue.citizenEmail || 'N/A'})`;
    document.getElementById('modal-meta-address').textContent = issue.address || (issue.latitude ? `GPS: ${issue.latitude.toFixed(4)}, ${issue.longitude.toFixed(4)}` : 'Not provided');
    document.getElementById('modal-meta-date').textContent = new Date(issue.createdAt).toLocaleString();

    // AI Classification Card Box
    const aiBox = document.getElementById('modal-ai-box');
    if (issue.aiConfidence) {
      aiBox.style.display = 'block';
      document.getElementById('modal-ai-details').textContent = `Classified as ${issue.aiSuggestedCategory || issue.category} with ${(issue.aiConfidence * 100).toFixed(1)}% model confidence score.`;
    } else {
      aiBox.style.display = 'none';
    }

    // Set current status in select dropdown
    const statusSelect = document.getElementById('modal-status-select');
    if (statusSelect) statusSelect.value = issue.status;

    // Render Timeline Updates
    const timeline = document.getElementById('modal-timeline');
    if (issue.updates && issue.updates.length > 0) {
      timeline.innerHTML = issue.updates.map(u => `
        <div class="timeline-item">
          <div class="timeline-dot"></div>
          <div class="timeline-content">
            <div class="timeline-title">
              <span>${this.formatUpdateType(u.updateType)} ${u.newStatus ? `&rarr; <span class="badge badge-${u.newStatus}">${u.newStatus}</span>` : ''}</span>
              <span class="timeline-date">${new Date(u.createdAt).toLocaleDateString()} ${new Date(u.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</span>
            </div>
            <div class="timeline-notes">${this.escapeHtml(u.notes || '')}</div>
            <div style="font-size: 0.75rem; color: var(--color-text-muted); margin-top: 0.25rem;">Updated by: ${u.updatedByUserName || 'System'}</div>
          </div>
        </div>
      `).join('');
    } else {
      timeline.innerHTML = '<div style="font-size: 0.85rem; color: var(--color-text-muted);">No timeline updates recorded yet.</div>';
    }
  },

  async handleModalStatusUpdate() {
    if (!this.selectedIssue) return;
    const newStatus = document.getElementById('modal-status-select').value;
    const notes = document.getElementById('modal-status-notes').value.trim();

    if (newStatus === this.selectedIssue.status) {
      this.showToast('Please select a different status to update.', true);
      return;
    }

    try {
      const res = await CivicApi.updateIssueStatus(this.selectedIssue.id, newStatus, notes, null);
      if (res.success) {
        this.showToast('Issue status updated successfully!');
        document.getElementById('modal-status-notes').value = '';
        this.openIssueDetails(this.selectedIssue.id);
        this.loadStats();
        this.loadIssues();
      }
    } catch (err) {
      this.showToast(`Failed to update status: ${err.message}`, true);
    }
  },

  async handleModalDepartmentAssign() {
    if (!this.selectedIssue) return;
    const deptId = document.getElementById('modal-assign-dept-select').value;
    const notes = document.getElementById('modal-assign-notes').value.trim();

    if (!deptId) {
      this.showToast('Please choose a department to assign.', true);
      return;
    }

    try {
      const res = await CivicApi.assignIssue(this.selectedIssue.id, deptId, null, notes, null);
      if (res.success) {
        this.showToast('Department assigned successfully!');
        document.getElementById('modal-assign-notes').value = '';
        this.openIssueDetails(this.selectedIssue.id);
        this.loadStats();
        this.loadIssues();
      }
    } catch (err) {
      this.showToast(`Assignment failed: ${err.message}`, true);
    }
  },

  async handleIssueSubmit(e) {
    e.preventDefault();
    const btn = document.getElementById('btn-submit-issue');
    btn.disabled = true;
    btn.textContent = 'Submitting...';

    const title = document.getElementById('issue-title').value;
    const description = document.getElementById('issue-description').value;
    const category = document.querySelector('input[name="category"]:checked')?.value || 'OTHER';
    const address = document.getElementById('issue-address').value;
    const lat = document.getElementById('issue-lat').value ? parseFloat(document.getElementById('issue-lat').value) : null;
    const lng = document.getElementById('issue-lng').value ? parseFloat(document.getElementById('issue-lng').value) : null;
    const citizenName = document.getElementById('citizen-name').value;
    const citizenEmail = document.getElementById('citizen-email').value;
    const citizenPhone = document.getElementById('citizen-phone').value;

    const payload = {
      title,
      description,
      category,
      address,
      latitude: lat,
      longitude: lng,
      citizenName,
      citizenEmail,
      citizenPhone
    };

    try {
      const res = await CivicApi.createIssue(payload);
      if (res.success && res.data) {
        this.showToast(`Issue ${res.data.trackingNumber} submitted successfully!`);
        document.getElementById('issue-form').reset();
        await this.loadStats();
        this.switchTab('tracker');
        this.openIssueDetails(res.data.id);
      }
    } catch (err) {
      this.showToast(`Failed to submit issue: ${err.message}`, true);
    } finally {
      btn.disabled = false;
      btn.textContent = 'Submit Civic Report';
    }
  },

  detectLocation() {
    if (!navigator.geolocation) {
      this.showToast('Geolocation is not supported by your browser.', true);
      return;
    }

    const geoBtn = document.getElementById('btn-get-location');
    geoBtn.textContent = 'Acquiring GPS...';

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        document.getElementById('issue-lat').value = pos.coords.latitude.toFixed(6);
        document.getElementById('issue-lng').value = pos.coords.longitude.toFixed(6);
        geoBtn.textContent = '📍 GPS Coordinates Captured';
        this.showToast('Location coordinates captured!');
      },
      (err) => {
        geoBtn.textContent = '📍 Get Current Location';
        this.showToast(`Geolocation error: ${err.message}`, true);
      }
    );
  },

  renderDepartmentsList() {
    const list = document.getElementById('departments-list');
    if (!list) return;

    list.innerHTML = this.departments.map(d => `
      <div class="card" style="padding: 1.5rem;">
        <div style="display:flex; justify-content:space-between; align-items:flex-start;">
          <div>
            <span class="tracking-code" style="background:#f1f5f9; padding: 2px 6px; border-radius:4px;">${d.code}</span>
            <h3 style="margin-top: 0.5rem; font-family: var(--font-heading);">${d.name}</h3>
          </div>
          <span class="badge" style="background:#ecfdf5; color:#047857; border:1px solid #a7f3d0;">Active Jurisdiction</span>
        </div>
        <p style="font-size: 0.875rem; color: var(--color-text-secondary); margin: 0.75rem 0;">${d.description || 'Municipal department for civic maintenance.'}</p>
        <div style="font-size: 0.8rem; color: var(--color-text-muted); display:flex; gap: 1.5rem; border-top: 1px solid var(--color-border); padding-top: 0.75rem;">
          <span>📧 ${d.contactEmail || 'contact@civic.gov'}</span>
          <span>📞 ${d.contactPhone || '1-800-CIVIC'}</span>
        </div>
      </div>
    `).join('');
  },

  formatUpdateType(type) {
    const map = {
      'INITIAL_REPORT': '📝 Issue Submitted',
      'AI_CLASSIFICATION': '🤖 AI Classification & Auto-Route',
      'STATUS_CHANGE': '🔄 Status Update',
      'DEPARTMENT_ASSIGNMENT': '🏛️ Department Assigned',
      'RESOLUTION': '✅ Resolution Recorded'
    };
    return map[type] || type;
  },

  escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  },

  showToast(message, isError = false) {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = 'toast';
    if (isError) toast.style.backgroundColor = '#991b1b';
    toast.textContent = message;
    container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      setTimeout(() => toast.remove(), 300);
    }, 4000);
  }
};
