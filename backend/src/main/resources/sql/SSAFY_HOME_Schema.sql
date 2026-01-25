-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema ssafy_home
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `ssafy_home` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci ;
USE `ssafy_home` ;

-- -----------------------------------------------------
-- Table `ssafy_home`.`dongcodes`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `ssafy_home`.`dongcodes` ;

CREATE TABLE IF NOT EXISTS `ssafy_home`.`dongcodes` (
  `dong_code` VARCHAR(10) NOT NULL comment '법정동코드',
  `sido_name` VARCHAR(30) NULL DEFAULT NULL comment '시도이름',
  `gugun_name` VARCHAR(30) NULL DEFAULT NULL comment '구군이름',
  `dong_name` VARCHAR(30) NULL DEFAULT NULL comment '동이름',
  PRIMARY KEY (`dong_code`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci
comment '법정동코드테이블';


-- -----------------------------------------------------
-- Table `ssafy_home`.`houseinfos`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `ssafy_home`.`houseinfos` ;

CREATE TABLE IF NOT EXISTS `ssafy_home`.`houseinfos` (
  `apt_seq` VARCHAR(20) NOT NULL comment '아파트코드',
  `sgg_cd` VARCHAR(5) NULL DEFAULT NULL comment '시군구코드',
  `umd_cd` VARCHAR(5) NULL DEFAULT NULL comment '읍면동코드',
  `umd_nm` VARCHAR(20) NULL DEFAULT NULL comment '읍면동이름',
  `jibun` VARCHAR(10) NULL DEFAULT NULL comment '지번',
  `road_nm_sgg_cd` VARCHAR(5) NULL DEFAULT NULL comment '도로명시군구코드',
  `road_nm` VARCHAR(20) NULL DEFAULT NULL comment '도로명',
  `road_nm_bonbun` VARCHAR(10) NULL DEFAULT NULL comment '도로명기초번호',
  `road_nm_bubun` VARCHAR(10) NULL DEFAULT NULL comment '도로명추가번호',
  `apt_nm` VARCHAR(40) NULL DEFAULT NULL comment '아파트이름',
  `build_year` INT NULL DEFAULT NULL comment '준공년도',
  `latitude` VARCHAR(45) NULL DEFAULT NULL comment '위도',
  `longitude` VARCHAR(45) NULL DEFAULT NULL comment '경도',
  PRIMARY KEY (`apt_seq`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci
comment '주택정보테이블';


-- -----------------------------------------------------
-- Table `ssafy_home`.`housedeals`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `ssafy_home`.`housedeals` ;

CREATE TABLE IF NOT EXISTS `ssafy_home`.`housedeals` (
  `no` INT NOT NULL AUTO_INCREMENT comment '거래번호',
  `apt_seq` VARCHAR(20) NULL DEFAULT NULL comment '아파트코드',
  `apt_dong` VARCHAR(40) NULL DEFAULT NULL comment '아파트동',
  `floor` VARCHAR(3) NULL DEFAULT NULL comment '아파트층',
  `deal_year` INT NULL DEFAULT NULL comment '거래년도',
  `deal_month` INT NULL DEFAULT NULL comment '거래월',
  `deal_day` INT NULL DEFAULT NULL comment '거래일',
  `exclu_use_ar` DECIMAL(7,2) NULL DEFAULT NULL  comment '아파트면적',
  `deal_amount` VARCHAR(10) NULL DEFAULT NULL  comment '거래가격',
  PRIMARY KEY (`no`),
  INDEX `apt_seq_to_house_info_idx` (`apt_seq` ASC) VISIBLE,
  CONSTRAINT `apt_seq_to_house_info`
    FOREIGN KEY (`apt_seq`)
    REFERENCES `ssafy_home`.`houseinfos` (`apt_seq`))
ENGINE = InnoDB
AUTO_INCREMENT = 7084512
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci
comment '주택거래정보테이블';


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;

-- -----------------------------------------------------
-- 아래부터는 애플리케이션 사용을 위해 추가된 테이블 정의입니다.
-- 기존 스키마는 건드리지 않고, 필요한 객체만 하단에 생성합니다.
-- -----------------------------------------------------

-- 사용자 계정 테이블 (MyBatis UserDao에서 사용)
-- 참고: 테이블명이 MySQL 예약어와 충돌할 수 있어 백틱(`)으로 감쌉니다.
CREATE TABLE IF NOT EXISTS `ssafy_home`.`user` (
  `mno` INT NOT NULL AUTO_INCREMENT COMMENT '사용자 번호',
  `name` VARCHAR(100) NOT NULL COMMENT '이름',
  `email` VARCHAR(255) NOT NULL COMMENT '이메일',
  `password` VARCHAR(255) NOT NULL COMMENT '비밀번호(해시 저장 권장)',
  `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '역할',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`mno`),
  UNIQUE KEY `uk_user_email` (`email`)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT='사용자 계정';

-- Refresh Token 테이블 (JWT 재발급 관리)
CREATE TABLE IF NOT EXISTS `ssafy_home`.`refresh_tokens` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `user_id` INT NOT NULL COMMENT 'user.mno 참조',
  `token` VARCHAR(512) NOT NULL COMMENT '원문 토큰(운영에선 해시 저장 권장)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '발급 시각',
  PRIMARY KEY (`id`),
  KEY `idx_refresh_user` (`user_id`),
  UNIQUE KEY `uk_refresh_token` (`token`),
  CONSTRAINT `fk_refresh_user` FOREIGN KEY (`user_id`) REFERENCES `ssafy_home`.`user` (`mno`) ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT='JWT 리프레시 토큰 저장';

-- 즐겨찾기 테이블
CREATE TABLE IF NOT EXISTS `ssafy_home`.`favorites` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `user_id` INT NOT NULL COMMENT 'user.mno 참조',
  `type` VARCHAR(20) NOT NULL COMMENT 'REGION | APARTMENT',
  `target_id` VARCHAR(64) NOT NULL COMMENT '대상 식별자(행정코드 또는 apt_seq)',
  `area_bucket` VARCHAR(20) NULL DEFAULT NULL COMMENT '면적 구간(옵션, 예: 0-20,20-30)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
  PRIMARY KEY (`id`),
  KEY `idx_fav_user` (`user_id`),
  CONSTRAINT `fk_fav_user` FOREIGN KEY (`user_id`) REFERENCES `ssafy_home`.`user` (`mno`) ON DELETE CASCADE,
  UNIQUE KEY `uk_fav_user_target` (`user_id`, `type`, `target_id`, `area_bucket`)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT='사용자 즐겨찾기';

-- 관심 아파트(최근 거래가/거래월 포함)
CREATE TABLE IF NOT EXISTS `ssafy_home`.`favorite_apartments` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `user_id` INT NOT NULL COMMENT 'user.mno 참조',
  `apt_seq` VARCHAR(20) NOT NULL COMMENT '아파트코드',
  `apt_name` VARCHAR(100) NOT NULL COMMENT '아파트명',
  `sido` VARCHAR(30) NOT NULL COMMENT '시',
  `gugun` VARCHAR(30) NOT NULL COMMENT '구군',
  `dong` VARCHAR(30) NOT NULL COMMENT '동',
  `last_deal_amount_krw` BIGINT NULL COMMENT '최근 거래가(원)',
  `last_deal_month` CHAR(6) NULL COMMENT '최근 거래월(YYYYMM)',
  `last_deal_area_pyeong` DECIMAL(7,2) NULL COMMENT '최근 거래 전용면적(평)',
  `build_year` INT NULL COMMENT '준공년도',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_favapt_user_apt` (`user_id`, `apt_seq`),
  KEY `idx_favapt_user_updated` (`user_id`, `updated_at`),
  CONSTRAINT `fk_favorite_apartments_user` FOREIGN KEY (`user_id`) REFERENCES `ssafy_home`.`user` (`mno`) ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT='관심 아파트';
