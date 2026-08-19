import React from 'react';
import ReactDOM from 'react-dom/client';
import { EventType } from '@azure/msal-browser';
import { MsalProvider } from '@azure/msal-react';
import './App.css';
import App from './App';
import { isAuthEnabled, msalInstance } from './authConfig';

const root = ReactDOM.createRoot(document.getElementById('root'));

function render(children) {
  root.render(<React.StrictMode>{children}</React.StrictMode>);
}

if (isAuthEnabled && msalInstance) {
  msalInstance
    .initialize()
    .then(() => msalInstance.handleRedirectPromise())
    .then((response) => {
      if (response && response.account) {
        msalInstance.setActiveAccount(response.account);
      } else if (!msalInstance.getActiveAccount() && msalInstance.getAllAccounts().length > 0) {
        msalInstance.setActiveAccount(msalInstance.getAllAccounts()[0]);
      }

      msalInstance.addEventCallback((event) => {
        if (event.eventType === EventType.LOGIN_SUCCESS && event.payload && event.payload.account) {
          msalInstance.setActiveAccount(event.payload.account);
        }
      });

      render(
        <MsalProvider instance={msalInstance}>
          <App />
        </MsalProvider>
      );
    })
    .catch((error) => {
      console.error('Microsoft Entra ID initialisation failed', error);
    });
} else {
  render(<App />);
}
