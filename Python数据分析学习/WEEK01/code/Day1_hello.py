# Day1 示例：变量与数据类型
# 运行：python code/Day1_hello.py

print("Hello, Python!")
print("你好，Python！")

# 四大基本类型
a = 10            # int
b = 3.14          # float
c = "Python"      # str
d = True          # bool

print(type(a))   # <class 'int'>
print(type(b))   # <class 'float'>
print(type(c))   # <class 'str'>
print(type(d))   # <class 'bool'>

# 类型转换
age_str = "18"
age_num = int(age_str)
print(age_num + 2)   # 20

# 输入与 f-string
name = input("请输入你的名字：")
age = int(input("请输入你的年龄："))
print(f"我叫{name}，今年{age}岁，明年{age + 1}岁")
