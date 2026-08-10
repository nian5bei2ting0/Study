# Day7 · 综合应用

> 把本周学的统计方法串起来，用一个数据集走完整分析流程。

---

## 一、方法论：统计在分析流程中的位置

### 1. 完整分析流程

```
1. 理解数据（Day1-2）
   - 看分布、看形态
   - 选合适的统计量
2. 评估样本（Day3）
   - 抽样是否无偏
   - 样本量是否足够
3. 发现关系（Day5）
   - 相关性分析
   - 卡方检验
4. 验证差异（Day4）
   - 假设检验
   - 判断差异是否真实
5. 建模解释（Day6）
   - 回归分析
   - 解读系数
6. 结论与建议
   - 统计显著 + 业务显著
```

### 2. 统计方法在后续周的应用

| 本周方法 | 后续应用 |
|---------|---------|
| 分布判断 | WEEK01 EDA 的分布诊断 |
| 描述统计量 | WEEK01 报告的数据描述 |
| 中心极限定理 | WEEK02 A/B 测试的理论基础 |
| 假设检验 | WEEK02 A/B 测试显著性 |
| 相关性 | WEEK02 特征选择 |
| 回归 | WEEK02 建模、WEEK03 预测 |
| 置信区间 | WEEK03 预测不确定性 |

---

## 二、案例实战：某电商用户消费分析

### 1. 业务问题
- 某电商想了解用户消费行为
- 数据：1000 个用户的消费记录
- 目标：发现影响消费金额的因素

### 2. 第一步：理解数据（Day1-2）

```python
import numpy as np
import pandas as pd
from scipy import stats

df = pd.read_csv("user_spending.csv")
# 字段：age, income, days_since_register, is_vip, amount

# 分布
print(df['amount'].describe())
print("偏度:", stats.skew(df['amount']))  # 正偏
print("峰度:", stats.kurtosis(df['amount']))

# 长尾 → 用中位数
print("均值:", df['amount'].mean())
print("中位数:", df['amount'].median())  # 更真实
```

**发现**：amount 正偏长尾，均值 > 中位数，用中位数描述典型用户。

### 3. 第二步：评估样本（Day3）

```python
# 检查样本是否随机
print(df['is_vip'].value_counts(normalize=True))
# VIP 占比是否合理？
```

**自检**：抽样是否有偏？VIP 占比是否符合实际？

### 4. 第三步：发现关系（Day5）

```python
# 相关性
print(df[['age','income','days_since_register','amount']].corr())

# 卡方：VIP 与否是否影响是否大额消费
df['high_spender'] = df['amount'] > df['amount'].median()
table = pd.crosstab(df['is_vip'], df['high_spender'])
chi2, p, _, _ = stats.chi2_contingency(table)
print(f"卡方 p={p:.4f}")
```

**发现**：
- income 与 amount 强相关（Pearson 0.6）
- VIP 与大额消费显著相关（p < 0.05）

### 5. 第四步：验证差异（Day4）

```python
# VIP vs 非VIP 消费金额差异
vip = df[df['is_vip']==1]['amount']
non_vip = df[df['is_vip']==0]['amount']
t, p = stats.ttest_ind(vip, non_vip)
print(f"t={t:.3f}, p={p:.4f}")

# 效应量
diff = vip.mean() - non_vip.mean()
print(f"差异: {diff:.2f}")
```

**发现**：VIP 消费显著高于非 VIP（p<0.01，差异 200 元）

### 6. 第五步：建模解释（Day6）

```python
import statsmodels.api as sm

X = df[['age','income','days_since_register','is_vip']]
X = sm.add_constant(X)
Y = df['amount']
model = sm.OLS(Y, X).fit()
print(model.summary())
```

**解读**：
- R² = 0.45（解释了 45% 变异）
- income 系数显著（p<0.001），每增加 1 万收入，消费增加 X 元
- is_vip 系数显著（p<0.01），VIP 比非 VIP 多消费 Y 元
- age 不显著（p>0.05）

### 7. 第六步：结论

**统计结论**：
- 收入和 VIP 身份显著影响消费金额
- 年龄影响不显著
- 模型解释 45% 变异

**业务建议**：
- 重点运营高收入用户
- 提升 VIP 转化（VIP 消费更高）
- 年龄不是有效分层维度

**边界**：
- 样本 1000，VIP 占比 20%，结论对 VIP 群体代表性有限
- R² 0.45，还有 55% 变异未解释，需更多特征

---

## 三、本周回顾

### 方法论地图

```
统计基础
├── Day1：概率 + 四大分布（正态/二项/泊松/对数正态）
├── Day2：描述统计 + 分布形态（偏度/峰度/箱线图）
├── Day3：抽样 + CLT + 标准误 + 置信区间
├── Day4：假设检验 + p值 + 两类错误 + t检验
├── Day5：相关性（Pearson/Spearman）+ 卡方检验
├── Day6：回归（线性/逻辑）+ R² + 残差分析
└── Day7：综合应用 + 完整流程
```

### 自测清单
- [ ] 能判断数据服从什么分布
- [ ] 能选合适的统计量描述数据
- [ ] 能解释 CLT 为什么让样本能推断总体
- [ ] 能做假设检验并正确解读 p 值
- [ ] 能算相关性并区分 Pearson/Spearman
- [ ] 能做线性/逻辑回归并解读系数
- [ ] 能用统计方法走完整分析流程

### 关键原则回顾
1. **分布决定方法**：长尾用中位数/IQR，正态用均值/标准差
2. **样本质量 > 样本量**：抽样偏差最致命
3. **相关 ≠ 因果**：警惕混淆变量
4. **统计显著 ≠ 业务显著**：看效应量
5. **检查假设**：回归五大假设、检验前提

### 下周预告
WEEK01 进入"分析思维与方法论基础"，把本周的统计基础用到业务分析框架中。
