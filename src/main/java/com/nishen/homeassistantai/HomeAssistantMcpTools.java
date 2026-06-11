package com.nishen.homeassistantai;

import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class HomeAssistantMcpTools {
    private final HomeAssistantService service;

    public HomeAssistantMcpTools(HomeAssistantService service) {
        this.service = service;
    }

    @McpTool(name = "health", description = "Get the API health status.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public Mono<Map<String, Object>> health() {
        return Mono.just(service.health());
    }

    @McpTool(name = "list_states", description = "List all Home Assistant entity states.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Mono<List<Map<String, Object>>> listStates() {
        return service.states();
    }

    @McpTool(name = "list_areas", description = "List Home Assistant areas with related entities and devices.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Mono<List<Map<String, Object>>> listAreas() {
        return service.areas();
    }

    @McpTool(name = "list_devices", description = "List Home Assistant devices with area and entity associations.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Mono<List<Map<String, Object>>> listDevices() {
        return service.devices();
    }

    @McpTool(name = "get_state", description = "Get the current state for a specific Home Assistant entity.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Mono<Map<String, Object>> getState(
            @McpArg(name = "entityId", description = "The full Home Assistant entity ID, for example light.kitchen.", required = true)
            String entityId) {
        return service.state(entityId);
    }

    @McpTool(name = "list_entities", description = "List entity IDs with state and attributes.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Mono<List<Map<String, Object>>> listEntities() {
        return service.entities();
    }

    @McpTool(name = "list_helpers", description = "List Home Assistant helper entities such as input_boolean and input_number.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Mono<List<Map<String, Object>>> listHelpers() {
        return service.helpers();
    }

    @McpTool(name = "list_automations", description = "List Home Assistant automation entities.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Mono<List<Map<String, Object>>> listAutomations() {
        return service.automations();
    }

    @McpTool(name = "get_automation", description = "Get a specific automation entity by entity ID.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Mono<Map<String, Object>> getAutomation(
            @McpArg(name = "automationEntityId", description = "The full automation entity ID, for example automation.night_lights.", required = true)
            String automationEntityId) {
        return service.automation(automationEntityId);
    }

    @McpTool(name = "find_automations_using_entity", description = "Find automations whose available state metadata references an entity.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Mono<Map<String, Object>> findAutomationsUsingEntity(
            @McpArg(name = "entityId", description = "The full Home Assistant entity ID to inspect.", required = true)
            String entityId) {
        return service.automationsUsingEntity(entityId);
    }

    @McpTool(name = "get_entity_relationships", description = "Get known relationships for an entity based on visible automation metadata.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Mono<Map<String, Object>> getEntityRelationships(
            @McpArg(name = "entityId", description = "The full Home Assistant entity ID to inspect.", required = true)
            String entityId) {
        return service.relationships(entityId);
    }

    @McpTool(name = "get_recommendations", description = "Return a summary prompt for AI-driven automation analysis.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Mono<Map<String, Object>> getRecommendations() {
        return service.recommendations();
    }

    @McpTool(name = "analyze_unused_entities", description = "Report whether unused entity analysis is implemented.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Mono<Map<String, Object>> analyzeUnusedEntities() {
        return Mono.just(service.unusedEntities());
    }

    @McpTool(name = "analyze_duplicate_logic", description = "Report whether duplicate automation logic analysis is implemented.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Mono<Map<String, Object>> analyzeDuplicateLogic() {
        return Mono.just(service.duplicateLogic());
    }

    @McpTool(name = "propose_automation", description = "Create an automation proposal for manual review and import.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false))
    public Mono<AutomationProposal> proposeAutomation(
            @McpArg(name = "title", description = "A short human-readable title for the proposed automation.", required = false)
            String title,
            @McpArg(name = "request", description = "The user request describing the automation to propose.", required = true)
            String request) {
        return Mono.just(service.propose(title, request));
    }

    @McpTool(name = "apply_automation_proposal", description = "Approve a stored automation proposal and return manual import instructions.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = true))
    public Mono<Map<String, Object>> applyAutomationProposal(
            @McpArg(name = "proposalId", description = "The proposal ID returned by propose_automation.", required = true)
            String proposalId,
            @McpArg(name = "approvalToken", description = "The approval token returned by propose_automation.", required = true)
            String approvalToken) {
        return Mono.just(service.apply(proposalId, approvalToken));
    }
}
