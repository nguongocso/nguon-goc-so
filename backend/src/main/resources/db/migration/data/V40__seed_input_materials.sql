-- Seed data for Input Materials (US NCL-09-CN-010-CV-01)

INSERT IGNORE INTO input_materials 
(id, name, material_group, active_ingredient, unit, quarantine_days, apply_to_all_crops, reference_source, is_active, created_at)
VALUES
('018f9d00-0001-7000-8000-000000000001', 'Brightin 4.0EC', 'PESTICIDE', 'Abamectin', 'ml', 7, TRUE, 'Thông tư 10/2020/TT-BNNPTNT', TRUE, NOW()),
('018f9d00-0002-7000-8000-000000000002', 'Dithane M-45 80WP', 'PESTICIDE', 'Mancozeb', 'g', 14, TRUE, 'QCVN 01-132:2013/BNNPTNT, Thông tư 10/2020/TT-BNNPTNT', TRUE, NOW()),
('018f9d00-0003-7000-8000-000000000003', 'Amistar Top 325SC', 'PESTICIDE', 'Azoxystrobin + Difenoconazole', 'ml', 7, TRUE, 'Danh mục Thuốc BVTV BNNPTNT', TRUE, NOW()),
('018f9d00-0004-7000-8000-000000000004', 'Prevathon 35WG', 'PESTICIDE', 'Chlorantraniliprole', 'g', 3, TRUE, 'Thông tư 10/2020/TT-BNNPTNT', TRUE, NOW()),
('018f9d00-0005-7000-8000-000000000005', 'Confidor 100SL', 'PESTICIDE', 'Imidacloprid', 'ml', 7, TRUE, 'Danh mục Thuốc BVTV Bộ NN&PTNT', TRUE, NOW()),
('018f9d00-0006-7000-8000-000000000006', 'Anvil 5SC', 'PESTICIDE', 'Hexaconazole', 'ml', 14, TRUE, 'Thông tư 10/2020/TT-BNNPTNT', TRUE, NOW()),
('018f9d00-0007-7000-8000-000000000007', 'Proclaim 1.9EC', 'PESTICIDE', 'Emamectin benzoate', 'ml', 7, TRUE, 'Danh mục Thuốc BVTV Bộ NN&PTNT', TRUE, NOW()),
('018f9d00-0008-7000-8000-000000000008', 'Basta 15SL', 'PESTICIDE', 'Glufosinate ammonium', 'lít', 14, TRUE, 'Thông tư 10/2020/TT-BNNPTNT', TRUE, NOW()),
('018f9d00-0009-7000-8000-000000000009', 'Coc 85WP', 'PESTICIDE', 'Copper Oxychloride', 'g', 7, TRUE, 'Tiêu chuẩn VietGAP TCVN 11892-1:2017', TRUE, NOW()),
('018f9d00-0010-7000-8000-000000000010', 'Phân bón NPK 16-16-8+TE', 'FERTILIZER', 'N, P2O5, K2O, TE', 'kg', 0, TRUE, 'Nghị định 84/2019/NĐ-CP', TRUE, NOW()),
('018f9d00-0011-7000-8000-000000000011', 'Phân Đạm Ure Hà Bắc', 'FERTILIZER', 'Nitrogen (N 46.3%)', 'kg', 0, TRUE, 'TCVN 2637:2015, Nghị định 84/2019/NĐ-CP', TRUE, NOW()),
('018f9d00-0012-7000-8000-000000000012', 'Phân Hữu cơ Vi sinh Sông Gianh', 'FERTILIZER', 'Hữu cơ 15%, Azotobacter, Bacillus spp.', 'kg', 0, TRUE, 'TCVN 9268:2012 Phân bón hữu cơ vi sinh', TRUE, NOW()),
('018f9d00-0013-7000-8000-000000000013', 'Phân bón lá Humic Acid Premium', 'FERTILIZER', 'Potassium Humate 85%, Fulvic Acid 10%', 'kg', 0, TRUE, 'Nghị định 84/2019/NĐ-CP', TRUE, NOW()),
('018f9d00-0014-7000-8000-000000000014', 'Phân Kali Clorua (KCl 60%)', 'FERTILIZER', 'Potassium Oxide (K2O 60%)', 'kg', 0, TRUE, 'Nghị định 84/2019/NĐ-CP', TRUE, NOW()),
('018f9d00-0015-7000-8000-000000000015', 'Trichoderma spp. Nông Nghiệp', 'BIOLOGICAL', 'Trichoderma harzianum / viride', 'kg', 0, TRUE, 'TCVN 10785:2015 Chế phẩm vi sinh', TRUE, NOW()),
('018f9d00-0016-7000-8000-000000000016', 'Vi sinh Bacillus thuringiensis (Bt)', 'BIOLOGICAL', 'Bacillus thuringiensis var. kurstaki', 'g', 3, TRUE, 'Danh mục Chế phẩm Biological BNNPTNT', TRUE, NOW()),
('018f9d00-0017-7000-8000-000000000017', 'Chế phẩm EM1 Nông nghiệp', 'BIOLOGICAL', 'Vi khuẩn Lactic, Vi khuẩn quang hợp, Nấm men', 'lít', 0, TRUE, 'Quy trình canh tác sinh thái VietGAP', TRUE, NOW()),
('018f9d00-0018-7000-8000-000000000018', 'Vôi bột Nông nghiệp', 'OTHER', 'Calcium Oxide (CaO 70%)', 'kg', 0, TRUE, 'TCVN 11793:2017 Vôi bón nông nghiệp', TRUE, NOW()),
('018f9d00-0019-7000-8000-000000000019', 'Màng phủ Nông nghiệp 2 mặt', 'OTHER', 'Nhựa PE chống UV', 'cuộn', 0, TRUE, 'Tiêu chuẩn phụ trợ canh tác VietGAP', TRUE, NOW()),
('018f9d00-0020-7000-8000-000000000020', 'Chất điều hòa sinh trưởng Atonik 1.8SL', 'OTHER', 'Sodium Nitrophenolate', 'ml', 7, TRUE, 'Danh mục Điều hòa sinh trưởng BNNPTNT', TRUE, NOW());
