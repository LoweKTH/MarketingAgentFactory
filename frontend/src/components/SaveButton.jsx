// NEW: SaveButton Component (create this as a separate file: components/SaveButton.js)
import React from 'react';

const SaveButton = ({ onClick, isLoading, isSaved, taskId }) => {
    const getButtonContent = () => {
        if (isLoading) {
            return (
                <>
                    <i className="fas fa-spinner fa-spin"></i>
                    <span style={{ marginLeft: '8px' }}>Saving...</span>
                </>
            );
        }
        
        if (isSaved) {
            return (
                <>
                    <i className="fas fa-check"></i>
                    <span style={{ marginLeft: '8px' }}>Saved</span>
                </>
            );
        }
        
        return (
            <>
                <i className="fas fa-save"></i>
                <span style={{ marginLeft: '8px' }}>Save Content</span>
            </>
        );
    };

    return (
        <button
            onClick={onClick}
            disabled={isLoading || isSaved}
            style={{
                backgroundColor: isSaved ? '#4caf50' : '#2196f3',
                color: 'white',
                border: 'none',
                padding: '12px 24px',
                borderRadius: '6px',
                cursor: isLoading || isSaved ? 'not-allowed' : 'pointer',
                fontSize: '1rem',
                fontWeight: '500',
                display: 'flex',
                alignItems: 'center',
                transition: 'all 0.3s ease',
                opacity: isLoading || isSaved ? 0.7 : 1
            }}
            title={isSaved ? `Saved with task ID: ${taskId}` : 'Save this content to database'}
        >
            {getButtonContent()}
        </button>
    );
};

export default SaveButton;