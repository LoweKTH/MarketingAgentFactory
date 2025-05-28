// src/api/Connection-api.js

const API_BASE_URL = 'http://localhost:8080/api'; // Or your actual backend URL

export const fetchConnectedPlatforms = async () => {
    try {
        const response = await fetch(`${API_BASE_URL}/auth/connections`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                // Add authorization headers here if your backend requires it
                // e.g., 'Authorization': `Bearer ${localStorage.getItem('jwt_token')}`
            },
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || 'Failed to fetch connected platforms');
        }

        const data = await response.json();
        return data; // This will be a map like {twitter: true, linkedin: false, ...}
    } catch (error) {
        console.error("Error fetching connected platforms:", error);
        throw error;
    }
};