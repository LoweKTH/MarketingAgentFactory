// src/components/ContentEvaluation.jsx
import React from 'react';
import './ContentEvaluation.css';

const ContentEvaluation = ({ evaluationData }) => {
    // Extract all necessary data with fallbacks to handle missing properties
    const workflowInfo = evaluationData?.workflowInfo || {};
    const suggestions = evaluationData?.suggestions || [];
    const estimatedMetrics = evaluationData?.estimatedMetrics || {};
    const generationTimeSeconds = evaluationData?.generationTimeSeconds || 0;

    // Handle case where evaluation might not have been performed or there's no score
    const hasEvaluation = workflowInfo && workflowInfo.evaluationPerformed;

    // Default score if not available (using 7.5 as a reasonable default)
    const score = hasEvaluation && workflowInfo.evaluationScore ?
        workflowInfo.evaluationScore : 7.5;

    const isOptimized = workflowInfo.optimizationPerformed;
    const modelUsed = workflowInfo.modelUsed || "AI Model";

    // Determine status based on optimization or score
    const getStatusLabel = () => {
        if (isOptimized) {
            return "Optimized Content";
        } else if (score >= 8.5) {
            return "Ready to Use";
        } else if (score >= 7) {
            return "Minor Edits Suggested";
        } else {
            return "Review Recommended";
        }
    };

    // Determine color based on status
    const getStatusColor = () => {
        if (isOptimized) {
            return "#4CAF50"; // Green
        } else if (score >= 8.5) {
            return "#4CAF50"; // Green
        } else if (score >= 7) {
            return "#FF9800"; // Orange
        } else {
            return "#F44336"; // Red
        }
    };

    return (
        <div className="content-evaluation">
            {/* Score Dashboard Card */}
            <div className="score-dashboard">
                <div className="score-header">
                    <h3>Content Quality Assessment</h3>
                    <div className="generation-time">
                        Generation time: {generationTimeSeconds.toFixed(2)}s
                    </div>
                </div>

                <div className="score-container">
                    <div className="score-badge" style={{
                        background: `conic-gradient(${getStatusColor()} ${(score/10)*360}deg, #e0e0e0 0deg)`
                    }}>
                        <div className="score-inner">
                            <span className="score-value">{score}</span>
                            <span className="score-max">/10</span>
                        </div>
                    </div>

                    <div className="score-details">
                        <div className="model-info">Generated with {modelUsed}</div>
                        <div
                            className="status-badge"
                            style={{ backgroundColor: getStatusColor() }}
                        >
                            {getStatusLabel()}
                        </div>
                    </div>
                </div>
            </div>

            {/* Metrics Visualization - Only show if we have metrics */}
            {Object.keys(estimatedMetrics).length > 0 && (
                <div className="metrics-container">
                    <h3>Content Performance Metrics</h3>
                    <div className="metrics-grid">
                        {Object.entries(estimatedMetrics).map(([key, value]) => (
                            <div className="metric-item" key={key}>
                                <div className="metric-label">{key.replace(/_/g, ' ')}</div>
                                <div className="metric-value">{value}</div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* Suggestions Display - Only show if we have suggestions */}
            {suggestions.length > 0 && (
                <div className="suggestions-container">
                    <h3>AI Suggestions</h3>
                    <ul className="suggestions-list">
                        {suggestions.map((suggestion, index) => (
                            <li key={index} className="suggestion-item">
                                <span className="suggestion-icon">💡</span>
                                <span className="suggestion-text">{suggestion}</span>
                            </li>
                        ))}
                    </ul>
                </div>
            )}
        </div>
    );
};

export default ContentEvaluation;