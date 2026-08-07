# Day 5 · 函数

> 今日目标：学会把代码封装成函数，让程序更清晰、可复用。
> 预计时间：2 小时

---

## 一、为什么要函数

没有函数：

```python
# 算两个圆面积
r1 = 3
print(3.14 * r1 * r1)

r2 = 5
print(3.14 * r2 * r2)

r3 = 7
print(3.14 * r3 * r3)   # 同样的代码写了三遍
```

有函数：

```python
def circle_area(r):
    return 3.14 * r * r

print(circle_area(3))
print(circle_area(5))
print(circle_area(7))
```

函数 = 把一段逻辑打包，起个名字，需要时调用。**DRY 原则：Don't Repeat Yourself。**

---

## 二、定义与调用

```python
def greet(name):           # def 函数名(参数):
    print(f"Hello, {name}!")

greet("小明")              # 调用，传入实参
greet("小红")
```

- `def` 定义函数
- `name` 是**形参**（占位符）
- 调用时传的 `"小明"` 是**实参**
- 函数体要**缩进**

---

## 三、返回值 return

```python
def add(a, b):
    return a + b

result = add(3, 5)
print(result)   # 8
```

- `return` 把结果送回调用处，函数结束
- 没有 return 或只写 `return`，返回 `None`

```python
def say_hi():
    print("hi")

x = say_hi()    # 打印 hi，x = None
```

---

## 四、参数的几种形式

### 1. 位置参数（按顺序传）

```python
def info(name, age):
    print(f"{name} {age}岁")

info("小明", 18)        # 小明 18岁
info(18, "小明")        # 18 小明岁  ← 顺序错了！
```

### 2. 关键字参数（按名字传，更清晰）

```python
info(age=18, name="小明")   # 小明 18岁  顺序无所谓
```

### 3. 默认参数（可省略）

```python
def greet(name, greeting="你好"):
    print(f"{greeting}，{name}")

greet("小明")            # 你好，小明
greet("小明", "Hello")   # Hello，小明
```

> 默认参数必须放在普通参数**后面**。

### 4. 可变参数（进阶，了解）

```python
def sum_all(*nums):       # *收集成元组
    return sum(nums)

print(sum_all(1, 2, 3, 4))   # 10
```

---

## 五、作用域：变量能见度

```python
def func():
    x = 10          # 局部变量，函数外访问不到
    print(x)

func()
# print(x)         # 报错！x 不存在

y = 100             # 全局变量
def func2():
    print(y)        # 函数内能读全局变量

func2()
```

> 经验：尽量别在函数里改全局变量，容易出 bug。需要修改时用 `return` 把结果送出来。

---

## 六、函数即对象（Python 特色）

```python
def square(x):
    return x * x

f = square          # 函数可以赋值给变量
print(f(5))         # 25

nums = [1, 2, 3, 4]
result = list(map(square, nums))   # 对每个元素应用函数
print(result)       # [1, 4, 9, 16]
```

---

## 七、lambda 匿名函数

一次性小函数，不用 def。

```python
square = lambda x: x * x
print(square(5))    # 25

# 常配合 sorted/map 使用
students = [{"name": "A", "score": 78}, {"name": "B", "score": 90}]
students.sort(key=lambda s: s["score"], reverse=True)
print(students)     # B 在前
```

> 不要滥用 lambda，逻辑复杂就用 def。

---

## 八、文档字符串 docstring

```python
def circle_area(r):
    """计算圆面积，r 为半径。"""
    return 3.14 * r * r

print(circle_area.__doc__)   # 计算圆面积，r 为半径。
```

养成写 docstring 的习惯，方便别人和未来的自己看懂。

---

## 每日练习

1. 写函数 `is_even(n)` 判断偶数，返回 True/False。
2. 写函数 `max_of_three(a, b, c)` 返回三个数中最大的。
3. 写函数 `count_words(text)` 统计一段英文的单词数。
4. 写函数 `fib(n)` 返回第 n 个斐波那契数（1,1,2,3,5,8...）。
5. 用 lambda + sorted 把学生列表按分数从高到低排序。

---

<details>
<summary>参考答案</summary>

**练习 1**

```python
def is_even(n):
    return n % 2 == 0

print(is_even(4))   # True
```

**练习 2**

```python
def max_of_three(a, b, c):
    return max(a, b, c)

print(max_of_three(3, 7, 5))   # 7
```

**练习 3**

```python
def count_words(text):
    return len(text.split())

print(count_words("hello world python"))   # 3
```

**练习 4**

```python
def fib(n):
    a, b = 1, 1
    for _ in range(n - 1):
        a, b = b, a + b
    return a

print(fib(6))   # 8
```

**练习 5**

```python
students = [{"name": "A", "score": 78}, {"name": "B", "score": 90}]
students.sort(key=lambda s: s["score"], reverse=True)
print(students)
```

</details>

---

## 今日小结

- ✅ def 定义函数，return 返回结果
- ✅ 位置参数 / 关键字参数 / 默认参数 / 可变参数
- ✅ 局部变量 vs 全局变量（作用域）
- ✅ 函数可作为参数传递
- ✅ lambda 匿名函数
- ✅ docstring 写文档

明天学：文件读写与异常处理，让程序能"记住"数据。
