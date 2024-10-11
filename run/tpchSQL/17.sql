explain select count(*)
from vodka_order_line,
     vodka_item
where i_id = ol_i_id
  and i_brand = 'Brand#23'
  and i_container = 'MED BOX'
  and ol_quantity < (select 0.2 * avg(ol_quantity)
                     from vodka_order_line
                     where ol_i_id = i_id);