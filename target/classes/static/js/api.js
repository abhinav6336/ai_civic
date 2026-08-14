/**
 * Civic Reporting Platform - Frontend API Client (Fetch API)
 */
const API_BASE_URL = '/api/v1';

const CivicApi = {
  // Issues
  async getIssues(params = {}) {
    const query = new URLSearchParams();
    if (params.status) query.append('status', params.status);
    if (params.category) query.append('category', params.category);
    if (params.departmentId) query.append('departmentId', params.departmentId);
    if (params.search) query.append('search', params.search);

    const url = `${API_BASE_URL}/issues${query.toString() ? '?' + query.toString() : ''}`;
    const response = await fetch(url);
    if (!response.ok) throw await this.parseError(response);
    return await response.json();
  },

  async getIssueByTracking(trackingNumber) {
    const response = await fetch(`${API_BASE_URL}/issues/track/${encodeURIComponent(trackingNumber)}`);
    if (!response.ok) throw await this.parseError(response);
    return await response.json();
  },

  async getIssueById(id) {
    const response = await fetch(`${API_BASE_URL}/issues/${id}`);
    if (!response.ok) throw await this.parseError(response);
    return await response.json();
  },

  async createIssue(payload) {
    const response = await fetch(`${API_BASE_URL}/issues`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    if (!response.ok) throw await this.parseError(response);
    return await response.json();
  },

  async updateIssueStatus(id, newStatus, notes, updatedByUserId) {
    const response = await fetch(`${API_BASE_URL}/issues/${id}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ newStatus, notes, updatedByUserId })
    });
    if (!response.ok) throw await this.parseError(response);
    return await response.json();
  },

  async assignIssue(id, departmentId, officerId, notes, assignedByUserId) {
    const response = await fetch(`${API_BASE_URL}/issues/${id}/assign`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ departmentId, officerId, notes, assignedByUserId })
    });
    if (!response.ok) throw await this.parseError(response);
    return await response.json();
  },

  // Departments
  async getDepartments() {
    const response = await fetch(`${API_BASE_URL}/departments`);
    if (!response.ok) throw await this.parseError(response);
    return await response.json();
  },

  // Users / Officers
  async getOfficers(departmentId) {
    const url = departmentId 
      ? `${API_BASE_URL}/users/officers/department/${departmentId}`
      : `${API_BASE_URL}/users?role=OFFICER`;
    const response = await fetch(url);
    if (!response.ok) throw await this.parseError(response);
    return await response.json();
  },

  // Dashboard Stats
  async getDashboardStats() {
    const response = await fetch(`${API_BASE_URL}/dashboard/stats`);
    if (!response.ok) throw await this.parseError(response);
    return await response.json();
  },

  // Helper error parser
  async parseError(response) {
    try {
      const errJson = await response.json();
      const msg = errJson.message || (errJson.details && errJson.details.join(', ')) || response.statusText;
      return new Error(msg);
    } catch {
      return new Error(`Server returned ${response.status}: ${response.statusText}`);
    }
  }
};
