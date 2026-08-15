/**
 * Civic Reporting Platform - REST API Client Wrapper (Fetch API)
 */
const API_BASE = '/api/v1';

const CivicApi = {
  // Store authentication token
  getAuthToken() {
    return localStorage.getItem('civic_auth_token') || '';
  },

  setAuthToken(token) {
    if (token) localStorage.setItem('civic_auth_token', token);
    else localStorage.removeItem('civic_auth_token');
  },

  getAuthHeaders(isMultipart = false) {
    const token = this.getAuthToken();
    const headers = {};
    if (!isMultipart) {
      headers['Content-Type'] = 'application/json';
    }
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
  },

  // Citizen Complaint Submission (Supports FormData / Multipart & JSON)
  async submitIssue(payload) {
    const isMultipart = (typeof FormData !== 'undefined' && payload instanceof FormData);
    const headers = this.getAuthHeaders(isMultipart);

    const res = await fetch(`${API_BASE}/issues`, {
      method: 'POST',
      headers: headers,
      body: isMultipart ? payload : JSON.stringify(payload)
    });

    if (!res.ok) throw await this.extractError(res);
    return await res.json();
  },

  // Track Complaint by Tracking ID
  async trackIssue(trackingNumber) {
    const res = await fetch(`${API_BASE}/issues/track/${encodeURIComponent(trackingNumber.trim())}`);
    if (!res.ok) throw await this.extractError(res);
    return await res.json();
  },

  // Get Complaint by numeric ID
  async getIssueById(id) {
    const res = await fetch(`${API_BASE}/issues/${id}`, {
      headers: this.getAuthHeaders()
    });
    if (!res.ok) throw await this.extractError(res);
    return await res.json();
  },

  // Filter Issues
  async getIssues(params = {}) {
    const q = new URLSearchParams();
    if (params.status) q.append('status', params.status);
    if (params.category) q.append('category', params.category);
    if (params.departmentId) q.append('departmentId', params.departmentId);
    if (params.search) q.append('search', params.search);

    const url = `${API_BASE}/issues${q.toString() ? '?' + q.toString() : ''}`;
    const res = await fetch(url, { headers: this.getAuthHeaders() });
    if (!res.ok) throw await this.extractError(res);
    return await res.json();
  },

  // Admin Auth
  async loginAdmin(email, password) {
    const res = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });
    if (!res.ok) throw await this.extractError(res);
    const data = await res.json();
    if (data.success && data.data && data.data.token) {
      this.setAuthToken(data.data.token);
      localStorage.setItem('civic_user', JSON.stringify(data.data));
    }
    return data;
  },

  async getCurrentUser() {
    const res = await fetch(`${API_BASE}/auth/me`, {
      headers: this.getAuthHeaders()
    });
    if (!res.ok) return null;
    return await res.json();
  },

  logoutAdmin() {
    this.setAuthToken(null);
    localStorage.removeItem('civic_user');
  },

  // Admin Status Update
  async updateStatus(id, newStatus, notes, updatedByUserId) {
    const res = await fetch(`${API_BASE}/issues/${id}/status`, {
      method: 'PATCH',
      headers: this.getAuthHeaders(),
      body: JSON.stringify({ newStatus, notes, updatedByUserId })
    });
    if (!res.ok) throw await this.extractError(res);
    return await res.json();
  },

  // Admin Assignment
  async assignDepartment(id, departmentId, officerId, notes, assignedByUserId) {
    const res = await fetch(`${API_BASE}/issues/${id}/assign`, {
      method: 'PUT',
      headers: this.getAuthHeaders(),
      body: JSON.stringify({ departmentId, officerId, notes, assignedByUserId })
    });
    if (!res.ok) throw await this.extractError(res);
    return await res.json();
  },

  // Departments
  async getDepartments() {
    const res = await fetch(`${API_BASE}/departments`);
    if (!res.ok) throw await this.extractError(res);
    return await res.json();
  },

  // Officers
  async getOfficers(departmentId) {
    const url = departmentId
      ? `${API_BASE}/users/officers/department/${departmentId}`
      : `${API_BASE}/users?role=OFFICER`;
    const res = await fetch(url, { headers: this.getAuthHeaders() });
    if (!res.ok) throw await this.extractError(res);
    return await res.json();
  },

  // Dashboard Stats
  async getStats() {
    const res = await fetch(`${API_BASE}/dashboard/stats`, {
      headers: this.getAuthHeaders()
    });
    if (!res.ok) throw await this.extractError(res);
    return await res.json();
  },

  async extractError(res) {
    try {
      const json = await res.json();
      return new Error(json.message || (json.details && json.details.join(', ')) || res.statusText);
    } catch {
      return new Error(`Server returned ${res.status}: ${res.statusText}`);
    }
  }
};
