import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './App.css';
import Navigation from './components/Navigation';
import Dashboard from './components/Dashboard';
import Claims from './components/Claims';

const API_BASE_URL = 'http://localhost:8080/api';
const DEMO_ACCOUNTS = [
  { username: 'alex', password: 'policy123', label: 'Alex Johnson · Policyholder' },
  { username: 'maria', password: 'policy123', label: 'Maria Garcia · Policyholder' },
  { username: 'david', password: 'policy123', label: 'David Chen · Policyholder' },
  { username: 'admin', password: 'admin123', label: 'Operations Admin' },
];

function App() {
  const [currentPage, setCurrentPage] = useState('dashboard');
  const [policies, setPolicies] = useState([]);
  const [claims, setClaims] = useState([]);
  const [auth, setAuth] = useState(null);
  const [currentUser, setCurrentUser] = useState(null);
  const [csrf, setCsrf] = useState(null);
  const [selectedAccount, setSelectedAccount] = useState(DEMO_ACCOUNTS[0].username);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    setLoading(false);
  }, []);

  const encodeBasicCredentials = (credentials) => {
    const bytes = new TextEncoder().encode(`${credentials.username}:${credentials.password}`);
    let binary = '';
    bytes.forEach((value) => {
      binary += String.fromCharCode(value);
    });
    return window.btoa(binary);
  };

  const buildRequestConfig = (credentials, csrfToken = csrf) => {
    const headers = {
      Authorization: `Basic ${encodeBasicCredentials(credentials)}`,
    };
    if (csrfToken?.headerName && csrfToken?.token) {
      headers[csrfToken.headerName] = csrfToken.token;
    }
    return {
      headers,
      withCredentials: true,
    };
  };

  const fetchData = async (credentials = auth) => {
    if (!credentials) {
      return;
    }

    try {
      setLoading(true);
      const authConfig = buildRequestConfig(credentials, null);
      const csrfRes = await axios.get(`${API_BASE_URL}/auth/csrf`, authConfig);
      const csrfToken = csrfRes.data;
      const [userRes, policiesRes, claimsRes] = await Promise.all([
        axios.get(`${API_BASE_URL}/auth/me`, authConfig),
        axios.get(`${API_BASE_URL}/policies`, authConfig),
        axios.get(`${API_BASE_URL}/claims`, authConfig),
      ]);
      setAuth(credentials);
      setCsrf(csrfToken);
      setCurrentUser(userRes.data);
      setPolicies(policiesRes.data);
      setClaims(claimsRes.data);
      setError(null);
    } catch (err) {
      setAuth(null);
      setCsrf(null);
      setCurrentUser(null);
      setPolicies([]);
      setClaims([]);
      setError(
        err.response?.status === 401
          ? 'Authentication failed. Choose a valid demo account and try again.'
          : 'Failed to load secured data from the backend. Ensure the Spring Boot server is running on port 8080.'
      );
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const refreshData = () => {
    fetchData();
  };

  const handleSignIn = () => {
    const credentials = DEMO_ACCOUNTS.find((account) => account.username === selectedAccount);
    fetchData(credentials);
  };

  const handleLogout = () => {
    setAuth(null);
    setCsrf(null);
    setCurrentUser(null);
    setPolicies([]);
    setClaims([]);
    setCurrentPage('dashboard');
    setError(null);
  };

  if (!auth || !currentUser) {
    const account = DEMO_ACCOUNTS.find((option) => option.username === selectedAccount);

    return (
      <div className="login-shell">
        <div className="login-card" data-testid="login-card">
          <h1>Secure access required</h1>
          <p>Choose a demo user to sign in with role-aware access to policies and claims.</p>

          {error && <div className="error-banner">{error}</div>}

          <div className="form-group">
            <label htmlFor="account-select">Demo account</label>
            <select
              id="account-select"
              value={selectedAccount}
              onChange={(event) => setSelectedAccount(event.target.value)}
              data-testid="account-select"
            >
              {DEMO_ACCOUNTS.map((option) => (
                <option key={option.username} value={option.username}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>

          <div className="account-summary" data-testid="account-summary">
            <strong>Username:</strong> {account.username}
            <br />
            <strong>Password:</strong> {account.password}
          </div>

          <button
            className="btn btn-primary"
            onClick={handleSignIn}
            disabled={loading}
            data-testid="sign-in-btn"
          >
            {loading ? 'Signing in…' : 'Sign in'}
          </button>
        </div>
      </div>
    );
  }

  const authConfig = buildRequestConfig(auth);

  if (loading) {
    return (
      <div className="app">
        <Navigation
          currentPage={currentPage}
          setCurrentPage={setCurrentPage}
          currentUser={currentUser}
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
          currentUser={currentUser}
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
        currentUser={currentUser}
        onLogout={handleLogout}
      />
      <main className="main-content">
        {currentPage === 'dashboard' && (
          <Dashboard
            policies={policies}
            claims={claims}
            onRefresh={refreshData}
            apiBase={API_BASE_URL}
            authConfig={authConfig}
            currentUser={currentUser}
          />
        )}
        {currentPage === 'claims' && (
          <Claims
            policies={policies}
            claims={claims}
            onRefresh={refreshData}
            apiBase={API_BASE_URL}
            authConfig={authConfig}
            currentUser={currentUser}
          />
        )}
      </main>
    </div>
  );
}

export default App;
