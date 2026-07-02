import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './App.css';
import Navigation from './components/Navigation';
import Dashboard from './components/Dashboard';
import Claims from './components/Claims';
import Renewals from './components/Renewals';

const API_BASE_URL = 'http://localhost:8080/api';

function App() {
  const [currentPage, setCurrentPage] = useState('dashboard');
  const [policies, setPolicies] = useState([]);
  const [claims, setClaims] = useState([]);
  const [renewalCount, setRenewalCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchData();
  }, []);

  // Listen for navigate events dispatched by the renewal banner
  useEffect(() => {
    const handler = (e) => setCurrentPage(e.detail);
    window.addEventListener('navigate', handler);
    return () => window.removeEventListener('navigate', handler);
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [policiesRes, claimsRes, renewalsRes] = await Promise.all([
        axios.get(`${API_BASE_URL}/policies`),
        axios.get(`${API_BASE_URL}/claims`),
        axios.get(`${API_BASE_URL}/renewals/upcoming`).catch(() => ({ data: [] })),
      ]);
      setPolicies(policiesRes.data);
      setClaims(claimsRes.data);
      setRenewalCount(renewalsRes.data.length);
      setError(null);
    } catch (err) {
      setError('Failed to load data from backend. Ensure Spring Boot server is running on port 8080.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const refreshData = () => {
    fetchData();
  };

  if (loading) {
    return (
      <div className="app">
        <Navigation currentPage={currentPage} setCurrentPage={setCurrentPage} renewalCount={renewalCount} />
        <main className="main-content">
          <div className="loader">Loading...</div>
        </main>
      </div>
    );
  }

  if (error) {
    return (
      <div className="app">
        <Navigation currentPage={currentPage} setCurrentPage={setCurrentPage} renewalCount={renewalCount} />
        <main className="main-content">
          <div className="error-banner">{error}</div>
        </main>
      </div>
    );
  }

  return (
    <div className="app">
      <Navigation currentPage={currentPage} setCurrentPage={setCurrentPage} renewalCount={renewalCount} />
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
        {currentPage === 'renewals' && (
          <Renewals apiBase={API_BASE_URL} />
        )}
      </main>
    </div>
  );
}

export default App;
