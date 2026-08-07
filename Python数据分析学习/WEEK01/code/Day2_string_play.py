# Day2 示例：运算符与字符串
# 运行：python code/Day2_string_play.py

# 算术运算
print(7 / 2)    # 3.5
print(7 // 2)   # 3
print(7 % 2)   # 1
print(7 ** 2)  # 49

# 字符串切片
s = "Python"
print(s[0])      # P
print(s[-1])     # n
print(s[1:4])    # yth
print(s[::-1])   # nohtyP

# 字符串方法
text = "  Hello, World  "
print(text.strip())
print(text.upper())
print(text.replace("World", "Python"))
print(text.split(","))
print(len(text))
print("World" in text)

# 手机号校验练习
phone = input("手机号：")
if len(phone) == 11 and phone.isdigit():
    print("合法")
else:
    print("不合法")
