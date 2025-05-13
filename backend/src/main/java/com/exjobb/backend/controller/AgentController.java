package com.exjobb.backend.controller;


import com.exjobb.backend.model.GeneratedResponse;
import com.exjobb.backend.model.TopicRequest;
import com.exjobb.backend.service.AgentService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * Tar emot en topic-request från frontend och vidarebefordrar den till Python-agenten.
     * @param request TopicRequest med fältet "topic"
     * @return Mono<GeneratedResponse> med genererad text från AI-agenten
     */
    @PostMapping("/generate")
    public Mono<GeneratedResponse> generate(@RequestBody TopicRequest request) {
        return agentService.getGeneratedText(request);
    }


    /**
     * Tar emot ett förbättringsönskemål från frontend där användaren skickar in ett topic eller en text.
     * Denna text vidarebefordras till AI-agenten via REST-anrop till /generate/improve-endpointen,
     * som returnerar en förbättrad version baserat på modellen (t.ex. flan-t5).
     *
     * @param request TopicRequest med ett fält "topic" som innehåller den ursprungliga texten
     * @return Mono<GeneratedResponse> som innehåller den förbättrade texten genererad av AI-agenten
     */
    @PostMapping("/generate/improve")
    public Mono<GeneratedResponse> improve(@RequestBody TopicRequest request) {
        return agentService.getImprovedText(request);
    }



}
