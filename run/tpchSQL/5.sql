explain
/*+
    SeqScan(vodka_supplier)
    BitmapScan(vodka_order_line vodka_order_line_pkey)
    SeqScan(vodka_customer)
    SeqScan(vodka_oorder)
    SeqScan(vodka_nation)
    SeqScan(vodka_region)
    Leading(((((vodka_region vodka_nation) (vodka_customer vodka_oorder)) vodka_order_line) vodka_supplier))
    HashJoin(vodka_region vodka_nation)
    HashJoin(vodka_customer vodka_oorder)
    HashJoin(vodka_region vodka_nation vodka_customer vodka_oorder)
    NestLoop(vodka_region vodka_nation vodka_customer vodka_oorder vodka_order_line)
    HashJoin(vodka_region vodka_nation vodka_customer vodka_oorder vodka_order_line vodka_supplier)
*/
explain analyze select n_name,
       sum(ol_amount * (1 - ol_discount)) as revenue, count(*)
from vodka_customer,
     vodka_oorder,
     vodka_order_line,
     vodka_supplier,
     vodka_nation,
     vodka_region
where c_w_id = o_w_id
  and c_d_id = o_d_id
  and c_id = o_c_id
  and ol_w_id = o_w_id
  and ol_d_id = o_d_id
  and ol_o_id = o_id
  and ol_suppkey = s_suppkey
  and c_nationkey = s_nationkey
  and s_nationkey = n_nationkey
  and n_regionkey = r_regionkey
  and r_name = 'ASIA'
  and o_entry_d < date '1992-01-01'
group by n_name
order by revenue desc;