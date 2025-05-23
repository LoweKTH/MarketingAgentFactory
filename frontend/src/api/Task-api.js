// src/services/api.js

const API_BASE_URL = 'http://localhost:8080/api'; // *** IMPORTANT: Replace with your actual backend API URL ***

/**
 * Fetches tasks for the current user from the backend API.
 * @returns {Promise<Array>} A promise that resolves to an array of task objects.
 * @throws {Error} If the network request fails or the server returns an error.
 */
export const fetchTasks = async () => {
    try {
        const response = await fetch(`${API_BASE_URL}/tasks/getTasks`); // Using API_BASE_URL
        if (!response.ok) {
            let errorMessage = `HTTP error! Status: ${response.status}`;
            try {
                const errorData = await response.json();
                errorMessage = errorData.message || errorMessage;
            } catch (jsonError) {
                console.error("Failed to parse error response as JSON:", jsonError);
            }
            throw new Error(errorMessage);
        }
        const data = await response.json();
        return data;
    } catch (error) {
        console.error("Error fetching tasks:", error);
        throw error;
    }
};
