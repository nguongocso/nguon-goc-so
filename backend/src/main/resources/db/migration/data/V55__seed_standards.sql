-- ============================================================
-- V49: Seed 15 quality standards (bảng standards)
--
-- Thêm 15 tiêu chuẩn chất lượng nông sản phổ biến để phục vụ
-- danh mục Tiêu chuẩn chất lượng (quản lý bởi role VT-01).
-- Idempotent: chạy lại an toàn nhờ khóa UNIQUE(name) + PK.
-- ============================================================

INSERT INTO standards (id, name, description, issuing_body, is_active, created_at, updated_at) VALUES
('f0892236-d181-4b3c-a4cd-2d394829a877', 'VietGAP', 'Thực hành nông nghiệp tốt cho trồng trọt tại Việt Nam theo VietGAP', 'Bộ Nông nghiệp và Phát triển nông thôn', TRUE, NOW(), NOW()),
('9e24816d-b3e5-4531-b990-2d2f0432f1f3', 'GlobalG.A.P.', 'Hệ thống đảm bảo thực hành nông nghiệp tốt toàn cầu (gồm Phần Cơ sở, Crops, Fruit & Vegetables)', 'GLOBALG.A.P.', TRUE, NOW(), NOW()),
('4b4b8984-26cb-4e14-b580-971f036a6f60', 'HACCP (TCVN 5603)', 'Phân tích mối nguy và kiểm soát điểm tới hạn trong quá trình sản xuất, chế biến thực phẩm', 'Bộ Khoa học và Công nghệ', TRUE, NOW(), NOW()),
('dd066377-adcc-4e1a-8ed5-0a08537b3e0b', 'ISO 22000:2018', 'Hệ thống quản lý an toàn thực phẩm theo ISO 22000:2018', 'Tổ chức Tiêu chuẩn hóa Quốc tế (ISO)', TRUE, NOW(), NOW()),
('e7096bbb-195d-4b70-9689-05d78f866d01', 'FSSC 22000', 'Chương trình chứng nhận an toàn thực phẩm dựa trên ISO 22000 và PAS 220', 'Foundation FSSC 22000', TRUE, NOW(), NOW()),
('3c6b90c5-be00-4392-a732-ca84a6b8b9cb', 'TCVN 11041-2:2017', 'Nông nghiệp hữu cơ - Phần 2: Trồng trọt hữu cơ', 'Bộ Khoa học và Công nghệ', TRUE, NOW(), NOW()),
('a45e212c-7fcd-407d-9772-17c262444286', 'USDA Organic', 'Tiêu chuẩn sản xuất hữu cơ của Hoa Kỳ (National Organic Program)', 'Bộ Nông nghiệp Hoa Kỳ (USDA)', TRUE, NOW(), NOW()),
('e3fee032-fa14-4e69-aa49-d0f3752cc0a9', 'EU Organic', 'Quy chuẩn sản xuất hữu cơ Liên minh châu Âu (EU 2018/848)', 'Ủy ban châu Âu (European Commission)', TRUE, NOW(), NOW()),
('487fd53e-efaa-4b60-b992-e97f29900248', 'Rainforest Alliance', 'Chứng nhận nông nghiệp bền vững về môi trường, xã hội và kinh tế', 'Rainforest Alliance', TRUE, NOW(), NOW()),
('7ee67ee6-1738-41d2-9195-177725dbbdb5', 'Fairtrade', 'Chứng nhận thương mại công bằng cho sản phẩm nông nghiệp', 'FLOCERT', TRUE, NOW(), NOW()),
('58cd8068-cd76-4645-b660-7d358ddd01c0', 'BRCGS Food Safety', 'Tiêu chuẩn an toàn thực phẩm toàn cầu BRCGS', 'BRCGS (Brand Reputation Compliance Global Standards)', TRUE, NOW(), NOW()),
('aa2f70d8-47a3-4ad9-ae7c-5394362be9a3', 'IFS Food', 'Tiêu chuẩn an toàn thực phẩm IFS Food', 'IFS Management GmbH', TRUE, NOW(), NOW()),
('03d3fd4b-6876-452d-b7b8-73e973f7f01f', 'SQF', 'Tiêu chuẩn chất lượng và an toàn thực phẩm SQF', 'SQFI (Safe Quality Food Institute)', TRUE, NOW(), NOW()),
('6c6a02e8-ae2d-451f-a1f4-314714350072', 'ASEAN GAP', 'Quy phạm thực hành nông nghiệp tốt quốc gia ASEAN', 'Ban thư ký ASEAN', TRUE, NOW(), NOW()),
('2e2ec365-91ee-4edc-b7a8-972746f7dddb', 'Codex Alimentarius (rau quả tươi)', 'Tiêu chuẩn chất lượng và an toàn rau quả tươi của Codex Alimentarius', 'FAO/WHO (Codex Alimentarius)', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    issuing_body = VALUES(issuing_body),
    is_active = VALUES(is_active),
    updated_at = NOW();