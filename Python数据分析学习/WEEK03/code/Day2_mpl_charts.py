# Day2 示例：matplotlib 常用图表
# 运行：python code/Day2_mpl_charts.py
import matplotlib.pyplot as plt
import numpy as np

plt.rcParams["font.sans-serif"] = ["SimHei", "Microsoft YaHei"]
plt.rcParams["axes.unicode_minus"] = False

# 柱状图
products = ["手机","电脑","耳机","键盘"]
sales = [500, 300, 200, 150]
plt.bar(products, sales, color=["red","green","blue","orange"])
plt.title("各产品销量"); plt.show()

# 横向柱状图
plt.barh(products, sales, color="steelblue")
plt.title("各产品销量（横向）"); plt.show()

# 分组柱状图
q1 = [100, 80, 60]; q2 = [120, 90, 70]
x = np.arange(len(products[:3]))
w = 0.35
plt.bar(x - w/2, q1, w, label="Q1")
plt.bar(x + w/2, q2, w, label="Q2")
plt.xticks(x, products[:3]); plt.legend()
plt.title("Q1 vs Q2"); plt.show()

# 饼图
labels = ["手机","电脑","耳机","其他"]
sizes = [40, 30, 20, 10]
plt.pie(sizes, labels=labels, autopct="%1.1f%%", startangle=90, explode=(0.05,0,0,0))
plt.axis("equal"); plt.title("销售占比"); plt.show()

# 散点图
rng = np.random.default_rng(0)
x = rng.normal(50, 10, 200)
y = x * 2 + rng.normal(0, 5, 200)
plt.scatter(x, y, c=x, cmap="coolwarm", alpha=0.7, s=30)
plt.colorbar(label="x"); plt.title("散点图"); plt.show()

# 直方图
data = np.random.normal(170, 8, 1000)
plt.hist(data, bins=30, color="skyblue", edgecolor="black")
plt.title("身高分布"); plt.show()

# 箱线图
data = [np.random.normal(50,10,100), np.random.normal(60,15,100), np.random.normal(70,8,100)]
plt.boxplot(data, labels=["A","B","C"])
plt.title("三组数据分布"); plt.show()

# 一张画布画多张不同图
fig, axes = plt.subplots(2, 3, figsize=(15, 8))
axes[0,0].bar(["A","B","C"], [3,5,2]); axes[0,0].set_title("柱状图")
axes[0,1].pie([30,40,30], labels=["X","Y","Z"], autopct="%1.0f%%"); axes[0,1].set_title("饼图")
axes[0,2].scatter(np.random.rand(50), np.random.rand(50)); axes[0,2].set_title("散点")
axes[1,0].hist(np.random.normal(0,1,500), bins=20); axes[1,0].set_title("直方图")
axes[1,1].boxplot(np.random.normal(50,10,100)); axes[1,1].set_title("箱线图")
axes[1,2].plot(range(10), [i**2 for i in range(10)]); axes[1,2].set_title("折线图")
plt.tight_layout(); plt.savefig("all_charts.png", dpi=120); plt.show()
