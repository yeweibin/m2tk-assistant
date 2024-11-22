DROP VIEW IF EXISTS `PUBLIC`.`v_pcr_stat`;
DROP VIEW IF EXISTS `PUBLIC`.`v_tr290_stat`;
DROP TABLE IF EXISTS `PUBLIC`.`t_stream_source`;
DROP TABLE IF EXISTS `PUBLIC`.`t_elementary_stream`;
DROP TABLE IF EXISTS `PUBLIC`.`t_mpeg_program`;
DROP TABLE IF EXISTS `PUBLIC`.`t_ca_stream`;
DROP TABLE IF EXISTS `PUBLIC`.`t_si_bouquet`;
DROP TABLE IF EXISTS `PUBLIC`.`t_si_network`;
DROP TABLE IF EXISTS `PUBLIC`.`t_si_multiplex`;
DROP TABLE IF EXISTS `PUBLIC`.`t_si_service`;
DROP TABLE IF EXISTS `PUBLIC`.`t_si_event`;
DROP TABLE IF EXISTS `PUBLIC`.`t_si_datetime`;
DROP TABLE IF EXISTS `PUBLIC`.`t_table_version`;
DROP TABLE IF EXISTS `PUBLIC`.`t_pcr`;
DROP TABLE IF EXISTS `PUBLIC`.`t_pcr_check`;
DROP TABLE IF EXISTS `PUBLIC`.`t_tr290_event`;
DROP TABLE IF EXISTS `PUBLIC`.`t_private_section`;
DROP TABLE IF EXISTS `PUBLIC`.`t_transport_packet`;
DROP TABLE IF EXISTS `PUBLIC`.`t_program_elementary_mapping`;
DROP TABLE IF EXISTS `PUBLIC`.`t_bouquet_service_mapping`;
DROP TABLE IF EXISTS `PUBLIC`.`t_multiplex_service_mapping`;

CREATE TABLE IF NOT EXISTS `PUBLIC`.`t_preference` (
  `key` VARCHAR(100) NOT NULL PRIMARY KEY,
  `value` VARCHAR(1000) NOT NULL
);

CREATE TABLE IF NOT EXISTS `PUBLIC`.`t_stream_source` (
  `id` INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  `bitrate` INT DEFAULT 0 NOT NULL,
  `frame_size` INT DEFAULT 188 NOT NULL,
  `transport_stream_id` INT DEFAULT 0 NOT NULL,
  `packet_count` BIGINT DEFAULT 0 NOT NULL,
  `source_name` VARCHAR(500) NOT NULL,
  `source_uri` VARCHAR(1000) NOT NULL
);

CREATE TABLE IF NOT EXISTS `PUBLIC`.`t_elementary_stream` (
  `pid` INT PRIMARY KEY,
  `last_pct` BIGINT DEFAULT -1 NOT NULL,
  `pkt_cnt` BIGINT DEFAULT 0 NOT NULL,
  `pcr_cnt` BIGINT DEFAULT 0 NOT NULL,
  `tse_cnt` BIGINT DEFAULT 0 NOT NULL,
  `cce_cnt` BIGINT DEFAULT 0 NOT NULL,
  `bitrate` BIGINT DEFAULT -1 NOT NULL,
  `ratio` DOUBLE DEFAULT 0 NOT NULL,
  `stream_type` INT DEFAULT -1 NOT NULL,
  `is_scrambled` BOOLEAN DEFAULT FALSE NOT NULL,
  `category` VARCHAR(20),
  `description` VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS `PUBLIC`.`t_mpeg_program` (
  `id` INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  `tsid` INT NOT NULL,
  `prg_num` INT NOT NULL,
  `pmt_pid` INT NOT NULL,
  `pcr_pid` INT DEFAULT -1 NOT NULL,
  `pmt_version` INT DEFAULT -1 NOT NULL,
  `is_free_access` BOOLEAN DEFAULT TRUE NOT NULL
);

CREATE TABLE IF NOT EXISTS `PUBLIC`.`t_ca_stream` (
  `id` INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  `system_id` INT NOT NULL,
  `stream_pid` INT NOT NULL,
  `stream_type` INT DEFAULT 0 NOT NULL,
  `stream_private_data` VARBINARY(255),
  `program_ref` INT DEFAULT -1 NOT NULL,
  `es_pid` INT DEFAULT -1 NOT NULL
);

CREATE TABLE IF NOT EXISTS `PUBLIC`.`t_si_bouquet` (
  `id` INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  `bouquet_id` INT NOT NULL,
  `bouquet_name` VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS `PUBLIC`.`t_si_network` (
  `id` INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  `network_id` INT NOT NULL,
  `network_name` VARCHAR(100),
  `is_actual_nw` BOOLEAN DEFAULT TRUE NOT NULL
);

CREATE TABLE IF NOT EXISTS `PUBLIC`.`t_si_multiplex` (
  `id` INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  `network_ref` INT NOT NULL,
  `transport_stream_id` INT NOT NULL,
  `original_network_id` INT NOT NULL,
  `delivery_type` VARCHAR(100),
  `transmit_frequency` VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS `PUBLIC`.`t_si_service` (
  `id` INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  `transport_stream_id` INT NOT NULL,
  `original_network_id` INT NOT NULL,
  `service_id` INT NOT NULL,
  `reference_service_id` INT DEFAULT -1 NOT NULL,
  `service_type` INT DEFAULT 1 NOT NULL,
  `service_name` VARCHAR(200),
  `service_provider` VARCHAR(200),
  `running_status` INT DEFAULT 0 NOT NULL,
  `is_free_access` BOOLEAN DEFAULT TRUE NOT NULL,
  `is_pnf_eit_enabled` BOOLEAN DEFAULT TRUE NOT NULL,
  `is_sch_eit_enabled` BOOLEAN DEFAULT TRUE NOT NULL,
  `is_actual_ts` BOOLEAN DEFAULT TRUE NOT NULL,
  `is_nvod_ref_srv` BOOLEAN DEFAULT FALSE NOT NULL,
  `is_nvod_shift_srv` BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE TABLE IF NOT EXISTS `PUBLIC`.`t_si_event` (
  `id` INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  `transport_stream_id` INT NOT NULL,
  `original_network_id` INT NOT NULL,
  `service_id` INT NOT NULL,
  `event_id` INT NOT NULL,
  `reference_service_id` INT DEFAULT -1 NOT NULL,
  `reference_event_id` INT DEFAULT -1 NOT NULL,
  `start_time` DATETIME,
  `duration` INT DEFAULT 0 NOT NULL,
  `running_status` INT DEFAULT 0 NOT NULL,
  `event_name` VARCHAR(500),
  `event_description` VARCHAR(1000),
  `language_code` CHAR(3) DEFAULT 'chi',
  `is_free_access` BOOLEAN DEFAULT TRUE NOT NULL,
  `is_present_evt` BOOLEAN DEFAULT TRUE NOT NULL,
  `is_schedule_evt` BOOLEAN DEFAULT TRUE NOT NULL,
  `is_nvod_shift_evt` BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE TABLE IF NOT EXISTS `PUBLIC`.`t_si_datetime` (
  `id` INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  `timepoint` DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS `PUBLIC`.`t_table_version` (
  `id` INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  `table_id` INT NOT NULL,
  `table_id_ext` INT NOT NULL,
  `version` INT NOT NULL,
  `pct` BIGINT NOT NULL,
  `tag` VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS `PUBLIC`.`t_tr290_event` (
  `id` INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  `type` VARCHAR(20) NOT NULL,
  `description` VARCHAR(1000) NOT NULL,
  `pid` INT NOT NULL,
  `pct` BIGINT NOT NULL,
  `timepoint` DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS `PUBLIC`.`t_pcr` (
  `id` INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  `pid` INT NOT NULL,
  `pct` BIGINT DEFAULT 0 NOT NULL,
  `value` BIGINT DEFAULT 0 NOT NULL
);

CREATE TABLE IF NOT EXISTS `PUBLIC`.`t_pcr_check` (
  `id` INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  `pid` INT NOT NULL,
  `pre_pcr` BIGINT DEFAULT 0 NOT NULL,
  `pre_pct` BIGINT DEFAULT 0 NOT NULL,
  `cur_pcr` BIGINT DEFAULT 0 NOT NULL,
  `cur_pct` BIGINT DEFAULT 0 NOT NULL,
  `bitrate` BIGINT DEFAULT 0 NOT NULL,
  `int_ns` BIGINT DEFAULT 0 NOT NULL,
  `dif_ns` BIGINT DEFAULT 0 NOT NULL,
  `acc_ns` BIGINT DEFAULT 0 NOT NULL,
  `is_rep_check_failed` BOOLEAN DEFAULT FALSE NOT NULL,
  `is_dct_check_failed` BOOLEAN DEFAULT FALSE NOT NULL,
  `is_acc_check_failed` BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE TABLE IF NOT EXISTS `PUBLIC`.`t_private_section` (
  `id` INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  `tag` VARCHAR(100) NOT NULL,
  `pid` INT NOT NULL,
  `pct` BIGINT NOT NULL,
  `encoding` VARBINARY(4096) NOT NULL
);

CREATE TABLE IF NOT EXISTS `PUBLIC`.`t_transport_packet` (
  `id` INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  `tag` VARCHAR(100) NOT NULL,
  `pid` INT NOT NULL,
  `pct` BIGINT NOT NULL,
  `encoding` BINARY(188) NOT NULL
);

CREATE TABLE IF NOT EXISTS `PUBLIC`.`t_program_elementary_mapping` (
  `id` INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  `program_ref` INT NOT NULL,
  `stream_pid` INT NOT NULL,
  `stream_type` INT NOT NULL
);

CREATE TABLE IF NOT EXISTS `PUBLIC`.`t_multiplex_service_mapping` (
  `id` INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  `multiplex_ref` INT NOT NULL,
  `service_id` INT NOT NULL
);

CREATE TABLE IF NOT EXISTS `PUBLIC`.`t_bouquet_service_mapping` (
  `id` INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  `bouquet_ref` INT NOT NULL,
  `original_network_id` INT NOT NULL,
  `transport_stream_id` INT NOT NULL,
  `service_id` INT NOT NULL
);

CREATE VIEW IF NOT EXISTS `PUBLIC`.`v_pcr_stat` AS
SELECT `A`.`pid` AS `pid`,
       `A`.`pcr_count` AS `pcr_count`,
       `B`.`avg_bitrate` AS `avg_bitrate`,
       `B`.`max_interval` AS `max_interval`,
       `B`.`min_interval` AS `min_interval`,
       `B`.`avg_interval` AS `avg_interval`,
       `B`.`max_accuracy` AS `max_accuracy`,
       `B`.`min_accuracy` AS `min_accuracy`,
       `B`.`avg_accuracy` AS `avg_accuracy`,
       `B`.`rep_errors` AS `rep_errors`,
       `B`.`dct_errors` AS `dct_errors`,
       `B`.`acc_errors` AS `acc_errors`
FROM
  (SELECT `pid`, COUNT(`id`) AS `pcr_count` FROM `PUBLIC`.`t_pcr` GROUP BY `pid`) `A`
LEFT JOIN
  (SELECT `pid`,
      AVG(`bitrate`) AS `avg_bitrate`,
      MAX(`int_ns`) AS `max_interval`,
      MIN(`int_ns`) AS `min_interval`,
      AVG(`int_ns`) AS `avg_interval`,
      MAX(`acc_ns`) AS `max_accuracy`,
      MIN(`acc_ns`) AS `min_accuracy`,
      AVG(`acc_ns`) AS `avg_accuracy`,
      SUM(CASE `is_rep_check_failed` WHEN TRUE THEN 1 ELSE 0 END) AS `rep_errors`,
      SUM(CASE `is_dct_check_failed` WHEN TRUE THEN 1 ELSE 0 END) AS `dct_errors`,
      SUM(CASE `is_acc_check_failed` WHEN TRUE THEN 1 ELSE 0 END) AS `acc_errors`
  FROM `PUBLIC`.`t_pcr_check`
  GROUP BY `pid`) `B`
ON `A`.`pid` = `B`.`pid`
ORDER BY `A`.`pid` ASC;

CREATE VIEW IF NOT EXISTS `PUBLIC`.`v_tr290_stat` AS
SELECT `A`.`id` AS `id`,
       `A`.`type` AS `type`,
       `A`.`timepoint` AS `timepoint`,
       `A`.`description` AS `description`,
       `B`.`cnt` AS `cnt`
FROM `PUBLIC`.`t_tr290_event` `A`
INNER JOIN
  (SELECT MAX(`id`) AS `id`, COUNT(`id`) AS `cnt` FROM `PUBLIC`.`t_tr290_event` GROUP BY `type`) `B`
ON A.`id` = B.`id`
ORDER BY A.`type` ASC;

-- 初始化基本流
INSERT INTO `PUBLIC`.`t_elementary_stream` (`pid`)
 SELECT X FROM SYSTEM_RANGE(0, 8191);