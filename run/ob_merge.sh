#!/bin/bash

# 设置MySQL连接参数
MYSQL_USER="root@sys"
MYSQL_HOST="49.52.27.33"
MYSQL_PORT="2887"
MYSQL_DATABASE="oceanbase"

# SQL语句
SQL_STATEMENT="ALTER SYSTEM MAJOR FREEZE TENANT = mysql;"

# 使用mysql客户端执行SQL语句
mysql -u"${MYSQL_USER}" -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" -D"${MYSQL_DATABASE}" -e"${SQL_STATEMENT}"

