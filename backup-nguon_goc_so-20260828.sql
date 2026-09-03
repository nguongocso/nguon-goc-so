-- MySQL dump 10.13  Distrib 8.4.11, for Linux (x86_64)
--
-- Host: localhost    Database: nguon_goc_so
-- ------------------------------------------------------
-- Server version	8.4.11

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `account_locks`
--

DROP TABLE IF EXISTS `account_locks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `account_locks` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `anomaly_id` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `locked_by` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `lock_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `locked_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `lock_until` timestamp(3) NULL DEFAULT NULL,
  `permanent` tinyint(1) NOT NULL DEFAULT '0',
  `unlocked_by` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unlocked_at` timestamp(3) NULL DEFAULT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'LOCKED',
  PRIMARY KEY (`id`),
  KEY `fk_account_locks_anomaly` (`anomaly_id`),
  KEY `fk_account_locks_locked_by` (`locked_by`),
  KEY `fk_account_locks_unlocked_by` (`unlocked_by`),
  KEY `idx_account_locks_user_status` (`user_id`,`status`),
  KEY `idx_account_locks_user_locked_at` (`user_id`,`locked_at` DESC),
  CONSTRAINT `fk_account_locks_anomaly` FOREIGN KEY (`anomaly_id`) REFERENCES `login_anomalies` (`id`),
  CONSTRAINT `fk_account_locks_locked_by` FOREIGN KEY (`locked_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_account_locks_unlocked_by` FOREIGN KEY (`unlocked_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_account_locks_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `account_locks`
--

LOCK TABLES `account_locks` WRITE;
/*!40000 ALTER TABLE `account_locks` DISABLE KEYS */;
/*!40000 ALTER TABLE `account_locks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `activity_logs`
--

DROP TABLE IF EXISTS `activity_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity_logs` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `organization_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `full_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `action` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `entity_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `entity_id` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_activity_log_user` (`user_id`),
  KEY `idx_activity_log_created` (`created_at`),
  KEY `idx_activity_log_org` (`organization_id`),
  KEY `idx_activity_log_action` (`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `activity_logs`
--

LOCK TABLES `activity_logs` WRITE;
/*!40000 ALTER TABLE `activity_logs` DISABLE KEYS */;
INSERT INTO `activity_logs` VALUES ('0240edb3-b733-48a2-bfab-43013b5ebacc','8f89a500-a1c4-11f1-9ae2-029fd41577b3','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','admin','Quản trị viên hệ thống','CREATE_API_KEY','Cấp khóa truy cập cho đối tác \'22\' (mã khóa nks_live_07596a83...)','PARTNER_API_KEY','b3d327cc-9331-4ef7-ad1a-49d614d7dfbb','172.18.0.1','2026-08-27 15:21:32'),('1587567c-0855-4a95-9be9-bc25e31a0e78','8f89a500-a1c4-11f1-9ae2-029fd41577b3','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','admin','Quản trị viên hệ thống','CREATE_API_KEY','Cấp khóa truy cập cho đối tác \'10\' (mã khóa nks_live_c521b9d9...)','PARTNER_API_KEY','1545480a-72e4-4b83-b674-0f4695629fa6','172.18.0.1','2026-08-27 15:20:54'),('2bef2d48-daad-4675-b16b-a8d1e8fbb8f6','8f89a500-a1c4-11f1-9ae2-029fd41577b3','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','admin','Quản trị viên hệ thống','CREATE_PRODUCT_CATEGORY','Thêm mới loại nông sản: Taó, thuộc nhóm hàng: Cây ăn quả','PRODUCT_CATEGORY',NULL,'172.18.0.1','2026-08-28 09:36:59'),('3806ba24-527e-4fb6-8d68-d8665e998dfe','8f89a500-a1c4-11f1-9ae2-029fd41577b3','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','admin','Quản trị viên hệ thống','CREATE_API_KEY','Cấp khóa truy cập cho đối tác \'00\' (mã khóa nks_live_a1c6c2fa...)','PARTNER_API_KEY','3d05ba09-0979-4f93-8801-f274d02b9655','172.18.0.1','2026-08-27 15:22:13'),('3fb74edf-ca58-4c1c-816d-27a2606f20e5','8f89a500-a1c4-11f1-9ae2-029fd41577b3','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','admin','Quản trị viên hệ thống','CREATE_API_KEY','Cấp khóa truy cập cho đối tác \'ghgf\' (mã khóa nks_live_e78b623e...)','PARTNER_API_KEY','cf8b8991-8afd-4235-a0b9-d559e502eae2','172.18.0.1','2026-08-27 15:19:59'),('79ff8a60-7a1a-4d3a-9e97-f9bd3a6412d6','8f89a500-a1c4-11f1-9ae2-029fd41577b3','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','admin','Quản trị viên hệ thống','CREATE_PRODUCT_CATEGORY','Thêm mới loại nông sản: Nho, thuộc nhóm hàng: Quả ngọt','PRODUCT_CATEGORY',NULL,'172.18.0.1','2026-08-28 09:37:43'),('7f188edf-9670-4788-a2d0-5e1771ba7b74','8f89a500-a1c4-11f1-9ae2-029fd41577b3','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','admin','Quản trị viên hệ thống','CREATE_API_KEY','Cấp khóa truy cập cho đối tác \'66\' (mã khóa nks_live_7c5cb427...)','PARTNER_API_KEY','56139679-6b0a-43f7-8566-db4a9e60140e','172.18.0.1','2026-08-27 15:22:02'),('8c594fa3-0879-4ef6-998a-280b8273f8e1','8f89a500-a1c4-11f1-9ae2-029fd41577b3','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','admin','Quản trị viên hệ thống','CREATE_API_KEY','Cấp khóa truy cập cho đối tác \'52\' (mã khóa nks_live_90fb58d9...)','PARTNER_API_KEY','4e6c1954-e5fb-4cc3-ba48-541244e7b25e','172.18.0.1','2026-08-27 15:21:04'),('8e9e406d-8224-4ca5-9cf5-7825bc5a59b0','8f89a500-a1c4-11f1-9ae2-029fd41577b3','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','admin','Quản trị viên hệ thống','CREATE_API_KEY','Cấp khóa truy cập cho đối tác \'1\' (mã khóa nks_live_0595086d...)','PARTNER_API_KEY','e5ea56da-3d1a-488b-892a-3836318a311a','172.18.0.1','2026-08-27 15:21:21'),('aae1ea59-74c9-4b4c-b618-be77449ab325','8f89a500-a1c4-11f1-9ae2-029fd41577b3','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','admin','Quản trị viên hệ thống','CREATE_API_KEY','Cấp khóa truy cập cho đối tác \'8\' (mã khóa nks_live_78205368...)','PARTNER_API_KEY','c1bee202-e3e3-495f-aa40-3939a9c17a73','172.18.0.1','2026-08-27 15:21:52'),('b60fabdf-686d-4449-808c-3fbb1a5b0344','8f89a500-a1c4-11f1-9ae2-029fd41577b3','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','admin','Quản trị viên hệ thống','CREATE_API_KEY','Cấp khóa truy cập cho đối tác \'11\' (mã khóa nks_live_8edca786...)','PARTNER_API_KEY','28d14f9f-8f9e-4bf9-b970-e26aaf9cd053','172.18.0.1','2026-08-27 15:22:23'),('b917a4c0-1663-4e64-b154-fc3535a1385f','8f89a500-a1c4-11f1-9ae2-029fd41577b3','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','admin','Quản trị viên hệ thống','CREATE_PRODUCT_CATEGORY','Thêm mới loại nông sản: Dỗi, thuộc nhóm hàng: Cây ăn quả','PRODUCT_CATEGORY',NULL,'172.18.0.1','2026-08-28 09:37:08'),('df48f259-2614-47d1-9abd-7acabdccf77d','8f89a500-a1c4-11f1-9ae2-029fd41577b3','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','admin','Quản trị viên hệ thống','CREATE_API_KEY','Cấp khóa truy cập cho đối tác \'88\'\' (mã khóa nks_live_4dfda835...)','PARTNER_API_KEY','cb2a2400-389d-4229-9a0b-383d67395b88','172.18.0.1','2026-08-27 15:21:12'),('e0f5ed21-15a0-4536-9aca-d662e6698bb1','8f89a500-a1c4-11f1-9ae2-029fd41577b3','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','admin','Quản trị viên hệ thống','CREATE_PRODUCT_CATEGORY','Thêm mới loại nông sản: Chè, thuộc nhóm hàng: Chè','PRODUCT_CATEGORY',NULL,'172.18.0.1','2026-08-28 09:37:22'),('e5140095-90cf-450f-ba69-668953ee13f3','8f89a500-a1c4-11f1-9ae2-029fd41577b3','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','admin','Quản trị viên hệ thống','CREATE_PRODUCT_CATEGORY','Thêm mới loại nông sản: Bồng bồng, thuộc nhóm hàng: Cây ăn quả','PRODUCT_CATEGORY',NULL,'172.18.0.1','2026-08-28 09:36:47'),('e92a1720-fbce-48cd-a85e-a191d58b6f3a','8f89a500-a1c4-11f1-9ae2-029fd41577b3','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','admin','Quản trị viên hệ thống','CREATE_API_KEY','Cấp khóa truy cập cho đối tác \'36\' (mã khóa nks_live_15069fb3...)','PARTNER_API_KEY','11257050-90e9-4edd-b770-489f204020d6','172.18.0.1','2026-08-27 15:21:44');
/*!40000 ALTER TABLE `activity_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `alerts`
--

DROP TABLE IF EXISTS `alerts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alerts` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `related_entity_type` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `related_entity_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `severity` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `details` json NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `message` text COLLATE utf8mb4_unicode_ci,
  `organization_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL,
  `resolved_at` datetime DEFAULT NULL,
  `resolved_by` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_alert_org` (`organization_id`),
  KEY `idx_alert_type` (`type`),
  KEY `idx_alert_status` (`status`),
  KEY `idx_alert_created_at` (`created_at`),
  KEY `idx_alert_related_entity` (`related_entity_id`),
  CONSTRAINT `fk_alert_org` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`organization_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `alerts`
--

LOCK TABLES `alerts` WRITE;
/*!40000 ALTER TABLE `alerts` DISABLE KEYS */;
/*!40000 ALTER TABLE `alerts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `backup_restore_history`
--

DROP TABLE IF EXISTS `backup_restore_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `backup_restore_history` (
  `id` int NOT NULL AUTO_INCREMENT,
  `operation_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Loại thao tác: BACKUP hoặc RESTORE',
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Tên file backup',
  `file_path` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Đường dẫn vật lý tới file backup trên server',
  `file_size` bigint DEFAULT NULL COMMENT 'Kích thước file backup tính bằng bytes',
  `backup_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Loại backup (SCHEDULED: Tự động, MANUAL: Thủ công)',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Trạng thái (IN_PROGRESS, SUCCESS, FAILED)',
  `error_message` text COLLATE utf8mb4_unicode_ci COMMENT 'Chi tiết lỗi nếu thao tác thất bại',
  `reference_id` int DEFAULT NULL COMMENT 'ID bản ghi BACKUP gốc được dùng để khôi phục (chỉ cho RESTORE)',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Người thực hiện (NULL nếu do hệ thống chạy tự động)',
  PRIMARY KEY (`id`),
  KEY `fk_br_history_user` (`created_by`),
  KEY `fk_br_history_ref` (`reference_id`),
  KEY `idx_br_history_op_type` (`operation_type`),
  KEY `idx_br_history_status` (`status`),
  CONSTRAINT `fk_br_history_ref` FOREIGN KEY (`reference_id`) REFERENCES `backup_restore_history` (`id`),
  CONSTRAINT `fk_br_history_user` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `backup_restore_history`
--

LOCK TABLES `backup_restore_history` WRITE;
/*!40000 ALTER TABLE `backup_restore_history` DISABLE KEYS */;
INSERT INTO `backup_restore_history` VALUES (1,'BACKUP','backup_20260828_015959.sql.gz','/app/./backups/backup_20260828_015959.sql.gz',28250,'SCHEDULED','SUCCESS',NULL,NULL,'2026-08-28 02:00:00',NULL);
/*!40000 ALTER TABLE `backup_restore_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `backup_schedules`
--

DROP TABLE IF EXISTS `backup_schedules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `backup_schedules` (
  `id` int NOT NULL AUTO_INCREMENT,
  `cron_expression` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Cron expression xác định thời gian chạy',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Mô tả lịch sao lưu',
  `is_active` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Trạng thái kích hoạt (1: Active, 0: Inactive)',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_backup_schedules_user` (`updated_by`),
  CONSTRAINT `fk_backup_schedules_user` FOREIGN KEY (`updated_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `backup_schedules`
--

LOCK TABLES `backup_schedules` WRITE;
/*!40000 ALTER TABLE `backup_schedules` DISABLE KEYS */;
INSERT INTO `backup_schedules` VALUES (1,'0 0 2 * * ?','Sao lưu dữ liệu tự động hằng ngày lúc 02:00 sáng',1,'2026-08-27 03:08:21','2026-08-27 03:08:21',NULL);
/*!40000 ALTER TABLE `backup_schedules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `certifications`
--

DROP TABLE IF EXISTS `certifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `certifications` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `organization_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `standard_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `issuing_body` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `code` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `issued_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `issue_date` date DEFAULT NULL,
  `expiry_date` date NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `fk_cert_org` (`organization_id`),
  KEY `fk_cert_standard` (`standard_id`),
  CONSTRAINT `fk_cert_org` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`organization_id`),
  CONSTRAINT `fk_cert_standard` FOREIGN KEY (`standard_id`) REFERENCES `standards` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `certifications`
--

LOCK TABLES `certifications` WRITE;
/*!40000 ALTER TABLE `certifications` DISABLE KEYS */;
/*!40000 ALTER TABLE `certifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `chain_events`
--

DROP TABLE IF EXISTS `chain_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chain_events` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `shipment_id` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `event_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_data` json DEFAULT NULL,
  `location` geometry DEFAULT NULL,
  `recorded_at` datetime NOT NULL,
  `recorded_by` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL,
  `parent_event_id` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_correction` tinyint(1) NOT NULL DEFAULT '0',
  `hash` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `previous_hash` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_chain_event_user` (`recorded_by`),
  KEY `fk_chain_event_parent` (`parent_event_id`),
  KEY `idx_chain_events_shipment_id_recorded_at` (`shipment_id`,`recorded_at`),
  CONSTRAINT `fk_chain_event_parent` FOREIGN KEY (`parent_event_id`) REFERENCES `chain_events` (`id`),
  CONSTRAINT `fk_chain_event_shipment` FOREIGN KEY (`shipment_id`) REFERENCES `shipments` (`id`),
  CONSTRAINT `fk_chain_event_user` FOREIGN KEY (`recorded_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chain_events`
--

LOCK TABLES `chain_events` WRITE;
/*!40000 ALTER TABLE `chain_events` DISABLE KEYS */;
/*!40000 ALTER TABLE `chain_events` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `code_ranges`
--

DROP TABLE IF EXISTS `code_ranges`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `code_ranges` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `organization_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `prefix` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `from_number` bigint DEFAULT NULL,
  `to_number` bigint DEFAULT NULL,
  `total_limit` bigint NOT NULL,
  `used_count` bigint NOT NULL DEFAULT '0',
  `created_by` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `prefix` (`prefix`),
  KEY `fk_code_range_org` (`organization_id`),
  CONSTRAINT `fk_code_range_org` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`organization_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `code_ranges`
--

LOCK TABLES `code_ranges` WRITE;
/*!40000 ALTER TABLE `code_ranges` DISABLE KEYS */;
INSERT INTO `code_ranges` VALUES ('c1ea3ac1-a1d1-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED15',1,100000,100000,1000,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 04:42:49','2026-08-27 04:42:49'),('e138d871-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED01',1,100000,100000,95000,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),('e138e053-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED02',1,100000,100000,95000,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),('e138e6b0-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED03',1,100000,100000,95000,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),('e138e863-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED04',1,100000,100000,95000,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),('e138e9a5-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED05',1,100000,100000,60000,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),('e138eae0-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED06',1,100000,100000,60000,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),('e138ecd3-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED07',1,100000,100000,60000,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),('e138ef07-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED08',1,100000,100000,60000,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),('e138f1a5-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED09',1,100000,100000,60000,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),('e138f41e-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED10',1,100000,100000,1000,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),('e138f664-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED11',1,100000,100000,1000,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),('e1390291-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED12',1,100000,100000,1000,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),('e13906bb-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED13',1,100000,100000,1000,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),('e13908c4-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED14',1,100000,100000,1000,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57');
/*!40000 ALTER TABLE `code_ranges` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dossier_export_history`
--

DROP TABLE IF EXISTS `dossier_export_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dossier_export_history` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `shipment_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `exporter_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `organization_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `exported_at` datetime NOT NULL,
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_size` bigint DEFAULT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_export_shipment` (`shipment_id`),
  KEY `fk_export_exporter` (`exporter_id`),
  KEY `fk_export_org` (`organization_id`),
  CONSTRAINT `fk_export_exporter` FOREIGN KEY (`exporter_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_export_org` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`organization_id`),
  CONSTRAINT `fk_export_shipment` FOREIGN KEY (`shipment_id`) REFERENCES `shipments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dossier_export_history`
--

LOCK TABLES `dossier_export_history` WRITE;
/*!40000 ALTER TABLE `dossier_export_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `dossier_export_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `failed_event_logs`
--

DROP TABLE IF EXISTS `failed_event_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `failed_event_logs` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `lot_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `lot_code` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `failure_reason` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `attempted_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_failed_event_user` (`user_id`),
  CONSTRAINT `fk_failed_event_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `failed_event_logs`
--

LOCK TABLES `failed_event_logs` WRITE;
/*!40000 ALTER TABLE `failed_event_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `failed_event_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `farm_areas`
--

DROP TABLE IF EXISTS `farm_areas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `farm_areas` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `organization_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `crop_type` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `area` decimal(10,2) NOT NULL,
  `area_unit` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `location` point NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_farm_area_organization` (`organization_id`),
  KEY `idx_farm_area_crop_type` (`crop_type`),
  SPATIAL KEY `idx_farm_area_location` (`location`),
  CONSTRAINT `fk_farm_area_organization` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`organization_id`),
  CONSTRAINT `fk_farm_area_product_category` FOREIGN KEY (`crop_type`) REFERENCES `product_categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `farm_areas`
--

LOCK TABLES `farm_areas` WRITE;
/*!40000 ALTER TABLE `farm_areas` DISABLE KEYS */;
INSERT INTO `farm_areas` VALUES ('dd2c2dd6-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd704b75-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 01',1.25,'HA',_binary '\0\0\0\0\0\0\0%��CsZ@B`\�\�\"�4@','2026-07-19 03:24:51','2026-08-27 04:42:49',1),('dd2c3308-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd704d75-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 02',2.00,'HA',_binary '\0\0\0\0\0\0\0\�\�\�SsZ@�Zd�4@','2026-07-20 03:24:51','2026-07-20 03:24:51',1),('dd2c344a-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd704e66-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 03',2.75,'HA',_binary '\0\0\0\0\0\0\0�ZdsZ@\�\�S\��4@','2026-07-21 03:24:51','2026-08-27 04:42:49',1),('dd2c3543-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd7050ef-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 04',3.50,'HA',_binary '\0\0\0\0\0\0\0�~j�tsZ@�C�l\��4@','2026-07-22 03:24:51','2026-07-22 03:24:51',1),('dd2c362e-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd7051ef-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 05',4.25,'HA',_binary '\0\0\0\0\0\0\0\�Q��sZ@\\�\�\�(�4@','2026-07-23 03:24:51','2026-08-27 04:42:49',1),('dd2c39fd-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd70445f-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 06',5.00,'HA',_binary '\0\0\0\0\0\0\0\�$��sZ@#\��~j�4@','2026-07-24 03:24:51','2026-07-24 03:24:51',1),('dd2c3b28-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd704b75-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 07',5.75,'HA',_binary '\0\0\0\0\0\0\0\�\�S\�sZ@\�&1��4@','2026-07-25 03:24:51','2026-08-27 04:42:49',1),('dd2c3c0a-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd704d75-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 08',6.50,'HA',_binary '\0\0\0\0\0\0\0�ʡE�sZ@�rh�\��4@','2026-07-26 03:24:51','2026-07-26 03:24:51',1),('dd2c3ce9-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd704e66-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 09',7.25,'HA',_binary '\0\0\0\0\0\0\0��\�\�sZ@w��\Z/�4@','2026-07-27 03:24:51','2026-08-27 04:42:49',1),('dd2c3dbb-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd7050ef-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 10',8.00,'HA',_binary '\0\0\0\0\0\0\0�p=\n\�sZ@=\nףp�4@','2026-07-28 03:24:51','2026-07-28 03:24:51',1),('dd2c3fa4-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd7051ef-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 11',8.75,'HA',_binary '\0\0\0\0\0\0\0�C�l\�sZ@V-��4@','2026-07-29 03:24:51','2026-08-27 04:42:49',1),('dd2c40bd-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd70445f-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 12',9.50,'HA',_binary '\0\0\0\0\0\0\0�\�\�\�sZ@ˡE�\��4@','2026-07-30 03:24:51','2026-07-30 03:24:51',1),('dd2c4195-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd704b75-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 13',10.25,'HA',_binary '\0\0\0\0\0\0\0y\�&1tZ@�\�|?5�4@','2026-07-31 03:24:51','2026-08-27 04:42:49',1),('dd2c43fc-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd704d75-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 14',11.00,'HA',_binary '\0\0\0\0\0\0\0j�t�tZ@X9�\�v�4@','2026-08-01 03:24:51','2026-08-01 03:24:51',1),('dd2c4516-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd704e66-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 15',11.75,'HA',_binary '\0\0\0\0\0\0\0\\�\�\�(tZ@�\�Q��4@','2026-08-02 03:24:51','2026-08-27 04:42:49',1),('dd2c45ea-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd7050ef-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 16',12.50,'HA',_binary '\0\0\0\0\0\0\0NbX9tZ@\�\�\"\���4@','2026-08-03 03:24:51','2026-08-03 03:24:51',1),('dd2c46c2-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd7051ef-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 17',13.25,'HA',_binary '\0\0\0\0\0\0\0?5^�ItZ@�Zd;�4@','2026-08-04 03:24:51','2026-08-27 04:42:49',1),('dd2c479e-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd70445f-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 18',14.00,'HA',_binary '\0\0\0\0\0\0\01�ZtZ@sh�\�|�4@','2026-08-05 03:24:51','2026-08-05 03:24:51',1),('dd2c4869-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd704b75-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 19',14.75,'HA',_binary '\0\0\0\0\0\0\0#\��~jtZ@9�\�v��4@','2026-08-06 03:24:51','2026-08-27 04:42:49',1),('dd2c4939-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd704d75-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 20',15.50,'HA',_binary '\0\0\0\0\0\0\0�G\�ztZ@\0\0\0\0\0\05@','2026-08-07 03:24:51','2026-08-07 03:24:51',1),('dd2c4a0e-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd704e66-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 21',16.25,'HA',_binary '\0\0\0\0\0\0\0��C�tZ@\�K7�A\05@','2026-08-08 03:24:51','2026-08-27 04:42:49',1),('dd2c4c2b-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd7050ef-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 22',17.00,'HA',_binary '\0\0\0\0\0\0\0�S㥛tZ@��n�\05@','2026-08-09 03:24:51','2026-08-09 03:24:51',1),('dd2c4d8c-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd7051ef-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 23',17.75,'HA',_binary '\0\0\0\0\0\0\0\�&1�tZ@T㥛\�\05@','2026-08-10 03:24:51','2026-08-27 04:42:49',1),('dd2c4e74-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd70445f-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 24',18.50,'HA',_binary '\0\0\0\0\0\0\0\��~j�tZ@/\�$5@','2026-08-11 03:24:51','2026-08-11 03:24:51',1),('dd2c4f4c-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd704b75-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 25',19.25,'HA',_binary '\0\0\0\0\0\0\0\�\�\�\�\�tZ@\�z�G5@','2026-08-12 03:24:51','2026-08-27 04:42:49',1),('dd2c5020-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd704d75-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 26',20.00,'HA',_binary '\0\0\0\0\0\0\0��\Z/\�tZ@�\�K7�5@','2026-08-13 03:24:51','2026-08-13 03:24:51',1),('dd2c50f5-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd704e66-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 27',20.75,'HA',_binary '\0\0\0\0\0\0\0�rh�\�tZ@o��\�5@','2026-08-14 03:24:51','2026-08-27 04:42:49',1),('dd2c51d3-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd7050ef-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 28',21.50,'HA',_binary '\0\0\0\0\0\0\0�E�\��tZ@5^�I5@','2026-08-15 03:24:51','2026-08-15 03:24:51',1),('dd2c52ae-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd7051ef-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 29',22.25,'HA',_binary '\0\0\0\0\0\0\0�VuZ@��\�\�M5@','2026-08-16 03:24:51','2026-08-27 04:42:49',1),('dd2c538c-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd70445f-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST V??ng tr???ng 30',23.00,'HA',_binary '\0\0\0\0\0\0\0�\�Q�uZ@\�\�(\\�5@','2026-08-17 03:24:51','2026-08-17 03:24:51',1);
/*!40000 ALTER TABLE `farm_areas` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `farm_log_attachments`
--

DROP TABLE IF EXISTS `farm_log_attachments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `farm_log_attachments` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `farm_log_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_size` bigint NOT NULL,
  `file_type` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_path` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `uploaded_by` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `uploaded_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `farm_log_id` (`farm_log_id`),
  KEY `uploaded_by` (`uploaded_by`),
  CONSTRAINT `farm_log_attachments_ibfk_1` FOREIGN KEY (`farm_log_id`) REFERENCES `farm_logs` (`id`),
  CONSTRAINT `farm_log_attachments_ibfk_2` FOREIGN KEY (`uploaded_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `farm_log_attachments`
--

LOCK TABLES `farm_log_attachments` WRITE;
/*!40000 ALTER TABLE `farm_log_attachments` DISABLE KEYS */;
/*!40000 ALTER TABLE `farm_log_attachments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `farm_logs`
--

DROP TABLE IF EXISTS `farm_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `farm_logs` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `production_lot_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `activity_type` enum('PLANTING','WATERING','FERTILIZING','PESTICIDE','WEEDING','HARVESTING','OTHER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `material` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `quantity` double DEFAULT NULL,
  `unit` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `executed_date` date NOT NULL,
  `notes` text COLLATE utf8mb4_unicode_ci,
  `created_by` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_farm_logs_production_lot` (`production_lot_id`),
  KEY `idx_farm_logs_created_by` (`created_by`),
  KEY `idx_farm_logs_executed_date` (`executed_date`),
  CONSTRAINT `fk_farm_logs_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_farm_logs_production_lot` FOREIGN KEY (`production_lot_id`) REFERENCES `production_lot` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `farm_logs`
--

LOCK TABLES `farm_logs` WRITE;
/*!40000 ALTER TABLE `farm_logs` DISABLE KEYS */;
INSERT INTO `farm_logs` VALUES ('e1334f1f-a1c6-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','WATERING','SEED_TEST V???t t?? 1',6,'KG','2026-06-09','SEED_TEST Ghi ch?? nh???t k?? s??? 1','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','2026-06-09 03:24:57'),('e1335fa6-a1c6-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','FERTILIZING','SEED_TEST V???t t?? 2',7,'KG','2026-06-10','SEED_TEST Ghi ch?? nh???t k?? s??? 2','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','2026-06-10 03:24:57'),('e133659c-a1c6-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','PESTICIDE','SEED_TEST V???t t?? 3',8,'KG','2026-06-11','SEED_TEST Ghi ch?? nh???t k?? s??? 3','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','2026-06-11 03:24:57'),('e1336c10-a1c6-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','WEEDING','SEED_TEST V???t t?? 4',9,'KG','2026-06-12','SEED_TEST Ghi ch?? nh???t k?? s??? 4','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','2026-06-12 03:24:57'),('e1336fab-a1c6-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','HARVESTING','SEED_TEST V???t t?? 5',10,'KG','2026-06-13','SEED_TEST Ghi ch?? nh???t k?? s??? 5','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','2026-06-13 03:24:57'),('e1337571-a1c6-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','OTHER','SEED_TEST V???t t?? 6',11,'KG','2026-06-14','SEED_TEST Ghi ch?? nh???t k?? s??? 6','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','2026-06-14 03:24:57'),('e1337858-a1c6-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','PLANTING','SEED_TEST V???t t?? 7',12,'KG','2026-06-15','SEED_TEST Ghi ch?? nh???t k?? s??? 7','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','2026-06-15 03:24:57'),('e1337e2d-a1c6-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','WATERING','SEED_TEST V???t t?? 8',13,'KG','2026-06-16','SEED_TEST Ghi ch?? nh???t k?? s??? 8','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','2026-06-16 03:24:57'),('e1338155-a1c6-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','FERTILIZING','SEED_TEST V???t t?? 9',14,'KG','2026-06-17','SEED_TEST Ghi ch?? nh???t k?? s??? 9','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','2026-06-17 03:24:57'),('e1338414-a1c6-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','PESTICIDE','SEED_TEST V???t t?? 10',15,'KG','2026-06-18','SEED_TEST Ghi ch?? nh???t k?? s??? 10','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','2026-06-18 03:24:57'),('e133898d-a1c6-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','WEEDING','SEED_TEST V???t t?? 11',16,'KG','2026-06-19','SEED_TEST Ghi ch?? nh???t k?? s??? 11','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','2026-06-19 03:24:57'),('e1338cbc-a1c6-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','HARVESTING','SEED_TEST V???t t?? 12',17,'KG','2026-06-20','SEED_TEST Ghi ch?? nh???t k?? s??? 12','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','2026-06-20 03:24:57'),('e1338f6e-a1c6-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','OTHER','SEED_TEST V???t t?? 13',18,'KG','2026-06-21','SEED_TEST Ghi ch?? nh???t k?? s??? 13','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','2026-06-21 03:24:57'),('e13392fd-a1c6-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','PLANTING','SEED_TEST V???t t?? 14',19,'KG','2026-06-22','SEED_TEST Ghi ch?? nh???t k?? s??? 14','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','2026-06-22 03:24:57'),('e133974a-a1c6-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','WATERING','SEED_TEST V???t t?? 15',20,'KG','2026-06-23','SEED_TEST Ghi ch?? nh???t k?? s??? 15','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','2026-06-23 03:24:57'),('e1339b45-a1c6-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','FERTILIZING','SEED_TEST V???t t?? 16',21,'KG','2026-06-24','SEED_TEST Ghi ch?? nh???t k?? s??? 16','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','2026-06-24 03:24:57'),('e1339e1c-a1c6-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','PESTICIDE','SEED_TEST V???t t?? 17',22,'KG','2026-06-25','SEED_TEST Ghi ch?? nh???t k?? s??? 17','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','2026-06-25 03:24:57'),('e133a0bf-a1c6-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','WEEDING','SEED_TEST V???t t?? 18',23,'KG','2026-06-26','SEED_TEST Ghi ch?? nh???t k?? s??? 18','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','2026-06-26 03:24:57'),('e133a35c-a1c6-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','HARVESTING','SEED_TEST V???t t?? 19',24,'KG','2026-06-27','SEED_TEST Ghi ch?? nh???t k?? s??? 19','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','2026-06-27 03:24:57'),('e133a929-a1c6-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','OTHER','SEED_TEST V???t t?? 20',25,'KG','2026-06-28','SEED_TEST Ghi ch?? nh???t k?? s??? 20','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','2026-06-28 03:24:57'),('e133ac47-a1c6-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','PLANTING','SEED_TEST V???t t?? 21',26,'KG','2026-06-29','SEED_TEST Ghi ch?? nh???t k?? s??? 21','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','2026-06-29 03:24:57'),('e133afed-a1c6-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','WATERING','SEED_TEST V???t t?? 22',27,'KG','2026-06-30','SEED_TEST Ghi ch?? nh???t k?? s??? 22','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','2026-06-30 03:24:57'),('e133b2f5-a1c6-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','FERTILIZING','SEED_TEST V???t t?? 23',28,'KG','2026-07-01','SEED_TEST Ghi ch?? nh???t k?? s??? 23','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','2026-07-01 03:24:57'),('e133b58d-a1c6-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','PESTICIDE','SEED_TEST V???t t?? 24',29,'KG','2026-07-02','SEED_TEST Ghi ch?? nh???t k?? s??? 24','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','2026-07-02 03:24:57');
/*!40000 ALTER TABLE `farm_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `script` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
INSERT INTO `flyway_schema_history` VALUES (1,'1','create auth tables','SQL','V1__create_auth_tables.sql',-59981747,'nguongocso','2026-08-27 03:08:16',1079,1),(2,'2','create product categories','SQL','V2__create_product_categories.sql',1385758622,'nguongocso','2026-08-27 03:08:16',151,1),(3,'3','create farm areas','SQL','V3__create_farm_areas.sql',-17403865,'nguongocso','2026-08-27 03:08:17',382,1),(4,'4','create production lot','SQL','V4__create_production_lot.sql',552118702,'nguongocso','2026-08-27 03:08:17',136,1),(5,'5','create farm logs','SQL','V5__create_farm_logs.sql',-1105385268,'nguongocso','2026-08-27 03:08:18',433,1),(6,'6','create shipments','SQL','V6__create_shipments.sql',-861436246,'nguongocso','2026-08-27 03:08:18',357,1),(7,'7','create standards and certifications','SQL','V7__create_standards_and_certifications.sql',721632762,'nguongocso','2026-08-27 03:08:18',288,1),(8,'8','create alerts and notifications','SQL','V8__create_alerts_and_notifications.sql',-1101599898,'nguongocso','2026-08-27 03:08:19',230,1),(9,'9','create chain events and code ranges','SQL','V9__create_chain_events_and_code_ranges.sql',-210309565,'nguongocso','2026-08-27 03:08:19',326,1),(10,'10','create logging tables','SQL','V10__create_logging_tables.sql',-1441836828,'nguongocso','2026-08-27 03:08:20',547,1),(11,'11','create export and import history','SQL','V11__create_export_and_import_history.sql',966101002,'nguongocso','2026-08-27 03:08:20',414,1),(12,'12','create backup tables','SQL','V12__create_backup_tables.sql',-832567519,'nguongocso','2026-08-27 03:08:21',357,1),(13,'13','create permission mapping tables','SQL','V13__create_permission_mapping_tables.sql',-300041227,'nguongocso','2026-08-27 03:08:21',506,1),(14,'14','seed roles','SQL','V14__seed_roles.sql',167540513,'nguongocso','2026-08-27 03:08:21',14,1),(15,'15','seed permissions','SQL','V15__seed_permissions.sql',-1229297910,'nguongocso','2026-08-27 03:08:21',15,1),(16,'16','seed role permissions','SQL','V16__seed_role_permissions.sql',1504529619,'nguongocso','2026-08-27 03:08:21',53,1),(17,'17','seed default admin','SQL','V17__seed_default_admin.sql',1633951872,'nguongocso','2026-08-27 03:08:21',28,1),(18,'18','seed backup schedule','SQL','V18__seed_backup_schedule.sql',290582182,'nguongocso','2026-08-27 03:08:22',18,1),(19,'19','add storage thresholds to product category','SQL','V19__add_storage_thresholds_to_product_category.sql',-932756834,'nguongocso','2026-08-27 03:08:22',176,1),(20,'20','add event hash fields','SQL','V20__add_event_hash_fields.sql',-1710589896,'nguongocso','2026-08-27 03:08:22',228,1),(21,'21','add suspect fields to trace codes','SQL','V21__add_suspect_fields_to_trace_codes.sql',-683689371,'nguongocso','2026-08-27 03:08:23',459,1),(22,'22','create recall requests','SQL','V22__create_recall_requests.sql',1657217560,'nguongocso','2026-08-27 03:08:23',255,1),(23,'23','create inspection requests','SQL','V23__create_inspection_requests.sql',711860086,'nguongocso','2026-08-27 03:08:23',103,1),(24,'24','create inspection criteria','SQL','V24__create_inspection_criteria.sql',417549300,'nguongocso','2026-08-27 03:08:23',92,1),(25,'25','create inspection criterion definitions','SQL','V25__create_inspection_criterion_definitions.sql',338558706,'nguongocso','2026-08-27 03:08:23',69,1),(26,'26','create inspection criterion results','SQL','V26__create_inspection_criterion_results.sql',788392661,'nguongocso','2026-08-27 03:08:24',171,1),(27,'27','create partner api keys table','SQL','V27__create_partner_api_keys_table.sql',-291047529,'nguongocso','2026-08-27 03:08:24',138,1),(28,'28','create help content table','SQL','V28__create_help_content_table.sql',807629749,'nguongocso','2026-08-27 03:08:24',131,1),(29,'29','seed help content','SQL','V29__seed_help_content.sql',439170349,'nguongocso','2026-08-27 03:08:24',28,1),(30,'30','add login anomaly tables','SQL','V30__add_login_anomaly_tables.sql',1812847700,'nguongocso','2026-08-27 03:08:24',284,1),(31,'31','create suspicious cases table','SQL','V31__create_suspicious_cases_table.sql',-202467536,'nguongocso','2026-08-27 03:08:25',118,1),(32,'32','add code range to shipments','SQL','V32__add_code_range_to_shipments.sql',617681832,'nguongocso','2026-08-27 03:08:25',205,1),(33,'34','seed help content inspection request','SQL','V34__seed_help_content_inspection_request.sql',1795384438,'nguongocso','2026-08-27 03:08:25',12,1),(34,'35','seed help content inspection result and update request','SQL','V35__seed_help_content_inspection_result_and_update_request.sql',-1453789042,'nguongocso','2026-08-27 03:08:25',22,1),(35,'36','create label export history','SQL','V36__create_label_export_history.sql',1720689062,'nguongocso','2026-08-27 03:08:25',94,1),(36,'38','create input materials','SQL','V38__create_input_materials.sql',1158293652,'nguongocso','2026-08-27 03:08:26',281,1),(37,'40','seed input materials','SQL','V40__seed_input_materials.sql',-661771684,'nguongocso','2026-08-27 03:08:26',20,1),(38,'39','seed input material permissions','SQL','V39__seed_input_material_permissions.sql',-1508228872,'nguongocso','2026-08-27 03:08:26',26,1),(39,'37','seed help content organization create','SQL','V37__seed_help_content_organization_create.sql',-1815165495,'nguongocso','2026-08-27 03:08:26',16,1),(40,'33','seed partner api keys','SQL','V33__seed_partner_api_keys.sql',142233997,'nguongocso','2026-08-27 14:44:34',58,1),(41,'41','add is active to farm areas','SQL','V41__add_is_active_to_farm_areas.sql',-1387029631,'nguongocso','2026-08-27 14:46:53',196,1),(42,'42','seed administrative units','SQL','V42__seed_administrative_units.sql',-366581066,'nguongocso','2026-08-28 03:02:16',57,0);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `help_content`
--

DROP TABLE IF EXISTS `help_content`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `help_content` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `screen_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `steps` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `example_data` text COLLATE utf8mb4_unicode_ci,
  `sort_order` int NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_help_content_screen_role` (`screen_key`,`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `help_content`
--

LOCK TABLES `help_content` WRITE;
/*!40000 ALTER TABLE `help_content` DISABLE KEYS */;
INSERT INTO `help_content` VALUES ('00000000-0000-0000-0000-000000000001','dashboard','GENERAL','Hướng dẫn sử dụng bảng điều khiển','[\"Xem các chỉ số tổng quan về lô sản xuất, sự kiện chuỗi cung ứng và cảnh báo\", \"Chọn bộ lọc thời gian (ngày/tuần/tháng) để xem số liệu phù hợp\", \"Nhấn vào thẻ chỉ số để truy cập nhanh trang chi tiết\", \"Dùng các nút hành động nhanh để tạo lô, ghi sự kiện hoặc tra cứu mã\"]','Ví dụ: Xem \"Tổng số lô đang sản xuất\" để theo dõi tiến độ trong tuần.',0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000002','production-lot-list','GENERAL','Hướng dẫn danh sách lô sản xuất','[\"Dùng thanh tìm kiếm và bộ lọc (trạng thái, khu vực, vụ mùa) để thu hẹp danh sách\", \"Nhấn vào tên lô để mở trang chi tiết\", \"Chọn hành động nhanh: chỉnh sửa, gửi duyệt hoặc xem nhật ký\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000003','production-lot-create','GENERAL','Hướng dẫn tạo lô sản xuất','[\"Chọn khu vực canh tác và danh mục sản phẩm phù hợp\", \"Nhập tên lô, vụ mùa, diện tích và ngày bắt đầu\", \"Điền thông tin giống cây trồng nếu có\", \"Bấm Lưu để tạo lô\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000004','production-lot-edit','GENERAL','Hướng dẫn chỉnh sửa lô sản xuất','[\"Cập nhật thông tin lô (tên, diện tích, vụ mùa)\", \"Đính kèm ghi chú thay đổi để lưu vết\", \"Bấm Lưu thay đổi\", \"Với lô mới, dùng nút Duyệt để kích hoạt lô\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000005','production-lot-detail','GENERAL','Hướng dẫn xem chi tiết lô sản xuất','[\"Xem tổng quan lô: thông tin cơ bản, trạng thái, diện tích\", \"Cuộn xem các lô hàng (shipments) trực thuộc\", \"Xem nhật ký canh tác và các sự kiện của lô\", \"Theo dõi mã truy xuất đã cấp cho lô\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000006','production-lot-import','GENERAL','Hướng dẫn nhập khẩu lô sản xuất','[\"Tải file mẫu (template) nhập khẩu lô\", \"Điền dữ liệu các lô theo đúng định dạng cột\", \"Tải file lên và xem kết quả kiểm tra\", \"Sửa các dòng lỗi theo thông báo rồi tải lại\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000007','farm-log-create','GENERAL','Hướng dẫn ghi nhật ký canh tác','[\"Chọn lô sản xuất phù hợp từ danh sách\", \"Nhập hoạt động canh tác (bón phân, tưới, phòng trừ...)\", \"Chụp hoặc tải lên ảnh minh chứng\", \"Bấm Lưu để gửi nhật ký cho quản lý duyệt\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000008','farm-log-history','GENERAL','Hướng dẫn xem lịch sử nhật ký canh tác','[\"Chọn lô sản xuất để xem toàn bộ nhật ký canh tác\", \"Xem chi tiết từng nhật ký kèm ảnh minh chứng\", \"Duyệt nhật ký hợp lệ hoặc từ chối kèm lý do\", \"Theo dõi trạng thái duyệt của từng nhật ký\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000009','preprocessing-event-create','GENERAL','Hướng dẫn ghi sự kiện sơ chế','[\"Chọn lô sản xuất đã thu hoạch\", \"Ghi loại sơ chế (rửa, phân loại, bảo quản...) và khối lượng\", \"Nhập thời gian, địa điểm và người thực hiện\", \"Bấm Lưu để ghi nhận sự kiện vào chuỗi\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000010','preprocessing-event-correct','GENERAL','Hướng dẫn chỉnh sửa sự kiện sơ chế','[\"Mở sự kiện sơ chế cần chỉnh sửa\", \"Chỉnh lại thông tin sai\", \"Nhập lý do chỉnh sửa (bắt buộc)\", \"Bấm Lưu — hệ thống ghi lại bản sửa và lý do\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000011','packaging-event-create','GENERAL','Hướng dẫn ghi sự kiện đóng gói','[\"Chọn lô sản xuất đã sơ chế\", \"Nhập số bao bì, khối lượng đóng gói\", \"Kiểm tra dải mã truy xuất hệ thống tự sinh\", \"Bấm Lưu để gán mã cho từng bao bì\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000012','packaging-event-correct','GENERAL','Hướng dẫn chỉnh sửa sự kiện đóng gói','[\"Mở sự kiện đóng gói cần chỉnh sửa\", \"Sửa số liệu bao bì/khối lượng\", \"Nhập lý do chỉnh sửa (bắt buộc)\", \"Bấm Lưu — lịch sử mã truy xuất được giữ lại\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000013','transport-event-record','GENERAL','Hướng dẫn ghi sự kiện vận chuyển','[\"Quét mã truy xuất trên bao bì/lô hàng\", \"Chọn phương tiện và người vận chuyển\", \"Nhập điểm đi, điểm đến, thời gian\", \"Bấm Lưu để ghi nhận chặng vận chuyển\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000014','scan-quick-event','GENERAL','Hướng dẫn quét sự kiện nhanh','[\"Quét mã truy xuất cần ghi sự kiện\", \"Chọn loại sự kiện nhanh (nhập kho, xuất kho, hư hỏng...)\", \"Điền thông tin bổ sung nếu cần\", \"Bấm Lưu — sự kiện được thêm ngay vào chuỗi\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000015','procurement-event','GENERAL','Hướng dẫn ghi sự kiện thu mua','[\"Xác nhận lô hàng cần thu mua từ danh sách\", \"Nhập thông tin thu mua: khối lượng, đơn giá\", \"Xác nhận phương thức thanh toán\", \"Bấm Xác nhận để ghi nhận sự kiện thu mua\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000016','warehouse-receipt','GENERAL','Hướng dẫn ghi phiếu nhập kho','[\"Chọn lô hàng đã vận chuyển đến\", \"Quét mã vận đơn hoặc mã truy xuất để đối soát\", \"Nhập thông tin nhập kho: số lượng, vị trí, người nhận\", \"Bấm Lưu để hoàn tất phiếu nhập kho\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000017','storage-condition','GENERAL','Hướng dẫn ghi điều kiện bảo quản','[\"Chọn kho/lô hàng đang lưu trữ\", \"Nhập nhiệt độ, độ ẩm và thời gian đo\", \"Ghi chú tình trạng hàng hóa\", \"Bấm Lưu để cập nhật điều kiện bảo quản\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000018','event-chain-verification','GENERAL','Hướng dẫn xác minh chuỗi sự kiện','[\"Chọn lô hoặc mã truy xuất cần kiểm tra\", \"Xem sơ đồ chuỗi sự kiện theo thời gian\", \"Kiểm tra dấu vân tay (hash) từng sự kiện\", \"Phát hiện điểm bất thường và gửi cảnh báo nếu cần\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000019','recall-request-list','GENERAL','Hướng dẫn danh sách yêu cầu thu hồi','[\"Lọc danh sách yêu cầu theo trạng thái (PENDING/APPROVED/REJECTED)\", \"Mở yêu cầu để xem lý do, bằng chứng và lô liên quan\", \"Duyệt để kích hoạt thu hồi hoặc từ chối kèm lý do\", \"Theo dõi các lô đã thu hồi từ màn hình này\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000020','recall-request-create','GENERAL','Hướng dẫn tạo yêu cầu thu hồi','[\"Chọn lô sản xuất cần thu hồi\", \"Nhập lý do thu hồi rõ ràng\", \"Đính kèm bằng chứng (ảnh/tài liệu)\", \"Bấm Gửi — yêu cầu chuyển đến quản lý duyệt\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000021','recall-request-detail','GENERAL','Hướng dẫn xử lý yêu cầu thu hồi','[\"Xem thông tin lô, lý do và bằng chứng yêu cầu\", \"Xem ảnh hưởng: các lô hàng và mã truy xuất liên quan\", \"Nhấn Duyệt để thu hồi toàn bộ chuỗi hoặc Từ chối kèm lý do\", \"Theo dõi thông báo gửi đến doanh nghiệp thu mua\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000022','report-lookup-statistics','GENERAL','Hướng dẫn xem thống kê lượt tra cứu','[\"Chọn khoảng thời gian cần thống kê\", \"Xem số lượt tra cứu, kênh tra cứu, mã được tra cứu nhiều\", \"Lọc theo sản phẩm hoặc khu vực nếu cần\", \"Xuất báo cáo ra file để chia sẻ\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000023','report-crop-area-analysis','GENERAL','Hướng dẫn phân tích diện tích canh tác','[\"Chọn vụ mùa và khu vực cần phân tích\", \"Xem biểu đồ diện tích gieo trồng theo khu vực\", \"So sánh với các vụ trước\", \"Xuất số liệu phân tích nếu cần\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000024','report-season-yield','GENERAL','Hướng dẫn so sánh năng suất theo vụ','[\"Chọn các vụ mùa cần so sánh\", \"Xem biểu đồ năng suất từng vụ\", \"Xem chi tiết theo khu vực/sản phẩm\", \"Xuất báo cáo so sánh\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000025','report-industry','GENERAL','Hướng dẫn xem báo cáo ngành','[\"Chọn kỳ báo cáo (tháng/quý/năm)\", \"Xem thống kê toàn ngành về sản lượng, diện tích\", \"Lọc theo sản phẩm/tỉnh thành\", \"Xuất báo cáo ngành ra file\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000026','report-activity-log','GENERAL','Hướng dẫn xem nhật ký hoạt động','[\"Chọn khoảng thời gian cần xem\", \"Lọc theo người dùng hoặc loại thao tác\", \"Xem chi tiết thay đổi từng bản ghi\", \"Xuất nhật ký hoạt động để lưu trữ\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000027','report-failed-events','GENERAL','Hướng dẫn xử lý sự kiện thất bại','[\"Xem danh sách sự kiện thất bại (lỗi hash, thiếu dữ liệu)\", \"Mở chi tiết để xem nguyên nhân lỗi\", \"Sửa dữ liệu hoặc nhấn Thử lại\", \"Theo dõi trạng thái đồng bộ lại\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000028','alert-scan-anomaly','GENERAL','Hướng dẫn xử lý cảnh báo bất thường khi quét','[\"Xem danh sách cảnh báo bất thường khi quét mã\", \"Lọc theo mức độ và thời gian\", \"Mở chi tiết để điều tra nguyên nhân\", \"Xử lý cảnh báo: đánh dấu hoặc cập nhật trạng thái\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000029','notifications','GENERAL','Hướng dẫn sử dụng thông báo','[\"Xem danh sách thông báo mới\", \"Bấm vào thông báo để mở trang liên quan\", \"Đánh dấu đã đọc từng thông báo\", \"Lọc theo trạng thái đã đọc/chưa đọc\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000030','admin-code-range-list','GENERAL','Hướng dẫn quản lý dải mã','[\"Xem danh sách dải mã đã cấp cho tổ chức\", \"Lọc theo trạng thái dải mã\", \"Tạo dải mã mới hoặc xem chi tiết dải mã\", \"Theo dõi số mã đã sử dụng/còn lại\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000031','admin-code-range-create','GENERAL','Hướng dẫn tạo dải mã','[\"Chọn tổ chức nhận dải mã\", \"Nhập số lượng mã và tiền tố (prefix)\", \"Chọn ngày hết hạn nếu có\", \"Bấm Tạo — dải mã được sinh tự động\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000032','admin-product-categories','GENERAL','Hướng dẫn quản lý danh mục sản phẩm','[\"Xem danh sách danh mục sản phẩm\", \"Thêm/sửa tên danh mục và mô tả\", \"Đặt ngưỡng cảnh báo (tồn kho/khu vực) nếu có\", \"Lưu thay đổi để áp dụng ngay\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000033','admin-standards','GENERAL','Hướng dẫn quản lý tiêu chuẩn','[\"Xem danh sách tiêu chuẩn áp dụng\", \"Thêm tiêu chuẩn mới với tên, phiên bản, mô tả\", \"Gán tiêu chuẩn cho danh mục sản phẩm\", \"Xuất bản để áp dụng cho hệ thống\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000034','admin-suspect-trace-codes','GENERAL','Hướng dẫn xử lý mã truy xuất nghi ngờ','[\"Xem danh sách mã truy xuất nghi ngờ\", \"Mở chi tiết mã để xem lịch sử sự kiện\", \"Phân tích dấu hiệu bất thường\", \"Cập nhật trạng thái xử lý cho mã\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000035','admin-backup-restore','GENERAL','Hướng dẫn sao lưu & khôi phục dữ liệu','[\"Xem lịch sao lưu tự động hàng ngày\", \"Tạo bản sao lưu thủ công trước thay đổi lớn\", \"Khôi phục từ bản sao lưu khi cần\", \"Kiểm tra trạng thái các bản sao lưu\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000036','certification-list','GENERAL','Hướng dẫn quản lý chứng nhận','[\"Xem danh sách chứng nhận của tổ chức\", \"Lọc theo trạng thái/sản phẩm\", \"Tạo chứng nhận mới\", \"Mở chi tiết để xem hồ sơ đính kèm\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000037','certification-create','GENERAL','Hướng dẫn tạo chứng nhận','[\"Chọn sản phẩm/lô cần chứng nhận\", \"Nhập loại chứng nhận, đơn vị cấp, ngày hiệu lực\", \"Đính kèm hồ sơ/minh chứng\", \"Bấm Lưu — chứng nhận được gửi duyệt\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000038','admin-api-keys','GENERAL','Hướng dẫn quản lý khóa API đối tác','[\"Xem danh sách khóa API đối tác\", \"Tạo khóa mới cho đối tác tích hợp\", \"Đặt giới hạn tần suất và ngày hết hạn\", \"Thu hồi khóa nếu đối tác không còn hợp tác\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000039','export-open-data','GENERAL','Hướng dẫn xuất dữ liệu mở','[\"Chọn bộ dữ liệu mở cần xuất (lô, sự kiện, truy xuất)\", \"Chọn định dạng và khoảng thời gian\", \"Xem trước dữ liệu trước khi xuất\", \"Tải file dữ liệu mở đã xuất\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000040','member-permissions','GENERAL','Hướng dẫn quản lý thành viên & quyền','[\"Xem danh sách thành viên của tổ chức\", \"Thêm thành viên mới bằng email/số điện thoại\", \"Gán vai trò và quyền cho thành viên\", \"Khóa/vô hiệu tài khoản khi cần\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000041','permission-config','GENERAL','Hướng dẫn cấu hình phân quyền vai trò','[\"Xem danh sách vai trò trong hệ thống\", \"Chọn vai trò để xem các quyền được cấp\", \"Bật/tắt quyền theo nhu cầu\", \"Lưu thay đổi — áp dụng ngay cho thành viên\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000042','product-feedback','GENERAL','Hướng dẫn quản lý phản hồi sản phẩm','[\"Xem danh sách phản hồi sản phẩm từ khách hàng\", \"Lọc theo sản phẩm/đánh giá\", \"Mở chi tiết phản hồi kèm mã truy xuất\", \"Xử lý hoặc gắn cờ phản hồi cần lưu ý\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000043','offline-events','GENERAL','Hướng dẫn đồng bộ sự kiện offline','[\"Xem danh sách sự kiện ghi offline chưa đồng bộ\", \"Chọn sự kiện cần đồng bộ\", \"Nhấn Đồng bộ — hệ thống gửi lên máy chủ\", \"Kiểm tra trạng thái thành công/thất bại\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000044','farm-area-list','GENERAL','Hướng dẫn quản lý khu vực canh tác','[\"Xem danh sách khu vực canh tác\", \"Lọc theo trạng thái hoạt động\", \"Thêm khu vực mới hoặc sửa khu vực cũ\", \"Theo dõi diện tích và lô đang hoạt động\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000045','farm-area-create','GENERAL','Hướng dẫn tạo khu vực canh tác','[\"Nhập tên và mã khu vực canh tác\", \"Nhập diện tích, vị trí (tọa độ nếu có)\", \"Chọn trạng thái hoạt động ban đầu\", \"Bấm Lưu để thêm khu vực vào hệ thống\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000046','inspection-request-create','GENERAL','Hướng dẫn tạo yêu cầu kiểm nghiệm','[\"Kiểm tra thông tin lô sản xuất ở thẻ Thông tin lô sản xuất bên phải\", \"Chọn các chỉ tiêu phân tích cần kiểm nghiệm (hỗ trợ phân trang, tìm kiếm và lọc trạng thái Đã chọn / Chưa chọn)\", \"Nhập tên Đơn vị phòng Lab tiếp nhận và Ngày gửi mẫu (không được lớn hơn ngày hiện tại)\", \"Nhập Ghi chú bảo quản & Yêu cầu phân tích bổ sung nếu có\", \"Bấm Lưu bản nháp để lưu tạm thời, hoặc bấm Tạo yêu cầu kiểm nghiệm để gửi yêu cầu chính thức\"]',NULL,0,'2026-08-25 00:00:00','2026-08-27 03:08:25'),('00000000-0000-0000-0000-000000000047','inspection-result-record','GENERAL','Hướng dẫn ghi nhận kết quả kiểm nghiệm','[\"Kiểm tra thông tin yêu cầu kiểm nghiệm và danh sách chỉ tiêu phân tích cần nhập\", \"Với từng chỉ tiêu, chọn kết luận: Đạt chuẩn, Không đạt hoặc Không kiểm tra\", \"Nhập Giá trị đo thực tế, Phương pháp thử nghiệm và Ghi chú chi tiết cho từng chỉ tiêu\", \"Nhập Ngày nhận kết quả, Người thực hiện phân tích và Kết luận tổng quan của phòng Lab\", \"Đính kèm tệp tài liệu kết quả kiểm nghiệm (PDF hoặc ảnh scan, tối đa 5MB) nếu có\", \"Kiểm tra tiến độ nhập ở thanh tác vụ nổi bên dưới và bấm Lưu kết quả kiểm nghiệm để hoàn tất\"]',NULL,0,'2026-08-27 03:08:25','2026-08-27 03:08:25'),('00000000-0000-0000-0000-000000000048','organization-create','GENERAL','Hướng dẫn tạo tổ chức mới','[\"1. Mã tổ chức: Chỉ dùng A-Z, 0-9, gạch ngang và gạch dưới. Lưu ý: Mã tổ chức và tên đăng nhập không thể thay đổi sau khi tạo.\", \"2. Thông tin tổ chức: Nhập tên tổ chức chính xác và chọn loại hình tổ chức (Hợp tác xã, Doanh nghiệp...).\", \"3. Quản trị viên đầu tiên: Nhập họ tên, tên đăng nhập và email quản lý. Tài khoản quản trị này sẽ được khởi tạo đồng thời cùng tổ chức.\", \"4. Mật khẩu: Thiết lập mật khẩu tối thiểu 8 ký tự, bao gồm chữ hoa, chữ thường, chữ số và ký tự đặc biệt (@$!%*?&).\", \"5. Sau khi tạo: Bạn có thể cập nhật thêm thông tin địa chỉ, số điện thoại, email liên hệ chi tiết trong mục Hồ sơ tổ chức.\"]',NULL,0,'2026-08-27 03:08:26','2026-08-27 03:08:26'),('00000000-0000-0000-0000-000000000101','production-lot-list','VT-02','Hướng dẫn duyệt lô sản xuất','[\"Lọc danh sách lô theo trạng thái chờ duyệt\", \"Nhấn vào lô để xem chi tiết trước khi quyết định\", \"Duyệt hoặc từ chối lô từ màn hình chỉnh sửa\", \"Theo dõi trạng thái lô sau khi duyệt\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000102','production-lot-list','VT-03','Hướng dẫn theo dõi lô sản xuất cho người ghi sự kiện','[\"Lọc danh sách lô theo trạng thái\", \"Mở chi tiết lô để ghi nhật ký canh tác hoặc tạo sự kiện\", \"Gửi lô cho quản lý duyệt khi hoàn tất sản xuất\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000103','production-lot-detail','VT-03','Hướng dẫn chi tiết lô cho người ghi sự kiện','[\"Xem thông tin lô và trạng thái hiện tại\", \"Bấm Ghi nhật ký để thêm hoạt động canh tác\", \"Ghi sự kiện sơ chế/đóng gói nếu lô đã thu hoạch\", \"Tạo yêu cầu thu hồi nếu phát hiện bất thường\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000104','event-chain-verification','VT-04','Hướng dẫn xác minh chuỗi sự kiện cho doanh nghiệp thu mua','[\"Tìm lô hàng của doanh nghiệp mình\", \"Xem toàn bộ chuỗi sự kiện từ sản xuất đến vận chuyển\", \"Kiểm tra dấu vân tay và tính toàn vẹn dữ liệu\", \"Xuất báo cáo xác minh để lưu hồ sơ\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00'),('00000000-0000-0000-0000-000000000105','admin-api-keys','VT-02','Hướng dẫn quản lý khóa API cho hợp tác xã','[\"Xem danh sách khóa API của hợp tác xã\", \"Tạo khóa mới cho đối tác thu mua\", \"Đặt giới hạn tần suất gọi API\", \"Thu hồi khóa khi ngừng hợp tác\"]',NULL,0,'2026-08-17 00:00:00','2026-08-17 00:00:00');
/*!40000 ALTER TABLE `help_content` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `input_material_crop_types`
--

DROP TABLE IF EXISTS `input_material_crop_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `input_material_crop_types` (
  `material_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `crop_category_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`material_id`,`crop_category_id`),
  KEY `fk_imct_crop` (`crop_category_id`),
  CONSTRAINT `fk_imct_crop` FOREIGN KEY (`crop_category_id`) REFERENCES `product_categories` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_imct_material` FOREIGN KEY (`material_id`) REFERENCES `input_materials` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `input_material_crop_types`
--

LOCK TABLES `input_material_crop_types` WRITE;
/*!40000 ALTER TABLE `input_material_crop_types` DISABLE KEYS */;
/*!40000 ALTER TABLE `input_material_crop_types` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `input_materials`
--

DROP TABLE IF EXISTS `input_materials`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `input_materials` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `material_group` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `active_ingredient` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `quarantine_days` int NOT NULL DEFAULT '0',
  `apply_to_all_crops` tinyint(1) NOT NULL DEFAULT '1',
  `reference_source` text COLLATE utf8mb4_unicode_ci,
  `image_urls` longtext COLLATE utf8mb4_unicode_ci,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_by` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_material_name_ingredient` (`name`,`active_ingredient`),
  KEY `idx_input_materials_group` (`material_group`),
  KEY `idx_input_materials_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `input_materials`
--

LOCK TABLES `input_materials` WRITE;
/*!40000 ALTER TABLE `input_materials` DISABLE KEYS */;
INSERT INTO `input_materials` VALUES ('018f9d00-0001-7000-8000-000000000001','Brightin 4.0EC','PESTICIDE','Abamectin','ml',7,1,'Thông tư 10/2020/TT-BNNPTNT',NULL,1,NULL,'2026-08-27 03:08:26',NULL),('018f9d00-0002-7000-8000-000000000002','Dithane M-45 80WP','PESTICIDE','Mancozeb','g',14,1,'QCVN 01-132:2013/BNNPTNT, Thông tư 10/2020/TT-BNNPTNT',NULL,1,NULL,'2026-08-27 03:08:26',NULL),('018f9d00-0003-7000-8000-000000000003','Amistar Top 325SC','PESTICIDE','Azoxystrobin + Difenoconazole','ml',7,1,'Danh mục Thuốc BVTV BNNPTNT',NULL,1,NULL,'2026-08-27 03:08:26',NULL),('018f9d00-0004-7000-8000-000000000004','Prevathon 35WG','PESTICIDE','Chlorantraniliprole','g',3,1,'Thông tư 10/2020/TT-BNNPTNT',NULL,1,NULL,'2026-08-27 03:08:26',NULL),('018f9d00-0005-7000-8000-000000000005','Confidor 100SL','PESTICIDE','Imidacloprid','ml',7,1,'Danh mục Thuốc BVTV Bộ NN&PTNT',NULL,1,NULL,'2026-08-27 03:08:26',NULL),('018f9d00-0006-7000-8000-000000000006','Anvil 5SC','PESTICIDE','Hexaconazole','ml',14,1,'Thông tư 10/2020/TT-BNNPTNT',NULL,1,NULL,'2026-08-27 03:08:26',NULL),('018f9d00-0007-7000-8000-000000000007','Proclaim 1.9EC','PESTICIDE','Emamectin benzoate','ml',7,1,'Danh mục Thuốc BVTV Bộ NN&PTNT',NULL,1,NULL,'2026-08-27 03:08:26',NULL),('018f9d00-0008-7000-8000-000000000008','Basta 15SL','PESTICIDE','Glufosinate ammonium','lít',14,1,'Thông tư 10/2020/TT-BNNPTNT',NULL,1,NULL,'2026-08-27 03:08:26',NULL),('018f9d00-0009-7000-8000-000000000009','Coc 85WP','PESTICIDE','Copper Oxychloride','g',7,1,'Tiêu chuẩn VietGAP TCVN 11892-1:2017',NULL,1,NULL,'2026-08-27 03:08:26',NULL),('018f9d00-0010-7000-8000-000000000010','Phân bón NPK 16-16-8+TE','FERTILIZER','N, P2O5, K2O, TE','kg',0,1,'Nghị định 84/2019/NĐ-CP',NULL,1,NULL,'2026-08-27 03:08:26',NULL),('018f9d00-0011-7000-8000-000000000011','Phân Đạm Ure Hà Bắc','FERTILIZER','Nitrogen (N 46.3%)','kg',0,1,'TCVN 2637:2015, Nghị định 84/2019/NĐ-CP',NULL,1,NULL,'2026-08-27 03:08:26',NULL),('018f9d00-0012-7000-8000-000000000012','Phân Hữu cơ Vi sinh Sông Gianh','FERTILIZER','Hữu cơ 15%, Azotobacter, Bacillus spp.','kg',0,1,'TCVN 9268:2012 Phân bón hữu cơ vi sinh',NULL,1,NULL,'2026-08-27 03:08:26',NULL),('018f9d00-0013-7000-8000-000000000013','Phân bón lá Humic Acid Premium','FERTILIZER','Potassium Humate 85%, Fulvic Acid 10%','kg',0,1,'Nghị định 84/2019/NĐ-CP',NULL,0,NULL,'2026-08-27 03:08:26','2026-08-28 01:25:04'),('018f9d00-0014-7000-8000-000000000014','Phân Kali Clorua (KCl 60%)','FERTILIZER','Potassium Oxide (K2O 60%)','kg',0,1,'Nghị định 84/2019/NĐ-CP',NULL,1,NULL,'2026-08-27 03:08:26',NULL),('018f9d00-0015-7000-8000-000000000015','Trichoderma spp. Nông Nghiệp','BIOLOGICAL','Trichoderma harzianum / viride','kg',0,1,'TCVN 10785:2015 Chế phẩm vi sinh',NULL,1,NULL,'2026-08-27 03:08:26',NULL),('018f9d00-0016-7000-8000-000000000016','Vi sinh Bacillus thuringiensis (Bt)','BIOLOGICAL','Bacillus thuringiensis var. kurstaki','g',3,1,'Danh mục Chế phẩm Biological BNNPTNT',NULL,0,NULL,'2026-08-27 03:08:26','2026-08-28 01:25:09'),('018f9d00-0017-7000-8000-000000000017','Chế phẩm EM1 Nông nghiệp','BIOLOGICAL','Vi khuẩn Lactic, Vi khuẩn quang hợp, Nấm men','lít',0,1,'Quy trình canh tác sinh thái VietGAP',NULL,1,NULL,'2026-08-27 03:08:26',NULL),('018f9d00-0018-7000-8000-000000000018','Vôi bột Nông nghiệp','OTHER','Calcium Oxide (CaO 70%)','kg',0,1,'TCVN 11793:2017 Vôi bón nông nghiệp',NULL,1,NULL,'2026-08-27 03:08:26',NULL),('018f9d00-0019-7000-8000-000000000019','Màng phủ Nông nghiệp 2 mặt','OTHER','Nhựa PE chống UV','cuộn',0,1,'Tiêu chuẩn phụ trợ canh tác VietGAP',NULL,1,NULL,'2026-08-27 03:08:26',NULL),('018f9d00-0020-7000-8000-000000000020','Chất điều hòa sinh trưởng Atonik 1.8SL','OTHER','Sodium Nitrophenolate','ml',7,1,'Danh mục Điều hòa sinh trưởng BNNPTNT',NULL,1,NULL,'2026-08-27 03:08:26',NULL);
/*!40000 ALTER TABLE `input_materials` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inspection_criteria`
--

DROP TABLE IF EXISTS `inspection_criteria`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inspection_criteria` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `inspection_request_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `standard_id` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `criterion_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `criterion_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inspection_request_criterion` (`inspection_request_id`,`criterion_code`),
  KEY `fk_inspection_criterion_standard` (`standard_id`),
  CONSTRAINT `fk_inspection_criterion_request` FOREIGN KEY (`inspection_request_id`) REFERENCES `inspection_requests` (`id`),
  CONSTRAINT `fk_inspection_criterion_standard` FOREIGN KEY (`standard_id`) REFERENCES `standards` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inspection_criteria`
--

LOCK TABLES `inspection_criteria` WRITE;
/*!40000 ALTER TABLE `inspection_criteria` DISABLE KEYS */;
/*!40000 ALTER TABLE `inspection_criteria` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inspection_criterion_definitions`
--

DROP TABLE IF EXISTS `inspection_criterion_definitions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inspection_criterion_definitions` (
  `id` int NOT NULL AUTO_INCREMENT,
  `code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `note` text COLLATE utf8mb4_unicode_ci,
  `standard_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_inspection_criterion_definition_standard_code` (`standard_id`,`code`),
  CONSTRAINT `fk_inspection_criterion_definition_standard` FOREIGN KEY (`standard_id`) REFERENCES `standards` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inspection_criterion_definitions`
--

LOCK TABLES `inspection_criterion_definitions` WRITE;
/*!40000 ALTER TABLE `inspection_criterion_definitions` DISABLE KEYS */;
INSERT INTO `inspection_criterion_definitions` VALUES (1,'C01','SEED_TEST Ch??? ti??u ki???m nghi???m 1','Ghi ch?? ti??u ch?? 1','e13530dc-a1c6-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),(2,'C02','SEED_TEST Ch??? ti??u ki???m nghi???m 2','Ghi ch?? ti??u ch?? 2','e13530dc-a1c6-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),(3,'C03','SEED_TEST Ch??? ti??u ki???m nghi???m 3','Ghi ch?? ti??u ch?? 3','e13530dc-a1c6-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),(4,'C04','SEED_TEST Ch??? ti??u ki???m nghi???m 4','Ghi ch?? ti??u ch?? 4','e13530dc-a1c6-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),(5,'C05','SEED_TEST Ch??? ti??u ki???m nghi???m 5','Ghi ch?? ti??u ch?? 5','e13530dc-a1c6-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),(6,'C06','SEED_TEST Ch??? ti??u ki???m nghi???m 6','Ghi ch?? ti??u ch?? 6','e13530dc-a1c6-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),(7,'C07','SEED_TEST Ch??? ti??u ki???m nghi???m 7','Ghi ch?? ti??u ch?? 7','e13530dc-a1c6-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),(8,'C08','SEED_TEST Ch??? ti??u ki???m nghi???m 8','Ghi ch?? ti??u ch?? 8','e13530dc-a1c6-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),(9,'C09','SEED_TEST Ch??? ti??u ki???m nghi???m 9','Ghi ch?? ti??u ch?? 9','e13530dc-a1c6-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),(10,'C10','SEED_TEST Ch??? ti??u ki???m nghi???m 10','Ghi ch?? ti??u ch?? 10','e13530dc-a1c6-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),(11,'C11','SEED_TEST Ch??? ti??u ki???m nghi???m 11','Ghi ch?? ti??u ch?? 11','e13530dc-a1c6-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),(12,'C12','SEED_TEST Ch??? ti??u ki???m nghi???m 12','Ghi ch?? ti??u ch?? 12','e13530dc-a1c6-11f1-9ae2-029fd41577b3','2026-08-27 03:24:57','2026-08-27 03:24:57'),(16,'C13','SEED_TEST Chi tieu kiem nghiem 13','Ghi chu tieu chi 13','e13534f8-a1c6-11f1-9ae2-029fd41577b3','2026-08-27 04:42:49','2026-08-27 04:42:49'),(17,'C14','SEED_TEST Chi tieu kiem nghiem 14','Ghi chu tieu chi 14','e13534f8-a1c6-11f1-9ae2-029fd41577b3','2026-08-27 04:42:49','2026-08-27 04:42:49'),(18,'C15','SEED_TEST Chi tieu kiem nghiem 15','Ghi chu tieu chi 15','e13534f8-a1c6-11f1-9ae2-029fd41577b3','2026-08-27 04:42:49','2026-08-27 04:42:49');
/*!40000 ALTER TABLE `inspection_criterion_definitions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inspection_criterion_results`
--

DROP TABLE IF EXISTS `inspection_criterion_results`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inspection_criterion_results` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `inspection_criterion_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `result_date` date NOT NULL,
  `expiry_date` date NOT NULL,
  `passed` tinyint(1) NOT NULL DEFAULT '1',
  `file_path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_by` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inspection_criterion_result` (`inspection_criterion_id`),
  KEY `fk_inspection_criterion_result_created_by` (`created_by`),
  KEY `idx_inspection_criterion_result_expiry_date` (`expiry_date`),
  KEY `idx_inspection_criterion_result_pass_status` (`passed`),
  CONSTRAINT `fk_inspection_criterion_result_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_inspection_criterion_result_criterion` FOREIGN KEY (`inspection_criterion_id`) REFERENCES `inspection_criteria` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inspection_criterion_results`
--

LOCK TABLES `inspection_criterion_results` WRITE;
/*!40000 ALTER TABLE `inspection_criterion_results` DISABLE KEYS */;
/*!40000 ALTER TABLE `inspection_criterion_results` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inspection_requests`
--

DROP TABLE IF EXISTS `inspection_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inspection_requests` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `production_lot_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `inspection_unit` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sample_sent_date` date NOT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_by` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_inspection_request_lot` (`production_lot_id`),
  KEY `fk_inspection_request_created_by` (`created_by`),
  CONSTRAINT `fk_inspection_request_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_inspection_request_lot` FOREIGN KEY (`production_lot_id`) REFERENCES `production_lot` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inspection_requests`
--

LOCK TABLES `inspection_requests` WRITE;
/*!40000 ALTER TABLE `inspection_requests` DISABLE KEYS */;
/*!40000 ALTER TABLE `inspection_requests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `invitations`
--

DROP TABLE IF EXISTS `invitations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `invitations` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `organization_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role_id` int NOT NULL,
  `token` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expiry_date` datetime NOT NULL,
  `used_at` datetime DEFAULT NULL,
  `created_by` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `token` (`token`),
  KEY `fk_invitation_org` (`organization_id`),
  KEY `fk_invitation_role` (`role_id`),
  KEY `fk_invitation_user` (`created_by`),
  CONSTRAINT `fk_invitation_org` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`organization_id`),
  CONSTRAINT `fk_invitation_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`),
  CONSTRAINT `fk_invitation_user` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `invitations`
--

LOCK TABLES `invitations` WRITE;
/*!40000 ALTER TABLE `invitations` DISABLE KEYS */;
/*!40000 ALTER TABLE `invitations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `label_export_history`
--

DROP TABLE IF EXISTS `label_export_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `label_export_history` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `shipment_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `exported_by` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `organization_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `exported_at` datetime NOT NULL,
  `start_index` int NOT NULL,
  `end_index` int NOT NULL,
  `quantity` int NOT NULL,
  `label_size` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_leh_shipment` (`shipment_id`),
  KEY `fk_leh_exported_by` (`exported_by`),
  KEY `fk_leh_organization` (`organization_id`),
  CONSTRAINT `fk_leh_exported_by` FOREIGN KEY (`exported_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_leh_organization` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`organization_id`),
  CONSTRAINT `fk_leh_shipment` FOREIGN KEY (`shipment_id`) REFERENCES `shipments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `label_export_history`
--

LOCK TABLES `label_export_history` WRITE;
/*!40000 ALTER TABLE `label_export_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `label_export_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `login_anomalies`
--

DROP TABLE IF EXISTS `login_anomalies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `login_anomalies` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `organization_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `attempt_count` int DEFAULT NULL,
  `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
  `country_code` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `detected_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `status` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPEN',
  `notification_id` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_login_anomalies_org_detected` (`organization_id`,`detected_at` DESC),
  KEY `idx_login_anomalies_user_detected` (`user_id`,`detected_at` DESC),
  KEY `idx_login_anomalies_status` (`status`),
  CONSTRAINT `fk_login_anomalies_org` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`organization_id`),
  CONSTRAINT `fk_login_anomalies_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `login_anomalies`
--

LOCK TABLES `login_anomalies` WRITE;
/*!40000 ALTER TABLE `login_anomalies` DISABLE KEYS */;
INSERT INTO `login_anomalies` VALUES ('3a74c9da-b5dc-4220-b014-ddff9c6479cd','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','8f89a500-a1c4-11f1-9ae2-029fd41577b3','UNUSUAL_COUNTRY',NULL,'172.18.0.1',NULL,'2026-08-27 08:13:24.670','OPEN',NULL),('e7309c77-4a6c-44ed-8ae7-ac06f2cf5828','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','UNUSUAL_COUNTRY',NULL,'172.18.0.1',NULL,'2026-08-27 18:26:47.736','OPEN',NULL);
/*!40000 ALTER TABLE `login_anomalies` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `login_attempts`
--

DROP TABLE IF EXISTS `login_attempts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `login_attempts` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `username_input` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `result` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
  `country_code` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_new_country` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_login_attempts_user_created` (`user_id`,`created_at` DESC),
  KEY `idx_login_attempts_user_result` (`user_id`,`result`),
  KEY `idx_login_attempts_user_country` (`user_id`,`country_code`),
  CONSTRAINT `fk_login_attempts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `login_attempts`
--

LOCK TABLES `login_attempts` WRITE;
/*!40000 ALTER TABLE `login_attempts` DISABLE KEYS */;
INSERT INTO `login_attempts` VALUES ('2bacd522-ef9d-492b-a2ee-90eb0e7fa552','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','manager','SUCCESS','172.18.0.1',NULL,0,'2026-08-27 18:26:47.637'),('3de60b4f-784c-46ec-a796-0f6c95dd5f25','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','admin','SUCCESS','0:0:0:0:0:0:0:1',NULL,0,'2026-08-27 03:57:34.992'),('5521920f-5e6c-4ca9-bf92-201502e4cd8b','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','manager','SUCCESS','0:0:0:0:0:0:0:1',NULL,0,'2026-08-27 04:03:59.976'),('5bfced8e-3b9b-463b-a7b2-82d0f130f22e','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','admin','SUCCESS','172.18.0.1',NULL,0,'2026-08-27 08:13:24.600'),('65d2a999-5d07-4ef3-99a3-654bec93c0e9','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','admin','FAILED','172.18.0.1',NULL,0,'2026-08-27 15:09:05.788'),('733ec064-ec40-4b10-9d8f-bd745fd6ca93','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','admin','SUCCESS','0:0:0:0:0:0:0:1',NULL,0,'2026-08-27 04:43:30.798'),('7434dcd5-9c40-49ed-b236-8a7a07b6c280','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','manager','SUCCESS','0:0:0:0:0:0:0:1',NULL,0,'2026-08-27 05:38:12.223'),('74d1377c-5f1e-4b8e-8d17-32c1171ee28f','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','manager','SUCCESS','0:0:0:0:0:0:0:1',NULL,0,'2026-08-27 04:43:29.097'),('7a4a8f22-3655-4046-84bd-2c863d0d2a49','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','manager','SUCCESS','0:0:0:0:0:0:0:1',NULL,0,'2026-08-27 03:57:34.432'),('93365d26-e7f8-4fef-b222-49c1f523e7e6','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','manager','SUCCESS','0:0:0:0:0:0:0:1',NULL,0,'2026-08-27 05:59:04.309'),('af118f10-ac95-4e34-811e-1abb1024b1b6','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','admin','SUCCESS','172.18.0.1',NULL,0,'2026-08-28 01:13:12.288'),('b6887bec-86f1-4ea4-8bb0-11e3ab9222d0','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','admin','SUCCESS','0:0:0:0:0:0:0:1',NULL,0,'2026-08-27 04:04:00.738');
/*!40000 ALTER TABLE `login_attempts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_read` tinyint(1) NOT NULL DEFAULT '0',
  `read_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_notification_user` (`user_id`),
  KEY `idx_notification_read` (`is_read`),
  KEY `idx_notification_created` (`created_at`),
  CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notifications`
--

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
INSERT INTO `notifications` VALUES ('0f6f8523-57c4-4c58-b758-910dd14210a3','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','LOGIN_ANOMALY_DETECTED','Phát hiện đăng nhập bất thường','Tài khoản admin (Quản trị viên hệ thống) phát hiện hoạt động đăng nhập bất thường. Lý do: UNUSUAL_COUNTRY. Địa chỉ IP: 172.18.0.1. Quốc gia: Không xác định.',1,'2026-08-27 15:13:33','2026-08-27 15:13:25'),('5f760f95-da03-48d1-b6f8-58d4bafe826c','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','LOGIN_ANOMALY_DETECTED','Phát hiện đăng nhập bất thường','Tài khoản manager (Qu???n l?? HTX ABC) phát hiện hoạt động đăng nhập bất thường. Lý do: UNUSUAL_COUNTRY. Địa chỉ IP: 172.18.0.1. Quốc gia: Không xác định.',1,'2026-08-28 01:26:53','2026-08-28 01:26:48');
/*!40000 ALTER TABLE `notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `offline_sync_logs`
--

DROP TABLE IF EXISTS `offline_sync_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `offline_sync_logs` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sync_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `offline_event_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `production_lot_id` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shipment_id` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `event_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `failure_reason` text COLLATE utf8mb4_unicode_ci,
  `synced_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `offline_event_id` (`offline_event_id`),
  KEY `fk_sync_user` (`user_id`),
  KEY `idx_sync_status` (`status`),
  CONSTRAINT `fk_sync_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `offline_sync_logs`
--

LOCK TABLES `offline_sync_logs` WRITE;
/*!40000 ALTER TABLE `offline_sync_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `offline_sync_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `organization_role_permissions`
--

DROP TABLE IF EXISTS `organization_role_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `organization_role_permissions` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `organization_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role_id` int NOT NULL,
  `permission_id` int NOT NULL,
  `is_enabled` tinyint(1) NOT NULL DEFAULT '0',
  `updated_by` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_org_role_permission` (`organization_id`,`role_id`,`permission_id`),
  KEY `idx_orp_org` (`organization_id`),
  KEY `idx_orp_role` (`role_id`),
  KEY `idx_orp_permission` (`permission_id`),
  KEY `idx_orp_updated_by` (`updated_by`),
  CONSTRAINT `fk_orp_organization` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`organization_id`),
  CONSTRAINT `fk_orp_permission` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`permission_id`),
  CONSTRAINT `fk_orp_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`),
  CONSTRAINT `fk_orp_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `organization_role_permissions`
--

LOCK TABLES `organization_role_permissions` WRITE;
/*!40000 ALTER TABLE `organization_role_permissions` DISABLE KEYS */;
/*!40000 ALTER TABLE `organization_role_permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `organization_users`
--

DROP TABLE IF EXISTS `organization_users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `organization_users` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `organization_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role_id` int NOT NULL,
  `custom_permissions` text COLLATE utf8mb4_unicode_ci,
  `joined_at` datetime NOT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_org_user` (`organization_id`,`user_id`),
  KEY `fk_org_user_user` (`user_id`),
  KEY `fk_org_user_role` (`role_id`),
  CONSTRAINT `fk_org_user_org` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`organization_id`),
  CONSTRAINT `fk_org_user_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`),
  CONSTRAINT `fk_org_user_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `organization_users`
--

LOCK TABLES `organization_users` WRITE;
/*!40000 ALTER TABLE `organization_users` DISABLE KEYS */;
INSERT INTO `organization_users` VALUES ('8f8ba6e4-a1c4-11f1-9ae2-029fd41577b3','8f89a500-a1c4-11f1-9ae2-029fd41577b3','8f8a6000-a1c4-11f1-9ae2-029fd41577b3',1,NULL,'2026-08-27 03:08:21','ACTIVE'),('c1f4e92b-a1d1-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','c1f0bec8-a1d1-11f1-9ae2-029fd41577b3',3,NULL,'2026-08-27 04:42:49','ACTIVE'),('c1f51dd8-a1d1-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','c1f0bf7b-a1d1-11f1-9ae2-029fd41577b3',3,NULL,'2026-08-27 04:42:49','ACTIVE'),('c1f5227a-a1d1-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','c1f0bf92-a1d1-11f1-9ae2-029fd41577b3',3,NULL,'2026-08-27 04:42:49','ACTIVE'),('c1f52443-a1d1-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','c1f0bfa5-a1d1-11f1-9ae2-029fd41577b3',3,NULL,'2026-08-27 04:42:49','ACTIVE'),('c1f525aa-a1d1-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','c1f0bfb6-a1d1-11f1-9ae2-029fd41577b3',3,NULL,'2026-08-27 04:42:49','ACTIVE'),('c1f52db4-a1d1-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','c1f0bfc7-a1d1-11f1-9ae2-029fd41577b3',3,NULL,'2026-08-27 04:42:49','ACTIVE'),('c1f52f58-a1d1-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','c1f0bfd7-a1d1-11f1-9ae2-029fd41577b3',3,NULL,'2026-08-27 04:42:49','ACTIVE'),('c1f5309b-a1d1-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','c1f0bfe7-a1d1-11f1-9ae2-029fd41577b3',3,NULL,'2026-08-27 04:42:49','ACTIVE'),('c1f533ed-a1d1-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','c1f0bff7-a1d1-11f1-9ae2-029fd41577b3',3,NULL,'2026-08-27 04:42:49','ACTIVE'),('c1f53566-a1d1-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','c1f0c007-a1d1-11f1-9ae2-029fd41577b3',3,NULL,'2026-08-27 04:42:49','ACTIVE'),('c1f536aa-a1d1-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','c1f0c023-a1d1-11f1-9ae2-029fd41577b3',3,NULL,'2026-08-27 04:42:49','ACTIVE'),('c1f537de-a1d1-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','c1f0c034-a1d1-11f1-9ae2-029fd41577b3',3,NULL,'2026-08-27 04:42:49','ACTIVE'),('c1f53919-a1d1-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','c1f0c044-a1d1-11f1-9ae2-029fd41577b3',3,NULL,'2026-08-27 04:42:49','ACTIVE'),('d74da29b-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',2,NULL,'2026-08-27 03:24:41','ACTIVE'),('d74da840-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3',3,NULL,'2026-08-27 03:24:41','ACTIVE');
/*!40000 ALTER TABLE `organization_users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `organizations`
--

DROP TABLE IF EXISTS `organizations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `organizations` (
  `organization_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`organization_id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `organizations`
--

LOCK TABLES `organizations` WRITE;
/*!40000 ALTER TABLE `organizations` DISABLE KEYS */;
INSERT INTO `organizations` VALUES ('8f89a500-a1c4-11f1-9ae2-029fd41577b3','Hệ thống','SYSTEM','SYSTEM','ACTIVE','System Organization',NULL,NULL,'2026-08-27 03:08:21','2026-08-27 03:08:21'),('c1f7d26c-a1d1-11f1-9ae2-029fd41577b3','SEED_TEST Don vi 01','SEEDDV01','COOPERATIVE','ACTIVE','Viet Nam','0987000001','dv01@seed.test','2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1f7e18b-a1d1-11f1-9ae2-029fd41577b3','SEED_TEST Don vi 02','SEEDDV02','ENTERPRISE','ACTIVE','Viet Nam','0987000002','dv02@seed.test','2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1f7e47e-a1d1-11f1-9ae2-029fd41577b3','SEED_TEST Don vi 03','SEEDDV03','COOPERATIVE','ACTIVE','Viet Nam','0987000003','dv03@seed.test','2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1f7e8a7-a1d1-11f1-9ae2-029fd41577b3','SEED_TEST Don vi 04','SEEDDV04','ENTERPRISE','ACTIVE','Viet Nam','0987000004','dv04@seed.test','2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1f7e9d3-a1d1-11f1-9ae2-029fd41577b3','SEED_TEST Don vi 05','SEEDDV05','COOPERATIVE','INACTIVE','Viet Nam','0987000005','dv05@seed.test','2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1f7ec7b-a1d1-11f1-9ae2-029fd41577b3','SEED_TEST Don vi 06','SEEDDV06','ENTERPRISE','ACTIVE','Viet Nam','0987000006','dv06@seed.test','2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1f7ed8d-a1d1-11f1-9ae2-029fd41577b3','SEED_TEST Don vi 07','SEEDDV07','COOPERATIVE','ACTIVE','Viet Nam','0987000007','dv07@seed.test','2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1f7ee61-a1d1-11f1-9ae2-029fd41577b3','SEED_TEST Don vi 08','SEEDDV08','ENTERPRISE','ACTIVE','Viet Nam','0987000008','dv08@seed.test','2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1f7f17c-a1d1-11f1-9ae2-029fd41577b3','SEED_TEST Don vi 09','SEEDDV09','COOPERATIVE','ACTIVE','Viet Nam','0987000009','dv09@seed.test','2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1f7f36f-a1d1-11f1-9ae2-029fd41577b3','SEED_TEST Don vi 10','SEEDDV10','ENTERPRISE','INACTIVE','Viet Nam','0987000010','dv10@seed.test','2026-08-27 04:42:49','2026-08-27 04:42:49'),('d74c09f2-a1c6-11f1-9ae2-029fd41577b3','HTX N??ng S???n ABC','ABC','COOPERATIVE','ACTIVE','S??? 1 ???????ng ABC, Qu???n Ba ????nh','0912345678','abc@test.com','2026-08-27 03:24:41','2026-08-27 03:24:41'),('e13b7078-a1c6-11f1-9ae2-029fd41577b3','SEED_TEST HTX Thanh Xu??n','SEEDTX','COOPERATIVE','ACTIVE','H?? N???i','0988000001','tx@seed.test','2026-08-27 03:24:57','2026-08-27 03:24:57'),('e13b782a-a1c6-11f1-9ae2-029fd41577b3','SEED_TEST C??ng ty N??ng s???n Vi???t','SEEDNSV','ENTERPRISE','ACTIVE','HCM','0988000002','nsv@seed.test','2026-08-27 03:24:57','2026-08-27 03:24:57'),('e13b7a0a-a1c6-11f1-9ae2-029fd41577b3','SEED_TEST H???p t??c x?? M?? K??ng','SEEDMK','COOPERATIVE','INACTIVE','C???n Th??','0988000003','mk@seed.test','2026-08-27 03:24:57','2026-08-27 03:24:57');
/*!40000 ALTER TABLE `organizations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `partner_api_keys`
--

DROP TABLE IF EXISTS `partner_api_keys`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `partner_api_keys` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `organization_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `partner_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `key_prefix` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `key_hash` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `rate_limit_per_hour` int NOT NULL,
  `expires_at` datetime NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_calls` bigint DEFAULT '0',
  `failed_calls` bigint DEFAULT '0',
  `last_called_at` datetime DEFAULT NULL,
  `last_call_status` int DEFAULT NULL,
  `last_call_ip` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_by` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL,
  `revoked_by` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `revoked_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `key_hash` (`key_hash`),
  KEY `fk_partner_api_keys_created_by` (`created_by`),
  KEY `fk_partner_api_keys_revoked_by` (`revoked_by`),
  KEY `idx_partner_api_keys_org_status` (`organization_id`,`status`),
  CONSTRAINT `fk_partner_api_keys_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_partner_api_keys_org` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`organization_id`),
  CONSTRAINT `fk_partner_api_keys_revoked_by` FOREIGN KEY (`revoked_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `partner_api_keys`
--

LOCK TABLES `partner_api_keys` WRITE;
/*!40000 ALTER TABLE `partner_api_keys` DISABLE KEYS */;
INSERT INTO `partner_api_keys` VALUES ('11257050-90e9-4edd-b770-489f204020d6','8f89a500-a1c4-11f1-9ae2-029fd41577b3','36','nks_live_15069fb3','ca54c166e9dbf3e08ad478495ccdd49f514f6d70e62e53a5a7f1e42d6208ae2d',100,'2026-09-27 08:21:00','ACTIVE',0,0,NULL,NULL,NULL,'8f8a6000-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 15:21:44',NULL,NULL),('1545480a-72e4-4b83-b674-0f4695629fa6','8f89a500-a1c4-11f1-9ae2-029fd41577b3','10','nks_live_c521b9d9','857d6a688f74bb7e014c90433f906c6a310f8b8550a92cd20702b955d37a465a',100,'2026-09-27 08:20:00','ACTIVE',0,0,NULL,NULL,NULL,'8f8a6000-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 15:20:54',NULL,NULL),('28d14f9f-8f9e-4bf9-b970-e26aaf9cd053','8f89a500-a1c4-11f1-9ae2-029fd41577b3','11','nks_live_8edca786','e42f9ed9b304f69a6f1c22c4c0003cc0f9cd53a881b6cf5d6a820744b7066df6',100,'2026-09-27 08:22:00','ACTIVE',0,0,NULL,NULL,NULL,'8f8a6000-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 15:22:23',NULL,NULL),('3d05ba09-0979-4f93-8801-f274d02b9655','8f89a500-a1c4-11f1-9ae2-029fd41577b3','00','nks_live_a1c6c2fa','11a9d706ed831079d9b6a05f600eac563b5cd83a51c58dc0185cc7dd1b911a0d',100,'2026-09-27 08:22:00','ACTIVE',0,0,NULL,NULL,NULL,'8f8a6000-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 15:22:13',NULL,NULL),('4e6c1954-e5fb-4cc3-ba48-541244e7b25e','8f89a500-a1c4-11f1-9ae2-029fd41577b3','52','nks_live_90fb58d9','bb68c5825491789204b9000392e27f123379815815db7eddef51eff89cbe38fe',100,'2026-09-27 08:21:00','ACTIVE',0,0,NULL,NULL,NULL,'8f8a6000-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 15:21:04',NULL,NULL),('56139679-6b0a-43f7-8566-db4a9e60140e','8f89a500-a1c4-11f1-9ae2-029fd41577b3','66','nks_live_7c5cb427','1169ac3279b26491da89ce5e121de12ee25aacd6e53011b3910f2a33784f145f',100,'2026-09-27 08:21:00','ACTIVE',0,0,NULL,NULL,NULL,'8f8a6000-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 15:22:02',NULL,NULL),('aaaaaaaa-1111-1111-1111-111111111111','c1f7d26c-a1d1-11f1-9ae2-029fd41577b3','CÃ´ng ty TNHH NÃ´ng sáº£n Viá»‡t','ngs_abc123','737db9f59e603519379ab6e4f2537e99339787d09a1d0390425dc331d6e757e2',1000,'2026-11-25 08:57:32','ACTIVE',1250,12,'2026-08-27 06:57:32',200,'192.168.1.100','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-07-28 08:57:32',NULL,NULL),('aaaaaaaa-2222-2222-2222-222222222222','c1f7d26c-a1d1-11f1-9ae2-029fd41577b3','CÃ´ng ty Cá»• pháº§n Logistics Xanh','ngs_log456','bc26aad897da27efb3ead9a5d2f2b809db89a607cb0c6712e4bd4d0117d4e5ed',5000,'2027-02-23 08:57:32','ACTIVE',8900,45,'2026-08-27 08:27:32',200,'10.0.0.50','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-12 08:57:32',NULL,NULL),('aaaaaaaa-3333-3333-3333-333333333333','c1f7e18b-a1d1-11f1-9ae2-029fd41577b3','CÃ´ng ty TNHH Cháº¿ biáº¿n NÃ´ng sáº£n Minh','ngs_min789','6a040ee3b67150a58527aa20af4c4554bfe98f75ef8d961c58e1992d03dc2d0e',2000,'2026-10-26 08:57:32','ACTIVE',3400,23,'2026-08-27 04:57:32',200,'203.0.113.45','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-07-13 08:57:32',NULL,NULL),('aaaaaaaa-4444-4444-4444-444444444444','c1f7e18b-a1d1-11f1-9ae2-029fd41577b3','CÃ´ng ty Cá»• pháº§n Xuáº¥t kháº©u NÃ´ng sáº£n ÄÃ  Láº¡t','ngs_dlt001','aa2f091604650d180f79db1b5842e54de3b34597d17b9de9e48252d5fd3526c7',3000,'2026-09-26 08:57:32','REVOKED',15600,89,'2026-08-25 08:57:32',401,'198.51.100.23','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','2026-05-29 08:57:32','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','2026-08-25 08:57:32'),('aaaaaaaa-5555-5555-5555-555555555555','c1f7e47e-a1d1-11f1-9ae2-029fd41577b3','CÃ´ng ty TNHH CÃ´ng nghá»‡ NÃ´ng nghiá»‡p SmartFarm','ngs_smt111','659cc6862220f42358fccf5398555be672eeaf20970407978c94d496467de82b',10000,'2027-08-27 08:57:32','ACTIVE',45600,120,'2026-08-27 08:42:32',200,'172.16.0.10','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-17 08:57:32',NULL,NULL),('aaaaaaaa-6666-6666-6666-666666666666','c1f7e47e-a1d1-11f1-9ae2-029fd41577b3','CÃ´ng ty TNHH Äáº§u tÆ° NÃ´ng nghiá»‡p Highland','ngs_hld222','8b2c9472fa6b8f905a6e83b34e65e2e67cf86bd4716cb6813a85e2c6e63ca191',500,'2026-08-17 08:57:32','EXPIRED',890,5,'2026-08-15 08:57:32',200,'103.45.67.89','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','2026-05-19 08:57:32',NULL,NULL),('aaaaaaaa-7777-7777-7777-777777777777','c1f7e8a7-a1d1-11f1-9ae2-029fd41577b3','CÃ´ng ty Cá»• pháº§n Thá»±c pháº©m An toÃ n VinFood','ngs_vin333','84446e63f12d0ec2301d393819cb0a39e7a8e3a1afbfda602baa75237a9017e7',2000,'2026-12-25 08:57:32','ACTIVE',7200,34,'2026-08-27 07:57:32',200,'113.160.200.15','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-07 08:57:32',NULL,NULL),('aaaaaaaa-8888-8888-8888-888888888888','c1f7e8a7-a1d1-11f1-9ae2-029fd41577b3','CÃ´ng ty TNHH Logistics Miá»n TÃ¢y','ngs_log444','625edfd849010a3c45b46d57556dd3085264dd5b82d9f8bc2d73fc74849f3dbb',1000,'2026-10-11 08:57:32','REVOKED',2100,15,'2026-08-22 08:57:32',403,'45.120.88.33','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','2026-06-28 08:57:32','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','2026-08-22 08:57:32'),('aaaaaaaa-9999-9999-9999-999999999999','8f89a500-a1c4-11f1-9ae2-029fd41577b3','Cá»•ng thÃ´ng tin Quá»‘c gia vá» NÃ´ng nghiá»‡p','ngs_gov555','aed3aa637aaa60f57fb9e5efb14590c53ba02b8c50b5256baf43004ace161651',50000,'2027-08-27 08:57:32','ACTIVE',125000,500,'2026-08-27 08:52:32',200,'10.10.10.10','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','2026-08-22 08:57:32',NULL,NULL),('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','8f89a500-a1c4-11f1-9ae2-029fd41577b3','Há»‡ thá»‘ng truy xuáº¥t nguá»“n gá»‘c trung Æ°Æ¡ng','ngs_trace666','79658776f3f9b4a0f3e3ae021adac74cae8012e35c7feffcd78ab72730f61e57',100000,'2027-08-27 08:57:32','ACTIVE',250000,1200,'2026-08-27 08:56:32',200,'10.10.10.11','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','2026-08-26 08:57:32',NULL,NULL),('b3d327cc-9331-4ef7-ad1a-49d614d7dfbb','8f89a500-a1c4-11f1-9ae2-029fd41577b3','22','nks_live_07596a83','85c8f912b05fcd1b8242d74f5382362196d2bc4fe638280bb827319036a9f064',100,'2026-09-27 08:21:00','ACTIVE',0,0,NULL,NULL,NULL,'8f8a6000-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 15:21:32',NULL,NULL),('c1bee202-e3e3-495f-aa40-3939a9c17a73','8f89a500-a1c4-11f1-9ae2-029fd41577b3','8','nks_live_78205368','4a1d6e46c62fe915b532e35c945b3ea96720524351c6f3ecce34ec302d455b02',100,'2026-09-27 08:21:00','ACTIVE',0,0,NULL,NULL,NULL,'8f8a6000-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 15:21:52',NULL,NULL),('cb2a2400-389d-4229-9a0b-383d67395b88','8f89a500-a1c4-11f1-9ae2-029fd41577b3','88\'','nks_live_4dfda835','2e8360ad8bacece3de42d24467fd4cf49f997cea283914d2b8095ac9cf631b1e',100,'2026-09-27 08:21:00','ACTIVE',0,0,NULL,NULL,NULL,'8f8a6000-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 15:21:12',NULL,NULL),('cf8b8991-8afd-4235-a0b9-d559e502eae2','8f89a500-a1c4-11f1-9ae2-029fd41577b3','ghgf','nks_live_e78b623e','d5599073c18c1d4e77a62e7f718c2bb8797b53f51e7d6b14f17ab72007d7c038',100,'2026-09-27 08:19:00','ACTIVE',0,0,NULL,NULL,NULL,'8f8a6000-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 15:19:59',NULL,NULL),('e5ea56da-3d1a-488b-892a-3836318a311a','8f89a500-a1c4-11f1-9ae2-029fd41577b3','1','nks_live_0595086d','fc5e07b2b35181f0bdd3602170ab398c281be046d0b13a94845ab953a6d63638',100,'2026-09-27 08:21:00','ACTIVE',0,0,NULL,NULL,NULL,'8f8a6000-a1c4-11f1-9ae2-029fd41577b3','2026-08-27 15:21:21',NULL,NULL);
/*!40000 ALTER TABLE `partner_api_keys` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `permissions`
--

DROP TABLE IF EXISTS `permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permissions` (
  `permission_id` int NOT NULL AUTO_INCREMENT,
  `resource` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `action` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`permission_id`),
  UNIQUE KEY `uk_permission_resource_action` (`resource`,`action`)
) ENGINE=InnoDB AUTO_INCREMENT=64 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `permissions`
--

LOCK TABLES `permissions` WRITE;
/*!40000 ALTER TABLE `permissions` DISABLE KEYS */;
INSERT INTO `permissions` VALUES (1,'organization','READ','Xem thông tin tổ chức'),(2,'organization','UPDATE','Cập nhật thông tin tổ chức'),(3,'farm_area','CREATE','Tạo vùng trồng'),(4,'farm_area','READ','Xem vùng trồng'),(5,'farm_area','UPDATE','Cập nhật vùng trồng'),(6,'farm_area','DELETE','Xóa vùng trồng'),(7,'production_lot','CREATE','Tạo lô sản xuất'),(8,'production_lot','READ','Xem lô sản xuất'),(9,'production_lot','UPDATE','Cập nhật lô sản xuất'),(10,'production_lot','APPROVE','Duyệt lô sản xuất'),(11,'farm_log','CREATE','Tạo nhật ký'),(12,'farm_log','READ','Xem nhật ký'),(13,'farm_log','UPDATE','Cập nhật nhật ký'),(14,'farm_log','VERIFY','Xác minh nhật ký'),(15,'shipment','CREATE','Tạo lô hàng'),(16,'shipment','READ','Xem lô hàng'),(17,'shipment','UPDATE','Cập nhật lô hàng'),(18,'shipment','EXPORT','Xuất hồ sơ truy xuất'),(19,'trace_code','CREATE','Sinh mã truy xuất'),(20,'trace_code','READ','Xem mã truy xuất'),(21,'trace_code','ACTIVATE','Kích hoạt mã'),(22,'chain_event','CREATE','Ghi sự kiện chuỗi'),(23,'chain_event','READ','Xem dòng sự kiện'),(24,'chain_event','UPDATE','Đính chính sự kiện'),(25,'certification','CREATE','Tạo chứng nhận'),(26,'certification','READ','Xem chứng nhận'),(27,'certification','UPDATE','Cập nhật chứng nhận'),(28,'standard','CREATE','Tạo tiêu chuẩn'),(29,'standard','READ','Xem tiêu chuẩn'),(30,'standard','UPDATE','Cập nhật tiêu chuẩn'),(31,'product_category','CREATE','Tạo loại nông sản'),(32,'product_category','READ','Xem loại nông sản'),(33,'product_category','UPDATE','Cập nhật loại nông sản'),(34,'organization_user','CREATE','Thêm thành viên'),(35,'organization_user','READ','Xem thành viên'),(36,'organization_user','UPDATE','Cập nhật thành viên'),(37,'organization_user','DELETE','Xóa thành viên'),(38,'role_permission','READ','Xem phân quyền'),(39,'role_permission','UPDATE','Cấu hình phân quyền'),(40,'notification','READ','Xem thông báo'),(41,'alert','READ','Xem cảnh báo'),(42,'alert','UPDATE','Xử lý cảnh báo'),(43,'report','READ','Xem báo cáo'),(44,'report','EXPORT','Xuất báo cáo'),(45,'scan_statistics','READ','Xem thống kê lượt quét'),(46,'activity_log','READ','Xem lịch sử hoạt động'),(47,'product_feedback','READ','Xem phản ánh sản phẩm'),(48,'code_range','CREATE','Tạo dải mã truy xuất'),(49,'code_range','READ','Xem dải mã truy xuất'),(50,'code_range','UPDATE','Cập nhật dải mã truy xuất'),(51,'traceability','READ','Xem trang truy xuất nguồn gốc'),(52,'recall','CREATE','Tạo yêu cầu thu hồi'),(53,'recall','READ','Xem thu hồi lô'),(54,'export','CREATE','Xuất dữ liệu'),(55,'export','READ','Xem lịch sử xuất dữ liệu'),(56,'user','CREATE','Tạo người dùng'),(57,'user','READ','Xem người dùng'),(58,'user','UPDATE','Cập nhật người dùng'),(59,'user','DELETE','Xóa người dùng'),(60,'input_material','CREATE','Tạo vật tư đầu vào'),(61,'input_material','READ','Xem danh mục vật tư đầu vào'),(62,'input_material','UPDATE','Cập nhật vật tư đầu vào'),(63,'input_material','DELETE','Xóa vật tư đầu vào');
/*!40000 ALTER TABLE `permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product_categories`
--

DROP TABLE IF EXISTS `product_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_categories` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category_group` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `temp_min` decimal(4,1) DEFAULT NULL,
  `temp_max` decimal(4,1) DEFAULT NULL,
  `humidity_min` decimal(5,1) DEFAULT NULL,
  `humidity_max` decimal(5,1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_categories_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_categories`
--

LOCK TABLES `product_categories` WRITE;
/*!40000 ALTER TABLE `product_categories` DISABLE KEYS */;
INSERT INTO `product_categories` VALUES ('32e7eb31-5ced-4ae9-8287-f33f4e009b49','Nho','Quả ngọt',NULL,1,NULL,NULL,NULL,NULL),('8a1e02c8-11c4-4ef7-9846-639baa956ebd','Dỗi','Cây ăn quả',NULL,1,NULL,NULL,NULL,NULL),('8ce59a68-8ad3-4095-8be4-80c3f19b12be','Chè','Chè',NULL,1,NULL,NULL,NULL,NULL),('bd70445f-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L??a','C??y l????ng th???c','L??a n???p/test',1,NULL,NULL,NULL,NULL),('bd704b75-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST C?? ph??','C??y c??ng nghi???p','C?? ph?? robusta/test',1,NULL,NULL,NULL,NULL),('bd704d75-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST H??? ti??u','C??y gia v???','Ti??u ??en/test',1,NULL,NULL,NULL,NULL),('bd704e66-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST S???u ri??ng','C??y ??n qu???','S???u ri??ng Ri6/test',1,NULL,NULL,NULL,NULL),('bd7050ef-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST Xo??i','C??y ??n qu???','Xo??i c??t/test',1,NULL,NULL,NULL,NULL),('bd7051ef-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST Rau c???i','Rau','C???i xanh/test',1,NULL,NULL,NULL,NULL),('e343c9dc-220c-445a-b4c1-b6f491a85050','Taó','Cây ăn quả',NULL,1,NULL,NULL,NULL,NULL),('eddc4f88-6556-4a7e-ada7-6635fc80de53','Bồng bồng','Cây ăn quả',NULL,1,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `product_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product_feedbacks`
--

DROP TABLE IF EXISTS `product_feedbacks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_feedbacks` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `production_lot_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_feedback_production_lot` (`production_lot_id`),
  CONSTRAINT `fk_feedback_production_lot` FOREIGN KEY (`production_lot_id`) REFERENCES `production_lot` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_feedbacks`
--

LOCK TABLES `product_feedbacks` WRITE;
/*!40000 ALTER TABLE `product_feedbacks` DISABLE KEYS */;
/*!40000 ALTER TABLE `product_feedbacks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `production_lot`
--

DROP TABLE IF EXISTS `production_lot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `production_lot` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `organization_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `farm_area_id` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `product_category_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expected_quantity` double NOT NULL,
  `expected_quantity_unit` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `actual_quantity` double DEFAULT NULL,
  `planting_date` date DEFAULT NULL,
  `harvest_date` date DEFAULT NULL,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `approval_notes` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_by` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `approved_by` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_PRODUCTION_LOT_ON_ORGANIZATION` (`organization_id`),
  KEY `FK_PRODUCTION_LOT_ON_FARM_AREA` (`farm_area_id`),
  KEY `FK_PRODUCTION_LOT_ON_PRODUCT_CATEGORY` (`product_category_id`),
  KEY `FK_PRODUCTION_LOT_ON_CREATED_BY` (`created_by`),
  KEY `FK_PRODUCTION_LOT_ON_APPROVED_BY` (`approved_by`),
  CONSTRAINT `FK_PRODUCTION_LOT_ON_APPROVED_BY` FOREIGN KEY (`approved_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FK_PRODUCTION_LOT_ON_CREATED_BY` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FK_PRODUCTION_LOT_ON_FARM_AREA` FOREIGN KEY (`farm_area_id`) REFERENCES `farm_areas` (`id`),
  CONSTRAINT `FK_PRODUCTION_LOT_ON_ORGANIZATION` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`organization_id`),
  CONSTRAINT `FK_PRODUCTION_LOT_ON_PRODUCT_CATEGORY` FOREIGN KEY (`product_category_id`) REFERENCES `product_categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `production_lot`
--

LOCK TABLES `production_lot` WRITE;
/*!40000 ALTER TABLE `production_lot` DISABLE KEYS */;
INSERT INTO `production_lot` VALUES ('dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c2dd6-a1c6-11f1-9ae2-029fd41577b3','bd704b75-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 01',1037,'KG',980,'2026-05-30','2026-07-29','PENDING',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-07-09 03:24:51','2026-07-09 03:24:51'),('dd2d9da7-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c3308-a1c6-11f1-9ae2-029fd41577b3','bd704d75-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 02',1074,'KG',1010,'2026-05-31','2026-07-30','APPROVED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-07-10 03:24:51','2026-07-10 03:24:51'),('dd2da207-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c344a-a1c6-11f1-9ae2-029fd41577b3','bd704e66-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 03',1111,'KG',1040,'2026-06-01','2026-07-31','REJECTED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-07-11 03:24:51','2026-07-11 03:24:51'),('dd2da415-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c3543-a1c6-11f1-9ae2-029fd41577b3','bd7050ef-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 04',1148,'KG',1070,'2026-06-02','2026-08-01','HARVESTED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-07-12 03:24:51','2026-07-12 03:24:51'),('dd2da5d1-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c362e-a1c6-11f1-9ae2-029fd41577b3','bd7051ef-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 05',1185,'KG',1100,'2026-06-03','2026-08-02','PREPROCESSED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-07-13 03:24:51','2026-07-13 03:24:51'),('dd2daa88-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c39fd-a1c6-11f1-9ae2-029fd41577b3','bd70445f-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 06',1222,'KG',1130,'2026-06-04','2026-08-03','PACKAGED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-07-14 03:24:51','2026-07-14 03:24:51'),('dd2dac6e-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c3b28-a1c6-11f1-9ae2-029fd41577b3','bd704b75-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 07',1259,'KG',1160,'2026-06-05','2026-08-04','DRAFT',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-07-15 03:24:51','2026-07-15 03:24:51'),('dd2daded-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c3c0a-a1c6-11f1-9ae2-029fd41577b3','bd704d75-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 08',1296,'KG',1190,'2026-06-06','2026-08-05','PENDING',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-07-16 03:24:51','2026-07-16 03:24:51'),('dd2daf75-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c3ce9-a1c6-11f1-9ae2-029fd41577b3','bd704e66-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 09',1333,'KG',1220,'2026-06-07','2026-08-06','APPROVED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-07-17 03:24:51','2026-07-17 03:24:51'),('dd2db0f4-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c3dbb-a1c6-11f1-9ae2-029fd41577b3','bd7050ef-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 10',1370,'KG',1250,'2026-06-08','2026-08-07','REJECTED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-07-18 03:24:51','2026-07-18 03:24:51'),('dd2db27c-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c3fa4-a1c6-11f1-9ae2-029fd41577b3','bd7051ef-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 11',1407,'KG',1280,'2026-06-09','2026-08-08','HARVESTED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-07-19 03:24:51','2026-07-19 03:24:51'),('dd2db6a5-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c40bd-a1c6-11f1-9ae2-029fd41577b3','bd70445f-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 12',1444,'KG',1310,'2026-06-10','2026-08-09','PREPROCESSED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-07-20 03:24:51','2026-07-20 03:24:51'),('dd2db880-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c4195-a1c6-11f1-9ae2-029fd41577b3','bd704b75-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 13',1481,'KG',1340,'2026-06-11','2026-08-10','PACKAGED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-07-21 03:24:51','2026-07-21 03:24:51'),('dd2db9bb-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c43fc-a1c6-11f1-9ae2-029fd41577b3','bd704d75-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 14',1518,'KG',1370,'2026-06-12','2026-08-11','DRAFT',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-07-22 03:24:51','2026-07-22 03:24:51'),('dd2dbae8-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c4516-a1c6-11f1-9ae2-029fd41577b3','bd704e66-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 15',1555,'KG',1400,'2026-06-13','2026-08-12','PENDING',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-07-23 03:24:51','2026-07-23 03:24:51'),('dd2dbc13-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c45ea-a1c6-11f1-9ae2-029fd41577b3','bd7050ef-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 16',1592,'KG',1430,'2026-06-14','2026-08-13','APPROVED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-07-24 03:24:51','2026-07-24 03:24:51'),('dd2dbd43-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c46c2-a1c6-11f1-9ae2-029fd41577b3','bd7051ef-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 17',1629,'KG',1460,'2026-06-15','2026-08-14','REJECTED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-07-25 03:24:51','2026-07-25 03:24:51'),('dd2dbf2d-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c479e-a1c6-11f1-9ae2-029fd41577b3','bd70445f-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 18',1666,'KG',1490,'2026-06-16','2026-08-15','HARVESTED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-07-26 03:24:51','2026-07-26 03:24:51'),('dd2dc201-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c4869-a1c6-11f1-9ae2-029fd41577b3','bd704b75-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 19',1703,'KG',1520,'2026-06-17','2026-08-16','PREPROCESSED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-07-27 03:24:51','2026-07-27 03:24:51'),('dd2dc375-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c4939-a1c6-11f1-9ae2-029fd41577b3','bd704d75-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 20',1740,'KG',1550,'2026-06-18','2026-08-17','PACKAGED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-07-28 03:24:51','2026-07-28 03:24:51'),('dd2dc4b3-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c4a0e-a1c6-11f1-9ae2-029fd41577b3','bd704e66-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 21',1777,'KG',1580,'2026-06-19','2026-08-18','DRAFT',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-07-29 03:24:51','2026-07-29 03:24:51'),('dd2dc5e4-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c4c2b-a1c6-11f1-9ae2-029fd41577b3','bd7050ef-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 22',1814,'KG',1610,'2026-06-20','2026-08-19','PENDING',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-07-30 03:24:51','2026-07-30 03:24:51'),('dd2dc710-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c4d8c-a1c6-11f1-9ae2-029fd41577b3','bd7051ef-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 23',1851,'KG',1640,'2026-06-21','2026-08-20','APPROVED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-07-31 03:24:51','2026-07-31 03:24:51'),('dd2dc844-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c4e74-a1c6-11f1-9ae2-029fd41577b3','bd70445f-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 24',1888,'KG',1670,'2026-06-22','2026-08-21','REJECTED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-08-01 03:24:51','2026-08-01 03:24:51'),('dd2dcc12-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c4f4c-a1c6-11f1-9ae2-029fd41577b3','bd704b75-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 25',1925,'KG',1700,'2026-06-23','2026-08-22','HARVESTED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-08-02 03:24:51','2026-08-02 03:24:51'),('dd2dcde3-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c5020-a1c6-11f1-9ae2-029fd41577b3','bd704d75-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 26',1962,'KG',1730,'2026-06-24','2026-08-23','PREPROCESSED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-08-03 03:24:51','2026-08-03 03:24:51'),('dd2dcf1d-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c50f5-a1c6-11f1-9ae2-029fd41577b3','bd704e66-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 27',1999,'KG',1760,'2026-06-25','2026-08-24','PACKAGED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-08-04 03:24:51','2026-08-04 03:24:51'),('dd2dd051-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c51d3-a1c6-11f1-9ae2-029fd41577b3','bd7050ef-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 28',2036,'KG',1790,'2026-06-26','2026-08-25','DRAFT',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-08-05 03:24:51','2026-08-05 03:24:51'),('dd2dd183-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c52ae-a1c6-11f1-9ae2-029fd41577b3','bd7051ef-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 29',2073,'KG',1820,'2026-06-27','2026-08-26','PENDING',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-08-06 03:24:51','2026-08-06 03:24:51'),('dd2dd2c8-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c538c-a1c6-11f1-9ae2-029fd41577b3','bd70445f-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 30',2110,'KG',1850,'2026-06-28','2026-08-27','APPROVED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-08-07 03:24:51','2026-08-07 03:24:51'),('dd2dd583-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c2dd6-a1c6-11f1-9ae2-029fd41577b3','bd704b75-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 31',2147,'KG',1880,'2026-06-29','2026-08-28','REJECTED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-08-08 03:24:51','2026-08-08 03:24:51'),('dd2dd705-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c3308-a1c6-11f1-9ae2-029fd41577b3','bd704d75-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 32',2184,'KG',1910,'2026-06-30','2026-08-29','HARVESTED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-08-09 03:24:51','2026-08-09 03:24:51'),('dd2dd84f-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c344a-a1c6-11f1-9ae2-029fd41577b3','bd704e66-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 33',2221,'KG',1940,'2026-07-01','2026-08-30','PREPROCESSED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-08-10 03:24:51','2026-08-10 03:24:51'),('dd2dd9c4-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c3543-a1c6-11f1-9ae2-029fd41577b3','bd7050ef-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 34',2258,'KG',1970,'2026-07-02','2026-08-31','PACKAGED',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-08-11 03:24:51','2026-08-11 03:24:51'),('dd2ddc08-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','dd2c362e-a1c6-11f1-9ae2-029fd41577b3','bd7051ef-a1c4-11f1-9ae2-029fd41577b3','SEED_TEST L?? 35',2295,'KG',2000,'2026-07-03','2026-09-01','DRAFT',NULL,'bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3',NULL,'2026-08-12 03:24:51','2026-08-12 03:24:51');
/*!40000 ALTER TABLE `production_lot` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `production_lot_certifications`
--

DROP TABLE IF EXISTS `production_lot_certifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `production_lot_certifications` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `production_lot_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `certification_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `attached_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `attached_by` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `note` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lot_cert` (`production_lot_id`,`certification_id`),
  KEY `fk_plc_cert` (`certification_id`),
  KEY `fk_plc_user` (`attached_by`),
  CONSTRAINT `fk_plc_cert` FOREIGN KEY (`certification_id`) REFERENCES `certifications` (`id`),
  CONSTRAINT `fk_plc_lot` FOREIGN KEY (`production_lot_id`) REFERENCES `production_lot` (`id`),
  CONSTRAINT `fk_plc_user` FOREIGN KEY (`attached_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `production_lot_certifications`
--

LOCK TABLES `production_lot_certifications` WRITE;
/*!40000 ALTER TABLE `production_lot_certifications` DISABLE KEYS */;
/*!40000 ALTER TABLE `production_lot_certifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `production_lot_import_history`
--

DROP TABLE IF EXISTS `production_lot_import_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `production_lot_import_history` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `organization_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `imported_by` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_rows` int NOT NULL,
  `success_count` int NOT NULL,
  `failed_count` int NOT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `imported_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_import_org` (`organization_id`),
  KEY `fk_import_user` (`imported_by`),
  CONSTRAINT `fk_import_org` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`organization_id`),
  CONSTRAINT `fk_import_user` FOREIGN KEY (`imported_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `production_lot_import_history`
--

LOCK TABLES `production_lot_import_history` WRITE;
/*!40000 ALTER TABLE `production_lot_import_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `production_lot_import_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `recall_requests`
--

DROP TABLE IF EXISTS `recall_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recall_requests` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `production_lot_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `requested_by` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `requested_at` datetime NOT NULL,
  `reason` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `evidence` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `approved_by` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `approved_at` datetime DEFAULT NULL,
  `approval_remarks` text COLLATE utf8mb4_unicode_ci,
  `rejected_by` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `rejected_at` datetime DEFAULT NULL,
  `rejection_reason` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_RECALL_REQUEST_ON_REQUESTED_BY` (`requested_by`),
  KEY `FK_RECALL_REQUEST_ON_APPROVED_BY` (`approved_by`),
  KEY `FK_RECALL_REQUEST_ON_REJECTED_BY` (`rejected_by`),
  KEY `idx_recall_requests_status` (`status`),
  KEY `idx_recall_requests_lot_status` (`production_lot_id`,`status`),
  CONSTRAINT `FK_RECALL_REQUEST_ON_APPROVED_BY` FOREIGN KEY (`approved_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FK_RECALL_REQUEST_ON_PRODUCTION_LOT` FOREIGN KEY (`production_lot_id`) REFERENCES `production_lot` (`id`),
  CONSTRAINT `FK_RECALL_REQUEST_ON_REJECTED_BY` FOREIGN KEY (`rejected_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FK_RECALL_REQUEST_ON_REQUESTED_BY` FOREIGN KEY (`requested_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `recall_requests`
--

LOCK TABLES `recall_requests` WRITE;
/*!40000 ALTER TABLE `recall_requests` DISABLE KEYS */;
/*!40000 ALTER TABLE `recall_requests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `recalls`
--

DROP TABLE IF EXISTS `recalls`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recalls` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `shipment_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `recalled_by` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `recalled_at` datetime NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_recall_shipment` (`shipment_id`),
  KEY `fk_recall_user` (`recalled_by`),
  CONSTRAINT `fk_recall_shipment` FOREIGN KEY (`shipment_id`) REFERENCES `shipments` (`id`),
  CONSTRAINT `fk_recall_user` FOREIGN KEY (`recalled_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `recalls`
--

LOCK TABLES `recalls` WRITE;
/*!40000 ALTER TABLE `recalls` DISABLE KEYS */;
/*!40000 ALTER TABLE `recalls` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `report_access_log`
--

DROP TABLE IF EXISTS `report_access_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_access_log` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `organization_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_organization_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `report_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `accessed_at` datetime NOT NULL,
  `success` tinyint(1) NOT NULL,
  `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_report_access_user` (`user_id`),
  KEY `fk_report_access_org` (`organization_id`),
  KEY `fk_report_access_target_org` (`target_organization_id`),
  CONSTRAINT `fk_report_access_org` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`organization_id`),
  CONSTRAINT `fk_report_access_target_org` FOREIGN KEY (`target_organization_id`) REFERENCES `organizations` (`organization_id`),
  CONSTRAINT `fk_report_access_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `report_access_log`
--

LOCK TABLES `report_access_log` WRITE;
/*!40000 ALTER TABLE `report_access_log` DISABLE KEYS */;
INSERT INTO `report_access_log` VALUES ('0961b07c-2acc-4e8e-a4f2-66dbac71574e','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','8f89a500-a1c4-11f1-9ae2-029fd41577b3','c1f7ed8d-a1d1-11f1-9ae2-029fd41577b3','YIELD_AND_LOT_DASHBOARD','2026-08-27 15:17:41',1,'172.18.0.1'),('1cdf5b61-b51b-423c-9059-3b524f6dfd09','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','YIELD_AND_LOT_DASHBOARD','2026-08-27 12:38:13',1,'0:0:0:0:0:0:0:1'),('3df11000-5dcf-4356-8079-f528da092e6c','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','8f89a500-a1c4-11f1-9ae2-029fd41577b3','c1f7d26c-a1d1-11f1-9ae2-029fd41577b3','YIELD_AND_LOT_DASHBOARD','2026-08-28 08:13:18',1,'172.18.0.1'),('5920b53f-82c5-44c8-8b9a-c37b823d9ccc','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','8f89a500-a1c4-11f1-9ae2-029fd41577b3','c1f7e18b-a1d1-11f1-9ae2-029fd41577b3','YIELD_AND_LOT_DASHBOARD','2026-08-28 08:13:20',1,'172.18.0.1'),('78ab71ac-1c32-491d-aac1-bd8e1e9fafe1','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','8f89a500-a1c4-11f1-9ae2-029fd41577b3','c1f7ee61-a1d1-11f1-9ae2-029fd41577b3','YIELD_AND_LOT_DASHBOARD','2026-08-28 08:13:29',1,'172.18.0.1'),('8f2986e4-7cbe-4f26-8958-3f54055662d5','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','8f89a500-a1c4-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','YIELD_AND_LOT_DASHBOARD','2026-08-28 08:13:35',1,'172.18.0.1'),('908a98eb-5e15-432d-9d27-1811a66be867','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','YIELD_AND_LOT_DASHBOARD','2026-08-28 01:26:50',1,'172.18.0.1'),('993f2f09-bab5-4471-b328-3c19da816ce7','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','8f89a500-a1c4-11f1-9ae2-029fd41577b3','8f89a500-a1c4-11f1-9ae2-029fd41577b3','YIELD_AND_LOT_DASHBOARD','2026-08-28 08:13:12',1,'172.18.0.1'),('9c9fa92f-5d12-4333-8308-7729565cab12','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','YIELD_AND_LOT_DASHBOARD','2026-08-27 12:59:04',1,'0:0:0:0:0:0:0:1'),('b1bdb6e1-e2b8-4e2b-afbf-318683e6e522','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','8f89a500-a1c4-11f1-9ae2-029fd41577b3','c1f7e8a7-a1d1-11f1-9ae2-029fd41577b3','YIELD_AND_LOT_DASHBOARD','2026-08-28 08:13:24',1,'172.18.0.1'),('b474deb7-4b3a-40dd-8c68-15d1addf9bdb','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','8f89a500-a1c4-11f1-9ae2-029fd41577b3','8f89a500-a1c4-11f1-9ae2-029fd41577b3','YIELD_AND_LOT_DASHBOARD','2026-08-27 22:14:18',1,'172.18.0.1'),('b6d9cff1-c7e9-4d0a-b917-891bd5aed34d','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','8f89a500-a1c4-11f1-9ae2-029fd41577b3','c1f7e47e-a1d1-11f1-9ae2-029fd41577b3','YIELD_AND_LOT_DASHBOARD','2026-08-28 08:13:22',1,'172.18.0.1'),('dc50d6dd-3f6f-46c5-8ce6-8922cedffbb6','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','8f89a500-a1c4-11f1-9ae2-029fd41577b3','8f89a500-a1c4-11f1-9ae2-029fd41577b3','YIELD_AND_LOT_DASHBOARD','2026-08-27 15:13:25',1,'172.18.0.1'),('f8e37e25-b775-4919-81a2-05583bd1793e','8f8a6000-a1c4-11f1-9ae2-029fd41577b3','8f89a500-a1c4-11f1-9ae2-029fd41577b3','c1f7e9d3-a1d1-11f1-9ae2-029fd41577b3','YIELD_AND_LOT_DASHBOARD','2026-08-28 08:13:28',1,'172.18.0.1');
/*!40000 ALTER TABLE `report_access_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role_permissions`
--

DROP TABLE IF EXISTS `role_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_permissions` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role_id` int NOT NULL,
  `permission_id` int NOT NULL,
  `is_enabled` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`,`permission_id`),
  KEY `idx_role_permission_role` (`role_id`),
  KEY `idx_role_permission_permission` (`permission_id`),
  CONSTRAINT `fk_role_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`permission_id`),
  CONSTRAINT `fk_role_permission_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role_permissions`
--

LOCK TABLES `role_permissions` WRITE;
/*!40000 ALTER TABLE `role_permissions` DISABLE KEYS */;
INSERT INTO `role_permissions` VALUES ('8f771962-a1c4-11f1-9ae2-029fd41577b3',1,46,1,'2026-08-27 03:08:21'),('8f771c3a-a1c4-11f1-9ae2-029fd41577b3',1,41,1,'2026-08-27 03:08:21'),('8f771c53-a1c4-11f1-9ae2-029fd41577b3',1,42,1,'2026-08-27 03:08:21'),('8f771c6e-a1c4-11f1-9ae2-029fd41577b3',1,25,1,'2026-08-27 03:08:21'),('8f771c7d-a1c4-11f1-9ae2-029fd41577b3',1,26,1,'2026-08-27 03:08:21'),('8f771c8c-a1c4-11f1-9ae2-029fd41577b3',1,27,1,'2026-08-27 03:08:21'),('8f771c9b-a1c4-11f1-9ae2-029fd41577b3',1,22,1,'2026-08-27 03:08:21'),('8f771caa-a1c4-11f1-9ae2-029fd41577b3',1,23,1,'2026-08-27 03:08:21'),('8f771cba-a1c4-11f1-9ae2-029fd41577b3',1,24,1,'2026-08-27 03:08:21'),('8f771cca-a1c4-11f1-9ae2-029fd41577b3',1,48,1,'2026-08-27 03:08:21'),('8f771cd9-a1c4-11f1-9ae2-029fd41577b3',1,49,1,'2026-08-27 03:08:21'),('8f771ce9-a1c4-11f1-9ae2-029fd41577b3',1,50,1,'2026-08-27 03:08:21'),('8f771cf6-a1c4-11f1-9ae2-029fd41577b3',1,54,1,'2026-08-27 03:08:21'),('8f771d05-a1c4-11f1-9ae2-029fd41577b3',1,55,1,'2026-08-27 03:08:21'),('8f771d15-a1c4-11f1-9ae2-029fd41577b3',1,3,1,'2026-08-27 03:08:21'),('8f771d23-a1c4-11f1-9ae2-029fd41577b3',1,6,1,'2026-08-27 03:08:21'),('8f771d30-a1c4-11f1-9ae2-029fd41577b3',1,4,1,'2026-08-27 03:08:21'),('8f771d3d-a1c4-11f1-9ae2-029fd41577b3',1,5,1,'2026-08-27 03:08:21'),('8f771d49-a1c4-11f1-9ae2-029fd41577b3',1,11,1,'2026-08-27 03:08:21'),('8f771d57-a1c4-11f1-9ae2-029fd41577b3',1,12,1,'2026-08-27 03:08:21'),('8f771d64-a1c4-11f1-9ae2-029fd41577b3',1,13,1,'2026-08-27 03:08:21'),('8f771d71-a1c4-11f1-9ae2-029fd41577b3',1,14,1,'2026-08-27 03:08:21'),('8f771d7f-a1c4-11f1-9ae2-029fd41577b3',1,40,1,'2026-08-27 03:08:21'),('8f771d8e-a1c4-11f1-9ae2-029fd41577b3',1,1,1,'2026-08-27 03:08:21'),('8f771d9c-a1c4-11f1-9ae2-029fd41577b3',1,2,1,'2026-08-27 03:08:21'),('8f771dab-a1c4-11f1-9ae2-029fd41577b3',1,34,1,'2026-08-27 03:08:21'),('8f771db8-a1c4-11f1-9ae2-029fd41577b3',1,37,1,'2026-08-27 03:08:21'),('8f771dc5-a1c4-11f1-9ae2-029fd41577b3',1,35,1,'2026-08-27 03:08:21'),('8f771dd2-a1c4-11f1-9ae2-029fd41577b3',1,36,1,'2026-08-27 03:08:21'),('8f771ddf-a1c4-11f1-9ae2-029fd41577b3',1,31,1,'2026-08-27 03:08:21'),('8f771dee-a1c4-11f1-9ae2-029fd41577b3',1,32,1,'2026-08-27 03:08:21'),('8f771dfb-a1c4-11f1-9ae2-029fd41577b3',1,33,1,'2026-08-27 03:08:21'),('8f771e09-a1c4-11f1-9ae2-029fd41577b3',1,47,1,'2026-08-27 03:08:21'),('8f771e16-a1c4-11f1-9ae2-029fd41577b3',1,10,1,'2026-08-27 03:08:21'),('8f771e23-a1c4-11f1-9ae2-029fd41577b3',1,7,1,'2026-08-27 03:08:21'),('8f771e30-a1c4-11f1-9ae2-029fd41577b3',1,8,1,'2026-08-27 03:08:21'),('8f771e3e-a1c4-11f1-9ae2-029fd41577b3',1,9,1,'2026-08-27 03:08:21'),('8f771e4a-a1c4-11f1-9ae2-029fd41577b3',1,52,1,'2026-08-27 03:08:21'),('8f771e57-a1c4-11f1-9ae2-029fd41577b3',1,53,1,'2026-08-27 03:08:21'),('8f771e64-a1c4-11f1-9ae2-029fd41577b3',1,44,1,'2026-08-27 03:08:21'),('8f771e71-a1c4-11f1-9ae2-029fd41577b3',1,43,1,'2026-08-27 03:08:21'),('8f771e7e-a1c4-11f1-9ae2-029fd41577b3',1,38,1,'2026-08-27 03:08:21'),('8f771e8b-a1c4-11f1-9ae2-029fd41577b3',1,39,1,'2026-08-27 03:08:21'),('8f771e98-a1c4-11f1-9ae2-029fd41577b3',1,45,1,'2026-08-27 03:08:21'),('8f771ea7-a1c4-11f1-9ae2-029fd41577b3',1,15,1,'2026-08-27 03:08:21'),('8f771eb4-a1c4-11f1-9ae2-029fd41577b3',1,18,1,'2026-08-27 03:08:21'),('8f771ec3-a1c4-11f1-9ae2-029fd41577b3',1,16,1,'2026-08-27 03:08:21'),('8f771ed0-a1c4-11f1-9ae2-029fd41577b3',1,17,1,'2026-08-27 03:08:21'),('8f771edd-a1c4-11f1-9ae2-029fd41577b3',1,28,1,'2026-08-27 03:08:21'),('8f771eea-a1c4-11f1-9ae2-029fd41577b3',1,29,1,'2026-08-27 03:08:21'),('8f771ef7-a1c4-11f1-9ae2-029fd41577b3',1,30,1,'2026-08-27 03:08:21'),('8f771f04-a1c4-11f1-9ae2-029fd41577b3',1,21,1,'2026-08-27 03:08:21'),('8f771f10-a1c4-11f1-9ae2-029fd41577b3',1,19,1,'2026-08-27 03:08:21'),('8f771f1d-a1c4-11f1-9ae2-029fd41577b3',1,20,1,'2026-08-27 03:08:21'),('8f771f2a-a1c4-11f1-9ae2-029fd41577b3',1,51,1,'2026-08-27 03:08:21'),('8f771f37-a1c4-11f1-9ae2-029fd41577b3',1,56,1,'2026-08-27 03:08:21'),('8f771f44-a1c4-11f1-9ae2-029fd41577b3',1,59,1,'2026-08-27 03:08:21'),('8f771f51-a1c4-11f1-9ae2-029fd41577b3',1,57,1,'2026-08-27 03:08:21'),('8f771f5d-a1c4-11f1-9ae2-029fd41577b3',1,58,1,'2026-08-27 03:08:21'),('8f7906f4-a1c4-11f1-9ae2-029fd41577b3',2,46,1,'2026-08-27 03:08:21'),('8f7907f6-a1c4-11f1-9ae2-029fd41577b3',2,41,1,'2026-08-27 03:08:21'),('8f790830-a1c4-11f1-9ae2-029fd41577b3',2,25,1,'2026-08-27 03:08:21'),('8f79086a-a1c4-11f1-9ae2-029fd41577b3',2,26,1,'2026-08-27 03:08:21'),('8f7908a3-a1c4-11f1-9ae2-029fd41577b3',2,27,1,'2026-08-27 03:08:21'),('8f7908d5-a1c4-11f1-9ae2-029fd41577b3',2,23,1,'2026-08-27 03:08:21'),('8f79090d-a1c4-11f1-9ae2-029fd41577b3',2,48,1,'2026-08-27 03:08:21'),('8f79093b-a1c4-11f1-9ae2-029fd41577b3',2,49,1,'2026-08-27 03:08:21'),('8f79096d-a1c4-11f1-9ae2-029fd41577b3',2,50,1,'2026-08-27 03:08:21'),('8f79099b-a1c4-11f1-9ae2-029fd41577b3',2,54,1,'2026-08-27 03:08:21'),('8f7909c4-a1c4-11f1-9ae2-029fd41577b3',2,55,1,'2026-08-27 03:08:21'),('8f7909f4-a1c4-11f1-9ae2-029fd41577b3',2,3,1,'2026-08-27 03:08:21'),('8f790a2a-a1c4-11f1-9ae2-029fd41577b3',2,6,1,'2026-08-27 03:08:21'),('8f790a5b-a1c4-11f1-9ae2-029fd41577b3',2,4,1,'2026-08-27 03:08:21'),('8f790c2b-a1c4-11f1-9ae2-029fd41577b3',2,5,1,'2026-08-27 03:08:21'),('8f790c7b-a1c4-11f1-9ae2-029fd41577b3',2,12,1,'2026-08-27 03:08:21'),('8f790cae-a1c4-11f1-9ae2-029fd41577b3',2,14,1,'2026-08-27 03:08:21'),('8f790ea2-a1c4-11f1-9ae2-029fd41577b3',2,40,1,'2026-08-27 03:08:21'),('8f790f06-a1c4-11f1-9ae2-029fd41577b3',2,1,1,'2026-08-27 03:08:21'),('8f790f4f-a1c4-11f1-9ae2-029fd41577b3',2,2,1,'2026-08-27 03:08:21'),('8f790f8d-a1c4-11f1-9ae2-029fd41577b3',2,34,1,'2026-08-27 03:08:21'),('8f790fc7-a1c4-11f1-9ae2-029fd41577b3',2,37,1,'2026-08-27 03:08:21'),('8f790fff-a1c4-11f1-9ae2-029fd41577b3',2,35,1,'2026-08-27 03:08:21'),('8f79103c-a1c4-11f1-9ae2-029fd41577b3',2,36,1,'2026-08-27 03:08:21'),('8f79106f-a1c4-11f1-9ae2-029fd41577b3',2,32,1,'2026-08-27 03:08:21'),('8f7910a1-a1c4-11f1-9ae2-029fd41577b3',2,47,1,'2026-08-27 03:08:21'),('8f7910d9-a1c4-11f1-9ae2-029fd41577b3',2,10,1,'2026-08-27 03:08:21'),('8f79110d-a1c4-11f1-9ae2-029fd41577b3',2,7,1,'2026-08-27 03:08:21'),('8f79113e-a1c4-11f1-9ae2-029fd41577b3',2,8,1,'2026-08-27 03:08:21'),('8f791175-a1c4-11f1-9ae2-029fd41577b3',2,9,1,'2026-08-27 03:08:21'),('8f79148e-a1c4-11f1-9ae2-029fd41577b3',2,52,1,'2026-08-27 03:08:21'),('8f7914eb-a1c4-11f1-9ae2-029fd41577b3',2,53,1,'2026-08-27 03:08:21'),('8f791518-a1c4-11f1-9ae2-029fd41577b3',2,44,1,'2026-08-27 03:08:21'),('8f791542-a1c4-11f1-9ae2-029fd41577b3',2,43,1,'2026-08-27 03:08:21'),('8f79157a-a1c4-11f1-9ae2-029fd41577b3',2,38,1,'2026-08-27 03:08:21'),('8f7915ac-a1c4-11f1-9ae2-029fd41577b3',2,39,1,'2026-08-27 03:08:21'),('8f7915d7-a1c4-11f1-9ae2-029fd41577b3',2,45,1,'2026-08-27 03:08:21'),('8f791602-a1c4-11f1-9ae2-029fd41577b3',2,15,1,'2026-08-27 03:08:21'),('8f79162d-a1c4-11f1-9ae2-029fd41577b3',2,18,1,'2026-08-27 03:08:21'),('8f791659-a1c4-11f1-9ae2-029fd41577b3',2,16,1,'2026-08-27 03:08:21'),('8f791684-a1c4-11f1-9ae2-029fd41577b3',2,17,1,'2026-08-27 03:08:21'),('8f7916aa-a1c4-11f1-9ae2-029fd41577b3',2,29,1,'2026-08-27 03:08:21'),('8f7916d5-a1c4-11f1-9ae2-029fd41577b3',2,20,1,'2026-08-27 03:08:21'),('8f791701-a1c4-11f1-9ae2-029fd41577b3',2,51,1,'2026-08-27 03:08:21'),('8f79fa70-a1c4-11f1-9ae2-029fd41577b3',3,22,1,'2026-08-27 03:08:21'),('8f79fac1-a1c4-11f1-9ae2-029fd41577b3',3,23,1,'2026-08-27 03:08:21'),('8f79faef-a1c4-11f1-9ae2-029fd41577b3',3,4,1,'2026-08-27 03:08:21'),('8f79fb1a-a1c4-11f1-9ae2-029fd41577b3',3,11,1,'2026-08-27 03:08:21'),('8f79fb45-a1c4-11f1-9ae2-029fd41577b3',3,12,1,'2026-08-27 03:08:21'),('8f79fb6e-a1c4-11f1-9ae2-029fd41577b3',3,13,1,'2026-08-27 03:08:21'),('8f79fb9c-a1c4-11f1-9ae2-029fd41577b3',3,40,1,'2026-08-27 03:08:21'),('8f79fbcb-a1c4-11f1-9ae2-029fd41577b3',3,8,1,'2026-08-27 03:08:21'),('8f79fbf2-a1c4-11f1-9ae2-029fd41577b3',3,16,1,'2026-08-27 03:08:21'),('8f79fc19-a1c4-11f1-9ae2-029fd41577b3',3,20,1,'2026-08-27 03:08:21'),('8f7af287-a1c4-11f1-9ae2-029fd41577b3',4,22,1,'2026-08-27 03:08:21'),('8f7af485-a1c4-11f1-9ae2-029fd41577b3',4,23,1,'2026-08-27 03:08:21'),('8f7af504-a1c4-11f1-9ae2-029fd41577b3',4,12,1,'2026-08-27 03:08:21'),('8f7af59c-a1c4-11f1-9ae2-029fd41577b3',4,40,1,'2026-08-27 03:08:21'),('8f7af63c-a1c4-11f1-9ae2-029fd41577b3',4,47,1,'2026-08-27 03:08:21'),('8f7af69c-a1c4-11f1-9ae2-029fd41577b3',4,8,1,'2026-08-27 03:08:21'),('8f7af6d3-a1c4-11f1-9ae2-029fd41577b3',4,16,1,'2026-08-27 03:08:21'),('8f7af714-a1c4-11f1-9ae2-029fd41577b3',4,21,1,'2026-08-27 03:08:21'),('8f7af76a-a1c4-11f1-9ae2-029fd41577b3',4,20,1,'2026-08-27 03:08:21'),('8f7af79f-a1c4-11f1-9ae2-029fd41577b3',4,51,1,'2026-08-27 03:08:21'),('8f7be932-a1c4-11f1-9ae2-029fd41577b3',5,46,1,'2026-08-27 03:08:21'),('8f7be980-a1c4-11f1-9ae2-029fd41577b3',5,41,1,'2026-08-27 03:08:21'),('8f7be9af-a1c4-11f1-9ae2-029fd41577b3',5,26,1,'2026-08-27 03:08:21'),('8f7be9dc-a1c4-11f1-9ae2-029fd41577b3',5,23,1,'2026-08-27 03:08:21'),('8f7bea06-a1c4-11f1-9ae2-029fd41577b3',5,4,1,'2026-08-27 03:08:21'),('8f7bea30-a1c4-11f1-9ae2-029fd41577b3',5,12,1,'2026-08-27 03:08:21'),('8f7bea58-a1c4-11f1-9ae2-029fd41577b3',5,40,1,'2026-08-27 03:08:21'),('8f7bea80-a1c4-11f1-9ae2-029fd41577b3',5,1,1,'2026-08-27 03:08:21'),('8f7beaab-a1c4-11f1-9ae2-029fd41577b3',5,32,1,'2026-08-27 03:08:21'),('8f7bead1-a1c4-11f1-9ae2-029fd41577b3',5,47,1,'2026-08-27 03:08:21'),('8f7beafe-a1c4-11f1-9ae2-029fd41577b3',5,8,1,'2026-08-27 03:08:21'),('8f7beb24-a1c4-11f1-9ae2-029fd41577b3',5,53,1,'2026-08-27 03:08:21'),('8f7beb48-a1c4-11f1-9ae2-029fd41577b3',5,44,1,'2026-08-27 03:08:21'),('8f7beb6d-a1c4-11f1-9ae2-029fd41577b3',5,43,1,'2026-08-27 03:08:21'),('8f7beb91-a1c4-11f1-9ae2-029fd41577b3',5,45,1,'2026-08-27 03:08:21'),('8f7bebb5-a1c4-11f1-9ae2-029fd41577b3',5,16,1,'2026-08-27 03:08:21'),('8f7bebd8-a1c4-11f1-9ae2-029fd41577b3',5,29,1,'2026-08-27 03:08:21'),('8f7bebfb-a1c4-11f1-9ae2-029fd41577b3',5,20,1,'2026-08-27 03:08:21'),('8f7bec1f-a1c4-11f1-9ae2-029fd41577b3',5,51,1,'2026-08-27 03:08:21'),('8f7cd4d8-a1c4-11f1-9ae2-029fd41577b3',6,47,1,'2026-08-27 03:08:21'),('8f7cd52f-a1c4-11f1-9ae2-029fd41577b3',6,51,1,'2026-08-27 03:08:21'),('921d49b0-a1c4-11f1-9ae2-029fd41577b3',1,60,1,'2026-08-27 03:08:26'),('921d4a75-a1c4-11f1-9ae2-029fd41577b3',1,63,1,'2026-08-27 03:08:26'),('921d4ae5-a1c4-11f1-9ae2-029fd41577b3',1,61,1,'2026-08-27 03:08:26'),('921d4b46-a1c4-11f1-9ae2-029fd41577b3',1,62,1,'2026-08-27 03:08:26'),('921e1fab-a1c4-11f1-9ae2-029fd41577b3',2,61,1,'2026-08-27 03:08:26'),('921e2020-a1c4-11f1-9ae2-029fd41577b3',3,61,1,'2026-08-27 03:08:26'),('921e2073-a1c4-11f1-9ae2-029fd41577b3',4,61,1,'2026-08-27 03:08:26'),('921e20c0-a1c4-11f1-9ae2-029fd41577b3',5,61,1,'2026-08-27 03:08:26');
/*!40000 ALTER TABLE `role_permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `role_id` int NOT NULL AUTO_INCREMENT,
  `code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `code` (`code`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES (1,'VT-01','ADMIN'),(2,'VT-02','ORG_MANAGER'),(3,'VT-03','EVENT_RECORDER'),(4,'VT-04','PROCUREMENT'),(5,'VT-05','REGULATOR'),(6,'VT-06','CONSUMER');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shipments`
--

DROP TABLE IF EXISTS `shipments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shipments` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `production_lot_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `organization_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_quantity` bigint NOT NULL,
  `packaging_info` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_by` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `code_range_id` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_shipment_production_lot` (`production_lot_id`),
  KEY `fk_shipment_organization` (`organization_id`),
  KEY `fk_shipment_created_by` (`created_by`),
  KEY `fk_shipment_code_range` (`code_range_id`),
  CONSTRAINT `fk_shipment_code_range` FOREIGN KEY (`code_range_id`) REFERENCES `code_ranges` (`id`),
  CONSTRAINT `fk_shipment_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_shipment_organization` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`organization_id`),
  CONSTRAINT `fk_shipment_production_lot` FOREIGN KEY (`production_lot_id`) REFERENCES `production_lot` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shipments`
--

LOCK TABLES `shipments` WRITE;
/*!40000 ALTER TABLE `shipments` DISABLE KEYS */;
INSERT INTO `shipments` VALUES ('d0474cbe-a1d1-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED_TEST Lo hang 01',60,'Thung 20kg x 1','CODE_PRINTED','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-08 04:43:13','2026-08-08 04:43:13',NULL),('d0479242-a1d1-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED_TEST Lo hang 02',70,'Thung 20kg x 2','ACTIVATED','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-09 04:43:13','2026-08-09 04:43:13',NULL),('d04797cc-a1d1-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED_TEST Lo hang 03',80,'Thung 20kg x 3','RECALLED','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-10 04:43:13','2026-08-10 04:43:13',NULL),('d04799b2-a1d1-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED_TEST Lo hang 04',90,'Thung 20kg x 4','DRAFT','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-11 04:43:13','2026-08-11 04:43:13',NULL),('d0479d0f-a1d1-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED_TEST Lo hang 05',100,'Thung 20kg x 5','CODE_PRINTED','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-12 04:43:13','2026-08-12 04:43:13',NULL),('d0479ec2-a1d1-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED_TEST Lo hang 06',110,'Thung 20kg x 6','ACTIVATED','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-13 04:43:13','2026-08-13 04:43:13',NULL),('d047a006-a1d1-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED_TEST Lo hang 07',120,'Thung 20kg x 7','RECALLED','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-14 04:43:13','2026-08-14 04:43:13',NULL),('d047a15c-a1d1-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED_TEST Lo hang 08',130,'Thung 20kg x 8','DRAFT','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-15 04:43:13','2026-08-15 04:43:13',NULL),('d047aa16-a1d1-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED_TEST Lo hang 09',140,'Thung 20kg x 9','CODE_PRINTED','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-16 04:43:13','2026-08-16 04:43:13',NULL),('d047abb3-a1d1-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED_TEST Lo hang 10',150,'Thung 20kg x 10','ACTIVATED','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-17 04:43:13','2026-08-17 04:43:13',NULL),('d047adbd-a1d1-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED_TEST Lo hang 11',160,'Thung 20kg x 11','RECALLED','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-18 04:43:13','2026-08-18 04:43:13',NULL),('d047b2e5-a1d1-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED_TEST Lo hang 12',170,'Thung 20kg x 12','DRAFT','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-19 04:43:13','2026-08-19 04:43:13',NULL),('d047b483-a1d1-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED_TEST Lo hang 13',180,'Thung 20kg x 13','CODE_PRINTED','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-20 04:43:13','2026-08-20 04:43:13',NULL),('d047b5c2-a1d1-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED_TEST Lo hang 14',190,'Thung 20kg x 14','ACTIVATED','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-21 04:43:13','2026-08-21 04:43:13',NULL),('d047b6d4-a1d1-11f1-9ae2-029fd41577b3','dd2d97bc-a1c6-11f1-9ae2-029fd41577b3','d74c09f2-a1c6-11f1-9ae2-029fd41577b3','SEED_TEST Lo hang 15',200,'Thung 20kg x 15','RECALLED','bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','2026-08-22 04:43:13','2026-08-22 04:43:13',NULL);
/*!40000 ALTER TABLE `shipments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `standards`
--

DROP TABLE IF EXISTS `standards`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `standards` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `issuing_body` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `standards`
--

LOCK TABLES `standards` WRITE;
/*!40000 ALTER TABLE `standards` DISABLE KEYS */;
INSERT INTO `standards` VALUES ('c1ec1e7c-a1d1-11f1-9ae2-029fd41577b3','SEED_TEST Tieu chuan bo sung 01','Tieu chuan test','Tong cuc test',1,'2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1ec32ec-a1d1-11f1-9ae2-029fd41577b3','SEED_TEST Tieu chuan bo sung 02','Tieu chuan test','Tong cuc test',1,'2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1ec38a8-a1d1-11f1-9ae2-029fd41577b3','SEED_TEST Tieu chuan bo sung 03','Tieu chuan test','Tong cuc test',1,'2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1ec3b6e-a1d1-11f1-9ae2-029fd41577b3','SEED_TEST Tieu chuan bo sung 04','Tieu chuan test','Tong cuc test',1,'2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1ec3d9b-a1d1-11f1-9ae2-029fd41577b3','SEED_TEST Tieu chuan bo sung 05','Tieu chuan test','Tong cuc test',1,'2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1ec3f85-a1d1-11f1-9ae2-029fd41577b3','SEED_TEST Tieu chuan bo sung 06','Tieu chuan test','Tong cuc test',1,'2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1ec494f-a1d1-11f1-9ae2-029fd41577b3','SEED_TEST Tieu chuan bo sung 07','Tieu chuan test','Tong cuc test',1,'2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1ec7468-a1d1-11f1-9ae2-029fd41577b3','SEED_TEST Tieu chuan bo sung 08','Tieu chuan test','Tong cuc test',1,'2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1ec97e3-a1d1-11f1-9ae2-029fd41577b3','SEED_TEST Tieu chuan bo sung 09','Tieu chuan test','Tong cuc test',1,'2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1ecca36-a1d1-11f1-9ae2-029fd41577b3','SEED_TEST Tieu chuan bo sung 10','Tieu chuan test','Tong cuc test',1,'2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1ecd1da-a1d1-11f1-9ae2-029fd41577b3','SEED_TEST Tieu chuan bo sung 11','Tieu chuan test','Tong cuc test',1,'2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1ecd400-a1d1-11f1-9ae2-029fd41577b3','SEED_TEST Tieu chuan bo sung 12','Tieu chuan test','Tong cuc test',1,'2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1ecd557-a1d1-11f1-9ae2-029fd41577b3','SEED_TEST Tieu chuan bo sung 13','Tieu chuan test','Tong cuc test',1,'2026-08-27 04:42:49','2026-08-27 04:42:49'),('e13530dc-a1c6-11f1-9ae2-029fd41577b3','SEED_TEST Ti??u chu???n ch???t l?????ng rau s???ch','Ti??u chu???n test','T???ng c???c test',1,'2026-08-27 03:24:57','2026-08-27 03:24:57'),('e13534f8-a1c6-11f1-9ae2-029fd41577b3','SEED_TEST Ti??u chu???n GlobalGAP','Ti??u chu???n test 2','GlobalGAP',1,'2026-08-27 03:24:57','2026-08-27 03:24:57');
/*!40000 ALTER TABLE `standards` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `suspicious_cases`
--

DROP TABLE IF EXISTS `suspicious_cases`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `suspicious_cases` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `organization_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPEN',
  `anomaly_count` int NOT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `resolved_at` timestamp(3) NULL DEFAULT NULL,
  `first_detected_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `last_detected_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_suspicious_cases_org_last_detected` (`organization_id`,`last_detected_at` DESC),
  KEY `idx_suspicious_cases_user_last_detected` (`user_id`,`last_detected_at` DESC),
  KEY `idx_suspicious_cases_status` (`status`),
  CONSTRAINT `fk_suspicious_cases_organization` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`organization_id`),
  CONSTRAINT `fk_suspicious_cases_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `suspicious_cases`
--

LOCK TABLES `suspicious_cases` WRITE;
/*!40000 ALTER TABLE `suspicious_cases` DISABLE KEYS */;
/*!40000 ALTER TABLE `suspicious_cases` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `trace_code_scan_logs`
--

DROP TABLE IF EXISTS `trace_code_scan_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trace_code_scan_logs` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `trace_code_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `scanned_at` datetime NOT NULL,
  `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_agent` text COLLATE utf8mb4_unicode_ci,
  `latitude` decimal(10,7) DEFAULT NULL,
  `longitude` decimal(10,7) DEFAULT NULL,
  `location` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_abnormal` tinyint(1) NOT NULL DEFAULT '0',
  `abnormal_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_scan_log_trace_code` (`trace_code_id`),
  KEY `idx_scan_log_scanned_at` (`scanned_at`),
  CONSTRAINT `fk_scan_log_trace_code` FOREIGN KEY (`trace_code_id`) REFERENCES `trace_codes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `trace_code_scan_logs`
--

LOCK TABLES `trace_code_scan_logs` WRITE;
/*!40000 ALTER TABLE `trace_code_scan_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `trace_code_scan_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `trace_codes`
--

DROP TABLE IF EXISTS `trace_codes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trace_codes` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `shipment_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `code_value` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `qr_image` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `activated_at` datetime DEFAULT NULL,
  `activated_by` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `suspicion_score` int DEFAULT '0',
  `suspicion_reason` text COLLATE utf8mb4_unicode_ci,
  `locked_at` timestamp NULL DEFAULT NULL,
  `locked_by` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lock_reason` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code_value` (`code_value`),
  KEY `fk_trace_code_shipment` (`shipment_id`),
  KEY `fk_trace_code_activated_by` (`activated_by`),
  KEY `idx_trace_codes_status` (`status`),
  KEY `idx_trace_codes_suspicion_score` (`suspicion_score`),
  KEY `fk_trace_codes_locked_by` (`locked_by`),
  CONSTRAINT `fk_trace_code_activated_by` FOREIGN KEY (`activated_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_trace_code_shipment` FOREIGN KEY (`shipment_id`) REFERENCES `shipments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_trace_codes_locked_by` FOREIGN KEY (`locked_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `trace_codes`
--

LOCK TABLES `trace_codes` WRITE;
/*!40000 ALTER TABLE `trace_codes` DISABLE KEYS */;
/*!40000 ALTER TABLE `trace_codes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `user_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `full_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `user_name` (`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES ('8f8a6000-a1c4-11f1-9ae2-029fd41577b3','admin','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','Quản trị viên hệ thống',NULL,NULL,'ACTIVE','2026-08-27 03:08:21','2026-08-27 03:08:21'),('bd6a2b4d-a1c4-11f1-9ae2-029fd41577b3','manager','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','Qu???n l?? HTX ABC','0913000001','manager@abc.test','ACTIVE','2026-08-27 03:09:38','2026-08-27 03:09:38'),('bd6a2fa0-a1c4-11f1-9ae2-029fd41577b3','nguoighi','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','Ng?????i ghi s??? ki???n ABC','0913000002','rec@abc.test','ACTIVE','2026-08-27 03:09:38','2026-08-27 03:09:38'),('c1f0bec8-a1d1-11f1-9ae2-029fd41577b3','member_01','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','Thanh vien test 01','0912000001','member01@abc.test','ACTIVE','2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1f0bf7b-a1d1-11f1-9ae2-029fd41577b3','member_02','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','Thanh vien test 02','0912000002','member02@abc.test','ACTIVE','2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1f0bf92-a1d1-11f1-9ae2-029fd41577b3','member_03','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','Thanh vien test 03','0912000003','member03@abc.test','ACTIVE','2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1f0bfa5-a1d1-11f1-9ae2-029fd41577b3','member_04','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','Thanh vien test 04','0912000004','member04@abc.test','ACTIVE','2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1f0bfb6-a1d1-11f1-9ae2-029fd41577b3','member_05','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','Thanh vien test 05','0912000005','member05@abc.test','ACTIVE','2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1f0bfc7-a1d1-11f1-9ae2-029fd41577b3','member_06','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','Thanh vien test 06','0912000006','member06@abc.test','ACTIVE','2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1f0bfd7-a1d1-11f1-9ae2-029fd41577b3','member_07','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','Thanh vien test 07','0912000007','member07@abc.test','ACTIVE','2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1f0bfe7-a1d1-11f1-9ae2-029fd41577b3','member_08','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','Thanh vien test 08','0912000008','member08@abc.test','ACTIVE','2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1f0bff7-a1d1-11f1-9ae2-029fd41577b3','member_09','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','Thanh vien test 09','0912000009','member09@abc.test','ACTIVE','2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1f0c007-a1d1-11f1-9ae2-029fd41577b3','member_10','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','Thanh vien test 10','0912000010','member10@abc.test','ACTIVE','2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1f0c023-a1d1-11f1-9ae2-029fd41577b3','member_11','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','Thanh vien test 11','0912000011','member11@abc.test','ACTIVE','2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1f0c034-a1d1-11f1-9ae2-029fd41577b3','member_12','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','Thanh vien test 12','0912000012','member12@abc.test','ACTIVE','2026-08-27 04:42:49','2026-08-27 04:42:49'),('c1f0c044-a1d1-11f1-9ae2-029fd41577b3','member_13','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','Thanh vien test 13','0912000013','member13@abc.test','ACTIVE','2026-08-27 04:42:49','2026-08-27 04:42:49');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'nguon_goc_so'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-28  3:28:15
