# API Docs - Tao yeu cau kiem nghiem cho lo

Cap nhat theo code hien tai: 2026-08-26
Nguon doi chieu: DOCX `NCL-11-CN-002-Tao yeu cau kiem nghiem cho lo.docx` va module `backend/certification`.

Tai lieu nay phan biet ro yeu cau cua DOCX voi hanh vi da duoc implement. Ten lop trong DOCX (`QualityTestRequest`, `TestCriteria`) khong phai ten dang dung trong project hien tai.

## 1. Luong nghiep vu hien tai

1. VT-02 lay danh sach tieu chi ap dung cho lo qua `GET /api/v1/production-lots/{lotId}/test-criteria`.
2. Backend chi cho phep tao yeu cau neu lo thuoc organization cua nguoi dung, co trang thai tu `APPROVED` tro len, khong bi `REJECTED`, va da co su kien `HARVEST`.
3. Client gui don vi kiem nghiem, ngay gui mau va danh sach ID dinh nghia tieu chi qua `POST /api/v1/production-lots/{lotId}/test-requests`.
4. Moi dinh nghia tieu chi phai ton tai va thuoc `Standard` da gan voi lo. Backend tao snapshot `InspectionCriterion` trong yeu cau, luu code/name/standard tai thoi diem tao.
5. Yeu cau moi duoc tao voi trang thai domain `PENDING_RESULT`; response API tra ve chuoi `PENDING`.
6. Neu da co yeu cau `PENDING_RESULT` cung bo tieu chi cho lo, backend tra `409 CONFLICT`, tru khi client gui `confirmDuplicate = true`.
7. Sau khi tao yeu cau, ket qua co the duoc ghi tung tieu chi bang `POST`, hoac ghi toan bo bang `PUT` tai cap request. Chi tiet luong nay nam trong [inspection-result.md](inspection-result.md).

## 2. API lay tieu chi cua lo

### `GET /api/v1/production-lots/{lotId}/test-criteria`

- Quyen: `VT-02`.
- Lo phai thuoc organization hien tai va thoa dieu kien lo nhu muc 1.
- Backend lay `Standard` dau tien trong danh sach certification cua lo, sau do lay cac `InspectionCriterionDefinition` cua standard theo `id ASC`.
- Neu lo khong co certification hoac khong co standard hop le, response thanh cong voi danh sach `criteria` rong.

Response data thuc te:

```json
{
  "lotId": "<uuid>",
  "standardId": "<uuid>",
  "standardName": "VietGAP",
  "criteria": [
    {
      "criteriaId": 101,
      "code": "RESIDUE_PESTICIDE",
      "name": "Du luong thuoc tru sau"
    }
  ]
}
```

> `isMandatory` co trong mo ta DOCX nhung khong co trong `ProductionLotTestCriteriaResponse` hien tai.

## 3. API tao yeu cau

### `POST /api/v1/production-lots/{lotId}/test-requests`

- Quyen: `VT-02`.
- Chi truy cap duoc lot trong organization hien tai.
- Thanh cong: `201 CREATED`.

Request body:

```json
{
  "testingUnit": "Trung tam kiem nghiem",
  "sampleSentDate": "2026-08-13",
  "criteriaIds": [101, 102],
  "confirmDuplicate": false
}
```

| Truong | Kieu | Bat buoc | Rang buoc thuc te |
|---|---|---:|---|
| `testingUnit` | string | Co | Khong rong sau khi trim |
| `sampleSentDate` | `YYYY-MM-DD` | Co | Khong duoc lon hon ngay hien tai |
| `criteriaIds` | `int[]` | Co | It nhat mot ID; khong null, khong trung lap |
| `confirmDuplicate` | boolean | Khong | Mac dinh `false`; `true` cho phep tao yeu cau trung bo tieu chi dang cho |

Response data thuc te:

```json
{
  "testRequestId": "<uuid>",
  "lotId": "<uuid>",
  "lotCode": "LOT-2026-000123",
  "status": "PENDING",
  "testingUnit": "Trung tam kiem nghiem",
  "sampleSentDate": "2026-08-13",
  "criteria": [
    {
      "criteriaId": null,
      "code": "RESIDUE_PESTICIDE",
      "name": "Du luong thuoc tru sau",
      "standardId": "<uuid>",
      "standardName": "VietGAP"
    }
  ],
  "createdBy": "Ten nguoi tao",
  "createdAt": "2026-08-26T09:00:00"
}
```

> Luu y: `criteriaId` trong danh sach criteria cua response tao hien dang duoc map thanh `null`. ID snapshot UUID dung cho ghi ket qua duoc tra ve trong API chi tiet `GET /api/v1/inspection-requests/{requestId}`.

### Cac dieu kien tu choi

| HTTP | Dieu kien / thong diep thuc te |
|---:|---|
| `400` | Request rong; thieu `testingUnit`, `sampleSentDate` hoac `criteriaIds`; ngay gui mau o tuong lai; ID tieu chi null/trung; tieu chi khong ton tai; tieu chi khong thuoc standard cua lo |
| `404` | Lo khong ton tai trong organization hien tai |
| `409` | Da co request `PENDING_RESULT` cung bo khoa `standardId:criterionCode` va `confirmDuplicate` khong phai `true` |
| `403` | Nguoi dung khong co role `VT-02` |

Kiem tra trung lap khong phu thuoc thu tu danh sach. Vi du `[A, B]` va `[B, A]` la cung mot bo tieu chi. Chi request dang `PENDING_RESULT` moi duoc dung de phat hien trung lap.

## 4. API danh sach va chi tiet yeu cau

### `GET /api/v1/test-requests`

- Quyen: `VT-02`.
- Query tuy chon: `lotId`, `status`, `page` mac dinh `0`, `size` mac dinh `20`.
- `status` khi gui vao phai dung ten enum domain: `PENDING_RESULT`, `PASSED`, `FAILED`, `CANCELLED`.
- Response map `PENDING_RESULT` thanh `PENDING`; cac trang thai con lai giu nguyen ten.
- Khi khong co `lotId`, ket qua luon duoc scope theo organization hien tai.
- Moi item co `criteriaCount`, `failedCriteriaCount`, `failedRatio`; `failedCriteriaCount` duoc tinh bang truy van group cho ca trang, khong N+1.

### `GET /api/v1/inspection-requests/{requestId}`

- Tra request, snapshot tieu chi va result da ghi; tieu chi chua co result co `result: null`.
- Kiem tra organization boundary; request cua organization khac duoc xem nhu khong ton tai.
- Cac truong tong hop: `totalCriteria`, `evaluatedCriteria`, `passedCriteria`, `failedCriteriaCount`, `failedRatio`.

## 5. Trang thai va audit

Enum hien tai: `PENDING_RESULT`, `PASSED`, `FAILED`, `CANCELLED`. Khong co `RESULTED` va khong co `EXPIRED`.

- Tao moi: `PENDING_RESULT` (response: `PENDING`).
- Ghi ket qua chua du tat ca tieu chi: van `PENDING_RESULT`.
- Du tat ca, tat ca `passed = true` va `expiryDate >= today`: `PASSED`.
- Du tat ca nhung co tieu chi khong dat hoac het han: `FAILED`.
- Request `FAILED` duoc phep ghi/cap nhat lai ket qua; day la diem khac voi mo ta cu chi cho phep `PENDING_RESULT`.

Tao request va ghi/cap nhat/xoa ket qua deu phat hanh activity log voi actor, organization va entity lien quan.

## 6. Doi chieu DOCX voi code

| Noi dung trong DOCX | Thuc te trong project |
|---|---|
| `QualityTestRequest`, `TestCriteria` | `InspectionRequest`, `InspectionCriterionDefinition`, `InspectionCriterion` |
| `isMandatory` trong criteria response | Chua co trong response hien tai |
| `PENDING, PASSED, FAILED, EXPIRED` | Domain la `PENDING_RESULT, PASSED, FAILED, CANCELLED`; response map `PENDING_RESULT` thanh `PENDING` |
| Repository `existsBy...CriteriaSetEquals` | Service tu tao key `standardId:criterionCode` va so sanh tap |
| `resource_type = QualityTestRequest` | Activity log hien tai dung `entityType = INSPECTION_REQUEST` |
| Story chi tao request | Code hien tai da co day du luong ghi ket qua tung tieu chi va batch |

## Nguon code doi chieu

- `backend/src/main/java/vn/nguongocso/certification/controller/InspectionRequestController.java`
- `backend/src/main/java/vn/nguongocso/certification/service/impl/InspectionRequestServiceImpl.java`
- `backend/src/main/java/vn/nguongocso/certification/entity/InspectionRequest.java`
- `backend/src/main/java/vn/nguongocso/certification/entity/InspectionCriterion.java`
- `backend/src/main/java/vn/nguongocso/certification/dto/request/CreateInspectionRequest.java`
- `backend/src/main/java/vn/nguongocso/certification/dto/response/ProductionLotTestCriteriaResponse.java`
- `backend/src/test/java/vn/nguongocso/certification/service/InspectionRequestServiceImplTest.java`
