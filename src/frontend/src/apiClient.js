import axios from 'axios';
import { InteractionRequiredAuthError } from '@azure/msal-browser';
import { apiRequest, isAuthEnabled, msalInstance } from './authConfig';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api';

const apiClient = axios.create({ baseURL: API_BASE_URL });

/**
 * Acquires an access token for the InsureWell API, silently when possible and via a redirect when
 * the user has to interact (consent, MFA, expired session).
 */
async function acquireAccessToken() {
  const account = msalInstance.getActiveAccount() || msalInstance.getAllAccounts()[0];
  if (!account) {
    return null;
  }
  try {
    const result = await msalInstance.acquireTokenSilent({ ...apiRequest, account });
    return result.accessToken;
  } catch (error) {
    if (error instanceof InteractionRequiredAuthError) {
      await msalInstance.acquireTokenRedirect({ ...apiRequest, account });
    }
    throw error;
  }
}

apiClient.interceptors.request.use(async (config) => {
  if (isAuthEnabled && msalInstance) {
    const accessToken = await acquireAccessToken();
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
  }
  return config;
});

export { API_BASE_URL, apiClient };
