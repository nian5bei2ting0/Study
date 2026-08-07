# Day 7 · 综合实战：端到端 ML 项目

> 今日目标：完成一个端到端 ML 项目，覆盖数据→特征→调参→评估→保存全流程。
> 预计时间：2.5 小时

---

## 一、项目说明

**场景**：信用卡欺诈检测（二分类，极不平衡）。

用 `make_classification` 造一份模拟数据，完整走通：

1. 数据生成与 EDA
2. 处理类别不平衡
3. 多模型对比
4. Pipeline + GridSearch 调参
5. 用 PR/ROC 评估（不平衡数据）
6. 阈值调优满足业务约束
7. 保存模型

涉及知识点：全周覆盖。

---

## 二、完整代码

```python
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import joblib

from sklearn.datasets import make_classification
from sklearn.model_selection import (train_test_split, StratifiedKFold,
                                       cross_validate, GridSearchCV)
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier, HistGradientBoostingClassifier
from sklearn.naive_bayes import GaussianNB
from sklearn.neighbors import KNeighborsClassifier
from sklearn.metrics import (classification_report, confusion_matrix,
                              roc_curve, auc, RocCurveDisplay,
                              precision_recall_curve, average_precision_score,
                              PrecisionRecallDisplay)

plt.rcParams["font.sans-serif"] = ["SimHei", "Microsoft YaHei"]
plt.rcParams["axes.unicode_minus"] = False

# ===== 1. 生成不平衡数据 =====
X, y = make_classification(n_samples=5000, n_features=20, n_informative=10,
                            n_redundant=5, n_classes=2, weights=[0.95, 0.05],
                            random_state=42)
df = pd.DataFrame(X, columns=[f"f{i}" for i in range(20)])
df["label"] = y
print(f"样本数: {len(df)}, 欺诈占比: {y.mean():.2%}")

# ===== 2. EDA =====
print(df.describe())
print(df["label"].value_counts())
df["label"].value_counts().plot.bar(); plt.title("类别分布"); plt.show()

# ===== 3. 划分 =====
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2,
                                            random_state=42, stratify=y)
print(f"训练: {len(X_tr)}, 测试: {len(X_te)}")

# ===== 4. 多模型对比（带 class_weight） =====
models = {
    "LogReg": Pipeline([("sc", StandardScaler()),
                         ("m", LogisticRegression(class_weight="balanced", max_iter=5000))]),
    "RF": RandomForestClassifier(100, class_weight="balanced", random_state=42, n_jobs=-1),
    "HistGBDT": HistGradientBoostingClassifier(random_state=42),
    "NB": GaussianNB(),
    "KNN": Pipeline([("sc", StandardScaler()), ("m", KNeighborsClassifier(5))]),
}

cv = StratifiedKFold(5, shuffle=True, random_state=42)
results = []
for name, m in models.items():
    r = cross_validate(m, X_tr, y_tr, cv=cv,
                       scoring=["accuracy", "precision", "recall", "f1", "roc_auc"])
    results.append({
        "model": name,
        "f1": r["test_f1"].mean(),
        "recall": r["test_recall"].mean(),
        "precision": r["test_precision"].mean(),
        "auc": r["test_roc_auc"].mean(),
    })
print("\n模型对比:")
print(pd.DataFrame(results).sort_values("f1", ascending=False))

# ===== 5. 选最佳模型调参（以 HistGBDT 为例） =====
best_model = HistGradientBoostingClassifier(random_state=42)
param_grid = {
    "max_iter": [100, 200],
    "learning_rate": [0.05, 0.1, 0.2],
    "max_depth": [3, 5, None],
}
gs = GridSearchCV(best_model, param_grid, cv=cv, scoring="f1", n_jobs=-1)
gs.fit(X_tr, y_tr)
print(f"\n最佳参数: {gs.best_params_}")
print(f"最佳 CV F1: {gs.best_score_:.4f}")

# ===== 6. 测试集评估 =====
final = gs.best_estimator_
y_pred = final.predict(X_te)
y_score = final.predict_proba(X_te)[:, 1]

print("\n测试集分类报告:")
print(classification_report(y_te, y_pred))

cm = confusion_matrix(y_te, y_pred)
print("混淆矩阵:\n", cm)

# ===== 7. ROC 与 PR 曲线 =====
fpr, tpr, _ = roc_curve(y_te, y_score)
print(f"AUC: {auc(fpr, tpr):.4f}")
RocCurveDisplay.from_predictions(y_te, y_score); plt.show()

prec, rec, thr = precision_recall_curve(y_te, y_score)
print(f"AP: {average_precision_score(y_te, y_score):.4f}")
PrecisionRecallDisplay.from_predictions(y_te, y_score); plt.show()

# ===== 8. 阈值调优：满足召回 ≥ 0.9 =====
target_recall = 0.9
idx = next(i for i, r in enumerate(rec) if r >= target_recall)
best_thr = thr[idx] if idx < len(thr) else 0.5
y_pred_custom = (y_score >= best_thr).astype(int)
print(f"\n阈值 {best_thr:.3f} 下（召回≥{target_recall}）:")
print(classification_report(y_te, y_pred_custom))

# ===== 9. 保存模型 =====
joblib.dump({"model": final, "threshold": best_thr}, "fraud_model.pkl")
print("\n模型已保存 fraud_model.pkl")
```

---

## 三、知识点对照

| 代码段 | 用到的知识（哪一天） |
|--------|-------------------|
| `make_classification` 不平衡 | Day1 数据 |
| `class_weight="balanced"` | Day2 逻辑回归 |
| `HistGradientBoostingClassifier` | Day3 树模型 |
| `StratifiedKFold` | Day6 分层 CV |
| `cross_validate` 多指标 | Day6 |
| `GridSearchCV` | Day5 调参 |
| ROC / PR | Day6 评估 |
| 阈值调优 | Day6 |
| `joblib` 保存 | Day1 工作流 |

---

## 四、进阶挑战

1. **采样法**：用 `imblearn` 的 SMOTE 过采样，对比 class_weight。
2. **成本敏感**：把假阴/假阳成本不同纳入评估。
3. **特征工程**：加交互特征、分箱，看 F1 是否提升。
4. **模型解释**：用 `shap` 或 `permutation_importance` 解释预测。
5. **部署**：写一个 `predict.py`，加载模型对新数据预测。

---

## 每日练习（必做）

1. 完整敲一遍代码并跑通。
2. 至少完成 1 个进阶挑战。
3. 写 200 字业务结论：基于 PR 曲线，建议阈值取多少，为什么。

---

# 🎉 恭喜完成第六周！

你现在具备：
- 系统的经典 ML 算法知识（线性/树/贝叶斯/KNN）
- 规范的调参流程（Pipeline + GridSearch）
- 科学的评估能力（CV/ROC/PR/阈值调优）
- 端到端项目经验

下一周（WEEK06）：深度学习入门（PyTorch）。
