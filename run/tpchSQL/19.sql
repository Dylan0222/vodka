explain select sum(ol_amount * (1 - ol_discount)) as revenue
from vodka_order_line,
     vodka_item
where (
            i_id = ol_i_id
        and i_brand = 'Brand#12'
        and i_container in ('SM CASE', 'SM BOX', 'SM PACK', 'SM PKG')
        and ol_quantity >= 1 and ol_quantity <= 1 + 10
        and i_size between 1 and 5
        and ol_shipmode in ('AIR', 'AIR REG')
        and ol_shipinstruct = 'DELIVER IN PERSON'
    );