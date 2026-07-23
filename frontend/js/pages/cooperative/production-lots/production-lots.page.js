import {
    requireAuth,
    setupLogout
} from "../../../core/auth-guard.js";

import {
    getUser,
    getToken
} from "../../../core/storage.js";

import {
    getProductionLots,
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
    if (!user || user.roleCode !== "VT-03") {
        return;
    }

    const menuIds = [
        "dashboardMenu",
        "farmAreasMenu",
        "organizationProfileMenu"
    ];

    menuIds.forEach(function (menuId) {
        const menuItem =
            document.getElementById(menuId);

        if (menuItem) {
            menuItem.style.display = "none";
        }
    });
}

setupSidebarByRole();

if (!user || !user.roleCode) {
    window.location.href =
        "/frontend/pages/auth/login.html";

    throw new Error(
        "User not authenticated."
    );
}

const roleCode = user.roleCode;

const allowedRoles = [
    "VT-01",
    "VT-02",
    "VT-03"
];

if (!allowedRoles.includes(roleCode)) {
    const loadingElement =
        document.getElementById(
            "loadingState"
        );

    const unauthorizedElement =
        document.getElementById(
            "unauthorizedState"
        );

    const mainElement =
        document.getElementById(
            "mainContent"
        );

    if (loadingElement) {
        loadingElement.style.display =
            "none";
    }

    if (unauthorizedElement) {
        unauthorizedElement.style.display =
            "flex";
    }

    if (mainElement) {
        mainElement.style.display =
            "none";
    }

    throw new Error(
        "Access denied: user does not have permission to access this page."
    );
}

/* =====================================================
   USER INFORMATION
===================================================== */

function populateUserInfo() {
    const sidebarName =
        document.getElementById(
            "sidebarUserName"
        );

    const sidebarOrg =
        document.getElementById(
            "sidebarUserOrg"
        );

    const sidebarAvatar =
        document.getElementById(
            "sidebarUserAvatar"
        );

    const headerName =
        document.getElementById(
            "headerUserName"
        );

    const headerOrg =
        document.getElementById(
            "headerUserOrg"
        );

    const headerRole =
        document.getElementById(
            "headerUserRole"
        );

    if (sidebarName) {
        sidebarName.textContent =
            user.fullName ||
            user.username ||
            "—";
    }

    if (sidebarOrg) {
        sidebarOrg.textContent =
            user.organizationName ||
            "—";
    }

    if (sidebarAvatar) {
        const displayName =
            user.fullName ||
            user.username ||
            "?";

        sidebarAvatar.textContent =
            displayName
                .charAt(0)
                .toUpperCase();
    }

    if (headerName) {
        headerName.textContent =
            user.fullName ||
            user.username ||
            "—";
    }

    if (headerOrg) {
        headerOrg.textContent =
            user.organizationName ||
            "—";
    }

    if (headerRole) {
        headerRole.textContent =
            user.roleCode ||
            "—";
    }
}

populateUserInfo();

/* =====================================================
   DOM REFERENCES
===================================================== */

const loadingState =
    document.getElementById(
        "loadingState"
    );

const errorState =
    document.getElementById(
        "errorState"
    );

const errorMessage =
    document.getElementById(
        "errorMessage"
    );

const retryButton =
    document.getElementById(
        "retryButton"
    );

const mainContent =
    document.getElementById(
        "mainContent"
    );

const emptyState =
    document.getElementById(
        "emptyState"
    );

const productionLotsTable =
    document.getElementById(
        "productionLotsTable"
    );

const productionLotsTableBody =
    document.getElementById(
        "productionLotsTableBody"
    );

/* =====================================================
   EDIT MODAL REFERENCES
===================================================== */

const editLotModal =
    document.getElementById(
        "editLotModal"
    );

const editLotOverlay =
    document.getElementById(
        "editLotOverlay"
    );

const closeEditLotButton =
    document.getElementById(
        "closeEditLotButton"
    );

const cancelEditLotButton =
    document.getElementById(
        "cancelEditLotButton"
    );

const editLotForm =
    document.getElementById(
        "editLotForm"
    );

const editLotMessage =
    document.getElementById(
        "editLotMessage"
    );

const editFields = {
    id:
        document.getElementById(
            "editLotId"
        ),

    name:
        document.getElementById(
            "editLotName"
        ),

    farmAreaId:
        document.getElementById(
            "editFarmAreaId"
        ),

    productCategoryId:
        document.getElementById(
            "editProductCategoryId"
        ),

    expectedQuantity:
        document.getElementById(
            "editExpectedQuantity"
        ),

    plantingDate:
        document.getElementById(
            "editPlantingDate"
        )
};

/* =====================================================
   PAGE STATE
===================================================== */

let productionLots = [];

/*
 * false: sử dụng API backend thật.
 * true: sử dụng dữ liệu giả bên dưới.
 */
const USE_MOCK_DATA = false;

/*
 * Dữ liệu tạm cho hai select trong modal sửa.
 * Khi tích hợp hoàn chỉnh có thể thay bằng API vùng trồng
 * và API danh mục nông sản.
 */
const mockFarmAreas = [
    {
        id: "farm-001",
        name: "Khu vực canh tác A1"
    },
    {
        id: "farm-002",
        name: "Khu vực canh tác B1"
    }
];

const mockProductCategories = [
    {
        id: "category-001",
        name: "Cà chua"
    },
    {
        id: "category-002",
        name: "Xoài Cát Chu"
    }
];

/* =====================================================
   FORMAT HELPERS
===================================================== */

function getStatusBadgeClass(status) {
    if (!status) {
        return "status-badge-draft";
    }

    const normalizedStatus =
        String(status)
            .trim()
            .toLowerCase();

    if (normalizedStatus === "draft") {
        return "status-badge-draft";
    }

    if (normalizedStatus === "pending") {
        return "status-badge-pending";
    }

    if (normalizedStatus === "approved") {
        return "status-badge-approved";
    }

    if (normalizedStatus === "harvested") {
        return "status-badge-harvested";
    }

    if (normalizedStatus === "packaged") {
        return "status-badge-packaged";
    }

    if (normalizedStatus === "closed") {
        return "status-badge-closed";
    }

    return "status-badge-draft";
}

function formatDate(dateStr) {
    if (!dateStr) {
        return "—";
    }

    /*
     * LocalDate từ backend thường có dạng YYYY-MM-DD.
     * Tách thủ công để tránh bị lệch ngày do múi giờ.
     */
    const dateParts =
        String(dateStr).split("-");

    if (dateParts.length === 3) {
        const [
            year,
            month,
            day
        ] = dateParts;

        return `${day}/${month}/${year}`;
    }

    const date =
        new Date(dateStr);

    if (
        Number.isNaN(
            date.getTime()
        )
    ) {
        return dateStr;
    }

    return date.toLocaleDateString(
        "vi-VN"
    );
}

function formatDateTime(dateStr) {
    if (!dateStr) {
        return "—";
    }

    const date =
        new Date(dateStr);

    if (
        Number.isNaN(
            date.getTime()
        )
    ) {
        return dateStr;
    }

    return date.toLocaleDateString(
        "vi-VN"
    );
}

/* =====================================================
   NAVIGATION
===================================================== */

function goToFarmLogHistory(lot) {
    if (!lot || !lot.id) {
        return;
    }

    const queryParams =
        new URLSearchParams({
            productionLotId:
                lot.id,

            productionLotName:
                lot.name || ""
        });

    window.location.href =
        `../farm-logs/history.html?${queryParams.toString()}`;
}

function goToCreateFarmLog(lot) {
    if (!lot || !lot.id) {
        return;
    }

    const queryParams =
        new URLSearchParams({
            productionLotId:
                lot.id,

            productionLotName:
                lot.name || ""
        });

    window.location.href =
        `../farm-logs/create.html?${queryParams.toString()}`;
}

function goToAttachments(lot) {
    if (!lot || !lot.id) {
        return;
    }

    const queryParams =
        new URLSearchParams({
            id: lot.id,
            productionLotId:
                lot.id,
            productionLotName:
                lot.name || ""
        });

    window.location.href =
        `./attachment.html?${queryParams.toString()}`;
}

/* =====================================================
   CREATE ACTION BUTTONS
===================================================== */

function createEditButton(
    lot,
    normalizedStatus
) {
    const editButton =
        document.createElement(
            "button"
        );

    editButton.type =
        "button";

    editButton.className =
        "btn btn-secondary btn-edit-lot";

    editButton.dataset.id =
        lot.id;

    editButton.textContent =
        "Sửa";

    if (normalizedStatus !== "DRAFT") {
        editButton.disabled =
            true;

        editButton.title =
            "Chỉ có thể sửa lô ở trạng thái DRAFT";
    }

    return editButton;
}

function createHistoryButton(lot) {
    const historyButton =
        document.createElement(
            "button"
        );

    historyButton.type =
        "button";

    historyButton.className =
        "btn btn-primary btn-history-lot";

    historyButton.dataset.id =
        lot.id;

    historyButton.textContent =
        "Lịch sử";

    historyButton.title =
        "Xem lịch sử nhật ký canh tác";

    return historyButton;
}

function createFarmLogButton(
    lot,
    normalizedStatus
) {
    const farmLogButton =
        document.createElement(
            "button"
        );

    farmLogButton.type =
        "button";

    farmLogButton.className =
        "btn btn-primary btn-create-farm-log";

    farmLogButton.dataset.id =
        lot.id;

    farmLogButton.textContent =
        "Ghi nhật ký";

    const canCreateFarmLog =
        normalizedStatus === "APPROVED" ||
        normalizedStatus === "HARVESTED";

    if (!canCreateFarmLog) {
        farmLogButton.disabled =
            true;

        farmLogButton.title =
            "Chỉ được ghi nhật ký cho lô ở trạng thái APPROVED hoặc HARVESTED";
    }

    return farmLogButton;
}

function createAttachmentButton(lot) {
    const attachmentButton =
        document.createElement(
            "button"
        );

    attachmentButton.type =
        "button";

    attachmentButton.className =
        "btn btn-secondary btn-attachment-lot";

    attachmentButton.dataset.id =
        lot.id;

    attachmentButton.textContent =
        "Attachment";

    attachmentButton.title =
        "Quản lý tệp đính kèm của lô sản xuất";

    return attachmentButton;
}

function createPackageButton(lot, normalizedStatus) {
    const packageButton = document.createElement("button");
    packageButton.type = "button";
    packageButton.className = "btn btn-success btn-package-lot";
    packageButton.dataset.id = lot.id;
    packageButton.textContent = "Đóng gói";
    packageButton.title = "Đóng gói lô sản xuất";
    if (normalizedStatus !== "HARVESTED") {
        packageButton.disabled = true;
        packageButton.title = "Chỉ có thể đóng gói lô ở trạng thái HARVESTED";
    }
    return packageButton;
}

function renderActionButtons(
    actionCell,
    lot,
    normalizedStatus
) {
    /*
     * VT-02: Quản lý hợp tác xã.
     * Có quyền sửa lô, xem lịch sử và quản lý attachment.
     */
    if (roleCode === "VT-02") {
        if (normalizedStatus === "DRAFT") {
            actionCell.appendChild(
                createEditButton(
                    lot,
                    normalizedStatus
                )
            );
        }

        if (normalizedStatus === "HARVESTED") {
            actionCell.appendChild(
                createPackageButton(lot, normalizedStatus)
            );
        }

        actionCell.appendChild(
            createHistoryButton(lot)
        );

        actionCell.appendChild(
            createAttachmentButton(lot)
        );

        return;
    }

    /*
     * VT-03: Người ghi nhật ký.
     * Có quyền ghi nhật ký và quản lý attachment.
     */
    if (roleCode === "VT-03") {
        const farmLogButton =
            createFarmLogButton(
                lot,
                normalizedStatus
            );

        const attachmentButton =
            createAttachmentButton(lot);

        actionCell.appendChild(
            farmLogButton
        );

        actionCell.appendChild(
            attachmentButton
        );

        if (normalizedStatus === "HARVESTED") {
            actionCell.appendChild(
                createPackageButton(lot, normalizedStatus)
            );
        }

        return;
    }

    /*
     * VT-01 hiện chưa được phân công thao tác
     * trong luồng này nên để dấu gạch ngang.
     */
    actionCell.textContent =
        "—";
}

/* =====================================================
   RENDER PRODUCTION LOT TABLE
===================================================== */

function renderProductionLots(lots) {
    if (
        !Array.isArray(lots) ||
        lots.length === 0
    ) {
        if (emptyState) {
            emptyState.style.display =
                "flex";
        }

        if (productionLotsTable) {
            productionLotsTable.style.display =
                "none";
        }

        return;
    }

    if (emptyState) {
        emptyState.style.display =
            "none";
    }

    if (productionLotsTable) {
        productionLotsTable.style.display =
            "table";
    }

    if (!productionLotsTableBody) {
        return;
    }

    productionLotsTableBody.innerHTML =
        "";

    lots.forEach(function (lot) {
        const row =
            document.createElement(
                "tr"
            );

        /* Tên lô */

        const nameCell =
            document.createElement(
                "td"
            );

        nameCell.textContent =
            lot.name || "—";

        /* Vùng trồng */

        const farmAreaCell =
            document.createElement(
                "td"
            );

        farmAreaCell.textContent =
            lot.farmAreaName || "—";

        /* Danh mục nông sản */

        const categoryCell =
            document.createElement(
                "td"
            );

        categoryCell.textContent =
            lot.productCategoryName ||
            "—";

        /* Sản lượng dự kiến */

        const quantityCell =
            document.createElement(
                "td"
            );

        quantityCell.textContent =
            lot.expectedQuantity != null
                ? String(
                    lot.expectedQuantity
                )
                : "—";

        /* Ngày gieo trồng */

        const plantingDateCell =
            document.createElement(
                "td"
            );

        plantingDateCell.textContent =
            formatDate(
                lot.plantingDate
            );

        /* Trạng thái */

        const statusCell =
            document.createElement(
                "td"
            );

        const statusBadge =
            document.createElement(
                "span"
            );

        statusBadge.className =
            "status-badge " +
            getStatusBadgeClass(
                lot.status
            );

        statusBadge.textContent =
            lot.status ||
            "DRAFT";

        statusCell.appendChild(
            statusBadge
        );

        /* Ngày tạo */

        const createdCell =
            document.createElement(
                "td"
            );

        createdCell.textContent =
            formatDateTime(
                lot.createdAt
            );

        /* Actions */

        const actionCell =
            document.createElement(
                "td"
            );

        actionCell.className =
            "production-lot-actions";

        const normalizedStatus =
            String(
                lot.status || ""
            )
                .trim()
                .toUpperCase();

        renderActionButtons(
            actionCell,
            lot,
            normalizedStatus
        );

        /* Thêm các ô vào hàng */

        row.appendChild(
            nameCell
        );

        row.appendChild(
            farmAreaCell
        );

        row.appendChild(
            categoryCell
        );

        row.appendChild(
            quantityCell
        );

        row.appendChild(
            plantingDateCell
        );

        row.appendChild(
            statusCell
        );

        row.appendChild(
            createdCell
        );

        row.appendChild(
            actionCell
        );

<<<<<<< HEAD
            actionGroup.appendChild(
                saveButton
            );

            actionGroup.appendChild(
                cancelButton
            );

            actionsCell.appendChild(
                actionGroup
            );
        } else if (
            isDraft ||
            String(lot.status)
                .toUpperCase() ===
                "PENDING"
        ) {
            createActionMenu(
                lot,
                actionsCell
            );
        } else {
            const lockedText =
                document.createElement(
                    "span"
                );

            lockedText.className =
                "action-locked";

            lockedText.textContent =
                "Locked";

            lockedText.title =
                "This production lot cannot be modified.";

            actionsCell.appendChild(
                lockedText
            );
        }

        var actionsCell = document.createElement("td");

var normalizedStatus = String(
    lot.status || ""
).toUpperCase();

var canCreateFarmLog =
    roleCode === "VT-03" &&
    (
        normalizedStatus === "APPROVED" ||
        normalizedStatus === "HARVESTED"
    );

if (canCreateFarmLog) {
    var farmLogButton =
        document.createElement("a");

    farmLogButton.className =
        "btn btn-primary btn-farm-log";

    farmLogButton.textContent =
        "Ghi nhật ký";

    farmLogButton.href =
        "/frontend/pages/cooperative/farm-logs/create.html" +
        "?productionLotId=" +
        encodeURIComponent(lot.id);

    actionsCell.appendChild(
        farmLogButton
    );
} else {
    actionsCell.textContent = "—";
}
        
        row.appendChild(nameCell);
        row.appendChild(farmAreaCell);
        row.appendChild(categoryCell);
        row.appendChild(qtyCell);
        row.appendChild(dateCell);
        row.appendChild(statusCell);
        row.appendChild(createdCell);
        row.appendChild(actionsCell);

        productionLotsTableBody
            .appendChild(row);
=======
        productionLotsTableBody.appendChild(
            row
        );
>>>>>>> feature/view-farm-log
    });
}

/* =====================================================
   MODAL SELECT HELPERS
===================================================== */

function fillSelect(
    selectElement,
    items,
    placeholder
) {
    if (!selectElement) {
        return;
    }

    selectElement.innerHTML =
        "";

    const defaultOption =
        document.createElement(
            "option"
        );

    defaultOption.value =
        "";

    defaultOption.textContent =
        placeholder;

    selectElement.appendChild(
        defaultOption
    );

    items.forEach(function (item) {
        const option =
            document.createElement(
                "option"
            );

        option.value =
            item.id;

        option.textContent =
            item.name;

        selectElement.appendChild(
            option
        );
    });
}

function loadEditSelectOptions() {
    fillSelect(
        editFields.farmAreaId,
        mockFarmAreas,
        "-- Chọn khu vực canh tác --"
    );

    fillSelect(
        editFields.productCategoryId,
        mockProductCategories,
        "-- Chọn loại nông sản --"
    );
}

/* =====================================================
   EDIT MODAL
===================================================== */

function openEditLotModal(lotId) {
    const lot =
        productionLots.find(
            function (item) {
                return (
                    item.id === lotId
                );
            }
        );

    if (
        !lot ||
        !editLotModal
    ) {
        return;
    }

    loadEditSelectOptions();

    if (editFields.id) {
        editFields.id.value =
            lot.id || "";
    }

    if (editFields.name) {
        editFields.name.value =
            lot.name || "";
    }

    if (editFields.farmAreaId) {
        editFields.farmAreaId.value =
            lot.farmAreaId || "";
    }

    if (
        editFields.productCategoryId
    ) {
        editFields.productCategoryId.value =
            lot.productCategoryId ||
            "";
    }

    if (
        editFields.expectedQuantity
    ) {
        editFields.expectedQuantity.value =
            lot.expectedQuantity ??
            "";
    }

    if (editFields.plantingDate) {
        editFields.plantingDate.value =
            lot.plantingDate || "";
    }

    if (editLotMessage) {
        editLotMessage.hidden =
            true;
    }

    editLotModal.hidden =
        false;

    document.body.classList.add(
        "modal-open"
    );
}

function closeEditLotModal() {
    if (!editLotModal) {
        return;
    }

    editLotModal.hidden =
        true;

    document.body.classList.remove(
        "modal-open"
    );

    if (editLotForm) {
        editLotForm.reset();
    }

    if (editLotMessage) {
        editLotMessage.hidden =
            true;
    }
}

/*
 * Hiện tại giữ nguyên logic cũ:
 * chỉ tạo payload và in ra Console,
 * chưa gọi API PUT cập nhật lô.
 */
function handleEditLotSubmit(event) {
    event.preventDefault();

    const lotId =
        editFields.id
            ? editFields.id.value
            : "";

    const payload = {
        farmAreaId:
            editFields.farmAreaId
                ? editFields
                    .farmAreaId
                    .value
                : "",

        productCategoryId:
            editFields
                .productCategoryId
                ? editFields
                    .productCategoryId
                    .value
                : "",

        name:
            editFields.name
                ? editFields
                    .name
                    .value
                    .trim()
                : "",

        expectedQuantity:
            editFields
                .expectedQuantity
                ? Number(
                    editFields
                        .expectedQuantity
                        .value
                )
                : 0,

        plantingDate:
            editFields
                .plantingDate
                ? editFields
                    .plantingDate
                    .value
                : ""
    };

    console.log(
        `PUT /api/v1/production-lots/${lotId}`
    );

    console.log(
        "Update payload:",
        payload
    );

    if (editLotMessage) {
        editLotMessage.textContent =
            "Giao diện đã sẵn sàng. Dữ liệu cập nhật đã được tạo trong Console.";

        editLotMessage.className =
            "modal-message success";

        editLotMessage.hidden =
            false;
    }
}

/* =====================================================
   NORMALIZE API DATA
===================================================== */

function extractProductionLots(
    response
) {
    if (!response) {
        return [];
    }

    /*
     * Trường hợp API trả:
     * {
     *   success: true,
     *   data: [...]
     * }
     */
    if (
        Array.isArray(
            response.data
        )
    ) {
        return response.data;
    }

    /*
     * Trường hợp API trả phân trang:
     * {
     *   data: {
     *     items: [...]
     *   }
     * }
     */
    if (
        response.data &&
        Array.isArray(
            response.data.items
        )
    ) {
        return response.data.items;
    }

    /*
     * Trường hợp service trả trực tiếp mảng.
     */
    if (Array.isArray(response)) {
        return response;
    }

    return [];
}

/* =====================================================
   LOAD PRODUCTION LOTS
===================================================== */

async function loadProductionLots() {
    if (loadingState) {
        loadingState.style.display =
            "flex";
    }

    if (errorState) {
        errorState.style.display =
            "none";
    }

    if (mainContent) {
        mainContent.style.display =
            "none";
    }

    try {
        if (USE_MOCK_DATA) {
            productionLots = [
                {
                    id:
                        "lot-001",

                    name:
                        "Lô cà chua vụ đông 2026",

                    farmAreaId:
                        "farm-001",

                    farmAreaName:
                        "Khu vực canh tác A1",

                    productCategoryId:
                        "category-001",

                    productCategoryName:
                        "Cà chua",

                    expectedQuantity:
                        500,

                    plantingDate:
                        "2026-08-01",

                    status:
                        "DRAFT",

                    createdAt:
                        "2026-07-21T10:00:00"
                },
                {
                    id:
                        "lot-002",

                    name:
                        "Lô xoài đợt 1 năm 2026",

                    farmAreaId:
                        "farm-002",

                    farmAreaName:
                        "Khu vực canh tác B1",

                    productCategoryId:
                        "category-002",

                    productCategoryName:
                        "Xoài Cát Chu",

                    expectedQuantity:
                        1200,

                    plantingDate:
                        "2026-07-25",

                    status:
                        "APPROVED",

                    createdAt:
                        "2026-07-20T08:30:00"
                }
            ];
        } else {
            const response =
                await getProductionLots();

            if (
                response &&
                response.success === false
            ) {
                throw new Error(
                    response.message ||
                    "Không thể tải danh sách lô sản xuất."
                );
            }

            productionLots =
                extractProductionLots(
                    response
                );
        }

        if (loadingState) {
            loadingState.style.display =
                "none";
        }

        if (mainContent) {
            mainContent.style.display =
                "block";
        }

        renderProductionLots(
            productionLots
        );
    } catch (error) {
        console.error(
            "Load production lots error:",
            error
        );

        if (loadingState) {
            loadingState.style.display =
                "none";
        }

        if (mainContent) {
            mainContent.style.display =
                "none";
        }

        let message =
            error.message ||
            "Đã xảy ra lỗi khi tải danh sách lô sản xuất.";

        const normalizedMessage =
            String(message)
                .toLowerCase();

        if (
            normalizedMessage.includes(
                "404"
            ) ||
            normalizedMessage.includes(
                "not found"
            )
        ) {
            if (mainContent) {
                mainContent.style.display =
                    "block";
            }

            renderProductionLots(
                []
            );

            return;
        }

        if (
            normalizedMessage.includes(
                "403"
            )
        ) {
            message =
                "Bạn không có quyền xem danh sách lô sản xuất.";
        }

        if (errorMessage) {
            errorMessage.textContent =
                message;
        }

        if (errorState) {
            errorState.style.display =
                "flex";
        }
    }
}

/* =====================================================
   TABLE ACTION EVENTS
===================================================== */

if (productionLotsTableBody) {
    productionLotsTableBody.addEventListener(
        "click",
        function (event) {
            const target =
                event.target;

            if (
                !(target instanceof HTMLElement)
            ) {
                return;
            }

            const button =
                target.closest("button");

            if (!button) {
                return;
            }

            const lotId =
                button.dataset.id;

            if (!lotId) {
                return;
            }

            const lot =
                productionLots.find(
                    function (item) {
                        return (
                            String(item.id) ===
                            String(lotId)
                        );
                    }
                );

            if (!lot) {
                console.warn(
                    "Không tìm thấy lô sản xuất:",
                    lotId
                );

                return;
            }

            /*
             * Nút sửa lô
             */
            if (
                button.classList.contains(
                    "btn-edit-lot"
                )
            ) {
                openEditLotModal(
                    lotId
                );

                return;
            }

            /*
             * Nút xem lịch sử nhật ký
             */
            if (
                button.classList.contains(
                    "btn-history-lot"
                )
            ) {
                goToFarmLogHistory(
                    lot
                );

                return;
            }

            /*
             * Nút ghi nhật ký canh tác
             */
            if (
                button.classList.contains(
                    "btn-create-farm-log"
                )
            ) {
                goToCreateFarmLog(
                    lot
                );

                return;
            }

            /*
             * Nút quản lý tệp đính kèm
             */
            if (
                button.classList.contains(
                    "btn-attachment-lot"
                )
            ) {
                goToAttachments(lot);

                return;
            }

            /*
             * Nút đóng gói lô sản xuất
             */
            if (
                button.classList.contains(
                    "btn-package-lot"
                )
            ) {
                if (confirm(`Bạn có chắc chắn muốn đóng gói lô sản xuất "${lot.name}" không?`)) {
                    button.disabled = true;
                    packageProductionLot(lotId)
                        .then(response => {
                            showToast("Đóng gói lô sản xuất thành công!");
                            loadProductionLots(); // refresh table
                        })
                        .catch(error => {
                            console.error("Lỗi đóng gói:", error);
                            showToast(error.message || "Đóng gói thất bại. Vui lòng kiểm tra lại nhật ký.", "error");
                            button.disabled = false;
                        });
                }
                return;
            }
        }
    );
}

/* =====================================================
   MODAL EVENTS
===================================================== */

if (editLotForm) {
    editLotForm.addEventListener(
        "submit",
        handleEditLotSubmit
    );
}

if (closeEditLotButton) {
    closeEditLotButton.addEventListener(
        "click",
        closeEditLotModal
    );
}

if (cancelEditLotButton) {
    cancelEditLotButton.addEventListener(
        "click",
        closeEditLotModal
    );
}

if (editLotOverlay) {
    editLotOverlay.addEventListener(
        "click",
        closeEditLotModal
    );
}

document.addEventListener(
    "keydown",
    function (event) {
        if (
            event.key === "Escape" &&
            editLotModal &&
            !editLotModal.hidden
        ) {
            closeEditLotModal();
        }
    }
);

/* =====================================================
   RETRY EVENT
===================================================== */

if (retryButton) {
    retryButton.addEventListener(
        "click",
        loadProductionLots
    );
}

/* =====================================================
   SEARCH
===================================================== */

const productionLotSearchInput =
    document.getElementById(
        "productionLotSearchInput"
    );

if (productionLotSearchInput) {
    productionLotSearchInput.addEventListener(
        "input",
        function (event) {
            const keyword =
                String(
                    event.target.value ||
                    ""
                )
                    .trim()
                    .toLowerCase();

            if (!keyword) {
                renderProductionLots(
                    productionLots
                );

                return;
            }

            const filteredLots =
                productionLots.filter(
                    function (lot) {
                        const searchableText = [
                            lot.name,
                            lot.farmAreaName,
                            lot.productCategoryName,
                            lot.status,
                            lot.expectedQuantity
                        ]
                            .filter(
                                function (value) {
                                    return (
                                        value !== null &&
                                        value !== undefined
                                    );
                                }
                            )
                            .join(" ")
                            .toLowerCase();

                        return searchableText.includes(
                            keyword
                        );
                    }
                );

            renderProductionLots(
                filteredLots
            );
        }
    );
}

/* =====================================================
   CREATE PRODUCTION LOT BUTTON
===================================================== */

const createProductionLotButton =
    document.getElementById(
        "createProductionLotButton"
    );

if (createProductionLotButton) {
    createProductionLotButton.addEventListener(
        "click",
        function () {
            window.location.href =
                "./create.html";
        }
    );
}

/* =====================================================
   LOGOUT
===================================================== */

setupLogout();

/* =====================================================
   INITIALIZE PAGE
===================================================== */

function showToast(message, type = "success") {
    let container = document.getElementById("toast-container");
    if (!container) {
        container = document.createElement("div");
        container.id = "toast-container";
        container.style.position = "fixed";
        container.style.top = "20px";
        container.style.right = "20px";
        container.style.zIndex = "9999";
        container.style.display = "flex";
        container.style.flexDirection = "column";
        container.style.gap = "10px";
        document.body.appendChild(container);
    }
    
    const toast = document.createElement("div");
    toast.className = `toast toast-${type}`;
    toast.style.background = type === "success" ? "#28a745" : "#dc3545";
    toast.style.color = "#fff";
    toast.style.padding = "12px 20px";
    toast.style.borderRadius = "8px";
    toast.style.boxShadow = "0 4px 12px rgba(0,0,0,0.15)";
    toast.style.minWidth = "250px";
    toast.style.opacity = "0";
    toast.style.transition = "opacity 0.3s ease, transform 0.3s ease";
    toast.style.transform = "translateY(-10px)";
    toast.style.fontFamily = "sans-serif";
    toast.style.fontSize = "14px";
    toast.style.display = "flex";
    toast.style.alignItems = "center";
    toast.style.justifyContent = "space-between";
    
    const textSpan = document.createElement("span");
    textSpan.textContent = message;
    toast.appendChild(textSpan);
    
    const closeBtn = document.createElement("button");
    closeBtn.textContent = "×";
    closeBtn.style.background = "none";
    closeBtn.style.border = "none";
    closeBtn.style.color = "#fff";
    closeBtn.style.fontSize = "18px";
    closeBtn.style.cursor = "pointer";
    closeBtn.style.marginLeft = "10px";
    closeBtn.onclick = () => toast.remove();
    toast.appendChild(closeBtn);
    
    container.appendChild(toast);
    
    setTimeout(() => {
        toast.style.opacity = "1";
        toast.style.transform = "translateY(0)";
    }, 10);
    
    setTimeout(() => {
        toast.style.opacity = "0";
        toast.style.transform = "translateY(-10px)";
        setTimeout(() => toast.remove(), 300);
    }, 6000);
}

function initializeWebSocket() {
    const token = getToken();
    const orgId = user ? user.organizationId : null;
    if (!token || !orgId) {
        console.warn("No token or organizationId, WebSocket connection skipped.");
        return;
    }

    if (typeof StompJs === "undefined") {
        console.error("StompJs library not loaded.");
        return;
    }

    const client = new StompJs.Client({
        brokerURL: `ws://${window.location.host}/ws`,
        connectHeaders: {
            Authorization: `Bearer ${token}`
        },
        debug: function (str) {
            console.log("[STOMP]", str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000
    });

    client.onConnect = function (frame) {
        console.log("Connected to WebSocket successfully!");
        client.subscribe(`/topic/notifications/${orgId}`, function (message) {
            console.log("Received websocket notification:", message.body);
            try {
                const payload = JSON.parse(message.body);
                if (payload.type === "PACKAGING_FAILED") {
                    showToast(payload.message, "error");
                }
            } catch (e) {
                console.error("Failed to parse websocket message", e);
            }
        });
    };

    client.onStompError = function (frame) {
        console.error("Broker reported error: " + frame.headers["message"]);
        console.error("Additional details: " + frame.body);
    };

    client.activate();
}

loadProductionLots();
initializeWebSocket();