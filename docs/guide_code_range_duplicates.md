# Dọn dữ liệu trùng lặp `code_ranges`

> Liên quan đến lỗi:
> `IncorrectResultSizeDataAccessException: Query did not return a unique result: 2 results were returned`
> tại `ShipmentServiceImpl.findAvailableCodeRange` / `EventValidationServiceImpl.deleteDraft`.

## 1. Nguyên nhân

Bảng `code_ranges` **không có ràng buộc `UNIQUE` trên `organization_id`** (chỉ `prefix` là UNIQUE – xem `V9__create_chain_events_and_code_ranges.sql`). Khi một tổ chức có nhiều dòng `code_ranges`, phương thức `findByOrganizationOrganizationId` (trả `Optional<CodeRange>` – single result) sẽ ném `NonUniqueResultException`.

Code đã được sửa để chọn **dải mã mới nhất** (`findFirstByOrganizationOrganizationIdOrderByCreatedAtDesc`). Tuy nhiên vẫn cần dọn dữ liệu trùng lặp để tránh dùng sai dải mã (tiêu thụ mã từ dải cũ thay vì dải mới).

## 2. Kiểm tra dữ liệu trùng lặp

```sql
SELECT organization_id, COUNT(*) AS cnt
FROM code_ranges
GROUP BY organization_id
HAVING COUNT(*) > 1;
```

Xem chi tiết các dòng trùng:

```sql
SELECT id, organization_id, prefix, from_number, to_number, total_limit, used_count, created_at
FROM code_ranges
WHERE organization_id IN (
    SELECT organization_id
    FROM code_ranges
    GROUP BY organization_id
    HAVING COUNT(*) > 1
)
ORDER BY organization_id, created_at ASC;
```

## 3. Cách dọn

### 3.1. Giữ dải mã mới nhất, xoá dải cũ (an toàn nhất)

```sql
-- Kiểm tra trước khi xoá
SELECT cr.id, cr.organization_id, cr.prefix
FROM code_ranges cr
JOIN code_ranges cr2
  ON cr.organization_id = cr2.organization_id
 AND cr.created_at < cr2.created_at;

-- Xoá (sau khi đã sao lưu database)
DELETE cr
FROM code_ranges cr
JOIN code_ranges cr2
  ON cr.organization_id = cr2.organization_id
 AND cr.created_at < cr2.created_at;
```

> ⚠️ Luôn sao lưu bảng trước khi chạy `DELETE`.

### 3.2. Nếu `created_at` giống nhau (dữ liệu seed)

```sql
DELETE cr
FROM code_ranges cr
LEFT JOIN (
    SELECT organization_id, MAX(id) AS keep_id
    FROM code_ranges
    GROUP BY organization_id
) k ON cr.id = k.keep_id
WHERE k.keep_id IS NULL;
```

## 4. Chống tái diễn

Khi dữ liệu đã sạch, thêm ràng buộc `UNIQUE` trên `organization_id` (migration mới):

```sql
ALTER TABLE code_ranges
  ADD CONSTRAINT uk_code_range_org UNIQUE (organization_id);
```

> Chỉ chạy sau khi đã dọn trùng lặp; nếu còn trùng, `ALTER TABLE` sẽ thất bại.