#!/bin/bash

# 循环执行的总时长（分钟）
total_duration=20

# 计算每次查询的间隔（秒）
interval=300

# 计算循环的次数
num_loops=$((total_duration * 60 / interval))
output_file="ob_table_row_counts.txt"
# 循环查询数据库表的行数
for ((i = 1; i <= num_loops; i++)); do
  echo "查询时间: $(date +"%Y-%m-%d %H:%M:%S")"

  # 连接到 PostgreSQL 数据库，并执行查询
  mysql -uroot@mysql -h49.52.27.20 -P2883 -D benchmarksql -e "SELECT SUM(row_count) AS total_row_count
                                                              FROM (
                                                              SELECT COUNT(*) AS row_count
                                                              FROM vodka_oorder
                                                              UNION ALL
                                                              SELECT COUNT(*) AS row_count
                                                              FROM vodka_order_line
                                                              ) counts;" >>"$output_file"

  if [ $i -lt $num_loops ]; then
    echo "等待 $interval 秒..."
    sleep $interval
  fi
done

echo "脚本执行完毕。"
