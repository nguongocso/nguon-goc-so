import {
    getOrganizations
} from "../../services/organization.service.js";

import {
    createCodeRange
} from "../../services/trace-code.service.js";

import {
    requireRole,
    setupLogout
} from "../../core/auth-guard.js";

/* ========================================
   DOM
======================================== */

const unauthorizedState =
    document.getElementById(
        "unauthorizedState"
    );

const pageContent =
    document.getElementById(
        "pageContent"
    );

const codeRangeForm =
    document.getElementById(
        "codeRangeForm"
    );

const formCard =
    codeRangeForm.closest(
        ".code-range-card"
    );

const organizationSelect =
    document.getElementById(
        "organizationId"
    );

const prefixInput =
    document.getElementById(
        "prefix"
    );

const totalLimitInput =
    document.getElementById(
        "totalLimit"
    );

const formMessage =
    document.getElementById(
        "formMessage"
    );

const submitButton =
    document.getElementById(
        "submitButton"
    );

const cancelButton =
    document.getElementById(
        "cancelButton"
    );

const successResult =
    document.getElementById(
        "successResult"
    );

const createAnotherButton =
    document.getElementById(
        "createAnotherButton"
    );

/* ========================================
   KHỞI TẠO TRANG
======================================== */

initializePage();

function initializePage() {
    if (!requireRole("VT-01")) {
        pageContent.hidden = true;
        unauthorizedState.hidden = false;

        return;
    }

    unauthorizedState.hidden = true;
    pageContent.hidden = false;

    setupLogout();
    bindEvents();
    loadOrganizations();
}

/* ========================================
   SỰ KIỆN
======================================== */

function bindEvents() {
    codeRangeForm.addEventListener(
        "submit",
        handleSubmit
    );

    cancelButton.addEventListener(
        "click",
        function () {
            window.location.href =
                "/frontend/pages/admin/dashboard.html";
        }
    );

    createAnotherButton.addEventListener(
        "click",
        resetPage
    );

    organizationSelect.addEventListener(
        "change",
        function () {
            clearFieldError(
                "organizationId"
            );
        }
    );

    prefixInput.addEventListener(
        "input",
        function () {
            clearFieldError(
                "prefix"
            );
        }
    );

    totalLimitInput.addEventListener(
        "input",
        function () {
            clearFieldError(
                "totalLimit"
            );
        }
    );
}

/* ========================================
   TẢI DANH SÁCH TỔ CHỨC
======================================== */

async function loadOrganizations() {
    setOrganizationLoading(true);
    clearMessage();

    try {
        const response =
            await getOrganizations();

        if (
            !response ||
            response.success !== true
        ) {
            throw new Error(
                response?.message ||
                "Không thể tải danh sách tổ chức."
            );
        }

        const organizations =
            Array.isArray(response.data)
                ? response.data
                : [];

        const availableOrganizations =
            organizations.filter(
                function (organization) {
                    return (
                        organization.status ===
                            "ACTIVE" &&
                        organization
                            .organizationType !==
                            "SYSTEM"
                    );
                }
            );

        renderOrganizationOptions(
            availableOrganizations
        );
    } catch (error) {
        console.error(
            "Lỗi tải danh sách tổ chức:",
            error
        );

        organizationSelect.innerHTML = "";

        const errorOption =
            document.createElement(
                "option"
            );

        errorOption.value = "";
        errorOption.textContent =
            "Không thể tải danh sách tổ chức";

        organizationSelect.appendChild(
            errorOption
        );

        showError(
            error.message ||
            "Không thể tải danh sách tổ chức."
        );
    }
}

function renderOrganizationOptions(
    organizations
) {
    organizationSelect.innerHTML = "";

    const defaultOption =
        document.createElement(
            "option"
        );

    defaultOption.value = "";

    if (organizations.length === 0) {
        defaultOption.textContent =
            "Không có tổ chức đang hoạt động";

        organizationSelect.appendChild(
            defaultOption
        );

        organizationSelect.disabled =
            true;

        showError(
            "Không có tổ chức phù hợp để cấp dải mã."
        );

        return;
    }

    defaultOption.textContent =
        "Chọn tổ chức";

    organizationSelect.appendChild(
        defaultOption
    );

    organizations.forEach(
        function (organization) {
            const option =
                document.createElement(
                    "option"
                );

            option.value =
                organization.organizationID;

            option.textContent =
                buildOrganizationLabel(
                    organization
                );

            organizationSelect.appendChild(
                option
            );
        }
    );

    organizationSelect.disabled =
        false;
}

function buildOrganizationLabel(
    organization
) {
    const organizationName =
        organization.organizationName ||
        "Tổ chức chưa có tên";

    const organizationCode =
        organization.organizationCode;

    if (!organizationCode) {
        return organizationName;
    }

    return (
        `${organizationName} ` +
        `(${organizationCode})`
    );
}

function setOrganizationLoading(
    isLoading
) {
    organizationSelect.disabled =
        isLoading;

    if (isLoading) {
        organizationSelect.innerHTML =
            '<option value="">' +
            "Đang tải danh sách tổ chức..." +
            "</option>";
    }
}

/* ========================================
   GỬI FORM
======================================== */

async function handleSubmit(event) {
    event.preventDefault();

    clearMessage();
    clearFieldErrors();

    const codeRangeData =
        getFormData();

    const errors =
        validateCodeRange(
            codeRangeData
        );

    if (
        Object.keys(errors).length > 0
    ) {
        showFieldErrors(errors);

        return;
    }

    const organizationName =
        organizationSelect
            .selectedOptions[0]
            ?.textContent ||
        "tổ chức đã chọn";

    const confirmed =
        window.confirm(
            "Xác nhận cấp dải mã " +
            `"${codeRangeData.prefix}" ` +
            `cho ${organizationName}?`
        );

    if (!confirmed) {
        return;
    }

    setSubmitting(true);

    try {
        const response =
            await createCodeRange(
                codeRangeData
            );

        if (
            !response ||
            response.success !== true ||
            !response.data
        ) {
            throw new Error(
                response?.message ||
                "Không thể cấp dải mã."
            );
        }

        showSuccessResult(
            response.data
        );
    } catch (error) {
        console.error(
            "Lỗi cấp dải mã:",
            error
        );

        handleSubmitError(error);
    } finally {
        setSubmitting(false);
    }
}

function getFormData() {
    return {
        organizationId:
            organizationSelect
                .value
                .trim(),

        prefix:
            prefixInput
                .value
                .trim(),

        totalLimit:
            Number(
                totalLimitInput.value
            )
    };
}

/* ========================================
   KIỂM TRA DỮ LIỆU
======================================== */

function validateCodeRange(data) {
    const errors = {};

    if (!data.organizationId) {
        errors.organizationId =
            "Vui lòng chọn tổ chức.";
    }

    if (!data.prefix) {
        errors.prefix =
            "Vui lòng nhập tiền tố mã.";
    } else if (
        data.prefix.length > 50
    ) {
        errors.prefix =
            "Tiền tố mã không được vượt quá 50 ký tự.";
    }

    if (
        !Number.isInteger(
            data.totalLimit
        ) ||
        data.totalLimit <= 0
    ) {
        errors.totalLimit =
            "Số lượng mã tối đa phải là số nguyên lớn hơn 0.";
    }

    return errors;
}

/* ========================================
   XỬ LÝ LỖI API
======================================== */

function handleSubmitError(error) {
    const message =
        String(
            error.message ||
            "Đã xảy ra lỗi khi cấp dải mã."
        );

    const normalizedMessage =
        message.toLowerCase();

    let hasFieldError = false;

    if (
        normalizedMessage.includes(
            "tiền tố"
        ) &&
        normalizedMessage.includes(
            "tồn tại"
        )
    ) {
        setFieldError(
            "prefix",
            message
        );

        hasFieldError = true;
    }

    if (
        normalizedMessage.includes(
            "tổ chức"
        ) &&
        normalizedMessage.includes(
            "không tồn tại"
        )
    ) {
        setFieldError(
            "organizationId",
            message
        );

        hasFieldError = true;
    }

    if (
        normalizedMessage.includes(
            "hạn mức"
        ) ||
        normalizedMessage.includes(
            "lớn hơn 0"
        )
    ) {
        setFieldError(
            "totalLimit",
            message
        );

        hasFieldError = true;
    }

    if (error.status === 403) {
        showError(
            "Bạn không có quyền cấp dải mã truy xuất."
        );

        return;
    }

    if (!hasFieldError) {
        showError(message);
    }
}

/* ========================================
   HIỂN THỊ LỖI FORM
======================================== */

function showFieldErrors(errors) {
    Object.entries(errors).forEach(
        function ([
            fieldName,
            message
        ]) {
            setFieldError(
                fieldName,
                message
            );
        }
    );

    const firstFieldName =
        Object.keys(errors)[0];

    const firstField =
        document.querySelector(
            `[name="${firstFieldName}"]`
        );

    if (firstField) {
        firstField.focus();
    }
}

function setFieldError(
    fieldName,
    message
) {
    const errorElement =
        document.querySelector(
            `[data-error-for="${fieldName}"]`
        );

    const fieldElement =
        document.querySelector(
            `[name="${fieldName}"]`
        );

    if (errorElement) {
        errorElement.textContent =
            message;
    }

    if (fieldElement) {
        fieldElement.classList.add(
            "input-error"
        );

        fieldElement.setAttribute(
            "aria-invalid",
            "true"
        );
    }
}

function clearFieldError(fieldName) {
    const errorElement =
        document.querySelector(
            `[data-error-for="${fieldName}"]`
        );

    const fieldElement =
        document.querySelector(
            `[name="${fieldName}"]`
        );

    if (errorElement) {
        errorElement.textContent = "";
    }

    if (fieldElement) {
        fieldElement.classList.remove(
            "input-error"
        );

        fieldElement.setAttribute(
            "aria-invalid",
            "false"
        );
    }
}

function clearFieldErrors() {
    clearFieldError(
        "organizationId"
    );

    clearFieldError(
        "prefix"
    );

    clearFieldError(
        "totalLimit"
    );
}

/* ========================================
   KẾT QUẢ THÀNH CÔNG
======================================== */

function showSuccessResult(data) {
    const totalLimit =
        Number(data.totalLimit) || 0;

    const usedCount =
        Number(data.usedCount) || 0;

    const remainingCount =
        Math.max(
            totalLimit - usedCount,
            0
        );

    setTextContent(
        "resultOrganizationName",
        data.organizationName || "—"
    );

    setTextContent(
        "resultPrefix",
        data.prefix || "—"
    );

    setTextContent(
        "resultTotalLimit",
        formatNumber(totalLimit)
    );

    setTextContent(
        "resultUsedCount",
        formatNumber(usedCount)
    );

    setTextContent(
        "resultRemainingCount",
        formatNumber(
            remainingCount
        )
    );

    setTextContent(
        "resultCreatedAt",
        formatDate(
            data.createdAt
        )
    );

    formCard.hidden = true;
    successResult.hidden = false;

    successResult.scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
}

function setTextContent(
    elementId,
    value
) {
    const element =
        document.getElementById(
            elementId
        );

    if (element) {
        element.textContent =
            value;
    }
}

/* ========================================
   ĐẶT LẠI TRANG
======================================== */

function resetPage() {
    codeRangeForm.reset();

    clearMessage();
    clearFieldErrors();

    successResult.hidden = true;
    formCard.hidden = false;

    organizationSelect.focus();
}

/* ========================================
   TRẠNG THÁI VÀ ĐỊNH DẠNG
======================================== */

function setSubmitting(isSubmitting) {
    submitButton.disabled =
        isSubmitting;

    submitButton.textContent =
        isSubmitting
            ? "Đang cấp dải mã..."
            : "Cấp dải mã";

    organizationSelect.disabled =
        isSubmitting;

    prefixInput.disabled =
        isSubmitting;

    totalLimitInput.disabled =
        isSubmitting;
}

function showError(message) {
    formMessage.textContent =
        message;

    formMessage.className =
        "form-message error";
}

function clearMessage() {
    formMessage.textContent = "";

    formMessage.className =
        "form-message";
}

function formatNumber(value) {
    return new Intl.NumberFormat(
        "vi-VN"
    ).format(value);
}

function formatDate(value) {
    if (!value) {
        return "—";
    }

    const date =
        new Date(value);

    if (
        Number.isNaN(
            date.getTime()
        )
    ) {
        return value;
    }

    return date.toLocaleString(
        "vi-VN"
    );
}