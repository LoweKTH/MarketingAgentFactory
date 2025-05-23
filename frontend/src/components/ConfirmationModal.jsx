// src/components/ConfirmationModal.jsx
import React from 'react';
import './ConfirmationModal.css'; // Create this CSS file

const ConfirmationModal = ({ message, onConfirm, onCancel }) => {
    return (
        <div className="modal-overlay">
            <div className="modal-content">
                <p>{message}</p>
                <div className="modal-actions">
                    <button onClick={onConfirm} className="modal-confirm-btn">Confirm</button>
                    <button onClick={onCancel} className="modal-cancel-btn">Cancel</button>
                </div>
            </div>
        </div>
    );
};

export default ConfirmationModal;
