# Day 2 · NumPy 进阶

> 今日目标：掌握向量化运算、广播、布尔索引、通用函数，写出"不用循环"的代码。
> 预计时间：2 小时

---

## 一、向量化运算：告别循环

NumPy 数组之间运算**逐元素**进行，不需要写循环：

```python
import numpy as np
a = np.array([1, 2, 3, 4])
print(a + 10)    # [11 12 13 14]   每个元素加10
print(a * 2)     # [2 4 6 8]
print(a ** 2)    # [1 4 9 16]
print(a + a)     # [2 4 6 8]       数组+数组
print(a * a)     # [1 4 9 16]
```

> ⚠️ 注意：`a * b` 是逐元素相乘，**不是矩阵乘法**。矩阵乘法用 `a @ b` 或 `np.dot(a, b)`。

---

## 二、广播 Broadcasting

不同形状的数组也能运算，NumPy 会"自动扩展"小数组：

```python
a = np.array([[1, 2, 3],
              [4, 5, 6]])     # shape (2, 3)
b = np.array([10, 20, 30])   # shape (3,)

print(a + b)
# [[11 22 33]
#  [14 25 36]]   b 被广播成 (2,3)
```

规则：从右往左对齐维度，每一维要么相同，要么其中一个是 1，要么不存在。

```python
a = np.array([[1], [2], [3]])  # (3, 1)
b = np.array([10, 20, 30])    # (3,)
print(a + b)
# [[11 21 31]
#  [12 22 32]
#  [13 23 33]]   一个列向量+一个行向量 → 3x3 矩阵
```

> 广播是 NumPy 最强大也最容易出错的特性，**不确定时手算一遍 shape**。

---

## 三、布尔索引：用条件筛数据

### 1. 比较运算返回布尔数组

```python
a = np.array([10, 20, 30, 40, 50])
print(a > 25)   # [False False  True  True  True]
```

### 2. 用布尔数组当索引

```python
print(a[a > 25])   # [30 40 50]   只保留 True 位置
print(a[a % 20 == 0])  # [20 40]   20的倍数
```

### 3. 多条件用 & | ~（不是 and/or/not）

```python
a = np.array([10, 20, 30, 40, 50])
mask = (a > 15) & (a < 45)
print(a[mask])   # [20 30 40]
```

> ⚠️ 必须用 `& | ~`，且**条件要加括号**，因为运算符优先级问题。

### 4. where 条件赋值

```python
a = np.array([1, 2, 3, 4, 5])
b = np.where(a > 3, "大", "小")
print(b)   # ['小' '小' '小' '大' '大']
```

---

## 四、通用函数 ufunc

对每个元素做数学运算，**比 Python 的 math 快**：

```python
a = np.array([1, 2, 3, 4])
print(np.sqrt(a))    # [1. 1.41 1.73 2.]
print(np.exp(a))     # e的x次方
print(np.log(a))     # 自然对数
print(np.abs([-1, -2]))
print(np.sin(np.pi/2))   # 1.0
```

### 统计聚合

```python
a = np.array([[1, 2, 3], [4, 5, 6]])
print(a.sum())          # 21   全部求和
print(a.sum(axis=0))    # [5 7 9]   按列求和（消掉行）
print(a.sum(axis=1))    # [6 15]    按行求和（消掉列）
print(a.mean())         # 3.5
print(a.max(), a.min()) # 6 1
print(a.std())          # 标准差
```

> axis 口诀：**axis=0 消行（按列），axis=1 消列（按行）**。

### 累计运算

```python
a = np.array([1, 2, 3, 4])
print(np.cumsum(a))    # [1 3 6 10]   累加
print(np.cumprod(a))   # [1 2 6 24]   累乘
```

---

## 五、排序与去重

```python
a = np.array([3, 1, 4, 1, 5, 9, 2, 6])
print(np.sort(a))              # [1 1 2 3 4 5 6 9]
print(np.argsort(a))          # 排序后的索引（重要！）
print(np.unique(a))           # [1 2 3 4 5 6 9]  去重并排序
```

`argsort` 在"按某列排序另一列"场景非常有用。

---

## 六、拼接与拆分

```python
a = np.array([1, 2, 3])
b = np.array([4, 5, 6])
print(np.concatenate([a, b]))   # [1 2 3 4 5 6]
print(np.vstack([a, b]))       # 垂直堆叠成 2x3
print(np.hstack([a, b]))       # 水平拼接（等同 concatenate）

m = np.array([[1,2,3],[4,5,6]])
print(np.vsplit(m, 2))         # 按行拆2块
print(np.hsplit(m, 3))         # 按列拆3块
```

---

## 七、实战：标准化数据

把数据减去均值、除以标准差（z-score 标准化），数据分析常用：

```python
data = np.array([10, 20, 30, 40, 50])
z = (data - data.mean()) / data.std()
print(z)   # [-1.41 -0.71 0. 0.71 1.41]
```

---

## 每日练习

1. 创建数组 `[1,2,3,4,5,6,7,8,9,10]`，用布尔索引取出偶数和大于 5 的数。
2. 创建 3×4 的随机整数数组（0~100），输出每行的最大值、每列的平均值。
3. 用 `where` 把数组 `[-3, -1, 0, 2, 5]` 的负数替换成 0。
4. 把数组 `[1,2,3,4,5]` 做 z-score 标准化，验证标准化后均值约为 0、标准差为 1。

---

<details>
<summary>参考答案</summary>

**练习 1**

```python
import numpy as np
a = np.arange(1, 11)
print(a[a % 2 == 0])
print(a[a > 5])
```

**练习 2**

```python
m = np.random.randint(0, 100, size=(3, 4))
print(m)
print(m.max(axis=1))   # 每行最大
print(m.mean(axis=0))  # 每列均值
```

**练习 3**

```python
a = np.array([-3, -1, 0, 2, 5])
print(np.where(a < 0, 0, a))   # [0 0 0 2 5]
```

**练习 4**

```python
a = np.array([1, 2, 3, 4, 5], dtype=float)
z = (a - a.mean()) / a.std()
print(z.mean(), z.std())   # ~0, 1
```

</details>

---

## 今日小结

- ✅ 向量化运算：数组运算逐元素进行，无需循环
- ✅ 广播：不同 shape 自动扩展，注意对齐规则
- ✅ 布尔索引：`a[a > x]`，多条件用 `& | ~` 加括号
- ✅ `np.where` 条件赋值
- ✅ ufunc：sqrt/exp/log/sin 等
- ✅ 聚合：sum/mean/max/min，axis=0 按列、axis=1 按行
- ✅ argsort / unique / concatenate / vstack / hstack

明天进入 Pandas：表格数据处理才是数据分析的主战场。
