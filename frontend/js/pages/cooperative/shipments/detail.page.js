import {
    requireAuth,
    setupLogout
} from "../../../core/auth-guard.js";

import {
    getUser
} from "../../../core/storage.js";

import {
    getShipmentById,
    activateShipment
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

// Activation elements
const activateShipmentBtn = document.getElementById("activateShipmentBtn");
const activatedMessage = document.getElementById("activatedMessage");
const activationError = document.getElementById("activationError");

let isActivating = false;

// QR View Modal
const qrViewModal = document.getElementById("qrViewModal");
const qrViewImage = document.getElementById("qrViewImage");
const qrViewCodeValue = document.getElementById("qrViewCodeValue");
const qrViewStatusBadge = document.getElementById("qrViewStatusBadge");
const qrViewDownloadBtn = document.getElementById("qrViewDownloadBtn");
const qrViewPrintBtn = document.getElementById("qrViewPrintBtn");
const qrViewCloseBtn = document.getElementById("qrViewCloseBtn");
const qrViewModalClose = document.getElementById("qrViewModalClose");

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

// 3-dot menu state
let activeMenu = null;

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
   QR IMAGE URL HELPER
===================================================== */

function getQrImageUrl(path) {
    if (!path) return "";
    if (path.startsWith("http://") || path.startsWith("https://")) {
        return path;
    }
    return API_FILE_BASE_URL + path;
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
   3-DOT CONTEXTUAL MENU FOR TRACE CODE ACTIONS
===================================================== */

function closeActiveMenu() {
    if (activeMenu) {
        activeMenu.remove();
        activeMenu = null;
    }
}

document.addEventListener("click", function () {
    closeActiveMenu();
});

function createTraceCodeActionsMenu(tc) {
    const container = document.createElement("div");
    container.className = "three-dot-menu-container";

    const dotButton = document.createElement("button");
    dotButton.type = "button";
    dotButton.className = "three-dot-button";
    dotButton.dataset.id = tc.codeValue || "tc";
    dotButton.innerHTML = "⋮";
    dotButton.title = "Actions";

    dotButton.addEventListener("click", function (event) {
        event.stopPropagation();
        closeActiveMenu();

        const dropdown = document.createElement("div");
        dropdown.className = "three-dot-dropdown";
        activeMenu = dropdown;

        const menuItems = [
            { label: "View QR", action: "view-qr" },
            { label: "Download QR", action: "download-qr" },
            { label: "Print QR", action: "print-qr" }
        ];

        menuItems.forEach(function (item) {
            const menuItem = document.createElement("button");
            menuItem.type = "button";
            menuItem.className = "three-dot-menu-item";
            menuItem.textContent = item.label;
            menuItem.dataset.action = item.action;

            menuItem.addEventListener("click", function (event) {
                event.stopPropagation();
                closeActiveMenu();
                handleTraceCodeAction(item.action, tc);
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

function handleTraceCodeAction(action, tc) {
    switch (action) {
        case "view-qr":
            openQrViewModal(tc);
            break;
        case "download-qr":
            downloadQr(tc);
            break;
        case "print-qr":
            printQr(tc);
            break;
    }
}

/* =====================================================
   QR VIEW MODAL
===================================================== */

function openQrViewModal(tc) {
    if (!tc) return;

    if (!tc.qrImage) {
        alert("QR code image is unavailable.");
        return;
    }

    const qrUrl = getQrImageUrl(tc.qrImage);
    qrViewImage.src = qrUrl;
    qrViewImage.alt = tc.codeValue || "QR Code";
    qrViewImage.onerror = function () {
        qrViewImage.src = "/frontend/assets/images/qr-placeholder.png";
    };

    qrViewCodeValue.textContent = tc.codeValue || "—";

    const normStatus = String(tc.status || "INACTIVE").trim().toUpperCase();
    qrViewStatusBadge.textContent = normStatus;
    qrViewStatusBadge.className = "status-badge " + getStatusBadgeClass(normStatus);

    // Store current trace code reference for download/print from modal
    qrViewDownloadBtn.dataset.tcCodeValue = tc.codeValue || "qr-code";
    qrViewDownloadBtn.dataset.tcQrImage = tc.qrImage || "";
    qrViewPrintBtn.dataset.tcCodeValue = tc.codeValue || "qr-code";
    qrViewPrintBtn.dataset.tcQrImage = tc.qrImage || "";
    qrViewPrintBtn.dataset.tcStatus = tc.status || "";

    qrViewModal.style.display = "flex";
    document.body.classList.add("modal-open");
}

function closeQrViewModal() {
    qrViewModal.style.display = "none";
    document.body.classList.remove("modal-open");
}

// QR View Modal close handlers
if (qrViewModalClose) {
    qrViewModalClose.addEventListener("click", closeQrViewModal);
}

if (qrViewCloseBtn) {
    qrViewCloseBtn.addEventListener("click", closeQrViewModal);
}

if (qrViewModal) {
    qrViewModal.addEventListener("click", function (event) {
        if (event.target === qrViewModal) {
            closeQrViewModal();
        }
    });
}

document.addEventListener("keydown", function (event) {
    if (event.key === "Escape" && qrViewModal && qrViewModal.style.display === "flex") {
        closeQrViewModal();
    }
});

// Download from QR View Modal
if (qrViewDownloadBtn) {
    qrViewDownloadBtn.addEventListener("click", function () {
        const codeValue = qrViewDownloadBtn.dataset.tcCodeValue || "qr-code";
        const qrImage = qrViewDownloadBtn.dataset.tcQrImage || "";
        const tc = { codeValue: codeValue, qrImage: qrImage };
        downloadQr(tc);
    });
}

// Print from QR View Modal
if (qrViewPrintBtn) {
    qrViewPrintBtn.addEventListener("click", function () {
        const codeValue = qrViewPrintBtn.dataset.tcCodeValue || "qr-code";
        const qrImage = qrViewPrintBtn.dataset.tcQrImage || "";
        const status = qrViewPrintBtn.dataset.tcStatus || "";
        const tc = { codeValue: codeValue, qrImage: qrImage, status: status };
        printQr(tc);
    });
}

/* =====================================================
   DOWNLOAD QR
===================================================== */

async function downloadQr(tc) {
    if (!tc || !tc.qrImage) {
        alert("QR code image is unavailable for download.");
        return;
    }

    const qrUrl = getQrImageUrl(tc.qrImage);
    const filename = (tc.codeValue || "qr-code") + ".png";

    try {
        // Fetch the image and trigger download via blob
        const response = await fetch(qrUrl);

        if (!response.ok) {
            throw new Error("Failed to fetch QR image.");
        }

        const blob = await response.blob();
        const blobUrl = URL.createObjectURL(blob);

        const link = document.createElement("a");
        link.href = blobUrl;
        link.download = filename;
        link.target = "_blank";
        link.rel = "noopener";

        document.body.appendChild(link);
        link.click();

        // Clean up
        setTimeout(function () {
            document.body.removeChild(link);
            URL.revokeObjectURL(blobUrl);
        }, 100);
    } catch (error) {
        console.error("Download QR error:", error);

        // Fallback: open in new tab for manual download
        const link = document.createElement("a");
        link.href = qrUrl;
        link.download = filename;
        link.target = "_blank";
        link.rel = "noopener";
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }
}

/* =====================================================
   PRINT QR
===================================================== */

function printQr(tc) {
    if (!tc || !tc.qrImage) {
        alert("QR code image is unavailable for printing.");
        return;
    }

    const qrUrl = getQrImageUrl(tc.qrImage);
    const codeValue = tc.codeValue || "-";
    const status = tc.status || "-";

    // Create a clean print-only popup
    const popup = window.open("", "_blank", "width=400,height=500");

    popup.document.write("<!DOCTYPE html><html><head>");
    popup.document.write("<title>Print QR - " + escapeHtml(codeValue) + "</title>");
    popup.document.write("<style>");
    popup.document.write("body { font-family: Arial, sans-serif; text-align: center; padding: 40px 20px; }");
    popup.document.write(".qr-container { display: inline-block; padding: 20px; }");
    popup.document.write(".qr-container img { width: 280px; height: 280px; object-fit: contain; }");
    popup.document.write(".qr-container h3 { margin: 16px 0 4px; font-size: 1.1rem; }");
    popup.document.write(".qr-container p { margin: 4px 0; color: #555; font-size: 0.9rem; }");
    popup.document.write("@media print { body { padding: 20px; } }");
    popup.document.write("</style>");
    popup.document.write("</head><body>");
    popup.document.write("<div class='qr-container'>");
    popup.document.write("<img src='" + escapeHtml(qrUrl) + "' alt='QR Code' onerror=\"this.alt='QR unavailable'\">");
    popup.document.write("<h3>" + escapeHtml(codeValue) + "</h3>");
    popup.document.write("<p>" + escapeHtml(status) + "</p>");
    popup.document.write("</div>");
    popup.document.write("</body></html>");

    popup.document.close();
    popup.focus();

    // Wait for image to load before printing
    setTimeout(function () {
        popup.print();
        popup.close();
    }, 500);
}

function escapeHtml(value) {
    const div = document.createElement("div");
    div.textContent = value ?? "";
    return div.innerHTML;
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

        // QR Image thumbnail
        const qrCell = document.createElement("td");
        if (tc.qrImage) {
            const qrImg = document.createElement("img");
            const qrUrl = getQrImageUrl(tc.qrImage);
            qrImg.src = qrUrl;
            qrImg.alt = "QR Code";
            qrImg.style.width = "48px";
            qrImg.style.height = "48px";
            qrImg.style.objectFit = "contain";
            qrImg.style.cursor = "pointer";
            qrImg.title = "Click to view QR";
            qrImg.onerror = function () {
                qrImg.src = "/frontend/assets/images/qr-placeholder.png";
            };
            // Click thumbnail to open QR view
            qrImg.addEventListener("click", function () {
                openQrViewModal(tc);
            });
            qrCell.appendChild(qrImg);
        } else {
            qrCell.textContent = "—";
        }

        // Actions column
        const actionCell = document.createElement("td");
        actionCell.className = "production-lot-actions";
        actionCell.appendChild(createTraceCodeActionsMenu(tc));

        row.appendChild(codeCell);
        row.appendChild(statusCell);
        row.appendChild(qrCell);
        row.appendChild(actionCell);

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
   ACTIVATION UI
===================================================== */

function setupActivationUI(shipment) {
    if (!shipment || !activateShipmentBtn || !activatedMessage || !activationError) return;

    // Hide activation error initially
    if (activationError) activationError.style.display = "none";

    const normalizedStatus = String(shipment.status || "").trim().toUpperCase();

    if (normalizedStatus === "CODE_PRINTED") {
        // Show activate button
        if (activateShipmentBtn) activateShipmentBtn.style.display = "inline-block";
        if (activatedMessage) activatedMessage.style.display = "none";
    } else if (normalizedStatus === "ACTIVATED") {
        // Show activated message
        if (activateShipmentBtn) activateShipmentBtn.style.display = "none";
        if (activatedMessage) activatedMessage.style.display = "block";
    } else {
        // Other statuses - hide both
        if (activateShipmentBtn) activateShipmentBtn.style.display = "none";
        if (activatedMessage) activatedMessage.style.display = "none";
    }
}

async function handleActivateShipment() {
    if (isActivating) return;
    if (!shipmentId || !currentShipment) return;

    // Show confirmation dialog
    if (!confirm("Are you sure you want to activate this shipment?\n\nAfter activation, the shipment and its trace codes will become active according to the backend business rules.")) {
        return;
    }

    isActivating = true;
    if (activateShipmentBtn) {
        activateShipmentBtn.disabled = true;
        activateShipmentBtn.textContent = "Activating...";
    }
    if (activationError) activationError.style.display = "none";

    try {
        const response = await activateShipment(shipmentId);

        if (response && response.success === false) {
            throw new Error(response.message || "Activation failed.");
        }

        // Extract updated shipment from response
        const updatedShipment = response && response.data ? response.data : response;

        if (updatedShipment) {
            // Update the page with the backend response
            currentShipment = updatedShipment;
            renderShipmentDetails(updatedShipment);

            allTraceCodes = updatedShipment.traceCodes || [];
            filteredTraceCodes = allTraceCodes.slice();
            currentPage = 1;
            renderTraceCodes(filteredTraceCodes);

            // Update activation UI
            setupActivationUI(updatedShipment);
        }

        // Show success message
        if (activatedMessage) {
            activatedMessage.textContent = "✓ Shipment activated successfully";
            activatedMessage.style.display = "block";
        }

    } catch (error) {
        console.error("Activate shipment error:", error);

        let message = error.message || "Unable to activate shipment. Please try again later.";
        const normalizedMessage = String(message).toLowerCase();

        if (normalizedMessage.includes("403")) {
            message = "You do not have permission to activate this shipment.";
        } else if (normalizedMessage.includes("404")) {
            message = "Shipment not found.";
        } else if (normalizedMessage.includes("already") || normalizedMessage.includes("đã")) {
            // Already activated - refresh the page state
            message = "This shipment has already been activated.";
        }

        if (activationError) {
            activationError.textContent = message;
            activationError.style.display = "block";
        }
    } finally {
        isActivating = false;
        if (activateShipmentBtn) {
            activateShipmentBtn.disabled = false;
            activateShipmentBtn.textContent = "Activate Shipment";
        }
    }
}

// Activate button event listener
if (activateShipmentBtn) {
    activateShipmentBtn.addEventListener("click", handleActivateShipment);
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
        setupActivationUI(currentShipment);

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