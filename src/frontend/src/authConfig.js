import { LogLevel, PublicClientApplication } from '@azure/msal-browser';

const tenantId = process.env.REACT_APP_AZURE_TENANT_ID || '';
const clientId = process.env.REACT_APP_AZURE_CLIENT_ID || '';
const instance = process.env.REACT_APP_AZURE_CLOUD_INSTANCE || 'https://login.microsoftonline.com';
const apiScope = process.env.REACT_APP_AZURE_API_SCOPE || (clientId ? `api://${clientId}/access_as_user` : '');

/**
 * Authentication is only turned on when the Microsoft Entra ID app registration is configured,
 * which keeps local development against the seeded backend working out of the box.
 */
export const isAuthEnabled = Boolean(tenantId && clientId);

export const msalConfig = {
  auth: {
    clientId,
    authority: `${instance.replace(/\/$/, '')}/${tenantId}`,
    redirectUri: process.env.REACT_APP_AZURE_REDIRECT_URI || window.location.origin,
    postLogoutRedirectUri: process.env.REACT_APP_AZURE_REDIRECT_URI || window.location.origin,
    navigateToLoginRequestUrl: false,
  },
  cache: {
    // Keep tokens out of localStorage; sessionStorage limits the exposure window.
    cacheLocation: 'sessionStorage',
    storeAuthStateInCookie: false,
  },
  system: {
    loggerOptions: {
      logLevel: LogLevel.Warning,
      piiLoggingEnabled: false,
      loggerCallback: (level, message, containsPii) => {
        if (!containsPii && level <= LogLevel.Warning) {
          console.warn(message);
        }
      },
    },
  },
};

/** Scopes requested during sign-in. */
export const loginRequest = {
  scopes: ['openid', 'profile', 'User.Read'],
};

/** Scopes required to call the InsureWell API. */
export const apiRequest = {
  scopes: apiScope ? [apiScope] : [],
};

export const msalInstance = isAuthEnabled ? new PublicClientApplication(msalConfig) : null;
