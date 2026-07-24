import {
    requireAuth,
    setupLogout
} from "../../../core/auth-guard.js";

import {
    getUser
} from "../../../core/storage.js";

import {
    getProductionLots,
    getFarmAreas,
    getProductCategories,
    updateProductionLot,
    submitProductionLot,
    approveProductionLot,
    returnToDraftProductionLot
} from "../../../services/production-lot.service.js";

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
   PAGE STATE
===================================================== */

let productionLots = [];

/*
 * Danh sách vùng trồng thật lấy từ API.
 */
let farmAreas = [];

/*
 * Danh sách loại nông sản thật lấy từ API.
 */
let productCategories = [];

/*
 * ID của lô đang được sửa trực tiếp trên bảng.
 * null nghĩa là hiện không có dòng nào đang sửa.
 */
let editingLotId = null;

/*
 * ID lô PENDING đang mở lựa chọn
 * Duyệt hoặc Từ chối.
 */
let reviewingLotId = null;

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

        return;
    }

    /*
     * VT-01 hiện chưa được phân công thao tác
     * trong luồng này nên để dấu gạch ngang.
     */
    actionCell.textContent =
        "—";
}

function getOptionId(item) {
    if (!item) {
        return "";
    }

    return (
        item.id ||
        item.farmAreaId ||
        item.productCategoryId ||
        ""
    );
}

function getOptionName(item) {
    if (!item) {
        return "—";
    }

    return (
        item.name ||
        item.farmAreaName ||
        item.productCategoryName ||
        "—"
    );
}

function createInlineInput(
    field,
    type,
    value,
    options = {}
) {
    const input =
        document.createElement("input");

    input.type = type;
    input.dataset.field = field;
    input.className =
        "inline-edit-input";

    input.value =
        value ?? "";

    if (options.min !== undefined) {
        input.min =
            String(options.min);
    }

    if (options.step !== undefined) {
        input.step =
            String(options.step);
    }

    return input;
}

function createInlineSelect(
    field,
    items,
    selectedId,
    selectedName,
    placeholder
) {
    const select =
        document.createElement("select");

    select.dataset.field = field;
    select.className =
        "inline-edit-select";

    const placeholderOption =
        document.createElement("option");

    placeholderOption.value = "";
    placeholderOption.textContent =
        placeholder;

    select.appendChild(
        placeholderOption
    );

    items.forEach(function (item) {
        const option =
            document.createElement("option");

        const itemId =
            getOptionId(item);

        const itemName =
            getOptionName(item);

        option.value =
            itemId;

        option.textContent =
            itemName;

        const selectedById =
            selectedId &&
            String(itemId) ===
            String(selectedId);

        const selectedByName =
            !selectedId &&
            selectedName &&
            String(itemName)
                .trim()
                .toLowerCase() ===
            String(selectedName)
                .trim()
                .toLowerCase();

        if (
            selectedById ||
            selectedByName
        ) {
            option.selected = true;
        }

        select.appendChild(option);
    });

    return select;
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
        const normalizedStatus =
            String(
                lot.status || "DRAFT"
            )
                .trim()
                .toUpperCase();

        const isEditing =
            String(editingLotId) ===
            String(lot.id);

        const row =
            document.createElement("tr");

        row.dataset.id =
            lot.id;

        if (isEditing) {
            row.classList.add(
                "production-lot-row-editing"
            );
        }

        /* =========================
           TÊN LÔ
        ========================= */

        const nameCell =
            document.createElement("td");

        if (isEditing) {
            const nameInput =
                createInlineInput(
                    "name",
                    "text",
                    lot.name
                );

            nameInput.required = true;
            nameInput.maxLength = 255;

            nameCell.appendChild(
                nameInput
            );
        } else {
            nameCell.textContent =
                lot.name || "—";
        }

        /* =========================
           VÙNG TRỒNG
        ========================= */

        const farmAreaCell =
            document.createElement("td");

        if (isEditing) {
            const farmAreaSelect =
                createInlineSelect(
                    "farmAreaId",
                    farmAreas,
                    lot.farmAreaId,
                    lot.farmAreaName,
                    "-- Chọn vùng trồng --"
                );

            farmAreaCell.appendChild(
                farmAreaSelect
            );
        } else {
            farmAreaCell.textContent =
                lot.farmAreaName || "—";
        }

        /* =========================
           LOẠI NÔNG SẢN
        ========================= */

        const categoryCell =
            document.createElement("td");

        if (isEditing) {
            const categorySelect =
                createInlineSelect(
                    "productCategoryId",
                    productCategories,
                    lot.productCategoryId,
                    lot.productCategoryName,
                    "-- Chọn loại nông sản --"
                );

            categoryCell.appendChild(
                categorySelect
            );
        } else {
            categoryCell.textContent =
                lot.productCategoryName ||
                "—";
        }

        /* =========================
           SẢN LƯỢNG DỰ KIẾN
        ========================= */

        const quantityCell =
            document.createElement("td");

        if (isEditing) {
            const quantityInput =
                createInlineInput(
                    "expectedQuantity",
                    "number",
                    lot.expectedQuantity,
                    {
                        min: 0.01,
                        step: 0.01
                    }
                );

            quantityInput.required = true;

            quantityCell.appendChild(
                quantityInput
            );
        } else {
            quantityCell.textContent =
                lot.expectedQuantity != null
                    ? String(
                        lot.expectedQuantity
                    )
                    : "—";
        }

        /* =========================
           NGÀY GIEO TRỒNG
        ========================= */

        const plantingDateCell =
            document.createElement("td");

        if (isEditing) {
            const plantingDateInput =
                createInlineInput(
                    "plantingDate",
                    "date",
                    lot.plantingDate
                );

            plantingDateInput.required =
                true;

            plantingDateCell.appendChild(
                plantingDateInput
            );
        } else {
            plantingDateCell.textContent =
                formatDate(
                    lot.plantingDate
                );
        }

        /* =========================
           TRẠNG THÁI
        ========================= */

        const statusCell =
    document.createElement("td");

const isReviewing =
    normalizedStatus === "PENDING" &&
    String(reviewingLotId) ===
    String(lot.id);

if (isReviewing) {
    const reviewActions =
        document.createElement("div");

    reviewActions.className =
        "status-review-actions";

    const approveButton =
        document.createElement("button");

    approveButton.type = "button";
    approveButton.dataset.id = lot.id;
    approveButton.className =
        "btn btn-primary btn-approve-inline";
    approveButton.textContent =
        "Duyệt";

    const rejectButton =
        document.createElement("button");

    rejectButton.type = "button";
    rejectButton.dataset.id = lot.id;
    rejectButton.className =
        "btn btn-secondary btn-reject-inline";
    rejectButton.textContent =
        "Từ chối";

    const cancelReviewButton =
        document.createElement("button");

    cancelReviewButton.type = "button";
    cancelReviewButton.dataset.id = lot.id;
    cancelReviewButton.className =
        "btn btn-secondary btn-cancel-review";
    cancelReviewButton.textContent =
        "Hủy";

    reviewActions.append(
        approveButton,
        rejectButton,
        cancelReviewButton
    );

    statusCell.appendChild(
        reviewActions
    );
} else {
    const statusButton =
        document.createElement("button");

    statusButton.type =
        "button";

    statusButton.dataset.id =
        lot.id;

    statusButton.dataset.status =
        normalizedStatus;

    statusButton.className =
        "status-badge status-button " +
        getStatusBadgeClass(
            normalizedStatus
        );

    statusButton.textContent =
        normalizedStatus;

    const canSubmitForApproval =
        roleCode === "VT-02" &&
        normalizedStatus === "DRAFT" &&
        editingLotId === null &&
        reviewingLotId === null;

    const canReviewPending =
        roleCode === "VT-02" &&
        normalizedStatus === "PENDING" &&
        editingLotId === null;

    if (canSubmitForApproval) {
        statusButton.classList.add(
            "status-button-clickable"
        );

        statusButton.title =
            "Bấm để gửi lô sang trạng thái PENDING";
    } else if (canReviewPending) {
        statusButton.classList.add(
            "status-pending-clickable"
        );

        statusButton.title =
            "Bấm để duyệt hoặc từ chối lô";
    } else {
        statusButton.disabled =
            true;
    }

    statusCell.appendChild(
        statusButton
    );
}

        /* =========================
           NGÀY TẠO
        ========================= */

        const createdCell =
            document.createElement("td");

        createdCell.textContent =
            formatDateTime(
                lot.createdAt
            );

        /* =========================
           ACTIONS
        ========================= */

        const actionCell =
            document.createElement("td");

        actionCell.className =
            "production-lot-actions";

        if (
            roleCode === "VT-02" &&
            normalizedStatus === "DRAFT"
        ) {
            if (isEditing) {
                const saveButton =
                    document.createElement(
                        "button"
                    );

                saveButton.type =
                    "button";

                saveButton.dataset.id =
                    lot.id;

                saveButton.className =
                    "btn btn-primary btn-save-inline";

                saveButton.textContent =
                    "Lưu";

                const cancelButton =
                    document.createElement(
                        "button"
                    );

                cancelButton.type =
                    "button";

                cancelButton.dataset.id =
                    lot.id;

                cancelButton.className =
                    "btn btn-secondary btn-cancel-inline";

                cancelButton.textContent =
                    "Hủy";

                actionCell.appendChild(
                    saveButton
                );

                actionCell.appendChild(
                    cancelButton
                );
            } else {
                const editButton =
                    createEditButton(
                        lot,
                        normalizedStatus
                    );

                /*
                 * Không cho mở dòng khác khi
                 * đang sửa một dòng.
                 */
                if (editingLotId !== null) {
                    editButton.disabled =
                        true;

                    editButton.title =
                        "Hãy lưu hoặc hủy dòng đang sửa trước";
                }

                actionCell.appendChild(
                    editButton
                );

                actionCell.appendChild(
                    createHistoryButton(
                        lot
                    )
                );

                actionCell.appendChild(
                    createAttachmentButton(
                        lot
                    )
                );
            }
        } else {
            renderActionButtons(
                actionCell,
                lot,
                normalizedStatus
            );
        }

        row.appendChild(nameCell);
        row.appendChild(farmAreaCell);
        row.appendChild(categoryCell);
        row.appendChild(quantityCell);
        row.appendChild(plantingDateCell);
        row.appendChild(statusCell);
        row.appendChild(createdCell);
        row.appendChild(actionCell);

        productionLotsTableBody.appendChild(
            row
        );
    });
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

function extractListData(response) {
    if (!response) {
        return [];
    }

    /*
     * API trả:
     * {
     *     success: true,
     *     data: [...]
     * }
     */
    if (Array.isArray(response.data)) {
        return response.data;
    }

    /*
     * API trả:
     * {
     *     data: {
     *         items: [...]
     *     }
     * }
     */
    if (
        response.data &&
        Array.isArray(response.data.items)
    ) {
        return response.data.items;
    }

    /*
     * Một số API phân trang Spring trả:
     * {
     *     data: {
     *         content: [...]
     *     }
     * }
     */
    if (
        response.data &&
        Array.isArray(response.data.content)
    ) {
        return response.data.content;
    }

    /*
     * Service trả trực tiếp mảng.
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
        /*
         * Gọi đồng thời 3 API:
         *
         * 1. Danh sách lô sản xuất.
         * 2. Danh sách vùng trồng.
         * 3. Danh sách loại nông sản.
         */
        const [
            productionLotsResponse,
            farmAreasResponse,
            productCategoriesResponse
        ] = await Promise.all([
            getProductionLots(),
            getFarmAreas(),
            getProductCategories()
        ]);

        /*
         * Kiểm tra lỗi API danh sách lô.
         */
        if (
            productionLotsResponse &&
            productionLotsResponse.success === false
        ) {
            throw new Error(
                productionLotsResponse.message ||
                "Không thể tải danh sách lô sản xuất."
            );
        }

        /*
         * Kiểm tra lỗi API vùng trồng.
         */
        if (
            farmAreasResponse &&
            farmAreasResponse.success === false
        ) {
            throw new Error(
                farmAreasResponse.message ||
                "Không thể tải danh sách vùng trồng."
            );
        }

        /*
         * Kiểm tra lỗi API loại nông sản.
         */
        if (
            productCategoriesResponse &&
            productCategoriesResponse.success === false
        ) {
            throw new Error(
                productCategoriesResponse.message ||
                "Không thể tải danh sách loại nông sản."
            );
        }

        productionLots =
            extractProductionLots(
                productionLotsResponse
            );

        farmAreas =
            extractListData(
                farmAreasResponse
            );

        productCategories =
            extractListData(
                productCategoriesResponse
            );

        /*
         * Sau khi tải lại dữ liệu,
         * đóng trạng thái sửa hiện tại.
         */
        editingLotId = null;

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
            "Đã xảy ra lỗi khi tải dữ liệu lô sản xuất.";

        const normalizedMessage =
            String(message)
                .toLowerCase();

        /*
         * Không có lô sản xuất thì vẫn
         * hiển thị trang với empty state.
         */
        if (
            normalizedMessage.includes(
                "404"
            ) ||
            normalizedMessage.includes(
                "not found"
            )
        ) {
            productionLots = [];

            if (mainContent) {
                mainContent.style.display =
                    "block";
            }

            renderProductionLots([]);

            return;
        }

        if (
            normalizedMessage.includes(
                "403"
            )
        ) {
            message =
                "Bạn không có quyền tải dữ liệu lô sản xuất.";
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
        async function (event) {
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

            const normalizedStatus =
                String(
                    lot.status || ""
                )
                    .trim()
                    .toUpperCase();

            /* =========================================
               BẤM DRAFT ĐỂ GỬI DUYỆT
            ========================================= */

            if (
                button.classList.contains(
                    "status-button-clickable"
                )
            ) {
                if (
                    roleCode !== "VT-02" ||
                    normalizedStatus !== "DRAFT"
                ) {
                    return;
                }

                if (editingLotId !== null) {
                    alert(
                        "Hãy lưu hoặc hủy dòng đang sửa trước."
                    );

                    return;
                }

                const confirmed =
                    window.confirm(
                        "Bạn có chắc muốn gửi lô sản xuất này để duyệt?\n\nTrạng thái sẽ chuyển từ DRAFT sang PENDING."
                    );

                if (!confirmed) {
                    return;
                }

                try {
                    button.disabled =
                        true;

                    button.textContent =
                        "Đang gửi...";

                    const response =
                        await submitProductionLot(
                            lot.id
                        );

                    if (
                        !response ||
                        response.success === false
                    ) {
                        throw new Error(
                            response?.message ||
                            "Không thể gửi duyệt lô sản xuất."
                        );
                    }

                    const responseData =
                        response.data ||
                        response;

                    lot.status =
                        responseData.status ||
                        "PENDING";

                    renderProductionLots(
                        productionLots
                    );
                } catch (error) {
                    console.error(
                        "Submit production lot error:",
                        error
                    );

                    alert(
                        error.message ||
                        "Không thể gửi duyệt lô sản xuất."
                    );

                    button.disabled =
                        false;

                    button.textContent =
                        "DRAFT";
                }

                return;
            }

            /* =========================================
   MỞ LỰA CHỌN DUYỆT LÔ PENDING
========================================= */

if (
    button.classList.contains(
        "status-pending-clickable"
    )
) {
    if (
        roleCode !== "VT-02" ||
        normalizedStatus !== "PENDING"
    ) {
        return;
    }

    reviewingLotId =
        lot.id;

    renderProductionLots(
        productionLots
    );

    return;
}

/* =========================================
   HỦY LỰA CHỌN DUYỆT
========================================= */

if (
    button.classList.contains(
        "btn-cancel-review"
    )
) {
    reviewingLotId =
        null;

    renderProductionLots(
        productionLots
    );

    return;
}

/* =========================================
   DUYỆT LÔ: PENDING -> APPROVED
========================================= */

if (
    button.classList.contains(
        "btn-approve-inline"
    )
) {
    const confirmed =
        window.confirm(
            "Bạn có chắc muốn duyệt lô sản xuất này?"
        );

    if (!confirmed) {
        return;
    }

    try {
        button.disabled =
            true;

        button.textContent =
            "Đang duyệt...";

        const response =
            await approveProductionLot(
                lot.id
            );

        if (
            !response ||
            response.success === false
        ) {
            throw new Error(
                response?.message ||
                "Không thể duyệt lô sản xuất."
            );
        }

        const responseData =
            response.data ||
            response;

        lot.status =
            responseData.status ||
            "APPROVED";

        reviewingLotId =
            null;

        renderProductionLots(
            productionLots
        );
    } catch (error) {
        console.error(
            "Approve production lot error:",
            error
        );

        alert(
            error.message ||
            "Không thể duyệt lô sản xuất."
        );

        button.disabled =
            false;

        button.textContent =
            "Duyệt";
    }

    return;
}

/* =========================================
   TỪ CHỐI: PENDING -> DRAFT
========================================= */

if (
    button.classList.contains(
        "btn-reject-inline"
    )
) {
    const reason =
        window.prompt(
            "Nhập lý do từ chối lô sản xuất:"
        );

    if (reason === null) {
        return;
    }

    const normalizedReason =
        reason.trim();

    if (!normalizedReason) {
        alert(
            "Vui lòng nhập lý do từ chối."
        );

        return;
    }

    try {
        button.disabled =
            true;

        button.textContent =
            "Đang xử lý...";

        const response =
            await returnToDraftProductionLot(
                lot.id,
                normalizedReason
            );

        if (
            !response ||
            response.success === false
        ) {
            throw new Error(
                response?.message ||
                "Không thể trả lại lô sản xuất."
            );
        }

        const responseData =
            response.data ||
            response;

        lot.status =
            responseData.status ||
            "DRAFT";

        lot.approvalNotes =
            responseData.approvalNotes ||
            normalizedReason;

        reviewingLotId =
            null;

        renderProductionLots(
            productionLots
        );
    } catch (error) {
        console.error(
            "Reject production lot error:",
            error
        );

        alert(
            error.message ||
            "Không thể trả lại lô sản xuất."
        );

        button.disabled =
            false;

        button.textContent =
            "Từ chối";
    }

    return;
}

            /* =========================================
               BẤM SỬA TRỰC TIẾP TRÊN DÒNG
            ========================================= */

            if (
                button.classList.contains(
                    "btn-edit-lot"
                )
            ) {
                if (
                    roleCode !== "VT-02"
                ) {
                    alert(
                        "Bạn không có quyền sửa lô sản xuất."
                    );

                    return;
                }

                if (
                    normalizedStatus !== "DRAFT"
                ) {
                    alert(
                        "Chỉ có thể sửa lô ở trạng thái DRAFT."
                    );

                    return;
                }

                if (
                    editingLotId !== null &&
                    String(editingLotId) !==
                    String(lot.id)
                ) {
                    alert(
                        "Hãy lưu hoặc hủy dòng đang sửa trước."
                    );

                    return;
                }

                editingLotId =
                    lot.id;

                renderProductionLots(
                    productionLots
                );

                const editingRow =
                    productionLotsTableBody.querySelector(
                        `tr[data-id="${lot.id}"]`
                    );

                const nameInput =
                    editingRow?.querySelector(
                        '[data-field="name"]'
                    );

                if (nameInput) {
                    nameInput.focus();
                    nameInput.select();
                }

                return;
            }

            /* =========================================
               HỦY SỬA
            ========================================= */

            if (
                button.classList.contains(
                    "btn-cancel-inline"
                )
            ) {
                editingLotId =
                    null;

                renderProductionLots(
                    productionLots
                );

                return;
            }

            /* =========================================
               LƯU THÔNG TIN ĐÃ SỬA
            ========================================= */

            if (
                button.classList.contains(
                    "btn-save-inline"
                )
            ) {
                if (
                    roleCode !== "VT-02" ||
                    normalizedStatus !== "DRAFT"
                ) {
                    alert(
                        "Bạn không có quyền cập nhật lô này."
                    );

                    return;
                }

                const row =
                    button.closest("tr");

                if (!row) {
                    alert(
                        "Không tìm thấy dòng dữ liệu cần cập nhật."
                    );

                    return;
                }

                const nameInput =
                    row.querySelector(
                        '[data-field="name"]'
                    );

                const farmAreaSelect =
                    row.querySelector(
                        '[data-field="farmAreaId"]'
                    );

                const categorySelect =
                    row.querySelector(
                        '[data-field="productCategoryId"]'
                    );

                const quantityInput =
                    row.querySelector(
                        '[data-field="expectedQuantity"]'
                    );

                const plantingDateInput =
                    row.querySelector(
                        '[data-field="plantingDate"]'
                    );

                const payload = {
                    name:
                        String(
                            nameInput?.value || ""
                        ).trim(),

                    farmAreaId:
                        String(
                            farmAreaSelect?.value ||
                            ""
                        ).trim(),

                    productCategoryId:
                        String(
                            categorySelect?.value ||
                            ""
                        ).trim(),

                    expectedQuantity:
                        Number(
                            quantityInput?.value
                        ),

                    plantingDate:
                        String(
                            plantingDateInput?.value ||
                            ""
                        ).trim()
                };

                if (!payload.name) {
                    alert(
                        "Vui lòng nhập tên lô sản xuất."
                    );

                    nameInput?.focus();

                    return;
                }

                if (!payload.farmAreaId) {
                    alert(
                        "Vui lòng chọn vùng trồng."
                    );

                    farmAreaSelect?.focus();

                    return;
                }

                if (
                    !payload.productCategoryId
                ) {
                    alert(
                        "Vui lòng chọn loại nông sản."
                    );

                    categorySelect?.focus();

                    return;
                }

                if (
                    !Number.isFinite(
                        payload.expectedQuantity
                    ) ||
                    payload.expectedQuantity <= 0
                ) {
                    alert(
                        "Sản lượng dự kiến phải lớn hơn 0."
                    );

                    quantityInput?.focus();

                    return;
                }

                if (!payload.plantingDate) {
                    alert(
                        "Vui lòng chọn ngày gieo trồng."
                    );

                    plantingDateInput?.focus();

                    return;
                }

                try {
                    button.disabled =
                        true;

                    button.textContent =
                        "Đang lưu...";

                    const response =
                        await updateProductionLot(
                            lot.id,
                            payload
                        );

                    if (
                        !response ||
                        response.success === false
                    ) {
                        throw new Error(
                            response?.message ||
                            "Không thể cập nhật lô sản xuất."
                        );
                    }

                    editingLotId =
                        null;

                    await loadProductionLots();
                } catch (error) {
                    console.error(
                        "Update production lot error:",
                        error
                    );

                    alert(
                        error.message ||
                        "Không thể cập nhật lô sản xuất."
                    );

                    button.disabled =
                        false;

                    button.textContent =
                        "Lưu";
                }

                return;
            }

            /* =========================================
               XEM LỊCH SỬ NHẬT KÝ
            ========================================= */

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

            /* =========================================
               GHI NHẬT KÝ CANH TÁC
            ========================================= */

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

            /* =========================================
               QUẢN LÝ TỆP ĐÍNH KÈM
            ========================================= */

            if (
                button.classList.contains(
                    "btn-attachment-lot"
                )
            ) {
                goToAttachments(
                    lot
                );
            }
        }
    );
}

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

loadProductionLots();