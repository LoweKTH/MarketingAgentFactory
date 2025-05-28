// src/api/Auth-api.js

// Import the custom axios instance
import api from '../api/api';

// const BACKEND_URL = 'http://localhost:8080'; // No longer strictly needed if base URL is in api.js

/**
 * Initiates the OAuth flow for a given social media platform by
 * calling the backend and redirecting the user to the platform's
 * authorization URL.
 *
 * @param {string} platform - The name of the social media platform (e.g., 'twitter').
 * @returns {Promise<void>} - A promise that resolves when the redirection happens or rejects on error.
 */
export const initiateOAuth = async (platform) => {
    try {
        // Use the 'api' (axios) instance for the GET request
        // The JWT will be automatically added by the interceptor.
        const response = await api.get(`/auth/${platform}/initiate`);

        // Axios automatically parses JSON and throws for non-2xx responses.
        // The backend should return a redirectUrl in its JSON response.
        const data = response.data;

        if (data.redirectUrl) {
            // Redirect the user's browser to the social media platform's authorization URL
            // This is a browser-level redirect, not an Axios redirect.
            window.location.href = data.redirectUrl;
        } else {
            throw new Error("Backend did not provide a redirect URL.");
        }
    } catch (err) {
        console.error(`Error initiating ${platform} OAuth:`, err.response?.data || err.message);
        // Re-throw the error so the calling component can handle state updates (e.g., setConnectionError)
        throw err;
    }
};