import matplotlib.pyplot as plt
import time
from matplotlib.ticker import FuncFormatter
import matplotlib.lines as mlines
import os
import numpy as np
import datetime
from datetime import datetime as datetime2

# DRAW_TYPE is used to determine x-axis, 1: time, 2: txn num
DRAW_TYPE = 2
BASE_DIR = "/home/xjk/hzr/Vodka-Benchmark/run"
DIR_NAME = "/result/delay"
CSV_FILE_DIR = BASE_DIR + DIR_NAME + "/"
# CSV_FILE_NAME = ['tpchresult-3.csv']
# CSV_FILE_NAME = ['tpchresult-4.csv']
# CSV_FILE_NAME = ['tpchresult-5.csv']
# CSV_FILE_NAME = ['tpchresult-4.csv', 'tpchresult-5.csv']
CSV_FILE_NAME = ['tpchresult-3.csv', 'tpchresult-4.csv', 'tpchresult-5.csv']
OFFSET = 220
RIGHTOFFSET = 60
QUERYNUMBER = 23
WAREHOUSE = 120
TargetTps = 1000
TPTHREADS = 400
APTHREADS = 1
threshold = 2000
fontsize = '10'
labelsize = 7
num_ticks = 5  # 设置y轴刻度的数量
pg_color = '#96C120'
ob_color = '#22BFCF'
tidb_color = '#EC7488'
polardb_color = '#A360AD'


# 自定义格式化函数
def format_y_ticks(y, _):
    if y >= 1000:
        return f'{y / 1000:.1f}K'
    else:
        return y


# 自定义格式化函数
def format_x_ticks(x, _):
    if x >= 1000:
        return f'{x / 1000}K'
    else:
        return x


def generate_result_txn_num():
    print("[read csv data]")
    total_data = []
    for csv_file_name in CSV_FILE_NAME:
        csv_file_path = CSV_FILE_DIR + csv_file_name
        headers = ""
        data = [[] for _ in range(QUERYNUMBER + 2)]
        with open(csv_file_path) as f:
            # headers = next(f)
            # print(headers)
            for row in f:
                cur = row.split(',')
                query_id = int(cur[1])
                date = cur[0][:19]
                try:
                    timeArray = time.strptime(date, "%Y-%m-%d %H:%M:%S")
                except:
                    print("date = ", date)
                start_time = time.mktime(timeArray)
                latency = round((float(cur[3])), 1)
                new_order = int(cur[4])
                payment = int(cur[5])
                data[query_id].append([start_time, latency, new_order + payment])
        total_data.append(data)

    print("[analyse]")
    avg_latencys = [[] for _ in range(QUERYNUMBER + 1)]
    for data in total_data:
        for i in range(1, QUERYNUMBER + 1):
            data[i].sort(key=lambda k: k[2])
        latencys = [0 for _ in range(QUERYNUMBER + 1)]
        for i in range(1, QUERYNUMBER + 1):
            for d in data[i]:
                latencys[i] += d[1]
        for i in range(1, QUERYNUMBER + 1):
            avg_latencys[i].append(latencys[i] / len(data[i]))

    print("[draw]")
    plt.rc('font', size='5', weight='bold')
    fig, ax = plt.subplots(5, 5, figsize=(14, 8))
    set_flag = False
    colors = [pg_color, ob_color, tidb_color, polardb_color]
    legends = ['PG', 'OB', 'TiDB', 'PolarDB']
    pos = 0
    # 假设您已经计算了每个CSV文件的height列表
    height = [[0 for _ in range(QUERYNUMBER + 1)] for _ in range(len(CSV_FILE_NAME))]
    max_differences = []

    for k, data in enumerate(total_data):
        l = [len(data[i]) for i in range(QUERYNUMBER + 1)]
        start_time = [data[i][0][0] if len(data[i]) > 0 else 0 for i in range(0, QUERYNUMBER + 1)]
        end_time = [data[i][-1][0] if len(data[i]) > 0 else 0 for i in range(0, QUERYNUMBER + 1)]
        for i in range(1, QUERYNUMBER + 1):
            p = 0
            while p < l[i]:
                if data[i][p][0] - start_time[i] >= OFFSET and end_time[i] - data[i][p][0] >= RIGHTOFFSET:
                    height[k][i] = max(height[k][i], data[i][p][1])
                p += 1
    for i in range(1, QUERYNUMBER + 1):
        query_heights = [height[j][i] for j in range(len(CSV_FILE_NAME))]
        max_difference = np.max(query_heights) - np.min(query_heights)
        max_differences.append(max_difference)

    for k, data in enumerate(total_data):
        l = [len(data[i]) for i in range(QUERYNUMBER + 1)]
        x = [[] for _ in range(QUERYNUMBER + 1)]
        y = [[] for _ in range(QUERYNUMBER + 1)]
        start_time = [data[i][0][0] if len(data[i]) > 0 else 0 for i in range(0, QUERYNUMBER + 1)]
        end_time = [data[i][-1][0] if len(data[i]) > 0 else 0 for i in range(0, QUERYNUMBER + 1)]
        print(end_time)
        for i in range(1, QUERYNUMBER + 1):
            p = 0
            while p < l[i]:
                if data[i][p][0] - start_time[i] >= OFFSET and end_time[i] - data[i][p][0] >= RIGHTOFFSET:
                    x[i].append(data[i][p][2] / 1000)
                    y[i].append(data[i][p][1])
                p += 1

        idx = 1
        titles = ['Q' + str(i) for i in range(0, QUERYNUMBER + 1)]
        wen = ['Q2', 'Q11', 'Q16']
        has_date = ['Q1', 'Q3', 'Q4', 'Q5', 'Q6', 'Q7', 'Q8', 'Q10', 'Q12', 'Q14', 'Q20']
        for i in range(5):
            for j in range(5):
                if idx > QUERYNUMBER:
                    ax[i, j].set_visible(False)
                    continue
                if max_differences[idx - 1] > threshold:
                    # 获取四条线中最小的两个height
                    sorted_heights = sorted([height[k][idx] for k in range(len(CSV_FILE_NAME))])
                    left_ylim = max(sorted_heights[:2]) + max(sorted_heights[:2]) // 2
                    right_ylim = sorted_heights[-1] + sorted_heights[-1] // 2
                    ax[i, j].set_ylim(0, left_ylim)
                    ax2 = ax[i, j].twinx()
                    ax2.set_ylim(0, right_ylim)
                    ymin, ymax = ax[i, j].get_ylim()
                    ax[i, j].set_yticks(np.linspace(0, ymax, num_ticks))
                    ymin, ymax = ax2.get_ylim()
                    ax2.set_yticks(np.linspace(0, ymax, num_ticks))
                    ax2.yaxis.set_major_formatter(FuncFormatter(format_y_ticks))
                    ax2.tick_params(axis='both', labelsize=labelsize)  # 更改刻度字体大小
                    # 绘制左侧y轴对应的线
                    for line_idx, y_value in enumerate(y[idx]):
                        if y_value <= left_ylim:
                            ax[i, j].plot(x[idx], y[idx], color=colors[k], linewidth=0.5, linestyle='-',
                                          label=legends[pos], marker=".")
                        else:
                            ax2.plot(x[idx], y[idx], color=colors[k], linewidth=0.5, linestyle='-', label=legends[pos],
                                     marker=".")
                else:
                    max_height = max([height[k][idx] for k in range(len(CSV_FILE_NAME))])
                    ax[i, j].set_ylim(0, max_height + max_height // 2)
                    ymin, ymax = ax[i, j].get_ylim()
                    ax[i, j].set_yticks(np.linspace(0, ymax, num_ticks))
                    ax[i, j].yaxis.set_major_formatter(FuncFormatter(format_y_ticks))
                    ax[i, j].plot(x[idx], y[idx], color=colors[k], linewidth=0.5, linestyle='-', label=legends[pos],
                                  marker=".")
                ax[i, j].tick_params(axis='both', labelsize=labelsize)  # 更改刻度字体大小
                ax[i, j].yaxis.set_major_formatter(FuncFormatter(format_y_ticks))
                ax[i, j].xaxis.set_major_formatter(FuncFormatter(format_x_ticks))
                ax[i, j].set_title(label=titles[idx], color='black', size=fontsize, weight="bold")  # 数据规模不变化
                ax[i, j].set_xlabel(xlabel='Txn', size=fontsize, weight="bold")
                ax[i, 0].set_ylabel(ylabel='Latency(ms)', size=fontsize, weight="bold")
                idx += 1
        plt.subplots_adjust(left=None, bottom=None, right=None, top=None, wspace=0.7, hspace=0.9)
        pos += 1

    idx = 1
    title = 'Experiment under ' + str(WAREHOUSE) + ' warehouses, ' + str(TargetTps) + ' targetTps, ' + str(
        TPTHREADS) + ' tp threads, ' + str(
        APTHREADS) + ' ap threads'
    fig.suptitle(title, fontsize=13)
    pg = mlines.Line2D([], [], color='#32A852', label='Postgres')
    ob = mlines.Line2D([], [], color='#6EB1DE', label='OceanBase')
    tidb = mlines.Line2D([], [], color='#EC7488', label='TiDB')
    polardb = mlines.Line2D([], [], color='#A360AD', label='PolarDB')
    legend_ax = fig.add_subplot(5, 5, 24)
    legend_ax.set_axis_off()
    legend_ax.legend(handles=[pg, ob, tidb, polardb], loc='center', fontsize=11)

    if (len(CSV_FILE_NAME) >= 2):
        plt.savefig("vodka_result/query-all.png", dpi=200)
        plt.savefig("vodka_result/query-all.pdf", dpi=200)
    else:
        plt.savefig("vodka_result/query-single.png", dpi=200)
        plt.savefig("vodka_result/query-single.pdf", dpi=200)
    plt.show()


def generate_legend():
    pg = mlines.Line2D([], [], color=pg_color, label='Postgres')
    ob = mlines.Line2D([], [], color=ob_color, label='OceanBase')
    tidb = mlines.Line2D([], [], color=tidb_color, label='TiDB')
    polardb = mlines.Line2D([], [], color=polardb_color, label='PolarDB')
    fig, ax = plt.subplots(figsize=(0.5, 0.5))
    ax.axis('off')  # 隐藏坐标轴
    ax.legend(handles=[pg, ob, tidb, polardb], loc='center')
    plt.savefig('legend.pdf', bbox_inches='tight')
    plt.show()


if __name__ == "__main__":
    generate_result_txn_num()
    generate_legend()
