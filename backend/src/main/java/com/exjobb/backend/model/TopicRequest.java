package com.exjobb.backend.model;



public class TopicRequest {
    private String topic;
    private String instruction;

    public TopicRequest(String topic, String instruction) {
        this.topic = topic;
        this.instruction = instruction;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }
}