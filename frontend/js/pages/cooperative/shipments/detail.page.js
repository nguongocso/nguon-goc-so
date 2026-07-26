import {
    requireAuth,
    setupLogout
} from "../../../core/auth-guard.js";

import {
    getUser
} from "../../../core/storage.js";

import {
    getShipmentById
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
const unauthorizedState = document.getElementById("unauthorizedState");

const detailName = document.getElementById("detailName");
const detailId = document.getElementById("detailId");
const detailProductionLot = document.getElementById("detailProductionLot");
const detailQuantity = document.getElementById("detailQuantity");
const detailStatus = document.getElementById("detailStatus");
const detailCreatedBy = document.getElementById("detailCreatedBy");
const detailCreatedAt = document.getElementById("detailCreatedAt");

const emptyTraceCodes = document.getElementById("emptyTraceCodes");
const traceCodesTable = document.getElementById("traceCodesTable");
const traceCodesTableBody = document.getElementById("traceCodesTableBody");
const pagination = document.getElementById("pagination");

const searchTraceCode = document.getElementById("searchTraceCode");

/* =====================================================
   GET URL PARAMETERS
===================================================== */

function getUrlParameter(name) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(name);
}

const shipmentId = getUrlParameter("id");

/* =====================================================
   FORMAT HELPERS
===================================================== */

function getStatusBadgeClass(status) {
    if (!status) return "status-badge-draft";

    const normalizedStatus = String(status).trim().toLowerCase();

    if (normalizedStatus === "draft") return "status-badge-draft";
    if (normalizedStatus === "code_printed" || normalizedStatus === "codeprinted") return "status-badge-packaged";
    if (normalizedStatus === "activated") return "status-badge-approved";
    if (normalizedStatus === "inactive") return "status-badge-draft";
    if (normalizedStatus === "active") return "status-badge-approved";

    return "status-badge-draft";
}

function formatDate(dateStr) {
    if (!dateStr) return "—";

    const date = new Date(dateStr);
    if (Number.isNaN(date.getTime())) return dateStr;
    return date.toLocaleDateString("vi-VN");
}

/* =====================================================
   PAGE STATE
===================================================== */

let currentShipment = null;
let allTraceCodes = [];
let filteredTraceCodes = [];
const API_FILE_BASE_URL = "http://localhost:8080";
const PAGE_SIZE = 10;
let currentPage = 1;

/* =====================================================
   EXTRACT SHIPMENT FROM API RESPONSE
===================================================== */

function extractShipment(response) {
    if (!response) return null;

    if (response.data) {
        return response.data;
    }

    return response;
}

/* =====================================================
   RENDER SHIPMENT DETAILS
===================================================== */

function renderShipmentDetails(shipment) {
    if (!shipment) return;

    detailName.textContent = shipment.name || "—";
    detailId.textContent = shipment.id || "—";
    detailProductionLot.textContent = shipment.productionLotName || "—";
    detailQuantity.textContent = shipment.totalQuantity != null ? String(shipment.totalQuantity) : "—";

    const normalizedStatus = String(shipment.status || "DRAFT").trim().toUpperCase();
    detailStatus.textContent = normalizedStatus;
    detailStatus.className = "status-badge " + getStatusBadgeClass(normalizedStatus);

    detailCreatedBy.textContent = shipment.createdByName || "—";
    detailCreatedAt.textContent = formatDate(shipment.createdAt);
}

/* =====================================================
   RENDER TRACE CODES TABLE
===================================================== */

function renderTraceCodes(traceCodes) {
    if (!Array.isArray(traceCodes) || traceCodes.length === 0) {
        if (emptyTraceCodes) emptyTraceCodes.style.display = "flex";
        if (traceCodesTable) traceCodesTable.style.display = "none";
        if (traceCodesTableBody) traceCodesTableBody.innerHTML = "";
        if (pagination) pagination.innerHTML = "";
        return;
    }

    if (emptyTraceCodes) emptyTraceCodes.style.display = "none";
    if (traceCodesTable) traceCodesTable.style.display = "table";

    if (!traceCodesTableBody) return;

    const totalPages = Math.ceil(traceCodes.length / PAGE_SIZE);
    currentPage = Math.min(currentPage, totalPages) || 1;

    const start = (currentPage - 1) * PAGE_SIZE;
    const pageCodes = traceCodes.slice(start, start + PAGE_SIZE);

    traceCodesTableBody.innerHTML = "";

    pageCodes.forEach(function (tc) {
        const row = document.createElement("tr");

        // Trace Code Value
        const codeCell = document.createElement("td");
        codeCell.textContent = tc.codeValue || "—";

        // Status
        const statusCell = document.createElement("td");
        const statusBadge = document.createElement("span");
        const normStatus = String(tc.status || "INACTIVE").trim().toUpperCase();
        statusBadge.className = "status-badge " + getStatusBadgeClass(normStatus);
        statusBadge.textContent = normStatus;
        statusCell.appendChild(statusBadge);

        // QR Image
        const qrCell = document.createElement("td");
        if (tc.qrImage) {
            const qrImg = document.createElement("img");
            const qrUrl = tc.qrImage.startsWith("http") ? tc.qrImage : API_FILE_BASE_URL + tc.qrImage;
            qrImg.src = qrUrl;
            qrImg.alt = "QR Code";
            qrImg.style.width = "48px";
            qrImg.style.height = "48px";
            qrImg.style.objectFit = "contain";
            qrImg.onerror = function () {
                qrImg.src = "/frontend/assets/images/qr-placeholder.png";
            };
            qrCell.appendChild(qrImg);
        } else {
            qrCell.textContent = "—";
        }

        row.appendChild(codeCell);
        row.appendChild(statusCell);
        row.appendChild(qrCell);

        traceCodesTableBody.appendChild(row);
    });

    // Render pagination
    renderPagination(totalPages);
}

function renderPagination(totalPages) {
    if (!pagination) return;

    pagination.innerHTML = "";

    if (totalPages <= 1) {
        pagination.style.display = "none";
        return;
    }

    pagination.style.display = "flex";

    var prevButton = document.createElement("button");
    prevButton.type = "button";
    prevButton.className = "btn btn-secondary";
    prevButton.textContent = "<";
    prevButton.style.padding = "4px 12px";
    prevButton.disabled = currentPage === 1;
    prevButton.addEventListener("click", function () {
        currentPage--;
        renderTraceCodes(filteredTraceCodes);
    });
    pagination.appendChild(prevButton);

    for (var i = 1; i <= totalPages; i++) {
        var pageButton = document.createElement("button");
        pageButton.type = "button";
        pageButton.textContent = String(i);
        pageButton.style.padding = "4px 12px";
        if (i === currentPage) {
            pageButton.className = "btn btn-primary";
        } else {
            pageButton.className = "btn btn-secondary";
        }
        pageButton.addEventListener("click", function (page) {
            return function () {
                currentPage = page;
                renderTraceCodes(filteredTraceCodes);
            };
        }(i));
        pagination.appendChild(pageButton);
    }

    var nextButton = document.createElement("button");
    nextButton.type = "button";
    nextButton.className = "btn btn-secondary";
    nextButton.textContent = ">";
    nextButton.style.padding = "4px 12px";
    nextButton.disabled = currentPage === totalPages;
    nextButton.addEventListener("click", function () {
        currentPage++;
        renderTraceCodes(filteredTraceCodes);
    });
    pagination.appendChild(nextButton);
}

/* =====================================================
   LOAD SHIPMENT DETAILS
===================================================== */

async function loadShipmentDetail() {
    if (!shipmentId) {
        if (loadingState) loadingState.style.display = "none";
        if (mainContent) mainContent.style.display = "none";
        if (errorState) {
            errorState.style.display = "flex";
            errorMessage.textContent = "No shipment ID provided.";
        }
        return;
    }

    if (loadingState) loadingState.style.display = "flex";
    if (errorState) errorState.style.display = "none";
    if (mainContent) mainContent.style.display = "none";

    try {
        const response = await getShipmentById(shipmentId);

        if (response && response.success === false) {
            throw new Error(response.message || "Không thể tải thông tin lô hàng.");
        }

        currentShipment = extractShipment(response);

        if (!currentShipment) {
            throw new Error("Không tìm thấy thông tin lô hàng.");
        }

        if (loadingState) loadingState.style.display = "none";
        if (mainContent) mainContent.style.display = "block";

        renderShipmentDetails(currentShipment);

        allTraceCodes = currentShipment.traceCodes || [];
        filteredTraceCodes = allTraceCodes.slice();
        currentPage = 1;
        renderTraceCodes(filteredTraceCodes);

    } catch (error) {
        console.error("Load shipment detail error:", error);

        if (loadingState) loadingState.style.display = "none";
        if (mainContent) mainContent.style.display = "none";

        let message = error.message || "Đã xảy ra lỗi khi tải thông tin lô hàng.";
        const normalizedMessage = String(message).toLowerCase();

        if (normalizedMessage.includes("404") || normalizedMessage.includes("not found") || normalizedMessage.includes("không tìm thấy")) {
            message = "Shipment not found.";
        }

        if (normalizedMessage.includes("403")) {
            message = "Bạn không có quyền xem lô hàng này.";
        }

        if (errorMessage) errorMessage.textContent = message;
        if (errorState) errorState.style.display = "flex";
    }
}

/* =====================================================
   SEARCH TRACE CODES
===================================================== */

if (searchTraceCode) {
    searchTraceCode.addEventListener("input", function (event) {
        const keyword = String(event.target.value || "").trim().toLowerCase();

        if (!keyword) {
            filteredTraceCodes = allTraceCodes.slice();
        } else {
            filteredTraceCodes = allTraceCodes.filter(function (tc) {
                const searchable = [
                    tc.codeValue,
                    tc.status
                ]
                    .filter(function (v) { return v !== null && v !== undefined; })
                    .join(" ")
                    .toLowerCase();

                return searchable.includes(keyword);
            });
        }

        currentPage = 1;
        renderTraceCodes(filteredTraceCodes);
    });
}

/* =====================================================
   RETRY EVENT
===================================================== */

if (retryButton) {
    retryButton.addEventListener("click", loadShipmentDetail);
}

/* =====================================================
   LOGOUT
===================================================== */

setupLogout();

/* =====================================================
   INITIALIZE PAGE
===================================================== */

loadShipmentDetail();