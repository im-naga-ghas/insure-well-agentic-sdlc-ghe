import React from 'react';
import { useMsal } from '@azure/msal-react';
import { loginRequest } from '../authConfig';
import '../styles/SignIn.css';

function SignIn() {
  const { instance, inProgress } = useMsal();

  const handleSignIn = () => {
    instance.loginRedirect(loginRequest).catch((error) => console.error(error));
  };

  return (
    <div className="signin" data-testid="signin">
      <div className="signin-card">
        <h1>🏥 InsureWell</h1>
        <p>Sign in with your organisation account to view your policies and claims.</p>
        <button
          className="signin-button"
          onClick={handleSignIn}
          disabled={inProgress !== 'none'}
          data-testid="signin-button"
        >
          Sign in with Microsoft
        </button>
        <span className="signin-note">Secured by Microsoft Entra ID</span>
      </div>
    </div>
  );
}

export default SignIn;
