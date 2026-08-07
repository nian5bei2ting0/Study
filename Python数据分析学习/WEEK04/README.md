# WEEK04 · 机器学习入门（scikit-learn）+ AI 全景图与提示词工程

> 前置：已完成 WEEK01（Python 语法）+ WEEK02（NumPy/Pandas）+ WEEK03（可视化/EDA）。
> 目标：用 7 天建立机器学习与 AI 的完整认知，能跑通分类/回归/聚类全流程，并掌握与大模型协作的提示词工程。
> 每天预计 2 小时（看 30 + 敲 60 + 练 30）。
> 唯一原则：**每个模型都要自己跑一遍，看懂输出指标，不要只看代码。**

---

## 本周学习路线

| Day | 主题 | 关键词 | 产出 |
|-----|------|--------|------|
| 1 | ML 全景 + sklearn 入门 | 监督/无监督/训练预测/鸢尾花 | ml_intro.py |
| 2 | 数据预处理与特征工程 | 缺失/编码/标准化/管道 | preprocessing.py |
| 3 | 监督学习-分类 | KNN/逻辑回归/决策树/混淆矩阵 | classification.py |
| 4 | 监督学习-回归+评估 | 线性回归/MAE/RMSE/交叉验证 | regression.py |
| 5 | 无监督学习 | KMeans/DBSCAN/PCA/轮廓系数 | unsupervised.py |
| 6 | AI 全景图与大模型基础 | AI 三次浪潮/LLM/Token/能力边界 | ai_landscape.md |
| 7 | 提示词工程实战+综合项目 | CRISPE/少样本/思维链/端到端项目 | prompt_engineering.md + ml_project.py |
| 8 | Prompt 进阶：Few-shot 与 CoT | 少样本提示/思维链/自洽性/组合 | Day8_prompt_advanced.py |

> Day6-8 偏认知与协作能力，代码量少但**对实际工作影响最大**——AI 时代会用大模型比会写算法更重要。

---

## 每日文件结构

```
WEEK04/
├── README.md
├── Day1_ML全景与sklearn入门.md
├── Day2_数据预处理与特征工程.md
├── Day3_监督学习分类.md
├── Day4_监督学习回归与评估.md
├── Day5_无监督学习.md
├── Day6_AI全景图与大模型基础.md
├── Day7_提示词工程实战.md
├── Day8_Prompt进阶_Fewshot与CoT.md
└── code/
    ├── Day1_ml_intro.py
    ├── Day2_preprocessing.py
    ├── Day3_classification.py
    ├── Day4_regression.py
    ├── Day5_unsupervised.py
    ├── Day7_ml_project.py
    └── Day8_prompt_advanced.py
```

---

## 环境准备

### 1. 安装库

```bash
python -m pip install numpy pandas matplotlib seaborn scikit-learn
```

### 2. 推荐版本

- scikit-learn ≥ 1.4
- numpy / pandas / matplotlib / seaborn（沿用前几周）

### 3. 验证

```bash
python -c "import sklearn; print(sklearn.__version__)"
```

### 4. Day6-7 额外准备

- 一个大模型账号（ChatGPT / Claude / 智谱 / 通义 / 文心 任一）
- 后面会教你如何用提示词驱动大模型，**不需要本地部署大模型**

---

## 学习方法

1. **先看**：通读当日 .md，理解概念与原理。
2. **再敲**：把 code/ 下示例逐行敲进编辑器并运行，**看懂每个指标**。
3. **改一改**：换数据、换参数、换模型，观察指标变化。
4. **做练习**：完成 .md 末尾"每日练习"。
5. **对答案**：练习答案在 .md 末尾折叠区。

> 机器学习最大的坑是"会调库不懂原理"。**每个模型至少能用自己的话讲清楚它怎么工作**。

---

## 学完本周你能做到

- [ ] 看到问题能判断是分类/回归/聚类
- [ ] 跑通 sklearn 的训练-预测-评估全流程
- [ ] 用预处理 Pipeline 让代码规范
- [ ] 看懂准确率/精确率/召回率/F1/RMSE 等指标
- [ ] 画出 AI 全景图，分清 ML/DL/LLM 关系
- [ ] 用提示词工程让大模型稳定产出可用结果
- [ ] 用 Few-shot 控制输出格式，用 CoT 提升推理准确率

下一周（WEEK05，后续开）：项目实战与作品集。
