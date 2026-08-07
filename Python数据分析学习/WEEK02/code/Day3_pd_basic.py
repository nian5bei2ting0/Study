# Day3 示例：Pandas 入门
# 运行：python code/Day3_pd_basic.py
import pandas as pd
import numpy as np

# Series
s = pd.Series([10, 20, 30], index=["a", "b", "c"])
print("Series:\n", s)
print("s['b']:", s["b"])
print("dict 创建:", pd.Series({"小明": 90, "小红": 85}))
print("describe:\n", s.describe())
print("s[s>15]:\n", s[s > 15])

# DataFrame
df = pd.DataFrame({
    "name": ["小明", "小红", "小刚"],
    "age": [18, 19, 20],
    "score": [90, 85, 78],
})
print("\nDataFrame:\n", df)
print("shape:", df.shape)
print("columns:", df.columns.tolist())
print("dtypes:\n", df.dtypes)
print("\ninfo:")
df.info()
print("\ndescribe:\n", df.describe())

# 选列
print("\n选列 name:\n", df["name"])
print("选多列:\n", df[["name", "age"]])

# 选行
print("iloc[0]:\n", df.iloc[0])
print("iloc[0:2, 1:3]:\n", df.iloc[0:2, 1:3])
print("loc[0:1, ['name','score']]:\n", df.loc[0:1, ["name", "score"]])

# 条件筛选
print("score>80:\n", df[df["score"] > 80])
print("多条件:\n", df[(df["score"] > 80) & (df["age"] < 19)])
print("isin:\n", df[df["name"].isin(["小明", "小红"])])
print("startswith:\n", df[df["name"].str.startswith("小")])

# 加列删行
df["city"] = ["北京", "上海", "广州"]
df["is_adult"] = df["age"] >= 18
df.loc[3] = ["小张", 21, 88, "深圳", True]
print("\n加列加行后:\n", df)

# 删
print("drop 列:\n", df.drop("city", axis=1))
print("drop 行:\n", df.drop(0, axis=0))

# 索引
df_idx = df.set_index("name")
print("set_index:\n", df_idx)
print("reset_index:\n", df_idx.reset_index())
