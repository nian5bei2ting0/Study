# Day4 示例：数据读取与清洗
# 运行：python code/Day4_pd_clean.py
import pandas as pd
import numpy as np

# 造脏数据
df = pd.DataFrame({
    "name": ["小明", "小红", "小明", "小刚", None],
    "age": [18, None, 18, 20, 19],
    "price": ["￥99", "￥199", "￥99", "￥299", "￥99"],
    "city": ["北京", "上海", "北京", "广州", "北京"],
    "date": ["2025-01-01", "2025-02-01", "2025-01-01", "2025-03-01", "2025-01-01"],
})
df.to_csv("dirty.csv", index=False, encoding="utf-8-sig")

# 读取
df = pd.read_csv("dirty.csv", encoding="utf-8-sig")
print("原始：", df.shape)
print("缺失率:\n", df.isnull().mean())
print("重复行数:", df.duplicated().sum())

# 清洗
df = df.drop_duplicates()
df.columns = df.columns.str.strip().str.lower()
df["name"] = df["name"].fillna("匿名")
df["age"] = df["age"].fillna(df["age"].median()).astype(int)
df["price"] = df["price"].str.replace("￥", "").astype(float)
df["city"] = df["city"].fillna("未知")
df["date"] = pd.to_datetime(df["date"], errors="coerce")
df = df.dropna(subset=["date"])

# 衍生列
df["year"] = df["date"].dt.year
df["month"] = df["date"].dt.month
df["weekday"] = df["date"].dt.day_name()

print("清洗后：\n", df)

# 内存对比
print("city 内存(object):", df["city"].astype(object).memory_usage(deep=True))
df["city"] = df["city"].astype("category")
print("city 内存(category):", df["city"].memory_usage(deep=True))

# 保存
df.to_csv("clean.csv", index=False, encoding="utf-8-sig")
print("已保存 clean.csv")
