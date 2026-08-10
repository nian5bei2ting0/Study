# -*- coding: utf-8 -*-
"""
Day2 · 描述性统计与分布形态
目标：用统计量描述数据，学会根据分布形态选合适的方法
"""
import numpy as np
import pandas as pd
from scipy import stats
import matplotlib.pyplot as plt

plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei']
plt.rcParams['axes.unicode_minus'] = False


def describe_distribution():
    """完整描述一份数据的标准动作"""
    # 模拟金额数据（长尾）
    np.random.seed(42)
    amounts = np.random.lognormal(3, 1, 10000)

    print("=" * 50)
    print("数据概览：金额（长尾分布）")
    print("=" * 50)

    # 集中趋势
    print(f"均值:   {amounts.mean():.2f}")
    print(f"中位数: {np.median(amounts):.2f}")
    print(f"众数(近似): {stats.mode(np.round(amounts))[0]}")

    # 离散程度
    print(f"标准差: {amounts.std():.2f}")
    q1, q3 = np.percentile(amounts, [25, 75])
    print(f"IQR:    {q3 - q1:.2f}")
    print(f"极差:   {amounts.max() - amounts.min():.2f}")

    # 分位数
    print(f"P5/P25/P50/P75/P95: {np.percentile(amounts, [5,25,50,75,95])}")

    # 形态
    print(f"偏度: {stats.skew(amounts):.2f}（>0 正偏长尾）")
    print(f"峰度: {stats.kurtosis(amounts):.2f}（>0 厚尾）")

    # 关键认知
    print(f"\n均值({amounts.mean():.0f}) >> 中位数({np.median(amounts):.0f})")
    print("→ 长尾数据用中位数描述典型用户更真实")


def compare_stats():
    """对比不同分布下的统计量选择"""
    fig, axes = plt.subplots(1, 2, figsize=(12, 4))

    # 对称数据（正态）
    normal_data = np.random.normal(100, 15, 5000)
    axes[0].hist(normal_data, bins=50, alpha=0.6)
    axes[0].axvline(normal_data.mean(), color='r', label=f'均值={normal_data.mean():.0f}')
    axes[0].axvline(np.median(normal_data), color='g', linestyle='--', label=f'中位数={np.median(normal_data):.0f}')
    axes[0].set_title(f'对称分布（偏度={stats.skew(normal_data):.2f}）\n→ 用均值+标准差')
    axes[0].legend()

    # 长尾数据
    skewed_data = np.random.lognormal(3, 1, 5000)
    axes[1].hist(skewed_data, bins=50, alpha=0.6)
    axes[1].axvline(skewed_data.mean(), color='r', label=f'均值={skewed_data.mean():.0f}')
    axes[1].axvline(np.median(skewed_data), color='g', linestyle='--', label=f'中位数={np.median(skewed_data):.0f}')
    axes[1].set_title(f'长尾分布（偏度={stats.skew(skewed_data):.2f}）\n→ 用中位数+IQR')
    axes[1].legend()

    plt.savefig('day2_compare.png', dpi=100, bbox_inches='tight')
    plt.show()


def boxplot_demo():
    """箱线图原理与异常检测"""
    data = np.concatenate([
        np.random.normal(50, 10, 200),
        [150, 160, 170]  # 注入异常值
    ])

    fig, ax = plt.subplots(figsize=(8, 4))
    bp = ax.boxplot(data, vert=False, showmeans=True)
    ax.set_title('箱线图：箱体=IQR，须=1.5×IQR，点=异常')

    # 标注
    q1, q3 = np.percentile(data, [25, 75])
    iqr = q3 - q1
    lower = q1 - 1.5 * iqr
    upper = q3 + 1.5 * iqr
    print(f"Q1={q1:.1f}, Q3={q3:.1f}, IQR={iqr:.1f}")
    print(f"异常下界={lower:.1f}, 异常上界={upper:.1f}")
    print(f"异常值: {data[(data < lower) | (data > upper)]}")

    plt.savefig('day2_boxplot.png', dpi=100, bbox_inches='tight')
    plt.show()


def transform_demo():
    """数据变换：对数变换让长尾趋近正态"""
    amounts = np.random.lognormal(3, 1, 5000)

    fig, axes = plt.subplots(1, 2, figsize=(12, 4))

    # 原始
    axes[0].hist(amounts, bins=50, alpha=0.6)
    axes[0].set_title(f'原始（偏度={stats.skew(amounts):.2f}）')

    # 对数变换
    log_amounts = np.log(amounts)
    axes[1].hist(log_amounts, bins=50, alpha=0.6)
    axes[1].set_title(f'对数变换后（偏度={stats.skew(log_amounts):.2f}）')

    plt.savefig('day2_transform.png', dpi=100, bbox_inches='tight')
    plt.show()

    print(f"变换前偏度: {stats.skew(amounts):.2f}")
    print(f"变换后偏度: {stats.skew(log_amounts):.2f}（趋近0）")
    print("→ 对数变换压缩右尾，让长尾数据趋近正态")


def choose_statistic():
    """根据分布形态选统计量的决策表"""
    print("=" * 50)
    print("统计量选择决策表")
    print("=" * 50)
    print("| 数据形态   | 集中趋势 | 离散程度 | 异常检测      |")
    print("|-----------|---------|---------|--------------|")
    print("| 对称正态  | 均值    | 标准差  | 3σ / z-score  |")
    print("| 长尾正偏  | 中位数  | IQR     | 1.5×IQR      |")
    print("| 双峰      | 分层描述 | -     | -            |")
    print("| 分类      | 众数    | -      | -            |")


if __name__ == '__main__':
    describe_distribution()
    print()
    compare_stats()
    print()
    boxplot_demo()
    print()
    transform_demo()
    print()
    choose_statistic()
