// src/api/ContentGenerator-api.js

// Import the custom axios instance
import api from '../api/api';

// const API_BASE_URL = 'http://localhost:8080/api'; // No longer needed

/**
 * Generate social media content (no saving to database)
 */
export async function generateSocialPost(data) {
    try {
        // Use api.post() for POST requests
        const response = await api.post('/content/generate', data); // Axios automatically strings JSON body

        return response.data;
    } catch (error) {
        console.error('Error generating social post:', error.response?.data || error.message);
        throw error;
    }
}

/**
 * NEW: Save generated content to database
 */
export async function saveGeneratedContent(data) {
    try {
        // Use api.post() for POST requests
        const response = await api.post('/content/save', data);

        return response.data;
    } catch (error) {
        console.error('Error saving content:', error.response?.data || error.message);
        throw error;
    }
}