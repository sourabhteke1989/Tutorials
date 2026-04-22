import React, { useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import apiService from './services/apiService';

function App() {
  const [authenticated, setAuthenticated] = useState(apiService.isAuthenticated());

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={
          authenticated ? <Navigate to="/dashboard" /> : <LoginPage onLogin={() => setAuthenticated(true)} />
        } />
        <Route path="/dashboard" element={
          authenticated ? <DashboardPage onLogout={() => setAuthenticated(false)} /> : <Navigate to="/login" />
        } />
        <Route path="*" element={<Navigate to={authenticated ? '/dashboard' : '/login'} />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
