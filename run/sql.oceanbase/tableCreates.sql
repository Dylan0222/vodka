create table vodka_config
(
    cfg_name  varchar(30) primary key,
    cfg_value varchar(50)
);

-- drop tablegroup tpcc_group;
drop tablegroup if exists vodka_group;
create tablegroup vodka_group binding true partition by hash partitions 93;

create table vodka_warehouse
(
    w_id       integer not null,
    w_name     varchar(10),
    w_street_1 varchar(20),
    w_street_2 varchar(20),
    w_city     varchar(20),
    w_state    char(2),
    w_zip      char(9),
    w_tax      decimal(4, 4),
    w_ytd      decimal(12, 2),
    constraint vodka_warehouse_pkey
        primary key (w_id)
) tablegroup='vodka_group' partition by hash(w_id) partitions 93;

create table vodka_district
(
    d_w_id      integer not null,
    d_id        integer not null,
    d_name      varchar(10),
    d_street_1  varchar(20),
    d_street_2  varchar(20),
    d_city      varchar(20),
    d_state     char(2),
    d_zip       char(9),
    d_tax       decimal(4, 4),
    d_ytd       decimal(12, 2),
    d_next_o_id integer,
    constraint vodka_district_pkey
        primary key (d_w_id, d_id)
) tablegroup='vodka_group' partition by hash(d_w_id) partitions 93;

create table vodka_customer
(
    c_w_id         integer not null,
    c_d_id         integer not null,
    c_id           integer not null,
    c_first        varchar(16),
    c_middle       char(2),
    c_last         varchar(16),
    c_street_1     varchar(20),
    c_street_2     varchar(20),
    c_city         varchar(20),
    c_nationkey    integer not null,
    c_zip          char(9),
    c_phone        char(16),
    c_since        timestamp,
    c_credit       char(2),
    c_credit_lim   decimal(12, 2),
    c_discount     decimal(4, 4),
    c_balance      decimal(12, 2),
    c_ytd_payment  decimal(12, 2),
    c_payment_cnt  integer,
    c_delivery_cnt integer,
    c_data         varchar(500),
    c_mktsegment   char(10),
    constraint vodka_customer_pkey
        primary key (c_w_id, c_d_id, c_id)
) tablegroup='vodka_group' partition by hash(c_w_id) partitions 93;

create table vodka_history
(
    hist_id  integer AUTO_INCREMENT,
    h_c_id   integer,
    h_c_d_id integer,
    h_c_w_id integer,
    h_d_id   integer,
    h_w_id   integer,
    h_date   timestamp,
    h_amount decimal(6, 2),
    h_data   varchar(24)
) tablegroup='vodka_group' partition by hash(h_w_id) partitions 93 ;

create table vodka_new_order
(
    no_w_id integer not null,
    no_d_id integer not null,
    no_o_id integer not null,
    constraint vodka_new_order_pkey
        primary key (no_w_id, no_d_id, no_o_id)
) tablegroup='vodka_group' partition by hash(no_w_id) partitions 93 ;

create table vodka_oorder
(
    o_w_id         integer not null,
    o_d_id         integer not null,
    o_id           integer not null,
    o_c_id         integer,
    o_entry_d      timestamp,
    o_carrier_id   integer,
    o_ol_cnt       integer,
    o_all_local    integer,
    o_comment      varchar(79),
    o_shippriority integer not null,
    constraint vodka_oorder_pkey
        primary key (o_w_id, o_d_id, o_id)
)  tablegroup='vodka_group' partition by hash(o_w_id) partitions 93;

create table vodka_order_line
(
    ol_w_id         integer not null,
    ol_d_id         integer not null,
    ol_o_id         integer not null,
    ol_number       integer not null,
    ol_i_id         integer not null,
    ol_delivery_d   timestamp,
    ol_amount       decimal(6, 2),
    ol_supply_w_id  integer,
    ol_quantity     integer,
    ol_tax          decimal(15, 2),
    ol_dist_info    char(24),
    ol_discount     decimal(15, 2),
    ol_shipmode     char(10),
    ol_shipinstruct char(25),
    ol_receipdate   timestamp,
    ol_commitdate   timestamp,
    ol_returnflag   char(1),
    ol_suppkey      integer not null,
    access_version  integer default 0,
    current_ts      TIMESTAMP,
    constraint vodka_order_line_pkey
        primary key (ol_w_id, ol_d_id, ol_o_id, ol_number)
) tablegroup='vodka_group' partition by hash(ol_w_id) partitions 93;

create table vodka_item
(
    i_id        integer     not null,
    i_name      varchar(24),
    i_price     decimal(5, 2),
    i_data      varchar(50),
    i_im_id     integer,
    i_container char(10)    not null,
    i_size      integer     not null,
    i_brand     char(10)    not null,
    i_type      varchar(25) not null,
    i_mfgr      char(25)    not null,
    constraint vodka_item_pkey
    primary key (i_id)
) use_bloom_filter=true duplicate_scope='cluster';

create table vodka_stock
(
    s_w_id        integer not null,
    s_i_id        integer not null,
    s_quantity    integer,
    s_ytd         integer,
    s_order_cnt   integer,
    s_remote_cnt  integer,
    s_data        varchar(50),
    s_dist_01     char(24),
    s_dist_02     char(24),
    s_dist_03     char(24),
    s_dist_04     char(24),
    s_dist_05     char(24),
    s_dist_06     char(24),
    s_dist_07     char(24),
    s_dist_08     char(24),
    s_dist_09     char(24),
    s_dist_10     char(24),
    s_supplycost  decimal(6, 2),
    s_tocksuppkey integer not null,
    constraint vodka_stock_pkey
        primary key (s_w_id, s_i_id)
) tablegroup='vodka_group' partition by hash(s_w_id) partitions 93;

create table vodka_nation
(
    n_nationkey integer     NOT NULL,
    n_name      varchar(25) NOT NULL,
    n_regionkey integer     NOT NULL,
    n_comment   varchar(152),
    constraint nation_pkey primary key (n_nationkey)
) use_bloom_filter=true duplicate_scope='cluster';

create table vodka_region
(
    r_regionkey integer     NOT NULL,
    r_name      varchar(25) NOT NULL,
    r_comment   varchar(152),
    constraint region_pkey primary key (r_regionkey)
) use_bloom_filter=true duplicate_scope='cluster';

create table vodka_supplier
(
    s_suppkey   integer NOT NULL,
    s_name      char(25),
    s_address   varchar(40),
    s_nationkey integer NOT NULL,
    s_phone     char(15),
    s_acctbal   decimal(15, 2),
    s_comment   varchar(101),
    constraint supplier_pkey primary key (s_suppkey)
) use_bloom_filter=true duplicate_scope='cluster';

create table vodka_time
(
    new_order integer UNSIGNED,
    payment integer UNSIGNED
);
insert into vodka_time values(0, 0);
