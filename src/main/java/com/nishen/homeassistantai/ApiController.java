package com.nishen.homeassistantai;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final HomeAssistantService service;

    public ApiController(HomeAssistantService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return service.health();
    }

    @GetMapping("/proposals")
    public Collection<AutomationProposal> proposals() {
        return service.proposals();
    }

    @GetMapping("/proposals/{proposalId}")
    public AutomationProposal proposal(@PathVariable("proposalId") String proposalId) {
        return service.proposal(proposalId);
    }

    @GetMapping(value = "/proposals/{proposalId}/yaml", produces = "text/plain")
    public String proposalYaml(@PathVariable("proposalId") String proposalId) {
        return service.proposalYaml(proposalId);
    }

    @DeleteMapping("/proposals/{proposalId}")
    public Map<String, Object> deleteProposal(@PathVariable("proposalId") String proposalId) {
        return service.deleteProposal(proposalId);
    }

    @GetMapping("/states")
    public Mono<List<Map<String, Object>>> states() {
        return service.states();
    }

    @GetMapping("/areas")
    public Mono<List<Map<String, Object>>> areas() {
        return service.areas();
    }

    @GetMapping("/devices")
    public Mono<List<Map<String, Object>>> devices() {
        return service.devices();
    }

    @GetMapping("/state/{entityId}")
    public Mono<Map<String, Object>> state(@PathVariable("entityId") String entityId) {
        return service.state(entityId);
    }

    @GetMapping("/entities")
    public Mono<List<Map<String, Object>>> entities() {
        return service.entities();
    }

    @GetMapping("/helpers")
    public Mono<List<Map<String, Object>>> helpers() {
        return service.helpers();
    }

    @GetMapping("/automations")
    public Mono<List<Map<String, Object>>> automations() {
        return service.automations();
    }

    @GetMapping("/automation/{automationEntityId}")
    public Mono<Map<String, Object>> automation(@PathVariable("automationEntityId") String automationEntityId) {
        return service.automation(automationEntityId);
    }

    @GetMapping("/entity/{entityId}/automations")
    public Mono<Map<String, Object>> automationsUsingEntity(@PathVariable("entityId") String entityId) {
        return service.automationsUsingEntity(entityId);
    }

    @GetMapping("/entity/{entityId}/relationships")
    public Mono<Map<String, Object>> relationships(@PathVariable("entityId") String entityId) {
        return service.relationships(entityId);
    }

    @PostMapping("/analysis/recommendations")
    public Mono<Map<String, Object>> recommendations() {
        return service.recommendations();
    }

    @PostMapping("/analysis/unused-entities")
    public Map<String, Object> unusedEntities() {
        return service.unusedEntities();
    }

    @PostMapping("/analysis/duplicate-logic")
    public Map<String, Object> duplicateLogic() {
        return service.duplicateLogic();
    }

    @PostMapping("/automation/propose")
    public AutomationProposal propose(@RequestBody AutomationProposalRequest request) {
        return service.propose(request.title(), request.request());
    }

    @PostMapping("/automation/apply")
    public Map<String, Object> apply(@RequestBody ApplyRequest request) {
        return service.apply(request.proposalId(), request.approvalToken());
    }

    public record AutomationProposalRequest(String title, String request) {}
    public record ApplyRequest(String proposalId, String approvalToken) {}
}
