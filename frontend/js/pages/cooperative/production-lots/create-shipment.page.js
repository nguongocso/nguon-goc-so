import {
    requireAuth,
    setupLogout
} from "../../../core/auth-guard.js";

import {
    getUser
} from "../../../core/storage.js";

import {
    getProductionLots
} from "../../../services/production-lot.service.js";

import {
    createShipment
} from "../../../services/shipment.service.js";

/* =====================================================
   AUTHENTICATION
===================================================== */

if (!requireAuth()) {
    // requireAuth đã tự chuyển về trang đăng nhập.
}

const user = getUser();

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
   GET PRODUCTION LOT ID FROM URL
===================================================== */

const urlParams = new URLSearchParams(window.location.search);
const productionLotId = urlParams.get("id");

if (!productionLotId) {
    window.location.href = "../production-lots/index.html";
    throw new Error("No production lot ID provided.");
}

/* =====================================================
   DOM REFERENCES
===================================================== */

const loadingState = document.getElementById("loadingState");
const errorState = document.getElementById("errorState");
const errorMessage = document.getElementById("errorMessage");
const retryButton = document.getElementById("retryButton");
const mainContent = document.getElementById("mainContent");

const lotNameEl = document.getElementById("lotName");
const lotProductCategoryEl = document.getElementById("lotProductCategory");
const lotFarmAreaEl = document.getElementById("lotFarmArea");
const lotStatusEl = document.getElementById("lotStatus");
const lotActualQuantityEl = document.getElementById("lotActualQuantity");

const createForm = document.getElementById("createShipmentForm");
const productionLotIdInput = document.getElementById("productionLotId");
const shipmentNameInput = document.getElementById("shipmentName");
const totalQuantityInput = document.getElementById("totalQuantity");
const packagingInfoInput = document.getElementById("packagingInfo");
const submitButton = document.getElementById("submitShipmentButton");
const formMessage = document.getElementById("formMessage");

const shipmentNameError = document.getElementById("shipmentNameError");
const totalQuantityError = document.getElementById("totalQuantityError");

/* =====================================================
   PAGE STATE
===================================================== */

let productionLot = null;

/* =====================================================
   LOAD PRODUCTION LOT
===================================================== */

async function loadProductionLot() {
    if (loadingState) loadingState.style.display = "flex";
    if (errorState) errorState.style.display = "none";
    if (mainContent) mainContent.style.display = "none";

    try {
        const response = await getProductionLots();

        if (response && response.success === false) {
            throw new Error(response.message || "Không thể tải thông tin lô sản xuất.");
        }

        const lots = extractProductionLots(response);
        productionLot = lots.find(function (item) {
            return String(item.id) === String(productionLotId);
        });

        if (!productionLot) {
            throw new Error("Không tìm thấy lô sản xuất.");
        }

        // Validate status is PACKAGED
        const normalizedStatus = String(productionLot.status || "").trim().toUpperCase();
        if (normalizedStatus !== "PACKAGED") {
            throw new Error("Lô sản xuất không ở trạng thái PACKAGED. Không thể tạo lô hàng.");
        }

        if (loadingState) loadingState.style.display = "none";
        if (mainContent) mainContent.style.display = "block";

        renderProductionLotInfo(productionLot);

    } catch (error) {
        console.error("Load production lot error:", error);

        if (loadingState) loadingState.style.display = "none";
        if (mainContent) mainContent.style.display = "none";

        let message = error.message || "Đã xảy ra lỗi khi tải thông tin lô sản xuất.";
        const normalizedMessage = String(message).toLowerCase();

        if (normalizedMessage.includes("403")) {
            message = "Bạn không có quyền xem lô sản xuất này.";
        }

        if (errorMessage) errorMessage.textContent = message;
        if (errorState) errorState.style.display = "flex";
    }
}

function extractProductionLots(response) {
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

function renderProductionLotInfo(lot) {
    if (lotNameEl) lotNameEl.textContent = lot.name || "—";
    if (lotProductCategoryEl) lotProductCategoryEl.textContent = lot.productCategoryName || "—";
    if (lotFarmAreaEl) lotFarmAreaEl.textContent = lot.farmAreaName || "—";
    if (lotStatusEl) {
        lotStatusEl.textContent = lot.status || "—";
        lotStatusEl.className = "status-badge status-badge-" + (lot.status || "").toLowerCase();
    }
    if (lotActualQuantityEl) {
        lotActualQuantityEl.textContent = lot.actualQuantity != null ? String(lot.actualQuantity) : (lot.expectedQuantity != null ? String(lot.expectedQuantity) : "—");
    }

    // Set hidden field
    if (productionLotIdInput) {
        productionLotIdInput.value = lot.id || "";
    }
}

/* =====================================================
   FORM VALIDATION
===================================================== */

function validateForm() {
    let isValid = true;

    // Reset errors
    if (shipmentNameError) shipmentNameError.textContent = "";
    if (totalQuantityError) totalQuantityError.textContent = "";

    // Shipment Name
    const name = shipmentNameInput ? shipmentNameInput.value.trim() : "";
    if (!name) {
        if (shipmentNameError) shipmentNameError.textContent = "Tên lô hàng không được để trống.";
        if (shipmentNameInput) shipmentNameInput.focus();
        isValid = false;
    }

    // Total Quantity
    const quantityStr = totalQuantityInput ? totalQuantityInput.value.trim() : "";
    if (!quantityStr) {
        if (totalQuantityError) totalQuantityError.textContent = "Vui lòng nhập số lượng.";
        if (!isValid && totalQuantityInput) totalQuantityInput.focus();
        if (isValid && totalQuantityInput) totalQuantityInput.focus();
        isValid = false;
    } else {
        const quantity = Number(quantityStr);
        if (Number.isNaN(quantity) || !Number.isFinite(quantity)) {
            if (totalQuantityError) totalQuantityError.textContent = "Số lượng không hợp lệ.";
            if (totalQuantityInput) totalQuantityInput.focus();
            isValid = false;
        } else if (quantity <= 0) {
            if (totalQuantityError) totalQuantityError.textContent = "Số lượng phải lớn hơn 0.";
            if (totalQuantityInput) totalQuantityInput.focus();
            isValid = false;
        } else if (!Number.isInteger(quantity)) {
            if (totalQuantityError) totalQuantityError.textContent = "Số lượng phải là số nguyên.";
            if (totalQuantityInput) totalQuantityInput.focus();
            isValid = false;
        }
    }

    return isValid;
}

function hideFormMessage() {
    if (formMessage) {
        formMessage.style.display = "none";
        formMessage.textContent = "";
        formMessage.className = "cs-form-message";
    }
}

function showFormError(message) {
    if (formMessage) {
        formMessage.textContent = message;
        formMessage.className = "cs-form-message cs-form-error";
        formMessage.style.display = "block";
    }
}

function showFormSuccess(message) {
    if (formMessage) {
        formMessage.textContent = message;
        formMessage.className = "cs-form-message cs-form-success";
        formMessage.style.display = "block";
    }
}

/* =====================================================
   SUBMIT HANDLER
===================================================== */

let isSubmitting = false;

async function handleSubmit(event) {
    event.preventDefault();

    if (isSubmitting) {
        return;
    }

    hideFormMessage();

    if (!validateForm()) {
        return;
    }

    const payload = {
        productionLotId: productionLotIdInput ? productionLotIdInput.value : "",
        name: shipmentNameInput ? shipmentNameInput.value.trim() : "",
        totalQuantity: Number(totalQuantityInput ? totalQuantityInput.value.trim() : 0),
        packagingInfo: packagingInfoInput ? packagingInfoInput.value.trim() : ""
    };

    try {
        isSubmitting = true;
        setSubmitLoading(true);

        const response = await createShipment(payload);

        // Check if response is successful
        if (!response || response.success === false) {
            throw new Error(response?.message || "Tạo lô hàng thất bại.");
        }

        // Navigate to success page with shipment data
        const shipmentData = response.data || response;
        const encodedData = encodeURIComponent(JSON.stringify(shipmentData));

        window.location.href = `../production-lots/shipment-success.html?shipment=${encodedData}`;

    } catch (error) {
        console.error("Create shipment error:", error);

        isSubmitting = false;
        setSubmitLoading(false);

        // Display server error message
        let message = error.message || "Tạo lô hàng thất bại.";

        // Handle specific HTTP error messages from backend
        const normalizedMessage = String(message).toLowerCase();

        if (normalizedMessage.includes("403") || normalizedMessage.includes("không có quyền")) {
            message = "Bạn không có quyền tạo lô hàng.";
        } else if (normalizedMessage.includes("404") || normalizedMessage.includes("not found")) {
            message = "Không tìm thấy lô sản xuất.";
        } else if (normalizedMessage.includes("sẵn sàng") || normalizedMessage.includes("chưa") || normalizedMessage.includes("packaged")) {
            message = "Lô sản xuất chưa ở trạng thái PACKAGED hoặc đã được sử dụng.";
        } else if (normalizedMessage.includes("409") || normalizedMessage.includes("conflict")) {
            message = message; // Use actual backend message for conflicts
        } else if (normalizedMessage.includes("500") || normalizedMessage.includes("internal")) {
            message = "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.";
        }

        showFormError(message);
    }
}

function setSubmitLoading(isLoading) {
    if (!submitButton) return;

    submitButton.disabled = isLoading;
    submitButton.textContent = isLoading ? "Creating..." : "Create Shipment";
}

/* =====================================================
   EVENT LISTENERS
===================================================== */

if (createForm) {
    createForm.addEventListener("submit", handleSubmit);
}

if (retryButton) {
    retryButton.addEventListener("click", loadProductionLot);
}

/* =====================================================
   LOGOUT
===================================================== */

setupLogout();

/* =====================================================
   INITIALIZE PAGE
===================================================== */

loadProductionLot();