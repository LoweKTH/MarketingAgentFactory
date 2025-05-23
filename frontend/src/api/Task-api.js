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

export const deleteTask = async (taskId) => {
    try {
        const response = await fetch(`${API_BASE_URL}/tasks/${taskId}`, { // Assuming DELETE /api/tasks/{taskId}
            method: 'DELETE',
            headers: {
                // Add any necessary headers, e.g., authorization token
                // 'Authorization': `Bearer ${yourAuthToken}`
            },
        });

        if (!response.ok) {
            let errorMessage = `HTTP error! Status: ${response.status}`;
            try {
                // Attempt to parse a more specific error message from the backend if available
                const errorData = await response.json();
                errorMessage = errorData.message || errorMessage;
            } catch (jsonError) {
                console.error("Failed to parse error response as JSON during delete:", jsonError);
            }
            throw new Error(errorMessage);
        }

        // If the response is 204 No Content, response.json() would fail.
        // Check if there's content before trying to parse JSON.
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            // If the backend sends a JSON response on success (e.g., confirmation message)
            const result = await response.json();
            return result;
        } else {
            // No content expected or successful deletion without a body
            return;
        }

    } catch (error) {
        console.error(`Error deleting task with ID ${taskId}:`, error);
        throw error; // Re-throw the error for the calling component to handle
    }
};
