# Day5 示例：函数
# 运行：python code/Day5_functions.py


def circle_area(r):
    """计算圆面积，r 为半径。"""
    return 3.14 * r * r


def greet(name, greeting="你好"):
    print(f"{greeting}，{name}")


def is_even(n):
    return n % 2 == 0


def max_of_three(a, b, c):
    return max(a, b, c)


def fib(n):
    """返回第 n 个斐波那契数。"""
    a, b = 1, 1
    for _ in range(n - 1):
        a, b = b, a + b
    return a


print(circle_area(5))
greet("小明")
greet("小明", "Hello")
print(is_even(4))
print(max_of_three(3, 7, 5))
print(fib(6))   # 8

# lambda 排序
students = [{"name": "A", "score": 78}, {"name": "B", "score": 90}]
students.sort(key=lambda s: s["score"], reverse=True)
print(students)

# 函数作为参数
nums = [1, 2, 3, 4]
print(list(map(lambda x: x * x, nums)))   # [1, 4, 9, 16]
