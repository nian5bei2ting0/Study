# Day3 示例：seaborn 入门
# 运行：python code/Day3_sns_basic.py
import seaborn as sns
import matplotlib.pyplot as plt
import numpy as np

plt.rcParams["font.sans-serif"] = ["SimHei", "Microsoft YaHei"]
plt.rcParams["axes.unicode_minus"] = False
sns.set_theme(style="whitegrid", palette="pastel")

tips = sns.load_dataset("tips")
print(tips.head())

# 直方图 + KDE
sns.histplot(tips["total_bill"], bins=30, kde=True)
plt.title("账单金额分布"); plt.show()

# KDE 按组
sns.kdeplot(data=tips, x="total_bill", hue="time", fill=True)
plt.title("午餐/晚餐账单分布"); plt.show()

# displot
sns.displot(data=tips, x="total_bill", hue="time", kind="kde")
plt.show()

# 箱线图
sns.boxplot(data=tips, x="day", y="total_bill", hue="time")
plt.title("各天账单分布"); plt.show()

# 小提琴图
sns.violinplot(data=tips, x="day", y="total_bill")
plt.title("各天账单分布（小提琴）"); plt.show()

# 计数图
sns.countplot(data=tips, x="day", hue="sex")
plt.title("各天就餐次数"); plt.show()

# 条形图（均值+置信区间）
sns.barplot(data=tips, x="day", y="tip", estimator=np.median)
plt.title("各天平均小费（中位数）"); plt.show()

# 点图
sns.pointplot(data=tips, x="day", y="total_bill", hue="time")
plt.title("各天平均账单趋势"); plt.show()

# 分面
g = sns.FacetGrid(tips, col="time", row="sex")
g.map(sns.histplot, "tip")
plt.show()
