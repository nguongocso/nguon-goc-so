# Nguon Goc So UI Design System

| Metadata | Value |
|---|---|
| Version | 2.1 |
| Effective date | 2026-09-12 |
| Scope | Nguon Goc So web frontend |
| Maintainer | Trần Văn Nhu |
| Technical baseline | `develop@17eac11d509cc5d5f2cf8a5ec58886450740103f` |

## 1. Purpose and authority

This is the single day-to-day source of truth for visual design, shared UI components, icons, and user-facing naming. AI agents, developers, and reviewers must apply it when creating a screen or changing related UI.

The system aims to keep every screen clean, modern, agricultural, trustworthy, professional, readable, spacious, and consistent. A new screen must look like an existing part of the same product, not a design created independently for one User Story.

This document consolidates and supersedes the previous `docs/AI_DESIGN_SYSTEM.md` version 1.0. The original approved Word documents are retained only for traceability:

- [`docs/design/references/UI_Common_Standards_v2.1.docx`](design/references/UI_Common_Standards_v2.1.docx)
- [`docs/design/references/Icon_and_Function_Naming_Standards_v1.1.docx`](design/references/Icon_and_Function_Naming_Standards_v1.1.docx)

When sources differ, use this precedence:

1. Shared component behavior and semantic tokens in the referenced technical baseline.
2. The approved UI standard version 2.1 for layout, typography, sizing, and component use.
3. The approved icon and naming standard version 1.1 for canonical icons and UI vocabulary.
4. Legacy screens only when they already comply with the standards above.

Update the shared component and this document in the same pull request when a new pattern or token is approved. Do not interpret a documentation update as proof that every legacy screen has already been migrated.

## 2. Core principles

- Reuse first. Inspect `components/ui`, `components/common`, and a stable screen of the same type before creating a component or style.
- Use shared primitives and semantic tokens. Do not hard-code a page-level value when a suitable primitive, variant, or token exists.
- Do not add a UI framework or icon library for one screen.
- Give color and state a business meaning; never add color only for decoration.
- Prefer whitespace and clear hierarchy over visual clutter.
- Keep routes, API contracts, backend fields, permissions, and business rules unchanged when only a UI label or icon changes.
- Do not copy legacy exceptions into new business screens.

Avoid random colors, heavy shadows, bright gradients, neon colors, arbitrary spacing, inconsistent radii, inconsistent button styles, emoji used as functional icons, and page-specific UI frameworks.

## 3. Frontend stack

| Area | Standard |
|---|---|
| Framework | React 19 and TypeScript |
| Styling | Tailwind CSS 4 |
| UI primitives | shadcn and `@base-ui/react` |
| Font | Geist Variable |
| Icons | `lucide-react` |
| Toasts | Sonner through the shared `AppToaster` |
| Forms | `react-hook-form` and Zod |
| Routing | React Router |

`antd` and `@ant-design/icons` remain in `package.json` as legacy dependencies. Do not introduce them into new UI or expand their use. The current standard is Tailwind, shadcn/Base UI, and Lucide.

## 4. Design tokens

Use semantic Tailwind utilities backed by the CSS variables in `frontend/src/index.css`. Do not choose a raw shade such as `bg-green-*` or `bg-blue-*` at page level when a semantic token or component variant expresses the intended meaning.

### 4.1 Light theme

| Token | Value | Role |
|---|---:|---|
| `--background` | `#F5F7FA` | Application background |
| `--foreground` | `#1F2937` | Primary text |
| `--card` | `#FFFFFF` | Card and panel surface |
| `--primary` | `#2E7D32` | Brand and primary emphasis |
| `--primary-hover` | `#256B29` | Primary hover |
| `--primary-light` | `#E8F5E9` | Light primary surface |
| `--secondary` | `#F1F8E9` | Secondary surface |
| `--muted` | `#F3F4F6` | Muted surface and table header |
| `--muted-foreground` | `#4B5563` | Secondary text |
| `--destructive` | `#D32F2F` | Error and destructive action |
| `--warning` | `#F9A825` | Warning |
| `--info` | `#1976D2` | Information |
| `--border` | `#E5E7EB` | Default border |
| `--input` | `#D1D5DB` | Input border |
| `--ring` | `#2E7D32` | Focus ring |
| `--disabled` | `#9CA3AF` | Disabled and placeholder text |
| `--table-header` | `#F3F4F6` | Table header |
| `--table-hover` | `#EDEFF2` | Table row hover |
| `--table-alternate` | `#F9FAFB` | Optional alternate row |
| `--label` | `#1F2937` | Field labels and column headings |
| `--success` | `#2E7D32` | Success state |

Supporting light surfaces are `--success-bg: #E8F5E9`, `--warning-bg: #FFF8E1`, `--error-bg: #FFEBEE`, and `--info-bg: #E3F2FD`.

Existing domain status tokens are `--status-draft: #9CA3AF`, `--status-pending: #F9A825`, `--status-approved: #2E7D32`, `--status-rejected: #D32F2F`, `--status-harvested: #7CB342`, `--status-packaged: #1565C0`, `--status-shipped: #00897B`, and `--status-completed: #00695C`. Consume them through an existing badge mapping rather than applying them directly at page level.

### 4.2 Dark theme

Dark mode does not currently require a user-facing toggle, but all new code must remain compatible with the `.dark` tokens already defined in `frontend/src/index.css`. Prefer utilities such as `bg-card`, `text-foreground`, and `border-border`. If an established pattern must use a direct emerald or slate utility, provide its corresponding dark variant.

Do not hard-code a white background or black text when a semantic token exists.

### 4.3 Status semantics

| Tone | Meaning |
|---|---|
| `success` | Successful, active, or approved |
| `warning` | Pending or warning |
| `danger` | Error, rejected, cancelled, destructive, or recalled |
| `info` | Informational or intermediate state |
| `neutral` | Draft, inactive, or neutral |

Reuse `StatusBadge` or an existing domain badge mapping. A status must always include a text label; color alone must not carry meaning. Do not create a new page-level status color mapping when a shared or domain mapping already exists.

## 5. Typography

Use `'Geist Variable', sans-serif` globally. Pages and components must not declare their own font family.

| Context | Preferred utility | Size |
|---|---|---:|
| Internal page title | `text-2xl font-bold tracking-tight` | 24px |
| Large card title | `text-xl font-semibold` | 20px |
| Dialog or warning title | `text-lg font-semibold` | 18px |
| Small section title | `text-sm font-semibold` | 14px |
| Body, table, and form text | `text-sm` | 14px |
| Emphasized body text | `text-base` | 16px |
| Description or supporting text | `text-sm text-muted-foreground` | 14px |
| Metadata or detail label | `text-xs text-muted-foreground` | 12px |
| Input and Textarea | `text-base md:text-sm` | 16px below 768px; 14px from 768px |
| Small Card title | `text-base font-semibold` | 16px |

Use `text-foreground` for primary text, `text-muted-foreground` for descriptions, `text-label` for labels, and `text-disabled` for placeholder or disabled text. Do not invent additional gray levels when the semantic tokens are sufficient.

## 6. Spacing, radius, and shadow

| Purpose | Preferred utility | Value |
|---|---|---:|
| Very small separation | `gap-1` or `gap-1.5` | 4-6px |
| Icon to text or closely related controls | `gap-2` | 8px |
| Toolbar controls | `gap-3` | 12px |
| Grid, form, or card content | `gap-4` | 16px |
| Major sections | `gap-6` or `space-y-6` | 24px |
| Compact panel padding | `p-3` | 12px |
| Card or list content padding | `p-4` | 16px |
| Large form section padding | `p-5` or `p-6` | 20px or 24px |

Do not use arbitrary values such as 13px, 19px, or 27px without a documented technical reason.

Radii are component-specific:

- Input and Select: `rounded-lg`, 10px.
- Default, small, and `icon-sm` Button: `--radius-md`, 8px.
- Card: `rounded-xl`, 14px.
- Base radius: `0.625rem`, 10px.

The standard card shadow is `0 2px 8px rgba(0, 0, 0, 0.04)`. Avoid nesting multiple shadowed cards; use a border and muted background for a child panel.

## 7. Application layout and responsive behavior

| Item | Standard |
|---|---|
| Header | 64px, `h-16`, sticky at the top |
| Desktop sidebar | 272px, `w-[17rem]` |
| Content maximum width | 1280px, `max-w-7xl` |
| Mobile | Below 768px |
| Tablet | 768-1279px |
| Desktop | 1280px and above |
| Major content separation | 24px, `space-y-6` |

Internal pages must use `MainLayout`; they must not recreate the header, sidebar, or outer container. Do not add large outer padding when `MainLayout` already provides it.

| Viewport width | Page padding | Sidebar behavior |
|---|---:|---|
| Below 640px | 12px, `p-3` | Drawer opened from Header |
| 640-767px | 16px, `sm:p-4` | Drawer opened from Header |
| 768-1023px | 20px, `md:p-5` | Hidden; toggle opens overlay |
| 1024-1279px | 24px, `lg:p-6` | Hidden; toggle opens overlay |
| 1280px and above | 32px, `xl:p-8` | Fixed at 272px |

`ListPageHeader` and `ListToolbar` stack vertically below 640px and become horizontal from 640px. Forms use one column below 768px and normally two columns from 768px. These content breakpoints are independent from sidebar behavior.

Use `AppBreadcrumb` as the standard navigation mechanism. Do not add a redundant Back button to an internal page that is already covered by the breadcrumb.

## 8. Standard page patterns

### 8.1 List page

Compose list screens from `ListPageHeader`, `ListCard`, `ListToolbar`, `SearchInput`, `FilterSelect`, optional `RefreshButton`, `DataTableShell`, `Pagination`, and `StatusBadge`.

Place the page header above the list card with 24px separation. In a wide toolbar, place search and filters on the left and refresh or secondary actions on the right. Stack the toolbar below 640px. Use shared components whenever they meet the need.

### 8.2 Create and edit page

- Use `Card`, `CardHeader`, `CardTitle`, `CardDescription`, and `CardContent`.
- Build fields with `Label` and `Input`, `Select`, or `Textarea`; use `Alert` when a warning is required.
- Use one column by default and two columns from `md`, with `gap-4`.
- Put Cancel or secondary actions before the primary action, normally right-aligned.

### 8.3 Detail page

Prefer `DetailSection`, `DetailField`, and `StatusBadge` or the existing domain badge. A child panel normally uses `rounded-lg border bg-muted/30 p-4`. Use a 12px muted detail label and a 14px medium value. Show missing data as `—`; render IDs, codes, IP addresses, and coordinates in monospace when useful.

## 9. Component standards

### 9.1 Button

| Variant | Purpose |
|---|---|
| `default` | Primary action |
| `create` | Create or add data |
| `outline` | Secondary action, Cancel, or secondary View |
| `secondary` | Secondary action with a surface |
| `ghost` | Icon action or low-emphasis action |
| `destructive` or `delete` | Destructive delete or cancel action |
| `search` | Search |
| `edit` | Emphasized edit action |
| `view` | Emphasized view action |
| `link` | Link action |

| Size | Visual height or size |
|---|---:|
| `xs` | 24px |
| `sm` and `default` | 36px |
| `lg` | 44px |
| `icon-xs` | 24px; use only in a suitable context |
| `icon-sm` | 32px |
| `icon` | 40px |
| `icon-lg` | 44px |

The shared `default` and `create` variants currently provide the established green-600 and green-700 hover treatment. This is an implementation detail of the shared variant, not permission for individual pages to choose arbitrary green shades.

On coarse-pointer devices, the shared CSS enforces a minimum 44x44px interaction target for relevant interactive elements. The visible icon size and the interaction target are separate concerns; verify the rendered control so local styles do not reduce the target.

### 9.2 Input, Select, and Textarea

- Input: 44px high, `border-input`, 16px horizontal padding.
- Select: 44px by default; 36px for `size="sm"`; `border-border`; 14px text.
- Textarea: `min-h-11` and `field-sizing-content`; never force it to remain 44px high.
- Input and Textarea text: 16px below 768px and 14px from 768px.
- Preserve the primitive's focus ring, placeholder, border, and disabled behavior.
- Every business form field requires a visible label; a placeholder is not a label.
- Validation requires a clear message or state, not only a red border.

### 9.3 Card and panel

Use `rounded-xl border bg-card shadow-card`. A default Card has a 20px title and 24px spacing. A small Card has a 16px title and 16px spacing. Descriptions are 14px. `ListCard` uses `CardContent p-4`; do not infer that every `CardContent` must use 16px padding.

Use `rounded-xl border bg-muted/30 p-4` for a child panel; `DetailSection` may use `rounded-lg`.

### 9.4 Table and data list

| Item | Standard |
|---|---|
| Font | 14px |
| Header height | 40px |
| Target row height | 56px through `h-14`; may grow for content |
| Cell padding | 16px |
| Header background | `bg-table-header` |
| Row hover | `bg-table-hover` |
| Header label | `font-medium text-label` |

Use `DataTableShell` for loading and empty states. On mobile, use `responsive-table-cards` or horizontal scrolling; do not shrink text until it becomes difficult to read.

Show no more than three frequently used actions directly in a row. Move remaining actions into the `Thêm thao tác` menu with `Ellipsis`; do not label this menu only `Thêm`, which can be confused with creating data. An icon-only table action uses `Button variant="ghost" size="icon-sm"` with a 16px icon and a clear accessible label. A text action such as `Xem` or `Chi tiết` may use `outline` and `sm`.

### 9.5 Dialog, confirmation, and toast

- Use `ConfirmDialog` or `AlertDialog` with a title, a consequence description, Cancel, and a specific action such as `Xóa`, `Thu hồi`, or `Từ chối`.
- A destructive action must not use a generic `Xác nhận` label when a precise consequence is available.
- Provide a loading state to prevent duplicate submissions.
- Standard dialogs use `rounded-xl`, a responsive maximum width, `p-4` content, and the shared footer treatment.
- Use the shared `AppToaster` in `components/ui/toast.tsx`: Sonner, top-right, five-second default, 20px icon.
- Do not use `window.alert()` or `window.confirm()` in new UI.

### 9.6 Breadcrumb

Internal screens in `MainLayout` use `AppBreadcrumb`. Text is 14px, the final item is semibold, and the `ChevronRight` separator is 14px. API-dependent labels use the override mechanism rather than displaying a raw UUID or technical ID.

## 10. Icon system

Use icons only from `lucide-react` in new UI.

| Context | Size and rule |
|---|---|
| Page-defined action icon | 16px, `size-4` |
| Default icon in default or small Button | 14px unless explicitly sized |
| Default icon in `xs` or `icon-xs` Button | 12px |
| Sidebar and toast | 20px, `size-5` |
| Page header | 24px, `size-6` |
| Status badge | 12px, `size-3` |
| Breadcrumb separator | 14px, `size-3.5` |
| Navigation or page icon color | Existing component pattern; `ListPageHeader` uses emerald-600 and dark emerald-400 |
| Neutral icon color | `text-muted-foreground` |
| Error or destructive icon color | Component `destructive` variant or `danger` tone |

Do not override a primitive's default icon size solely to force one universal number. Do not use emoji as functional icons, mix icon libraries in new UI, or give icons in one cluster unrelated colors. A module's Sidebar icon and page-header icon must use the same canonical icon.

Text carries the primary meaning. Every icon-only control requires an `aria-label` or equivalent accessible name; tooltip or `title` is additional visual help. Decorative icons use `aria-hidden`.

### 10.1 Canonical action icons and labels

| Function | Preferred Vietnamese label | Lucide icon | Rule |
|---|---|---|---|
| Create or add | `Tạo …` or `Thêm …` | `Plus` | Usually keep visible text |
| Save | `Lưu` | `Save` | Do not substitute `Check` for form save |
| Edit | `Chỉnh sửa` | `Pencil` | 16px in a table action |
| View or detail | `Xem` or `Chi tiết` | `Eye` | Use text when context is unclear |
| Delete | `Xóa` | `Trash2` | Destructive; confirm significant consequences |
| Refresh | `Làm mới` | `RefreshCw` | Do not select a different reload icon per page |
| Search | `Tìm kiếm` | `Search` | Prefer `SearchInput` |
| Filter | `Lọc` | `ListFilter` | Prefer `FilterSelect` or toolbar pattern |
| More actions | `Thêm thao tác` | `Ellipsis` | For less common row actions |
| Export generated file | `Xuất …` | `FileDown` | Use `Download` for an existing file |
| Import data | `Nhập …` | `FileUp` | Data imported into the system |
| Confirm or approve | `Xác nhận` or `Duyệt` | `Check` | Use `ShieldCheck` for verification/certification |
| Reject | `Từ chối` | `XCircle` | Danger semantics |
| Warning | `Cảnh báo` | `AlertTriangle` | Not for a normal action |
| Scan QR | `Quét mã` | `ScanLine` | `QrCode` represents a code; `ScanLine` triggers scanning |
| Close | `Đóng` | `X` | Close is not Delete |
| Upload attachment | `Tải lên` | `Upload` | File or image attachment, not data import |
| Download existing file | `Tải xuống` | `Download` | Distinct from generated export |

## 11. User-facing naming

User-facing UI is Vietnamese, sentence case, and uses consistent business vocabulary. Do not mix `Create`, `Delete`, `Submit`, or `Back` with `Tạo`, `Xóa`, `Lưu`, and the project breadcrumb pattern. This English design document intentionally preserves canonical Vietnamese labels exactly as they must appear in the product.

Use one canonical name and icon per concept across Sidebar, breadcrumb, list-page title, and page-header icon. Create, edit, detail, and workflow screens add an action verb appropriate to the operation.

### 11.1 Naming by position

- Menu group: use a short noun phrase for scope, such as `Quản lý`, `Vận hành sản xuất`, or `Thống kê & Báo cáo`.
- Menu item and list page: prefer the short entity or module name, such as `Tổ chức`, `Vùng trồng`, `Lô sản xuất`, or `Chứng nhận`. Omit redundant `Danh sách` or `Quản lý` prefixes.
- Workflow: use a precise business verb, such as `Xác thực chứng nhận`, `Phân công địa bàn`, or `Ghi sự kiện vận chuyển`.
- Page title: normally match the menu item. Put actor, scope, and conditions in a short description rather than repeating or extending the title.
- Help: place lengthy business rules in `HelpButton` or help content.

| Screen type | Naming pattern | Example |
|---|---|---|
| Create | `Tạo + {thực thể}` | `Tạo lô sản xuất` |
| Edit | `Chỉnh sửa + {thực thể}` | `Chỉnh sửa vùng trồng` |
| Detail | `Chi tiết + {thực thể}` | `Chi tiết lô hàng` |
| Workflow | `{Động từ nghiệp vụ} + {đối tượng}` | `Xác thực chứng nhận` |

Button and CTA labels begin with a verb and state the consequence when needed: `Tạo`, `Thêm`, `Lưu`, `Cập nhật`, `Xóa`, `Hủy`, `Xem`, `Chỉnh sửa`, `Làm mới`, `Tìm kiếm`, `Lọc`, `Xuất`, `Tải xuống`, `Quét mã`, `Xác nhận`, `Duyệt`, `Từ chối`, `Kích hoạt`, or `Thu hồi`. Do not use vague labels such as `OK`, `Submit`, `Action`, or `Click here`. In a dialog, Cancel comes before the primary or destructive action.

### 11.2 UI labels and code identifiers

| Layer | Rule | Example |
|---|---|---|
| UI label | Vietnamese, sentence case, canonical business vocabulary | `Lô sản xuất` |
| React component | PascalCase plus component role | `ProductionLotListPage` |
| Handler or function | camelCase beginning with a verb | `handleDelete`, `loadProductionLots` |
| Boolean | Begin with `is`, `has`, `can`, or `should` | `isLoading`, `canEdit` |
| Route, API, backend field | Preserve the existing technical contract | Map a Vietnamese label in UI |

### 11.3 Canonical module names and icons

| Concept | Canonical UI name | Canonical icon | Scope or distinction |
|---|---|---|---|
| Overview | `Tổng quan` | `LayoutDashboard` | Replace Sidebar `Dashboard`; keep `/dashboard` |
| Organization | `Tổ chức` | `Building2` | Organization list |
| Member | `Thành viên` | `Users` | Permission actions retain their own labels |
| Permission | `Phân quyền` | `ShieldCheck` | Permission configuration navigation |
| Farm area | `Vùng trồng` | `MapPinned` | Reserve `MapPin` for a point or jurisdiction |
| Production lot | `Lô sản xuất` | `Sprout` | Use consistently in Sidebar and page header |
| Shipment | `Lô hàng` | `Package` | Distinct from production lot |
| Chain progress | `Tiến độ chuỗi` | `Activity` | Put per-lot detail in description |
| Transportation | `Ghi sự kiện vận chuyển` | `Truck` | Workflow keeps its business verb |
| Quick scan | `Quét mã ghi sự kiện nhanh` | `ScanLine` | An in-page button may say `Quét mã` |
| Storage | `Bảo quản` | `Thermometer` | Preserve existing term |
| Label alert | `Cảnh báo tem bất thường` | `AlertTriangle` | Align Sidebar and breadcrumb |
| Code range | `Dải mã truy xuất` | `Hash` | Omit redundant `Quản lý` in module name |
| Certification | `Chứng nhận` | `Award` | Explain testing purpose in description or tab |
| Verification | `Xác thực chứng nhận` | `ShieldCheck` | Workflow distinct from certification list |
| Product feedback | `Phản ánh sản phẩm` | `MessageSquare` | One name for the same module |
| Recall | `Yêu cầu thu hồi` | `PackageX` | Group or list name |
| Per-shipment recall | `Yêu cầu thu hồi từng lô hàng` | `PackageX` | Preserve distinction from scoped recall |
| Scoped recall | `Yêu cầu thu hồi lô hàng theo phạm vi` | `PackageX` | Preserve business scope |
| Create recall | `Tạo yêu cầu thu hồi` | `AlertTriangle` | Creation workflow, not list icon |
| Impact trace | `Truy vết phạm vi ảnh hưởng` | `GitCompare` | Preserve full business meaning |
| Third-party API key | `Khóa API bên thứ ba` | `Key` | Keep API as a technical term |
| Pending sync | `Sự kiện chờ đồng bộ` | `WifiOff` | Explain offline state with text |
| Sent handover | `Phiếu bàn giao đã gửi` | `FileSignature` | Sender perspective |
| Received handover | `Phiếu bàn giao nhận` | `FileSignature` | Receiver perspective |
| Warehouse receipt | `Nhập kho` | `Warehouse` | `Phiếu nhập kho` may name a record/detail |
| Product category | `Danh mục nông sản` | `Layers` | `Danh mục` is part of the business concept |
| Inspection criterion | `Chỉ tiêu kiểm nghiệm` | `FlaskConical` | Preserve specialist meaning |
| Cultivation milestone | `Mốc canh tác` | `CalendarCheck` | Preserve existing term |
| Testing unit | `Đơn vị kiểm nghiệm` | `ShieldCheck` | Label distinguishes the entity |
| Input material | `Danh mục vật tư` | `PackageCheck` | Preserve `Danh mục` |
| Quality standard | `Tiêu chuẩn chất lượng` | `BookOpen` | Align Sidebar, breadcrumb, and list title |
| Area assignment | `Phân công địa bàn` | `MapPin` | Assignment workflow |
| Reports | `Thống kê & Báo cáo` | `PieChart` | Group name; exports use `FileDown` |

Workflow names such as `Xác thực chứng nhận`, `Duyệt yêu cầu …`, and `Ghi sự kiện vận chuyển` must retain their verbs; they are not ordinary CRUD list names.

### 11.4 Known baseline migrations

The following differences existed at the referenced baseline. Treat the target column as the standard and update all related UI locations together when that area is changed. Do not claim a migration is complete until the source has been verified.

| Baseline difference | Standard target | Locations to align |
|---|---|---|
| Sidebar `Dashboard`; breadcrumb `Tổng quan` | `Tổng quan` and `LayoutDashboard` | Sidebar and related titles; keep route |
| Production-lot Sidebar `Package`; page `Sprout` | `Lô sản xuất` and `Sprout` | Sidebar icon; retain correct page icon |
| Sidebar `Bảng tiến độ chuỗi`; long per-lot title | `Tiến độ chuỗi` and `Activity` | Sidebar, breadcrumb override, page title |
| Sidebar `Nhận phản ánh`; registry `Phản hồi người dùng` | `Phản ánh sản phẩm` | Sidebar, registry, override, title |
| Sidebar `Cấu hình phân quyền`; registry `Cấu hình quyền` | `Phân quyền` | Navigation and title; do not change permissions |
| Sidebar `Quản lý thành viên`; registry `Thành viên & quyền` | `Thành viên` | Module name; preserve permission action labels |
| Sidebar `Tiêu chuẩn chất lượng`; registry `Tiêu chuẩn` | `Tiêu chuẩn chất lượng` | Registry and list title |
| Sidebar `Khóa API bên thứ ba`; registry `Khóa API đối tác` | `Khóa API bên thứ ba` | Registry and related titles |
| Sidebar `Cảnh báo tem bất thường`; registry `Cảnh báo quét nghi vấn` | `Cảnh báo tem bất thường` | Registry and related titles |
| Sidebar `Chứng nhận`; registry `Kiểm nghiệm & chứng nhận` | `Chứng nhận` | Module label; preserve testing content in description/tab |

## 12. Accessibility and state handling

At minimum, verify mobile below 768px, tablet from 768px through 1279px, and desktop from 1280px. During implementation review, explicitly inspect 375, 640, 768, 1024, and 1280px widths.

- A dialog must remain inside the viewport.
- A table must use an appropriate responsive card pattern or horizontal scrolling.
- Do not reduce touch targets to fit more actions.
- Verify Tab navigation, visible focus, and keyboard activation.
- Icon-only actions require accessible names; decorative icons are hidden from assistive technology.
- Business inputs require labels.
- Loading, empty, error, disabled, and success states must be identifiable.
- Important information must not be conveyed by color alone.

## 13. Legacy exceptions

Do not use the following as templates for new business UI:

| Existing exception | Rule |
|---|---|
| Login and Auth use pill radii, glass/backdrop blur, and large shadows | Treat as an Auth-specific visual style |
| Legacy pages contain hard-coded slate, emerald, or blue utilities | Preserve only when narrowly editing that component; do not add new shades |
| Legacy dialogs contain hard-coded button colors | Use shared `Button` or `AlertDialog` variants in new work |
| Header logout dialog contains hard-coded blue and gray | Treat as legacy debt, not a standard |

The following previous values must not be propagated:

| Previous value | Current standard |
|---|---|
| 32px page title | 24px, `text-2xl` |
| 16px default body | 14px for common body/table; responsive Input/Textarea sizes |
| 44px default Button | 36px desktop; 44px for `lg` or coarse-pointer target |
| `#F8FAFC` table header | `--table-header: #F3F4F6` |
| Fixed 24px page padding | Responsive 12/16/20/24/32px from `MainLayout` |

## 14. Adding a pattern, token, name, or icon

Before adding anything new:

1. Check whether `components/ui` already supports it.
2. Check whether `components/common` already supports it.
3. Inspect a stable screen of the same type.
4. Try composing the behavior from existing primitives without a new style.
5. Check whether an existing semantic token expresses the meaning.

Only after all five checks fail may a new pattern or token be proposed. Name it by meaning, add light and dark values when theme-dependent, document the reason and affected scope, and update this document in the same pull request.

For a new function name or icon, first identify whether it is a module, CRUD page, or business workflow. Search Sidebar, breadcrumb, backlog, and related pages for established vocabulary. Reuse the canonical entry when available. If no canonical entry exists, choose the clearest business term and Lucide icon, add it to the catalog in this document, and verify title, description, CTA, breadcrumb, responsive length, and accessible naming.

## 15. Review checklist

- [ ] Internal screen uses `MainLayout` and the standard breadcrumb.
- [ ] Existing shared components were checked before creating a new one.
- [ ] List, create/edit, or detail composition follows the matching page pattern.
- [ ] Typography, spacing, radius, shadow, and tokens match this document.
- [ ] Button, Input, Select, Textarea, Card, Table, Dialog, Toast, and Badge use shared primitives and variants.
- [ ] No new UI framework, icon library, arbitrary color, gradient, or emoji icon was added.
- [ ] Sidebar, breadcrumb, page title, header icon, CTA, and tooltip use consistent canonical vocabulary and icons.
- [ ] UI labels remain Vietnamese; vague or mixed-language actions were removed.
- [ ] A row exposes no more than three common actions; remaining actions use `Thêm thao tác`.
- [ ] Icon-only controls have accessible names and appropriate interaction targets.
- [ ] Loading, empty, error, disabled, success, and destructive states are handled.
- [ ] Responsive behavior was checked at 375, 640, 768, 1024, and 1280px without unintended horizontal overflow.
- [ ] Dark theme compatibility was preserved.
- [ ] Any approved new token, pattern, name, or icon was added here in the same pull request.

## 16. Implementation references

Revalidate the documented values if the technical baseline changes materially.

- `frontend/src/index.css` - font, semantic colors, radii, shadow, responsive utilities, and touch targets.
- `frontend/src/components/layout/MainLayout.tsx` - layout breakpoints, page padding, Sidebar, and content area.
- `frontend/src/components/layout/Header.tsx` - 64px application header.
- `frontend/src/components/layout/Sidebar.tsx` - navigation labels, routes, and canonical module icons.
- `frontend/src/components/common/ListPageHeader.tsx` - page title and page icon.
- `frontend/src/components/common/ListToolbar.tsx` - filter and action placement.
- `frontend/src/components/common/ListCard.tsx` - list container.
- `frontend/src/components/common/DataTableShell.tsx` - loading and empty table states.
- `frontend/src/components/common/StatusBadge.tsx` - shared status semantics and icons.
- `frontend/src/components/common/AppBreadcrumb.tsx` - route label registry and override mechanism.
- `frontend/src/components/ui/button.tsx` - variants, sizes, icon behavior, and radius.
- `frontend/src/components/ui/input.tsx` - height and responsive font size.
- `frontend/src/components/ui/select.tsx` - default and small variants.
- `frontend/src/components/ui/textarea.tsx` - minimum and content-driven height.
- `frontend/src/components/ui/card.tsx` - spacing and typography by size.
- `frontend/src/components/ui/table.tsx` - table cells and rows.
- `frontend/src/components/ui/toast.tsx` - application-wide toast behavior.
- `frontend/src/components/ui/Breadcrumb.tsx` - typography and separator icon.
- Tailwind CSS responsive design rules - breakpoint semantics.

## 17. Change history

- **2.1 - 2026-09-12:** Consolidated the approved common UI standard 2.1 and icon/naming standard 1.1; aligned the document with the referenced source baseline; replaced conflicting version 1.0 values; added responsive layout, component variants, canonical Vietnamese naming, icon catalogs, accessibility, dark-mode guidance, legacy migration notes, and one unified review checklist.
- **1.0:** Initial design-system document; superseded by version 2.1.
