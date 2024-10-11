/*+
    IndexScan(vodka_item vodka_item_pkey)
    SeqScan(vodka_supplier)
    BitmapScan(vodka_order_line vodka_order_line_pkey)
    IndexScan(vodka_oorder vodka_oorder_pkey)
    IndexScan(vodka_customer vodka_customer_pkey)
    SeqScan(n1)
    SeqScan(n2)
    SeqScan(vodka_region)
    Leading((((((((n1 vodka_region) vodka_customer) vodka_oorder) vodka_order_line) vodka_item) vodka_supplier) n2))
    HashJoin(n1 vodka_region)
    HashJoin(n1 vodka_region vodka_customer)
    HashJoin(n1 vodka_region vodka_customer vodka_oorder)
    NestLoop(n1 vodka_region vodka_customer vodka_oorder vodka_order_line)
    HashJoin(n1 vodka_region vodka_customer vodka_oorder vodka_order_line vodka_item)
    HashJoin(n1 vodka_region vodka_customer vodka_oorder vodka_order_line vodka_item vodka_supplier)
    HashJoin(n1 vodka_region vodka_customer vodka_oorder vodka_order_line vodka_item vodka_supplier n2)
*/

select o_year,
       sum(case
               when nation = 'BRAZIL' then volume
               else 0
           end) / sum(volume) as mkt_share
from (select extract(year from o_entry_d)  as o_year,
             ol_amount * (1 - ol_discount) as volume,
             n2.n_name                     as nation
      from vodka_item,
           vodka_supplier,
           vodka_order_line,
           vodka_oorder,
           vodka_customer,
           vodka_nation n1,
           vodka_nation n2,
           vodka_region
      where i_id = ol_i_id
        and s_suppkey = ol_suppkey
        and ol_w_id = o_w_id
        and ol_d_id = o_d_id
        and ol_o_id = o_id
        and c_w_id = o_w_id
        and c_d_id = o_d_id
        and c_id = o_c_id
        and c_nationkey = n1.n_nationkey
        and n1.n_regionkey = r_regionkey
        and r_name = 'AMERICA'
        and s_nationkey = n2.n_nationkey
        and o_entry_d between date '1995-01-01' and date '1996-12-31'
        and i_type = 'ECONOMY ANODIZED STEEL') as all_nations
group by o_year
order by o_year;