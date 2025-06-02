// src/api/api.js
import axios from 'axios';

// Create a custom axios instance
const api = axios.create({
    baseURL: 'http://localhost:8080/api', // Your backend API base URL
     withCredentials: true,
});

// Function to set up or clear the request interceptor
export const setupInterceptors = (token) => {
    // Clear any existing interceptors to prevent duplicates
    api.interceptors.request.clear();

    if (token) {
        api.interceptors.request.use(
            (config) => {
                config.headers.Authorization = `Bearer ${token}`;
                return config;
            },
            (error) => {
                return Promise.reject(error);
            }
        );
    }
    // You can also add response interceptors here for error handling, e.g., 401 Unauthorized
    // api.interceptors.response.use(
    //     (response) => response,
    //     (error) => {
    //         if (error.response && error.response.status === 401) {
    //             // Handle unauthorized, e.g., redirect to login
    //             console.log("Unauthorized, redirecting to login...");
    //             // You would typically call a logout function from AuthContext here
    //             // import { useAuth } from '../context/AuthContext'; (if outside this file)
    //             // const { logout } = useAuth(); logout();
    //             // Or use window.location = '/login';
    //         }
    //         return Promise.reject(error);
    //     }
    // );
};

export default api;