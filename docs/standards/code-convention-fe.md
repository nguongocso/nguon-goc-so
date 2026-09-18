# Quy Ước Lập Trình Frontend (ReactJS + TypeScript) - Dự Án Nguồn Gốc Số

Tài liệu này định nghĩa bộ quy chuẩn lập trình, định dạng mã nguồn và tổ chức kiến trúc áp dụng bắt buộc cho toàn bộ mã nguồn Frontend (ReactJS + TypeScript) của dự án **Nguồn Gốc Số**. Mọi thành viên và AI Agent khi viết hoặc review code phải tuân thủ nghiêm ngặt các quy định này.

---

## 1. Triết Lý Clean Code & Nguyên Tắc Chung

1. **Code tự mô tả (Self-documenting Code)**: Ưu tiên tên component, hàm, biến có tính biểu đạt cao. Người đọc nhìn vào tên là hiểu ngay hành vi hiển thị hoặc logic nghiệp vụ.
2. **Không bẩn mã nguồn**: Nghiêm cấm để lại `console.log()`, `debugger`, code cũ bị comment lại, hoặc các đoạn mã thử nghiệm tạm bợ trong PR.
3. **Single Responsibility (SRP) cho Component**: Một component chỉ nên tập trung hiển thị hoặc xử lý một phần giao diện cụ thể. Component không được vượt quá 200 dòng (nếu vượt quá phải chia tách thành sub-components).
4. **TypeScript Strict Mode**: Tuyệt đối **không sử dụng kiểu `any`**. Định nghĩa interface/type tường minh cho Props, State và API Response.
5. **Đồng nhất Formatter**: Sử dụng UTF-8, thụt lề 2 khoảng trắng (Spaces), dấu nháy kép cho JSX và nháy đơn cho chuỗi trong TypeScript (tuân thủ ESLint + Prettier cấu hình dự án).

---

## 2. Quy Chuẩn Xuống Dòng (Line Wrapping) & Cách Dòng Trống (Vertical Blank Lines)

### 2.1. Quy tắc giới hạn độ dài & Xuống dòng (Line Wrapping)
* **Độ dài tối đa**: Tối đa **120 ký tự/dòng**. Bất kỳ câu lệnh hoặc thẻ JSX nào vượt quá 120 ký tự đều phải xuống dòng.
* **JSX Props Wrapping**:
  * Khi một component có **từ 3 props trở lên** hoặc dòng khai báo thẻ JSX dài vượt quá 80 ký tự: **Mỗi prop phải nằm trên 1 dòng riêng**, thụt lề thêm 2 spaces.
  * Dấu đóng thẻ `>` hoặc `/>` phải đặt ở **dòng mới riêng biệt**, thụt lề thẳng hàng với thẻ mở đầu.
  ```tsx
  // ĐÚNG
  <DataTable
    columns={tableColumns}
    dataSource={categoryList}
    loading={isLoading}
    pagination={{ pageSize: 10, total: totalRecords }}
    onChange={handleTableChange}
  />

  // SAI (Dồn ép tất cả props trên 1 dòng dài)
  <DataTable columns={tableColumns} dataSource={categoryList} loading={isLoading} pagination={{ pageSize: 10 }} onChange={handleTableChange} />
  ```
* **Destructuring Props & State dài**:
  * Khi destructure nhiều biến (từ 3 thuộc tính trở lên) hoặc vượt quá 100 ký tự: Xuống dòng sau dấu mở `{`, mỗi thuộc tính trên 1 dòng.
  ```tsx
  // ĐÚNG
  const {
    productCategoryId,
    maxScansPerHour,
    maxScansPerDay,
    onSuccess,
    onCancel,
  } = props;
  ```
* **Chained Calls & Promises**:
  * Xuống dòng **TRƯỚC dấu chấm `.`** cho Promise chaining hoặc mảng filter/map/reduce, thụt lề thêm 1 cấp.
  ```tsx
  // ĐÚNG
  const filteredList = data
    .filter(item => item.isActive)
    .map(item => ({ label: item.name, value: item.id }));
  ```
* **Toán tử logic & Toán tử 3 ngôi (Ternary Operator)**:
  * Khi biểu thức điều kiện dài: Xuống dòng **TRƯỚC toán tử** `?` và `:`.
  ```tsx
  // ĐÚNG
  const statusBadge = isScanningActive
    ? <Badge status="processing" text="Đang giám sát" />
    : <Badge status="default" text="Tạm dừng" />;
  ```

### 2.2. Quy tắc Cách dòng trống (Vertical Blank Lines)
* **1 dòng trống** giữa:
  * Khối `import` và khai báo Interface Props / Types.
  * Giữa các nhóm import: Thư viện ngoài (`react`, `antd`, `axios`) -> Thành phần nội bộ (Components, Hooks) -> Utils/Services/Types -> Styles CSS.
  * Interface Props và định nghĩa Component.
  * **Cấu trúc vòng đời bên trong Component**:
    1. Nhóm Hooks (React hooks & Custom hooks)
    2. *(1 dòng trống)*
    3. Nhóm State (`useState`)
    4. *(1 dòng trống)*
    5. Nhóm Effects (`useEffect`)
    6. *(1 dòng trống)*
    7. Nhóm Event Handlers (`handleSubmit`, `handleClick`)
    8. *(1 dòng trống)*
    9. Khối `return ( ... )` JSX.
* **Tuyệt đối CẤM**:
  * ❌ **Cấm 2 dòng trống liên tiếp** ở bất kỳ đâu trong file `.ts` / `.tsx` / `.css`.
  * ❌ **Cấm dòng trống** ngay sau dấu mở `{` hoặc ngay trước dấu đóng `}`.
  * ❌ **Cấm dòng trống thừa** ở đầu file hoặc cuối file.

---

## 3. Quy Chuẩn Vàng Về JSDoc & Comment (Clean Commenting)

> **Khẩu quyết**: *"Giao diện và mã nguồn React phải trực quan, dễ đọc. Comment chỉ dùng để giải thích TẠI SAO (Why), cấm tường thuật cú pháp hiển nhiên làm bẩn JSX."*

### 3.1. Giới hạn độ dài và nội dung JSDoc
* **Component JSDoc (Tối đa 2 - 4 dòng)**:
  * Viết bằng **tiếng Việt có dấu**.
  * Nêu ngắn gọn vai trò của Component, màn hình sử dụng và mã User Story (VD: `NCL-783`).
  ```tsx
  /**
   * Form cấu hình ngưỡng cảnh báo quét QR theo danh mục nông sản (NCL-08-CN-014).
   * Cho phép thiết lập số lượt quét tối đa theo giờ, ngày và khoảng cách tọa độ.
   */
  ```
* **Custom Hook JSDoc (Tối đa 2 - 4 dòng)**:
  * Giải thích ngắn gọn bài toán nghiệp vụ / trạng thái mà hook quản lý, tham số đầu vào và giá trị trả về.
  ```tsx
  /**
   * Hook quản lý trạng thái phân trang và bộ lọc tìm kiếm cho danh sách cảnh báo.
   * @param initialFilter Bộ lọc mặc định ban đầu
   * @returns Danh sách dữ liệu, trạng thái loading, hàm đổi trang và hàm reload
   */
  ```
* **Hàm API Service JSDoc (Tối đa 2 - 3 dòng)**:
  * 1 câu ngắn gọn nêu endpoint và mục đích.
  ```tsx
  /**
   * Gửi yêu cầu cập nhật ngưỡng cảnh báo theo danh mục lên server.
   */
  export const updateCategoryThreshold = async (...) => { ... };
  ```

### 3.2. Nghiêm cấm Noise Comments & Dead Code trong Frontend
* ❌ **Cấm comment hiển nhiên trong JSX và hàm**:
  ```tsx
  // SAI: Comment rác làm bẩn mã nguồn
  // Khởi tạo state loading
  const [loading, setLoading] = useState(false);

  {/* Hiển thị nút bấm */}
  <button onClick={handleClick}>Lưu</button>
  ```
* ❌ **Cấm comment lại JSX hoặc code cũ (Dead Code)**:
  ```tsx
  // SAI: Không comment code cũ, bắt buộc xóa bỏ hoàn toàn
  {/* <OldAlertBanner message={error} /> */}
  ```
* ❌ **Cấm `console.log()` và `// TODO:` sót lại**: Khi mở PR, bot sẽ quét và từ chối merge nếu phát hiện `console.log` hoặc `TODO`.

---

## 4. Quy Chuẩn Đặt Tên (Naming Convention)

| Thành phần | Quy tắc | Ví dụ ĐÚNG | Ví dụ SAI |
|---|---|---|---|
| **Component File** | PascalCase, đuôi `.tsx` | `CategoryThresholdForm.tsx`, `AlertTable.tsx` | `categoryThresholdForm.tsx`, `alert-table.tsx` |
| **Component Name** | PascalCase | `CategoryThresholdForm`, `AlertBadge` | `categoryThresholdForm`, `Alert_Badge` |
| **Custom Hook** | camelCase, bắt đầu bằng `use`, đuôi `.ts` | `useCategoryThreshold.ts`, `useDebounce.ts` | `categoryThresholdHook.ts`, `UseDebounce.ts` |
| **Service File** | camelCase, kết thúc bằng `Service.ts` | `alertService.ts`, `categoryService.ts` | `AlertService.ts`, `alert_service.ts` |
| **Interface / Type**| PascalCase, **không có tiền tố I** | `CategoryThresholdRequest`, `AlertFilter` | `ICategoryThresholdRequest`, `TAlertFilter` |
| **Event Handler** | Bắt đầu bằng `handle...` | `handleSubmit`, `handleSearch`, `handleCloseModal` | `submitForm`, `onClickBtn`, `search` |
| **Prop Callback** | Bắt đầu bằng `on...` | `onSuccess`, `onFilterChange`, `onClose` | `successCallback`, `changeFilter` |
| **Boolean State** | Bắt đầu bằng `is`, `has`, `should` | `isLoading`, `hasPermission`, `isOpenModal` | `loading`, `permission`, `open` |
| **CSS Class** | kebab-case hoặc BEM | `.category-threshold-form`, `.alert-table__row--active` | `.CategoryForm`, `.alert_table_row` |

---

## 5. Quy Chuẩn Chi Tiết Theo Từng Thành Phần (Component / Hook / API)

### 5.1. Component Kiến Trúc Chuẩn (`src/components/` hoặc `src/pages/`)
* **Thứ tự bố cục bắt buộc bên trong một Component**:
  1. Imports (thư viện ngoài -> nội bộ -> types -> styles)
  2. Interface Props
  3. Khai báo Component (`const MyComponent: React.FC<Props> = (...) => {`)
  4. Custom Hooks & React Hooks (`useNavigate`, `useTranslation`, `useTheme`...)
  5. State (`useState`)
  6. Effects (`useEffect`)
  7. Handlers (`handleSave`, `handleDelete`...)
  8. Return JSX
* **Mẫu Component chuẩn**:

```tsx
import React, { useState, useEffect } from 'react';
import { Button, Form, InputNumber, notification } from 'antd';
import { alertService } from '../../services/alertService';
import { CategoryThresholdRequest } from '../../types/alert';
import './CategoryThresholdForm.css';

/**
 * Props của form cấu hình ghi đè ngưỡng theo danh mục.
 */
interface CategoryThresholdFormProps {
  productCategoryId: string;
  initialValues?: CategoryThresholdRequest;
  onSuccess: () => void;
  onCancel: () => void;
}

/**
 * Form cấu hình ngưỡng cảnh báo quét QR theo danh mục nông sản (NCL-08-CN-014).
 */
export const CategoryThresholdForm: React.FC<CategoryThresholdFormProps> = ({
  productCategoryId,
  initialValues,
  onSuccess,
  onCancel,
}) => {
  const [form] = Form.useForm();
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  useEffect(() => {
    if (initialValues) {
      form.setFieldsValue(initialValues);
    }
  }, [initialValues, form]);

  const handleSubmit = async (values: CategoryThresholdRequest) => {
    try {
      setIsSubmitting(true);
      await alertService.saveCategoryThreshold({
        ...values,
        productCategoryId,
      });
      notification.success({ message: 'Lưu cấu hình ngưỡng cảnh báo thành công' });
      onSuccess();
    } catch (error) {
      notification.error({ message: 'Không thể lưu cấu hình, vui lòng thử lại' });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Form
      form={form}
      layout="vertical"
      className="category-threshold-form"
      onFinish={handleSubmit}
    >
      <Form.Item
        name="maxScansPerHour"
        label="Số lượt quét tối đa mỗi giờ"
        rules={[{ required: true, message: 'Vui lòng nhập số lượt quét tối đa mỗi giờ' }]}
      >
        <InputNumber min={1} className="category-threshold-form__input" />
      </Form.Item>

      <Form.Item
        name="maxScansPerDay"
        label="Số lượt quét tối đa mỗi ngày"
        rules={[{ required: true, message: 'Vui lòng nhập số lượt quét tối đa mỗi ngày' }]}
      >
        <InputNumber min={1} className="category-threshold-form__input" />
      </Form.Item>

      <div className="category-threshold-form__actions">
        <Button onClick={onCancel} disabled={isSubmitting}>
          Hủy bỏ
        </Button>
        <Button type="primary" htmlType="submit" loading={isSubmitting}>
          Lưu cấu hình
        </Button>
      </div>
    </Form>
  );
};
```

---

### 5.2. Custom Hook Chuẩn (`src/hooks/use*.ts`)
* **Quy tắc**:
  * Tách biệt logic quản lý trạng thái phức tạp ra khỏi UI.
  * Luôn bắt đầu bằng `use`.
  * Khai báo đầy đủ dependency array cho `useCallback`, `useEffect` để tránh stale closure.
  * Phải có cơ chế cleanup nếu lắng nghe sự kiện hoặc set timeout/interval.

```tsx
import { useState, useCallback, useEffect } from 'react';
import { alertService } from '../services/alertService';
import { CategoryThresholdResponse } from '../types/alert';

/**
 * Hook quản lý nạp và làm mới cấu hình ngưỡng cảnh báo theo danh mục.
 */
export const useCategoryThreshold = (categoryId: string) => {
  const [threshold, setThreshold] = useState<CategoryThresholdResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchThreshold = useCallback(async () => {
    if (!categoryId) return;
    try {
      setIsLoading(true);
      setError(null);
      const data = await alertService.getCategoryThreshold(categoryId);
      setThreshold(data);
    } catch (err) {
      setError('Lỗi khi tải thông tin ngưỡng cảnh báo');
    } finally {
      setIsLoading(false);
    }
  }, [categoryId]);

  useEffect(() => {
    fetchThreshold();
  }, [fetchThreshold]);

  return { threshold, isLoading, error, refetch: fetchThreshold };
};
```

---

### 5.3. Tầng Service / Gọi API (`src/services/*Service.ts`)
* **Quy tắc**:
  * Tập trung qua axios client chung (có interceptor xử lý token và lỗi HTTP).
  * Hàm API phải có kiểu dữ liệu trả về `Promise<T>` rõ ràng, không dùng `Promise<any>`.
  * ❌ **Cấm**: Tuyệt đối không hardcode baseURL hoặc API token trong mã nguồn.

```tsx
import apiClient from './apiClient';
import { CategoryThresholdRequest, CategoryThresholdResponse } from '../types/alert';
import { ApiResponse } from '../types/common';

/**
 * Service giao tiếp API quản lý cấu hình ngưỡng cảnh báo (NCL-08-CN-014).
 */
export const alertService = {
  /**
   * Lưu hoặc cập nhật cấu hình ngưỡng cảnh báo theo danh mục.
   */
  saveCategoryThreshold: async (
    payload: CategoryThresholdRequest
  ): Promise<CategoryThresholdResponse> => {
    const response = await apiClient.post<ApiResponse<CategoryThresholdResponse>>(
      '/api/v1/alert-thresholds/categories',
      payload
    );
    return response.data.data;
  },

  /**
   * Lấy chi tiết cấu hình ngưỡng cảnh báo theo ID danh mục.
   */
  getCategoryThreshold: async (
    categoryId: string
  ): Promise<CategoryThresholdResponse> => {
    const response = await apiClient.get<ApiResponse<CategoryThresholdResponse>>(
      `/api/v1/alert-thresholds/categories/${categoryId}`
    );
    return response.data.data;
  },
};
```

---

### 5.4. Quy Chuẩn CSS & Styling
* **Quy tắc**:
  * Sử dụng cấu trúc class theo chuẩn BEM (`.block__element--modifier`) hoặc kebab-case.
  * Tận dụng các biến màu và khoảng cách CSS Variables của hệ thống (`var(--color-primary)`, `var(--spacing-md)`).
  * **Độ co giãn Responsive**: Sử dụng đơn vị linh hoạt (`rem`, `%`, `flex`, `grid`), không dùng `width: 800px` cố định gây tràn màn hình ngang mobile.
  * Thẻ `<img>` bắt buộc phải có thuộc tính `alt` mô tả nội dung (chuẩn Accessibility A11y).
  * ❌ **Cấm**: Không lạm dụng `!important` tràn lan (chỉ dùng khi override CSS thư viện bên thứ 3 trong trường hợp bất khả kháng).

---

## 6. Bảng Tóm Tắt Tiêu Chuẩn Nhanh Cho Frontend & AI Reviewer

| Tiêu chí | Chuẩn quy định | Mức độ vi phạm nếu không đạt |
|---|---|---|
| Ngôn ngữ code & đặt tên | Tiếng Anh, camelCase / PascalCase | **Major** |
| TypeScript Type Safety | Strict mode, cấm hoàn toàn kiểu `any` | **Blocker** |
| Giới hạn JSDoc | ≤ 2 - 4 dòng cho Component/Hook, cấm comment rác | **Minor / Clutter** |
| Noise comments / Dead code | Không có comment hiển nhiên, cấm comment code JSX cũ | **Major / Clean Code** |
| Giới hạn độ dài dòng | ≤ 120 ký tự/dòng, ngắt dòng props JSX khi ≥ 3 props | **Minor** |
| Khoảng cách dòng trống | 1 dòng giữa các phần vòng đời Component; cấm 2 dòng trống liên tiếp | **Minor** |
| Kích thước Component | ≤ 200 dòng/file, tách sub-components khi quá dài | **Major** |
| Debug statements | Xóa sạch `console.log()` và `debugger` trước khi PR | **Blocker** |
| A11y & Form | Thẻ `<img>` có `alt`, thẻ input có label hoặc `aria-label` | **Major** |
| Layout Responsive | Không vỡ layout hoặc overflow-x trên thiết bị di động | **Blocker** |
