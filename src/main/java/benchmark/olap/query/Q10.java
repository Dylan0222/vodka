package benchmark.olap.query;

import benchmark.olap.OLAPTerminal;
import benchmark.oltp.OLTPClient;
import config.CommonConfig;
import org.apache.log4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static benchmark.oltp.OLTPClient.gloabalSysCurrentTime;
import static config.CommonConfig.DB_OCEANBASE;

public class Q10 extends baseQuery {
    private static Logger log = Logger.getLogger(Q10.class);
    public double k;
    public double b;
    private int dbType;

    public Q10(int dbType) throws ParseException {
        super();
        this.k = OLTPClient.k1;
        this.b = OLTPClient.b1;
        this.filterRate = benchmark.olap.OLAPClient.filterRate[9];                          //o_entry_d0.0765
        this.dbType = dbType;
        this.dynamicParam = getDeltaTimes();
        this.q = getQuery();
    }

    public String updateQuery() throws ParseException {
//        this.orderlineTSize=OLAPTerminal.orderLineTableSize;
//        this.orderTSize=OLAPTerminal.oorderTableSize;
//        this.olNotnullSize=OLAPTerminal.orderlineTableNotNullSize;
        this.k = OLTPClient.k1;
        this.b = OLTPClient.b1;
        this.dynamicParam = getDeltaTimes();
        this.q = getQuery();
        return this.q;
    }

    public String getDeltaTimes() throws ParseException {
        return String.valueOf(OLTPClient.sampler4Order.getPercentile(filterRate).getO_entry_d());
    }

    @Override
    public String getQuery() throws ParseException {
        // this.dynamicParam = getDeltaTimes();
        String query;
        switch (this.dbType) {
            case CommonConfig.DB_TIDB:
                query = "select /*+ read_from_storage(tiflash[vodka_customer, vodka_oorder, vodka_order_line, vodka_nation]) */" +
                        "    c_w_id,c_d_id,c_id, " +
                        "    c_last, " +
                        "    sum(ol_amount * (1 - ol_discount)) as revenue, " +
                        "    c_balance, " +
                        "    n_name, " +
                        "    c_phone " +
                        "from " +
                        "    vodka_customer, " +
                        "    vodka_oorder, " +
                        "    vodka_order_line, " +
                        "    vodka_nation " +
                        "where " +
                        "        c_w_id= o_w_id and c_d_id = o_d_id and c_id = o_c_id " +
                        "  and ol_w_id = o_w_id and ol_d_id = o_d_id and ol_o_id = o_id " +
                        "  and o_entry_d <  '" + this.dynamicParam + "' " +
                        "  and ol_returnflag = 'R' " +
                        "  and c_nationkey = n_nationkey " +
                        "group by " +
                        "    c_w_id,c_d_id,c_id, " +
                        "    c_last, " +
                        "    c_balance, " +
                        "    c_phone, " +
                        "    n_name " +
                        "order by " +
                        "    revenue desc limit 20;";
                break;
            default:
                query = "select " +
                        "    c_w_id,c_d_id,c_id, " +
                        "    c_last, " +
                        "    sum(ol_amount * (1 - ol_discount)) as revenue, " +
                        "    c_balance, " +
                        "    n_name, " +
                        "    c_phone " +
                        "from " +
                        "    vodka_customer, " +
                        "    vodka_oorder, " +
                        "    vodka_order_line, " +
                        "    vodka_nation " +
                        "where " +
                        "        c_w_id= o_w_id and c_d_id = o_d_id and c_id = o_c_id " +
                        "  and ol_w_id = o_w_id and ol_d_id = o_d_id and ol_o_id = o_id " +
                        "  and o_entry_d < TIMESTAMP  '" + this.dynamicParam + "' " +
                        "  and ol_returnflag = 'R' " +
                        "  and c_nationkey = n_nationkey " +
                        "group by " +
                        "    c_w_id,c_d_id,c_id, " +
                        "    c_last, " +
                        "    c_balance, " +
                        "    c_phone, " +
                        "    n_name " +
                        "order by " +
                        "    revenue desc limit 20;";
                break;
        }
        return query;
    }

    // @Override
    // public String getCountQuery() {
    //     String q_str = "";
    //     if (benchmark.olap.OLAPTerminal.countCheck) {
    //         q_str = "select count(*) " +
    //                 "from " +
    //                 "    vodka_oorder " +
    //                 "where " +
    //                 "  o_entry_d >= date '" + this.dynamicParam + "' " +
    //                 "  and o_entry_d < date '" + this.dynamicParam + "' + interval '3' month ;";
    //     }
    //     return q_str;
    // }

    @Override
    public String getExplainQuery() {
        switch (dbType) {
            case DB_OCEANBASE -> {
                return "EXPLAIN EXTENDED " + this.q;
            }
            default -> {
                return "EXPLAIN ANALYZE " + this.q;
            }
        }
    }

    @Override
    public String getFilterCheckQuery() {
        if (benchmark.olap.OLAPTerminal.filterRateCheck) {
            return "select ( " +
                    "(select count(*) from vodka_oorder where o_entry_d < TIMESTAMP '" + this.dynamicParam + "') " +
                    "/ " +
                    "(select count(*) from vodka_oorder) " +
                    ");";
        }
        return "";
    }

    @Override
    public String getDetailedExecutionPlan() {
        return "explain (analyze,costs false, timing false, summary false, format json) " + this.q;
    }
}
