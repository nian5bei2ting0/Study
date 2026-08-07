# Day1 示例：NumPy 基础
# 运行：python code/Day1_np_basic.py
import numpy as np

# 创建数组
a = np.array([1, 2, 3])
b = np.array([[1, 2, 3], [4, 5, 6]])
print("一维：", a)
print("二维：\n", b)

# 特殊数组
print("zeros:", np.zeros(5))
print("ones:\n", np.ones((2, 3)))
print("arange:", np.arange(0, 10, 2))
print("linspace:", np.linspace(0, 1, 5))
print("eye:\n", np.eye(3))
print("random:", np.random.rand(3))
print("randint:", np.random.randint(0, 10, size=5))

# 属性
arr = np.array([[1, 2, 3], [4, 5, 6]])
print("shape:", arr.shape, "ndim:", arr.ndim, "size:", arr.size, "dtype:", arr.dtype)
print("T:\n", arr.T)

# 索引切片
print("第0行:", arr[0])
print("第0行第1列:", arr[0, 1])
print("所有行第0列:", arr[:, 0])
print("行1~2列0~2:\n", arr[1:3, 0:2])

# reshape
print("reshape 3x4:\n", np.arange(12).reshape(3, 4))
print("reshape -1x2:\n", np.arange(12).reshape(-1, 2))

# 类型转换
print("astype:", np.array([1, 2, 3]).astype(float))
