import {
    requireAuth,
    setupLogout
} from "../../../core/auth-guard.js";

import {
    getUser
} from "../../../core/storage.js";

import {
    getShipments
} from "../../../services/shipment.service.js";

/* =====================================================
   AUTHENTICATION
===================================================== */

if (!requireAuth()) {
    // requireAuth đã tự chuyển về trang đăng nhập.
}

const user = getUser();

function setupSidebarByRole() {
    if (!user || !user.roleCode) {
        return;
    }

    const menuIds = [
        "dashboardMenu",
        "farmAreasMenu",
        "organizationProfileMenu"
    ];

    if (user.roleCode === "VT-03") {
        menuIds.forEach(function (menuId) {
            const menuItem = document.getElementById(menuId);
            if (menuItem) {
                menuItem.style.display = "none";
            }
        });
    }
}

setupSidebarByRole();

if (!user || !user.roleCode) {
    window.location.href =
        "/frontend/pages/auth/login.html";
    throw new Error("User not authenticated.");
}

const roleCode = user.roleCode;

const allowedRoles = ["VT-02"];

if (!allowedRoles.includes(roleCode)) {
    const loadingElement = document.getElementById("loadingState");
    const unauthorizedElement = document.getElementById("unauthorizedState");
    const mainElement = document.getElementById("mainContent");

    if (loadingElement) loadingElement.style.display = "none";
    if (unauthorizedElement) unauthorizedElement.style.display = "flex";
    if (mainElement) mainElement.style.display = "none";

    throw new Error("Access denied: user does not have permission to access this page.");
}

/* =====================================================
   USER INFORMATION
===================================================== */

function populateUserInfo() {
    const sidebarName = document.getElementById("sidebarUserName");
    const sidebarOrg = document.getElementById("sidebarUserOrg");
    const sidebarAvatar = document.getElementById("sidebarUserAvatar");
    const headerName = document.getElementById("headerUserName");
    const headerOrg = document.getElementById("headerUserOrg");
    const headerRole = document.getElementById("headerUserRole");

    if (sidebarName) sidebarName.textContent = user.fullName || user.username || "—";
    if (sidebarOrg) sidebarOrg.textContent = user.organizationName || "—";
    if (sidebarAvatar) {
        const displayName = user.fullName || user.username || "?";
        sidebarAvatar.textContent = displayName.charAt(0).toUpperCase();
    }
    if (headerName) headerName.textContent = user.fullName || user.username || "—";
    if (headerOrg) headerOrg.textContent = user.organizationName || "—";
    if (headerRole) headerRole.textContent = user.roleCode || "—";
}

populateUserInfo();

/* =====================================================
   DOM REFERENCES
===================================================== */

const loadingState = document.getElementById("loadingState");
const errorState = document.getElementById("errorState");
const errorMessage = document.getElementById("errorMessage");
const retryButton = document.getElementById("retryButton");
const mainContent = document.getElementById("mainContent");
const emptyState = document.getElementById("emptyState");
const emptyStateText = document.getElementById("emptyStateText");
const shipmentsTable = document.getElementById("shipmentsTable");
const shipmentsTableBody = document.getElementById("shipmentsTableBody");

/* =====================================================
   PAGE STATE
===================================================== */

let shipments = [];

/* =====================================================
   GET URL PARAMETERS
===================================================== */

function getUrlParameter(name) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(name);
}

const productionLotIdFilter = getUrlParameter("productionLotId");
const productionLotNameFilter = getUrlParameter("productionLotName");

/* =====================================================
   FORMAT HELPERS
===================================================== */

function getStatusBadgeClass(status) {
    if (!status) return "status-badge-draft";

    const normalizedStatus = String(status).trim().toLowerCase();

    if (normalizedStatus === "draft") return "status-badge-draft";
    if (normalizedStatus === "code_printed" || normalizedStatus === "codeprinted") return "status-badge-packaged";
    if (normalizedStatus === "activated") return "status-badge-approved";

    return "status-badge-draft";
}

function formatDate(dateStr) {
    if (!dateStr) return "—";

    const date = new Date(dateStr);
    if (Number.isNaN(date.getTime())) return dateStr;
    return date.toLocaleDateString("vi-VN");
}

/* =====================================================
   EXTRACT SHIPMENTS FROM API RESPONSE
===================================================== */

function extractShipments(response) {
    if (!response) return [];

    if (Array.isArray(response.data)) {
        return response.data;
    }

    if (response.data && Array.isArray(response.data.items)) {
        return response.data.items;
    }

    if (Array.isArray(response)) {
        return response;
    }

    return [];
}

/* =====================================================
   3-DOT CONTEXTUAL MENU
===================================================== */

let activeMenu = null;

function closeActiveMenu() {
    if (activeMenu) {
        activeMenu.remove();
        activeMenu = null;
    }
}

document.addEventListener("click", function () {
    closeActiveMenu();
});

function createThreeDotMenu(shipment) {
    const container = document.createElement("div");
    container.className = "three-dot-menu-container";

    const dotButton = document.createElement("button");
    dotButton.type = "button";
    dotButton.className = "three-dot-button";
    dotButton.dataset.id = shipment.id;
    dotButton.innerHTML = "⋮";
    dotButton.title = "Actions";

    dotButton.addEventListener("click", function (event) {
        event.stopPropagation();
        closeActiveMenu();

        const dropdown = document.createElement("div");
        dropdown.className = "three-dot-dropdown";
        activeMenu = dropdown;

        var menuItems = [];

        if (shipment.traceCodes && shipment.traceCodes.length > 0) {
            menuItems = [
                { label: "View Trace Codes", action: "view-trace-codes" }
            ];
        }

        menuItems.forEach(function (item) {
            const menuItem = document.createElement("button");
            menuItem.type = "button";
            menuItem.className = "three-dot-menu-item";
            menuItem.textContent = item.label;
            menuItem.dataset.action = item.action;
            menuItem.dataset.id = shipment.id;

            menuItem.addEventListener("click", function (event) {
                event.stopPropagation();
                closeActiveMenu();
                handleMenuAction(item.action, shipment);
            });

            dropdown.appendChild(menuItem);
        });

        // Position the dropdown
        const rect = dotButton.getBoundingClientRect();
        dropdown.style.position = "fixed";
        dropdown.style.top = (rect.bottom + 4) + "px";
        dropdown.style.left = Math.max(8, rect.left - 100 + rect.width / 2) + "px";

        document.body.appendChild(dropdown);
    });

    container.appendChild(dotButton);
    return container;
}

function handleMenuAction(action, shipment) {
    switch (action) {
        case "view-trace-codes":
            window.location.href = "/frontend/pages/cooperative/shipment/tracecode.html?id=" + encodeURIComponent(shipment.id);
            break;
    }
}

/* =====================================================
   RENDER SHIPMENTS TABLE
===================================================== */

function renderShipments(shipmentsList) {
    if (!Array.isArray(shipmentsList) || shipmentsList.length === 0) {
        if (emptyState) emptyState.style.display = "flex";
        if (shipmentsTable) shipmentsTable.style.display = "none";
        if (shipmentsTableBody) shipmentsTableBody.innerHTML = "";
        return;
    }

    if (emptyState) emptyState.style.display = "none";
    if (shipmentsTable) shipmentsTable.style.display = "table";

    if (!shipmentsTableBody) return;

    shipmentsTableBody.innerHTML = "";

    shipmentsList.forEach(function (shipment) {
        const row = document.createElement("tr");

        const nameCell = document.createElement("td");
        nameCell.textContent = shipment.name || "—";

        const lotCell = document.createElement("td");
        lotCell.textContent = shipment.productionLotName || "—";

        const quantityCell = document.createElement("td");
        quantityCell.textContent = shipment.totalQuantity != null ? String(shipment.totalQuantity) : "—";

        const statusCell = document.createElement("td");
        const statusBadge = document.createElement("span");
        const normalizedStatus = String(shipment.status || "DRAFT").trim().toUpperCase();
        statusBadge.className = "status-badge " + getStatusBadgeClass(normalizedStatus);
        statusBadge.textContent = normalizedStatus;
        statusCell.appendChild(statusBadge);

        const createdByCell = document.createElement("td");
        createdByCell.textContent = shipment.createdByName || "—";

        const createdDateCell = document.createElement("td");
        createdDateCell.textContent = formatDate(shipment.createdAt);

        const actionCell = document.createElement("td");
        actionCell.className = "production-lot-actions";

        actionCell.appendChild(createThreeDotMenu(shipment));

        row.appendChild(nameCell);
        row.appendChild(lotCell);
        row.appendChild(quantityCell);
        row.appendChild(statusCell);
        row.appendChild(createdByCell);
        row.appendChild(createdDateCell);
        row.appendChild(actionCell);

        shipmentsTableBody.appendChild(row);
    });
}

/* =====================================================
   LOAD SHIPMENTS
===================================================== */

async function loadShipments() {
    if (loadingState) loadingState.style.display = "flex";
    if (errorState) errorState.style.display = "none";
    if (mainContent) mainContent.style.display = "none";

    try {
        const response = await getShipments();

        if (response && response.success === false) {
            throw new Error(response.message || "Không thể tải danh sách lô hàng.");
        }

        shipments = extractShipments(response);

        // If there's a productionLotId filter, apply it
        if (productionLotIdFilter) {
            shipments = shipments.filter(function (s) {
                return String(s.productionLotId) === String(productionLotIdFilter);
            });

            if (emptyStateText) {
                emptyStateText.textContent = "No shipments have been created for this production lot.";
            }
        } else {
            if (emptyStateText) {
                emptyStateText.textContent = "No shipments available for your organization.";
            }
        }

        if (loadingState) loadingState.style.display = "none";
        if (mainContent) mainContent.style.display = "block";

        renderShipments(shipments);
    } catch (error) {
        console.error("Load shipments error:", error);

        if (loadingState) loadingState.style.display = "none";
        if (mainContent) mainContent.style.display = "none";

        let message = error.message || "Đã xảy ra lỗi khi tải danh sách lô hàng.";
        const normalizedMessage = String(message).toLowerCase();

        if (normalizedMessage.includes("404") || normalizedMessage.includes("not found")) {
            if (mainContent) mainContent.style.display = "block";
            renderShipments([]);
            return;
        }

        if (normalizedMessage.includes("403")) {
            message = "Bạn không có quyền xem danh sách lô hàng.";
        }

        if (errorMessage) errorMessage.textContent = message;
        if (errorState) errorState.style.display = "flex";
    }
}

/* =====================================================
   SEARCH
===================================================== */

const searchInput = document.getElementById("searchShipment");

if (searchInput) {
    searchInput.addEventListener("input", function (event) {
        const keyword = String(event.target.value || "").trim().toLowerCase();

        if (!keyword) {
            renderShipments(shipments);
            return;
        }

        const filteredShipments = shipments.filter(function (s) {
            const searchableText = [
                s.name,
                s.productionLotName,
                s.status,
                s.createdByName
            ]
                .filter(function (value) {
                    return value !== null && value !== undefined;
                })
                .join(" ")
                .toLowerCase();

            return searchableText.includes(keyword);
        });

        renderShipments(filteredShipments);
    });
}

/* =====================================================
   RETRY EVENT
===================================================== */

if (retryButton) {
    retryButton.addEventListener("click", loadShipments);
}

/* =====================================================
   LOGOUT
===================================================== */

setupLogout();

/* =====================================================
   INITIALIZE PAGE
===================================================== */

loadShipments();