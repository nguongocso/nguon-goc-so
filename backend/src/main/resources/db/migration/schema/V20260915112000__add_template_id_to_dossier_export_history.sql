-- NCL-07-CN-007: Lưu mẫu hồ sơ đã áp dụng khi xuất bộ hồ sơ truy xuất hàng loạt
ALTER TABLE dossier_export_history
    ADD COLUMN template_id CHAR(36) NULL,
    ADD CONSTRAINT fk_dos_exp_hist_template FOREIGN KEY (template_id)
        REFERENCES profile_templates (id) ON DELETE SET NULL;
