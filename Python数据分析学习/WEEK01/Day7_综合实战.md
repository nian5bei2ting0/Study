# Day 7 · 综合实战：命令行待办清单

> 今日目标：把前 6 天学的全部串起来，做一个能真正用的小程序。
> 预计时间：2.5 小时

---

## 一、项目说明

做一个**命令行待办清单（To-Do List）**，功能：

1. 查看所有待办
2. 添加待办
3. 完成待办（标记为已完成）
4. 删除待办
5. 保存到文件，下次打开还在
6. 退出

涉及知识点：变量、字符串、流程控制、列表、字典、函数、文件读写、异常处理——**前 6 天全覆盖**。

---

## 二、数据结构设计

每条待办用一个字典表示：

```python
{"title": "买牛奶", "done": False}
```

整个清单是一个列表：

```python
todos = [
    {"title": "买牛奶", "done": False},
    {"title": "写作业", "done": True},
]
```

存到文件 `todos.json`。

---

## 三、完整代码（先看懂，再自己敲）

```python
import json
import os

TODO_FILE = "todos.json"


def load_todos():
    """从文件加载待办，文件不存在返回空列表。"""
    if not os.path.exists(TODO_FILE):
        return []
    try:
        with open(TODO_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    except (json.JSONDecodeError, OSError):
        print("⚠️ 待办文件损坏，已重置为空。")
        return []


def save_todos(todos):
    """保存待办到文件。"""
    with open(TODO_FILE, "w", encoding="utf-8") as f:
        json.dump(todos, f, ensure_ascii=False, indent=2)


def show_todos(todos):
    """显示所有待办。"""
    if not todos:
        print("（暂无待办）")
        return
    print("\n=== 待办清单 ===")
    for i, t in enumerate(todos, 1):
        status = "✅" if t["done"] else "⬜"
        print(f"{i}. {status} {t['title']}")
    print()


def add_todo(todos):
    """添加待办。"""
    title = input("待办内容：").strip()
    if not title:
        print("内容不能为空")
        return
    todos.append({"title": title, "done": False})
    save_todos(todos)
    print(f"已添加：{title}")


def done_todo(todos):
    """标记完成。"""
    show_todos(todos)
    try:
        idx = int(input("完成第几条？")) - 1
        if 0 <= idx < len(todos):
            todos[idx]["done"] = True
            save_todos(todos)
            print("已标记完成")
        else:
            print("序号超出范围")
    except ValueError:
        print("请输入数字")


def delete_todo(todos):
    """删除待办。"""
    show_todos(todos)
    try:
        idx = int(input("删除第几条？")) - 1
        if 0 <= idx < len(todos):
            removed = todos.pop(idx)
            save_todos(todos)
            print(f"已删除：{removed['title']}")
        else:
            print("序号超出范围")
    except ValueError:
        print("请输入数字")


def main():
    todos = load_todos()
    print("📋 待办清单程序（输入 help 查看命令）")

    while True:
        cmd = input("\n> ").strip().lower()

        if cmd in ("help", "h", "?"):
            print("命令：list 查看 | add 添加 | done 完成 | del 删除 | quit 退出")
        elif cmd in ("list", "ls", "l"):
            show_todos(todos)
        elif cmd in ("add", "a"):
            add_todo(todos)
        elif cmd in ("done", "d"):
            done_todo(todos)
        elif cmd in ("del", "delete"):
            delete_todo(todos)
        elif cmd in ("quit", "q", "exit"):
            print("再见！")
            break
        else:
            print("未知命令，输入 help 查看帮助")


if __name__ == "__main__":
    main()
```

---

## 四、逐段拆解（对照知识点）

| 代码段 | 用到的知识（哪一天） |
|--------|-------------------|
| `import json, os` | Day6 文件 + 标准库 |
| `TODO_FILE = "todos.json"` | Day1 变量 |
| `load_todos / save_todos` | Day5 函数 + Day6 文件异常 |
| `show_todos` 的 `for i, t in enumerate(...)` | Day4 列表遍历 |
| `f"{i}. {status} {t['title']}"` | Day1 f-string |
| `done_todo` 的 `try/except ValueError` | Day6 异常 |
| `cmd in ("list", "ls", "l")` | Day2 字符串 + Day3 if |
| `while True: ... break` | Day3 while 循环 |
| `todos.append({"title":..., "done":...})` | Day4 列表 + 字典 |
| `if __name__ == "__main__":` | Day5 函数入口（标准写法） |

---

## 五、运行方式

```bash
python code/Day7_todo_list.py
```

试一次完整流程：

```
> add
待办内容：学完 Python 第一周
已添加：学完 Python 第一周

> add
待办内容：写一个爬虫
已添加：写一个爬虫

> list
=== 待办清单 ===
1. ⬜ 学完 Python 第一周
2. ⬜ 写一个爬虫

> done
完成第几条？1
已标记完成

> list
=== 待办清单 ===
1. ✅ 学完 Python 第一周
2. ⬜ 写一个爬虫

> quit
再见！
```

退出后再运行，数据还在（因为存到了 `todos.json`）。

---

## 六、进阶挑战（可选）

完成基础版后，挑战加功能：

1. **优先级**：每条待办加 `priority`（高/中/低），按优先级排序显示。
2. **按状态过滤**：命令 `list done` 只看已完成，`list todo` 只看未完成。
3. **修改待办**：加 `edit` 命令修改标题。
4. **截止日期**：加 `due` 字段，过期的标红提示。
5. **统计**：加 `stats` 命令显示总数、完成率。

> 每加一个功能，就复习一遍对应知识点。能做完 3 个进阶，说明你真的入门了。

---

## 每日练习（必做）

1. 把上面的代码**完整敲一遍**并运行成功。
2. 至少完成 1 个进阶挑战。
3. 故意制造 3 个错误（输错序号、文件改名、删空内容），看程序怎么处理，理解异常机制。

---

## 今日小结

- ✅ 用一个真实小程序把前 6 天的知识全部串起来
- ✅ 函数拆分让代码清晰可维护
- ✅ 文件持久化让数据不丢失
- ✅ 异常处理让程序健壮不崩溃

---

# 🎉 恭喜完成第一周！

你现在已经掌握了 Python 的核心语法。建议接下来：
- **WEEK02**（后续开）：面向对象、模块与包、常用标准库
- **方向选择**：数据分析（pandas）/ 爬虫（requests+bs4）/ 自动化（脚本+定时任务）/ Web（Flask）

无论选哪个方向，本周打好的语法地基都够用了。继续加油！
