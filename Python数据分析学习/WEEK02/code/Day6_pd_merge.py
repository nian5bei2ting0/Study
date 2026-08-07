# Day6 示例：合并、透视、时间序列
# 运行：python code/Day6_pd_merge.py
import pandas as pd
import numpy as np

# merge
emp = pd.DataFrame({
    "emp_id": [1, 2, 3, 4],
    "name": ["小明", "小红", "小刚", "小李"],
    "dept_id": [10, 20, 10, 30],
})
dept = pd.DataFrame({
    "dept_id": [10, 20, 40],
    "dept_name": ["销售", "技术", "人事"],
})
print("inner:\n", pd.merge(emp, dept, on="dept_id", how="inner"))
print("\nleft:\n", pd.merge(emp, dept, on="dept_id", how="left"))
print("\nouter:\n", pd.merge(emp, dept, on="dept_id", how="outer"))

# concat
a = pd.DataFrame({"x": [1, 2], "y": [3, 4]})
b = pd.DataFrame({"x": [5, 6], "y": [7, 8]})
print("\nconcat 上下:\n", pd.concat([a, b], ignore_index=True))
print("\nconcat 左右:\n", pd.concat([a, b], axis=1))

# pivot_table
df = pd.DataFrame({
    "city": ["北京","上海"]*4,
    "product": ["A","A","B","B"]*2,
    "sales": [100, 200, 80, 90, 150, 180, 70, 110],
})
pivot = df.pivot_table(values="sales", index="city", columns="product", aggfunc="sum")
print("\npivot:\n", pivot)

# melt
wide = pivot.reset_index()
print("\nwide:\n", wide)
print("\nmelt:\n", wide.melt(id_vars="city", var_name="product", value_name="sales"))

# crosstab
print("\ncrosstab:\n", pd.crosstab(df["city"], df["product"], margins=True))

# 时间序列
idx = pd.date_range("2025-01-01", periods=30, freq="D")
s = pd.Series(np.random.randint(50, 200, 30), index=idx)
print("\n按周:\n", s.resample("W").sum())
print("\n7日均线:\n", s.rolling(7).mean().dropna().head())
print("\nshift 环比:\n", (s - s.shift(1)).head())

# apply / map
df2 = pd.DataFrame({"a": [1, 2, 3], "b": [4, 5, 6]})
print("\napply 按列:\n", df2.apply(sum))
print("\napply 按行:\n", df2.apply(sum, axis=1))
print("\nmap:\n", pd.Series([1, 2, 3]).map({1: "一", 2: "二"}))
