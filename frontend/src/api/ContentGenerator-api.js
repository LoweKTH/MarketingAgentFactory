// Updated ContentGenerator-api.js
const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Generate social media content (no saving to database)
 */
export async function generateSocialPost(data) {
    try {
        const response = await fetch(`${API_BASE_URL}/content/generate`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data),
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || `API request failed with status: ${response.status}`);
        }

        const result = await response.json();
        return result;
    } catch (error) {
        console.error('Error generating social post:', error);
        throw error;
    }
}

/**
 * NEW: Save generated content to database
 */
export async function saveGeneratedContent(data) {
    try {
        const response = await fetch(`${API_BASE_URL}/content/save`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data),
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || `Save request failed with status: ${response.status}`);
        }

        const result = await response.json();
        return result;
    } catch (error) {
        console.error('Error saving content:', error);
        throw error;
    }
}