# Day7 综合实战：电商销售数据分析
# 运行：python code/Day7_sales_analysis.py
# 详细讲解见 Day7_综合实战.md

import numpy as np
import pandas as pd

# ===== 1. 生成模拟数据 =====
rng = np.random.default_rng(42)
n = 1000
products = ["手机", "电脑", "耳机", "键盘", "鼠标", "显示器"]
regions = ["华北", "华东", "华南", "华中", "西南", "西北"]
channels = ["官网", "APP", "门店", "第三方"]

df = pd.DataFrame({
    "order_id": [f"O{i:06d}" for i in range(n)],
    "date": pd.date_range("2025-01-01", periods=n, freq="3H"),
    "product": rng.choice(products, n),
    "region": rng.choice(regions, n, p=[0.15, 0.3, 0.2, 0.15, 0.1, 0.1]),
    "channel": rng.choice(channels, n, p=[0.2, 0.5, 0.2, 0.1]),
    "quantity": rng.integers(1, 10, n),
    "unit_price": rng.choice([99, 199, 599, 1999, 4999, 8999], n),
})

# 造脏数据
df.loc[rng.choice(n, 30), "quantity"] = np.nan
df.loc[rng.choice(n, 20), "region"] = np.nan
df["unit_price"] = df["unit_price"].astype(str).str.replace("1999", "￥1999", regex=False)
df = pd.concat([df, df.sample(10)], ignore_index=True)
df.to_csv("sales_raw.csv", index=False, encoding="utf-8-sig")
print(f"生成原始数据 {len(df)} 行 -> sales_raw.csv")

# ===== 2. 读取与清洗 =====
df = pd.read_csv("sales_raw.csv", encoding="utf-8-sig")
print(f"\n原始：{df.shape}")

df = df.drop_duplicates()
df.columns = df.columns.str.strip().str.lower()
df["quantity"] = df["quantity"].fillna(df["quantity"].median())
df["region"] = df["region"].fillna("未知")
df["quantity"] = df["quantity"].astype(int)
df["unit_price"] = (
    df["unit_price"].astype(str).str.replace("￥", "", regex=False).astype(float)
)
df["date"] = pd.to_datetime(df["date"], errors="coerce")
df = df.dropna(subset=["date"])
df["amount"] = df["quantity"] * df["unit_price"]
df["month"] = df["date"].dt.to_period("M").astype(str)
df["weekday"] = df["date"].dt.day_name()
print(f"清洗后：{df.shape}")

# ===== 3. 描述性统计 =====
print("\n--- 金额统计 ---")
print(df["amount"].describe())

# ===== 4. 分组聚合 =====
by_product = df.groupby("product").agg(
    订单数=("order_id", "count"),
    总销量=("quantity", "sum"),
    总金额=("amount", "sum"),
    客单价=("amount", "mean"),
).reset_index().sort_values("总金额", ascending=False)
print("\n--- 各商品 ---")
print(by_product)

by_region = df.groupby("region").agg(
    总金额=("amount", "sum"),
    订单数=("order_id", "count"),
).reset_index().sort_values("总金额", ascending=False)
print("\n--- 各地区 ---")
print(by_region)

by_channel = df.groupby("channel")["amount"].sum().reset_index()
print("\n--- 各渠道 ---")
print(by_channel)

# ===== 5. 透视表 =====
pivot = df.pivot_table(values="amount", index="region", columns="product",
                       aggfunc="sum", fill_value=0)
print("\n--- 地区×商品 透视 ---")
print(pivot)

# ===== 6. 时间序列 =====
ts = df.set_index("date")["amount"]
monthly = ts.resample("M").sum()
print("\n--- 月度销售 ---")
print(monthly)

# ===== 7. Top 与异常 =====
print("\n--- Top 5 商品 ---")
print(by_product.head(5))

print("\n--- Top 3 地区 ---")
print(by_region.head(3))

threshold = df["amount"].quantile(0.99)
outliers = df[df["amount"] > threshold]
print(f"\n--- 异常大额订单（>{threshold:.0f}）{len(outliers)} 条 ---")
print(outliers[["order_id", "product", "region", "amount"]].head())

# ===== 8. 保存结果 =====
by_product.to_csv("result_by_product.csv", index=False, encoding="utf-8-sig")
by_region.to_csv("result_by_region.csv", index=False, encoding="utf-8-sig")
monthly.to_csv("result_monthly.csv", encoding="utf-8-sig")
print("\n结果已保存：result_by_product.csv / result_by_region.csv / result_monthly.csv")
