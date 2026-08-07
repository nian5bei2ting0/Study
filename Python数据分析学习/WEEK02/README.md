# WEEK02 · NumPy 与 Pandas 数据处理

> 前置：已完成 WEEK01（Python 核心语法）。
> 目标：用 7 天掌握 Python 数据分析两大基石库——NumPy（数值计算）与 Pandas（表格数据处理），能独立完成一个小型数据分析任务。
> 每天预计 2 小时（看 30 分钟 + 敲 60 分钟 + 练习 30 分钟）。
> 唯一原则：**所有代码必须自己敲一遍，不要复制粘贴。**

---

## 本周学习路线

| Day | 主题 | 关键词 | 产出 |
|-----|------|--------|------|
| 1 | NumPy 基础 | ndarray / 创建 / 属性 / 索引切片 | np_basic.py |
| 2 | NumPy 进阶 | 向量化运算 / 广播 / 布尔索引 / 通用函数 | np_advanced.py |
| 3 | Pandas 入门 | Series / DataFrame / 创建 / 索引 | pd_basic.py |
| 4 | 数据读取与清洗 | read_csv / 缺失值 / 类型转换 / 去重 | pd_clean.py |
| 5 | 筛选排序分组 | loc/iloc / query / sort / groupby / agg | pd_groupby.py |
| 6 | 合并透视时间 | merge / concat / pivot / resample | pd_merge.py |
| 7 | 综合实战 | 整合全周 → 电商销售分析 | sales_analysis.py |

---

## 每日文件结构

```
WEEK02/
├── README.md                   # 本文件
├── Day1_NumPy基础.md
├── Day2_NumPy进阶.md
├── Day3_Pandas入门.md
├── Day4_数据读取与清洗.md
├── Day5_筛选排序分组.md
├── Day6_合并透视时间序列.md
├── Day7_综合实战.md
└── code/
    ├── Day1_np_basic.py
    ├── Day2_np_advanced.py
    ├── Day3_pd_basic.py
    ├── Day4_pd_clean.py
    ├── Day5_pd_groupby.py
    ├── Day6_pd_merge.py
    └── Day7_sales_analysis.py
```

---

## 环境准备

### 1. 安装库

```bash
python -m pip install numpy pandas matplotlib
```

### 2. 推荐版本

- numpy ≥ 1.26
- pandas ≥ 2.2
- matplotlib ≥ 3.8（Day7 画图用）

### 3. 验证

```bash
python -c "import numpy, pandas; print(numpy.__version__, pandas.__version__)"
```

看到两个版本号即成功。

---

## 学习方法（与 WEEK01 一致）

1. **先看**：通读当日 .md，理解概念。
2. **再敲**：把 code/ 下当日示例逐行敲进编辑器并运行。
3. **改一改**：故意改几个值，观察输出变化。
4. **做练习**：完成 .md 末尾"每日练习"。
5. **对答案**：练习答案在 .md 末尾"参考答案"折叠区。

> 数据处理库 API 很多，**不要试图背全**。记住常用 20%，其余查文档：
> - NumPy: https://numpy.org/doc/stable/
> - Pandas: https://pandas.pydata.org/docs/

---

## 学完本周你能做到

- [ ] 用 NumPy 做向量化数值计算（比纯 Python 快几十倍）
- [ ] 用 Pandas 读取、清洗、筛选、聚合表格数据
- [ ] 处理缺失值、重复值、类型转换
- [ ] 做分组统计（groupby）和透视表
- [ ] 合并多张表、处理时间序列
- [ ] 独立完成一个电商销售数据分析小项目

下一周（WEEK03，后续再开）：数据可视化（matplotlib / seaborn）+ EDA 实战。
