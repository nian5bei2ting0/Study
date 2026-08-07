# Day 6 · 模型评估深入

> 今日目标：掌握交叉验证变体、ROC/PR 曲线、学习曲线，能科学评估模型。
> 预计时间：2 小时

---

## 一、交叉验证变体

### 1. K 折（默认）

```python
from sklearn.model_selection import cross_val_score
scores = cross_val_score(model, X, y, cv=5)
```

### 2. 分层 K 折（分类必用）

保持每折类别比例，**类别不平衡时必备**：

```python
from sklearn.model_selection import StratifiedKFold
cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
scores = cross_val_score(model, X, y, cv=cv)
```

> 分类任务**永远用分层**，cross_val_score 对分类默认就是分层。

### 3. 留一法 LOO

每折只留 1 个样本测试，N 个样本就 N 折：

```python
from sklearn.model_selection import LeaveOneOut
scores = cross_val_score(model, X, y, cv=LeaveOneOut())
```

> 小数据集可用，大数据太慢。

### 4. 时间序列分割

不能随机打乱时间数据，要按时间切：

```python
from sklearn.model_selection import TimeSeriesSplit
cv = TimeSeriesSplit(n_splits=5)
```

---

## 二、cross_validate 多指标

```python
from sklearn.model_selection import cross_validate
result = cross_validate(model, X, y, cv=5,
                         scoring=["accuracy","precision_macro","recall_macro","f1_macro"],
                         return_train_score=True)
import pandas as pd
print(pd.DataFrame(result))
```

---

## 三、分类评估指标深入

### 1. 混淆矩阵

```python
from sklearn.metrics import confusion_matrix, ConfusionMatrixDisplay
cm = confusion_matrix(y_test, y_pred)
ConfusionMatrixDisplay(cm).plot()
```

### 2. 精确率/召回率/F1

```python
from sklearn.metrics import classification_report
print(classification_report(y_test, y_pred))
```

### 3. 多分类的 average

| average | 含义 |
|---------|------|
| macro | 各类平均（小类权重大） |
| micro | 全局算（大类主导） |
| weighted | 按样本数加权 |

> 不平衡数据看 **macro F1**，不被大类淹没。

---

## 四、ROC 曲线（二分类）

```python
from sklearn.metrics import roc_curve, auc, RocCurveDisplay
import matplotlib.pyplot as plt

# 用概率而非预测值
y_score = model.predict_proba(X_te)[:, 1]   # 正类概率
fpr, tpr, thresholds = roc_curve(y_te, y_score)
roc_auc = auc(fpr, tpr)
print("AUC:", roc_auc)

RocCurveDisplay.from_predictions(y_te, y_score)
plt.plot([0,1],[0,1],"--",color="gray")   # 随机基线
plt.title(f"ROC (AUC={roc_auc:.3f})")
plt.show()
```

### 读法
- X 轴：假正率 FPR
- Y 轴：真正率 TPR
- 对角线：随机猜测
- 越靠左上越好
- **AUC**：曲线下面积，1 完美，0.5 随机

### AUC 优点
- 与阈值无关（看模型整体区分能力）
- 对不平衡不敏感
- 跨模型可比

---

## 五、PR 曲线（不平衡数据更合适）

```python
from sklearn.metrics import precision_recall_curve, average_precision_score, PrecisionRecallDisplay

prec, rec, thr = precision_recall_curve(y_te, y_score)
ap = average_precision_score(y_te, y_score)
print("AP:", ap)

PrecisionRecallDisplay.from_predictions(y_te, y_score)
plt.title(f"PR (AP={ap:.3f})")
plt.show()
```

### ROC vs PR

| 场景 | 用 |
|------|-----|
| 类别平衡 | ROC |
| 类别极不平衡（如 1:100） | PR |
| 关注正类（少数类） | PR |

> 不平衡时 ROC 看起来都很好（FPR 被大量负类稀释），PR 更真实。

---

## 六、阈值调优

```python
# 找满足召回率 ≥ 0.9 的最高精确率
prec, rec, thr = precision_recall_curve(y_te, y_score)
target_recall = 0.9
idx = next(i for i, r in enumerate(rec) if r >= target_recall)
print(f"满足召回 {target_recall} 的阈值: {thr[idx] if idx < len(thr) else 'N/A'}")

# 用自定义阈值预测
y_pred_custom = (y_score >= thr[idx]).astype(int)
```

> 业务中常需"召回 ≥ X"或"精确 ≥ Y"，靠调阈值而非改模型。

---

## 七、回归评估指标

```python
from sklearn.metrics import (mean_absolute_error, mean_squared_error,
                              r2_score, mean_absolute_percentage_error)
import numpy as np

y_pred = model.predict(X_te)
print("MAE:", mean_absolute_error(y_te, y_pred))
print("MSE:", mean_squared_error(y_te, y_pred))
print("RMSE:", np.sqrt(mean_squared_error(y_te, y_pred)))
print("R²:", r2_score(y_te, y_pred))
print("MAPE:", mean_absolute_percentage_error(y_te, y_pred))
```

| 指标 | 单位 | 特点 |
|------|------|------|
| MAE | y | 直观抗异常 |
| RMSE | y | 对大误差敏感 |
| R² | 无量纲 | 0~1 |
| MAPE | % | 相对误差 |

---

## 八、学习曲线与验证曲线

### 1. 学习曲线（变样本数）

```python
from sklearn.model_selection import learning_curve
import numpy as np

ts, tr_s, te_s = learning_curve(model, X, y, cv=5,
                                  train_sizes=np.linspace(0.1, 1.0, 10))
plt.plot(ts, tr_s.mean(axis=1), label="train")
plt.plot(ts, te_s.mean(axis=1), label="test")
plt.fill_between(ts, tr_s.mean(axis=1)-tr_s.std(axis=1),
                 tr_s.mean(axis=1)+tr_s.std(axis=1), alpha=0.2)
plt.legend(); plt.xlabel("样本数"); plt.show()
```

读法：
- 两线都低 → 欠拟合（加复杂模型/加特征）
- 训练高测试低 → 过拟合（加数据/简化）
- 两线接近且高 → 刚好

### 2. 验证曲线（变超参）

```python
from sklearn.model_selection import validation_curve
param_range = range(1, 20)
tr_s, te_s = validation_curve(KNeighborsClassifier(), X, y,
                               param_name="n_neighbors", param_range=param_range, cv=5)
plt.plot(param_range, tr_s.mean(axis=1), label="train")
plt.plot(param_range, te_s.mean(axis=1), label="test")
plt.legend(); plt.xlabel("k"); plt.show()
```

---

## 每日练习

用 `load_breast_cancer`：

1. 训练逻辑回归，画 ROC 曲线，输出 AUC。
2. 画 PR 曲线，输出 AP，对比 ROC。
3. 找满足召回率 ≥ 0.95 的最高精确率阈值。
4. 用学习曲线判断随机森林是否过拟合。
5. 用验证曲线找 KNN 的最佳 K。

---

<details>
<summary>参考答案</summary>

```python
from sklearn.datasets import load_breast_cancer
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier
from sklearn.neighbors import KNeighborsClassifier
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
from sklearn.model_selection import train_test_split, learning_curve, validation_curve
from sklearn.metrics import (roc_curve, auc, RocCurveDisplay,
                              precision_recall_curve, average_precision_score,
                              PrecisionRecallDisplay)
import numpy as np, matplotlib.pyplot as plt

X, y = load_breast_cancer(return_X_y=True)
X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

# 1
lr = Pipeline([("sc",StandardScaler()),("m",LogisticRegression(max_iter=5000))]).fit(X_tr,y_tr)
y_score = lr.predict_proba(X_te)[:,1]
fpr,tpr,_ = roc_curve(y_te,y_score)
print("AUC:", auc(fpr,tpr))
RocCurveDisplay.from_predictions(y_te,y_score); plt.show()

# 2
prec,rec,_ = precision_recall_curve(y_te,y_score)
print("AP:", average_precision_score(y_te,y_score))
PrecisionRecallDisplay.from_predictions(y_te,y_score); plt.show()

# 3
idx = next(i for i,r in enumerate(rec) if r >= 0.95)
thr = _[idx] if idx < len(_) else 0
print("阈值:", thr, "精确:", prec[idx])

# 4
ts, tr_s, te_s = learning_curve(RandomForestClassifier(random_state=42), X, y, cv=5,
                                  train_sizes=np.linspace(0.1,1.0,10))
plt.plot(ts,tr_s.mean(axis=1),label="train"); plt.plot(ts,te_s.mean(axis=1),label="test")
plt.legend(); plt.show()

# 5
pr = range(1,20)
tr_s,te_s = validation_curve(Pipeline([("sc",StandardScaler()),("m",KNeighborsClassifier())]),
                              X, y, param_name="knn__n_neighbors", param_range=pr, cv=5)
plt.plot(pr,tr_s.mean(axis=1),label="train"); plt.plot(pr,te_s.mean(axis=1),label="test")
plt.legend(); plt.show()
```

</details>

---

## 今日小结

- ✅ 分层 K 折（分类必用）/ 时间序列分割
- ✅ cross_validate 多指标
- ✅ ROC + AUC（平衡数据）
- ✅ PR + AP（不平衡数据）
- ✅ 阈值调优满足业务约束
- ✅ 学习曲线诊断过/欠拟合
- ✅ 验证曲线找最佳超参
