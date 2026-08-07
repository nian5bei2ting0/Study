# Day 1 · NumPy 基础

> 今日目标：理解 NumPy 的核心数据结构 ndarray，学会创建、查看、索引数组。
> 预计时间：2 小时

---

## 一、为什么学 NumPy

纯 Python 列表做数值计算慢且麻烦：

```python
a = [1, 2, 3]
b = [4, 5, 6]
# 想要 [5,7,9]？必须写循环
c = [x + y for x, y in zip(a, b)]
```

NumPy 用 **ndarray**（n 维数组）一次性对整个数组运算，**比纯 Python 快几十倍**，是 Pandas、机器学习库的底层。

```python
import numpy as np
a = np.array([1, 2, 3])
b = np.array([4, 5, 6])
print(a + b)   # [5 7 9]   直接相加！
```

约定俗成：`import numpy as np`，全世界都这么写。

---

## 二、创建数组

### 1. 从列表创建

```python
import numpy as np

a = np.array([1, 2, 3])              # 一维
b = np.array([[1, 2, 3], [4, 5, 6]]) # 二维（2行3列）
print(a)
print(b)
```

### 2. 创建特殊数组

```python
np.zeros(5)            # [0. 0. 0. 0. 0.]
np.ones((2, 3))        # 2行3列全1
np.arange(0, 10, 2)    # [0 2 4 6 8]   类似 range
np.linspace(0, 1, 5)  # [0. 0.25 0.5 0.75 1.]  均分5个点
np.eye(3)             # 3x3 单位矩阵
np.random.rand(3)     # 3个0~1随机数
np.random.randint(0, 10, size=5)  # 0~10的5个随机整数
```

> `linspace` 和 `arange` 区别：`arange` 按步长，`linspace` 按个数。

---

## 三、数组属性

```python
a = np.array([[1, 2, 3], [4, 5, 6]])

print(a.shape)     # (2, 3)   形状
print(a.ndim)      # 2        维度数
print(a.size)      # 6        元素总数
print(a.dtype)     # int64    元素类型
print(a.T)         # 转置 3行2列
```

> shape 是最常查的属性，**记住 (行, 列) 顺序**。

---

## 四、索引与切片（和列表很像）

### 1. 一维

```python
a = np.array([10, 20, 30, 40, 50])
print(a[0])      # 10
print(a[-1])     # 50
print(a[1:4])    # [20 30 40]
print(a[::-1])   # 反转
```

### 2. 二维

```python
b = np.array([[1, 2, 3],
              [4, 5, 6],
              [7, 8, 9]])

print(b[0])         # [1 2 3]      第0行
print(b[0, 1])      # 2           第0行第1列
print(b[:, 0])      # [1 4 7]     所有行的第0列
print(b[1:3, 0:2])  # [[4 5][7 8]] 行1~2、列0~1
```

口诀：`arr[行索引, 列索引]`，用 `:` 表示"全部"。

### 3. 花式索引

```python
b = np.array([10, 20, 30, 40, 50])
idx = [0, 2, 4]
print(b[idx])   # [10 30 50]   一次取多个位置
```

---

## 五、修改形状

```python
a = np.arange(12)            # [0 1 ... 11]
print(a.reshape(3, 4))       # 3行4列
print(a.reshape(-1, 2))      # -1 表示自动算：6行2列
print(a.reshape(2, -1))      # 2行6列

# flatten / ravel：展平
b = a.reshape(3, 4)
print(b.flatten())           # [0 1 ... 11]  一维
```

> `reshape` 元素总数必须一致，否则报错。
> `-1` 是偷懒写法，让 NumPy 自己算那一维。

---

## 六、数据类型 dtype

```python
a = np.array([1, 2, 3])           # int64
b = np.array([1.0, 2.0])          # float64
c = np.array([1, 2], dtype=float) # 指定 float
d = a.astype(float)               # 类型转换
```

常见 dtype：`int32 / int64 / float32 / float64 / bool / str`。

> 处理大数据时 `float32` 比 `float64` 省一半内存，注意权衡精度。

---

## 每日练习

1. 创建一个 5×5 的二维数组，元素是 1~25 的整数（按行排列），输出它的 shape、第 2 行、第 3 列。
2. 用 `arange` 和 `reshape` 创建一个 4×4 的数组，元素是 0~15。
3. 创建一个 10 个元素的随机整数数组（0~100），用切片取出前 3 个和后 3 个。
4. 把一个 int 数组 `[1,2,3,4]` 转成 float 类型。

---

<details>
<summary>参考答案</summary>

**练习 1**

```python
import numpy as np
a = np.arange(1, 26).reshape(5, 5)
print(a.shape)   # (5, 5)
print(a[2])      # 第2行
print(a[:, 3])   # 第3列
```

**练习 2**

```python
a = np.arange(16).reshape(4, 4)
print(a)
```

**练习 3**

```python
a = np.random.randint(0, 100, size=10)
print(a[:3])
print(a[-3:])
```

**练习 4**

```python
a = np.array([1, 2, 3, 4])
print(a.astype(float))   # [1. 2. 3. 4.]
```

</details>

---

## 今日小结

- ✅ ndarray 是 NumPy 核心，向量化运算比列表快
- ✅ 创建：`np.array / zeros / ones / arange / linspace / random`
- ✅ 属性：`shape / ndim / size / dtype / T`
- ✅ 索引切片：`arr[行, 列]`，`:` 表示全部
- ✅ `reshape` 改形状，`-1` 自动推断，`astype` 转类型

明天学：NumPy 进阶——向量化运算、广播机制、布尔索引、通用函数。
