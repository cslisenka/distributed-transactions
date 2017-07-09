CREATE DATABASE  IF NOT EXISTS `partner_bank` /*!40100 DEFAULT CHARACTER SET utf8 */;
USE `partner_bank`;
-- MySQL dump 10.13  Distrib 5.7.9, for Win64 (x86_64)
--
-- Host: localhost    Database: partner_bank
-- ------------------------------------------------------
-- Server version	5.7.12-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `account`
--

DROP TABLE IF EXISTS `account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `account` (
  `identifier` varchar(10) NOT NULL,
  `balance` int(11) NOT NULL,
  PRIMARY KEY (`identifier`),
  UNIQUE KEY `identifier_UNIQUE` (`identifier`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `account`
--

LOCK TABLES `account` WRITE;
/*!40000 ALTER TABLE `account` DISABLE KEYS */;
INSERT INTO `account` VALUES ('acc1',800),('acc2',5200),('acc3',10000);
/*!40000 ALTER TABLE `account` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `money_transfer`
--

DROP TABLE IF EXISTS `money_transfer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `money_transfer` (
  `transfer_id` int(11) NOT NULL AUTO_INCREMENT,
  `from_account` varchar(10) NOT NULL,
  `to_account` varchar(10) NOT NULL,
  `amount` int(11) NOT NULL,
  PRIMARY KEY (`transfer_id`),
  UNIQUE KEY `transfer_id_UNIQUE` (`transfer_id`),
  KEY `from_account_idx` (`from_account`),
  KEY `to_account_idx` (`to_account`),
  CONSTRAINT `from_account` FOREIGN KEY (`from_account`) REFERENCES `account` (`identifier`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `to_account` FOREIGN KEY (`to_account`) REFERENCES `account` (`identifier`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `money_transfer`
--

LOCK TABLES `money_transfer` WRITE;
/*!40000 ALTER TABLE `money_transfer` DISABLE KEYS */;
INSERT INTO `money_transfer` VALUES (2,'acc1','acc2',100),(3,'acc1','acc2',100),(4,'acc1','acc2',100);
/*!40000 ALTER TABLE `money_transfer` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2017-07-08 21:50:18
