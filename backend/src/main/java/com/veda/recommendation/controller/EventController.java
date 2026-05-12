package com.veda.recommendation.controller;

import com.veda.recommendation.dto.InteractionEventRequest;
import com.veda.recommendation.dto.ReplaySimulationRequest;
import com.veda.recommendation.dto.ReplaySimulationResponse;
import com.veda.recommendation.entity.InteractionEvent;
import com.veda.recommendation.service.EventProducerService;
import com.veda.recommendation.service.ReplaySimulationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventProducerService eventProducerService;
    private final ReplaySimulationService replaySimulationService;

    public EventController(
            EventProducerService eventProducerService,
            ReplaySimulationService replaySimulationService
    ) {
        this.eventProducerService = eventProducerService;
        this.replaySimulationService = replaySimulationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InteractionEvent record(@Valid @RequestBody InteractionEventRequest request) {
        return eventProducerService.record(request);
    }

    @GetMapping("/user/{userId}")
    public List<InteractionEvent> eventsForUser(@PathVariable Long userId) {
        return eventProducerService.eventsForUser(userId);
    }

    /**
     * Mirror of {@code POST /api/platform/replay} for clients on older builds and simpler discovery in Swagger.
     */
    @PostMapping("/replay")
    public ReplaySimulationResponse replaySimulation(@RequestBody ReplaySimulationRequest request) {
        return replaySimulationService.replay(request);
    }
}
