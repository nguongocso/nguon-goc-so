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
    createFarmLog
} from "../../../services/farm-log.service.js";

const LOGIN_URL =
    "/frontend/pages/auth/login.html";

const PRODUCTION_LOT_LIST_URL =
    "/frontend/pages/cooperative/production-lots/index.html";

const ALLOWED_ROLE = "VT-03";

const ALLOWED_STATUSES = [
    "APPROVED",
    "HARVESTED"
];

const ACTIVITY_LABELS = {
    PLANTING: "Gieo trồng",
    WATERING: "Tưới nước",
    FERTILIZING: "Bón phân",
    PESTICIDE: "Phun thuốc",
    WEEDING: "Làm cỏ",
    HARVESTING: "Thu hoạch",
    OTHER: "Khác"
};

if (!requireAuth()) {
    throw new Error(
        "Người dùng chưa đăng nhập."
    );
}

const user = getUser();

function setupSidebarByRole() {
    if (user.roleCode !== "VT-03") {
        return;
    }

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

setupSidebarByRole();

if (!user || !user.roleCode) {
    window.location.href = LOGIN_URL;

    throw new Error(
        "Không tìm thấy thông tin người dùng."
    );
}

const getElement = function (id) {
    return document.getElementById(id);
};

const elements = {
    loadingState: getElement("loadingState"),
    errorState: getElement("errorState"),
    errorMessage: getElement("errorMessage"),
    unauthorizedState: getElement("unauthorizedState"),
    mainContent: getElement("mainContent"),

    form: getElement("farmLogForm"),
    formMessage: getElement("formMessage"),
    submitButton: getElement("submitButton"),
    retryButton: getElement("retryButton"),
    backButton: getElement("backButton"),
    cancelButton: getElement("cancelButton"),

    productionLot: getElement("productionLotId"),
    activityType: getElement("activityType"),
    material: getElement("material"),
    quantity: getElement("quantity"),
    unit: getElement("unit"),
    executedDate: getElement("executedDate"),
    notes: getElement("notes"),
    notesCounter: getElement("notesCounter"),

    summaryProductionLot:
        getElement("summaryProductionLot"),

    summaryActivityType:
        getElement("summaryActivityType"),

    summaryMaterial:
        getElement("summaryMaterial"),

    summaryQuantity:
        getElement("summaryQuantity"),

    summaryUnit:
        getElement("summaryUnit"),

    summaryExecutedDate:
        getElement("summaryExecutedDate"),

    summaryNotes:
        getElement("summaryNotes")
};

const productionLotIdFromUrl =
    new URLSearchParams(
        window.location.search
    ).get("productionLotId");

function hideElement(element) {
    if (element) {
        element.classList.add("is-hidden");
        element.style.display = "none";
    }
}

function showElement(element, displayValue) {
    if (element) {
        element.classList.remove("is-hidden");
        element.style.display =
            displayValue || "block";
    }
}

function showOnly(viewName) {
    hideElement(elements.loadingState);
    hideElement(elements.errorState);
    hideElement(elements.unauthorizedState);
    hideElement(elements.mainContent);

    if (viewName === "loading") {
        showElement(
            elements.loadingState,
            "flex"
        );
    }

    if (viewName === "error") {
        showElement(
            elements.errorState,
            "flex"
        );
    }

    if (viewName === "unauthorized") {
        showElement(
            elements.unauthorizedState,
            "flex"
        );
    }

    if (viewName === "main") {
        showElement(
            elements.mainContent,
            "block"
        );
    }
}

function fillUserInformation() {
    const displayName =
        user.fullName ||
        user.username ||
        "—";

    const organizationName =
        user.organizationName ||
        "—";

    const sidebarName =
        getElement("sidebarUserName");

    const sidebarOrganization =
        getElement("sidebarUserOrg");

    const sidebarAvatar =
        getElement("sidebarUserAvatar");

    const headerName =
        getElement("headerUserName");

    const headerOrganization =
        getElement("headerUserOrg");

    const headerRole =
        getElement("headerUserRole");

    if (sidebarName) {
        sidebarName.textContent =
            displayName;
    }

    if (sidebarOrganization) {
        sidebarOrganization.textContent =
            organizationName;
    }

    if (sidebarAvatar) {
        sidebarAvatar.textContent =
            displayName
                .charAt(0)
                .toUpperCase();
    }

    if (headerName) {
        headerName.textContent =
            displayName;
    }

    if (headerOrganization) {
        headerOrganization.textContent =
            organizationName;
    }

    if (headerRole) {
        headerRole.textContent =
            user.roleCode;
    }
}

function showFormMessage(
    message,
    type
) {
    elements.formMessage.textContent =
        message;

    elements.formMessage.className =
        "form-message " +
        (type || "error");

    showElement(
        elements.formMessage
    );
}

function hideFormMessage() {
    elements.formMessage.textContent = "";

    elements.formMessage.className =
        "form-message";

    hideElement(
        elements.formMessage
    );
}

function goBack() {
    window.location.href =
        PRODUCTION_LOT_LIST_URL;
}

function showLoadError(message) {
    elements.errorMessage.textContent =
        message ||
        "Không thể tải dữ liệu.";

    showOnly("error");
}

function getSelectedOptionText(
    selectElement
) {
    const selectedOption =
        selectElement.options[
            selectElement.selectedIndex
        ];

    if (
        !selectedOption ||
        !selectedOption.value
    ) {
        return "—";
    }

    return selectedOption
        .textContent
        .trim();
}

function formatDate(dateValue) {
    if (!dateValue) {
        return "—";
    }

    const dateParts =
        dateValue.split("-");

    if (dateParts.length !== 3) {
        return dateValue;
    }

    return (
        dateParts[2] +
        "/" +
        dateParts[1] +
        "/" +
        dateParts[0]
    );
}

function updateSummary() {
    elements.summaryProductionLot.textContent =
        getSelectedOptionText(
            elements.productionLot
        );

    elements.summaryActivityType.textContent =
        ACTIVITY_LABELS[
            elements.activityType.value
        ] || "—";

    elements.summaryMaterial.textContent =
        elements.material.value.trim() ||
        "—";

    elements.summaryQuantity.textContent =
        elements.quantity.value ||
        "—";

    elements.summaryUnit.textContent =
        elements.unit.value.trim() ||
        "—";

    elements.summaryExecutedDate.textContent =
        formatDate(
            elements.executedDate.value
        );

    elements.summaryNotes.textContent =
        elements.notes.value.trim() ||
        "—";

    elements.notesCounter.textContent =
        elements.notes.value.length +
        " / 1000";
}

function createOption(
    value,
    text,
    disabled
) {
    const option =
        document.createElement("option");

    option.value = value;
    option.textContent = text;
    option.disabled = Boolean(disabled);

    return option;
}

function isAllowedProductionLot(lot) {
    const status =
        String(
            lot.status || ""
        ).toUpperCase();

    return ALLOWED_STATUSES.includes(
        status
    );
}

function getProductionLotName(lot) {
    const name =
        lot.name ||
        "Lô sản xuất không có tên";

    const status =
        lot.status ||
        "—";

    return (
        name +
        " (" +
        status +
        ")"
    );
}

function renderProductionLots(lots) {
    elements.productionLot.innerHTML = "";

    elements.productionLot.appendChild(
        createOption(
            "",
            "-- Chọn lô sản xuất --"
        )
    );

    if (!lots.length) {
        elements.productionLot.appendChild(
            createOption(
                "",
                "Không có lô APPROVED hoặc HARVESTED",
                true
            )
        );

        elements.productionLot.disabled =
            true;

        elements.submitButton.disabled =
            true;

        updateSummary();

        return;
    }

    lots.forEach(function (lot) {
        elements.productionLot.appendChild(
            createOption(
                lot.id,
                getProductionLotName(lot)
            )
        );
    });

    elements.productionLot.disabled =
        false;

    elements.submitButton.disabled =
        false;

    if (productionLotIdFromUrl) {
        const selectedLot =
            lots.find(function (lot) {
                return (
                    String(lot.id) ===
                    String(
                        productionLotIdFromUrl
                    )
                );
            });

        if (selectedLot) {
            elements.productionLot.value =
                String(selectedLot.id);

            elements.productionLot.disabled =
                true;
        } else {
            showFormMessage(
                "Lô sản xuất không tồn tại, " +
                "không thuộc tổ chức của bạn " +
                "hoặc chưa ở trạng thái phù hợp.",
                "error"
            );

            elements.submitButton.disabled =
                true;
        }
    }

    updateSummary();
}

async function loadProductionLots() {
    showOnly("loading");
    hideFormMessage();

    try {
        const response =
            await getProductionLots();

        if (
            !response ||
            response.success !== true
        ) {
            throw new Error(
                response &&
                response.message
                    ? response.message
                    : "Không thể tải danh sách lô sản xuất."
            );
        }

        const lots =
            Array.isArray(response.data)
                ? response.data.filter(
                    isAllowedProductionLot
                )
                : [];

        renderProductionLots(lots);
        showOnly("main");

    } catch (error) {
        console.error(
            "Load production lots error:",
            error
        );

        showLoadError(
            error.message ||
            "Không thể tải danh sách lô sản xuất."
        );
    }
}

function clearFieldErrors() {
    document
        .querySelectorAll(".field-error")
        .forEach(function (errorElement) {
            errorElement.textContent = "";
        });

    document
        .querySelectorAll(".input-error")
        .forEach(function (inputElement) {
            inputElement.classList.remove(
                "input-error"
            );

            inputElement.setAttribute(
                "aria-invalid",
                "false"
            );
        });
}

function setFieldError(
    fieldName,
    message
) {
    const inputElement =
        elements[fieldName];

    if (!inputElement) {
        return;
    }

    const errorElement =
        getElement(
            inputElement.id +
            "Error"
        );

    inputElement.classList.add(
        "input-error"
    );

    inputElement.setAttribute(
        "aria-invalid",
        "true"
    );

    if (errorElement) {
        errorElement.textContent =
            message;
    }
}

function validateForm() {
    clearFieldErrors();

    let isValid = true;

    if (!elements.productionLot.value) {
        setFieldError(
            "productionLot",
            "Vui lòng chọn lô sản xuất."
        );

        isValid = false;
    }

    if (!elements.activityType.value) {
        setFieldError(
            "activityType",
            "Vui lòng chọn loại hoạt động."
        );

        isValid = false;
    }

    const quantityValue =
        elements.quantity.value;

    if (
        quantityValue !== "" &&
        (
            Number.isNaN(
                Number(quantityValue)
            ) ||
            Number(quantityValue) <= 0
        )
    ) {
        setFieldError(
            "quantity",
            "Số lượng phải lớn hơn 0."
        );

        isValid = false;
    }

    if (!elements.executedDate.value) {
        setFieldError(
            "executedDate",
            "Vui lòng chọn ngày thực hiện."
        );

        isValid = false;
    }

    return isValid;
}

function optionalText(value) {
    const text =
        String(value || "").trim();

    return text || null;
}

function buildRequestBody() {
    return {
        productionLotId:
            elements.productionLot.value,

        activityType:
            elements.activityType.value,

        material:
            optionalText(
                elements.material.value
            ),

        quantity:
            elements.quantity.value === ""
                ? null
                : Number(
                    elements.quantity.value
                ),

        unit:
            optionalText(
                elements.unit.value
            ),

        executedDate:
            elements.executedDate.value,

        notes:
            optionalText(
                elements.notes.value
            )
    };
}

async function handleSubmit(event) {
    event.preventDefault();
    hideFormMessage();

    if (!validateForm()) {
        showFormMessage(
            "Vui lòng kiểm tra lại thông tin đã nhập.",
            "error"
        );

        return;
    }

    elements.submitButton.disabled =
        true;

    elements.submitButton.textContent =
        "Đang lưu...";

    try {
        const response =
            await createFarmLog(
                buildRequestBody()
            );

        if (
            !response ||
            response.success !== true
        ) {
            throw new Error(
                response &&
                response.message
                    ? response.message
                    : "Không thể lưu nhật ký canh tác."
            );
        }

        showFormMessage(
            "Ghi nhật ký canh tác thành công.",
            "success"
        );

        window.setTimeout(
            goBack,
            1200
        );

    } catch (error) {
        console.error(
            "Create farm log error:",
            error
        );

        showFormMessage(
            error.message ||
            "Không thể lưu nhật ký canh tác.",
            "error"
        );

        elements.submitButton.disabled =
            false;

        elements.submitButton.textContent =
            "Lưu nhật ký";
    }
}

function bindEvents() {
    const formFields = [
        elements.productionLot,
        elements.activityType,
        elements.material,
        elements.quantity,
        elements.unit,
        elements.executedDate,
        elements.notes
    ];

    formFields.forEach(function (field) {
        field.addEventListener(
            "input",
            updateSummary
        );

        field.addEventListener(
            "change",
            updateSummary
        );
    });

    elements.form.addEventListener(
        "submit",
        handleSubmit
    );

    elements.backButton.addEventListener(
        "click",
        goBack
    );

    elements.cancelButton.addEventListener(
        "click",
        goBack
    );

    elements.retryButton.addEventListener(
        "click",
        loadProductionLots
    );
}

fillUserInformation();
setupLogout();
bindEvents();

if (user.roleCode === ALLOWED_ROLE) {
    loadProductionLots();
} else {
    showOnly("unauthorized");
}