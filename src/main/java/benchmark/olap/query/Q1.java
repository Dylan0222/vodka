package benchmark.olap.query;

import benchmark.olap.OLAPTerminal;
import benchmark.oltp.OLTPClient;
import config.CommonConfig;
import lombok.Getter;

import org.apache.log4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static config.CommonConfig.DB_OCEANBASE;

@Getter
public class Q1 extends baseQuery {
    private static Logger log = Logger.getLogger(Q1.class);
    public double k;
    public double b;
    private int dbType;

    public Q1(int dbType) throws ParseException {
        super();
        this.k = OLTPClient.k2;
        this.b = OLTPClient.b2;
        this.filterRate = benchmark.olap.OLAPClient.filterRate[0]; // ol_delivery_d = 0.3,0.01014
        this.dbType = dbType;
        this.q = getQuery();
    }

    public String updateQuery() throws ParseException {
        this.k = OLTPClient.k2;
        this.b = OLTPClient.b2;
        this.dynamicParam = this.getDeltaTimes();
        this.q = getQuery();
        return this.q;
    }

    public String getDeltaTimes() throws ParseException {
        // var result = OLTPClient.sampler4OrderLine.getRandomItem();
        // String t1 = String.valueOf(result.getOl_delivery_d());
        // var result = OLTPClient.sampler4OrderLine.getPercentileList(filterRate);
        // String t1 = String.valueOf(result.get(0).getOl_delivery_d());
        // String t2 = String.valueOf(result.get(1).getOl_delivery_d());
        // System.out.println("returned is " + t1 + ", max is" + t2);
        String t1 = "1995-01-01";
        return t1;
    }

    @Override
    public String getQuery() throws ParseException { // ol_delivery_d'1998-12-01'
        // this.dynamicParam = this.getDeltaTimes();
        String query;
        if (this.dbType == CommonConfig.DB_TIDB) {
            query = "select /*+ read_from_storage(tiflash[vodka_order_line]) */" +
                    "    ol_number, " +
                    "    ol_returnflag, " +
                    "    sum(ol_quantity) as sum_qty, " +
                    "    sum(ol_amount) as sum_base_price, " +
                    "    sum(ol_amount * (1 - ol_discount)) as sum_disc_price, " +
                    "    sum(ol_amount * (1 - ol_discount) * (1 + ol_tax)) as sum_charge, " +
                    "    avg(ol_quantity) as avg_qty, " +
                    "    avg(ol_amount) as avg_price, " +
                    "    avg(ol_discount) as avg_disc, " +
                    "    count(*) as count_order " +
                    "from " +
                    "    vodka_order_line " +
                    "where " +
                    "        ol_delivery_d <= '" + this.dynamicParam + "' " +
                    "group by " +
                    "    ol_returnflag, " +
                    "    ol_number " +
                    "order by " +
                    "    ol_returnflag, " +
                    "    ol_number; ";
        } else {
            query = "/*+ SeqScan(vodka_order_line) */ "
                    + "select " +
                    " ol_number, " +
                    " ol_returnflag, " +
                    " sum(ol_quantity) as sum_qty, " +
                    " sum(ol_amount) as sum_base_price, " +
                    " sum(ol_amount * (1 - ol_discount)) as sum_disc_price, " +
                    " sum(ol_amount * (1 - ol_discount) * (1 + ol_tax)) as sum_charge, " +
                    " avg(ol_quantity) as avg_qty, " +
                    " avg(ol_amount) as avg_price, " +
                    " avg(ol_discount) as avg_disc, " +
                    " count(*) as count_order " +
                    "from " +
                    " vodka_order_line " +
                    "where " +
                    " ol_delivery_d >= TIMESTAMP '" + this.dynamicParam + "' " +
                    "group by " +
                    " ol_returnflag, " +
                    " ol_number " +
                    "order by " +
                    " ol_returnflag, " +
                    " ol_number; ";
        }
        return query;
    }

    @Override
    public String getExplainQuery() {
        if (dbType == DB_OCEANBASE) {
            return "EXPLAIN EXTENDED " + this.q;
        }
        return "explain analyze" + this.q;
    }

    @Override
    public String getFilterCheckQuery() {
        if (benchmark.olap.OLAPTerminal.filterRateCheck) {
            return "select ( " +
                    "(select count(*) from vodka_order_line where ol_delivery_d<= TIMESTAMP '" + this.dynamicParam
                    + "') " +
                    "/ " +
                    "(select count(*) from vodka_order_line) " +
                    ");";
        }
        return "";
    }

    @Override
    public String getDetailedExecutionPlan() {
        return "explain (analyze, costs false, timing false, summary false, format json) " + this.q;
    }

}
