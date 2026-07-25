import {
    getFarmLogHistory
} from "../../../services/farm-log.service.js";

import {
    clearAuth,
    getUser
} from "../../../core/storage.js";

document.addEventListener(
    "DOMContentLoaded",
    initPage
);

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

async function initPage() {
    setupSidebarByRole();
    bindEvents();

    function renderCurrentUser() {
    const storedUser =
        localStorage.getItem("currentUser");

    if (!storedUser) {
        console.error(
            "Kh\u00f4ng t\u00ecm th\u1ea5y currentUser trong localStorage"
        );
        return;
    }

    let currentUser;

    try {
        currentUser =
            JSON.parse(storedUser);

        if (typeof currentUser === "string") {
            currentUser =
                JSON.parse(currentUser);
        }
    } catch (error) {
        console.error(
            "Kh\u00f4ng th\u1ec3 parse currentUser:",
            error
        );
        return;
    }

    setText(
        "headerUserRole",
        currentUser.roleCode ||
            currentUser.roleName ||
            "\u2014"
    );

    setText(
        "headerUserName",
        currentUser.fullName ||
            currentUser.username ||
            "\u2014"
    );

    setText(
        "headerUserOrg",
        currentUser.organizationName ||
            currentUser.organizationCode ||
            "\u2014"
    );

    setText(
        "sidebarUserName",
        currentUser.fullName ||
            currentUser.username ||
            "\u2014"
    );

    setText(
        "sidebarUserOrg",
        currentUser.organizationName ||
            currentUser.organizationCode ||
            "\u2014"
    );

    const avatar =
        document.getElementById(
            "sidebarUserAvatar"
        );

    if (avatar) {
        const displayName =
            currentUser.fullName ||
            currentUser.username ||
            "N";

        avatar.textContent =
            displayName
                .trim()
                .charAt(0)
                .toUpperCase();
    }
}
    renderCurrentUser();

    const productionLotId =
        getProductionLotId();

    const productionLotName =
        getProductionLotName();

    setText(
        "productionLotName",
        productionLotName || "\u2014"
    );

    if (!productionLotId) {
        showError("Thi\u1ebfu productionLotId.");
        return;
    }

    await loadFarmLogHistory(
        productionLotId,
        0,
        10
    );
}

function bindEvents() {
    const logoutButton =
        document.getElementById(
            "logoutButton"
        );

    const backButton =
        document.getElementById(
            "backButton"
        );

    const retryButton =
        document.getElementById(
            "retryButton"
        );

    if (logoutButton) {
        logoutButton.addEventListener(
            "click",
            handleLogout
        );
    }

    if (backButton) {
        backButton.addEventListener(
            "click",
            () => {
                window.location.href =
                    "../production-lots/index.html";
            }
        );
    }

    if (retryButton) {
        retryButton.addEventListener(
            "click",
            () => {
                window.location.reload();
            }
        );
    }
}

function renderCurrentUser() {
    let currentUser = null;

    try {
        currentUser = JSON.parse(
            localStorage.getItem("currentUser")
        );
    } catch (error) {
        console.error(
            "Kh\u00f4ng th\u1ec3 \u0111\u1ecdc th\u00f4ng tin ng\u01b0\u1eddi d\u00f9ng:",
            error
        );
    }

    if (!currentUser) {
        return;
    }

    setText(
        "headerUserRole",
        currentUser.roleCode ||
            currentUser.roleName ||
            ""
    );

    setText(
        "headerUserName",
        currentUser.fullName ||
            currentUser.username ||
            "Ng\u01b0\u1eddi d\u00f9ng"
    );

    setText(
        "headerUserOrg",
        currentUser.organizationName ||
            currentUser.organizationCode ||
            ""
    );

    setText(
        "sidebarUserName",
        currentUser.fullName ||
            currentUser.username ||
            "Ng\u01b0\u1eddi d\u00f9ng"
    );

    setText(
        "sidebarUserOrg",
        currentUser.organizationName ||
            currentUser.organizationCode ||
            ""
    );

    const avatar =
        document.getElementById(
            "sidebarUserAvatar"
        );

    if (avatar) {
        avatar.textContent =
            (
                currentUser.fullName ||
                currentUser.username ||
                "N"
            )
                .trim()
                .charAt(0)
                .toUpperCase();
    }
}

function getProductionLotId() {
    const params =
        new URLSearchParams(
            window.location.search
        );

    return params.get(
        "productionLotId"
    );
}

function getProductionLotName() {
    const params =
        new URLSearchParams(
            window.location.search
        );

    return params.get(
        "productionLotName"
    );
}

async function loadFarmLogHistory(
    productionLotId,
    page,
    size
) {
    showLoading();

    try {
        const response =
            await getFarmLogHistory(
                productionLotId,
                page,
                size
            );

        const pageData =
            response?.data;

        if (!pageData) {
            throw new Error(
                "D\u1eef li\u1ec7u tr\u1ea3 v\u1ec1 kh\u00f4ng h\u1ee3p l\u1ec7."
            );
        }

        renderPage(pageData);
    } catch (error) {
        console.error(error);

        showError(
            error.message ||
            "Kh\u00f4ng th\u1ec3 t\u1ea3i l\u1ecbch s\u1eed nh\u1eadt k\u00fd."
        );
    }
}

function renderPage(pageData) {
    hideAllStates();

    const items =
        Array.isArray(pageData.items)
            ? pageData.items
            : [];

    updateSummary(pageData);

    const mainContent =
        document.getElementById(
            "mainContent"
        );

    if (mainContent) {
        mainContent.style.display =
            "block";
    }

    if (items.length === 0) {
        showEmptyState();

        return;
    }

    renderTimeline(items);
    renderPagination(pageData);
}

function updateSummary(pageData) {
    const totalElements =
        pageData.totalElements ?? 0;

    setText(
        "totalElements",
        totalElements
    );

    setText(
        "activityCount",
        totalElements
    );

    setText(
        "paginationTotalElements",
        totalElements
    );

    setText(
        "currentPageText",
        (pageData.page ?? 0) + 1
    );

    setText(
        "totalPagesText",
        pageData.totalPages ?? 0
    );
}

function showEmptyState() {
    const emptyState =
        document.getElementById(
            "emptyState"
        );

    const historyTimeline =
        document.getElementById(
            "historyTimeline"
        );

    const paginationContainer =
        document.getElementById(
            "paginationContainer"
        );

    if (emptyState) {
        emptyState.style.display =
            "flex";
    }

    if (historyTimeline) {
        historyTimeline.style.display =
            "none";
    }

    if (paginationContainer) {
        paginationContainer.style.display =
            "none";
    }
}

function renderTimeline(items) {
    const timeline =
        document.getElementById(
            "historyTimeline"
        );

    const emptyState =
        document.getElementById(
            "emptyState"
        );

    if (!timeline) {
        return;
    }

    timeline.innerHTML =
        items
            .map(createTimelineCard)
            .join("");

    timeline.style.display =
        "block";

    if (emptyState) {
        emptyState.style.display =
            "none";
    }
}

function createTimelineCard(item) {
    const attachments = Array.isArray(item.attachments) ? item.attachments : [];
    const attachmentCount = attachments.length;

    let attachmentsHtml = "";
    if (attachmentCount > 0) {
        attachmentsHtml = "\n            <div class=\"farm-log-attachments\">\n                <div class=\"attachments-header\" onclick=\"this.parentElement.classList.toggle('attachments-expanded')\">\n                    <span class=\"attachments-toggle\">\u25B6</span>\n                    <span class=\"attachments-label\">Attachments (" + attachmentCount + ")</span>\n                </div>\n                <div class=\"attachments-list\">\n                    " + attachments.map(function(att) {
                        return "\n                            <div class=\"attachment-item\">\n                                <span class=\"attachment-icon\">\uD83D\uDCCE</span>\n                                <div class=\"attachment-info\">\n                                    <span class=\"attachment-filename\">" + escapeHtml(att.fileName || "\u2014") + "</span>\n                                    " + (att.description ? '<span class="attachment-description">' + escapeHtml(att.description) + '</span>' : '') + "\n                                    <span class=\"attachment-meta\">\n                                        " + (att.fileType ? escapeHtml(att.fileType) : "") + "\n                                        " + (att.fileSize ? ' | ' + formatFileSize(att.fileSize) : "") + "\n                                        " + (att.uploadedAt ? ' | ' + formatDateTime(att.uploadedAt) : "") + "\n                                    </span>\n                                </div>\n                                " + (att.fileUrl ? '<a href="' + escapeHtml(att.fileUrl) + '" class="attachment-view-btn" target="_blank" rel="noopener">View</a>' : '') + "\n                            </div>\n                        ";
                    }).join("") + "\n                </div>\n            </div>\n        ";
    }

    return "\n        <div class=\"timeline-card\">\n            <div class=\"timeline-card-header\">\n                <span class=\"timeline-date\">" + formatDate(item.executedDate) + "</span>\n                <span class=\"timeline-activity-type " + getActivityTypeClass(item.activityType) + "\">" + escapeHtml(item.activityType ?? "\u2014") + "</span>\n            </div>\n\n            <div class=\"timeline-card-body\">\n                " + (item.material ? "\n                    <div class=\"timeline-field\">\n                        <span class=\"timeline-field-label\">Material:</span>\n                        <span class=\"timeline-field-value\">" + escapeHtml(item.material) + "</span>\n                    </div>\n                " : "") + "\n\n                " + (item.quantity != null ? "\n                    <div class=\"timeline-field\">\n                        <span class=\"timeline-field-label\">Quantity:</span>\n                        <span class=\"timeline-field-value\">" + item.quantity + " " + escapeHtml(item.unit ?? "") + "</span>\n                    </div>\n                " : "") + "\n\n                " + (item.notes ? "\n                    <div class=\"timeline-field\">\n                        <span class=\"timeline-field-label\">Notes:</span>\n                        <span class=\"timeline-field-value\">" + escapeHtml(item.notes) + "</span>\n                    </div>\n                " : "") + "\n\n                <div class=\"timeline-field\">\n                    <span class=\"timeline-field-label\">Created by:</span>\n                    <span class=\"timeline-field-value\">" + escapeHtml(item.createdByName ?? "\u2014") + "</span>\n                </div>\n\n                <div class=\"timeline-field\">\n                    <span class=\"timeline-field-label\">Created at:</span>\n                    <span class=\"timeline-field-value\">" + formatDateTime(item.createdAt) + "</span>\n                </div>\n            </div>\n\n            " + attachmentsHtml + "\n        </div>\n    ";
}

function getActivityTypeClass(activityType) {
    if (!activityType) return "activity-type-default";

    const type = String(activityType).toLowerCase();

    if (type.includes("plant") || type.includes("sow")) return "activity-type-planting";
    if (type.includes("fertil") || type.includes("pest") || type.includes("irrig")) return "activity-type-care";
    if (type.includes("harvest")) return "activity-type-harvest";
    if (type.includes("transport")) return "activity-type-transport";

    return "activity-type-default";
}

function formatFileSize(bytes) {
    if (bytes == null) return "\u2014";
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
    return (bytes / (1024 * 1024)).toFixed(1) + " MB";
}

function renderPagination(pageData) {
    const container =
        document.getElementById(
            "paginationContainer"
        );

    const buttons =
        document.getElementById(
            "paginationButtons"
        );

    if (!container || !buttons) {
        return;
    }

    container.style.display =
        "flex";

    buttons.innerHTML = "";

    const currentPage =
        pageData.page ?? 0;

    const totalPages =
        pageData.totalPages ?? 0;

    if (totalPages <= 1) {
        return;
    }

    buttons.appendChild(
        createPageButton(
            "\u2039",
            currentPage - 1,
            currentPage === 0
        )
    );

    for (
        let index = 0;
        index < totalPages;
        index += 1
    ) {
        buttons.appendChild(
            createPageButton(
                String(index + 1),
                index,
                false,
                index === currentPage
            )
        );
    }

    buttons.appendChild(
        createPageButton(
            "\u203A",
            currentPage + 1,
            currentPage >= totalPages - 1
        )
    );
}

function createPageButton(
    label,
    page,
    disabled,
    active = false
) {
    const button =
        document.createElement(
            "button"
        );

    button.type = "button";
    button.textContent = label;
    button.disabled = disabled;
    button.className =
        "pagination-button";

    if (active) {
        button.classList.add(
            "active"
        );
    }

    button.addEventListener(
        "click",
        async () => {
            const productionLotId =
                getProductionLotId();

            const pageSize =
                Number(
                    document
                        .getElementById(
                            "pageSizeSelect"
                        )
                        ?.value || 10
                );

            await loadFarmLogHistory(
                productionLotId,
                page,
                pageSize
            );
        }
    );

    return button;
}

function showLoading() {
    hideAllStates();

    const loadingState =
        document.getElementById(
            "loadingState"
        );

    if (loadingState) {
        loadingState.style.display =
            "flex";
    }
}

function showError(message) {
    hideAllStates();

    const errorState =
        document.getElementById(
            "errorState"
        );

    const errorMessage =
        document.getElementById(
            "errorMessage"
        );

    if (errorMessage) {
        errorMessage.textContent =
            message;
    }

    if (errorState) {
        errorState.style.display =
            "flex";
    }
}

function hideAllStates() {
    const elementIds = [
        "loadingState",
        "errorState",
        "unauthorizedState",
        "mainContent"
    ];

    elementIds.forEach(
        (elementId) => {
            const element =
                document.getElementById(
                    elementId
                );

            if (element) {
                element.style.display =
                    "none";
            }
        }
    );
}

function setText(
    elementId,
    value
) {
    const element =
        document.getElementById(
            elementId
        );

    if (element) {
        element.textContent =
            String(value);
    }
}

function formatDate(value) {
    if (!value) {
        return "\u2014";
    }

    const [
        year,
        month,
        day
    ] = value.split("-");

    return day + "/" + month + "/" + year;
}

function formatDateTime(value) {
    if (!value) {
        return "\u2014";
    }

    const date =
        new Date(value);

    if (
        Number.isNaN(
            date.getTime()
        )
    ) {
        return value;
    }

    return date.toLocaleString(
        "vi-VN"
    );
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

function handleLogout() {
    clearAuth();

    window.location.href =
        "/frontend/pages/auth/login.html";
}