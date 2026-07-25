import {
    requireAuth,
    setupLogout
} from "../../core/auth-guard.js";

import {
    getUser
} from "../../core/storage.js";

import {
    getOrganizations
} from "../../services/organization.service.js";

import {
    createCodeRange
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
    const app = document.getElementById("app") || document.body;
    app.innerHTML = `
        <main class="code-range-page">
            <section class="code-range-card" style="text-align:center;padding:80px 40px;">
                <h1 style="color:var(--color-danger);margin-bottom:16px;">Access Denied</h1>
                <p style="color:var(--color-text-muted);font-size:1.1rem;">
                    You do not have permission to create code ranges.
                </p>
                <a href="/frontend/pages/admin/code-ranges/index.html"
                   class="btn btn-primary"
                   style="display:inline-block;margin-top:24px;padding:12px 32px;border-radius:var(--border-radius-sm);text-decoration:none;">
                    Back to Code Ranges
                </a>
            </section>
        </main>
    `;
    throw new Error("Access denied: user does not have permission to create code ranges.");
}

// ---- DOM references ----

const codeRangeForm = document.getElementById("codeRangeForm");
const formMessage = document.getElementById("formMessage");
const submitButton = document.getElementById("submitButton");
const cancelButton = document.getElementById("cancelButton");
const organizationSelect = document.getElementById("organizationId");
const prefixInput = document.getElementById("prefix");
const totalLimitInput = document.getElementById("totalLimit");

// ---- Store loaded organizations for validation ----

var loadedOrganizations = [];

// ---- Load organizations for dropdown ----

async function loadOrganizations() {
    try {
        const response = await getOrganizations();

        if (!response.success) {
            console.warn("Failed to load organizations:", response.message);
            return;
        }

        const organizations = response.data || [];

        // Store raw organizations for pre-submit validation
        loadedOrganizations = organizations;

        // Filter to only COOPERATIVE type
        var cooperativeOrgs = organizations.filter(function (org) {
            return org.organizationType === "COOPERATIVE";
        });

        // Clear existing options
        organizationSelect.innerHTML = "";

        if (cooperativeOrgs.length === 0) {
            // No eligible organizations
            var noOption = document.createElement("option");
            noOption.value = "";
            noOption.textContent = "No cooperative organizations available";
            noOption.disabled = true;
            noOption.selected = true;
            organizationSelect.appendChild(noOption);
            submitButton.disabled = true;
            return;
        }

        // Add placeholder
        var placeholder = document.createElement("option");
        placeholder.value = "";
        placeholder.textContent = "Select an organization";
        organizationSelect.appendChild(placeholder);

        cooperativeOrgs.forEach(function (org) {
            var option = document.createElement("option");
            option.value = org.organizationID;
            option.textContent = org.organizationName + " (" + (org.organizationCode || "") + ")";
            organizationSelect.appendChild(option);
        });

    } catch (error) {
        console.error("Load organizations error:", error);
        // Show error on the select field
        var errorOption = document.createElement("option");
        errorOption.value = "";
        errorOption.textContent = "Failed to load organizations";
        errorOption.disabled = true;
        organizationSelect.innerHTML = "";
        organizationSelect.appendChild(errorOption);
    }
}

// ---- Form submit ----

codeRangeForm.addEventListener("submit", async function (event) {
    event.preventDefault();

    clearMessage();
    clearFieldErrors();

    const formData = new FormData(codeRangeForm);

    const codeRangeData = {
        organizationId: getFormValue(formData, "organizationId"),
        prefix: getFormValue(formData, "prefix"),
        totalLimit: parseTotalLimit(getFormValue(formData, "totalLimit"))
    };

    const errors = validateCodeRange(codeRangeData);

    if (Object.keys(errors).length > 0) {
        showFieldErrors(errors);
        return;
    }

    // Pre-submit validation: selected organization must be COOPERATIVE
    var selectedOrg = loadedOrganizations.find(function (org) {
        return org.organizationID === codeRangeData.organizationId;
    });

    if (!selectedOrg || selectedOrg.organizationType !== "COOPERATIVE") {
        showError("Only organizations of type COOPERATIVE can be assigned a Code Range.");
        setLoading(false);
        return;
    }

    setLoading(true);

    try {
        const response = await createCodeRange(codeRangeData);

        // Redirect back to code range list after successful creation
        window.location.href = "index.html";

    } catch (error) {
        console.error("Create code range error:", error);

        const message = error.message || "An unexpected error occurred.";

        // Map backend validation messages to fields
        if (message.toLowerCase().includes("tiền tố mã đã tồn tại") ||
            message.toLowerCase().includes("prefix") && message.toLowerCase().includes("already") ||
            message.toLowerCase().includes("prefix") && message.toLowerCase().includes("exist") ||
            message.toLowerCase().includes("prefix") && message.toLowerCase().includes("in use")) {
            const prefixErrorEl = document.querySelector('[data-error-for="prefix"]');
            if (prefixErrorEl) {
                prefixErrorEl.textContent = "This code prefix is already in use. Please enter a different prefix.";
                document.querySelector('[name="prefix"]').classList.add("input-error");
            }
        }

        if (message.toLowerCase().includes("tổ chức không tồn tại") ||
            message.toLowerCase().includes("organization") && message.toLowerCase().includes("exist")) {
            const orgErrorEl = document.querySelector('[data-error-for="organizationId"]');
            if (orgErrorEl) {
                orgErrorEl.textContent = "The selected organization does not exist.";
                document.querySelector('[name="organizationId"]').classList.add("input-error");
            }
        }

        if (message.toLowerCase().includes("giới hạn") ||
            message.toLowerCase().includes("limit") && message.toLowerCase().includes("exceed") ||
            message.toLowerCase().includes("hạn mức")) {
            const limitErrorEl = document.querySelector('[data-error-for="totalLimit"]');
            if (limitErrorEl) {
                limitErrorEl.textContent = message;
                document.querySelector('[name="totalLimit"]').classList.add("input-error");
            }
        }

        showError(message);

    } finally {
        setLoading(false);
    }
});

// ---- Cancel ----

cancelButton.addEventListener("click", function () {
    window.location.href = "index.html";
});

// ---- Setup logout ----

setupLogout();

// ---- Load organizations on page load ----

loadOrganizations();

// ---- Helper functions ----

function getFormValue(formData, fieldName) {
    const value = formData.get(fieldName);
    return value ? value.trim() : "";
}

function parseTotalLimit(value) {
    if (!value) return null;
    const parsed = parseInt(value, 10);
    return isNaN(parsed) ? null : parsed;
}

function validateCodeRange(data) {
    const errors = {};

    // organizationId
    if (!data.organizationId) {
        errors.organizationId = "Organization is required.";
    }

    // prefix
    if (!data.prefix) {
        errors.prefix = "Code prefix is required.";
    } else if (data.prefix.length > 50) {
        errors.prefix = "Code prefix must not exceed 50 characters.";
    }

    // totalLimit
    if (data.totalLimit == null || data.totalLimit === "") {
        errors.totalLimit = "Code range limit is required.";
    } else if (isNaN(data.totalLimit) || data.totalLimit < 1) {
        errors.totalLimit = "Code range limit must be a positive integer.";
    }

    return errors;
}

function showFieldErrors(errors) {
    Object.entries(errors).forEach(function ([fieldName, message]) {
        const errorElement = document.querySelector(`[data-error-for="${fieldName}"]`);
        const inputElement = document.querySelector(`[name="${fieldName}"]`);

        if (errorElement) {
            errorElement.textContent = message;
        }

        if (inputElement) {
            inputElement.classList.add("input-error");
            inputElement.setAttribute("aria-invalid", "true");
        }
    });

    const firstErrorField = Object.keys(errors)[0];
    const firstInput = document.querySelector(`[name="${firstErrorField}"]`);

    if (firstInput) {
        firstInput.focus();
    }
}

function clearFieldErrors() {
    const errorElements = document.querySelectorAll(".field-error");
    errorElements.forEach(function (element) {
        element.textContent = "";
    });

    const errorInputs = document.querySelectorAll(".input-error");
    errorInputs.forEach(function (input) {
        input.classList.remove("input-error");
        input.setAttribute("aria-invalid", "false");
    });
}

function setLoading(isLoading) {
    submitButton.disabled = isLoading;
    submitButton.textContent = isLoading
        ? "Creating..."
        : "Create Code Range";
}

function showSuccess(message) {
    formMessage.textContent = message;
    formMessage.className = "form-message success";
}

function showError(message) {
    formMessage.textContent = message;
    formMessage.className = "form-message error";
}

function clearMessage() {
    formMessage.textContent = "";
    formMessage.className = "form-message";
}