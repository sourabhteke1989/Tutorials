import React, { useState } from 'react';
import apiService from '../services/apiService';

export default function LoginPage({ onLogin }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [tenantId, setTenantId] = useState('maxlogic');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await apiService.login(username, password, tenantId);
      onLogin();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 400, margin: '80px auto', padding: 24, border: '1px solid #ddd', borderRadius: 8 }}>
      <h2>Login</h2>
      {error && <div data-testid="login-error" style={{ color: 'red', marginBottom: 12 }}>{error}</div>}
      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 12 }}>
          <label htmlFor="tenantId">Tenant</label><br />
          <select id="tenantId" value={tenantId} onChange={(e) => setTenantId(e.target.value)} style={{ width: '100%', padding: 8 }}>
            <option value="maxlogic">MaxLogic Solutions</option>
            <option value="acme">Acme Corp</option>
            <option value="globex">Globex Inc</option>
            <option value="initech">Initech</option>
          </select>
        </div>
        <div style={{ marginBottom: 12 }}>
          <label htmlFor="username">Email</label><br />
          <input id="username" type="email" value={username} onChange={(e) => setUsername(e.target.value)} required style={{ width: '100%', padding: 8 }} />
        </div>
        <div style={{ marginBottom: 12 }}>
          <label htmlFor="password">Password</label><br />
          <input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required style={{ width: '100%', padding: 8 }} />
        </div>
        <button type="submit" disabled={loading} data-testid="login-button" style={{ width: '100%', padding: 10, background: '#1976d2', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer' }}>
          {loading ? 'Logging in...' : 'Login'}
        </button>
      </form>
      <div style={{ marginTop: 16, fontSize: 12, color: '#888' }}>
        <strong>Test accounts:</strong><br />
        admin@maxlogic.com / max123 (MaxLogic)<br />
        lisa@maxlogic.com / lisa123 (MaxLogic)<br />
        admin@acme.com / admin123 (Acme)<br />
        jane@acme.com / jane123 (Acme)<br />
        hans@globex.com / hans123 (Globex)
      </div>
    </div>
  );
}
