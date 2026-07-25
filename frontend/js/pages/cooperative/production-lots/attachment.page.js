import {
    requireAuth,
    setupLogout
} from "../../../core/auth-guard.js";

import {
    getUser
} from "../../../core/storage.js";

/* =========================================================
   Auth
========================================================= */

if (!requireAuth()) {
    // redirected
}

var user = getUser();

if (!user || !user.roleCode) {
    window.location.href = "/frontend/pages/auth/login.html";
    throw new Error("User not authenticated.");
}

var roleCode = user.roleCode;

var allowedRoles = [
    "VT-01",
    "VT-02",
    "VT-03"
];

if (!allowedRoles.includes(roleCode)) {

    document.getElementById("loadingState").style.display = "none";
    document.getElementById("unauthorizedState").style.display = "flex";

    throw new Error("Access denied.");

}

/* =========================================================
   Sidebar: Hide menus for VT-03
========================================================= */

if (roleCode === "VT-03") {
    var menuIds = [
        "dashboardMenu",
        "farmAreasMenu",
        "organizationProfileMenu"
    ];

    menuIds.forEach(function (menuId) {
        var menuItem = document.getElementById(menuId);
        if (menuItem) {
            menuItem.style.display = "none";
        }
    });
}

/* =========================================================
   User Info
========================================================= */

function populateUserInfo() {

    var sidebarName = document.getElementById("sidebarUserName");
    var sidebarOrg = document.getElementById("sidebarUserOrg");
    var sidebarAvatar = document.getElementById("sidebarUserAvatar");

    if (sidebarName)
        sidebarName.textContent =
        user.fullName || user.username || "—";

    if (sidebarOrg)
        sidebarOrg.textContent =
        user.organizationName || "—";

    if (sidebarAvatar)
        sidebarAvatar.textContent =
        (user.fullName || user.username || "?")[0].toUpperCase();

    var headerName = document.getElementById("headerUserName");
    var headerOrg = document.getElementById("headerUserOrg");
    var headerRole = document.getElementById("headerUserRole");

    if (headerName)
        headerName.textContent =
        user.fullName || user.username || "—";

    if (headerOrg)
        headerOrg.textContent =
        user.organizationName || "—";

    if (headerRole)
        headerRole.textContent =
        user.roleCode || "—";

}

populateUserInfo();

/* =========================================================
   DOM
========================================================= */

var loadingState = document.getElementById("loadingState");
var errorState = document.getElementById("errorState");
var unauthorizedState = document.getElementById("unauthorizedState");

var mainContent = document.getElementById("mainContent");

var dropZone =
    document.getElementById("attachmentDropZone");

var fileInput =
    document.getElementById("attachmentInput");

var chooseButton =
    document.getElementById("attachmentChooseButton");

var attachmentTable =
    document.getElementById("attachmentTable");

var attachmentTableBody =
    document.getElementById("attachmentTableBody");

var attachmentCount =
    document.getElementById("attachmentCount");

var emptyState =
    document.getElementById("attachmentEmptyState");

var previewContainer =
    document.getElementById("documentPreviewContainer");

/* =========================================================
   Role-based UI: VT-02 = read-only, VT-03 = can upload
========================================================= */

var isReadOnly = (roleCode === "VT-02");

function setupRoleBasedUI() {
    if (isReadOnly) {
        // VT-02: Hide upload section completely
        var uploadCard = document.querySelector(".attachment-card");
        if (uploadCard) {
            uploadCard.style.display = "none";
        }

        // VT-02: Hide delete buttons in action column
        // Will be handled also in render
    }

    // For VT-03: show upload section (default)
}

/* =========================================================
   State
========================================================= */

var attachments = [];

/* =========================================================
   Init
========================================================= */

function initializePage() {

    loadingState.style.display = "none";
    errorState.style.display = "none";
    unauthorizedState.style.display = "none";

    mainContent.style.display = "block";

    setupRoleBasedUI();

    if (!isReadOnly) {
        initializeUpload();
    }

    renderAttachmentList();

}

setupLogout();

initializePage();

/* =========================================================
   Upload
========================================================= */

function initializeUpload() {

    chooseButton.addEventListener(
        "click",
        function () {

            fileInput.click();

        }
    );

    fileInput.addEventListener(
        "change",
        function (event) {

            addFiles(event.target.files);

            fileInput.value = "";

        }
    );

    dropZone.addEventListener(
        "dragover",
        function (event) {

            event.preventDefault();

            dropZone.classList.add("drag-over");

        }
    );

    dropZone.addEventListener(
        "dragleave",
        function () {

            dropZone.classList.remove("drag-over");

        }
    );

    dropZone.addEventListener(
        "drop",
        function (event) {

            event.preventDefault();

            dropZone.classList.remove("drag-over");

            addFiles(event.dataTransfer.files);

        }
    );

}

/* =========================================================
   Add Files
========================================================= */

function addFiles(fileList) {

    if (!fileList)
        return;

    Array.from(fileList).forEach(function (file) {

        attachments.push({

            id:
                Date.now() +
                Math.random(),

            name:
                file.name,

            type:
                getExtension(file.name),

            size:
                file.size,

            uploadedAt:
                new Date(),

            file:
                file

        });

    });

    renderAttachmentList();

}

/* =========================================================
   Render
========================================================= */

function renderAttachmentList() {

    attachmentTableBody.innerHTML = "";

    attachmentCount.textContent =
        attachments.length +
        " Files";

    if (attachments.length === 0) {

        emptyState.style.display = "flex";
        attachmentTable.style.display = "none";

        return;

    }

    emptyState.style.display = "none";
    attachmentTable.style.display = "table";

    attachments.forEach(function (item) {

        var row =
            document.createElement("tr");

        row.className =
            "file-item-row";

        // Build action buttons based on role
        var actionButtons = "";

        actionButtons +=
            "<button class='file-action-btn file-action-view' data-id='" + item.id + "'>View</button>" +
            "<button class='file-action-btn file-action-download' data-id='" + item.id + "'>Download</button>";

        // Only show delete for non-read-only roles
        if (!isReadOnly) {
            actionButtons +=
                "<button class='file-action-btn file-action-delete' data-id='" + item.id + "'>Delete</button>";
        }

        row.innerHTML =

            "<td>" +

            "<div class='file-item-name'>" +

            "<div class='file-icon file-icon-" +
            item.type.toLowerCase() +
            "'>" +

            item.type +

            "</div>" +

            "<div class='file-name'>" +
            item.name +
            "</div>" +

            "</div>" +

            "</td>" +

            "<td class='file-type'>" +
            item.type +
            "</td>" +

            "<td class='file-size'>" +
            formatSize(item.size) +
            "</td>" +

            "<td class='file-date'>" +
            formatDate(item.uploadedAt) +
            "</td>" +

            "<td>" +

            "<div class='file-actions'>" +
            actionButtons +
            "</div>" +

            "</td>";

        attachmentTableBody.appendChild(row);

    });

    bindActionEvents();

}
/* =========================================================
   Bind Actions
========================================================= */

function bindActionEvents() {

    document
        .querySelectorAll(".file-action-view")
        .forEach(function (button) {

            button.onclick = function () {

                previewFile(
                    Number(this.dataset.id)
                );

            };

        });

    document
        .querySelectorAll(".file-action-download")
        .forEach(function (button) {

            button.onclick = function () {

                downloadFile(
                    Number(this.dataset.id)
                );

            };

        });

    // Only bind delete events if not read-only
    if (!isReadOnly) {
        document
            .querySelectorAll(".file-action-delete")
            .forEach(function (button) {

                button.onclick = function () {

                    deleteFile(
                        Number(this.dataset.id)
                    );

                };

            });
    }

}

/* =========================================================
   Preview
========================================================= */

function previewFile(id) {

    var item =
        attachments.find(function (x) {

            return x.id === id;

        });

    if (!item)
        return;

    var type =
        item.type.toLowerCase();

    var url =
        URL.createObjectURL(item.file);

    if (
        type === "png" ||
        type === "jpg" ||
        type === "jpeg"
    ) {

        previewContainer.innerHTML =

            "<img " +
            "class='document-preview-image' " +
            "src='" + url + "'>";

        return;

    }

    if (type === "pdf") {

        previewContainer.innerHTML =

            "<iframe " +
            "class='document-preview-pdf' " +
            "src='" + url + "'>" +
            "</iframe>";

        return;

    }

    previewContainer.innerHTML =

        "<div class='document-preview-placeholder'>" +

        "<svg width='60' height='60' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2'>" +

        "<path d='M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z'></path>" +

        "<polyline points='14 2 14 8 20 8'></polyline>" +

        "</svg>" +

        "<h3>No Preview</h3>" +

        "<p>This file type cannot be previewed.</p>" +

        "</div>";

}

/* =========================================================
   Download
========================================================= */

function downloadFile(id) {

    var item =
        attachments.find(function (x) {

            return x.id === id;

        });

    if (!item)
        return;

    var url =
        URL.createObjectURL(item.file);

    var link =
        document.createElement("a");

    link.href = url;

    link.download =
        item.name;

    document.body.appendChild(link);

    link.click();

    document.body.removeChild(link);

    URL.revokeObjectURL(url);

}

/* =========================================================
   Delete
========================================================= */

function deleteFile(id) {

    attachments =
        attachments.filter(function (item) {

            return item.id !== id;

        });

    renderAttachmentList();

    previewContainer.innerHTML =

        "<div class='empty-state-container'>" +

        "<div class='empty-state-icon'>" +

        "<svg width='48' height='48' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2'>" +

        "<rect x='3' y='3' width='18' height='18' rx='2'></rect>" +

        "<circle cx='8.5' cy='8.5' r='1.5'></circle>" +

        "<polyline points='21 15 16 10 5 21'></polyline>" +

        "</svg>" +

        "</div>" +

        "<p class='empty-state-text'>" +

        "No preview available" +

        "</p>" +

        "<p class='empty-state-hint'>" +

        "Select a document to preview." +

        "</p>" +

        "</div>";

}

/* =========================================================
   Helpers
========================================================= */

function getExtension(fileName) {

    var index =
        fileName.lastIndexOf(".");

    if (index < 0)
        return "FILE";

    return fileName
        .substring(index + 1)
        .toUpperCase();

}

function formatSize(bytes) {

    if (bytes < 1024)
        return bytes + " B";

    if (bytes < 1024 * 1024)
        return (bytes / 1024)
            .toFixed(1) + " KB";

    return (bytes / (1024 * 1024))
        .toFixed(2) + " MB";

}

function formatDate(date) {

    return new Date(date)
        .toLocaleDateString(
            "en-US",
            {
                year: "numeric",
                month: "short",
                day: "numeric"
            }
        );

}