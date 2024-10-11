import os

import numpy as np
import pandas as pd
import statsmodels.api as sm
from sklearn.linear_model import HuberRegressor
from sklearn.pipeline import make_pipeline
from sklearn.metrics import mean_squared_error, r2_score
from sklearn.preprocessing import PolynomialFeatures

np.random.seed(2023)
files = ['tpchresult-3.csv', 'tpchresult-4.csv', 'tpchresult-5.csv', 'tpchresult-6.csv']
file_path = os.getcwd() + "/result/delay/"

query_number = 23
sql_path_list = ['tpchSQL/1.sql', 'tpchSQL/2.sql', 'tpchSQL/3.sql', 'tpchSQL/4.sql', 'tpchSQL/5.sql', 'tpchSQL/6.sql',
                 'tpchSQL/7.sql', 'tpchSQL/8.sql', 'tpchSQL/9.sql', 'tpchSQL/10.sql', 'tpchSQL/11.sql',
                 'tpchSQL/12.sql',
                 'tpchSQL/13.sql', 'tpchSQL/14.sql', 'tpchSQL/15.sql', 'tpchSQL/16.sql', 'tpchSQL/17.sql',
                 'tpchSQL/18.sql',
                 'tpchSQL/19.sql', 'tpchSQL/20.sql', 'tpchSQL/21.sql', 'tpchSQL/22.sql', 'tpchSQL/23.sql']
all_fit_result_dict = {}
all_query_delay_list = {}
all_query_line_list = {}
all_delay_result = {}
all_query_count_result = {}
time = 30 * 60
targetTpmC = 100_000

def transform_list(lst):
    new_list = [[lst[j][i] for j in range(len(lst))] for i in range(len(lst[0]))]
    return new_list

def curve_fit(X, Y, i, dbtype):
    # 创建新的转换器和估计器对象
    poly = PolynomialFeatures(degree=2)
    huber = HuberRegressor()
    model = make_pipeline(poly, huber)
    res_wls = model.fit(X, Y)
    print('{0}: Current Query Type is: Q{1}'.format(str.upper(dbtype), i + 1))
    return res_wls


def print_fit_func(model, dimension=1):
    if dimension == 1:
        w = model[0]
        b = model[1]
        print('y = {0} * x + ({1});'.format(w, b))
    else:
        w1 = model[0]
        w2 = model[1]
        b = model[2]
        print('y = {0} * x^2 + ({1} * x) + ({2});'.format(w1, w2, b))


def check_db_type(i):
    i = int(i)
    if i == 3:
        return "PG"
    elif i == 4:
        return "OceanBase"
    elif i == 5:
        return "TiDB"
    elif i == 6:
        return "PolarDB"
    else:
        return "None"


def test_add_line(df):
    start_number = 5000
    start_index = 0
    size = len(df)
    counter = 1
    while (start_index < size):
        df.loc[start_index:, 'line'] = start_number
        start_number += 100
        start_index = start_index + 1
        counter = counter + 1
        if counter == 23:
            start_number = start_number + 1000
            counter = 0
    return df


def print_fit_func4huber(model):
    huber_regressor = model.named_steps['huberregressor']
    poly_features = model.named_steps['polynomialfeatures']
    coef = huber_regressor.coef_
    powers = poly_features.powers_
    # 输出拟合模型
    equation = f"Query {i + 1}'s Model is ：y = "
    for power, coeff in zip(powers, coef):
        equation += f"{coeff} * x^{int(power.sum())} + "
    equation = equation[:-3]
    print(equation)


if __name__ == '__main__':
    single_query_list = [[] for i in range(4)]
    cnt = 0
    for file in files:
        path = os.path.join(file_path, file)
        if path.endswith('.csv'):
            data = pd.read_csv(path)
            data = test_add_line(data)
            dbtype = check_db_type(file.split('-')[1][0])
            query_delay_list = [[] for i in range(query_number)]
            query_line_list = [[] for i in range(query_number)]
            fit_result_list = []
            query_result = 0
            qps = 0
            query_count = 0
            for index, row in data.iterrows():
                for i in range(len(sql_path_list)):
                    if sql_path_list[i].strip() == row['path'].strip():
                        query_delay_list[i].append(row['delay'])
                        query_line_list[i].append(row['line'])
            for i in range(query_number):
                x = query_line_list[i]
                # X = sm.add_constant(x)
                X = np.array(x).reshape(-1, 1)
                Y = query_delay_list[i]
                model = curve_fit(X, Y, i, dbtype)
                fit_result_list.append(model)
                # print_fit_func(model.params)
                # 输出拟合参数
                print_fit_func4huber(model)
                max_line_number = x[-1]
                min_line_number = x[0]
                mid_line_number = max_line_number + min_line_number // 2
                predict_x = [1, mid_line_number]
                # predict_X = sm.add_constant(predict_x)
                predict_X = np.array([predict_x[1]]).reshape(-1, 1)
                delay_value = model.predict(predict_X)
                # query_result += delay_value[1]
                query_result += delay_value
                # 计算均方误差和R²得分
                mse = mean_squared_error(Y, model.predict(X))
                r2 = r2_score(Y, model.predict(X))
                single_query_list[cnt].append(delay_value[0])
                print("均方误差 (MSE): {:.2f}".format(mse))
                print("R²得分: {:.2f}".format(r2))
                print('Expected Query {0} Execution Time is: {1} ms, Accumulated Time is: {2} ms;\n'.format(i + 1,
                                                                                                            delay_value,
                                                                                                            query_result))
            query_result = query_result / 1000
            qphH = 60 * 60 * query_number / query_result
            query_count = time / query_result * query_number
            all_fit_result_dict[dbtype] = fit_result_list
            all_query_line_list[dbtype] = query_line_list
            all_query_delay_list[dbtype] = query_delay_list
            all_delay_result[dbtype] = qphH
            all_query_count_result[dbtype] = query_count
            cnt = cnt + 1
    print("Under the target {0}tpmC, final QphH result is:{1}, query_count is:{2} ".format(targetTpmC, all_delay_result,
                                                                                           all_query_count_result))
    print(transform_list(single_query_list))
