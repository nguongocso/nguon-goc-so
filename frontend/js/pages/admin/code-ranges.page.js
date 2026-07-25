import {
    requireAuth,
    setupLogout
} from "../../core/auth-guard.js";

import {
    getUser
} from "../../core/storage.js";

import {
    getCodeRangeStatus
} from "../../services/code-range.service.js";

// ---- Auth check ----

if (!requireAuth()) {
    // redirected to login
}

const user = getUser();

if (!user || !user.roleCode) {
    window.location.href = "/frontend/pages/auth/login.html";
}

const roleCode = user.roleCode;

const allowedRoles = ["VT-01"];

if (!allowedRoles.includes(roleCode)) {
    document.getElementById("loadingState").style.display = "none";
    document.getElementById("unauthorizedState").style.display = "flex";
    document.getElementById("dashboardContent").style.display = "none";
    throw new Error("Access denied: user does not have permission to access Code Range Management.");
}

// ---- DOM references ----

const loadingState = document.getElementById("loadingState");
const errorState = document.getElementById("errorState");
const errorMessage = document.getElementById("errorMessage");
const retryButton = document.getElementById("retryButton");
const unauthorizedState = document.getElementById("unauthorizedState");
const dashboardContent = document.getElementById("dashboardContent");
const emptyState = document.getElementById("emptyState");
const codeRangeTable = document.getElementById("codeRangeTable");
const codeRangeTableBody = document.getElementById("codeRangeTableBody");

// ---- Load code ranges ----

async function loadCodeRanges() {
    // Show loading, hide others
    loadingState.style.display = "flex";
    errorState.style.display = "none";
    unauthorizedState.style.display = "none";
    dashboardContent.style.display = "none";

    try {
        const response = await getCodeRangeStatus();

        if (!response.success) {
            throw new Error(response.message || "Failed to load code ranges.");
        }

        const codeRanges = response.data || [];

        // Hide loading, show content
        loadingState.style.display = "none";
        dashboardContent.style.display = "block";

        // Render
        renderCodeRanges(codeRanges);

    } catch (error) {
        console.error("Load code ranges error:", error);

        loadingState.style.display = "none";
        dashboardContent.style.display = "none";

        errorMessage.textContent = error.message || "An unexpected error occurred while loading code ranges.";
        errorState.style.display = "flex";
    }
}

// ---- Render code range list ----

function renderCodeRanges(codeRanges) {
    if (!codeRanges || codeRanges.length === 0) {
        codeRangeTable.style.display = "none";
        emptyState.style.display = "flex";
        return;
    }

    emptyState.style.display = "none";
    codeRangeTable.style.display = "table";

    // Clear existing rows
    codeRangeTableBody.innerHTML = "";

    codeRanges.forEach(function (cr) {
        var row = document.createElement("tr");

        // Organization
        var orgCell = document.createElement("td");
        orgCell.setAttribute("data-label", "Organization");
        orgCell.textContent = cr.organizationName || "—";
        row.appendChild(orgCell);

        // Prefix
        var prefixCell = document.createElement("td");
        prefixCell.setAttribute("data-label", "Prefix");
        prefixCell.textContent = cr.prefix || "—";
        row.appendChild(prefixCell);

        // Limit
        var limitCell = document.createElement("td");
        limitCell.setAttribute("data-label", "Limit");
        limitCell.textContent = cr.totalLimit != null ? cr.totalLimit.toLocaleString() : "—";
        row.appendChild(limitCell);

        // Used
        var usedCell = document.createElement("td");
        usedCell.setAttribute("data-label", "Used");
        usedCell.textContent = cr.usedCount != null ? cr.usedCount.toLocaleString() : "—";
        row.appendChild(usedCell);

        // Remaining
        var remainingCell = document.createElement("td");
        remainingCell.setAttribute("data-label", "Remaining");
        var remaining = (cr.totalLimit != null && cr.usedCount != null)
            ? (cr.totalLimit - cr.usedCount)
            : null;
        remainingCell.textContent = remaining != null ? remaining.toLocaleString() : "—";
        row.appendChild(remainingCell);

        // Usage
        var usageCell = document.createElement("td");
        usageCell.setAttribute("data-label", "Usage");
        if (cr.usagePercent != null) {
            usageCell.textContent = cr.usagePercent + "%";
        } else {
            usageCell.textContent = "—";
        }
        row.appendChild(usageCell);

        // Status
        var statusCell = document.createElement("td");
        statusCell.setAttribute("data-label", "Status");

        var statusBadge = document.createElement("span");
        statusBadge.className = "status-badge";

        var status = cr.status || "";

        if (status === "OK") {
            statusBadge.classList.add("status-badge-active");
            statusBadge.textContent = "Active";
        } else if (status === "NEARLY_EXHAUSTED") {
            statusBadge.classList.add("code-range-status-warning");
            statusBadge.textContent = "Nearly Exhausted";
        } else if (status === "EXHAUSTED") {
            statusBadge.classList.add("status-badge-inactive");
            statusBadge.textContent = "Exhausted";
        } else {
            statusBadge.textContent = status || "—";
        }

        statusCell.appendChild(statusBadge);
        row.appendChild(statusCell);

        // Created At — not returned by /status endpoint, display "—"
        var createdCell = document.createElement("td");
        createdCell.setAttribute("data-label", "Created At");
        createdCell.textContent = "—";
        row.appendChild(createdCell);

        codeRangeTableBody.appendChild(row);
    });
}

// ---- Retry ----

retryButton.addEventListener("click", function () {
    loadCodeRanges();
});

// ---- Setup logout ----

setupLogout();

// ---- Initial load ----

loadCodeRanges();