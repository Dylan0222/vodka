select ((select count(*) from vodka_order_line where ol_delivery_d<= '1997-09-02 00:00:00') /
         (select count(*) from vodka_order_line) );



