// src/api/Auth-api.js

// Import the custom axios instance and axios for direct login calls
import api from '../api/api';
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api'; // Your backend API base URL

/**
 * Logs in a user with username and password
 * @param {string} username - User's username
 * @param {string} password - User's password
 * @returns {Promise<Object>} - Promise that resolves to login response with token and user data
 */
export const loginUser = async (username, password) => {
    try {
        // Use axios directly for login (before interceptors are set up)
        const response = await axios.post(`${API_BASE_URL}/auth/login`, {
            username,
            password
        });

        // The response should contain token and user information
        const data = response.data;
        
        if (!data.token) {
            throw new Error("No authentication token received from server");
        }

        return {
            token: data.token,
            user: data.user || null,
            ...data // Include any other data from the response
        };
    } catch (err) {
        console.error('Login error:', err.response?.data || err.message);
        
        // Provide a more user-friendly error message
        if (err.response?.status === 401) {
            throw new Error("Invalid username or password");
        } else if (err.response?.status === 404) {
            throw new Error("User not found");
        } else if (err.response?.data?.message) {
            throw new Error(err.response.data.message);
        } else {
            throw new Error("Login failed. Please try again.");
        }
    }
};

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