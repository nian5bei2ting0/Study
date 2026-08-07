# Day7 综合实战：命令行待办清单
# 运行：python code/Day7_todo_list.py
# 详细讲解见 Day7_综合实战.md

import json
import os

TODO_FILE = "todos.json"


def load_todos():
    if not os.path.exists(TODO_FILE):
        return []
    try:
        with open(TODO_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    except (json.JSONDecodeError, OSError):
        print("⚠️ 待办文件损坏，已重置为空。")
        return []


def save_todos(todos):
    with open(TODO_FILE, "w", encoding="utf-8") as f:
        json.dump(todos, f, ensure_ascii=False, indent=2)


def show_todos(todos):
    if not todos:
        print("（暂无待办）")
        return
    print("\n=== 待办清单 ===")
    for i, t in enumerate(todos, 1):
        status = "✅" if t["done"] else "⬜"
        print(f"{i}. {status} {t['title']}")
    print()


def add_todo(todos):
    title = input("待办内容：").strip()
    if not title:
        print("内容不能为空")
        return
    todos.append({"title": title, "done": False})
    save_todos(todos)
    print(f"已添加：{title}")


def done_todo(todos):
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
