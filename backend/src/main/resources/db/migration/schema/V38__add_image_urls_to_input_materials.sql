-- Add image_urls column to input_materials table (US NCL-09-CN-010)

ALTER TABLE input_materials ADD COLUMN image_urls LONGTEXT DEFAULT NULL;
