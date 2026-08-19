import React, { useState, useEffect, useCallback } from 'react';
import { useIsAuthenticated } from '@azure/msal-react';
import './App.css';
import Navigation from './components/Navigation';
import Dashboard from './components/Dashboard';
import Claims from './components/Claims';
import SignIn from './components/SignIn';
import { API_BASE_URL, apiClient } from './apiClient';
import { isAuthEnabled } from './authConfig';

function App() {
  const isAuthenticated = useIsAuthenticated();
  const [currentPage, setCurrentPage] = useState('dashboard');
  const [policies, setPolicies] = useState([]);
  const [claims, setClaims] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const [policiesRes, claimsRes] = await Promise.all([
        apiClient.get('/policies'),
        apiClient.get('/claims'),
      ]);
      setPolicies(policiesRes.data);
      setClaims(claimsRes.data);
      setError(null);
    } catch (err) {
      if (err.response && (err.response.status === 401 || err.response.status === 403)) {
        setError('Your session is not authorised to access InsureWell. Sign in again with your organisation account.');
      } else {
        setError('Failed to load data from backend. Ensure Spring Boot server is running on port 8080.');
      }
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isAuthEnabled && !isAuthenticated) {
      setLoading(false);
      return;
    }
    fetchData();
  }, [fetchData, isAuthenticated]);

  const refreshData = () => {
    fetchData();
  };

  if (isAuthEnabled && !isAuthenticated) {
    return <SignIn />;
  }

  if (loading) {
    return (
      <div className="app">
        <Navigation currentPage={currentPage} setCurrentPage={setCurrentPage} />
        <main className="main-content">
          <div className="loader">Loading...</div>
        </main>
      </div>
    );
  }

  if (error) {
    return (
      <div className="app">
        <Navigation currentPage={currentPage} setCurrentPage={setCurrentPage} />
        <main className="main-content">
          <div className="error-banner">{error}</div>
        </main>
      </div>
    );
  }

  return (
    <div className="app">
      <Navigation currentPage={currentPage} setCurrentPage={setCurrentPage} />
      <main className="main-content">
        {currentPage === 'dashboard' && (
          <Dashboard
            policies={policies}
            claims={claims}
            onRefresh={refreshData}
            apiBase={API_BASE_URL}
          />
        )}
        {currentPage === 'claims' && (
          <Claims
            policies={policies}
            claims={claims}
            onRefresh={refreshData}
            apiBase={API_BASE_URL}
          />
        )}
      </main>
    </div>
  );
}

export default App;
