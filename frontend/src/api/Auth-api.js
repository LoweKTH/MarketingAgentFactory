// src/api/Auth-api.js

const BACKEND_URL = 'http://localhost:8080'; // Your Spring Boot backend URL

/**
 * Initiates the OAuth flow for a given social media platform by
 * calling the backend and redirecting the user to the platform's
 * authorization URL.
 *
 * @param {string} platform - The name of the social media platform (e.g., 'facebook', 'twitter').
 * @returns {Promise<void>} - A promise that resolves when the redirection happens or rejects on error.
 */
export const initiateOAuth = async (platform) => {
    try {
        // Dynamically construct the endpoint based on the platform
        const response = await fetch(`${BACKEND_URL}/api/auth/${platform}/initiate`, {
            method: 'GET',
            credentials: 'include', // This is the key fix - includes session cookies
            headers: {
                'Content-Type': 'application/json',
            }
        });
        
        if (!response.ok) {
            // Attempt to parse JSON error response from backend
            let errorMessage = `Failed to initiate ${platform} OAuth on backend.`;
            try {
                const errorData = await response.json();
                errorMessage = errorData.message || errorMessage;
            } catch (jsonError) {
                // If response is not JSON, use the status text
                errorMessage = `Backend error: ${response.status} ${response.statusText}`;
            }
            throw new Error(errorMessage);
        }

        const data = await response.json();

        if (data.redirectUrl) {
            // Redirect the user's browser to the social media platform's authorization URL
            window.location.href = data.redirectUrl;
        } else {
            throw new Error("Backend did not provide a redirect URL.");
        }
    } catch (err) {
        console.error(`Error initiating ${platform} OAuth:`, err);
        // Re-throw the error so the calling component can handle state updates (e.g., setConnectionError)
        throw err;
    }
};