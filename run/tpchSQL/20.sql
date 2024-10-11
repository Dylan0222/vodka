select s_name,
       s_address
from vodka_supplier,
     vodka_nation
where s_suppkey in (select s_tocksuppkey
                    from vodka_stock
                    where s_i_id in (select i_id
                                     from vodka_item
                                     where i_name like 'forest%')
                      and s_quantity > (select 0.5 * sum(ol_quantity)
                                        from vodka_order_line
                                        where ol_i_id = s_i_id
                                          and ol_suppkey = s_tocksuppkey
                                          and ol_delivery_d >= date '1994-01-01'
                                          and ol_delivery_d < date '1994-01-01' + interval '1' year))
  and s_nationkey = n_nationkey
  and n_name = 'CANADA'
order by s_name;