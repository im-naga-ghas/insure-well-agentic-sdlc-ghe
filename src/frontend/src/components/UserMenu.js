import React from 'react';
import { useMsal } from '@azure/msal-react';
import { isAuthEnabled } from '../authConfig';
import '../styles/UserMenu.css';

function UserMenu() {
  const { instance } = useMsal();

  if (!isAuthEnabled || !instance) {
    return null;
  }

  const account = instance.getActiveAccount() || instance.getAllAccounts()[0];
  if (!account) {
    return null;
  }

  const handleSignOut = () => {
    instance.logoutRedirect({ account }).catch((error) => console.error(error));
  };

  return (
    <div className="user-menu" data-testid="user-menu">
      <span className="user-name" data-testid="user-name">
        {account.name || account.username}
      </span>
      <button className="signout-button" onClick={handleSignOut} data-testid="signout-button">
        Sign out
      </button>
    </div>
  );
}

export default UserMenu;
