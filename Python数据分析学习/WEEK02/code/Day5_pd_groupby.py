# Day5 示例：筛选、排序、分组聚合
# 运行：python code/Day5_pd_groupby.py
import pandas as pd

df = pd.DataFrame({
    "name": ["小明","小红","小刚","小李","小王","小张","小陈","小刘"],
    "dept": ["销售","销售","技术","技术","销售","技术","人事","人事"],
    "city": ["北京","上海","北京","广州","上海","广州","北京","上海"],
    "salary": [8000, 9000, 15000, 13000, 8500, 16000, 7000, 7500],
    "years": [1, 2, 5, 3, 2, 6, 1, 2],
})

# 筛选
print("薪资>10000:\n", df[df["salary"] > 10000])
print("\n多条件:\n", df[(df["salary"] > 10000) & (df["city"] == "北京")])
print("\nisin:\n", df[df["city"].isin(["北京", "上海"])])
print("\nquery:\n", df.query("salary > 10000 and city == '北京'"))
print("\nstartswith:\n", df[df["name"].str.startswith("小")])

# 排序
print("\n按薪资降序:\n", df.sort_values("salary", ascending=False).head(3))
print("\n多列排序:\n", df.sort_values(["dept", "salary"], ascending=[True, False]))

# rank
df["rank"] = df["salary"].rank(ascending=False, method="dense")
print("\n排名:\n", df[["name", "salary", "rank"]])

# 分组聚合
print("\n按部门平均薪资:\n", df.groupby("dept")["salary"].mean())
print("\n多聚合:\n", df.groupby("dept")["salary"].agg(["mean", "max", "min", "count"]))

print("\n命名聚合:")
print(df.groupby("dept").agg(
    人数=("name", "count"),
    平均薪资=("salary", "mean"),
    最高薪资=("salary", "max"),
    平均工龄=("years", "mean"),
).reset_index())

# 多列分组
print("\n部门+城市:\n", df.groupby(["dept", "city"])["salary"].mean())

# transform
df["部门平均"] = df.groupby("dept")["salary"].transform("mean")
print("\ntransform:\n", df[["name", "dept", "salary", "部门平均"]])

# idxmax
print("\n每城市薪资最高:\n", df.loc[df.groupby("city")["salary"].idxmax()])
