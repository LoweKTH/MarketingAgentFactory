package com.exjobb.backend.service;

import com.exjobb.backend.model.GeneratedResponse;
import com.exjobb.backend.model.TopicRequest;
import reactor.core.publisher.Mono;

public interface AgentService {

    public Mono<GeneratedResponse> getGeneratedText(TopicRequest topicRequest);
    public Mono<GeneratedResponse> getImprovedText(TopicRequest topicRequest);

}
