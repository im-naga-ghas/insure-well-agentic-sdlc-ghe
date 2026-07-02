import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import './App.css';
import Navigation from './components/Navigation';
import Dashboard from './components/Dashboard';
import Claims from './components/Claims';

const API_BASE_URL = 'http://localhost:8080/api';

function App() {
  const [currentPage, setCurrentPage] = useState('dashboard');
  const [auth, setAuth] = useState(null);
  const [policies, setPolicies] = useState([]);
  const [claims, setClaims] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [loginForm, setLoginForm] = useState({ username: '', password: '' });
  const [loginError, setLoginError] = useState('');

  const authToken = auth?.authToken || '';
  const authHeaders = authToken ? { Authorization: authToken } : {};

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const headers = authToken ? { Authorization: authToken } : {};
      const [policiesRes, claimsRes] = await Promise.all([
        axios.get(`${API_BASE_URL}/policies`, { headers }),
        axios.get(`${API_BASE_URL}/claims`, { headers }),
      ]);
      setPolicies(policiesRes.data);
      setClaims(claimsRes.data);
      setError(null);
    } catch (err) {
      if (err.response?.status === 401) {
        setAuth(null);
        setLoginError('Session expired. Please sign in again.');
        setPolicies([]);
        setClaims([]);
      } else {
        setError('Failed to load data from backend. Ensure Spring Boot server is running on port 8080.');
      }
    } finally {
      setLoading(false);
    }
  }, [authToken]);

  useEffect(() => {
    if (auth) {
      fetchData();
    }
  }, [auth, fetchData]);

  const handleLoginInput = (e) => {
    const { name, value } = e.target;
    setLoginForm(prev => ({ ...prev, [name]: value }));
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoginError('');
    setError(null);

    if (!loginForm.username || !loginForm.password) {
      setLoginError('Username and password are required');
      return;
    }

    const headers = { Authorization: `Basic ${btoa(`${loginForm.username}:${loginForm.password}`)}` };

    try {
      const me = await axios.get(`${API_BASE_URL}/auth/me`, { headers });
      setAuth({
        username: me.data.username || loginForm.username,
        authToken: headers.Authorization,
        role: me.data.role,
        displayName: me.data.holderName || loginForm.username,
      });
      setLoginForm({ username: '', password: '' });
      setCurrentPage('dashboard');
    } catch (err) {
      setLoginError('Invalid credentials');
    }
  };

  const handleLogout = () => {
    setAuth(null);
    setPolicies([]);
    setClaims([]);
    setError(null);
  };

  const refreshData = () => {
    fetchData();
  };

  if (!auth) {
    return (
      <div className="auth-page" data-testid="login-page">
        <div className="auth-card">
          <h1>InsureWell Login</h1>
          <p>Sign in with your policyholder or admin account.</p>
          {loginError && <div className="alert alert-error" data-testid="login-error">{loginError}</div>}
          <form onSubmit={handleLogin} data-testid="login-form">
            <div className="form-group">
              <label>Username</label>
              <input
                type="text"
                name="username"
                value={loginForm.username}
                onChange={handleLoginInput}
                data-testid="login-username"
              />
            </div>
            <div className="form-group">
              <label>Password</label>
              <input
                type="password"
                name="password"
                value={loginForm.password}
                onChange={handleLoginInput}
                data-testid="login-password"
              />
            </div>
            <button type="submit" className="btn btn-primary" data-testid="login-submit">
              Login
            </button>
          </form>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="app">
        <Navigation
          currentPage={currentPage}
          setCurrentPage={setCurrentPage}
          role={auth.role}
          displayName={auth.displayName}
          onLogout={handleLogout}
        />
        <main className="main-content">
          <div className="loader">Loading...</div>
        </main>
      </div>
    );
  }

  if (error) {
    return (
      <div className="app">
        <Navigation
          currentPage={currentPage}
          setCurrentPage={setCurrentPage}
          role={auth.role}
          displayName={auth.displayName}
          onLogout={handleLogout}
        />
        <main className="main-content">
          <div className="error-banner">{error}</div>
        </main>
      </div>
    );
  }

  return (
    <div className="app">
      <Navigation
        currentPage={currentPage}
        setCurrentPage={setCurrentPage}
        role={auth.role}
        displayName={auth.displayName}
        onLogout={handleLogout}
      />
      <main className="main-content">
        {currentPage === 'dashboard' && (
          <Dashboard
            policies={policies}
            claims={claims}
            onRefresh={refreshData}
            apiBase={API_BASE_URL}
            canManagePolicies={auth.role === 'ADMIN'}
            authHeaders={authHeaders}
          />
        )}
        {currentPage === 'claims' && (
          <Claims
            policies={policies}
            claims={claims}
            onRefresh={refreshData}
            apiBase={API_BASE_URL}
            canReviewClaims={auth.role === 'ADMIN'}
            authHeaders={authHeaders}
          />
        )}
      </main>
    </div>
  );
}

export default App;
