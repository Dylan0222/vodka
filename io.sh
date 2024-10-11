#!/bin/bash
#P地址和登录凭证
if [ $# -ne 1 ]; then
    echo "Usage: $0 <DB>"
    exit 1
fi
remote_hosts=("49.52.27.33" "49.52.27.34" "49.52.27.35")
username="xjk"
password="qwer"  # 或者使用密钥登录，将密码设为""并确保已配置SSH密钥登录
db="$1"
# 定义要在远程机器上运行的脚本命令
remote_script="bash io.sh $db"  # 请替换 your_script.sh 和 your_db_name

# 循环连接远程机器并运行脚本
for host in "${remote_hosts[@]}"; do
    sshpass -p "$password" ssh "$username@$host" "$remote_script" &
done

# 等待所有远程任务完成
wait
