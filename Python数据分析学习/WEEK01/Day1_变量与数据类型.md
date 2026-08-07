# Day 1 · 环境搭建 + 变量与数据类型

> 今日目标：装好 Python，写出第一行代码，理解"变量"和"数据类型"。
> 预计时间：1.5 小时

---

## 一、安装 Python

### 1. 下载安装
1. 打开 https://www.python.org/downloads/
2. 下载 **Python 3.10 及以上**版本
3. 安装时**务必勾选** `Add Python to PATH`（非常重要！）
4. 一路 Next 完成安装

### 2. 验证安装
打开终端（Windows: PowerShell；Mac: 终端），输入：

```bash
python --version
```

看到类似 `Python 3.12.x` 即成功。若提示"找不到命令"，说明安装时没勾选 PATH，重装并勾选。

### 3. 选一个编辑器
推荐 **VS Code** 或 **Cursor**。安装后建一个文件夹 `WEEK01/code/` 专门放代码。

---

## 二、第一个程序：Hello World

新建文件 `code/Day1_hello.py`，敲入：

```python
print("Hello, Python!")
print("你好，Python！")
```

运行：

```bash
python code/Day1_hello.py
```

### 知识点
- `print()` 是 Python 内置函数，作用是把括号里的内容打印到屏幕。
- 字符串要用**引号**包起来，单引号 `'...'` 或双引号 `"..."` 都可以。

---

## 三、变量：给数据起名字

```python
name = "小明"
age = 18
print(name)
print(age)
```

### 知识点
- `变量名 = 值` 就完成了一次"赋值"。
- 变量名规则：字母/数字/下划线，**不能以数字开头**，不能用关键字（如 if、for）。
- 命名习惯：用英文、小写+下划线，如 `user_age`（不要用拼音 `yonghu_nianling`）。

---

## 四、四大基本数据类型

| 类型 | 关键字 | 例子 | 说明 |
|------|--------|------|------|
| 整数 | `int` | `18`, `-5`, `0` | 没有小数点的数 |
| 浮点数 | `float` | `3.14`, `-0.5` | 带小数点的数 |
| 字符串 | `str` | `"hello"`, `'a'` | 引号包起来的文本 |
| 布尔 | `bool` | `True`, `False` | 只有这两个值，首字母大写 |

```python
a = 10            # int
b = 3.14          # float
c = "Python"      # str
d = True          # bool

print(type(a))    # <class 'int'>
print(type(b))    # <class 'float'>
print(type(c))    # <class 'str'>
print(type(d))    # <class 'bool'>
```

### 用 `type()` 查看类型
这是你以后排错最常用的工具之一。

---

## 五、类型转换

```python
age_str = "18"
age_num = int(age_str)      # 字符串 -> 整数
print(age_num + 2)           # 20

price = 9.9
price_int = int(price)       # 浮点 -> 整数（直接截断小数）
print(price_int)             # 9

num = 100
text = str(num)              # 数字 -> 字符串
print("数字是 " + text)
```

常见转换函数：`int()` `float()` `str()` `bool()`。

注意：`int("abc")` 会报错，因为 "abc" 不是数字字符串。

---

## 六、输入：让程序和用户互动

```python
name = input("请输入你的名字：")
print("你好，" + name + "！")
```

### 知识点
- `input()` 会暂停程序等待用户输入，回车后返回**字符串**。
- 即使你输入的是数字，得到的也是字符串，需要 `int()` 转换。

```python
age = input("年龄：")      # 输入 18
print(age + 1)             # 报错！字符串不能加整数
print(int(age) + 1)        # 正确：19
```

---

## 七、f-string：格式化字符串（重点！）

Python 3.6+ 推荐写法，超级常用：

```python
name = "小明"
age = 18
height = 1.75

print(f"我叫{name}，今年{age}岁，身高{height}米")
# 我叫小明，今年18岁，身高1.75米

print(f"明年我{age + 1}岁")   # 花括号里可以写表达式
# 明年我19岁
```

> 以后写程序 80% 的输出都用 f-string，务必熟练。

---

## 八、注释

```python
# 这是单行注释，# 后面的内容不会被运行
print("这行会执行")   # 行尾注释也可以

"""
这是多行注释（其实是字符串）
可以写好几行
"""
```

注释是写给**人**看的，不是写给机器的。好的代码 + 好的注释 = 别人（和未来的你）能看懂。

---

## 每日练习

1. 写程序：用 `input` 让用户输入名字和年龄，用 f-string 输出"XX 今年 X 岁，明年 X+1 岁"。
2. 写程序：输入一个圆的半径（字符串），计算并输出圆的面积（π 取 3.14）。
3. 思考：`print(type("123"))` 和 `print(type(123))` 输出有什么不同？为什么？

---

<details>
<summary>参考答案（先自己做，做完再展开）</summary>

**练习 1**

```python
name = input("名字：")
age = int(input("年龄："))
print(f"{name} 今年 {age} 岁，明年 {age + 1} 岁")
```

**练习 2**

```python
r = float(input("半径："))
area = 3.14 * r * r
print(f"半径 {r} 的圆面积是 {area}")
```

**练习 3**

`type("123")` 是 `<class 'str'>`，`type(123)` 是 `<class 'int'>`。前者是字符串（文本），后者是整数（数字），类型不同，能做的运算也不同。

</details>

---

## 今日小结

今天你学会了：
- ✅ 安装并运行 Python
- ✅ `print()` 输出、`input()` 输入
- ✅ 变量与四大基本类型（int / float / str / bool）
- ✅ `type()` 查类型、`int()/float()/str()` 转类型
- ✅ f-string 格式化输出

明天学：运算符与字符串方法。今天没搞懂的，明天还会用到，别担心。
