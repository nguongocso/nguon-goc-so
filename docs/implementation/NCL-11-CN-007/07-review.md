# Final Review — NCL-11-CN-007

## 1. Phạm vi review

Đã review integrated diff của nhánh `feature/NCL-11-CN-007-inspection-result-entry-portal` sau khi merge `origin/develop`, đối chiếu với API contract, Implementation Contract, AC TC-01..04 và QTN-14/QTN-20/QTN-21.

## 2. Kết quả các finding cũ

| Finding | Trạng thái cuối | Bằng chứng |
|---|---|---|
| File upload lộ path/giả mạo ownership | resolved | Opaque handle bind `tokenHash + requestId + criterionId`; foreign handle test trả 403. |
| Concurrent issue tạo nhiều link ACTIVE | resolved | Pessimistic lock + atomic revoke; concurrency test chỉ còn một link ACTIVE. |
| Tenant query fallback unscoped | resolved | Đã bỏ fallback `findDetailById/findById`; authenticated flow chỉ dùng tenant-scoped query. |
| Manual/portal race | resolved | Hai luồng cùng khóa request; luồng đến sau bị 409/410. |
| Concurrent double-submit | resolved | Atomic `ACTIVE -> USED`; concurrency test xác minh chỉ một success. |
| API/doc mismatch | resolved | 201 issue, PUT submit, 5 MB, `testingUnitName`, opaque handle và provenance đã đồng bộ. |
| UI thiếu status/provenance | resolved | Internal page gọi latest-link và render provenance; portal có đủ loading/error/form states. |
| Thiếu test bảo mật/concurrency | resolved | 137 inspection tests PASS, gồm role, tenant, rate-limit, file scope và concurrency. |

## 3. Review theo yêu cầu

| Yêu cầu | Trạng thái | Nhận xét |
|---|---|---|
| TC-01 | verified | Luồng issue → public detail → submit → provenance được integration test xác minh. |
| TC-02 | verified | Expired link trả 410. |
| TC-03 | verified | Replay và concurrent submit được chặn. |
| TC-04 | verified | Manual entry revoke link và giữ actor nội bộ. |
| QTN-14 | verified | Tối đa một link ACTIVE. |
| QTN-20 | verified | Hash-only token, rate limit, no-store, object scope và generic public errors. |
| QTN-21 | verified | Status/expiry behavior dùng chung với luồng hiện hữu. |
| API contract | verified | Backend, frontend types và tài liệu khớp nhau. |
| UI runtime | verified | Playwright xác minh invalid và valid form states; valid state không có console error. |
| MySQL migration runtime | unverified | Không có Docker/MySQL trên máy; cần CI hoặc reviewer chạy clean/upgrade migration. |
| Full repo regression | failed-baseline | 5 backend H2 schema errors và 1 frontend NCL-07 assertion ngoài diff NCL-11. |

## 4. Rủi ro còn lại

1. Rate limit và opaque file-handle registry dùng bộ nhớ của một instance; triển khai multi-instance cần distributed store/sticky routing theo giới hạn đã ghi trong contract.
2. Tệp upload chưa submit có thể trở thành orphan; cleanup scheduler nằm ngoài phạm vi Story.
3. MySQL/Flyway clean và upgrade migration cần được chạy trong CI hoặc môi trường có MySQL 8.4 trước merge.
4. Full regression baseline cần được sửa ở Story/PR sở hữu `CodeRangeRepositoryTest`, `ShipmentSplitRelationshipRepositoryTest` và `OrganizationUsagePage.test.tsx`.

## 5. Kết luận

`REVIEW RESULT: FEATURE VERIFIED / READY FOR HUMAN REVIEW WITH DISCLOSED BASELINE LIMITATIONS`

Không còn BLOCKER hoặc MAJOR thuộc phạm vi NCL-11-CN-007. Không tuyên bố full-gate PASS vì migration MySQL chưa được chạy và repository regression vẫn đỏ ở các module ngoài Story.
