import {
    requireAuth,
    setupLogout
} from "../../../core/auth-guard.js";

import {
    getUser
} from "../../../core/storage.js";

/* =====================================================
   AUTHENTICATION
===================================================== */

if (!requireAuth()) {
    // requireAuth tự chuyển về trang đăng nhập.
}

const user = getUser();

if (!user || !user.roleCode) {
    window.location.href = "/frontend/pages/auth/login.html";
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

    throw new Error("Access denied.");
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
   GET SHIPMENT DATA FROM URL
===================================================== */

const urlParams = new URLSearchParams(window.location.search);
const encodedShipmentData = urlParams.get("shipment");

let shipmentData = null;

if (encodedShipmentData) {
    try {
        shipmentData = JSON.parse(decodeURIComponent(encodedShipmentData));
    } catch (e) {
        console.error("Failed to parse shipment data from URL:", e);
    }
}

/* =====================================================
   DOM REFERENCES
===================================================== */

const loadingState = document.getElementById("loadingState");
const mainContent = document.getElementById("mainContent");
const errorState = document.getElementById("errorState");

const shipmentIdEl = document.getElementById("shipmentId");
const shipmentNameEl = document.getElementById("shipmentName");
const shipmentProductionLotEl = document.getElementById("shipmentProductionLot");
const shipmentQuantityEl = document.getElementById("shipmentQuantity");
const shipmentStatusEl = document.getElementById("shipmentStatus");
const traceCodesSection = document.getElementById("traceCodesSection");
const traceCodesList = document.getElementById("traceCodesList");
const noTraceCodesMessage = document.getElementById("noTraceCodesMessage");
const viewShipmentBtn = document.getElementById("viewShipmentBtn");

/* =====================================================
   RENDER SHIPMENT DATA
===================================================== */

function displayShipmentData() {
    if (!shipmentData) {
        if (loadingState) loadingState.style.display = "none";
        if (mainContent) mainContent.style.display = "none";
        if (errorState) errorState.style.display = "flex";

        const errorMsgEl = document.getElementById("errorMessage");
        if (errorMsgEl) {
            errorMsgEl.textContent = "Shipment data not available. The shipment may have been created successfully, but details could not be loaded.";
        }
        return;
    }

    if (loadingState) loadingState.style.display = "none";
    if (mainContent) mainContent.style.display = "block";

    // Fill shipment details
    if (shipmentIdEl) shipmentIdEl.textContent = shipmentData.id || "—";
    if (shipmentNameEl) shipmentNameEl.textContent = shipmentData.name || "—";
    if (shipmentProductionLotEl) shipmentProductionLotEl.textContent = shipmentData.productionLotName || "—";
    if (shipmentQuantityEl) shipmentQuantityEl.textContent = shipmentData.totalQuantity != null ? String(shipmentData.totalQuantity) : "—";

    if (shipmentStatusEl) {
        shipmentStatusEl.textContent = shipmentData.status || "—";
        const statusClass = getStatusBadgeClass(shipmentData.status);
        shipmentStatusEl.className = "badge " + statusClass;
    }

    // Display trace codes if available
    const traceCodes = shipmentData.traceCodes;

    if (traceCodes && Array.isArray(traceCodes) && traceCodes.length > 0) {
        if (traceCodesSection) traceCodesSection.style.display = "block";
        if (noTraceCodesMessage) noTraceCodesMessage.style.display = "none";

        if (traceCodesList) {
            traceCodesList.innerHTML = "";
            traceCodes.forEach(function (trace) {
                const badge = document.createElement("span");
                badge.className = "ss-trace-code-badge";
                badge.textContent = trace.codeValue || trace.id || "—";
                traceCodesList.appendChild(badge);
            });
        }

        // Show "View Shipment" button linking to existing tracecode page
        if (viewShipmentBtn) {
            viewShipmentBtn.style.display = "inline-flex";
            viewShipmentBtn.href = `/frontend/pages/cooperative/shipment/index.html?shipmentId=${encodeURIComponent(shipmentData.id || "")}`;
        }
    } else if (traceCodes && Array.isArray(traceCodes) && traceCodes.length === 0) {
        if (traceCodesSection) traceCodesSection.style.display = "none";
        if (noTraceCodesMessage) {
            noTraceCodesMessage.style.display = "block";
            noTraceCodesMessage.textContent = "Shipment created but no trace codes were returned.";
        }
    } else {
        // traceCodes not present in response
        if (traceCodesSection) traceCodesSection.style.display = "none";
        if (noTraceCodesMessage) {
            noTraceCodesMessage.style.display = "block";
            noTraceCodesMessage.textContent = "Shipment created successfully. Trace codes are being processed.";
        }
    }
}

function getStatusBadgeClass(status) {
    if (!status) return "badge-neutral";

    const normalized = String(status).trim().toUpperCase();

    const classMap = {
        "DRAFT": "badge-neutral",
        "CODE_PRINTED": "badge-info",
        "ACTIVATED": "badge-active",
        "ACTIVE": "badge-active",
        "INACTIVE": "badge-inactive",
        "RECALLED": "badge-danger"
    };

    return classMap[normalized] || "badge-neutral";
}

/* =====================================================
   LOGOUT
===================================================== */

setupLogout();

/* =====================================================
   INITIALIZE PAGE
===================================================== */

displayShipmentData();