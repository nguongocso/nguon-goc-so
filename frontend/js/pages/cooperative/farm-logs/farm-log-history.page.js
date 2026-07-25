import {
    getFarmLogHistory,
    getFarmLogAttachments,
    uploadFarmLogAttachment,
    deleteFarmLogAttachment
} from "../../../services/farm-log.service.js";

import {
    clearAuth,
    getUser
} from "../../../core/storage.js";

document.addEventListener(
    "DOMContentLoaded",
    initPage
);

// ── Upload Modal ──────────────────────────────────────────

let uploadModal = null;
let uploadModalOverlay = null;
let selectedFarmLogId = null;

function createUploadModal() {
    // Overlay
    const overlay = document.createElement("div");
    overlay.className = "modal-overlay upload-modal-overlay";
    overlay.style.display = "none";
    document.body.appendChild(overlay);

    // Modal
    const modal = document.createElement("div");
    modal.className = "upload-modal";
    modal.innerHTML = `
        <div class="upload-modal-header">
            <h3>Upload Attachment</h3>
            <button type="button" class="upload-modal-close-btn">&times;</button>
        </div>
        <form id="uploadAttachmentForm" class="upload-modal-body">
            <input type="hidden" id="uploadFarmLogId" value="" />
            <div class="upload-modal-field">
                <label for="uploadFileInput">File <span class="required">*</span></label>
                <input type="file" id="uploadFileInput" required accept=".jpg,.jpeg,.png,.pdf" />
            </div>
            <div class="upload-modal-field">
                <label for="uploadDescriptionInput">Description</label>
                <textarea id="uploadDescriptionInput" rows="2" placeholder="Optional description"></textarea>
            </div>
            <div class="upload-modal-actions">
                <button type="button" class="btn btn-secondary upload-modal-cancel-btn">Cancel</button>
                <button type="submit" class="btn btn-primary upload-modal-submit-btn">Upload</button>
            </div>
            <div id="uploadModalError" class="upload-modal-error" style="display:none;"></div>
        </form>
    `;
    document.body.appendChild(modal);

    uploadModalOverlay = overlay;
    uploadModal = modal;

    // Close handlers
    const closeBtn = modal.querySelector(".upload-modal-close-btn");
    const cancelBtn = modal.querySelector(".upload-modal-cancel-btn");

    function closeModal() {
        modal.style.display = "none";
        overlay.style.display = "none";
        selectedFarmLogId = null;
        document.getElementById("uploadFileInput").value = "";
        document.getElementById("uploadDescriptionInput").value = "";
        document.getElementById("uploadModalError").style.display = "none";
    }

    closeBtn.addEventListener("click", closeModal);
    cancelBtn.addEventListener("click", closeModal);
    overlay.addEventListener("click", closeModal);

    // Submit handler
    const form = document.getElementById("uploadAttachmentForm");
    form.addEventListener("submit", handleUploadSubmit);
}

function openUploadModal(farmLogId) {
    if (!uploadModal) {
        createUploadModal();
    }
    selectedFarmLogId = farmLogId;
    document.getElementById("uploadFarmLogId").value = farmLogId;
    document.getElementById("uploadFileInput").value = "";
    document.getElementById("uploadDescriptionInput").value = "";
    document.getElementById("uploadModalError").style.display = "none";
    uploadModal.style.display = "block";
    uploadModalOverlay.style.display = "block";
}

async function handleUploadSubmit(event) {
    event.preventDefault();

    const fileInput = document.getElementById("uploadFileInput");
    const descriptionInput = document.getElementById("uploadDescriptionInput");
    const errorDiv = document.getElementById("uploadModalError");

    if (!fileInput.files || fileInput.files.length === 0) {
        errorDiv.textContent = "Please select a file.";
        errorDiv.style.display = "block";
        return;
    }

    const file = fileInput.files[0];
    const description = descriptionInput.value.trim() || "";

    // Validate file type
    const allowedTypes = ["image/jpeg", "image/png", "application/pdf"];
    if (!allowedTypes.includes(file.type)) {
        errorDiv.textContent = "Only JPG, PNG, and PDF files are supported.";
        errorDiv.style.display = "block";
        return;
    }

    // Validate file size (5MB)
    if (file.size > 5 * 1024 * 1024) {
        errorDiv.textContent = "File size must not exceed 5MB.";
        errorDiv.style.display = "block";
        return;
    }

    const submitBtn = document.querySelector(".upload-modal-submit-btn");
    submitBtn.disabled = true;
    submitBtn.textContent = "Uploading...";
    errorDiv.style.display = "none";

    try {
        const formData = new FormData();
        formData.append("file", file);
        if (description) {
            formData.append("description", description);
        }

        const response = await uploadFarmLogAttachment(selectedFarmLogId, formData);

        if (response?.success) {
            // Close modal
            uploadModal.style.display = "none";
            uploadModalOverlay.style.display = "none";
            selectedFarmLogId = null;

            // Refresh the history page to show new attachment
            const productionLotId = getProductionLotId();
            const pageSizeSelect = document.getElementById("pageSizeSelect");
            const pageSize = pageSizeSelect ? Number(pageSizeSelect.value) : 10;
            const currentPage = getCurrentPage();
            await loadFarmLogHistory(productionLotId, currentPage, pageSize);
        } else {
            errorDiv.textContent = response?.message || "Upload failed.";
            errorDiv.style.display = "block";
        }
    } catch (error) {
        console.error("Upload error:", error);
        errorDiv.textContent = error.message || "An error occurred during upload.";
        errorDiv.style.display = "block";
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = "Upload";
    }
}

function getCurrentPage() {
    const pageText = document.getElementById("currentPageText");
    if (pageText) {
        const val = parseInt(pageText.textContent, 10);
        return val > 0 ? val - 1 : 0;
    }
    return 0;
}

// ── Delete Attachment ─────────────────────────────────────

async function handleDeleteAttachment(attachmentId) {
    if (!confirm("Are you sure you want to delete this attachment?")) {
        return;
    }

    try {
        const response = await deleteFarmLogAttachment(attachmentId);

        // Refresh the page
        const productionLotId = getProductionLotId();
        const pageSizeSelect = document.getElementById("pageSizeSelect");
        const pageSize = pageSizeSelect ? Number(pageSizeSelect.value) : 10;
        const currentPage = getCurrentPage();
        await loadFarmLogHistory(productionLotId, currentPage, pageSize);
    } catch (error) {
        console.error("Delete error:", error);
        alert(error.message || "An error occurred while deleting the attachment.");
    }
}

// ── Sidebar setup ─────────────────────────────────────────

function setupSidebarByRole() {
    const currentUser = getUser();

    if (!currentUser || !currentUser.roleCode) {
        return;
    }

    if (currentUser.roleCode === "VT-03") {
        const menuIds = [
            "dashboardMenu",
            "farmAreasMenu",
            "organizationProfileMenu"
        ];

        menuIds.forEach(function (menuId) {
            const menuItem = document.getElementById(menuId);
            if (menuItem) {
                menuItem.style.display = "none";
            }
        });
    }
}

// ── Page Init ─────────────────────────────────────────────

async function initPage() {
    setupSidebarByRole();
    bindEvents();
    renderCurrentUser();

    const productionLotId = getProductionLotId();
    const productionLotName = getProductionLotName();

    setText("productionLotName", productionLotName || "\u2014");

    if (!productionLotId) {
        showError("Thi\u1ebfu productionLotId.");
        return;
    }

    await loadFarmLogHistory(productionLotId, 0, 10);
}

// ── Event Bindings ────────────────────────────────────────

function bindEvents() {
    const logoutButton = document.getElementById("logoutButton");
    const backButton = document.getElementById("backButton");
    const retryButton = document.getElementById("retryButton");
    const pageSizeSelect = document.getElementById("pageSizeSelect");

    if (logoutButton) {
        logoutButton.addEventListener("click", handleLogout);
    }

    if (backButton) {
        backButton.addEventListener("click", () => {
            window.location.href = "../production-lots/index.html";
        });
    }

    if (retryButton) {
        retryButton.addEventListener("click", () => {
            window.location.reload();
        });
    }

    if (pageSizeSelect) {
        pageSizeSelect.addEventListener("change", async () => {
            const productionLotId = getProductionLotId();
            const size = Number(pageSizeSelect.value);
            await loadFarmLogHistory(productionLotId, 0, size);
        });
    }
}

// ── User Info Rendering ───────────────────────────────────

function renderCurrentUser() {
    let currentUser = null;

    try {
        currentUser = JSON.parse(localStorage.getItem("currentUser"));
    } catch (error) {
        console.error("Cannot read user info:", error);
    }

    if (!currentUser) {
        return;
    }

    setText("headerUserRole", currentUser.roleCode || currentUser.roleName || "");
    setText("headerUserName", currentUser.fullName || currentUser.username || "User");
    setText("headerUserOrg", currentUser.organizationName || currentUser.organizationCode || "");
    setText("sidebarUserName", currentUser.fullName || currentUser.username || "User");
    setText("sidebarUserOrg", currentUser.organizationName || currentUser.organizationCode || "");

    const avatar = document.getElementById("sidebarUserAvatar");
    if (avatar) {
        avatar.textContent = (currentUser.fullName || currentUser.username || "N")
            .trim()
            .charAt(0)
            .toUpperCase();
    }
}

// ── URL Params ────────────────────────────────────────────

function getProductionLotId() {
    const params = new URLSearchParams(window.location.search);
    return params.get("productionLotId");
}

function getProductionLotName() {
    const params = new URLSearchParams(window.location.search);
    return params.get("productionLotName");
}

// ── Data Loading ──────────────────────────────────────────

async function loadFarmLogHistory(productionLotId, page, size) {
    showLoading();

    try {
        const response = await getFarmLogHistory(productionLotId, page, size);
        const pageData = response?.data;

        if (!pageData) {
            throw new Error("Invalid response data.");
        }

        // Store current page info for refresh
        storePageInfo(pageData);

        renderPage(pageData);
    } catch (error) {
        console.error(error);
        showError(error.message || "Cannot load farm log history.");
    }
}

function storePageInfo(pageData) {
    // Store current page state in sessionStorage for refresh capability
    try {
        sessionStorage.setItem("farmLogHistoryPage", JSON.stringify({
            page: pageData.page ?? 0,
            size: pageData.size ?? 10
        }));
    } catch (e) {
        // Ignore storage errors
    }
}

// ── Page Rendering ────────────────────────────────────────

function renderPage(pageData) {
    hideAllStates();

    const items = Array.isArray(pageData.items) ? pageData.items : [];

    updateSummary(pageData);

    const mainContent = document.getElementById("mainContent");
    if (mainContent) {
        mainContent.style.display = "block";
    }

    if (items.length === 0) {
        showEmptyState();
        return;
    }

    renderTimeline(items, pageData);
    renderPagination(pageData);
}

function updateSummary(pageData) {
    const totalElements = pageData.totalElements ?? 0;

    setText("totalElements", totalElements);
    setText("activityCount", totalElements);
    setText("paginationTotalElements", totalElements);
    setText("currentPageText", (pageData.page ?? 0) + 1);
    setText("totalPagesText", pageData.totalPages ?? 0);
}

function showEmptyState() {
    const emptyState = document.getElementById("emptyState");
    const historyTimeline = document.getElementById("historyTimeline");
    const paginationContainer = document.getElementById("paginationContainer");

    if (emptyState) emptyState.style.display = "flex";
    if (historyTimeline) historyTimeline.style.display = "none";
    if (paginationContainer) paginationContainer.style.display = "none";
}

function renderTimeline(items, pageData) {
    const timeline = document.getElementById("historyTimeline");
    const emptyState = document.getElementById("emptyState");

    if (!timeline) return;

    const currentUser = getUser();
    const roleCode = currentUser?.roleCode || "";
    const isVT03 = roleCode === "VT-03";

    timeline.innerHTML = items
        .map(item => createTimelineCard(item, isVT03))
        .join("");

    timeline.style.display = "block";
    if (emptyState) emptyState.style.display = "none";
}

function createTimelineCard(item, isVT03) {
    const attachments = Array.isArray(item.attachments) ? item.attachments : [];
    const attachmentCount = attachments.length;
    const farmLogId = item.id;

    let attachmentsHtml = "";

    // Build attachments section
    if (attachmentCount > 0) {
        const attachmentItemsHtml = attachments.map(function(att) {
            const deleteBtnHtml = isVT03
                ? '<button type="button" class="attachment-delete-btn" data-attachment-id="' + att.id + '" title="Delete attachment">&times;</button>'
                : "";

            return '\n                            <div class="attachment-item">\n                                <span class="attachment-icon">\uD83D\uDCCE</span>\n                                <div class="attachment-info">\n                                    <span class="attachment-filename">' + escapeHtml(att.fileName || "\u2014") + '</span>\n                                    ' + (att.description ? '<span class="attachment-description">' + escapeHtml(att.description) + '</span>' : '') + '\n                                    <span class="attachment-meta">\n                                        ' + (att.fileType ? escapeHtml(att.fileType) : "") + '\n                                        ' + (att.fileSize ? " | " + formatFileSize(att.fileSize) : "") + '\n                                        ' + (att.uploadedAt ? " | " + formatDateTime(att.uploadedAt) : "") + '\n                                    </span>\n                                </div>\n                                <a href="' + escapeHtml(att.fileUrl) + '" class="attachment-view-btn" target="_blank" rel="noopener">View</a>\n                                ' + deleteBtnHtml + '\n                            </div>\n                        ';
        }).join("");

        attachmentsHtml = '\n            <div class="farm-log-attachments">\n                <div class="attachments-header" onclick="this.parentElement.classList.toggle(\'attachments-expanded\')">\n                    <span class="attachments-toggle">\u25B6</span>\n                    <span class="attachments-label">Attachments (' + attachmentCount + ')</span>\n                </div>\n                <div class="attachments-list">\n                    ' + attachmentItemsHtml + '\n                </div>\n            </div>\n        ';
    } else {
        // Show empty attachments section but keep it collapsed
        attachmentsHtml = '\n            <div class="farm-log-attachments">\n                <div class="attachments-header" onclick="this.parentElement.classList.toggle(\'attachments-expanded\')">\n                    <span class="attachments-toggle">\u25B6</span>\n                    <span class="attachments-label">Attachments (0)</span>\n                </div>\n                <div class="attachments-list">\n                    <div class="attachment-empty">No attachments yet.</div>\n                </div>\n            </div>\n        ';
    }

    // Add Attachment button (VT-03 only)
    const addAttachmentHtml = isVT03
        ? '\n                <button type="button" class="btn btn-sm btn-primary add-attachment-btn" data-farm-log-id="' + farmLogId + '">\n                    + Add Attachment\n                </button>\n            '
        : "";

    return '\n        <div class="timeline-card">\n            <div class="timeline-card-header">\n                <span class="timeline-date">' + formatDate(item.executedDate) + '</span>\n                <span class="timeline-activity-type ' + getActivityTypeClass(item.activityType) + '">' + escapeHtml(item.activityType ?? "\u2014") + '</span>\n            </div>\n\n            <div class="timeline-card-body">\n                ' + (item.material ? '\n                    <div class="timeline-field">\n                        <span class="timeline-field-label">Material:</span>\n                        <span class="timeline-field-value">' + escapeHtml(item.material) + '</span>\n                    </div>\n                ' : "") + '\n\n                ' + (item.quantity != null ? '\n                    <div class="timeline-field">\n                        <span class="timeline-field-label">Quantity:</span>\n                        <span class="timeline-field-value">' + item.quantity + " " + escapeHtml(item.unit ?? "") + '</span>\n                    </div>\n                ' : "") + '\n\n                ' + (item.notes ? '\n                    <div class="timeline-field">\n                        <span class="timeline-field-label">Notes:</span>\n                        <span class="timeline-field-value">' + escapeHtml(item.notes) + '</span>\n                    </div>\n                ' : "") + '\n\n                <div class="timeline-field">\n                    <span class="timeline-field-label">Created by:</span>\n                    <span class="timeline-field-value">' + escapeHtml(item.createdByName ?? "\u2014") + '</span>\n                </div>\n\n                <div class="timeline-field">\n                    <span class="timeline-field-label">Created at:</span>\n                    <span class="timeline-field-value">' + formatDateTime(item.createdAt) + '</span>\n                </div>\n            </div>\n\n            ' + attachmentsHtml + '\n\n            ' + addAttachmentHtml + '\n        </div>\n    ';
}

// ── Activity Type CSS Class ───────────────────────────────

function getActivityTypeClass(activityType) {
    if (!activityType) return "activity-type-default";

    const type = String(activityType).toLowerCase();

    if (type.includes("plant") || type.includes("sow")) return "activity-type-planting";
    if (type.includes("fertil") || type.includes("pest") || type.includes("irrig")) return "activity-type-care";
    if (type.includes("harvest")) return "activity-type-harvest";
    if (type.includes("transport")) return "activity-type-transport";

    return "activity-type-default";
}

// ── Formatting Helpers ────────────────────────────────────

function formatFileSize(bytes) {
    if (bytes == null) return "\u2014";
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
    return (bytes / (1024 * 1024)).toFixed(1) + " MB";
}

function formatDate(value) {
    if (!value) return "\u2014";
    const [year, month, day] = value.split("-");
    return day + "/" + month + "/" + year;
}

function formatDateTime(value) {
    if (!value) return "\u2014";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return date.toLocaleString("vi-VN");
}

function escapeHtml(value) {
    var amp = "&" + "amp;";
    var lt = "&" + "lt;";
    var gt = "&" + "gt;";
    var quot = "&" + "quot;";
    var apos = "&#" + "039;";
    return String(value)
        .replace(/&/g, amp)
        .replace(/</g, lt)
        .replace(/>/g, gt)
        .replace(/"/g, quot)
        .replace(/'/g, apos);
}

// ── Pagination ────────────────────────────────────────────

function renderPagination(pageData) {
    const container = document.getElementById("paginationContainer");
    const buttons = document.getElementById("paginationButtons");

    if (!container || !buttons) return;

    container.style.display = "flex";
    buttons.innerHTML = "";

    const currentPage = pageData.page ?? 0;
    const totalPages = pageData.totalPages ?? 0;

    if (totalPages <= 1) return;

    buttons.appendChild(createPageButton("\u2039", currentPage - 1, currentPage === 0));

    for (let index = 0; index < totalPages; index += 1) {
        buttons.appendChild(createPageButton(String(index + 1), index, false, index === currentPage));
    }

    buttons.appendChild(createPageButton("\u203A", currentPage + 1, currentPage >= totalPages - 1));
}

function createPageButton(label, page, disabled, active = false) {
    const button = document.createElement("button");
    button.type = "button";
    button.textContent = label;
    button.disabled = disabled;
    button.className = "pagination-button";
    if (active) button.classList.add("active");

    button.addEventListener("click", async () => {
        const productionLotId = getProductionLotId();
        const pageSize = Number(document.getElementById("pageSizeSelect")?.value || 10);
        await loadFarmLogHistory(productionLotId, page, pageSize);
    });

    return button;
}

// ── Event Delegation for Dynamic Elements ─────────────────

document.addEventListener("click", function (event) {
    // Handle Add Attachment button clicks
    const addBtn = event.target.closest(".add-attachment-btn");
    if (addBtn) {
        const farmLogId = addBtn.getAttribute("data-farm-log-id");
        if (farmLogId) {
            openUploadModal(farmLogId);
        }
        return;
    }

    // Handle Delete Attachment button clicks
    const deleteBtn = event.target.closest(".attachment-delete-btn");
    if (deleteBtn) {
        const attachmentId = deleteBtn.getAttribute("data-attachment-id");
        if (attachmentId) {
            handleDeleteAttachment(attachmentId);
        }
        return;
    }
});

// ── State Helpers ─────────────────────────────────────────

function showLoading() {
    hideAllStates();
    const loadingState = document.getElementById("loadingState");
    if (loadingState) loadingState.style.display = "flex";
}

function showError(message) {
    hideAllStates();
    const errorState = document.getElementById("errorState");
    const errorMessage = document.getElementById("errorMessage");
    if (errorMessage) errorMessage.textContent = message;
    if (errorState) errorState.style.display = "flex";
}

function hideAllStates() {
    const elementIds = ["loadingState", "errorState", "unauthorizedState", "mainContent"];
    elementIds.forEach(function (elementId) {
        const element = document.getElementById(elementId);
        if (element) element.style.display = "none";
    });
}

function setText(elementId, value) {
    const element = document.getElementById(elementId);
    if (element) element.textContent = String(value);
}

function handleLogout() {
    clearAuth();
    window.location.href = "/frontend/pages/auth/login.html";
}