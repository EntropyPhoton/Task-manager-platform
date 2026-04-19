-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: localhost    Database: task_manager
-- ------------------------------------------------------
-- Server version	8.0.42

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
-- Table structure for table `task_tags`
--

DROP TABLE IF EXISTS `task_tags`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_tags` (
  `task_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tag` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  KEY `FK7xi1reghkj37gqwlr1ujxrxll` (`task_id`),
  CONSTRAINT `FK7xi1reghkj37gqwlr1ujxrxll` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `task_tags`
--

LOCK TABLES `task_tags` WRITE;
/*!40000 ALTER TABLE `task_tags` DISABLE KEYS */;
INSERT INTO `task_tags` VALUES ('79c8febb','生活'),('79c8febb','购物'),('4e0b29ac','调研'),('4e0b29ac','沟通'),('5524b2d1','调研'),('5524b2d1','规划'),('0798a0f3','准备'),('af4f0fc5','规划'),('cceedc51','执行'),('8a27399c','执行'),('8a27399c','沟通'),('51c0c823','规划'),('51c0c823','执行');
/*!40000 ALTER TABLE `task_tags` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tasks`
--

DROP TABLE IF EXISTS `tasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tasks` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `priority` enum('high','low','medium') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('completed','in_progress','pending') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tasks`
--

LOCK TABLES `tasks` WRITE;
/*!40000 ALTER TABLE `tasks` DISABLE KEYS */;
INSERT INTO `tasks` VALUES ('0798a0f3','2026-04-19 11:19:40.693800','整理身份证、医保卡、过往病历、检查报告、现金或支付工具、水杯等必需品。','high','pending','准备就诊物品','2026-04-19 11:19:40.693800'),('4e0b29ac','2026-04-19 11:19:40.621385','核对医院地址、科室、预约时间、医生姓名以及需要携带的证件和病历资料。','high','pending','确认就诊信息','2026-04-19 11:19:40.621385'),('51c0c823','2026-04-19 11:19:40.732312','根据看诊结束时间规划返程，并整理医嘱、药品，安排复查或治疗计划。','low','pending','安排返程与后续','2026-04-19 11:19:40.732312'),('5524b2d1','2026-04-19 11:19:40.682790','根据出发地点，查询并确定前往上海六院的交通方式、换乘方案及预估时间。','high','pending','规划出行路线','2026-04-19 11:19:40.682790'),('79c8febb','2026-04-19 10:15:37.981225','明天上午9点，嘉陵路菜场','medium','pending','买菜','2026-04-19 10:15:37.981225'),('8a27399c','2026-04-19 11:19:40.723309','抵达后完成取号、候诊、看诊、缴费、检查或取药等一系列院内步骤。','low','pending','完成院内流程','2026-04-19 11:19:40.723309'),('af4f0fc5','2026-04-19 11:19:40.703803','根据预约时间和交通状况，设定起床、早餐及出发的闹钟，预留充足缓冲。','medium','pending','安排出发时间','2026-04-19 11:19:40.703803'),('cceedc51','2026-04-19 11:19:40.713805','按照规划路线准时出发前往医院，途中注意交通状况并及时调整。','medium','pending','执行出行计划','2026-04-19 11:19:40.713805');
/*!40000 ALTER TABLE `tasks` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-19 19:53:48
