// src/services/api.js

// Import the custom axios instance
import api from '../api/api';

// const API_BASE_URL = 'http://localhost:8080/api'; // No longer needed

/**
 * Fetches tasks for the current user from the backend API.
 * @returns {Promise<Array>} A promise that resolves to an array of task objects.
 * @throws {Error} If the network request fails or the server returns an error.
 */
export const fetchTasks = async () => {
    try {
        // Use api.get()
        const response = await api.get('/tasks/getTasks');

        // Axios handles response.ok and JSON parsing automatically
        return response.data;
    } catch (error) {
        console.error("Error fetching tasks:", error.response?.data || error.message);
        throw error;
    }
};

export const deleteTask = async (taskId) => {
    try {
        // Use api.delete()
        const response = await api.delete(`/tasks/${taskId}`);

        // Axios handles successful responses, even 204 No Content.
        // If your backend returns data on delete (uncommon but possible), it would be in response.data
        return response.data; // Will be undefined for 204 No Content
    } catch (error) {
        console.error(`Error deleting task with ID ${taskId}:`, error.response?.data || error.message);
        throw error; // Re-throw the error for the calling component to handle
    }
};