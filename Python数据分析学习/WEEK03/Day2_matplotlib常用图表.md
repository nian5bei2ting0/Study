# Day 2 · matplotlib 常用图表

> 今日目标：掌握柱状图、饼图、散点图、直方图、箱线图 5 种最常用图表。
> 预计时间：2 小时

---

## 一、柱状图 bar

适合：**比较不同类别的数值大小**。

```python
import matplotlib.pyplot as plt
plt.rcParams["font.sans-serif"] = ["SimHei"]
plt.rcParams["axes.unicode_minus"] = False

products = ["手机","电脑","耳机","键盘"]
sales = [500, 300, 200, 150]

plt.bar(products, sales, color=["red","green","blue","orange"])
plt.title("各产品销量")
plt.xlabel("产品")
plt.ylabel("销量")
plt.show()
```

### 横向柱状图 barh

```python
plt.barh(products, sales, color="steelblue")
plt.title("各产品销量")
plt.show()
```

### 分组柱状图（多组对比）

```python
import numpy as np
products = ["手机","电脑","耳机"]
q1 = [100, 80, 60]
q2 = [120, 90, 70]

x = np.arange(len(products))
w = 0.35
plt.bar(x - w/2, q1, w, label="Q1")
plt.bar(x + w/2, q2, w, label="Q2")
plt.xticks(x, products)
plt.legend()
plt.title("Q1 vs Q2 销量")
plt.show()
```

---

## 二、饼图 pie

适合：**展示各部分占整体的比例**。

```python
labels = ["手机","电脑","耳机","其他"]
sizes = [40, 30, 20, 10]
colors = ["red","green","blue","gray"]

plt.pie(sizes, labels=labels, colors=colors, autopct="%1.1f%%",
        startangle=90, explode=(0.05, 0, 0, 0))
plt.title("销售占比")
plt.axis("equal")   # 圆形
plt.show()
```

| 参数 | 含义 |
|------|------|
| `autopct` | 显示百分比格式 |
| `startangle` | 起始角度 |
| `explode` | 把某块"拉出来"突出 |
| `shadow` | 阴影 |

> ⚠️ 饼图类别不要超过 6 个，否则可读性差。

---

## 三、散点图 scatter

适合：**看两个变量的关系**。

```python
import numpy as np
rng = np.random.default_rng(0)
x = rng.normal(50, 10, 100)
y = x * 2 + rng.normal(0, 5, 100)

plt.scatter(x, y, c="blue", alpha=0.6, s=30)
plt.title("身高与体重关系")
plt.xlabel("身高")
plt.ylabel("体重")
plt.show()
```

| 参数 | 含义 |
|------|------|
| `c` | 颜色（可传数组做颜色映射） |
| `s` | 点大小 |
| `alpha` | 透明度（0~1，重叠多时调小） |
| `cmap` | 颜色映射（配合 c=数值数组） |

### 颜色映射散点

```python
z = rng.normal(0, 1, 100)
plt.scatter(x, y, c=z, cmap="viridis", alpha=0.7)
plt.colorbar(label="Z 值")
plt.show()
```

---

## 四、直方图 hist

适合：**看单个数值变量的分布形态**。

```python
data = np.random.normal(170, 8, 1000)   # 模拟身高

plt.hist(data, bins=30, color="skyblue", edgecolor="black")
plt.title("身高分布")
plt.xlabel("身高")
plt.ylabel("频数")
plt.show()
```

| 参数 | 含义 |
|------|------|
| `bins` | 分桶数 |
| `density` | True 则归一化为密度（面积=1） |
| `edgecolor` | 柱子边框色 |

### 多组直方图叠加

```python
a = np.random.normal(170, 8, 500)
b = np.random.normal(175, 7, 500)
plt.hist(a, bins=30, alpha=0.5, label="男")
plt.hist(b, bins=30, alpha=0.5, label="女")
plt.legend()
plt.show()
```

---

## 五、箱线图 boxplot

适合：**看分布的五数概括 + 异常值**。

```python
data = [np.random.normal(50, 10, 100),
        np.random.normal(60, 15, 100),
        np.random.normal(70, 8, 100)]

plt.boxplot(data, labels=["A组","B组","C组"])
plt.title("三组数据分布对比")
plt.ylabel("分数")
plt.show()
```

箱线图读法：
- 箱子上下边 = 上下四分位数（Q3/Q1）
- 箱中横线 = 中位数
- 上下"须" = 1.5 倍 IQR 范围
- 圆点 = 异常值

---

## 六、一张画布画多张不同图

```python
fig, axes = plt.subplots(2, 3, figsize=(15, 8))

# 柱状图
axes[0,0].bar(["A","B","C"], [3,5,2]); axes[0,0].set_title("柱状图")
# 饼图
axes[0,1].pie([30,40,30], labels=["X","Y","Z"], autopct="%1.0f%%"); axes[0,1].set_title("饼图")
# 散点
axes[0,2].scatter(np.random.rand(50), np.random.rand(50)); axes[0,2].set_title("散点")
# 直方
axes[1,0].hist(np.random.normal(0,1,500), bins=20); axes[1,0].set_title("直方图")
# 箱线
axes[1,1].boxplot(np.random.normal(50,10,100)); axes[1,1].set_title("箱线图")
# 折线
axes[1,2].plot(range(10), [i**2 for i in range(10)]); axes[1,2].set_title("折线图")

plt.tight_layout()
plt.savefig("all_charts.png", dpi=120)
plt.show()
```

---

## 七、选图指南

| 想表达 | 用什么图 |
|--------|---------|
| 比较类别大小 | 柱状图 |
| 占比 | 饼图 |
| 两个变量关系 | 散点图 |
| 单变量分布 | 直方图 |
| 分布+异常值 | 箱线图 |
| 随时间变化 | 折线图 |

> 选图先问"我想表达什么"，再选图，不要为了花哨而用错图。

---

## 每日练习

用 `np.random` 生成数据完成：

1. 画 5 个城市的 GDP 柱状图，横向柱状图再画一次。
2. 画 4 类商品销售占比的饼图，把占比最大的那块"拉出来"。
3. 生成 200 个 (x, y) 点，x 服从正态分布，y = 2x + 噪声，画散点图，用颜色映射 x 值。
4. 生成 1000 个正态分布数据，画直方图（30 桶），加 `edgecolor`。
5. 生成 3 组数据画箱线图，比较分布。

---

<details>
<summary>参考答案</summary>

**练习 1**

```python
import matplotlib.pyplot as plt
import numpy as np
plt.rcParams["font.sans-serif"] = ["SimHei"]
plt.rcParams["axes.unicode_minus"] = False

cities = ["北京","上海","广州","深圳","成都"]
gdp = [30000, 35000, 25000, 27000, 18000]

plt.figure(figsize=(8,4))
plt.bar(cities, gdp, color="steelblue")
plt.title("各城市 GDP")
plt.show()

plt.barh(cities, gdp, color="coral")
plt.title("各城市 GDP（横向）")
plt.show()
```

**练习 2**

```python
labels = ["手机","电脑","耳机","其他"]
sizes = [45, 25, 20, 10]
plt.pie(sizes, labels=labels, autopct="%1.1f%%", explode=(0.1,0,0,0), startangle=90)
plt.axis("equal"); plt.title("销售占比")
plt.show()
```

**练习 3**

```python
rng = np.random.default_rng(1)
x = rng.normal(50, 10, 200)
y = 2*x + rng.normal(0, 5, 200)
plt.scatter(x, y, c=x, cmap="coolwarm", alpha=0.7)
plt.colorbar(label="x")
plt.show()
```

**练习 4**

```python
data = np.random.normal(170, 8, 1000)
plt.hist(data, bins=30, color="skyblue", edgecolor="black")
plt.show()
```

**练习 5**

```python
data = [np.random.normal(50,10,100), np.random.normal(60,15,100), np.random.normal(70,8,100)]
plt.boxplot(data, labels=["A","B","C"])
plt.show()
```

</details>

---

## 今日小结

- ✅ bar / barh 柱状图（分组对比）
- ✅ pie 饼图（占比，类别 ≤6）
- ✅ scatter 散点图（两变量关系，c/s/alpha）
- ✅ hist 直方图（分布形态）
- ✅ boxplot 箱线图（五数+异常）
- ✅ 选图先想"表达什么"

明天进入 seaborn：更简洁的统计绘图。
