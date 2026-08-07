# Day7 综合实战：完整 EDA 报告
# 运行：python code/Day7_eda_report.py
# 详细讲解见 Day7_综合实战.md
import pandas as pd
import numpy as np
import seaborn as sns
import matplotlib.pyplot as plt

plt.rcParams["font.sans-serif"] = ["SimHei", "Microsoft YaHei"]
plt.rcParams["axes.unicode_minus"] = False
sns.set_theme(style="whitegrid", palette="pastel")

# ===== 1. 加载与概览 =====
tips = sns.load_dataset("tips")
print(f"数据规模：{tips.shape}")
print(tips.info())
print(tips.head())

def data_profile(df):
    return pd.DataFrame([{
        "列名": c, "类型": str(df[c].dtype),
        "缺失率": f"{df[c].isnull().mean():.2%}",
        "唯一值数": df[c].nunique(),
        "示例": str(df[c].iloc[0]),
    } for c in df.columns])

print("\n数据画像：")
print(data_profile(tips))

# ===== 2. 数据质量 =====
print(f"\n缺失总数：{tips.isnull().sum().sum()}")
print(f"重复行数：{tips.duplicated().sum()}")
tips = tips.drop_duplicates()

# ===== 3. 单变量分布 =====
fig, axes = plt.subplots(2, 2, figsize=(14, 10))
sns.histplot(tips["total_bill"], bins=30, kde=True, ax=axes[0,0])
axes[0,0].set_title("账单金额分布")
sns.histplot(tips["tip"], bins=30, kde=True, ax=axes[0,1])
axes[0,1].set_title("小费金额分布")
sns.countplot(data=tips, x="size", ax=axes[1,0])
axes[1,0].set_title("就餐人数分布")
sns.countplot(data=tips, x="day", ax=axes[1,1])
axes[1,1].set_title("各天就餐次数")
plt.tight_layout()
plt.savefig("fig1_univariate.png", dpi=120, bbox_inches="tight")
plt.show()

# ===== 4. 异常值识别 =====
fig, axes = plt.subplots(1, 2, figsize=(12, 4))
sns.boxplot(x=tips["total_bill"], ax=axes[0]); axes[0].set_title("账单箱线图")
sns.boxplot(x=tips["tip"], ax=axes[1]); axes[1].set_title("小费箱线图")
plt.tight_layout()
plt.savefig("fig2_outliers.png", dpi=120, bbox_inches="tight")
plt.show()

q1, q3 = tips["total_bill"].quantile([0.25, 0.75])
iqr = q3 - q1
bill_outliers = tips[(tips["total_bill"] < q1-1.5*iqr) | (tips["total_bill"] > q3+1.5*iqr)]
print(f"\n账单异常（IQR法）：{len(bill_outliers)} 条")

# ===== 5. 账单 vs 小费 =====
fig, axes = plt.subplots(1, 2, figsize=(14, 5))
sns.scatterplot(data=tips, x="total_bill", y="tip", hue="time", ax=axes[0])
axes[0].set_title("账单 vs 小费")
sns.regplot(data=tips, x="total_bill", y="tip", scatter_kws={"alpha":0.5}, ax=axes[1])
axes[1].set_title("账单 vs 小费（含回归线）")
plt.tight_layout()
plt.savefig("fig3_bivariate.png", dpi=120, bbox_inches="tight")
plt.show()

# ===== 6. 分组对比 =====
fig, axes = plt.subplots(2, 2, figsize=(14, 10))
sns.boxplot(data=tips, x="day", y="total_bill", ax=axes[0,0])
axes[0,0].set_title("各天账单分布")
sns.violinplot(data=tips, x="time", y="tip", ax=axes[0,1])
axes[0,1].set_title("午餐/晚餐小费分布")
sns.boxplot(data=tips, x="day", y="tip", hue="sex", ax=axes[1,0])
axes[1,0].set_title("各天小费（按性别）")
sns.barplot(data=tips, x="day", y="total_bill", hue="smoker", ax=axes[1,1])
axes[1,1].set_title("各天平均账单（吸烟与否）")
plt.tight_layout()
plt.savefig("fig4_grouped.png", dpi=120, bbox_inches="tight")
plt.show()

# ===== 7. 相关性分析 =====
plt.figure(figsize=(6, 5))
corr = tips.select_dtypes(include="number").corr()
sns.heatmap(corr, annot=True, cmap="coolwarm", center=0, fmt=".2f",
            square=True, vmin=-1, vmax=1)
plt.title("相关性矩阵")
plt.savefig("fig5_correlation.png", dpi=120, bbox_inches="tight")
plt.show()

sns.pairplot(tips, hue="time", diag_kind="kde")
plt.savefig("fig6_pairplot.png", dpi=120, bbox_inches="tight")
plt.show()

# ===== 8. 关键发现 =====
print("\n" + "="*50)
print("关键发现：")
print("="*50)
print(f"1. 账单均值 {tips['total_bill'].mean():.2f}，小费均值 {tips['tip'].mean():.2f}")
print(f"2. 平均小费率 {tips['tip'].sum()/tips['total_bill'].sum():.2%}")
print(f"3. 账单与小费相关性 {tips['total_bill'].corr(tips['tip']):.3f}（强正相关）")
print(f"4. 晚餐平均账单 {tips[tips['time']=='Dinner']['total_bill'].mean():.2f}，"
      f"午餐 {tips[tips['time']=='Lunch']['total_bill'].mean():.2f}")
print(f"5. 周末平均账单 {tips[tips['day'].isin(['Sat','Sun'])]['total_bill'].mean():.2f}，"
      f"工作日 {tips[~tips['day'].isin(['Sat','Sun'])]['total_bill'].mean():.2f}")
print(f"6. 账单异常 {len(bill_outliers)} 条，占比 {len(bill_outliers)/len(tips):.2%}")
print(f"7. 吸烟组小费率 {tips[tips['smoker']=='Yes']['tip'].sum()/tips[tips['smoker']=='Yes']['total_bill'].sum():.2%}，"
      f"不吸烟 {tips[tips['smoker']=='No']['tip'].sum()/tips[tips['smoker']=='No']['total_bill'].sum():.2%}")
print("\n已生成 fig1~fig6.png 共 6 张图")
