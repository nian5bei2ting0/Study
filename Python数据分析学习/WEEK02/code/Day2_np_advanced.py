# Day2 示例：NumPy 进阶
# 运行：python code/Day2_np_advanced.py
import numpy as np

# 向量化运算
a = np.array([1, 2, 3, 4])
print("a+10:", a + 10)
print("a*2:", a * 2)
print("a**2:", a ** 2)
print("a+a:", a + a)

# 广播
m = np.array([[1, 2, 3], [4, 5, 6]])
v = np.array([10, 20, 30])
print("广播加：\n", m + v)

# 布尔索引
a = np.array([10, 20, 30, 40, 50])
print("a>25:", a[a > 25])
print("20的倍数:", a[a % 20 == 0])
print("多条件:", a[(a > 15) & (a < 45)])

# where
print("where:", np.where(np.array([1, 2, 3, 4, 5]) > 3, "大", "小"))

# ufunc
x = np.array([1, 2, 3, 4])
print("sqrt:", np.sqrt(x))
print("exp:", np.exp(x))
print("log:", np.log(x))

# 聚合
m = np.array([[1, 2, 3], [4, 5, 6]])
print("sum:", m.sum(), "按列:", m.sum(axis=0), "按行:", m.sum(axis=1))
print("mean:", m.mean(), "std:", m.std())

# 累计
print("cumsum:", np.cumsum([1, 2, 3, 4]))
print("cumprod:", np.cumprod([1, 2, 3, 4]))

# 排序去重
a = np.array([3, 1, 4, 1, 5, 9, 2, 6])
print("sort:", np.sort(a))
print("argsort:", np.argsort(a))
print("unique:", np.unique(a))

# 拼接
print("concat:", np.concatenate([np.array([1,2,3]), np.array([4,5,6])]))

# z-score 标准化
data = np.array([10, 20, 30, 40, 50], dtype=float)
print("z-score:", (data - data.mean()) / data.std())
