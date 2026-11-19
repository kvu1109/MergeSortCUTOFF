import os
import numpy as np
import pandas as pd

RAW_CSV_PATH = os.path.join("results", "raw_benchmark.csv")
RESULTS_DIR = "results"


def df_to_markdown_table(df: pd.DataFrame) -> str:
    """Chuyển đổi DataFrame thành bảng Markdown chuẩn mà không cần cài thêm thư viện tabulate."""
    headers = [str(col) for col in df.columns]
    lines = [
        "| " + " | ".join(headers) + " |",
        "| " + " | ".join(["---"] * len(headers)) + " |",
    ]
    for _, row in df.iterrows():
        cells = []
        for val in row:
            if isinstance(val, float):
                cells.append(f"{val:.4f}" if abs(val) < 1 else f"{val:.3f}")
            else:
                cells.append(str(val))
        lines.append("| " + " | ".join(cells) + " |")
    return "\n".join(lines)


def main():
    if not os.path.exists(RAW_CSV_PATH):
        raise FileNotFoundError(
            f"Không tìm thấy file '{RAW_CSV_PATH}'. Hãy chạy BenchmarkRunner trước."
        )

    os.makedirs(RESULTS_DIR, exist_ok=True)
    df = pd.read_csv(RAW_CSV_PATH)

    # =========================================================================
    # BẢNG 1: THỐNG KÊ MÔ TẢ CHI TIẾT THEO (DISTRIBUTION, SIZE, CUTOFF)
    # =========================================================================
    grouped = df.groupby(["distribution", "size", "cutoff"], as_index=False)["time_ms"]
    summary = grouped.agg(
        runs="count",
        mean_ms="mean",
        std_ms="std",
        min_ms="min",
        q25_ms=lambda s: np.percentile(s, 25),
        median_ms="median",
        q75_ms=lambda s: np.percentile(s, 75),
        max_ms="max",
    )
    summary["iqr_ms"] = summary["q75_ms"] - summary["q25_ms"]

    # Tính % cải thiện tốc độ (Speedup) so với mốc cơ sở CUTOFF = 0 (Mergesort thuần túy)
    baseline = summary[summary["cutoff"] == 0][
        ["distribution", "size", "median_ms", "mean_ms"]
    ].rename(
        columns={
            "median_ms": "base_median_ms",
            "mean_ms": "base_mean_ms",
        }
    )

    summary = summary.merge(baseline, on=["distribution", "size"], how="left")
    summary["speedup_median_pct"] = (
        (summary["base_median_ms"] - summary["median_ms"])
        / summary["base_median_ms"]
        * 100.0
    )
    summary["speedup_mean_pct"] = (
        (summary["base_mean_ms"] - summary["mean_ms"])
        / summary["base_mean_ms"]
        * 100.0
    )

    summary_out = summary.drop(columns=["base_median_ms", "base_mean_ms"])
    summary_csv = os.path.join(RESULTS_DIR, "summary_statistics.csv")
    summary_out.to_csv(summary_csv, index=False, float_format="%.6f")

    # =========================================================================
    # BẢNG 2: TỔNG HỢP CUTOFF TỐI ƯU & ĐỐI CHIẾU VỚI MỐC GIÁO TRÌNH (0, 7, 10)
    # =========================================================================
    optimal_rows = []
    for (dist, size), group in summary_out.groupby(["distribution", "size"]):
        best_row = group.loc[group["median_ms"].idxmin()]

        t_cut0 = group.loc[group["cutoff"] == 0, "median_ms"].values[0]
        t_cut7 = (
            group.loc[group["cutoff"] == 7, "median_ms"].values[0]
            if 7 in group["cutoff"].values
            else np.nan
        )
        t_cut10 = (
            group.loc[group["cutoff"] == 10, "median_ms"].values[0]
            if 10 in group["cutoff"].values
            else np.nan
        )

        optimal_rows.append(
            {
                "distribution": dist,
                "size": size,
                "best_cutoff": int(best_row["cutoff"]),
                "best_median_ms": best_row["median_ms"],
                "cutoff_0_ms": t_cut0,
                "cutoff_7_ms": t_cut7,
                "cutoff_10_ms": t_cut10,
                "gain_vs_0_pct": best_row["speedup_median_pct"],
            }
        )

    optimal_df = pd.DataFrame(optimal_rows)
    optimal_csv = os.path.join(RESULTS_DIR, "optimal_cutoff_summary.csv")
    optimal_df.to_csv(optimal_csv, index=False, float_format="%.6f")

    # =========================================================================
    # BẢNG 3: KIỂM CHỨNG GIẢ THUYẾT NHÂN ĐÔI (DOUBLING HYPOTHESIS - BÀI GIẢNG 1.4)
    # So sánh bậc tăng trưởng của CUTOFF = 0 và CUTOFF = 10 trên dữ liệu random
    # =========================================================================
    doubling_rows = []
    for cut in [0, 10]:
        sub = summary_out[
            (summary_out["distribution"] == "random") & (summary_out["cutoff"] == cut)
        ].sort_values("size")

        prev_time = None
        for _, r in sub.iterrows():
            n = int(r["size"])
            t_sec = r["median_ms"] / 1000.0
            ratio = (t_sec / prev_time) if prev_time is not None else np.nan
            lg_ratio = np.log2(ratio) if (prev_time is not None and ratio > 0) else np.nan
            # Ước lượng hằng số a trong mô hình T(N) ~ a * N * lg(N)
            const_a = t_sec / (n * np.log2(n))

            doubling_rows.append(
                {
                    "cutoff": cut,
                    "size_N": n,
                    "median_time_s": t_sec,
                    "ratio_T2N_TN": ratio,
                    "lg_ratio_b": lg_ratio,
                    "const_a_NlgN": f"{const_a:.3e}",
                }
            )
            prev_time = t_sec

    doubling_df = pd.DataFrame(doubling_rows)
    doubling_csv = os.path.join(RESULTS_DIR, "doubling_hypothesis.csv")
    doubling_df.to_csv(doubling_csv, index=False, float_format="%.6f")

    # =========================================================================
    # XUẤT BÁO CÁO BẢNG MARKDOWN ĐỂ DÁN TRỰC TIẾP VÀO REPORT
    # =========================================================================
    md_path = os.path.join(RESULTS_DIR, "statistical_tables.md")
    with open(md_path, "w", encoding="utf-8") as f:
        f.write("# Kết quả Thống kê Thực nghiệm Mergesort Cutoff\n\n")

        f.write("## 1. Bảng tổng hợp giá trị CUTOFF tối ưu theo kích thước và phân phối\n\n")
        f.write(df_to_markdown_table(optimal_df) + "\n\n")

        f.write("## 2. Bảng kiểm chứng Doubling Hypothesis (Dữ liệu Random)\n\n")
        f.write(df_to_markdown_table(doubling_df) + "\n\n")

        f.write("## 3. Bảng thống kê chi tiết tại kích thước lớn nhất (N = 800,000)\n\n")
        max_n = summary_out["size"].max()
        detail_max_n = summary_out[summary_out["size"] == max_n][
            [
                "distribution",
                "cutoff",
                "mean_ms",
                "std_ms",
                "median_ms",
                "iqr_ms",
                "speedup_median_pct",
            ]
        ]
        f.write(df_to_markdown_table(detail_max_n) + "\n")

    print("=== DA XUAT CAC BANG THONG KE THANH CONG ===")
    print(f"1. Thong ke chi tiet toan bo : {summary_csv}")
    print(f"2. Tong hop CUTOFF toi uu    : {optimal_csv}")
    print(f"3. Kiem chung Doubling Hyp.  : {doubling_csv}")
    print(f"4. Bang Markdown cho bao cao : {md_path}\n")
    print("--- XEM NHANH BANG CUTOFF TOI UU ---")
    print(optimal_df.to_string(index=False))


if __name__ == "__main__":
    main()