// src/api/Connection-api.js

// Import the custom axios instance
import api from '../api/api';

// const API_BASE_URL = 'http://localhost:8080/api'; // No longer needed if base URL is set in api.js

export const fetchConnectedPlatforms = async () => {
    try {
        // Use the 'api' (axios) instance instead of fetch
        const response = await api.get('/auth/connections'); // Use api.get() for GET requests

        // Axios automatically handles response.ok and parses JSON,
        // so much of the boilerplate can be removed.
        return response.data; // Axios puts the response body directly in .data
    } catch (error) {
        console.error("Error fetching connected platforms:", error.response?.data || error.message);
        throw error; // Re-throw the error for the calling component to handle
    }
};