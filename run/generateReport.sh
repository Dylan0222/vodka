#!/usr/bin/env bash

if [ $# -lt 1 ] ; then
    echo "usage: $(basename $0) RESULT_DIR [SKIP_MINUTES]" >&2
    exit 2
fi

TABLE_WIDTH="1100px"
if [ $# -gt 1 ] ; then
	SKIP=$2
elses
	SKIP=0
fi

function getRunInfo()
{
    exec 3< data/runInfo.csv
    read hdrs <&3
    hdrs=$(echo ${hdrs} | tr ',' ' ')
    IFS=, read $hdrs <&3
    exec <&3-

    eval echo "\$$1"
}

function getRunInfoColumns()
{
    exec 3< data/runInfo.csv
    read hdrs <&3
    hdrs=$(echo ${hdrs} | tr ',' ' ')
    exec <&3-

    echo "${hdrs}"
}

function getProp()
{
    grep "^${1}=" run.properties | sed -e "s/^${1}=//"
}

bash generateGraphs.sh "${1}" $SKIP
cd "${1}"
echo -n "Generating ${1}/report.html ... "

# ----
# Start the report.
# ----
cat >report.html <<_EOF_
<html>
<head>
  <title>
    Vodka Run #$(getRunInfo run) started $(getRunInfo sessionStart)
  </title>
  <style>

h1,h2,h3,h4	{ color:#2222AA;
		}

h1		{ font-family: Helvetica,Arial;
		  font-weight: 700;
		  font-size: 24pt;
		}

h2		{ font-family: Helvetica,Arial;
		  font-weight: 700;
		  font-size: 18pt;
		}

h3,h4		{ font-family: Helvetica,Arial;
		  font-weight: 700;
		  font-size: 16pt;
		}

p,li,dt,dd	{ font-family: Helvetica,Arial;
		  font-size: 14pt;
		}

p		{ margin-left: 50px;
		}

pre		{ font-family: Courier,Fixed;
		  font-size: 14pt;
		}

samp		{ font-family: Courier,Fixed;
		  font-weight: 900;
		  font-size: 14pt;
		}

big		{ font-weight: 900;
		  font-size: 120%;
		}

  </style>
</head>
<body bgcolor="#ffffff">
  <h1>
    Vodka Run #$(getRunInfo run) started $(getRunInfo sessionStart)
  </h1>

  <p>
    This TPC-C&TPC-H style benchmark run was performed by the "$(getRunInfo driver)"
    driver of Vodka version $(getRunInfo driverVersion). 
  </p>
_EOF_

# ----
# Show the run properties.
# ----
cat >>report.html <<_EOF_
  <h2>
    Run Properties
  </h2>
  <p>
    <table width="${TABLE_WIDTH}" border="0">
    <tr><td bgcolor="#f0f0f0">
    <pre><small>
_EOF_
sed -e 's/^password=.*/password=\*\*\*\*\*\*/' <run.properties >>report.html
cat >>report.html <<_EOF_
    </small></pre>
    </td></tr>
    </table>
  </p>

_EOF_

# ----
# Show the result summary.
# ----
cat >>report.html <<_EOF_
  <h2>
    Result Summary
  </h2>
_EOF_

if [ $(getRunInfo driver) == "simple" ] ; then
    cat >> report.html <<_EOF_
    <p>
      Note that the "simple" driver is not a true TPC-C implementation.
      This driver only measures the database response time, not the
      response time of a System under Test as it would be experienced
      by an end-user in a 3-tier test implementation.
    </p>
_EOF_
fi

cat >> report.html <<_EOF_
  <p>
    <table width="${TABLE_WIDTH}" border="2">
    <tr>
      <th rowspan="2" width="16%"><b>Transaction<br/>Type</b></th>
      <th colspan="3" width="24%"><b>Latency</b></th>
      <th rowspan="2" width="12%"><b>Count</b></th>
      <th rowspan="2" width="12%"><b>Percent</b></th>
      <th rowspan="2" width="12%"><b>Rollback</b></th>
      <th rowspan="2" width="12%"><b>Errors</b></th>
      <th rowspan="2" width="12%"><b>Skipped<br/>Deliveries</b></th>
    </tr>
    <tr>
      <th width="8%"><b>90th&nbsp;%</b></th>
      <th width="8%"><b>Avg</b></th>
      <th width="8%"><b>Max</b></th>
    </tr>
_EOF_

tr ',' ' ' <data/tx_summary.csv | \
    while read name count percent ninth avg max limit rbk error dskipped ; do
	[ ${name} == "tx_name" ] && continue
	[ ${name} == "tpmC" ] && continue
	[ ${name} == "tpmTotal" ] && continue

	echo "    <tr>"
	echo "      <td align=\"left\">${name}</td>"
	echo "      <td align=\"right\">${ninth}</td>"
	echo "      <td align=\"right\">${avg}</td>"
	echo "      <td align=\"right\">${max}</td>"
	echo "      <td align=\"right\">${count}</td>"
	echo "      <td align=\"right\">${percent}</td>"
	echo "      <td align=\"right\">${rbk}</td>"
	echo "      <td align=\"right\">${error}</td>"
	echo "      <td align=\"right\">${dskipped}</td>"
	echo "    </tr>"
    done >>report.html

tpmC=$(grep "^tpmC," data/tx_summary.csv | sed -e 's/[^,]*,//' -e 's/,.*//')
tpmCpct=$(grep "^tpmC," data/tx_summary.csv | sed -e 's/[^,]*,[^,]*,//' -e 's/,.*//')
tpmTotal=$(grep "^tpmTotal," data/tx_summary.csv | sed -e 's/[^,]*,//' -e 's/,.*//')
cat >>report.html <<_EOF_
    </table>
  </p>

  <p>
    <table border="0">
      <tr>
        <td align="left"><big><b>Overall tpmC:</b></big></td>
        <td align="right"><big><b>${tpmC}</b></big></td>
      </tr>
      <tr>
        <td align="left"><big><b>Overall tpmTotal:</b></big></td>
        <td align="right"><big><b>${tpmTotal}</b></big></td>
      </tr>
    </table>
  </p>
  <p>
    The TPC-C specification has an theoretical maximum of 12.86 NEW_ORDER
    transactions per minute per warehouse. In reality this value cannot
    be reached because it would require a perfect mix with 45% of NEW_ORDER
    transactions and a ZERO response time from the System under Test
    including the database. 
  </p>
  <p>
    The above tpmC of ${tpmC} is ${tpmCpct} of that theoretical maximum for a
    database with $(getRunInfo runWarehouses) warehouses.
  </p>

_EOF_

# ----
# Show the graphs for tpmC/tpmTOTAL and latency.
# ----
cat >>report.html <<_EOF_
  <h2>
    Transactions per Minute and Transaction Latency
  </h2>
  <p>
    tpmC is the number of NEW_ORDER Transactions, that where processed
    per minute. tpmTOTAL is the number of Transactions processed per
    minute for all transaction types, but without the background part
    of the DELIVERY transaction. 

    <br/>
    <img src="data:image/svg+xml;base64,$(base64 tpm_nopm.svg)" />
    <br/>
    <img src="data:image/svg+xml;base64,$(base64 latency.svg)" />
  </p>
_EOF_

# ----
# Add all the System Resource graphs. First the CPU and dirty buffers.
# ----
cat >>report.html <<_EOF_
  <h2>
    System Resource Usage
  </h2>
  <h3>
    CPU Utilization for 49.52.27.33
  </h3>
  <p>
    <table border="2">
    <tr>
      <th colspan="2"><b>Overall Average CPU Utilization</b></th>
    </tr>
_EOF_

tr ',' ' ' <data/xjk@49.52.27.33/cpu_summary.csv | \
# tr ',' ' ' <data/ly@49.52.27.33/cpu_summary.csv | \
    while read category value ; do
	[ $category == "cpu_category" ] && continue
	echo "    <tr>"
	echo "      <td align=\"left\">${category}</td>"
	echo "      <td align=\"right\">${value}</td>"
	echo "    </tr>"
    done >>report.html

cat >>report.html <<_EOF_
  <h2>
    System Resource Usage
  </h2>
  <h3>
    CPU Utilization for 49.52.27.34
  </h3>
  <p>
    <table border="2">
    <tr>
      <th colspan="2"><b>Overall Average CPU Utilization</b></th>
    </tr>
_EOF_

tr ',' ' ' <data/xjk@49.52.27.34/cpu_summary.csv | \
# tr ',' ' ' <data/ly@49.52.27.34/cpu_summary.csv | \
    while read category value ; do
	[ $category == "cpu_category" ] && continue
	echo "    <tr>"
	echo "      <td align=\"left\">${category}</td>"
	echo "      <td align=\"right\">${value}</td>"
	echo "    </tr>"
    done >>report.html

cat >>report.html <<_EOF_
   <h2>
     System Resource Usage
   </h2>
   <h3>
     CPU Utilization for 49.52.27.35
   </h3>
   <p>
     <table border="2">
     <tr>
       <th colspan="2"><b>Overall Average CPU Utilization</b></th>
     </tr>
_EOF_

tr ',' ' ' <data/xjk@49.52.27.35/cpu_summary.csv | \
#  tr ',' ' ' <data/ly@49.52.27.35/cpu_summary.csv | \
     while read category value ; do
 	[ $category == "cpu_category" ] && continue
 	echo "    <tr>"
 	echo "      <td align=\"left\">${category}</td>"
 	echo "      <td align=\"right\">${value}</td>"
 	echo "    </tr>"
    done >>report.html

cat >>report.html <<_EOF_
    </table>
  </p>
  <br/>
  <br/>
  <p>
    <b>Note:</b>In the graph below the percentages for User, System and IOWait CPU time are stacked
    on top of each other. 

    <br/>
    <img src="data:image/svg+xml;base64,$(base64 cpu_utilization.svg)" />
  </p>

  <h3>
    Dirty Kernel Buffers
  </h3>
  <p>
    We track the number of dirty kernel buffers, as measured by
    the "nr_dirty" line in /proc/vmstat, to be able to correlate
    IO problems with when the kernel's IO schedulers are flushing
    writes to disk. A write(2) system call does not immediately
    cause real IO on a storage device. The data written is just
    copied into a kernel buffer. Several tuning parameters control
    when the OS is actually transferring these dirty buffers to
    the IO controller(s) in order to eventually get written to
    real disks (or similar). 

    <br/>
    <img src="data:image/svg+xml;base64,$(base64 dirty_buffers.svg)" />
  </p>
_EOF_

# ----
# Add all the block device IOPS and KBPS
# ---
for devdata in data/blk_*.csv ; do
    if [ ! -f "$devdata" ] ; then
        break
    fi

    dev=$(basename ${devdata} .csv)
    cat >>report.html <<_EOF_
    <h3>
      Block Device ${dev}
    </h3>
    <p>
      <img src="data:image/svg+xml;base64,$(base64 ${dev}_iops.svg)" />
      <br/>
      <img src="data:image/svg+xml;base64,$(base64 ${dev}_kbps.svg)" />
    </p>
_EOF_
done

# ----
# Add all the network device IOPS and KBPS
# ---
for devdata in data/net_*.csv ; do
    if [ ! -f "$devdata" ] ; then
        break
    fi

    dev=$(basename ${devdata} .csv)
    cat >>report.html <<_EOF_
    <h3>
      Network Device ${dev}
    </h3>
    <p>
      <img src="data:image/svg+xml;base64,$(base64 ${dev}_iops.svg)" />
      <br/>
      <img src="data:image/svg+xml;base64,$(base64 ${dev}_kbps.svg)" />
    </p>
_EOF_
done

# ----
# Finish the document.
# ----
cat >>report.html <<_EOF_
</body>
</html>

_EOF_

## ----
## Copy the report to the name of the result directory as .html
## We do the dirname-trick to strip any trailing / that might
## have resulted from tab-completion.
## ----
#cp report.html ../$(dirname ${1}/.dummy).html

echo "OK"
