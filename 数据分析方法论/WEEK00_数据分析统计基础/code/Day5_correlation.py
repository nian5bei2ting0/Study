# -*- coding: utf-8 -*-
"""
Day5 · 相关性与卡方检验
目标：量化变量间关系，判断分类变量是否独立
"""
import numpy as np
import pandas as pd
from scipy import stats
import matplotlib.pyplot as plt
import seaborn as sns

plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei']
plt.rcParams['axes.unicode_minus'] = False


def pearson_vs_spearman():
    """Pearson vs Spearman：线性 vs 单调"""
    np.random.seed(42)

    # 线性关系
    x = np.random.normal(0, 1, 200)
    y_linear = 2 * x + np.random.normal(0, 0.5, 200)

    # 单调但非线性
    y_mono = x ** 3 + np.random.normal(0, 1, 200)

    # 有异常值
    x_outlier = np.concatenate([x, [10]])
    y_outlier = np.concatenate([y_linear, [100]])

    print("=" * 50)
    print("Pearson vs Spearman 对比")
    print("=" * 50)

    print(f"线性关系: Pearson={stats.pearsonr(x, y_linear)[0]:.3f}, "
          f"Spearman={stats.spearmanr(x, y_linear)[0]:.3f}")
    print(f"非线性单调: Pearson={stats.pearsonr(x, y_mono)[0]:.3f}, "
          f"Spearman={stats.spearmanr(x, y_mono)[0]:.3f}（Spearman更高）")
    print(f"有异常值: Pearson={stats.pearsonr(x_outlier, y_outlier)[0]:.3f}, "
          f"Spearman={stats.spearmanr(x_outlier, y_outlier)[0]:.3f}")

    fig, axes = plt.subplots(1, 3, figsize=(15, 4))
    axes[0].scatter(x, y_linear); axes[0].set_title('线性关系')
    axes[1].scatter(x, y_mono); axes[1].set_title('非线性单调')
    axes[2].scatter(x_outlier, y_outlier); axes[2].set_title('有异常值')
    plt.savefig('day5_pearson_vs_spearman.png', dpi=100, bbox_inches='tight')
    plt.show()


def correlation_matrix():
    """相关性矩阵 + 热力图"""
    np.random.seed(42)
    df = pd.DataFrame({
        'age': np.random.normal(35, 10, 500),
        'income': np.random.normal(50000, 20000, 500),
        'spending': np.random.normal(3000, 1000, 500),
    })
    # 让 spending 与 income 相关
    df['spending'] = df['income'] * 0.05 + np.random.normal(0, 200, 500)
    df['days_active'] = np.random.randint(1, 100, 500)

    print("=" * 50)
    print("相关性矩阵")
    print("=" * 50)
    print(df.corr())

    # 热力图
    plt.figure(figsize=(8, 6))
    sns.heatmap(df.corr(), annot=True, cmap='coolwarm', center=0, fmt='.2f')
    plt.title('相关性矩阵热力图')
    plt.savefig('day5_corr_matrix.png', dpi=100, bbox_inches='tight')
    plt.show()

    # 解读
    print("\n解读：")
    print(f"  income vs spending: {df.corr().loc['income','spending']:.2f}（强正相关）")
    print(f"  age vs days_active: {df.corr().loc['age','days_active']:.2f}（弱相关）")


def correlation_not_causation():
    """相关 ≠ 因果：混淆变量示例"""
    np.random.seed(42)
    n = 500
    # 夏天（C）同时影响冰淇淋销量和溺水率
    summer = np.random.randint(0, 2, n)
    ice_cream = summer * 50 + np.random.normal(20, 10, n)
    drowning = summer * 10 + np.random.normal(3, 2, n)

    r, p = stats.pearsonr(ice_cream, drowning)
    print("=" * 50)
    print("相关 ≠ 因果")
    print("=" * 50)
    print(f"冰淇淋销量 vs 溺水率: r={r:.3f}, p={p:.4f}")
    print("→ 显著正相关，但不是因果！")
    print("→ 混淆变量：夏天同时影响两者")

    # 控制夏天后看相关
    for s in [0, 1]:
        sub_ice = ice_cream[summer == s]
        sub_drown = drowning[summer == s]
        r_sub, _ = stats.pearsonr(sub_ice, sub_drown)
        print(f"  夏天={s}组内相关: r={r_sub:.3f}（控制混淆变量后相关消失）")


def chi_square_test():
    """卡方检验：两个分类变量是否独立"""
    # 渠道 vs 是否流失
    data = np.array([
        # 流失  未流失
        [80,  920],   # 渠道A
        [120, 880],   # 渠道B
        [60,  940],   # 渠道C
    ])

    print("=" * 50)
    print("卡方检验：渠道 vs 流失")
    print("=" * 50)
    chi2, p, dof, expected = stats.chi2_contingency(data)
    print(f"卡方统计量: {chi2:.3f}")
    print(f"p值: {p:.4f}")
    print(f"自由度: {dof}")

    if p < 0.05:
        print("→ 渠道与流失不独立（有关联）")
    else:
        print("→ 渠道与流失独立（无关联）")

    # 各渠道流失率
    for i, name in enumerate(['A', 'B', 'C']):
        rate = data[i, 0] / data[i].sum()
        print(f"  渠道{name}流失率: {rate:.2%}")

    # 期望值
    print(f"\n期望值（如果独立）:\n{expected}")


def chi_square_small_sample():
    """小样本用 Fisher 精确检验"""
    # 期望值 < 5 的情况
    data = np.array([
        [3, 10],
        [1, 15]
    ])
    print("\n" + "=" * 50)
    print("小样本：Fisher 精确检验")
    print("=" * 50)
    odds_ratio, p = stats.fisher_exact(data)
    print(f"Fisher p={p:.4f}")
    print("→ 期望值<5时用Fisher，不用卡方")


if __name__ == '__main__':
    pearson_vs_spearman()
    print()
    correlation_matrix()
    print()
    correlation_not_causation()
    print()
    chi_square_test()
    print()
    chi_square_small_sample()
