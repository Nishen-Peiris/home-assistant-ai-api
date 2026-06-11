package com.nishen.homeassistantai;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

public record AutomationProposal(
        String proposalId,
        @JsonIgnore
        String approvalToken,
        String title,
        String request,
        String yaml,
        String summary,
        Instant createdAt,
        boolean approved
) {
}
