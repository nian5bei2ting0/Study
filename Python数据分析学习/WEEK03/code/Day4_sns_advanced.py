# Day4 示例：seaborn 进阶
# 运行：python code/Day4_sns_advanced.py
import seaborn as sns
import matplotlib.pyplot as plt

plt.rcParams["font.sans-serif"] = ["SimHei", "Microsoft YaHei"]
plt.rcParams["axes.unicode_minus"] = False
sns.set_theme(style="whitegrid")

tips = sns.load_dataset("tips")

# relplot 关系图
sns.relplot(data=tips, x="total_bill", y="tip", hue="time", style="sex", size="size")
plt.show()

# 折线关系
sns.relplot(data=tips, x="size", y="total_bill", kind="line")
plt.show()

# 散点进阶
sns.scatterplot(data=tips, x="total_bill", y="tip",
                hue="time", style="sex", size="size", sizes=(20,200), alpha=0.7)
plt.show()

# 回归图
sns.regplot(data=tips, x="total_bill", y="tip", scatter_kws={"alpha":0.5})
plt.show()

# 分组回归
sns.lmplot(data=tips, x="total_bill", y="tip", hue="smoker")
plt.show()

# 多项式回归
sns.regplot(data=tips, x="total_bill", y="tip", order=2)
plt.show()

# pairplot 散点矩阵
sns.pairplot(tips, hue="time", diag_kind="kde")
plt.show()

# jointplot 联合分布
sns.jointplot(data=tips, x="total_bill", y="tip", kind="kde")
plt.show()

# 热力图
corr = tips.select_dtypes(include="number").corr()
sns.heatmap(corr, annot=True, cmap="coolwarm", center=0, fmt=".2f", square=True, vmin=-1, vmax=1)
plt.title("相关性矩阵"); plt.show()

# 透视热力图
pivot = tips.pivot_table(values="total_bill", index="day", columns="time", aggfunc="mean")
sns.heatmap(pivot, annot=True, cmap="YlGnBu", fmt=".1f")
plt.title("各天各时段平均账单"); plt.show()

# 聚类热力图
sns.clustermap(corr, annot=True, cmap="coolwarm")
plt.show()

# FacetGrid
g = sns.FacetGrid(tips, col="day", row="time", hue="sex", height=3)
g.map_dataframe(sns.scatterplot, x="total_bill", y="tip")
g.add_legend()
plt.show()

# PairGrid 自定义
g = sns.PairGrid(tips, hue="time")
g.map_upper(sns.scatterplot)
g.map_lower(sns.kdeplot)
g.map_diag(sns.histplot)
g.add_legend()
plt.show()
