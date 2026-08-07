# WEEK03 · 数据可视化（matplotlib / seaborn）+ EDA 实战

> 前置：已完成 WEEK01（Python 语法）+ WEEK02（NumPy/Pandas）。
> 目标：用 7 天掌握 Python 两大绘图库，并能独立完成一份完整的 EDA（探索性数据分析）报告。
> 每天预计 2 小时（看 30 + 敲 60 + 练 30）。
> 唯一原则：**所有图必须自己敲一遍并跑出结果，截图存档。**

---

## 本周学习路线

| Day | 主题 | 关键词 | 产出 |
|-----|------|--------|------|
| 1 | matplotlib 基础 | figure/axes/折线/子图/样式 | mpl_basic.py |
| 2 | matplotlib 常用图 | 柱/饼/散点/直方/箱线 | mpl_charts.py |
| 3 | seaborn 入门 | 主题/分布图/直方/KDE/箱线 | sns_basic.py |
| 4 | seaborn 进阶 | 关系/分类/回归/热力/分面 | sns_advanced.py |
| 5 | EDA 方法论 | 数据画像/缺失/分布/相关性 | eda_method.py |
| 6 | 多变量与异常 | 散点矩阵/相关热力/异常可视化 | eda_multivar.py |
| 7 | 综合实战 | 整合全周 → 完整 EDA 报告 | eda_report.py |

---

## 每日文件结构

```
WEEK03/
├── README.md
├── Day1_matplotlib基础.md
├── Day2_matplotlib常用图表.md
├── Day3_seaborn入门.md
├── Day4_seaborn进阶.md
├── Day5_EDA方法论.md
├── Day6_多变量与异常.md
├── Day7_综合实战.md
└── code/
    ├── Day1_mpl_basic.py
    ├── Day2_mpl_charts.py
    ├── Day3_sns_basic.py
    ├── Day4_sns_advanced.py
    ├── Day5_eda_method.py
    ├── Day6_eda_multivar.py
    └── Day7_eda_report.py
```

---

## 环境准备

### 1. 安装库

```bash
python -m pip install numpy pandas matplotlib seaborn
```

### 2. 推荐版本

- matplotlib ≥ 3.8
- seaborn ≥ 0.13

### 3. 中文显示（重要！）

matplotlib 默认不支持中文，会显示方块。在脚本开头加：

```python
import matplotlib.pyplot as plt
plt.rcParams["font.sans-serif"] = ["SimHei", "Microsoft YaHei", "Arial Unicode MS"]
plt.rcParams["axes.unicode_minus"] = False   # 负号正常显示
```

### 4. 验证

```bash
python -c "import matplotlib, seaborn; print(matplotlib.__version__, seaborn.__version__)"
```

---

## 学习方法

1. **先看**：通读当日 .md，理解图表用途。
2. **再敲**：把 code/ 下示例逐行敲进编辑器并运行，**每张图都要弹出来看到**。
3. **改一改**：改数据、改颜色、改参数，观察图的变化。
4. **做练习**：完成 .md 末尾"每日练习"。
5. **对答案**：练习答案在 .md 末尾折叠区。

> 可视化是"手艺活"，**看 100 张图不如自己画 10 张**。每张图都要运行、看效果、调参数。

---

## 学完本周你能做到

- [ ] 用 matplotlib 画 6 种基础图（线/柱/饼/散点/直方/箱线）
- [ ] 用 seaborn 画统计图（分布/关系/分类/回归/热力）
- [ ] 看到一份新数据知道从哪些角度做 EDA
- [ ] 处理缺失、异常、分布偏态的可视化诊断
- [ ] 输出一份含 8+ 张图的完整 EDA 报告

下一周（WEEK04，后续开）：机器学习入门（scikit-learn）。
