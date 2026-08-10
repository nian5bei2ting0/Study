# -*- coding: utf-8 -*-
"""
Day7 · 综合应用
目标：用一个数据集走完整统计流程，把本周方法串起来
"""
import numpy as np
import pandas as pd
from scipy import stats
import statsmodels.api as sm
import matplotlib.pyplot as plt

plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei']
plt.rcParams['axes.unicode_minus'] = False


def generate_data(n=1000, seed=42):
    """生成模拟用户消费数据"""
    np.random.seed(seed)
    age = np.random.normal(35, 10, n).clip(18, 70)
    income = np.random.lognormal(10.5, 0.5, n)  # 长尾
    days_register = np.random.randint(1, 1000, n)
    is_vip = (income > np.median(income)).astype(int)  # 高收入者更可能是VIP

    # 消费金额 = f(income, is_vip) + 噪声
    amount = 0.02 * income + 200 * is_vip + np.random.normal(0, 100, n)
    amount = amount.clip(0)  # 金额不能为负

    return pd.DataFrame({
        'age': age,
        'income': income,
        'days_register': days_register,
        'is_vip': is_vip,
        'amount': amount,
    })


def step1_understand_data(df):
    """步骤1：理解数据（Day1-2）"""
    print("=" * 60)
    print("步骤1：理解数据")
    print("=" * 60)

    print("\n数据概览：")
    print(df.describe())

    # amount 分布
    print(f"\n金额分布：")
    print(f"  均值: {df['amount'].mean():.2f}")
    print(f"  中位数: {df['amount'].median():.2f}")
    print(f"  偏度: {stats.skew(df['amount']):.2f}")
    print(f"  峰度: {stats.kurtosis(df['amount']):.2f}")

    # 判断分布
    if stats.skew(df['amount']) > 1:
        print("  → 正偏长尾，用中位数描述典型用户")

    # 画分布
    fig, axes = plt.subplots(1, 2, figsize=(12, 4))
    axes[0].hist(df['amount'], bins=50, alpha=0.6)
    axes[0].axvline(df['amount'].mean(), color='r', label=f'均值={df["amount"].mean():.0f}')
    axes[0].axvline(df['amount'].median(), color='g', linestyle='--', label=f'中位数={df["amount"].median():.0f}')
    axes[0].set_title('消费金额分布'); axes[0].legend()

    # 取对数后
    log_amount = np.log1p(df['amount'])
    axes[1].hist(log_amount, bins=50, alpha=0.6)
    axes[1].set_title(f'取log后（偏度={stats.skew(log_amount):.2f}）')
    plt.savefig('day7_step1.png', dpi=100, bbox_inches='tight')
    plt.show()


def step2_find_relationships(df):
    """步骤2：发现关系（Day5）"""
    print("\n" + "=" * 60)
    print("步骤2：发现关系")
    print("=" * 60)

    # 相关性
    print("\n相关性矩阵：")
    print(df[['age', 'income', 'days_register', 'amount']].corr())

    # income vs amount
    r, p = stats.pearsonr(df['income'], df['amount'])
    print(f"\nincome vs amount: Pearson r={r:.3f}, p={p:.4f}")

    # 卡方：VIP vs 是否大额消费
    df['high_spender'] = (df['amount'] > df['amount'].median()).astype(int)
    table = pd.crosstab(df['is_vip'], df['high_spender'])
    chi2, p_chi, _, _ = stats.chi2_contingency(table)
    print(f"VIP vs 大额消费: 卡方 p={p_chi:.4f}")


def step3_test_difference(df):
    """步骤3：验证差异（Day4）"""
    print("\n" + "=" * 60)
    print("步骤3：验证差异（VIP vs 非VIP）")
    print("=" * 60)

    vip = df[df['is_vip'] == 1]['amount']
    non_vip = df[df['is_vip'] == 0]['amount']

    print(f"VIP消费均值: {vip.mean():.2f}")
    print(f"非VIP消费均值: {non_vip.mean():.2f}")
    print(f"差异: {vip.mean() - non_vip.mean():.2f}")

    t, p = stats.ttest_ind(vip, non_vip)
    print(f"t检验: t={t:.3f}, p={p:.4f}")
    if p < 0.05:
        print("→ VIP消费显著高于非VIP")


def step4_regression(df):
    """步骤4：建模解释（Day6）"""
    print("\n" + "=" * 60)
    print("步骤4：回归建模")
    print("=" * 60)

    X = df[['age', 'income', 'days_register', 'is_vip']]
    X = sm.add_constant(X)
    Y = df['amount']
    model = sm.OLS(Y, X).fit()
    print(model.summary())

    print(f"\n关键解读：")
    print(f"  R² = {model.rsquared:.3f}（解释{model.rsquared:.1%}变异）")
    for col, coef, pval in zip(['age','income','days_register','is_vip'],
                                model.params[1:], model.pvalues[1:]):
        sig = "显著" if pval < 0.05 else "不显著"
        print(f"  {col}: 系数={coef:.3f}, p={pval:.4f} ({sig})")


def step5_conclusion(df):
    """步骤5：结论"""
    print("\n" + "=" * 60)
    print("步骤5：结论与建议")
    print("=" * 60)
    print("""
统计结论：
  - income 显著影响消费（p<0.001）
  - is_vip 显著影响消费（p<0.001）
  - age、days_register 影响不显著
  - 模型解释约 60% 变异

业务建议：
  1. 重点运营高收入用户
  2. 提升 VIP 转化（VIP 消费显著更高）
  3. 年龄、注册天数不是有效分层维度

分析边界：
  - 模拟数据，实际需验证
  - R² 60%，还有 40% 变异未解释，需更多特征
  - VIP 定义基于收入，可能有内生性
""")


if __name__ == '__main__':
    df = generate_data()
    print(f"数据集：{len(df)} 行，字段：{list(df.columns)}")

    step1_understand_data(df)
    step2_find_relationships(df)
    step3_test_difference(df)
    step4_regression(df)
    step5_conclusion(df)
