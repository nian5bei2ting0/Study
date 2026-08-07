# Day5 示例：EDA 方法论与数据画像
# 运行：python code/Day5_eda_method.py
import pandas as pd
import numpy as np
import seaborn as sns
import matplotlib.pyplot as plt

plt.rcParams["font.sans-serif"] = ["SimHei", "Microsoft YaHei"]
plt.rcParams["axes.unicode_minus"] = False
sns.set_theme(style="whitegrid")

# 用 tips 数据集演示
tips = sns.load_dataset("tips")

# ===== Step 1 数据概览 =====
print("shape:", tips.shape)
print(tips.info())
print(tips.describe())
print(tips.describe(include="object"))

# ===== Step 2 数据质量 =====
print("\n缺失:\n", tips.isnull().sum())
print("重复行:", tips.duplicated().sum())

# ===== 数据画像函数 =====
def data_profile(df):
    profile = {"行数": len(df), "列数": df.shape[1],
               "重复行": int(df.duplicated().sum())}
    col_df = pd.DataFrame([{
        "列名": c, "类型": str(df[c].dtype),
        "缺失率": f"{df[c].isnull().mean():.2%}",
        "唯一值数": df[c].nunique(),
        "示例": str(df[c].iloc[0]),
    } for c in df.columns])
    return profile, col_df

profile, col_df = data_profile(tips)
print("\n画像:", profile)
print(col_df)

# ===== 缺失可视化 =====
missing = tips.isnull().sum()
if missing.sum() > 0:
    missing[missing > 0].sort_values().plot.barh()
    plt.title("各列缺失数"); plt.show()

sns.heatmap(tips.isnull(), cbar=False, yticklabels=False, cmap="viridis")
plt.title("缺失值位置"); plt.show()

# ===== Step 3-4 单变量分布与异常 =====
print("\n账单偏度:", tips["total_bill"].skew())
print("账单峰度:", tips["total_bill"].kurt())

fig, axes = plt.subplots(1, 2, figsize=(12, 4))
sns.histplot(tips["total_bill"], bins=30, kde=True, ax=axes[0])
axes[0].set_title("账单分布")
sns.boxplot(x=tips["total_bill"], ax=axes[1])
axes[1].set_title("账单箱线图")
plt.show()

# 对数变换
tips["log_bill"] = np.log1p(tips["total_bill"])
fig, axes = plt.subplots(1, 2, figsize=(12, 4))
sns.histplot(tips["total_bill"], bins=30, ax=axes[0]); axes[0].set_title("原始")
sns.histplot(tips["log_bill"], bins=30, ax=axes[1]); axes[1].set_title("对数变换后")
plt.show()

# QQ plot
from scipy import stats
stats.probplot(tips["total_bill"], plot=plt)
plt.title("QQ plot"); plt.show()

# ===== Step 5 分类变量 =====
print("\n各天频数:\n", tips["day"].value_counts())
tips["day"].value_counts().plot.bar()
plt.title("各天就餐次数"); plt.show()

# ===== Step 6 多变量 =====
sns.pairplot(tips.select_dtypes("number"))
plt.show()

sns.heatmap(tips.select_dtypes("number").corr(), annot=True, cmap="coolwarm", center=0)
plt.title("相关性"); plt.show()
