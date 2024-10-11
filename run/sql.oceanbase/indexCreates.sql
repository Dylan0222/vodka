create index vodka_customer_idx1
    on vodka_customer (c_w_id, c_d_id, c_last, c_first) local;

create unique index vodka_oorder_idx1
    on vodka_oorder (o_w_id, o_d_id, o_carrier_id, o_id);

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

analyze
table vodka_warehouse partition(vodka_warehouse) compute statistics for all columns size auto;
analyze
table vodka_customer partition(vodka_customer) compute statistics for all columns size auto;
analyze
table vodka_district partition(vodka_district) compute statistics for all columns size auto;
analyze
table vodka_oorder partition(vodka_oorder) compute statistics for all columns size auto;
analyze
table vodka_new_order partition(vodka_new_order) compute statistics for all columns size auto;
analyze
table vodka_order_line partition(vodka_order_line) compute statistics for all columns size auto;
analyze
table vodka_stock partition(vodka_stock) compute statistics for all columns size auto;
analyze
table vodka_history partition(vodka_history) compute statistics for all columns size auto;
analyze
table vodka_item compute statistics for all columns size auto;
analyze
table vodka_nation compute statistics for all columns size auto;
analyze
table vodka_region compute statistics for all columns size auto;
analyze
table vodka_supplier compute statistics for all columns size auto;
analyze
table vodka_time compute statistics for all columns size auto;



