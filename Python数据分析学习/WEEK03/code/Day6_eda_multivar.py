# Day6 示例：多变量探索与异常检测
# 运行：python code/Day6_eda_multivar.py
import pandas as pd
import numpy as np
import seaborn as sns
import matplotlib.pyplot as plt

plt.rcParams["font.sans-serif"] = ["SimHei", "Microsoft YaHei"]
plt.rcParams["axes.unicode_minus"] = False
sns.set_theme(style="whitegrid")

tips = sns.load_dataset("tips")
num = tips.select_dtypes(include="number")

# ===== 相关性 =====
print("Pearson:\n", num.corr())
print("\nSpearman:\n", num.corr(method="spearman"))

sns.heatmap(num.corr(), annot=True, cmap="coolwarm", center=0, fmt=".2f",
            square=True, vmin=-1, vmax=1)
plt.title("相关性矩阵"); plt.show()

# ===== pairplot =====
sns.pairplot(tips, hue="time", vars=["total_bill","tip","size"], diag_kind="kde")
plt.show()

# ===== 分组对比 =====
sns.boxplot(data=tips, x="day", y="total_bill", hue="time")
plt.title("各天账单分布"); plt.show()

sns.violinplot(data=tips, x="day", y="total_bill", hue="sex", split=True)
plt.title("各天账单（按性别）"); plt.show()

sns.scatterplot(data=tips, x="total_bill", y="tip", hue="time", style="sex", size="size")
plt.title("账单 vs 小费"); plt.show()

# 多维分面
g = sns.FacetGrid(tips, col="day", row="time", hue="sex", height=3)
g.map_dataframe(sns.scatterplot, x="total_bill", y="tip")
g.add_legend()
plt.show()

# ===== 异常检测 =====
# IQR
q1, q3 = tips["total_bill"].quantile([0.25, 0.75])
iqr = q3 - q1
outliers = tips[(tips["total_bill"] < q1-1.5*iqr) | (tips["total_bill"] > q3+1.5*iqr)]
print(f"IQR 异常：{len(outliers)} 条")

plt.scatter(tips.index, tips["total_bill"], alpha=0.5, label="正常")
plt.scatter(outliers.index, outliers["total_bill"], color="red", s=60, label="异常")
plt.legend(); plt.title("IQR 异常点"); plt.show()

# 3σ
mean, std = tips["tip"].mean(), tips["tip"].std()
out_3s = tips[(tips["tip"]-mean).abs() > 3*std]
print(f"3σ 异常：{len(out_3s)} 条")

tips["z"] = (tips["tip"] - mean) / std
sns.histplot(tips["z"], bins=30)
plt.axvline(3, color="red", linestyle="--")
plt.axvline(-3, color="red", linestyle="--")
plt.title("Z-score 分布"); plt.show()

# ===== 二维密度 =====
sns.kdeplot(data=tips, x="total_bill", y="tip", fill=True, cmap="Blues")
plt.title("二维密度"); plt.show()

sns.jointplot(data=tips, x="total_bill", y="tip", kind="kde")
plt.show()

# ===== 时间序列异常 =====
idx = pd.date_range("2025-01-01", periods=60, freq="D")
sales = pd.Series(np.sin(np.arange(60)/5)*50 + 100 + np.random.normal(0,5,60), index=idx)
sales.iloc[20] = 200
plt.figure(figsize=(12,4))
plt.plot(sales)
plt.scatter([sales.index[20]], [sales.iloc[20]], color="red", s=80, label="异常")
plt.legend(); plt.title("时间序列异常点"); plt.show()

# ===== 变量重要性 =====
corr_tip = num.corr()["tip"].drop("tip").sort_values(ascending=False)
corr_tip.plot.bar(); plt.title("与 tip 相关性"); plt.show()
