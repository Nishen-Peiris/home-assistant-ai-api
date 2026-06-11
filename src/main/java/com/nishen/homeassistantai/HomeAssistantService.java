package com.nishen.homeassistantai;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class HomeAssistantService {
    private static final Pattern YAML_BLOCK_PATTERN = Pattern.compile(
            "(?s)(?:^|\\n\\n)(alias:.*?)(?:\\n\\n[A-Z][^\\n]*|\\z)"
    );
    private static final Pattern ALIAS_PATTERN = Pattern.compile("(?m)^alias:\\s*\"?([^\"]+)\"?\\s*$");
    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("(?m)^description:\\s*\"?([^\"]*)\"?\\s*$");
    private static final Pattern SERVICE_PATTERN = Pattern.compile("(?i)service\\s+([a-z0-9_\\.]+)");
    private static final Pattern MODE_PATTERN = Pattern.compile("(?i)mode\\s*:?\\s*([a-z_]+)");
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("(?i)message\\s+[\"']([^\"']+)[\"']");
    private static final Pattern NOTIFICATION_TITLE_PATTERN = Pattern.compile("(?i)title\\s+[\"']([^\"']+)[\"']");
    private static final Pattern TIME_24H_PATTERN = Pattern.compile("(?i)at\\s+[\"']?(\\d{1,2}:\\d{2}(?::\\d{2})?)[\"']?");
    private static final Pattern TIME_12H_PATTERN = Pattern.compile("(?i)(\\d{1,2}:\\d{2})\\s*(AM|PM)");

    private final HomeAssistantClient ha;
    private final ProposalStore proposalStore;

    public HomeAssistantService(HomeAssistantClient ha, ProposalStore proposalStore) {
        this.ha = ha;
        this.proposalStore = proposalStore;
    }

    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }

    public Collection<AutomationProposal> proposals() {
        return proposalStore.list();
    }

    public AutomationProposal proposal(String proposalId) {
        return proposalStore.getRequired(proposalId);
    }

    public String proposalYaml(String proposalId) {
        return proposalStore.getRequired(proposalId).yaml();
    }

    public Map<String, Object> deleteProposal(String proposalId) {
        AutomationProposal removed = proposalStore.delete(proposalId);
        return Map.of(
                "status", "DELETED",
                "proposalId", removed.proposalId(),
                "title", removed.title()
        );
    }

    public Mono<List<Map<String, Object>>> states() {
        return ha.states();
    }

    public Mono<List<Map<String, Object>>> areas() {
        return ha.areas();
    }

    public Mono<List<Map<String, Object>>> devices() {
        return ha.devices();
    }

    public Mono<Map<String, Object>> state(String entityId) {
        return ha.state(entityId);
    }

    public Mono<List<Map<String, Object>>> entities() {
        return ha.states().map(states -> states.stream()
                .map(s -> Map.of(
                        "entityId", s.get("entity_id"),
                        "state", s.get("state"),
                        "attributes", s.get("attributes")
                ))
                .collect(Collectors.toList()));
    }

    public Mono<List<Map<String, Object>>> helpers() {
        return ha.states().map(states -> states.stream()
                .filter(s -> {
                    String id = String.valueOf(s.get("entity_id"));
                    return id.startsWith("input_boolean.")
                            || id.startsWith("input_number.")
                            || id.startsWith("input_select.")
                            || id.startsWith("input_text.")
                            || id.startsWith("input_datetime.");
                })
                .collect(Collectors.toList()));
    }

    public Mono<List<Map<String, Object>>> automations() {
        return ha.states().map(states -> states.stream()
                .filter(s -> String.valueOf(s.get("entity_id")).startsWith("automation."))
                .collect(Collectors.toList()));
    }

    public Mono<Map<String, Object>> automation(String automationEntityId) {
        return ha.state(automationEntityId);
    }

    public Mono<Map<String, Object>> automationsUsingEntity(String entityId) {
        return automations().map(autos -> Map.of(
                "entityId", entityId,
                "note", "Home Assistant REST states do not expose UI automation YAML. This endpoint returns matches only if entity references are visible in available state metadata.",
                "matchingAutomations", autos.stream()
                        .filter(a -> String.valueOf(a).contains(entityId))
                        .collect(Collectors.toList())
        ));
    }

    public Mono<Map<String, Object>> relationships(String entityId) {
        return automationsUsingEntity(entityId).map(result -> Map.of(
                "entityId", entityId,
                "usedBy", result.get("matchingAutomations"),
                "limitations", "For UI-created automations, Home Assistant does not expose full YAML through the standard states API."
        ));
    }

    public Mono<Map<String, Object>> recommendations() {
        return ha.states().map(states -> Map.of(
                "summary", "Use Open WebUI/GPT to analyze these entities and suggest automations.",
                "entityCount", states.size(),
                "suggestedPrompt", "Review the entity list and suggest useful Home Assistant automations."
        ));
    }

    public Map<String, Object> unusedEntities() {
        return Map.of(
                "status", "NOT_IMPLEMENTED",
                "note", "Requires recorder/history access or automation YAML access."
        );
    }

    public Map<String, Object> duplicateLogic() {
        return Map.of(
                "status", "NOT_IMPLEMENTED",
                "note", "Requires automation YAML/config access."
        );
    }

    public AutomationProposal propose(String title, String request) {
        String extractedYaml = extractYamlFromRequest(request);
        String resolvedTitle = safeTitle(firstNonBlank(title, extractAlias(extractedYaml)));
        String yaml = extractedYaml != null ? extractedYaml : synthesizeYamlFromRequest(resolvedTitle, request);
        String normalizedRequest = extractedYaml != null
                ? summarizeYamlRequest(resolvedTitle, yaml, request)
                : normalizeRequest(request, null);
        String summary = extractedYaml != null
                ? summarizeYamlProposal(resolvedTitle, yaml)
                : summarizeGeneratedProposal(request, yaml, resolvedTitle);

        return proposalStore.create(resolvedTitle, normalizedRequest, yaml, summary);
    }

    public Map<String, Object> apply(String proposalId, String approvalToken) {
        AutomationProposal approved = (approvalToken == null || approvalToken.isBlank())
                ? proposalStore.approve(proposalId)
                : proposalStore.approve(proposalId, approvalToken);

        return Map.of(
                "status", "APPROVED_MANUAL_IMPORT_REQUIRED",
                "proposal", approved,
                "instructions", List.of(
                        "Open Home Assistant.",
                        "Go to Settings -> Automations & scenes.",
                        "Create Automation -> Edit in YAML.",
                        "Paste the approved YAML.",
                        "Save and test manually."
                )
        );
    }

    private String safeAlias(String title) {
        return title.replace("\"", "'");
    }

    private String defaultYaml(String title) {
        return """
                alias: "%s"
                description: "Generated proposal. Review before importing into Home Assistant."
                trigger: []
                condition: []
                action: []
                mode: single
                """.formatted(safeAlias(title));
    }

    private String synthesizeYamlFromRequest(String title, String request) {
        String description = safeAlias(summarizeRequestSentence(request, title));
        String time = extractTriggerTime(request);
        String service = extractPattern(request, SERVICE_PATTERN);
        String mode = firstNonBlank(extractPattern(request, MODE_PATTERN), "single");
        String notificationTitle = extractPattern(request, NOTIFICATION_TITLE_PATTERN);
        String message = extractPattern(request, MESSAGE_PATTERN);
        boolean noConditions = containsIgnoreCase(request, "condition: none")
                || containsIgnoreCase(request, "condition none")
                || containsIgnoreCase(request, "condition: []");

        String trigger = time != null
                ? """
                trigger:
                  - platform: time
                    at: "%s"
                """.formatted(time)
                : "trigger: []\n";

        String condition = noConditions ? "condition: []\n" : "condition: []\n";

        String action = service != null
                ? buildNotificationAction(service, notificationTitle, message)
                : "action: []\n";

        return """
                alias: "%s"
                description: "%s"
                %s%s%smode: %s
                """.formatted(
                safeAlias(title),
                description,
                trigger,
                condition,
                action,
                mode
        );
    }

    private String buildNotificationAction(String service, String notificationTitle, String message) {
        if (notificationTitle == null && message == null) {
            return """
                    action:
                      - service: %s
                    """.formatted(service);
        }

        StringBuilder builder = new StringBuilder();
        builder.append("action:\n");
        builder.append("  - service: ").append(service).append("\n");
        builder.append("    data:\n");
        if (notificationTitle != null) {
            builder.append("      title: \"").append(escapeYaml(notificationTitle)).append("\"\n");
        }
        if (message != null) {
            builder.append("      message: \"").append(escapeYaml(message)).append("\"\n");
        }
        return builder.toString();
    }

    private String safeTitle(String title) {
        if (title == null || title.isBlank()) {
            return "AI Proposed Automation";
        }

        return title.trim();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private String extractYamlFromRequest(String request) {
        if (request == null || request.isBlank()) {
            return null;
        }

        Matcher matcher = YAML_BLOCK_PATTERN.matcher(request);
        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1).trim() + "\n";
    }

    private String extractAlias(String yaml) {
        if (yaml == null || yaml.isBlank()) {
            return null;
        }

        Matcher matcher = ALIAS_PATTERN.matcher(yaml);
        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1).trim();
    }

    private String extractDescription(String yaml) {
        if (yaml == null || yaml.isBlank()) {
            return null;
        }

        Matcher matcher = DESCRIPTION_PATTERN.matcher(yaml);
        if (!matcher.find()) {
            return null;
        }

        String description = matcher.group(1).trim();
        return description.isBlank() ? null : description;
    }

    private String normalizeRequest(String request, String extractedYaml) {
        if (request == null || request.isBlank()) {
            return "";
        }

        if (extractedYaml == null) {
            return request.trim();
        }

        String normalized = request.replace(extractedYaml.trim(), "[User supplied Home Assistant YAML]").trim();
        return normalized.replaceAll("\\n{3,}", "\n\n");
    }

    private String summarizeYamlRequest(String title, String yaml, String originalRequest) {
        String description = extractDescription(yaml);
        String prose = stripYamlFromRequest(originalRequest, yaml);

        if (prose != null && !prose.isBlank()) {
            return prose;
        }

        if (description != null) {
            return "Create or import the automation \"" + title + "\": " + description;
        }

        return "Create or import the automation \"" + title + "\" from user-supplied Home Assistant YAML.";
    }

    private String summarizeYamlProposal(String title, String yaml) {
        String description = extractDescription(yaml);
        if (description != null) {
            return description + " Review the YAML before importing into Home Assistant.";
        }

        return "Review the YAML for \"" + title + "\" before importing it into Home Assistant.";
    }

    private String stripYamlFromRequest(String request, String yaml) {
        if (request == null || request.isBlank()) {
            return "";
        }

        String withoutYaml = request.replace(yaml.trim(), " ").replaceAll("\\s+", " ").trim();
        withoutYaml = withoutYaml.replace("with this YAML:", "").trim();
        withoutYaml = withoutYaml.replace("with the following YAML:", "").trim();
        withoutYaml = withoutYaml.replace("Create a Home Assistant automation", "Create a Home Assistant automation").trim();
        return withoutYaml;
    }

    private String summarizeGeneratedProposal(String request, String yaml, String title) {
        String description = extractDescription(yaml);
        if (description != null && !description.isBlank()) {
            return description;
        }

        return summarizeRequestSentence(request, title);
    }

    private String summarizeRequestSentence(String request, String title) {
        if (request == null || request.isBlank()) {
            return "Generated automation proposal for \"" + title + "\".";
        }

        String normalized = request.replaceAll("\\s+", " ").trim();
        normalized = normalized.replaceFirst("(?i)^create a home assistant automation that\\s*", "");
        normalized = normalized.replaceFirst("(?i)^create a home assistant automation\\s*", "");
        normalized = normalized.replaceFirst("(?i)^with this yaml:?\\s*", "");

        int sentenceEnd = normalized.indexOf(". ");
        String summary = sentenceEnd >= 0 ? normalized.substring(0, sentenceEnd + 1) : normalized;
        if (summary.length() > 180) {
            summary = summary.substring(0, 177).trim() + "...";
        }
        return Character.toUpperCase(summary.charAt(0)) + summary.substring(1);
    }

    private String extractTriggerTime(String request) {
        String explicit24h = extractPattern(request, TIME_24H_PATTERN);
        if (explicit24h != null) {
            return normalizeTime(explicit24h);
        }

        Matcher matcher = TIME_12H_PATTERN.matcher(request == null ? "" : request);
        if (!matcher.find()) {
            return null;
        }

        String time = matcher.group(1);
        String meridiem = matcher.group(2);
        return to24Hour(time, meridiem);
    }

    private String normalizeTime(String value) {
        String[] parts = value.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        int second = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        return "%02d:%02d:%02d".formatted(hour, minute, second);
    }

    private String to24Hour(String time, String meridiem) {
        String[] parts = time.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        if ("PM".equalsIgnoreCase(meridiem) && hour < 12) {
            hour += 12;
        }
        if ("AM".equalsIgnoreCase(meridiem) && hour == 12) {
            hour = 0;
        }
        return "%02d:%02d:00".formatted(hour, minute);
    }

    private String extractPattern(String input, Pattern pattern) {
        if (input == null || input.isBlank()) {
            return null;
        }

        Matcher matcher = pattern.matcher(input);
        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1).trim();
    }

    private boolean containsIgnoreCase(String input, String search) {
        return input != null && input.toLowerCase().contains(search.toLowerCase());
    }

    private String escapeYaml(String value) {
        return value.replace("\"", "\\\"");
    }
}
