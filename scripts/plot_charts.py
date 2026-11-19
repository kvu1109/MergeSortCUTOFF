import os
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

SUMMARY_CSV_PATH = os.path.join("results", "summary_statistics.csv")
CHARTS_DIR = os.path.join("results", "charts")

# Cấu hình giao diện biểu đồ rõ nét phục vụ in ấn/chèn báo cáo
plt.rcParams.update({
    "figure.dpi": 150,
    "savefig.dpi": 300,
    "font.size": 10,
    "axes.titlesize": 11,
    "axes.titleweight": "bold",
    "axes.labelsize": 10,
    "legend.fontsize": 9,
    "axes.grid": True,
    "grid.alpha": 0.3,
    "grid.linestyle": "--",
})

DIST_LABELS = {
    "random": "Ngẫu nhiên (Random)",
    "sorted": "Đã sắp xếp (Sorted)",
    "reverse": "Sắp xếp ngược (Reverse)",
    "partially_sorted": "Gần như sắp xếp (Partially Sorted)",
    "duplicates": "Nhiều trùng lặp (Duplicates)",
}


def plot_fig1_cutoff_optimization(df: pd.DataFrame):
    """
    Biểu đồ 1: Tìm điểm CUTOFF tối ưu trên dữ liệu Random.
    - Trái: Đường cong chữ U (Median + dải IQR Q25-Q75) tại kích thước lớn nhất.
    - Phải: Thời gian chuẩn hóa T(N) / (N * lg N) qua tất cả các kích thước N.
    """
    rand_df = df[df["distribution"] == "random"].copy()
    max_n = rand_df["size"].max()
    sub_max = rand_df[rand_df["size"] == max_n].sort_values("cutoff")

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 5.5))

    # --- Panel A: Thời gian thực tế tại N lớn nhất + dải tứ phân vị IQR ---
    cutoffs = sub_max["cutoff"].values
    medians = sub_max["median_ms"].values
    q25 = sub_max["q25_ms"].values
    q75 = sub_max["q75_ms"].values

    ax1.plot(cutoffs, medians, marker="o", color="#1f77b4", linewidth=2, label="Trung vị (Median)")
    ax1.fill_between(cutoffs, q25, q75, color="#1f77b4", alpha=0.18, label="Khoảng tứ phân vị (IQR: Q25–Q75)")

    # Đánh dấu các mốc quan trọng: CUTOFF = 0, 7, 10 và Best
    best_row = sub_max.loc[sub_max["median_ms"].idxmin()]
    best_cut = int(best_row["cutoff"])
    best_val = best_row["median_ms"]

    ax1.axvline(best_cut, color="#d62728", linestyle="-.", linewidth=1.5,
                label=f"Tối ưu thực nghiệm: CUTOFF = {best_cut} ({best_val:.2f} ms)")
    if 7 in cutoffs:
        v7 = sub_max.loc[sub_max["cutoff"] == 7, "median_ms"].values[0]
        ax1.scatter([7], [v7], color="#ff7f0e", zorder=5, s=70, label=f"Java 6: CUTOFF = 7 ({v7:.2f} ms)")
    if 10 in cutoffs:
        v10 = sub_max.loc[sub_max["cutoff"] == 10, "median_ms"].values[0]
        ax1.scatter([10], [v10], color="#2ca02c", zorder=5, s=70, label=f"Giáo trình mới: CUTOFF = 10 ({v10:.2f} ms)")

    ax1.set_title(f"A. Ảnh hưởng của CUTOFF tới thời gian chạy (Random, N = {max_n:,})")
    ax1.set_xlabel("Ngưỡng chuyển sang Insertion Sort (CUTOFF)")
    ax1.set_ylabel("Thời gian chạy trung vị (ms)")
    ax1.legend(loc="upper right")

    # --- Panel B: Thời gian chuẩn hóa T(N) / (N lg N) theo từng kích thước N ---
    for size, grp in rand_df.groupby("size"):
        grp = grp.sort_values("cutoff")
        # Quy đổi ra nanosecond trên mỗi đơn vị N * log2(N)
        norm_ns = (grp["median_ms"] * 1e6) / (size * np.log2(size))
        ax2.plot(grp["cutoff"], norm_ns, marker=".", linewidth=1.6, label=f"N = {size:,}")

    ax2.axvline(best_cut, color="#d62728", linestyle="-.", alpha=0.7, linewidth=1.2)
    ax2.set_title("B. Thời gian chuẩn hóa T(N) / (N lg N) qua các kích thước N")
    ax2.set_xlabel("Ngưỡng chuyển sang Insertion Sort (CUTOFF)")
    ax2.set_ylabel("Thời gian chuẩn hóa (ns / N lg N)")
    ax2.legend(title="Kích thước mảng", loc="upper right")

    plt.tight_layout()
    out_path = os.path.join(CHARTS_DIR, "fig1_cutoff_optimization.png")
    plt.savefig(out_path, bbox_inches="tight")
    plt.close()
    print(f"Đã lưu: {out_path}")


def plot_fig2_distributions_analysis(df: pd.DataFrame):
    """
    Biểu đồ 2: So sánh hiệu năng theo 5 phân phối dữ liệu tại N lớn nhất.
    - Trái: Mức cải thiện tốc độ (% Speedup so với CUTOFF = 0) theo CUTOFF trên từng phân phối.
    - Phải: Biểu đồ cột so sánh % cải thiện tại CUTOFF = 7, CUTOFF = 10 và Best CUTOFF.
    """
    max_n = df["size"].max()
    sub = df[df["size"] == max_n].copy()

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 5.5))

    # --- Panel A: Đường cong % Speedup trên 5 loại dữ liệu ---
    for dist, label in DIST_LABELS.items():
        d_grp = sub[sub["distribution"] == dist].sort_values("cutoff")
        if d_grp.empty:
            continue
        ax1.plot(d_grp["cutoff"], d_grp["speedup_median_pct"], marker="o", markersize=4,
                 linewidth=1.8, label=label)

    ax1.axhline(0, color="black", linewidth=1, linestyle="-")
    ax1.set_title(f"A. Mức cải thiện tốc độ (%) so với CUTOFF = 0 (N = {max_n:,})")
    ax1.set_xlabel("Ngưỡng CUTOFF")
    ax1.set_ylabel("Tốc độ cải thiện so với Mergesort gốc (%)")
    ax1.legend(loc="best")

    # --- Panel B: Biểu đồ cột so sánh CUTOFF 7, CUTOFF 10 và Best CUTOFF ---
    dists = [d for d in DIST_LABELS.keys() if d in sub["distribution"].unique()]
    x = np.arange(len(dists))
    width = 0.25

    gain_7, gain_10, gain_best, best_labels = [], [], [], []
    for d in dists:
        d_grp = sub[sub["distribution"] == d]
        g7 = d_grp.loc[d_grp["cutoff"] == 7, "speedup_median_pct"].values[0]
        g10 = d_grp.loc[d_grp["cutoff"] == 10, "speedup_median_pct"].values[0]
        best_r = d_grp.loc[d_grp["median_ms"].idxmin()]
        gb = best_r["speedup_median_pct"]
        bc = int(best_r["cutoff"])

        gain_7.append(g7)
        gain_10.append(g10)
        gain_best.append(gb)
        best_labels.append(f"c={bc}")

    ax2.bar(x - width, gain_7, width, label="CUTOFF = 7 (Java 6)", color="#ff7f0e")
    ax2.bar(x, gain_10, width, label="CUTOFF = 10 (Giáo trình mới)", color="#2ca02c")
    bars_best = ax2.bar(x + width, gain_best, width, label="CUTOFF tối ưu trên máy", color="#1f77b4")

    # Ghi nhãn giá trị cutoff tối ưu trên đỉnh cột thứ 3
    for bar, lbl in zip(bars_best, best_labels):
        h = bar.get_height()
        ax2.annotate(lbl,
                     xy=(bar.get_x() + bar.get_width() / 2, h),
                     xytext=(0, 3 if h >= 0 else -12),
                     textcoords="offset points",
                     ha="center", va="bottom", fontsize=8, fontweight="bold")

    ax2.axhline(0, color="black", linewidth=1)
    ax2.set_title(f"B. Đối chiếu mức cải thiện (%) theo phân phối dữ liệu (N = {max_n:,})")
    ax2.set_xticks(x)
    ax2.set_xticklabels([d.replace("_", "\n") for d in dists])
    ax2.set_ylabel("Mức cải thiện tốc độ (%)")
    ax2.legend(loc="best")

    plt.tight_layout()
    out_path = os.path.join(CHARTS_DIR, "fig2_distributions_analysis.png")
    plt.savefig(out_path, bbox_inches="tight")
    plt.close()
    print(f"Đã lưu: {out_path}")


def plot_fig3_empirical_growth_plots(df: pd.DataFrame):
    """
    Biểu đồ 3: Kiểm chứng bậc tăng trưởng thực nghiệm theo Bài giảng 1.4.
    - Trái: Standard Plot T(N) vs N.
    - Phải: Log-Log Plot lg(T(N)) vs lg(N) kèm hồi quy tuyến tính T(N) = a * N^b.
    """
    rand_df = df[df["distribution"] == "random"].copy()
    max_n = rand_df["size"].max()
    best_cut = int(rand_df[rand_df["size"] == max_n].loc[
        rand_df[rand_df["size"] == max_n]["median_ms"].idxmin(), "cutoff"
    ])

    target_cutoffs = sorted(list({0, 7, 10, best_cut}))
    colors = {0: "#7f7f7f", 7: "#ff7f0e", 10: "#2ca02c", best_cut: "#d62728"}

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 5.5))

    print("\n--- PHƯƠNG TRÌNH HỒI QUY POWER LAW T(N) = a * N^b (LOG-LOG PLOT) ---")
    for cut in target_cutoffs:
        sub = rand_df[rand_df["cutoff"] == cut].sort_values("size")
        sizes = sub["size"].values
        times_sec = sub["median_ms"].values / 1000.0

        lbl = f"CUTOFF = {cut}"
        if cut == 0:
            lbl += " (Gốc)"
        elif cut == best_cut and cut not in (7, 10):
            lbl += " (Tối ưu)"

        # Panel A: Standard plot
        ax1.plot(sizes, times_sec, marker="o", linewidth=2, color=colors.get(cut, "#1f77b4"), label=lbl)

        # Panel B: Log-log plot (cơ số 2 theo ký hiệu lg của giáo trình Sedgewick)
        lg_n = np.log2(sizes)
        lg_t = np.log2(times_sec)
        slope_b, intercept_c = np.polyfit(lg_n, lg_t, 1)
        const_a = 2 ** intercept_c

        print(f"CUTOFF = {cut:2d} | Hệ số góc b = {slope_b:.4f} | Hằng số a = {const_a:.3e} | T(N) ≈ {const_a:.3e} * N^{slope_b:.3f} (s)")

        ax2.plot(lg_n, lg_t, marker="o", linewidth=2, color=colors.get(cut, "#1f77b4"),
                 label=f"{lbl} (slope b = {slope_b:.3f})")

    ax1.set_title("A. Standard Plot: Thời gian chạy T(N) theo kích thước N")
    ax1.set_xlabel("Kích thước mảng đầu vào (N)")
    ax1.set_ylabel("Thời gian chạy trung vị T(N) (giây)")
    ax1.legend(loc="upper left")

    ax2.set_title("B. Log-Log Plot: lg T(N) theo lg N (Kiểm chứng Power Law)")
    ax2.set_xlabel("lg N  (log₂ N)")
    ax2.set_ylabel("lg T(N)  (log₂ T(N) tính bằng giây)")
    ax2.legend(loc="upper left")

    plt.tight_layout()
    out_path = os.path.join(CHARTS_DIR, "fig3_empirical_growth_plots.png")
    plt.savefig(out_path, bbox_inches="tight")
    plt.close()
    print(f"\nĐã lưu: {out_path}")


def main():
    if not os.path.exists(SUMMARY_CSV_PATH):
        raise FileNotFoundError(
            f"Không tìm thấy '{SUMMARY_CSV_PATH}'. Hãy chạy 'python scripts/analyze_results.py' trước."
        )

    os.makedirs(CHARTS_DIR, exist_ok=True)
    df = pd.read_csv(SUMMARY_CSV_PATH)

    plot_fig1_cutoff_optimization(df)
    plot_fig2_distributions_analysis(df)
    plot_fig3_empirical_growth_plots(df)


if __name__ == "__main__":
    main()