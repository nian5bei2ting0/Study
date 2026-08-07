# Day 7 · 综合实战：电商销售数据分析

> 今日目标：用全周所学完成一个完整的电商销售数据分析项目。
> 预计时间：2.5 小时

---

## 一、项目说明

模拟一份电商销售数据，从原始数据出发，完成：

1. 数据生成与读取
2. 数据清洗（缺失、重复、类型）
3. 描述性统计
4. 分组聚合分析（按商品/地区/月份）
5. 透视表分析
6. 时间序列趋势
7. 找出 Top 商品、Top 地区、异常订单
8. 生成一份简明分析报告

涉及知识点：NumPy 数组运算 + Pandas 全套（读写/清洗/筛选/分组/透视/合并/时间序列）——**全周覆盖**。

---

## 二、数据生成

先造一份接近真实的销售数据：

```python
import numpy as np
import pandas as pd

rng = np.random.default_rng(42)

n = 1000
products = ["手机", "电脑", "耳机", "键盘", "鼠标", "显示器"]
regions = ["华北", "华东", "华南", "华中", "西南", "西北"]
channels = ["官网", "APP", "门店", "第三方"]

df = pd.DataFrame({
    "order_id": [f"O{i:06d}" for i in range(n)],
    "date": pd.date_range("2025-01-01", periods=n, freq="3H"),
    "product": rng.choice(products, n),
    "region": rng.choice(regions, n, p=[0.15,0.3,0.2,0.15,0.1,0.1]),
    "channel": rng.choice(channels, n, p=[0.2,0.5,0.2,0.1]),
    "quantity": rng.integers(1, 10, n),
    "unit_price": rng.choice([99, 199, 599, 1999, 4999, 8999], n),
})

# 故意造点脏数据
df.loc[rng.choice(n, 30), "quantity"] = np.nan          # 缺失
df.loc[rng.choice(n, 20), "region"] = np.nan            # 缺失
df["unit_price"] = df["unit_price"].astype(str).str.replace("1999", "￥1999", regex=False)
# 重复行
df = pd.concat([df, df.sample(10)], ignore_index=True)

df.to_csv("sales_raw.csv", index=False, encoding="utf-8-sig")
print(f"生成 {len(df)} 行原始数据")
```

---

## 三、完整分析代码（先看懂，再自己敲）

```python
import numpy as np
import pandas as pd

# ===== 1. 读取 =====
df = pd.read_csv("sales_raw.csv", encoding="utf-8-sig")
print(f"原始数据：{df.shape}")
print(df.info())

# ===== 2. 清洗 =====
# 2.1 去重
df = df.drop_duplicates()
# 2.2 列名规范化
df.columns = df.columns.str.strip().str.lower()
# 2.3 缺失值
df["quantity"] = df["quantity"].fillna(df["quantity"].median())
df["region"] = df["region"].fillna("未知")
# 2.4 类型转换
df["quantity"] = df["quantity"].astype(int)
df["unit_price"] = (
    df["unit_price"].astype(str).str.replace("￥", "", regex=False).astype(float)
)
# 2.5 日期
df["date"] = pd.to_datetime(df["date"], errors="coerce")
df = df.dropna(subset=["date"])
# 2.6 衍生列
df["amount"] = df["quantity"] * df["unit_price"]
df["month"] = df["date"].dt.to_period("M").astype(str)
df["weekday"] = df["date"].dt.day_name()

print(f"清洗后：{df.shape}")

# ===== 3. 描述性统计 =====
print("\n--- 金额统计 ---")
print(df["amount"].describe())

# ===== 4. 分组聚合 =====
# 4.1 按商品
by_product = df.groupby("product").agg(
    订单数=("order_id", "count"),
    总销量=("quantity", "sum"),
    总金额=("amount", "sum"),
    客单价=("amount", "mean"),
).reset_index().sort_values("总金额", ascending=False)
print("\n--- 各商品 ---")
print(by_product)

# 4.2 按地区
by_region = df.groupby("region").agg(
    总金额=("amount", "sum"),
    订单数=("order_id", "count"),
).reset_index().sort_values("总金额", ascending=False)
print("\n--- 各地区 ---")
print(by_region)

# 4.3 按渠道
by_channel = df.groupby("channel")["amount"].sum().reset_index()
print("\n--- 各渠道 ---")
print(by_channel)

# ===== 5. 透视表：地区 × 商品 =====
pivot = df.pivot_table(values="amount", index="region", columns="product",
                       aggfunc="sum", fill_value=0)
print("\n--- 地区×商品 透视 ---")
print(pivot)

# ===== 6. 时间序列 =====
ts = df.set_index("date")["amount"]
monthly = ts.resample("M").sum()
print("\n--- 月度销售 ---")
print(monthly)

# 7日移动平均
daily = ts.resample("D").sum()
ma7 = daily.rolling(7).mean()

# ===== 7. Top 与异常 =====
print("\n--- Top 5 商品 ---")
print(by_product.head(5))

print("\n--- Top 3 地区 ---")
print(by_region.head(3))

# 异常订单：金额超过 99 分位的
threshold = df["amount"].quantile(0.99)
outliers = df[df["amount"] > threshold]
print(f"\n--- 异常大额订单（>{threshold:.0f}）{len(outliers)} 条 ---")
print(outliers[["order_id","product","region","amount"]].head())

# ===== 8. 保存结果 =====
by_product.to_csv("result_by_product.csv", index=False, encoding="utf-8-sig")
by_region.to_csv("result_by_region.csv", index=False, encoding="utf-8-sig")
monthly.to_csv("result_monthly.csv", encoding="utf-8-sig")
print("\n结果已保存：result_by_product.csv / result_by_region.csv / result_monthly.csv")
```

---

## 四、知识点对照

| 代码段 | 用到的知识（哪一天） |
|--------|-------------------|
| `np.random.default_rng` | Day1/2 NumPy 随机 |
| `pd.read_csv / to_csv` | Day4 读写 |
| `drop_duplicates / fillna / astype` | Day4 清洗 |
| `str.replace` 清理价格 | Day4 字符串清理 |
| `to_datetime / dt.to_period` | Day4 日期 |
| `df[df["amount"] > x]` | Day5 筛选 |
| `sort_values` | Day5 排序 |
| `groupby / agg` 命名聚合 | Day5 分组 |
| `pivot_table` | Day6 透视 |
| `resample / rolling` | Day6 时间序列 |
| `quantile` 异常检测 | Day2 统计 |

---

## 五、运行方式

```bash
python code/Day7_sales_analysis.py
```

会生成 `sales_raw.csv`（原始数据）和三个 `result_*.csv`（分析结果）。

---

## 六、进阶挑战（可选）

1. **画图**：用 matplotlib 画月度销售折线图、各商品销售额柱状图、地区占比饼图。
2. **RFM 分析**：按最近购买(R)、频次(F)、金额(M) 给用户分层。
3. **同比环比**：算每月销售额的环比增长率、同比（需造去年数据）。
4. **异常检测**：用 3σ 原则或 IQR 找异常订单。
5. **多表合并**：再加一张"用户表"，merge 后分析不同用户群体的购买偏好。

> 能做完 2 个进阶，说明你已经具备独立做数据分析的能力了。

---

## 每日练习（必做）

1. 把上面的代码**完整敲一遍**并运行成功。
2. 至少完成 1 个进阶挑战（推荐画图）。
3. 用自己的话写一段 200 字的"分析结论"：这份数据告诉你什么业务洞察？

---

## 今日小结

- ✅ 用一个真实项目把全周知识串起来
- ✅ 数据生成 → 清洗 → 统计 → 分组 → 透视 → 时间序列 → 异常检测
- ✅ 结果落地为 csv，可复用
- ✅ 找到 Top 商品/地区、识别异常订单

---

# 🎉 恭喜完成第二周！

你现在掌握了 Python 数据分析的两大基石。建议接下来：
- **WEEK03**（后续开）：数据可视化（matplotlib / seaborn）+ EDA 实战
- **方向选择**：金融数据分析 / 用户行为分析 / 业务 BI 报表 / 机器学习预处理

无论选哪个方向，本周的 NumPy + Pandas 都是地基，继续加油！
