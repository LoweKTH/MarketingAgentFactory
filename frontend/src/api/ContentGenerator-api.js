

const API_BASE_URL = 'http://localhost:8080/api'; // *** IMPORTANT: Replace with your actual backend API URL ***

/**
 * Calls the API to generate social media content based on provided parameters.
 *
 * @param {object} data - The payload for the API request.
 * @param {string} data.contentType - The type of content to generate (e.g., "social_post").
 * @param {string} data.brandVoice - The desired brand voice (e.g., "professional").
 * @param {string} data.topic - The main topic of the content.
 * @param {string} data.platform - The social media platform (e.g., "linkedin").
 * @param {string} data.targetAudience - The intended audience.
 * @param {string[]} data.keyMessages - An array of key messages to include.
 * @returns {Promise<object>} - A promise that resolves with the generated content or rejects with an error.
 */
export async function generateSocialPost(data) {
    try {
        const response = await fetch(`${API_BASE_URL}/content/generate`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                // Add any other headers like authorization tokens if needed
                // 'Authorization': `Bearer ${yourAuthToken}`
            },
            body: JSON.stringify(data),
        });

        // Check if the response was successful (status code 2xx)
        if (!response.ok) {
            const errorData = await response.json(); // Attempt to parse error message from backend
            throw new Error(errorData.message || `API request failed with status: ${response.status}`);
        }

        const result = await response.json();
        return result; // This will be the generated social media post
    } catch (error) {
        console.error('Error generating social post:', error);
        throw error; // Re-throw the error for the calling component to handle
    }
}

// You can add more API functions here if needed, for different endpoints or types of requests.
// export async function anotherApiCall(params) { ... }