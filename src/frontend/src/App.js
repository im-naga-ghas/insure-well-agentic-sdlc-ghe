import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './App.css';
import Navigation from './components/Navigation';
import Dashboard from './components/Dashboard';
import Claims from './components/Claims';

const API_BASE_URL = 'http://localhost:8080/api';

function App() {
  const [currentPage, setCurrentPage] = useState('dashboard');
  const [policies, setPolicies] = useState([]);
  const [claims, setClaims] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [policiesRes, claimsRes] = await Promise.all([
        axios.get(`${API_BASE_URL}/policies`),
        axios.get(`${API_BASE_URL}/claims`),
      ]);
      setPolicies(policiesRes.data);
      setClaims(claimsRes.data);
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
          <button className="btn btn-primary" onClick={fetchData} data-testid="retry-load-btn">
            Retry
          </button>
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
