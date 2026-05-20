import React from 'react';
import '../styles/Navigation.css';

function Navigation({ currentPage, setCurrentPage, role, displayName, onLogout }) {
  return (
    <nav className="navbar" data-testid="navbar">
      <div className="navbar-brand">
        <h1>🏥 InsureWell</h1>
      </div>
      <div className="navbar-right">
        <ul className="navbar-menu">
          <li>
            <button
              className={`nav-link ${currentPage === 'dashboard' ? 'active' : ''}`}
              onClick={() => setCurrentPage('dashboard')}
              data-testid="nav-dashboard"
            >
              Dashboard
            </button>
          </li>
          <li>
            <button
              className={`nav-link ${currentPage === 'claims' ? 'active' : ''}`}
              onClick={() => setCurrentPage('claims')}
              data-testid="nav-claims"
            >
              Claims
            </button>
          </li>
        </ul>
        <div className="user-panel" data-testid="user-panel">
          <span className="user-role" data-testid="user-role">{role}</span>
          <span className="user-name" data-testid="user-name">{displayName}</span>
          <button className="nav-link logout-btn" onClick={onLogout} data-testid="logout-btn">
            Logout
          </button>
        </div>
      </div>
    </nav>
  );
}

export default Navigation;
