/*+
    SeqScan(vodka_customer)
    BitmapScan(vodka_oorder vodka_oorder_entry_d_index)
    BitmapScan(vodka_order_line vodka_order_line_pkey)
    IndexScan(vodka_nation nation_pkey)
    Leading((((vodka_customer vodka_oorder) vodka_order_line) vodka_nation))
    MergeJoin(vodka_customer vodka_oorder)
    NestLoop(vodka_customer vodka_oorder vodka_order_line)
    NestLoop(vodka_customer vodka_oorder vodka_order_line vodka_nation)
*/
select c_w_id,
       c_d_id,
       c_id,
       c_last,
       sum(ol_amount * (1 - ol_discount)) as revenue,
       c_balance,
       n_name,
       c_phone
from vodka_customer,
     vodka_oorder,
     vodka_order_line,
     vodka_nation
where c_w_id = o_w_id
  and c_d_id = o_d_id
  and c_id = o_c_id
  and ol_w_id = o_w_id
  and ol_d_id = o_d_id
  and ol_o_id = o_id
  and o_entry_d >= date '1993-10-01'
  and o_entry_d < date '1993-10-01' + interval '3' month
  and ol_returnflag = 'R'
  and c_nationkey = n_nationkey
group by c_w_id, c_d_id, c_id,
         c_last,
         c_balance,
         c_phone,
         n_name
order by revenue desc;