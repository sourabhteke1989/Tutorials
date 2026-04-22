import React, { useEffect, useState } from 'react';
import apiService from '../services/apiService';

export default function DashboardPage({ onLogout }) {
  const [user, setUser] = useState(apiService.getUser());
  const [org, setOrg] = useState(null);
  const [users, setUsers] = useState([]);
  const [version, setVersion] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        const [me, ver] = await Promise.all([apiService.getMe(), apiService.getVersion()]);
        setUser(me);
        setVersion(ver);

        if (me.tenant_id) {
          const orgData = await apiService.getOrganizationDetails(me.tenant_id);
          setOrg(orgData);
        }

        const userList = await apiService.getUsers();
        setUsers(Array.isArray(userList) ? userList : []);
      } catch (err) {
        setError(err.message);
      }
    };
    load();
  }, []);

  const handleLogout = async () => {
    await apiService.logout();
    onLogout();
  };

  return (
    <div style={{ maxWidth: 800, margin: '40px auto', padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h2>Dashboard</h2>
        <button onClick={handleLogout} data-testid="logout-button" style={{ padding: '8px 16px', cursor: 'pointer' }}>Logout</button>
      </div>

      {error && <div style={{ color: 'red', marginBottom: 12 }}>{error}</div>}

      {user && (
        <div data-testid="user-info" style={{ marginBottom: 24, padding: 16, border: '1px solid #ddd', borderRadius: 8 }}>
          <h3>User Profile</h3>
          <p><strong>Name:</strong> {user.user_name}</p>
          <p><strong>Email:</strong> {user.email}</p>
          <p><strong>Tenant:</strong> {user.tenant_id}</p>
          <p><strong>Status:</strong> {user.status}</p>
        </div>
      )}

      {org && (
        <div data-testid="org-info" style={{ marginBottom: 24, padding: 16, border: '1px solid #ddd', borderRadius: 8 }}>
          <h3>Organization</h3>
          <p><strong>Name:</strong> {org.name}</p>
          <p><strong>Region:</strong> {org.region}</p>
          <p><strong>Country:</strong> {org.country}</p>
          <p><strong>Status:</strong> {org.status}</p>
        </div>
      )}

      {users.length > 0 && (
        <div data-testid="users-list" style={{ marginBottom: 24, padding: 16, border: '1px solid #ddd', borderRadius: 8 }}>
          <h3>Users ({users.length})</h3>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr><th style={th}>Name</th><th style={th}>Email</th><th style={th}>Status</th></tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.user_id}><td style={td}>{u.user_name}</td><td style={td}>{u.email}</td><td style={td}>{u.status}</td></tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {version && (
        <div data-testid="version-info" style={{ fontSize: 12, color: '#888' }}>Service: {version.service} v{version.version}</div>
      )}
    </div>
  );
}

const th = { textAlign: 'left', borderBottom: '1px solid #ddd', padding: 8 };
const td = { padding: 8, borderBottom: '1px solid #eee' };
