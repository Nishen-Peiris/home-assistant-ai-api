package com.nishen.homeassistantai;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProposalStore {
    private final Map<String, AutomationProposal> proposals = new ConcurrentHashMap<>();

    public AutomationProposal create(String title, String request, String yaml, String summary) {
        String id = UUID.randomUUID().toString();
        String token = UUID.randomUUID().toString();

        AutomationProposal proposal = new AutomationProposal(
                id,
                token,
                title,
                request,
                yaml,
                summary,
                Instant.now(),
                false
        );

        proposals.put(id, proposal);
        return proposal;
    }

    public Collection<AutomationProposal> list() {
        return proposals.values();
    }

    public AutomationProposal getRequired(String id) {
        AutomationProposal proposal = proposals.get(id);
        if (proposal == null) {
            throw new IllegalArgumentException("Proposal not found: " + id);
        }
        return proposal;
    }

    public AutomationProposal delete(String id) {
        AutomationProposal removed = proposals.remove(id);
        if (removed == null) {
            throw new IllegalArgumentException("Proposal not found: " + id);
        }
        return removed;
    }

    public AutomationProposal approve(String id, String token) {
        AutomationProposal existing = getRequired(id);

        if (!existing.approvalToken().equals(token)) {
            throw new SecurityException("Invalid approval token");
        }

        AutomationProposal approved = new AutomationProposal(
                existing.proposalId(),
                existing.approvalToken(),
                existing.title(),
                existing.request(),
                existing.yaml(),
                existing.summary(),
                existing.createdAt(),
                true
        );

        proposals.put(id, approved);
        return approved;
    }

    public AutomationProposal approve(String id) {
        AutomationProposal existing = getRequired(id);
        return approve(id, existing.approvalToken());
    }
}
