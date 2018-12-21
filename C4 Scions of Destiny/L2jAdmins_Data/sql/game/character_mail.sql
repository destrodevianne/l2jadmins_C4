CREATE TABLE IF NOT EXISTS `character_mail` (
  `charId` int(10) NOT NULL,
  `letterId` int(10),
  `senderId` int(10) NOT NULL,
  `location` varchar(45) NOT NULL,
  `recipientNames` varchar(200) DEFAULT NULL,
  `subject` varchar(128) DEFAULT NULL,
  `message` varchar(3000) DEFAULT NULL,
  `sentDate` timestamp NULL DEFAULT NULL,
  `unread` smallint(1) DEFAULT 1,
  PRIMARY KEY  (`letterId`)
);