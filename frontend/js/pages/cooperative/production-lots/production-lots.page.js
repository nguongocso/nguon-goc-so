import {
    requireAuth,
    setupLogout
} from "../../../core/auth-guard.js";

import {
    getUser
} from "../../../core/storage.js";

import {
    getProductionLots,
    submitProductionLot,
    approveProductionLot,
    returnToDraftProductionLot,
    updateProductionLot,
    getFarmAreas,
    getProductCategories,
    checkPackagingReadiness,
    packageProductionLot
} from "../../../services/production-lot.service.js";

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

const allowedRoles = ["VT-01", "VT-02", "VT-03"];

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
const productionLotsTable = document.getElementById("productionLotsTable");
const productionLotsTableBody = document.getElementById("productionLotsTableBody");

/* =====================================================
   CREATE BUTTON VISIBILITY
===================================================== */

// VT-03 must not see the Create button or empty state create link
function setupCreateButtonVisibility() {
    if (roleCode === "VT-03") {
        const createButtons = document.querySelectorAll(".production-lot-create-button");
        createButtons.forEach(function (btn) {
            if (btn) btn.style.display = "none";
        });
        // Also hide the header action create link
        const headerCreateLink = document.querySelector(".production-lots-header-actions a.btn-primary");
        if (headerCreateLink) headerCreateLink.style.display = "none";
    }
}

setupCreateButtonVisibility();

/* =====================================================
   EDIT MODAL REFERENCES
===================================================== */

const editLotModal = document.getElementById("editLotModal");
const editLotOverlay = document.getElementById("editLotOverlay");
const closeEditLotButton = document.getElementById("closeEditLotButton");
const cancelEditLotButton = document.getElementById("cancelEditLotButton");
const editLotForm = document.getElementById("editLotForm");
const editLotMessage = document.getElementById("editLotMessage");

const editFields = {
    id: document.getElementById("editLotId"),
    name: document.getElementById("editLotName"),
    farmAreaId: document.getElementById("editFarmAreaId"),
    productCategoryId: document.getElementById("editProductCategoryId"),
    expectedQuantity: document.getElementById("editExpectedQuantity"),
    plantingDate: document.getElementById("editPlantingDate")
};

/* =====================================================
   PAGE STATE
===================================================== */

let productionLots = [];
const USE_MOCK_DATA = false;

/* =====================================================
   FORMAT HELPERS
===================================================== */

function getStatusBadgeClass(status) {
    if (!status) return "status-badge-draft";

    const normalizedStatus = String(status).trim().toLowerCase();

    if (normalizedStatus === "draft") return "status-badge-draft";
    if (normalizedStatus === "pending") return "status-badge-pending";
    if (normalizedStatus === "approved") return "status-badge-approved";
    if (normalizedStatus === "harvested") return "status-badge-harvested";
    if (normalizedStatus === "packaged") return "status-badge-packaged";
    if (normalizedStatus === "closed") return "status-badge-closed";

    return "status-badge-draft";
}

function formatDate(dateStr) {
    if (!dateStr) return "—";

    const dateParts = String(dateStr).split("-");
    if (dateParts.length === 3) {
        const [year, month, day] = dateParts;
        return `${day}/${month}/${year}`;
    }

    const date = new Date(dateStr);
    if (Number.isNaN(date.getTime())) return dateStr;
    return date.toLocaleDateString("vi-VN");
}

function formatDateTime(dateStr) {
    if (!dateStr) return "—";

    const date = new Date(dateStr);
    if (Number.isNaN(date.getTime())) return dateStr;
    return date.toLocaleDateString("vi-VN");
}

/* =====================================================
   NAVIGATION
===================================================== */

function goToFarmLogHistory(lot) {
    if (!lot || !lot.id) return;

    const queryParams = new URLSearchParams({
        productionLotId: lot.id,
        productionLotName: lot.name || ""
    });

    window.location.href = `../farm-logs/history.html?${queryParams.toString()}`;
}

function goToCreateFarmLog(lot) {
    if (!lot || !lot.id) return;

    const queryParams = new URLSearchParams({
        productionLotId: lot.id,
        productionLotName: lot.name || ""
    });

    window.location.href = `../farm-logs/create.html?${queryParams.toString()}`;
}

function goToAttachments(lot) {
    if (!lot || !lot.id) return;

    const queryParams = new URLSearchParams({
        id: lot.id,
        productionLotId: lot.id,
        productionLotName: lot.name || ""
    });

    window.location.href = `./attachment.html?${queryParams.toString()}`;
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

function createThreeDotMenu(lot, normalizedStatus) {
    const container = document.createElement("div");
    container.className = "three-dot-menu-container";

    const dotButton = document.createElement("button");
    dotButton.type = "button";
    dotButton.className = "three-dot-button";
    dotButton.dataset.id = lot.id;
    dotButton.innerHTML = "⋮";
    dotButton.title = "Actions";

    dotButton.addEventListener("click", function (event) {
        event.stopPropagation();
        closeActiveMenu();

        const dropdown = document.createElement("div");
        dropdown.className = "three-dot-dropdown";
        activeMenu = dropdown;

        // Build menu items based on role and status
        var menuItems = [];

        if (roleCode === "VT-02") {
            if (normalizedStatus === "DRAFT") {
                menuItems = [
                    { label: "View Details", action: "view-details" },
                    { label: "Edit", action: "edit" },
                    { label: "Submit for Approval", action: "submit" }
                ];
            } else if (normalizedStatus === "PENDING") {
                menuItems = [
                    { label: "View Details", action: "view-details" },
                    { label: "Approve", action: "approve" },
                    { label: "Return to Draft", action: "return-draft" }
                ];
            } else if (normalizedStatus === "APPROVED") {
                menuItems = [
                    { label: "View Details", action: "view-details" },
                    { label: "View Farming Logs", action: "view-farm-logs" }
                ];
            } else if (normalizedStatus === "HARVESTED") {
                menuItems = [
                    { label: "View Details", action: "view-details" },
                    { label: "Check Packaging", action: "check-packaging" }
                ];
            }
        } else if (roleCode === "VT-01") {
            if (normalizedStatus === "HARVESTED") {
                menuItems = [
                    { label: "View Details", action: "view-details" },
                    { label: "Check Packaging", action: "check-packaging" }
                ];
            } else {
                menuItems = [
                    { label: "View Details", action: "view-details" }
                ];
            }
        } else if (roleCode === "VT-03") {
            menuItems = [
                { label: "View Details", action: "view-details" },
                { label: "View Farming Logs", action: "view-farm-logs" },
                { label: "Create Farming Log", action: "create-farm-log" }
            ];
        }

        menuItems.forEach(function (item) {
            const menuItem = document.createElement("button");
            menuItem.type = "button";
            menuItem.className = "three-dot-menu-item";
            menuItem.textContent = item.label;
            menuItem.dataset.action = item.action;
            menuItem.dataset.id = lot.id;

            menuItem.addEventListener("click", function (event) {
                event.stopPropagation();
                closeActiveMenu();
                handleMenuAction(item.action, lot);
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

function handleMenuAction(action, lot) {
    switch (action) {
        case "view-details":
            window.location.href = `detail.html?id=${encodeURIComponent(lot.id)}`;
            break;
        case "edit":
            openEditLotModal(lot.id);
            break;
        case "submit":
            handleSubmitLot(lot.id);
            break;
        case "approve":
            handleApproveLot(lot.id);
            break;
        case "return-draft":
            handleReturnToDraft(lot.id);
            break;
        case "view-farm-logs":
            goToFarmLogHistory(lot);
            break;
        case "create-farm-log":
            goToCreateFarmLog(lot);
            break;
        case "attach-document":
            goToAttachments(lot);
            break;
        case "check-packaging":
            openPackagingCheckModal(lot);
            break;
    }
}

/* =====================================================
   RENDER PRODUCTION LOT TABLE
===================================================== */

function renderProductionLots(lots) {
    if (!Array.isArray(lots) || lots.length === 0) {
        if (emptyState) emptyState.style.display = "flex";
        if (productionLotsTable) productionLotsTable.style.display = "none";
        if (productionLotsTableBody) productionLotsTableBody.innerHTML = "";
        return;
    }

    if (emptyState) emptyState.style.display = "none";
    if (productionLotsTable) productionLotsTable.style.display = "table";

    if (!productionLotsTableBody) return;

    productionLotsTableBody.innerHTML = "";

    lots.forEach(function (lot) {
        const row = document.createElement("tr");

        const nameCell = document.createElement("td");
        nameCell.textContent = lot.name || "—";

        const farmAreaCell = document.createElement("td");
        farmAreaCell.textContent = lot.farmAreaName || "—";

        const categoryCell = document.createElement("td");
        categoryCell.textContent = lot.productCategoryName || "—";

        const quantityCell = document.createElement("td");
        quantityCell.textContent = lot.expectedQuantity != null ? String(lot.expectedQuantity) : "—";

        const plantingDateCell = document.createElement("td");
        plantingDateCell.textContent = formatDate(lot.plantingDate);

        const statusCell = document.createElement("td");
        const statusBadge = document.createElement("span");
        const normalizedStatus = String(lot.status || "DRAFT").trim().toUpperCase();
        statusBadge.className = "status-badge " + getStatusBadgeClass(normalizedStatus);
        statusBadge.textContent = normalizedStatus;
        statusCell.appendChild(statusBadge);

        const createdCell = document.createElement("td");
        createdCell.textContent = formatDateTime(lot.createdAt);

        const actionCell = document.createElement("td");
        actionCell.className = "production-lot-actions";

        actionCell.appendChild(createThreeDotMenu(lot, normalizedStatus));

        row.appendChild(nameCell);
        row.appendChild(farmAreaCell);
        row.appendChild(categoryCell);
        row.appendChild(quantityCell);
        row.appendChild(plantingDateCell);
        row.appendChild(statusCell);
        row.appendChild(createdCell);
        row.appendChild(actionCell);

        productionLotsTableBody.appendChild(row);
    });
}

/* =====================================================
   MODAL SELECT HELPERS
===================================================== */

function fillSelect(selectElement, items, placeholder) {
    if (!selectElement) return;

    selectElement.innerHTML = "";

    const defaultOption = document.createElement("option");
    defaultOption.value = "";
    defaultOption.textContent = placeholder;
    selectElement.appendChild(defaultOption);

    items.forEach(function (item) {
        const option = document.createElement("option");
        option.value = item.id;
        option.textContent = item.name;
        selectElement.appendChild(option);
    });
}

async function loadEditSelectOptions() {
    try {
        const farmAreasResponse = await getFarmAreas();
        const categoriesResponse = await getProductCategories();

        const farmAreas = farmAreasResponse?.data && Array.isArray(farmAreasResponse.data)
            ? farmAreasResponse.data
            : [];

        const categories = categoriesResponse?.data && Array.isArray(categoriesResponse.data)
            ? categoriesResponse.data
            : [];

        fillSelect(editFields.farmAreaId, farmAreas, "-- Chọn khu vực canh tác --");
        fillSelect(editFields.productCategoryId, categories, "-- Chọn loại nông sản --");
    } catch (error) {
        console.warn("Could not load select options from API, using empty selects:", error);
        fillSelect(editFields.farmAreaId, [], "-- Chọn khu vực canh tác --");
        fillSelect(editFields.productCategoryId, [], "-- Chọn loại nông sản --");
    }
}

/* =====================================================
   EDIT MODAL
===================================================== */

function openEditLotModal(lotId) {
    const lot = productionLots.find(function (item) {
        return item.id === lotId;
    });

    if (!lot || !editLotModal) return;

    loadEditSelectOptions();

    if (editFields.id) editFields.id.value = lot.id || "";
    if (editFields.name) editFields.name.value = lot.name || "";
    if (editFields.farmAreaId) editFields.farmAreaId.value = lot.farmAreaId || "";
    if (editFields.productCategoryId) editFields.productCategoryId.value = lot.productCategoryId || "";
    if (editFields.expectedQuantity) editFields.expectedQuantity.value = lot.expectedQuantity ?? "";
    if (editFields.plantingDate) editFields.plantingDate.value = lot.plantingDate || "";

    if (editLotMessage) editLotMessage.hidden = true;

    editLotModal.hidden = false;
    document.body.classList.add("modal-open");
}

function closeEditLotModal() {
    if (!editLotModal) return;

    editLotModal.hidden = true;
    document.body.classList.remove("modal-open");
    if (editLotForm) editLotForm.reset();
    if (editLotMessage) editLotMessage.hidden = true;
}

async function handleEditLotSubmit(event) {
    event.preventDefault();

    const lotId = editFields.id ? editFields.id.value : "";

    const payload = {
        farmAreaId: editFields.farmAreaId ? editFields.farmAreaId.value : "",
        productCategoryId: editFields.productCategoryId ? editFields.productCategoryId.value : "",
        name: editFields.name ? editFields.name.value.trim() : "",
        expectedQuantity: editFields.expectedQuantity ? Number(editFields.expectedQuantity.value) : 0,
        plantingDate: editFields.plantingDate ? editFields.plantingDate.value : ""
    };

    try {
        const response = await updateProductionLot(lotId, payload);

        if (!response || response.success === false) {
            throw new Error(response?.message || "Cập nhật lô sản xuất thất bại.");
        }

        if (editLotMessage) {
            editLotMessage.textContent = "Cập nhật lô sản xuất thành công.";
            editLotMessage.className = "modal-message success";
            editLotMessage.hidden = false;
        }

        // Close modal after brief delay
        setTimeout(function () {
            closeEditLotModal();
            loadProductionLots();
        }, 1000);
    } catch (error) {
        console.error("Update production lot error:", error);

        if (editLotMessage) {
            editLotMessage.textContent = error.message || "Cập nhật thất bại.";
            editLotMessage.className = "modal-message error";
            editLotMessage.hidden = false;
        }
    }
}

/* =====================================================
   SUBMIT / APPROVE / RETURN ACTIONS
===================================================== */

async function handleSubmitLot(lotId) {
    if (!confirm("Bạn có chắc chắn muốn submit lô sản xuất này để duyệt?")) {
        return;
    }

    try {
        const response = await submitProductionLot(lotId);

        if (!response || response.success === false) {
            throw new Error(response?.message || "Submit lô sản xuất thất bại.");
        }

        alert("Submit lô sản xuất thành công. Trạng thái đã chuyển thành PENDING.");
        await loadProductionLots();
    } catch (error) {
        console.error("Submit production lot error:", error);
        alert(error.message || "Submit lô sản xuất thất bại.");
    }
}

async function handleApproveLot(lotId) {
    if (!confirm("Bạn có chắc chắn muốn duyệt lô sản xuất này?")) {
        return;
    }

    try {
        const response = await approveProductionLot(lotId);

        if (!response || response.success === false) {
            throw new Error(response?.message || "Duyệt lô sản xuất thất bại.");
        }

        alert("Duyệt lô sản xuất thành công. Trạng thái đã chuyển thành APPROVED.");
        await loadProductionLots();
    } catch (error) {
        console.error("Approve production lot error:", error);
        alert(error.message || "Duyệt lô sản xuất thất bại.");
    }
}

async function handleReturnToDraft(lotId) {
    const reason = prompt("Vui lòng nhập lý do trả về DRAFT:");

    if (reason === null) {
        // User cancelled
        return;
    }

    if (!reason || !reason.trim()) {
        alert("Vui lòng nhập lý do trả về DRAFT.");
        return;
    }

    try {
        const response = await returnToDraftProductionLot(lotId, reason);

        if (!response || response.success === false) {
            throw new Error(response?.message || "Trả về DRAFT thất bại.");
        }

        alert("Trả về DRAFT thành công.");
        await loadProductionLots();
    } catch (error) {
        console.error("Return to draft error:", error);
        alert(error.message || "Trả về DRAFT thất bại.");
    }
}

/* =====================================================
   NORMALIZE API DATA
===================================================== */

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

/* =====================================================
   LOAD PRODUCTION LOTS
===================================================== */

async function loadProductionLots() {
    if (loadingState) loadingState.style.display = "flex";
    if (errorState) errorState.style.display = "none";
    if (mainContent) mainContent.style.display = "none";

    try {
        if (USE_MOCK_DATA) {
            productionLots = [];
        } else {
            const response = await getProductionLots();

            if (response && response.success === false) {
                throw new Error(response.message || "Không thể tải danh sách lô sản xuất.");
            }

            productionLots = extractProductionLots(response);
        }

        if (loadingState) loadingState.style.display = "none";
        if (mainContent) mainContent.style.display = "block";

        renderProductionLots(productionLots);
    } catch (error) {
        console.error("Load production lots error:", error);

        if (loadingState) loadingState.style.display = "none";
        if (mainContent) mainContent.style.display = "none";

        let message = error.message || "Đã xảy ra lỗi khi tải danh sách lô sản xuất.";
        const normalizedMessage = String(message).toLowerCase();

        if (normalizedMessage.includes("404") || normalizedMessage.includes("not found")) {
            if (mainContent) mainContent.style.display = "block";
            renderProductionLots([]);
            return;
        }

        if (normalizedMessage.includes("403")) {
            message = "Bạn không có quyền xem danh sách lô sản xuất.";
        }

        if (errorMessage) errorMessage.textContent = message;
        if (errorState) errorState.style.display = "flex";
    }
}

/* =====================================================
   TABLE ACTION EVENTS
===================================================== */

if (productionLotsTableBody) {
    productionLotsTableBody.addEventListener("click", function (event) {
        const target = event.target;
        if (!(target instanceof HTMLElement)) return;

        const button = target.closest("button");
        if (!button) return;

        const lotId = button.dataset.id;
        if (!lotId) return;

        const lot = productionLots.find(function (item) {
            return String(item.id) === String(lotId);
        });

        if (!lot) {
            console.warn("Không tìm thấy lô sản xuất:", lotId);
            return;
        }

        if (button.classList.contains("btn-edit-lot")) {
            openEditLotModal(lotId);
            return;
        }

        if (button.classList.contains("btn-submit-lot")) {
            handleSubmitLot(lotId);
            return;
        }

        if (button.classList.contains("btn-approve-lot")) {
            handleApproveLot(lotId);
            return;
        }

        if (button.classList.contains("btn-return-lot")) {
            handleReturnToDraft(lotId);
            return;
        }

        if (button.classList.contains("btn-history-lot")) {
            goToFarmLogHistory(lot);
            return;
        }

        if (button.classList.contains("btn-create-farm-log")) {
            goToCreateFarmLog(lot);
            return;
        }

        if (button.classList.contains("btn-attachment-lot")) {
            goToAttachments(lot);
            return;
        }
    });
}

/* =====================================================
   MODAL EVENTS
===================================================== */

if (editLotForm) {
    editLotForm.addEventListener("submit", handleEditLotSubmit);
}

if (closeEditLotButton) {
    closeEditLotButton.addEventListener("click", closeEditLotModal);
}

if (cancelEditLotButton) {
    cancelEditLotButton.addEventListener("click", closeEditLotModal);
}

if (editLotOverlay) {
    editLotOverlay.addEventListener("click", closeEditLotModal);
}

document.addEventListener("keydown", function (event) {
    if (event.key === "Escape" && editLotModal && !editLotModal.hidden) {
        closeEditLotModal();
    }
});

/* =====================================================
   RETRY EVENT
===================================================== */

if (retryButton) {
    retryButton.addEventListener("click", loadProductionLots);
}

/* =====================================================
   SEARCH
===================================================== */

const productionLotSearchInput = document.getElementById("searchProductionLot");

if (productionLotSearchInput) {
    productionLotSearchInput.addEventListener("input", function (event) {
        const keyword = String(event.target.value || "").trim().toLowerCase();

        if (!keyword) {
            renderProductionLots(productionLots);
            return;
        }

        const filteredLots = productionLots.filter(function (lot) {
            const searchableText = [
                lot.name,
                lot.farmAreaName,
                lot.productCategoryName,
                lot.status,
                lot.expectedQuantity
            ]
                .filter(function (value) {
                    return value !== null && value !== undefined;
                })
                .join(" ")
                .toLowerCase();

            return searchableText.includes(keyword);
        });

        renderProductionLots(filteredLots);
    });
}

/* =====================================================
   PACKAGING CHECK MODAL
===================================================== */

function openPackagingCheckModal(lot) {
    if (!lot || !lot.id) return;

    // Create modal overlay
    const overlay = document.createElement("div");
    overlay.className = "modal__overlay";
    overlay.style.cssText = "position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.5);z-index:999;";

    // Create modal
    const modal = document.createElement("div");
    modal.className = "modal";
    modal.style.cssText = "position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);background:#fff;border-radius:12px;z-index:1000;width:440px;max-width:90vw;max-height:80vh;overflow-y:auto;box-shadow:0 20px 60px rgba(0,0,0,0.3);";

    // Initial loading content
    modal.innerHTML = `
        <div style="padding:32px;text-align:center;">
            <div style="margin-bottom:16px;">
                <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#6b7280" stroke-width="2">
                    <circle cx="12" cy="12" r="10" />
                    <path d="M12 6v6l4 2" />
                </svg>
            </div>
            <p style="color:#6b7280;font-size:0.9rem;">Đang kiểm tra điều kiện đóng gói...</p>
        </div>
    `;

    document.body.appendChild(overlay);
    document.body.appendChild(modal);
    document.body.classList.add("modal-open");

    // Close handler
    function closeModal() {
        if (overlay.parentNode) overlay.parentNode.removeChild(overlay);
        if (modal.parentNode) modal.parentNode.removeChild(modal);
        document.body.classList.remove("modal-open");
    }

    overlay.addEventListener("click", closeModal);

    // Escape key handler
    function escHandler(e) {
        if (e.key === "Escape") {
            closeModal();
            document.removeEventListener("keydown", escHandler);
        }
    }
    document.addEventListener("keydown", escHandler);

    // Call API
    checkPackagingReadiness(lot.id)
        .then(function (response) {
            const data = response && response.data ? response.data : response;

            if (data.canPackage) {
                // Ready for packaging
                modal.innerHTML = `
                    <div style="padding:32px;">
                        <div style="text-align:center;margin-bottom:20px;">
                            <div style="width:56px;height:56px;border-radius:50%;background:#d1fae5;display:flex;align-items:center;justify-content:center;margin:0 auto 12px;">
                                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#059669" stroke-width="2.5">
                                    <polyline points="20 6 9 17 4 12" />
                                </svg>
                            </div>
                            <h3 style="margin:0 0 4px;font-size:1.1rem;font-weight:700;color:#172033;">Sẵn sàng đóng gói</h3>
                            <p style="margin:0;color:#6b7280;font-size:0.9rem;">Tất cả nhật ký canh tác bắt buộc đã có.</p>
                        </div>
                        <div style="margin-bottom:20px;">
                            <div style="display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid #f3f4f6;">
                                <span style="color:#6b7280;font-size:0.85rem;">Lô sản xuất</span>
                                <span style="font-weight:600;font-size:0.85rem;">${lot.name || "—"}</span>
                            </div>
                            <div style="display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid #f3f4f6;">
                                <span style="color:#6b7280;font-size:0.85rem;">Trạng thái</span>
                                <span class="status-badge status-badge-harvested" style="font-size:0.75rem;">${data.status || "HARVESTED"}</span>
                            </div>
                        </div>
                        <div style="display:flex;gap:12px;">
                            <button id="packagingCancelBtn" style="flex:1;padding:10px;border:1px solid #d8dfdb;border-radius:8px;background:#fff;color:#344054;font-size:0.9rem;cursor:pointer;">Đóng</button>
                            <button id="packageConfirmBtn" style="flex:1;padding:10px;border:none;border-radius:8px;background:#059669;color:#fff;font-size:0.9rem;cursor:pointer;">Đóng gói</button>
                        </div>
                    </div>
                `;

                document.getElementById("packagingCancelBtn").addEventListener("click", closeModal);
                document.getElementById("packageConfirmBtn").addEventListener("click", function () {
                    handlePackageLot(lot, modal, closeModal);
                });
            } else {
                // Not ready - show missing logs
                const missingItems = (data.missingLogs || [])
                    .map(function (log) {
                        return '<div style="display:flex;align-items:center;gap:8px;padding:8px 12px;margin-bottom:6px;background:#fef2f2;border-radius:8px;color:#dc2626;font-size:0.85rem;">' +
                            '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>' +
                            '<span>' + log + '</span></div>';
                    })
                    .join("");

                modal.innerHTML = `
                    <div style="padding:32px;">
                        <div style="text-align:center;margin-bottom:20px;">
                            <div style="width:56px;height:56px;border-radius:50%;background:#fef2f2;display:flex;align-items:center;justify-content:center;margin:0 auto 12px;">
                                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#dc2626" stroke-width="2.5">
                                    <circle cx="12" cy="12" r="10"/>
                                    <line x1="12" y1="8" x2="12" y2="12"/>
                                    <line x1="12" y1="16" x2="12.01" y2="16"/>
                                </svg>
                            </div>
                            <h3 style="margin:0 0 4px;font-size:1.1rem;font-weight:700;color:#172033;">Chưa sẵn sàng đóng gói</h3>
                            <p style="margin:0;color:#6b7280;font-size:0.9rem;">Thiếu nhật ký canh tác bắt buộc:</p>
                        </div>
                        <div style="margin-bottom:20px;">
                            ${missingItems}
                        </div>
                        <div style="display:flex;gap:12px;">
                            <button id="packagingCloseBtn" style="flex:1;padding:10px;border:1px solid #d8dfdb;border-radius:8px;background:#fff;color:#344054;font-size:0.9rem;cursor:pointer;">Đóng</button>
                            <button id="addFarmLogBtn" style="flex:1;padding:10px;border:none;border-radius:8px;background:#2563eb;color:#fff;font-size:0.9rem;cursor:pointer;">Thêm nhật ký</button>
                        </div>
                    </div>
                `;

                document.getElementById("packagingCloseBtn").addEventListener("click", closeModal);
                document.getElementById("addFarmLogBtn").addEventListener("click", function () {
                    closeModal();
                    goToCreateFarmLog(lot);
                });
            }
        })
        .catch(function (error) {
            // Error state
            modal.innerHTML = `
                <div style="padding:32px;text-align:center;">
                    <div style="width:56px;height:56px;border-radius:50%;background:#fef2f2;display:flex;align-items:center;justify-content:center;margin:0 auto 16px;">
                        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#dc2626" stroke-width="2.5">
                            <circle cx="12" cy="12" r="10"/>
                            <line x1="12" y1="8" x2="12" y2="12"/>
                            <line x1="12" y1="16" x2="12.01" y2="16"/>
                        </svg>
                    </div>
                    <h3 style="margin:0 0 8px;font-size:1rem;font-weight:700;color:#172033;">Lỗi kiểm tra</h3>
                    <p style="margin:0 0 20px;color:#6b7280;font-size:0.9rem;">${error.message || "Không thể kiểm tra điều kiện đóng gói."}</p>
                    <button id="packagingErrorCloseBtn" style="padding:10px 24px;border:1px solid #d8dfdb;border-radius:8px;background:#fff;color:#344054;font-size:0.9rem;cursor:pointer;">Đóng</button>
                </div>
            `;

            document.getElementById("packagingErrorCloseBtn").addEventListener("click", closeModal);
        });
}

async function handlePackageLot(lot, modal, closeModal) {
    // Show loading state
    modal.innerHTML = `
        <div style="padding:32px;text-align:center;">
            <div style="margin-bottom:16px;">
                <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#6b7280" stroke-width="2">
                    <circle cx="12" cy="12" r="10" />
                    <path d="M12 6v6l4 2" />
                </svg>
            </div>
            <p style="color:#6b7280;font-size:0.9rem;">Đang đóng gói...</p>
        </div>
    `;

    try {
        const response = await packageProductionLot(lot.id);

        if (!response || response.success === false) {
            throw new Error(response?.message || "Đóng gói thất bại.");
        }

        // Success
        modal.innerHTML = `
            <div style="padding:32px;text-align:center;">
                <div style="width:56px;height:56px;border-radius:50%;background:#d1fae5;display:flex;align-items:center;justify-content:center;margin:0 auto 12px;">
                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#059669" stroke-width="2.5">
                        <polyline points="20 6 9 17 4 12" />
                    </svg>
                </div>
                <h3 style="margin:0 0 4px;font-size:1.1rem;font-weight:700;color:#172033;">Đã đóng gói thành công</h3>
                <p style="margin:0 0 20px;color:#6b7280;font-size:0.9rem;">Lô sản xuất đã được chuyển sang trạng thái PACKAGED.</p>
                <button id="packagingDoneBtn" style="padding:10px 24px;border:none;border-radius:8px;background:#059669;color:#fff;font-size:0.9rem;cursor:pointer;">Hoàn tất</button>
            </div>
        `;

        document.getElementById("packagingDoneBtn").addEventListener("click", function () {
            closeModal();
            loadProductionLots();
        });
    } catch (error) {
        // Error
        modal.innerHTML = `
            <div style="padding:32px;text-align:center;">
                <div style="width:56px;height:56px;border-radius:50%;background:#fef2f2;display:flex;align-items:center;justify-content:center;margin:0 auto 16px;">
                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#dc2626" stroke-width="2.5">
                        <circle cx="12" cy="12" r="10"/>
                        <line x1="12" y1="8" x2="12" y2="12"/>
                        <line x1="12" y1="16" x2="12.01" y2="16"/>
                    </svg>
                </div>
                <h3 style="margin:0 0 8px;font-size:1rem;font-weight:700;color:#172033;">Đóng gói thất bại</h3>
                <p style="margin:0 0 20px;color:#6b7280;font-size:0.9rem;">${error.message || "Không thể đóng gói lô sản xuất."}</p>
                <button id="packagingErrorCloseBtn" style="padding:10px 24px;border:1px solid #d8dfdb;border-radius:8px;background:#fff;color:#344054;font-size:0.9rem;cursor:pointer;">Đóng</button>
            </div>
        `;

        document.getElementById("packagingErrorCloseBtn").addEventListener("click", closeModal);
    }
}

/* =====================================================
   LOGOUT
===================================================== */

setupLogout();

/* =====================================================
   INITIALIZE PAGE
===================================================== */

loadProductionLots();
