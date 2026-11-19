# Khảo Sát Thực Nghiệm Giá Trị `CUTOFF` Tối Ưu Cho Thuật Toán Mergesort

Dự án nghiên cứu thực nghiệm nhằm xác định ngưỡng kích thước mảng con (`CUTOFF`) tối ưu để chuyển từ đệ quy **Mergesort** sang **Insertion Sort**, đồng thời đánh giá tác động của đặc trưng phân phối dữ liệu và kiến trúc hệ thống (JVM, CPU Cache) lên hiệu năng thực tế của thuật toán.

---

## 1. Mục tiêu dự án

* **Cài đặt chuẩn & Kiểm thử:** Tự cài đặt thuật toán Mergesort lai ghép Insertion Sort theo thiết kế chuẩn (đảm bảo tính ổn định — *stability* và cô lập các cải tiến phụ trợ khác), kiểm chứng tính đúng đắn bằng bộ kiểm thử đơn vị JUnit 5.
* **Thực nghiệm tái lập (Reproducible Benchmark):** Xây dựng bộ dữ liệu đa dạng về kích thước ($N \in [50{,}000;\; 800{,}000]$) và phân phối (`random`, `sorted`, `reverse`, `partially_sorted`, `duplicates`) với hạt giống ngẫu nhiên cố định ($\text{SEED} = 42$).
* **Phân tích định lượng & Đối chiếu lý thuyết:** Khảo sát $17$ ngưỡng $\text{CUTOFF} \in [0, 64]$, đối chiếu kết quả thực nghiệm với các mốc kinh điển ($\text{CUTOFF} = 7$ của Java 6 và $\text{CUTOFF} = 10$ trong giáo trình *Algorithms* của R. Sedgewick & K. Wayne), đồng thời kiểm chứng mô hình hàm lũy thừa $T(N) \approx a \cdot N^b$ qua giả thuyết nhân đôi (*Doubling Hypothesis*).

---

## 2. Cấu trúc thư mục dự án

```text
MergeSortCUTOFF/
├── src/
│   ├── main/java/
│   │   ├── Insertion.java          # Cài đặt Insertion Sort (toàn mảng và đoạn [lo, hi])
│   │   ├── Merge.java              # Cài đặt Mergesort hỗ trợ tham số cutoff tùy chỉnh
│   │   ├── DataGenerator.java      # Script sinh 25 bộ dữ liệu thí nghiệm (SEED = 42)
│   │   └── BenchmarkRunner.java    # Chương trình đo hiệu năng tự động & ghi thông số hệ thống
│   └── test/java/
│       └── SortTest.java           # Bộ kiểm thử đơn vị JUnit 5 (biên, ranh giới cutoff, stability)
├── data/                           # Chứa 25 tệp dữ liệu đầu vào (.txt)
├── scripts/
│   ├── analyze_results.py          # Script Python tính thống kê mô tả & xuất bảng Markdown
│   └── plot_charts.py              # Script Python vẽ biểu đồ phân tích & hồi quy Log-Log
├── results/
│   ├── system_info.txt             # Thông số phần cứng, hệ điều hành và phiên bản JVM
│   ├── raw_benchmark.csv           # Dữ liệu thô của toàn bộ 6,375 lượt đo
│   ├── summary_statistics.csv      # Bảng tổng hợp Mean, SD, Median, IQR, Speedup (%)
│   ├── optimal_cutoff_summary.csv  # Bảng tổng hợp CUTOFF tối ưu theo từng kích thước & phân phối
│   ├── doubling_hypothesis.csv     # Bảng kiểm chứng Doubling Hypothesis trên dữ liệu random
│   ├── statistical_tables.md       # Các bảng thống kê định dạng Markdown
│   └── charts/                     # Các biểu đồ trực quan hóa (300 DPI)
│       ├── fig1_cutoff_optimization.png
│       ├── fig2_distributions_analysis.png
│       └── fig3_empirical_growth_plots.png
├── report/
│   └── REPORT.md                   # Báo cáo phân tích thực nghiệm hoàn chỉnh
├── ai-logs/
│   └── chat_history.md             # Nhật ký toàn bộ quá trình trao đổi hỗ trợ cùng AI
├── pom.xml                         # Cấu hình dự án Maven & JUnit 5
├── .gitignore                      # Quy tắc loại trừ tệp tạm/biên dịch trên Git
└── README.md                       # Tài liệu hướng dẫn tổng quan dự án
```

---

## 3. Yêu cầu môi trường

* **Java Development Kit (JDK):** Phiên bản 17 trở lên (thí nghiệm gốc thực hiện trên **Java 25.0.2**).
* **Apache Maven:** Phiên bản 3.8+ để biên dịch và chạy kiểm thử JUnit 5.
* **Python:** Phiên bản 3.9+ cùng các thư viện xử lý dữ liệu và vẽ biểu đồ:
  ```bash
  pip install pandas numpy matplotlib
  ```

---

## 4. Hướng dẫn chạy và tái lập thí nghiệm

Mở terminal tại thư mục gốc của dự án (nơi chứa tệp `pom.xml`) và thực hiện lần lượt các bước sau:

### Bước 1: Chạy kiểm thử đơn vị (Unit Tests)
Xác minh tính chính xác và tính ổn định (stability) của hai lớp `Insertion` và `Merge` trên mọi kích thước mảng và ngưỡng `CUTOFF`:
```bash
mvn test
```

### Bước 2: Sinh bộ dữ liệu thí nghiệm
Tạo $25$ tệp dữ liệu trong thư mục `data/` với hạt giống cố định $\text{SEED} = 42$ (bỏ qua bước này nếu sử dụng sẵn dữ liệu đã lưu trong repo):
```bash
mvn compile exec:java -Dexec.mainClass="DataGenerator"
```

### Bước 3: Chạy đo đạc hiệu năng (Benchmark)
Thực thi $6{,}375$ lượt đo chính thức (sau bước làm nóng JVM) và xuất kết quả thô ra `results/raw_benchmark.csv`:
```bash
mvn compile exec:java -Dexec.mainClass="BenchmarkRunner"
```
*(Lưu ý: Nên đóng các ứng dụng chạy nền nặng và cắm nguồn điện ổn định trong quá trình chạy để hạn chế nhiễu xung nhịp CPU).*

### Bước 4: Thống kê số liệu và vẽ biểu đồ
Xử lý dữ liệu thô từ `results/raw_benchmark.csv`, xuất các bảng thống kê và sinh 3 bộ biểu đồ độ phân giải cao trong `results/charts/`:
```bash
python scripts/analyze_results.py
python scripts/plot_charts.py
```

---

## 5. Tóm tắt kết quả thực nghiệm chính

Trên cấu hình thử nghiệm (**Windows 11, CPU 8 logical cores, Java 25.0.2 HotSpot 64-Bit Server VM**):

1. **Giá trị `CUTOFF` tối ưu cho dữ liệu ngẫu nhiên (`random`):**
   * Điểm cực tiểu đạt tại **$\text{CUTOFF} = 12$**, đạt thời gian trung vị $328.182\text{ ms}$ tại $N = 800{,}000$ (nhanh hơn **$+6.45\%$** so với Mergesort thuần túy $\text{CUTOFF} = 0$, và đạt mức cải thiện tới **$+29.74\%$** tại $N = 200{,}000$), đồng thời vượt qua cả hai mốc lịch sử $\text{CUTOFF} = 7$ và $\text{CUTOFF} = 10$.
2. **Ảnh hưởng của phân phối dữ liệu đầu vào:**
   * Với mảng đã sắp xếp (`sorted`) hoặc gần sắp xếp (`partially_sorted`), Insertion Sort đạt thời gian tuyến tính $O(N)$ nên hiệu năng tăng liên tục tới biên khảo sát $\text{CUTOFF} = 64$ (cải thiện **$+33.08\%$** và **$+29.81\%$** tại $N = 800{,}000$).
   * Với mảng đảo ngược (`reverse`), chi phí bậc hai $\sim \frac{1}{2}N^2$ khiến hiệu năng suy giảm mạnh nếu đặt ngưỡng lớn ($-70.12\%$ tại $\text{CUTOFF} = 64$). Do đó, vùng **$\text{CUTOFF} \in [8, 12]$** là khoảng cấu hình cân bằng và an toàn nhất cho dữ liệu tổng quát.
3. **Bất biến bậc tăng trưởng tiệm cận:**
   * Phân tích hồi quy tuyến tính trên đồ thị Log-Log xác nhận mọi cấu hình đều giữ nguyên bậc tăng trưởng $\Theta(N \lg N)$ với hệ số góc $b \in [1.033, 1.128]$, trong khi việc áp dụng `CUTOFF` giúp giảm hệ số hằng số $a$ từ $2.330 \times 10^{-7}$ xuống vùng $6.26 \times 10^{-8} - 8.17 \times 10^{-8}$.

Chi tiết toàn bộ bảng biểu, đồ thị và biện luận khoa học được trình bày tại [`report/REPORT.md`](report/REPORT.md).

---

## 6. Ghi chú về việc sử dụng AI hỗ trợ

Trong quá trình thực hiện dự án, trợ lý AI (**Gemini**) được sử dụng như một công cụ hỗ trợ kỹ thuật và kiểm soát quy trình khoa học, bao gồm:
* Tư vấn cấu trúc dự án theo chuẩn phương pháp thực nghiệm và lập kế hoạch kiểm soát biến số.
* Tạo bộ khung khai báo lớp thuật toán (người thực hiện tự hoàn thiện phần cài đặt chi tiết) và bộ kiểm thử đơn vị JUnit 5 (`SortTest.java`).
* Hỗ trợ viết các script tự động hóa sinh dữ liệu (`DataGenerator.java`), đo thời gian có kiểm soát JVM warm-up (`BenchmarkRunner.java`), phân tích thống kê (`analyze_results.py`) và trực quan hóa đồ thị (`plot_charts.py`).
* Hỗ trợ tổng hợp số liệu và định dạng báo cáo thí nghiệm (`REPORT.md`).

Toàn bộ lịch sử trao đổi chi tiết được lưu trữ đầy đủ tại [`ai-logs/chat_history.md`](ai-logs/chat_history.md).
