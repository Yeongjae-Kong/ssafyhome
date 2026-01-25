-- 관심 아파트(사용자별) 테이블 - 최근 거래가/거래월 포함
CREATE TABLE IF NOT EXISTS `ssafy_home`.`favorite_apartments` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `apt_seq` VARCHAR(20) NOT NULL,
  `apt_name` VARCHAR(100) NOT NULL,
  `sido` VARCHAR(30) NOT NULL,
  `gugun` VARCHAR(30) NOT NULL,
  `dong` VARCHAR(30) NOT NULL,
  `last_deal_amount_krw` BIGINT NULL,
  `last_deal_month` CHAR(6) NULL,
  `last_deal_area_pyeong` DECIMAL(7,2) NULL COMMENT '최근 거래 전용면적(평)',
  `build_year` INT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fav_user_apt` (`user_id`,`apt_seq`),
  KEY `idx_fav_user_updated` (`user_id`,`updated_at`),
  CONSTRAINT `fk_favorite_apartments_user` FOREIGN KEY (`user_id`) REFERENCES `ssafy_home`.`user`(`mno`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='관심 아파트';
