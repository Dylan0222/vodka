/*+
    IndexScan(vodka_item vodka_item_pkey)
    SeqScan(vodka_supplier)
    SeqScan(vodka_stock)
    SeqScan(vodka_nation)
    SeqScan(vodka_region)
*/
select s_acctbal,
       s_name,
       n_name,
       s_address,
       s_phone,
       s_comment
from vodka_item,
     vodka_supplier,
     vodka_stock,
     vodka_nation,
     vodka_region
where i_id = s_i_id
  and s_suppkey = s_tocksuppkey
  and i_size = 15
  and i_type like '%BRASS'
  and s_nationkey = n_nationkey
  and n_regionkey = r_regionkey
  and r_name = 'EUROPE'
  and s_supplycost = (select min(s_supplycost)
                      from vodka_stock,
                           vodka_supplier,
                           vodka_nation,
                           vodka_region
                      where i_id = s_i_id
                        and s_suppkey = s_tocksuppkey
                        and s_nationkey = n_nationkey
                        and n_regionkey = r_regionkey
                        and r_name = 'EUROPE')
order by s_acctbal desc,
         n_name,
         s_name,
         i_id;
