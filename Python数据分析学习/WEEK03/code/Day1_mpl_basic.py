# Day1 示例：matplotlib 基础
# 运行：python code/Day1_mpl_basic.py
import matplotlib.pyplot as plt
import numpy as np

plt.rcParams["font.sans-serif"] = ["SimHei", "Microsoft YaHei"]
plt.rcParams["axes.unicode_minus"] = False

# 第一张折线图
months = ["1月","2月","3月","4月","5月","6月"]
sales = [120, 150, 180, 200, 230, 260]

plt.figure(figsize=(8, 4))
plt.plot(months, sales, color="red", marker="o", linestyle="--", label="销售额")
plt.title("上半年销售趋势")
plt.xlabel("月份")
plt.ylabel("销售额（万元）")
plt.legend()
plt.grid(True)
plt.show()

# 多条线对比
sales_a = [120, 150, 180, 200, 230, 260]
sales_b = [100, 110, 130, 160, 170, 190]
plt.plot(months, sales_a, "r-o", label="产品A")
plt.plot(months, sales_b, "b--s", label="产品B")
plt.legend(); plt.title("产品对比"); plt.show()

# subplots 一次建多子图
fig, axes = plt.subplots(2, 2, figsize=(10, 8))
x = [1, 2, 3, 4, 5]
axes[0,0].plot(x, [i**2 for i in x], "r-o"); axes[0,0].set_title("平方")
axes[0,1].plot(x, [i**3 for i in x], "g-s"); axes[0,1].set_title("立方")
axes[1,0].plot(x, [2**i for i in x], "b-^"); axes[1,0].set_title("2的幂")
axes[1,1].plot(x, x, "y--d"); axes[1,1].set_title("线性")
plt.tight_layout()
plt.savefig("myplot.png", dpi=150, bbox_inches="tight")
plt.show()

# 样式切换
plt.style.use("ggplot")
plt.plot([1,2,3], [1,4,9])
plt.title("ggplot 样式")
plt.show()
