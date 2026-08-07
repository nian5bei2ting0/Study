# Day 7 · 综合实战：完整 EDA 报告

> 今日目标：整合全周知识，对一份真实数据集输出完整 EDA 报告（含 8+ 张图）。
> 预计时间：2.5 小时

---

## 一、项目说明

用 seaborn 内置 `tips` 数据集（餐厅小费），按 EDA 标准流程输出一份完整报告：

1. 数据概览与画像
2. 数据质量检查
3. 单变量分布分析（账单、小费、人数、星期）
4. 异常值识别
5. 双变量关系（账单 vs 小费）
6. 多变量分组对比
7. 相关性分析
8. 关键发现与业务建议

涉及知识点：matplotlib + seaborn + EDA 方法论——**全周覆盖**。

---

## 二、完整代码（先看懂，再自己敲）

```python
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

# 画像
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

# 3.1 账单分布
sns.histplot(tips["total_bill"], bins=30, kde=True, ax=axes[0,0])
axes[0,0].set_title("账单金额分布")

# 3.2 小费分布
sns.histplot(tips["tip"], bins=30, kde=True, ax=axes[0,1])
axes[0,1].set_title("小费金额分布")

# 3.3 就餐人数
sns.countplot(data=tips, x="size", ax=axes[1,0])
axes[1,0].set_title("就餐人数分布")

# 3.4 各天就餐次数
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
sns.regplot(data=tips, x="total_bill", y="tip",
            scatter_kws={"alpha":0.5}, ax=axes[1])
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
fig, axes = plt.subplots(1, 2, figsize=(14, 5))
corr = tips.select_dtypes(include="number").corr()
sns.heatmap(corr, annot=True, cmap="coolwarm", center=0, fmt=".2f",
            square=True, vmin=-1, vmax=1, ax=axes[0])
axes[0].set_title("相关性矩阵")
sns.pairplot_data = tips  # 占位
plt.tight_layout()
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
```

---

## 三、知识点对照

| 代码段 | 用到的知识（哪一天） |
|--------|-------------------|
| `plt.rcParams` 中文 | Day1 |
| `subplots` 多子图 | Day1 |
| `histplot / countplot / boxplot / violinplot` | Day2/3 |
| `scatterplot / regplot` | Day2/4 |
| `heatmap` 相关性 | Day4 |
| `pairplot` | Day4 |
| `data_profile` 函数 | Day5 |
| IQR 异常检测 | Day6 |
| 分组对比 hue | Day3/4 |

---

## 四、运行方式

```bash
python code/Day7_eda_report.py
```

会生成 6 张图：`fig1_univariate.png` 到 `fig6_pairplot.png`，并打印关键发现。

---

## 五、进阶挑战（可选）

1. **小费率分析**：新增 `tip_rate = tip / total_bill` 列，分析其分布与影响因素。
2. **多维分面**：用 `FacetGrid` 按 `day × time` 拆分画账单分布。
3. **聚类初探**：用 `clustermap` 看数值变量聚类。
4. **业务建议**：基于发现写 3 条餐厅运营建议（如"晚餐时段重点服务"）。
5. **换数据集**：用 `sns.load_dataset("penguins")` 重做一份 EDA 报告。

> 能做完 2 个进阶，说明你已具备独立做 EDA 的能力。

---

## 每日练习（必做）

1. 把上面的代码**完整敲一遍**并运行成功，所有图都看到。
2. 至少完成 1 个进阶挑战。
3. 用自己的话写一份 300 字的 EDA 报告，含 3 条业务建议。

---

## 今日小结

- ✅ 用一个完整项目把全周知识串起来
- ✅ EDA 6 步流程落地：概览→质量→单变量→异常→双变量→多变量
- ✅ 输出 6 张图 + 7 条关键发现
- ✅ 形成"看图说话"的业务洞察能力

---

# 🎉 恭喜完成第三周！

你现在具备了完整的数据分析能力链：Python 语法 → NumPy/Pandas 处理 → 可视化 + EDA。建议接下来：
- **WEEK04**（后续开）：机器学习入门（scikit-learn）
- **方向选择**：业务 BI 报表 / 用户行为分析 / 金融数据分析 / 数据科学竞赛

无论选哪个方向，前三周打好的地基都够用了。继续加油！
