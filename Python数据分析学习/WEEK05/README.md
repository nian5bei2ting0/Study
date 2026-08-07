# WEEK05 · 经典机器学习与 Scikit-learn 深入

> 前置：已完成 WEEK01-04（Python / NumPy·Pandas / 可视化·EDA / ML 入门 + AI 全景）。
> 目标：用 7 天系统掌握经典机器学习算法家族，能独立完成端到端 ML 项目。
> 与 WEEK04 区别：WEEK04 是"入门概览"，本周是"算法深挖 + 工程化实战"。
> 每天预计 2 小时（看 30 + 敲 70 + 练 20）。
> 唯一原则：**每个算法都要能用自己的话讲清原理，并知道何时用、何时不用。**

---

## 本周学习路线

| Day | 主题 | 关键词 | 产出 |
|-----|------|--------|------|
| 1 | ML 基础回顾与 sklearn 工作流 | 偏差方差/训练评估/estimator API | workflow.py |
| 2 | 线性模型家族 | 线性回归/岭/Lasso/逻辑回归/SVM | linear_models.py |
| 3 | 树模型 | 决策树/随机森林/GBDT/特征重要性 | tree_models.py |
| 4 | 朴素贝叶斯与 KNN | 贝叶斯公式/高斯NB/KNN 距离 | nb_knn.py |
| 5 | 模型选择与超参调优 | GridSearch/RandomSearch/Pipeline 调参 | tuning.py |
| 6 | 模型评估深入 | K折/分层/ROC/PR/学习曲线 | evaluation.py |
| 7 | 综合实战 | 端到端 ML 项目（数据→调参→评估→部署） | project.py |

---

## 每日文件结构

```
WEEK05/
├── README.md
├── Day1_ML基础与sklearn工作流.md
├── Day2_线性模型家族.md
├── Day3_树模型.md
├── Day4_朴素贝叶斯与KNN.md
├── Day5_模型选择与超参调优.md
├── Day6_模型评估深入.md
├── Day7_综合实战.md
└── code/
    ├── Day1_workflow.py
    ├── Day2_linear_models.py
    ├── Day3_tree_models.py
    ├── Day4_nb_knn.py
    ├── Day5_tuning.py
    ├── Day6_evaluation.py
    └── Day7_project.py
```

---

## 环境准备

```bash
python -m pip install numpy pandas matplotlib seaborn scikit-learn
```

推荐版本：scikit-learn ≥ 1.4

---

## 学完本周你能做到

- [ ] 解释偏差-方差权衡，判断模型过/欠拟合
- [ ] 用线性/树/贝叶斯/KNN 四大算法家族解决实际问题
- [ ] 用 Pipeline + GridSearchCV 做规范调参
- [ ] 看懂 ROC/PR 曲线，选合适阈值
- [ ] 完成一个端到端 ML 项目并保存模型

下一周（WEEK06，后续开）：深度学习入门（PyTorch）。
