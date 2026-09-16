-- ============================================================
-- V20260915101000: Cập nhật tên tiếng Anh (name_en) cho các tiêu chuẩn chất lượng đã seed
-- User Story: NCL-06-CN-004 - Trang tra cứu công khai bằng tiếng Anh
-- ============================================================

UPDATE standards SET name_en = 'VietGAP' WHERE name = 'VietGAP';
UPDATE standards SET name_en = 'GlobalG.A.P.' WHERE name = 'GlobalG.A.P.';
UPDATE standards SET name_en = 'HACCP (TCVN 5603)' WHERE name = 'HACCP (TCVN 5603)';
UPDATE standards SET name_en = 'ISO 22000:2018' WHERE name = 'ISO 22000:2018';
UPDATE standards SET name_en = 'FSSC 22000' WHERE name = 'FSSC 22000';
UPDATE standards SET name_en = 'TCVN 11041-2:2017 Organic' WHERE name = 'TCVN 11041-2:2017';
UPDATE standards SET name_en = 'USDA Organic' WHERE name = 'USDA Organic';
UPDATE standards SET name_en = 'EU Organic' WHERE name = 'EU Organic';
UPDATE standards SET name_en = 'Rainforest Alliance' WHERE name = 'Rainforest Alliance';
UPDATE standards SET name_en = 'Fairtrade' WHERE name = 'Fairtrade';
UPDATE standards SET name_en = 'BRCGS Food Safety' WHERE name = 'BRCGS Food Safety';
UPDATE standards SET name_en = 'IFS Food' WHERE name = 'IFS Food';
UPDATE standards SET name_en = 'SQF' WHERE name = 'SQF';
UPDATE standards SET name_en = 'ASEAN GAP' WHERE name = 'ASEAN GAP';
UPDATE standards SET name_en = 'Codex Alimentarius (Fresh Produce)' WHERE name = 'Codex Alimentarius (rau quả tươi)';
