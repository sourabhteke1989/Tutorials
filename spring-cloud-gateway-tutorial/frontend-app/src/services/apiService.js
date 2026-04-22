const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://maxlogic.yourorg.com';

class ApiService {
  constructor() {
    this.baseUrl = API_BASE_URL;
  }

  getToken() {
    return localStorage.getItem('access_token');
  }

  setToken(token) {
    localStorage.setItem('access_token', token);
  }

  clearToken() {
    localStorage.removeItem('access_token');
    localStorage.removeItem('user');
  }

  getUser() {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  }

  setUser(user) {
    localStorage.setItem('user', JSON.stringify(user));
  }

  isAuthenticated() {
    return !!this.getToken();
  }

  async request(path, options = {}) {
    const url = `${this.baseUrl}${path}`;
    const headers = { 'Content-Type': 'application/json', ...options.headers };
    const token = this.getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
      headers['X-Auth-Mode'] = 'internal';
    }

    const response = await fetch(url, { ...options, headers });

    if (response.status === 401) {
      this.clearToken();
      window.location.href = '/login';
      throw new Error('Session expired');
    }

    return response;
  }

  async login(username, password, tenantId) {
    const response = await this.request('/user-mgmt/api/auth/internal-auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password, tenant_id: tenantId }),
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Login failed' }));
      throw new Error(error.message || 'Login failed');
    }

    const data = await response.json();
    this.setToken(data.access_token);
    this.setUser(data.user);
    return data;
  }

  async getMe() {
    const response = await this.request('/user-mgmt/api/user/me');
    if (!response.ok) throw new Error('Failed to fetch user');
    return response.json();
  }

  async getOrganizationDetails(tenantId) {
    const response = await this.request(`/user-mgmt/api/organization/details?tenant_id=${encodeURIComponent(tenantId)}`);
    if (!response.ok) throw new Error('Failed to fetch organization');
    return response.json();
  }

  async getUsers() {
    const response = await this.request('/user-mgmt/api/user/list');
    if (!response.ok) throw new Error('Failed to fetch users');
    return response.json();
  }

  async getVersion() {
    const response = await this.request('/user-mgmt/api/version');
    if (!response.ok) throw new Error('Failed to fetch version');
    return response.json();
  }

  async logout() {
    try {
      await this.request('/user-mgmt/api/auth/internal-auth/logout', { method: 'POST' });
    } finally {
      this.clearToken();
    }
  }
}

const apiService = new ApiService();
export default apiService;
