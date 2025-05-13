package com.exjobb.backend.service.impl;

import com.exjobb.backend.model.GeneratedResponse;
import com.exjobb.backend.model.TopicRequest;
import com.exjobb.backend.service.AgentService;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class AgentServiceImpl implements AgentService {

    private final WebClient webClient;

    public AgentServiceImpl(WebClient.Builder builder){
        this.webClient = builder.baseUrl("http://localhost:8000").build();
    }


    /**
     * Anropar AI-agentens /generate-endpoint med ett topic och returnerar resultatet
     * Kom ihåg att eftersom metoden returnerar en Mono så betyder det att även controllerns metod måste
     * vara reaktiv och returnera Mono
     * @param request topicRequest med ämnet att generera text om
     * @return Mono<GeneratedResponse> - reaktiv wrapper kring svaret
     */
    @Override
    public Mono<GeneratedResponse> getGeneratedText(TopicRequest request) {
        return webClient.post()
                .uri("/generate") // endpoint hos python-agenten
                .bodyValue(request) // skickar topicrequest som json-body
                .retrieve()         // initierar request och förbereder att läsa svaret
                .bodyToMono(GeneratedResponse.class); // tolkar json-svar som java-objekt
    }

    @Override
    public Mono<GeneratedResponse> getImprovedText(TopicRequest request) {
        String fullPrompt = request.getInstruction() + "\n\n" + request.getTopic();

        TopicRequest constructedRequest = new TopicRequest(fullPrompt, null); // bara topic-fält används av Python-agenten

        return webClient.post()
                .uri("/generate/improve")
                .bodyValue(constructedRequest)
                .retrieve()
                .bodyToMono(GeneratedResponse.class);
    }

}
