# Day 1 · matplotlib 基础

> 今日目标：理解 matplotlib 的 figure/axes 模型，画第一张折线图，掌握子图与样式。
> 预计时间：2 小时

---

## 一、为什么先学 matplotlib

- Python 生态里**最底层**的绘图库，seaborn、pandas 自带绘图都基于它。
- 控制力最强，能画任何图，但代码相对繁琐。
- 学会 matplotlib，seaborn 就是"语法糖"。

约定俗成：`import matplotlib.pyplot as plt`。

---

## 二、中文与负号配置（每次都要加）

```python
import matplotlib.pyplot as plt
plt.rcParams["font.sans-serif"] = ["SimHei", "Microsoft YaHei", "Arial Unicode MS"]
plt.rcParams["axes.unicode_minus"] = False
```

> 不加这两行，中文会显示成方块，负号会显示成方框。

---

## 三、figure 与 axes 模型（核心概念）

matplotlib 的"两层结构"：

- **Figure**：整张画布（一个窗口/一张图）
- **Axes**：画布上的一个坐标系区域（一张子图）

一个 Figure 可以有多个 Axes（多子图），每个 Axes 上画具体的图。

```
Figure（画布）
├── Axes 1（左上子图）
├── Axes 2（右上子图）
└── ...
```

---

## 四、第一张折线图

```python
import matplotlib.pyplot as plt
plt.rcParams["font.sans-serif"] = ["SimHei", "Microsoft YaHei"]
plt.rcParams["axes.unicode_minus"] = False

months = ["1月","2月","3月","4月","5月","6月"]
sales = [120, 150, 180, 200, 230, 260]

plt.figure(figsize=(8, 4))           # 画布大小
plt.plot(months, sales, color="red", marker="o", linestyle="--", label="销售额")
plt.title("上半年销售趋势")
plt.xlabel("月份")
plt.ylabel("销售额（万元）")
plt.legend()
plt.grid(True)
plt.show()
```

### 常用参数

| 参数 | 含义 | 常用值 |
|------|------|--------|
| `color` | 颜色 | "red"/"blue"/"#FF5733" |
| `marker` | 数据点标记 | "o"圆 "s"方 "^"三角 |
| `linestyle` | 线型 | "-"实线 "--"虚线 ":"点线 |
| `linewidth` | 线宽 | 数字 |
| `label` | 图例标签 | 字符串 |

---

## 五、多条线对比

```python
months = ["1月","2月","3月","4月","5月","6月"]
sales_a = [120, 150, 180, 200, 230, 260]
sales_b = [100, 110, 130, 160, 170, 190]

plt.plot(months, sales_a, "r-o", label="产品A")
plt.plot(months, sales_b, "b--s", label="产品B")
plt.legend()
plt.title("产品对比")
plt.show()
```

> `"r-o"` 是简写：颜色 r + 线型 - + 标记 o。熟练后常用这种简写。

---

## 六、子图 subplot

### 1. plt.subplot 简单写法

```python
plt.figure(figsize=(10, 4))

plt.subplot(1, 2, 1)   # 1行2列的第1个
plt.plot([1,2,3], [1,4,9])
plt.title("图1")

plt.subplot(1, 2, 2)   # 第2个
plt.plot([1,2,3], [1,2,3])
plt.title("图2")

plt.tight_layout()     # 自动调整间距
plt.show()
```

### 2. subplots 一次创建（推荐）

```python
fig, axes = plt.subplots(2, 2, figsize=(10, 8))

axes[0,0].plot([1,2,3], [1,4,9], "r")
axes[0,0].set_title("左上")

axes[0,1].plot([1,2,3], [9,4,1], "g")
axes[0,1].set_title("右上")

axes[1,0].plot([1,2,3], [1,2,3], "b")
axes[1,0].set_title("左下")

axes[1,1].plot([1,2,3], [3,2,1], "y")
axes[1,1].set_title("右下")

plt.tight_layout()
plt.show()
```

> 注意：用 `axes` 对象时，方法名加 `set_` 前缀：`set_title / set_xlabel / set_ylabel`。

---

## 七、保存图片

```python
plt.savefig("sales.png", dpi=150, bbox_inches="tight")
plt.show()   # show 会清空画布，保存要在 show 之前
```

> ⚠️ `plt.show()` 之后画布会被清空，**savefig 必须在 show 之前**。

---

## 八、样式美化

```python
# 内置样式
print(plt.style.available)   # 看有哪些
plt.style.use("seaborn-v0_8-whitegrid")   # 用 seaborn 风格

# 临时样式（用完恢复）
with plt.style.context("ggplot"):
    plt.plot([1,2,3], [1,4,9])
    plt.show()
```

常用样式：`ggplot`、`seaborn-v0_8-whitegrid`、`bmh`、`fivethirtyeight`。

---

## 每日练习

1. 画一张折线图：12 个月的温度变化（自己编数据），加标题、轴标签、网格、图例。
2. 在一张画布上画 2×2 子图，分别画 4 条不同的线（不同颜色和标记）。
3. 把练习 2 的图保存为 `myplot.png`，dpi=150。
4. 试试 `plt.style.use("ggplot")`，看图风格变化。

---

<details>
<summary>参考答案</summary>

**练习 1**

```python
import matplotlib.pyplot as plt
plt.rcParams["font.sans-serif"] = ["SimHei"]
plt.rcParams["axes.unicode_minus"] = False

months = [f"{i}月" for i in range(1, 13)]
temp = [2, 5, 11, 17, 22, 27, 30, 29, 24, 18, 11, 5]

plt.figure(figsize=(10, 4))
plt.plot(months, temp, "r-o", label="月均温")
plt.title("年度温度变化")
plt.xlabel("月份")
plt.ylabel("温度（℃）")
plt.legend()
plt.grid(True)
plt.show()
```

**练习 2+3**

```python
fig, axes = plt.subplots(2, 2, figsize=(10, 8))
x = [1, 2, 3, 4, 5]
axes[0,0].plot(x, [i**2 for i in x], "r-o"); axes[0,0].set_title("平方")
axes[0,1].plot(x, [i**3 for i in x], "g-s"); axes[0,1].set_title("立方")
axes[1,0].plot(x, [2**i for i in x], "b-^"); axes[1,0].set_title("2的幂")
axes[1,1].plot(x, [i for i in x], "y--d"); axes[1,1].set_title("线性")
plt.tight_layout()
plt.savefig("myplot.png", dpi=150, bbox_inches="tight")
plt.show()
```

</details>

---

## 今日小结

- ✅ figure/axes 两层模型
- ✅ 中文配置 + 负号修复
- ✅ `plt.plot` 折线图，颜色/线型/标记参数
- ✅ `subplots` 一次建多子图（推荐）
- ✅ `savefig` 保存（在 show 之前）
- ✅ `plt.style.use` 切换样式

明天学：matplotlib 的 6 种常用图表。
