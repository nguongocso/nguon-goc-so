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
    activateTraceCodes
} from "../../../services/trace-code.service.js";

const LOGIN_URL =
    "/frontend/pages/auth/login.html";

const PRODUCTION_LOT_LIST_URL =
    "/frontend/pages/cooperative/production-lots/index.html";

const ALLOWED_ROLES = [
    "VT-02",
    "VT-03"
];

const ALLOWED_STATUSES = [
    "APPROVED",
    "HARVESTED"
];

if (!requireAuth()) {
    throw new Error(
        "Người dùng chưa đăng nhập."
    );
}

const user = getUser();

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

    form: getElement("traceCodeActivationForm"),
    formMessage: getElement("formMessage"),
    submitButton: getElement("submitButton"),
    retryButton: getElement("retryButton"),
    backButton: getElement("backButton"),
    cancelButton: getElement("cancelButton"),

    productionLot: getElement("productionLotId"),
    codeRange: getElement("codeRangeId"),
    quantity: getElement("quantity"),

    summaryProductionLot:
        getElement("summaryProductionLot"),

    summaryCodeRange:
        getElement("summaryCodeRange"),

    summaryRemainingQuantity:
        getElement("summaryRemainingQuantity"),

    summaryQuantity:
        getElement("summaryQuantity"),

    activationResult:
        getElement("activationResult"),

    activationResultMessage:
        getElement("activationResultMessage"),

    resultActivatedQuantity:
        getElement("resultActivatedQuantity"),

    resultStatus:
        getElement("resultStatus"),

    resultStartCode:
        getElement("resultStartCode"),

    resultEndCode:
        getElement("resultEndCode"),

    resultActivatedAt:
        getElement("resultActivatedAt")
};

const queryParams =
    new URLSearchParams(
        window.location.search
    );

const productionLotIdFromUrl =
    queryParams.get("productionLotId");

const codeRangeIdFromUrl =
    queryParams.get("codeRangeId");

const codeRangeLabelFromUrl =
    queryParams.get("codeRangeLabel") ||
    queryParams.get("codeRangeName");

const remainingQuantityFromUrl =
    parseOptionalInteger(
        queryParams.get("remainingQuantity")
    );

function parseOptionalInteger(value) {
    if (
        value === null ||
        value === ""
    ) {
        return null;
    }

    const numberValue = Number(value);

    if (
        !Number.isInteger(numberValue) ||
        numberValue < 0
    ) {
        return null;
    }

    return numberValue;
}

function hideElement(element) {
    if (!element) {
        return;
    }

    element.classList.add("is-hidden");
    element.style.display = "none";
}

function showElement(
    element,
    displayValue = "block"
) {
    if (!element) {
        return;
    }

    element.classList.remove("is-hidden");
    element.style.display = displayValue;
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

function setupSidebarByRole() {
    if (user.roleCode !== "VT-03") {
        return;
    }

    const dashboardMenu =
        getElement("dashboardMenu");

    const farmAreasMenu =
        getElement("farmAreasMenu");

    if (dashboardMenu) {
        dashboardMenu.style.display = "none";
    }

    if (farmAreasMenu) {
        farmAreasMenu.style.display = "none";
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
        sidebarName.textContent = displayName;
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
        headerName.textContent = displayName;
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

function showFormMessage(
    message,
    type = "error"
) {
    elements.formMessage.textContent =
        message;

    elements.formMessage.className =
        `form-message ${type}`;

    showElement(elements.formMessage);
}

function hideFormMessage() {
    elements.formMessage.textContent = "";
    elements.formMessage.className =
        "form-message is-hidden";

    hideElement(elements.formMessage);
}

function createOption(
    value,
    text,
    disabled = false
) {
    const option =
        document.createElement("option");

    option.value = value;
    option.textContent = text;
    option.disabled = Boolean(disabled);

    return option;
}

function normalizeListData(response) {
    if (!response) {
        return [];
    }

    if (Array.isArray(response.data)) {
        return response.data;
    }

    if (
        response.data &&
        Array.isArray(response.data.items)
    ) {
        return response.data.items;
    }

    return [];
}

function normalizeStatus(status) {
    return String(status || "")
        .trim()
        .toUpperCase();
}

function isAllowedProductionLot(lot) {
    return ALLOWED_STATUSES.includes(
        normalizeStatus(lot.status)
    );
}

function getProductionLotLabel(lot) {
    const name =
        lot.name ||
        lot.lotName ||
        lot.code ||
        lot.lotCode ||
        "Lô sản xuất không có tên";

    const status =
        normalizeStatus(lot.status) ||
        "—";

    return `${name} (${status})`;
}

function getSelectedOptionText(selectElement) {
    if (!selectElement) {
        return "—";
    }

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

    return selectedOption.textContent.trim();
}

function formatNumber(value) {
    const numberValue = Number(value);

    if (!Number.isFinite(numberValue)) {
        return "—";
    }

    return new Intl.NumberFormat("vi-VN")
        .format(numberValue);
}

function formatDateTime(value) {
    if (!value) {
        return "—";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return new Intl.DateTimeFormat(
        "vi-VN",
        {
            dateStyle: "short",
            timeStyle: "medium"
        }
    ).format(date);
}

function formatStatus(status) {
    const normalizedStatus =
        normalizeStatus(status);

    if (normalizedStatus === "ACTIVATED") {
        return "Đã kích hoạt";
    }

    return status || "—";
}

function updateSummary() {
    elements.summaryProductionLot.textContent =
        getSelectedOptionText(
            elements.productionLot
        );

    elements.summaryCodeRange.textContent =
        getSelectedOptionText(
            elements.codeRange
        );

    elements.summaryRemainingQuantity.textContent =
        remainingQuantityFromUrl === null
            ? "Chưa có dữ liệu"
            : `${formatNumber(
                remainingQuantityFromUrl
            )} tem`;

    const quantityValue =
        elements.quantity.value;

    elements.summaryQuantity.textContent =
        quantityValue === ""
            ? "—"
            : `${formatNumber(
                quantityValue
            )} tem`;
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

        elements.productionLot.disabled = true;
        elements.submitButton.disabled = true;
        updateSummary();
        return;
    }

    lots.forEach(function (lot) {
        elements.productionLot.appendChild(
            createOption(
                String(lot.id),
                getProductionLotLabel(lot)
            )
        );
    });

    elements.productionLot.disabled = false;

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
                "Lô sản xuất không tồn tại, không thuộc tổ chức của bạn hoặc chưa ở trạng thái phù hợp."
            );

            elements.submitButton.disabled =
                true;
        }
    }

    updateSummary();
}

function renderCodeRangeFromUrl() {
    elements.codeRange.innerHTML = "";

    elements.codeRange.appendChild(
        createOption(
            "",
            "-- Chọn dải tem --"
        )
    );

    if (!codeRangeIdFromUrl) {
        elements.codeRange.appendChild(
            createOption(
                "",
                "Chưa có API lấy danh sách dải tem",
                true
            )
        );

        elements.codeRange.disabled = true;
        elements.submitButton.disabled = true;

        showFormMessage(
            "Tài liệu hiện chỉ có API kích hoạt tem, chưa có API lấy danh sách dải tem. Hãy mở trang với tham số codeRangeId hoặc bổ sung API danh sách dải tem.",
            "error"
        );

        updateSummary();
        return;
    }

    const label =
        codeRangeLabelFromUrl ||
        `Dải tem ${codeRangeIdFromUrl}`;

    elements.codeRange.appendChild(
        createOption(
            codeRangeIdFromUrl,
            label
        )
    );

    elements.codeRange.value =
        codeRangeIdFromUrl;

    elements.codeRange.disabled = true;
    updateSummary();
}

async function loadInitialData() {
    showOnly("loading");
    hideFormMessage();
    hideElement(elements.activationResult);

    try {
        const response =
            await getProductionLots();

        if (
            !response ||
            response.success !== true
        ) {
            throw new Error(
                response && response.message
                    ? response.message
                    : "Không thể tải danh sách lô sản xuất."
            );
        }

        const lots =
            normalizeListData(response)
                .filter(isAllowedProductionLot);

        renderProductionLots(lots);
        renderCodeRangeFromUrl();
        showOnly("main");
    } catch (error) {
        console.error(
            "Load trace-code activation data error:",
            error
        );

        showLoadError(
            error.message ||
            "Không thể tải dữ liệu kích hoạt tem."
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
    element,
    message
) {
    if (!element) {
        return;
    }

    const errorElement =
        getElement(`${element.id}Error`);

    element.classList.add("input-error");
    element.setAttribute(
        "aria-invalid",
        "true"
    );

    if (errorElement) {
        errorElement.textContent = message;
    }
}

function validateForm() {
    clearFieldErrors();

    let isValid = true;

    if (!elements.productionLot.value) {
        setFieldError(
            elements.productionLot,
            "Vui lòng chọn lô sản xuất."
        );
        isValid = false;
    }

    if (!elements.codeRange.value) {
        setFieldError(
            elements.codeRange,
            "Vui lòng chọn dải tem."
        );
        isValid = false;
    }

    const quantityValue =
        Number(elements.quantity.value);

    if (elements.quantity.value === "") {
        setFieldError(
            elements.quantity,
            "Vui lòng nhập số lượng tem."
        );
        isValid = false;
    } else if (
        !Number.isInteger(quantityValue) ||
        quantityValue < 1
    ) {
        setFieldError(
            elements.quantity,
            "Số lượng phải là số nguyên từ 1 trở lên."
        );
        isValid = false;
    } else if (
        remainingQuantityFromUrl !== null &&
        quantityValue > remainingQuantityFromUrl
    ) {
        setFieldError(
            elements.quantity,
            `Dải tem chỉ còn ${formatNumber(
                remainingQuantityFromUrl
            )} tem.`
        );
        isValid = false;
    }

    return isValid;
}

function buildRequestBody() {
    return {
        productionLotId:
            elements.productionLot.value,

        codeRangeId:
            elements.codeRange.value,

        quantity:
            Number(elements.quantity.value)
    };
}

function renderActivationResult(response) {
    const data =
        response && response.data
            ? response.data
            : {};

    elements.activationResultMessage.textContent =
        response.message ||
        "Dải tem đã được liên kết với lô sản xuất.";

    elements.resultActivatedQuantity.textContent =
        data.activatedQuantity === undefined
            ? "—"
            : `${formatNumber(
                data.activatedQuantity
            )} tem`;

    elements.resultStatus.textContent =
        formatStatus(data.status);

    elements.resultStartCode.textContent =
        data.startCode || "—";

    elements.resultEndCode.textContent =
        data.endCode || "—";

    elements.resultActivatedAt.textContent =
        formatDateTime(data.activatedAt);

    showElement(elements.activationResult);

    elements.activationResult.scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
}

function resetSubmitButton() {
    elements.submitButton.disabled = false;
    elements.submitButton.textContent =
        "Kích hoạt tem";
}

async function handleSubmit(event) {
    event.preventDefault();
    hideFormMessage();
    hideElement(elements.activationResult);

    if (!validateForm()) {
        showFormMessage(
            "Vui lòng kiểm tra lại thông tin đã nhập."
        );
        return;
    }

    const requestBody = buildRequestBody();

    const confirmed = window.confirm(
        `Bạn có chắc muốn kích hoạt ${formatNumber(
            requestBody.quantity
        )} tem cho lô sản xuất đã chọn?`
    );

    if (!confirmed) {
        return;
    }

    elements.submitButton.disabled = true;
    elements.submitButton.textContent =
        "Đang kích hoạt...";

    try {
        const response =
            await activateTraceCodes(
                requestBody
            );

        if (
            !response ||
            response.success !== true
        ) {
            throw new Error(
                response && response.message
                    ? response.message
                    : "Không thể kích hoạt tem."
            );
        }

        showFormMessage(
            response.message ||
            "Kích hoạt tem thành công.",
            "success"
        );

        renderActivationResult(response);

        elements.productionLot.disabled = true;
        elements.codeRange.disabled = true;
        elements.quantity.disabled = true;
        elements.submitButton.textContent =
            "Đã kích hoạt";
    } catch (error) {
        console.error(
            "Activate trace codes error:",
            error
        );

        showFormMessage(
            error.message ||
            "Không thể kích hoạt tem."
        );

        resetSubmitButton();
    }
}

function bindEvents() {
    [
        elements.productionLot,
        elements.codeRange,
        elements.quantity
    ].forEach(function (field) {
        if (!field) {
            return;
        }

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
        loadInitialData
    );
}

setupSidebarByRole();
fillUserInformation();
setupLogout();
bindEvents();

if (
    ALLOWED_ROLES.includes(
        user.roleCode
    )
) {
    loadInitialData();
} else {
    showOnly("unauthorized");
}
