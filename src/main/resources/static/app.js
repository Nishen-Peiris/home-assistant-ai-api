const state = {
  proposals: [],
};

const elements = {
  emptyState: document.getElementById("emptyState"),
  proposalList: document.getElementById("proposalList"),
};

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function proposalById(proposalId) {
  return state.proposals.find((proposal) => proposal.proposalId === proposalId) || null;
}

function formatDate(value) {
  if (!value) {
    return "-";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

async function fetchJson(url, options) {
  const response = await fetch(url, options);
  if (!response.ok) {
    const body = await response.text();
    throw new Error(body || `${response.status} ${response.statusText}`);
  }
  return response.json();
}

function renderProposals() {
  if (state.proposals.length === 0) {
    elements.proposalList.innerHTML = "";
    elements.emptyState.classList.remove("hidden");
    return;
  }

  elements.emptyState.classList.add("hidden");
  const total = state.proposals.length;
  elements.proposalList.innerHTML = state.proposals.map((proposal, index) => {
    const title = proposal.title || "Untitled Proposal";
    const summary = proposal.summary || "-";
    const yaml = proposal.yaml || "";
    const position = `${index + 1} of ${total}`;

    return `
      <article class="panel proposal-detail">
        <div class="detail-topbar">
          <div>
            <p class="panel-kicker">Inspection</p>
            <h2 class="proposal-title">${escapeHtml(title)}</h2>
            <p class="detail-subtext">${escapeHtml(formatDate(proposal.createdAt))}</p>
          </div>
          <span class="inspection-count">${escapeHtml(position)}</span>
        </div>

        <section class="detail-block">
          <h3>Summary</h3>
          <p class="body-copy">${escapeHtml(summary)}</p>
        </section>

        <section class="detail-block yaml-block">
          <div class="block-header">
            <h3>YAML</h3>
            <div class="action-row">
              <button class="button button-danger compact delete-proposal-button" type="button" data-proposal-id="${escapeHtml(proposal.proposalId)}">Delete</button>
              <button class="button button-ghost compact copy-yaml-button" type="button" data-proposal-id="${escapeHtml(proposal.proposalId)}">Copy YAML</button>
            </div>
          </div>
          <pre>${escapeHtml(yaml)}</pre>
        </section>
      </article>`;
  }).join("");

  document.querySelectorAll(".copy-yaml-button").forEach((button) => {
    button.addEventListener("click", async () => {
      const proposalId = button.getAttribute("data-proposal-id") || "";
      const yaml = proposalById(proposalId)?.yaml || "";
      await copyYaml(button, yaml);
    });
  });

  document.querySelectorAll(".delete-proposal-button").forEach((button) => {
    button.addEventListener("click", async () => {
      const proposalId = button.getAttribute("data-proposal-id");
      if (!proposalId) {
        return;
      }
      await deleteProposal(button, proposalId);
    });
  });
}

async function loadProposals() {
  try {
    const proposals = await fetchJson("/api/proposals");
    state.proposals = Array.isArray(proposals) ? proposals : [];
    renderProposals();
  } catch (error) {
    elements.proposalList.innerHTML = "";
    elements.emptyState.classList.add("hidden");
  }
}

async function copyYaml(button, yaml) {
  if (!yaml) {
    return;
  }

  const previous = button.textContent;
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(yaml);
    } else {
      fallbackCopyText(yaml);
    }
    button.textContent = "Copied";
  } catch (error) {
    button.textContent = "Copy failed";
  }

  window.setTimeout(() => {
    button.textContent = previous;
  }, 1200);
}

async function deleteProposal(button, proposalId) {
  const title = button.closest(".proposal-detail")?.querySelector(".proposal-title")?.textContent || "this proposal";
  if (!window.confirm(`Delete ${title}?`)) {
    return;
  }

  const previous = button.textContent;
  button.disabled = true;
  button.textContent = "Deleting...";

  try {
    await fetchJson(`/api/proposals/${encodeURIComponent(proposalId)}`, {
      method: "DELETE",
    });
    state.proposals = state.proposals.filter((proposal) => proposal.proposalId !== proposalId);
    renderProposals();
  } catch (error) {
    button.textContent = "Delete failed";
    window.setTimeout(() => {
      button.textContent = previous;
      button.disabled = false;
    }, 1200);
    return;
  }
}

function fallbackCopyText(text) {
  const textarea = document.createElement("textarea");
  textarea.value = text;
  textarea.setAttribute("readonly", "");
  textarea.style.position = "fixed";
  textarea.style.top = "-9999px";
  textarea.style.left = "-9999px";
  document.body.appendChild(textarea);
  textarea.select();
  textarea.setSelectionRange(0, textarea.value.length);
  const successful = document.execCommand("copy");
  document.body.removeChild(textarea);

  if (!successful) {
    throw new Error("Copy command was rejected");
  }
}

loadProposals();
