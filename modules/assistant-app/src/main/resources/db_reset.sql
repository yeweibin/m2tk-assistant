TRUNCATE TABLE `PUBLIC`.`t_elementary_stream`;
TRUNCATE TABLE `PUBLIC`.`t_mpeg_program`;
TRUNCATE TABLE `PUBLIC`.`t_ca_stream`;
TRUNCATE TABLE `PUBLIC`.`t_si_bouquet`;
TRUNCATE TABLE `PUBLIC`.`t_si_network`;
TRUNCATE TABLE `PUBLIC`.`t_si_multiplex`;
TRUNCATE TABLE `PUBLIC`.`t_si_service`;
TRUNCATE TABLE `PUBLIC`.`t_si_event`;
TRUNCATE TABLE `PUBLIC`.`t_si_datetime`;
TRUNCATE TABLE `PUBLIC`.`t_table_version`;
TRUNCATE TABLE `PUBLIC`.`t_pcr`;
TRUNCATE TABLE `PUBLIC`.`t_pcr_check`;
TRUNCATE TABLE `PUBLIC`.`t_tr290_event`;
TRUNCATE TABLE `PUBLIC`.`t_private_section`;
TRUNCATE TABLE `PUBLIC`.`t_transport_packet`;
TRUNCATE TABLE `PUBLIC`.`t_program_elementary_mapping`;
TRUNCATE TABLE `PUBLIC`.`t_bouquet_service_mapping`;
TRUNCATE TABLE `PUBLIC`.`t_multiplex_service_mapping`;
TRUNCATE TABLE `PUBLIC`.`t_density_bulk`;

-- 初始化基本流
INSERT INTO `PUBLIC`.`t_elementary_stream` (`pid`)
 SELECT X FROM SYSTEM_RANGE(0, 8191);

