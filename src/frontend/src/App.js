import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import './App.css';
import Navigation from './components/Navigation';
import Dashboard from './components/Dashboard';
import Claims from './components/Claims';
import Login from './components/Login';
import { getUser, getAuthHeader, clearAuth } from './services/authService';

const API_BASE_URL = 'http://localhost:8080/api';

function App() {
  const [currentPage, setCurrentPage] = useState('dashboard');
  const [policies, setPolicies] = useState([]);
  const [claims, setClaims] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [user, setUser] = useState(() => getUser());

  const handleLogout = useCallback(() => {
    clearAuth();
    setUser(null);
    setPolicies([]);
    setClaims([]);
  }, []);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const headers = getAuthHeader();
      const [policiesRes, claimsRes] = await Promise.all([
        axios.get(`${API_BASE_URL}/policies`, { headers }),
        axios.get(`${API_BASE_URL}/claims`, { headers }),
      ]);
      setPolicies(policiesRes.data);
      setClaims(claimsRes.data);
      setError(null);
    } catch (err) {
      if (err.response?.status === 401 || err.response?.status === 403) {
        handleLogout();
      } else {
        setError('Failed to load data from backend. Ensure Spring Boot server is running on port 8080.');
      }
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, [handleLogout]);

  useEffect(() => {
    if (user) {
      fetchData();
    } else {
      setLoading(false);
    }
  }, [user, fetchData]);

  const handleLogin = (loggedInUser) => {
    setUser(loggedInUser);
  };

  if (!user) {
    return <Login apiBase={API_BASE_URL} onLogin={handleLogin} />;
  }

  if (loading) {
    return (
      <div className="app">
        <Navigation currentPage={currentPage} setCurrentPage={setCurrentPage} user={user} onLogout={handleLogout} />
        <main className="main-content">
          <div className="loader">Loading...</div>
        </main>
      </div>
    );
  }

  if (error) {
    return (
      <div className="app">
        <Navigation currentPage={currentPage} setCurrentPage={setCurrentPage} user={user} onLogout={handleLogout} />
        <main className="main-content">
          <div className="error-banner">{error}</div>
        </main>
      </div>
    );
  }

  return (
    <div className="app">
      <Navigation currentPage={currentPage} setCurrentPage={setCurrentPage} user={user} onLogout={handleLogout} />
      <main className="main-content">
        {currentPage === 'dashboard' && (
          <Dashboard
            policies={policies}
            claims={claims}
            onRefresh={fetchData}
            apiBase={API_BASE_URL}
            user={user}
          />
        )}
        {currentPage === 'claims' && (
          <Claims
            policies={policies}
            claims={claims}
            onRefresh={fetchData}
            apiBase={API_BASE_URL}
            user={user}
          />
        )}
      </main>
    </div>
  );
}

export default App;
