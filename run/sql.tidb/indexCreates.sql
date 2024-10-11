
alter table vodka_district
    add constraint vodka_district_pkey
        primary key (d_w_id, d_id);

alter table vodka_customer
    add constraint vodka_customer_pkey
        primary key (c_w_id, c_d_id, c_id);

create index vodka_customer_idx1
    on vodka_customer (c_w_id, c_d_id, c_last, c_first);

alter table vodka_oorder
    add constraint vodka_oorder_pkey
        primary key (o_w_id, o_d_id, o_id);

create unique index vodka_oorder_idx1
    on vodka_oorder (o_w_id, o_d_id, o_carrier_id, o_id);

alter table vodka_new_order
    add constraint vodka_new_order_pkey
        primary key (no_w_id, no_d_id, no_o_id);

alter table vodka_order_line
    add constraint vodka_order_line_pkey
        primary key (ol_w_id, ol_d_id, ol_o_id, ol_number);

alter table vodka_stock
    add constraint vodka_stock_pkey
        primary key (s_w_id, s_i_id);

alter table vodka_item
    add constraint vodka_item_pkey
        primary key (i_id);

alter table vodka_nation
    add constraint nation_pkey primary key (n_nationkey);
alter table vodka_region
    add constraint region_pkey primary key (r_regionkey);
alter table vodka_supplier
    add constraint supplier_pkey primary key (s_suppkey);

create index vodka_order_line_ol_i_id_index on vodka_order_line (ol_i_id);
create index supplier_s_nationkey_index on vodka_supplier (s_nationkey);
create index vodka_customer_c_state_index on vodka_customer (c_nationkey);
create index nation_n_regionkey_index on vodka_nation (n_regionkey);
create index vodka_stock_s_tocksuppkey_index on vodka_stock (s_tocksuppkey);
create index vodka_stock_s_i_id_index on vodka_stock (s_i_id);
create index vodka_order_line_ol_suppkey_index on vodka_order_line (ol_suppkey);
create index vodka_order_line_ol_delivery_d_index on vodka_order_line (ol_delivery_d);
create index vodka_oorder_entry_d_index on vodka_oorder (o_entry_d);
create index vodka_order_line_currentts_index on vodka_order_line (current_ts);
create index vodka_time_index on vodka_time (new_order, payment);

ALTER TABLE `benchmarksql`.`vodka_customer` SET TIFLASH REPLICA 3;
ALTER TABLE `benchmarksql`.`vodka_config` SET TIFLASH REPLICA 3;
ALTER TABLE `benchmarksql`.`vodka_district` SET TIFLASH REPLICA 3;
ALTER TABLE `benchmarksql`.`vodka_history` SET TIFLASH REPLICA 3;
ALTER TABLE `benchmarksql`.`vodka_item` SET TIFLASH REPLICA 3;
ALTER TABLE `benchmarksql`.`vodka_nation` SET TIFLASH REPLICA 3;
ALTER TABLE `benchmarksql`.`vodka_new_order` SET TIFLASH REPLICA 3;
ALTER TABLE `benchmarksql`.`vodka_oorder` SET TIFLASH REPLICA 3;
ALTER TABLE `benchmarksql`.`vodka_order_line` SET TIFLASH REPLICA 3;
ALTER TABLE `benchmarksql`.`vodka_region` SET TIFLASH REPLICA 3;
ALTER TABLE `benchmarksql`.`vodka_stock` SET TIFLASH REPLICA 3;
ALTER TABLE `benchmarksql`.`vodka_supplier` SET TIFLASH REPLICA 3;
ALTER TABLE `benchmarksql`.`vodka_warehouse` SET TIFLASH REPLICA 3;
ALTER TABLE `benchmarksql`.`vodka_time` SET TIFLASH REPLICA 3;

ANALYZE
TABLE vodka_warehouse ;
ANALYZE
TABLE vodka_district ;
ANALYZE
TABLE vodka_customer ;
ANALYZE
TABLE vodka_oorder ;
ANALYZE
TABLE vodka_new_order ;
ANALYZE
TABLE vodka_order_line ;
ANALYZE
TABLE vodka_oorder ;
ANALYZE
TABLE vodka_stock ;
ANALYZE
TABLE vodka_item ;
ANALYZE
TABLE vodka_history ;
ANALYZE
TABLE vodka_nation ;
ANALYZE
TABLE vodka_region ;
ANALYZE
TABLE vodka_supplier ;
ANALYZE
TABLE vodka_time ;

-- SELECT * FROM information_schema.tiflash_replica WHERE TABLE_SCHEMA = 'benchmarksql' and TABLE_NAME = 'vodka_customer';
-- SELECT * FROM information_schema.tiflash_replica WHERE TABLE_SCHEMA = 'benchmarksql' and TABLE_NAME = 'vodka_config';
-- SELECT * FROM information_schema.tiflash_replica WHERE TABLE_SCHEMA = 'benchmarksql' and TABLE_NAME = 'vodka_district';
-- SELECT * FROM information_schema.tiflash_replica WHERE TABLE_SCHEMA = 'benchmarksql' and TABLE_NAME = 'vodka_history';
-- SELECT * FROM information_schema.tiflash_replica WHERE TABLE_SCHEMA = 'benchmarksql' and TABLE_NAME = 'vodka_item';
-- SELECT * FROM information_schema.tiflash_replica WHERE TABLE_SCHEMA = 'benchmarksql' and TABLE_NAME = 'vodka_nation';
-- SELECT * FROM information_schema.tiflash_replica WHERE TABLE_SCHEMA = 'benchmarksql' and TABLE_NAME = 'vodka_new_order';
-- SELECT * FROM information_schema.tiflash_replica WHERE TABLE_SCHEMA = 'benchmarksql' and TABLE_NAME = 'vodka_oorder';
-- SELECT * FROM information_schema.tiflash_replica WHERE TABLE_SCHEMA = 'benchmarksql' and TABLE_NAME = 'vodka_order_line';
-- SELECT * FROM information_schema.tiflash_replica WHERE TABLE_SCHEMA = 'benchmarksql' and TABLE_NAME = 'vodka_region';
-- SELECT * FROM information_schema.tiflash_replica WHERE TABLE_SCHEMA = 'benchmarksql' and TABLE_NAME = 'vodka_stock';
-- SELECT * FROM information_schema.tiflash_replica WHERE TABLE_SCHEMA = 'benchmarksql' and TABLE_NAME = 'vodka_supplier';
-- SELECT * FROM information_schema.tiflash_replica WHERE TABLE_SCHEMA = 'benchmarksql' and TABLE_NAME = 'vodka_warehouse';

