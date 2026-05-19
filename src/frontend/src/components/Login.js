import React, { useState } from 'react';
import axios from 'axios';
import { saveAuth } from '../services/authService';
import '../styles/Login.css';

function Login({ apiBase, onLogin }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!username.trim() || !password.trim()) {
      setError('Username and password are required');
      return;
    }

    try {
      setLoading(true);
      const res = await axios.post(`${apiBase}/auth/login`, { username, password });
      const { token, username: uname, role } = res.data;
      saveAuth(token, uname, role);
      onLogin({ username: uname, role });
    } catch (err) {
      setError(err.response?.data?.error || 'Login failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-header">
          <h1>🏥 InsureWell</h1>
          <p>Sign in to access your account</p>
        </div>
        {error && <div className="alert alert-error" data-testid="login-error">{error}</div>}
        <form className="login-form" onSubmit={handleSubmit} data-testid="login-form">
          <div className="form-group">
            <label htmlFor="username">Username</label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Enter your username"
              autoComplete="username"
              data-testid="login-username"
            />
          </div>
          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your password"
              autoComplete="current-password"
              data-testid="login-password"
            />
          </div>
          <button
            type="submit"
            className="login-btn"
            disabled={loading}
            data-testid="login-submit"
          >
            {loading ? 'Signing in…' : 'Sign In'}
          </button>
        </form>
        <div className="login-hint">
          <p>Demo credentials:</p>
          <ul>
            <li><strong>admin</strong> / admin123 — full access (Claims Adjuster/Admin)</li>
            <li><strong>policyholder</strong> / holder123 — view and submit claims</li>
          </ul>
        </div>
      </div>
    </div>
  );
}

export default Login;
