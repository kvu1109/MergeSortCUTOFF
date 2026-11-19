# Nhật Ký Trao Đổi Với Trợ Lý AI (AI Interaction Logs)

* **Dự án:** Khảo sát thực nghiệm tìm giá trị `CUTOFF` tối ưu cho thuật toán Mergesort
* **Công cụ AI sử dụng:** Gemini
* **Mục đích sử dụng:** Tư vấn quy trình thiết kế thực nghiệm khoa học, tạo bộ khung mã nguồn và kiểm thử đơn vị (JUnit 5), xây dựng script tự động hóa sinh dữ liệu, đo lường hiệu năng, xử lý thống kê và trực quan hóa dữ liệu.

---

## Lượt 1: Lập kế hoạch quy trình thực hiện dự án từ A đến Z

### Người dùng (User)
Yêu cầu hướng dẫn quy trình thực hiện dự án thực nghiệm tìm giá trị `CUTOFF` tối ưu cho Mergesort từ A đến Z, đối chiếu với nội dung bài giảng *Analysis of Algorithms*, *Elementary Sorts*, và *Mergesort*.

### Trợ lý AI (Gemini)
Đề xuất quy trình triển khai gồm 7 giai đoạn theo chuẩn phương pháp khoa học:
1. **Khởi tạo Git Repository & Cấu trúc thư mục:** Tổ chức các thư mục `src/`, `data/`, `scripts/`, `results/`, `report/`, `ai-logs/` và thực hiện commit theo từng mốc tiến độ.
2. **Nền tảng lý thuyết & Đặt giả thuyết:** Áp dụng 5 bước của phương pháp khoa học (Observe $\rightarrow$ Hypothesize $\rightarrow$ Predict $\rightarrow$ Verify $\rightarrow$ Validate), phân tích yếu tố độc lập và phụ thuộc hệ thống lý giải sự khác biệt giữa mốc $\text{CUTOFF} = 7$ (Java 6) và $\text{CUTOFF} = 10$ (giáo trình mới).
3. **Cài đặt thuật toán & Kiểm thử:** Nạp chồng phương thức `Insertion.sort(a, lo, hi)`, thêm tham số `cutoff` vào `Merge.sort`, cô lập biến khảo sát và kiểm thử tính đúng đắn cùng tính ổn định (Stability).
4. **Thiết kế bộ dữ liệu thí nghiệm:** Sử dụng quy tắc nhân đôi kích thước (*Doubling hypothesis*), cố định `seed` ngẫu nhiên để đảm bảo tính tái lập, bao phủ 5 phân phối: Ngẫu nhiên đồng đều (Knuth Shuffle), Đã sắp xếp, Sắp xếp ngược, Gần như đã sắp xếp, và Nhiều trùng lặp.
5. **Chạy thí nghiệm đo lường:** Tách biệt thời gian đọc tệp/sao chép mảng khỏi vùng bấm giờ, làm nóng JVM (warm-up), lặp lại nhiều lần và lưu dữ liệu thô ra CSV.
6. **Thống kê & Trực quan hóa:** Tính Mean, Median, IQR, Speedup ($\%$) và vẽ 3 nhóm đồ thị: đường cong chữ U tìm `CUTOFF`, biểu đồ so sánh phân phối dữ liệu, và cặp đồ thị Standard plot / Log-log plot.
7. **Viết báo cáo & Tổng hợp AI Logs:** Hoàn thiện báo cáo phân tích và lưu trữ nhật ký trao đổi lên Git.

---

## Lượt 2: Thiết kế khung lớp thuật toán và bộ kiểm thử đơn vị JUnit 5

### Người dùng (User)
> "thiết kế unit test bằng Junit cho tôi, cùng với đó là bộ khung hàm cho các lớp mergesort và insertionsort chưa có nội dung các phương thức"

### Trợ lý AI (Gemini)
Cung cấp bộ khung mã nguồn Java (chỉ gồm khai báo lớp, chữ ký phương thức và chú thích hướng dẫn `TODO`, để trống phần thân để người dùng tự cài đặt) cùng bộ kiểm thử JUnit 5:
* **`Insertion.java`:** Khai báo `sort(Comparable[] a)`, `sort(Comparable[] a, int lo, int hi)`, và các hàm hỗ trợ `less()`, `exch()`.
* **`Merge.java`:** Khai báo `merge()`, hàm đệ quy `sort(a, aux, lo, hi, cutoff)`, và các hàm công khai `sort(a, cutoff)`, `sort(a)`.
* **`SortTest.java` (JUnit 5):** Thiết kế 7 nhóm test case kiểm tra sắp xếp cục bộ đoạn `[lo, hi]`, trường hợp biên ($N = 0, 1, 2$), ranh giới kích thước quanh ngưỡng cắt ($N = \text{cutoff} - 1, \text{cutoff}, \text{cutoff} + 1$), các phân phối dữ liệu đặc thù, đối chiếu với `Arrays.sort()` trên mảng lớn ($N = 10{,}000$), và kiểm tra tính ổn định (`testStability`) thông qua lớp `Item` lưu chỉ số gốc.

---

## Lượt 3: Cấu hình tệp `pom.xml` cho dự án Maven

### Người dùng (User)
> "tôi dùng maven, cho tôi script của file pom.xml"

### Trợ lý AI (Gemini)
Cung cấp cấu hình tệp `pom.xml` chuẩn Maven tích hợp thư viện `org.junit.jupiter:junit-jupiter` (JUnit 5) và plugin `maven-surefire-plugin` phiên bản `3.5.2` để thực thi kiểm thử qua lệnh `mvn test`.

---

## Lượt 4: Cấu hình tệp `.gitignore`

### Người dùng (User)
> "cho xin script .gitignore"

### Trợ lý AI (Gemini)
Cung cấp tệp `.gitignore` loại bỏ các tệp biên dịch của Maven/Java (`target/`, `*.class`), môi trường ảo Python (`__pycache__/`, `.venv/`), tệp cấu hình IDE (`.idea/`, `.vscode/`) và tệp hệ điều hành (`.DS_Store`, `Thumbs.db`), đồng thời giữ lại các định dạng dữ liệu và kết quả thí nghiệm (`.csv`, `.txt`, `.png`, `.pdf`) để phục vụ việc nộp bài trên Git.

---

## Lượt 5: Xác định bước tiếp theo sau khi hoàn thiện thuật toán

### Người dùng (User)
> "tôi đã xây dựng xong mã nguồn lớp merge và insertion, bước tiếp theo là gì"

### Trợ lý AI (Gemini)
Hướng dẫn 3 bước tiếp theo:
1. Chạy `mvn test` xác nhận toàn bộ unit test đạt trạng thái xanh (Pass) và commit mã nguồn lên Git.
2. Xây dựng bộ sinh dữ liệu thí nghiệm tĩnh (`data/`) với hạt giống `seed` cố định và 5 phân phối dữ liệu.
3. Xây dựng lớp chạy benchmark tự động đo thời gian và xuất dữ liệu thô ra định dạng CSV.

---

## Lượt 6: Viết script sinh bộ dữ liệu thí nghiệm bằng Java (`DataGenerator.java`)

### Người dùng (User)
> "mvn test của tôi đã xanh hết, tôi cần bạn viết script sinh bộ dữ liệu test bằng Java"

### Trợ lý AI (Gemini)
Cung cấp lớp `DataGenerator.java` sử dụng `SEED = 42L` để sinh tự động $25$ tệp dữ liệu trong thư mục `data/`, tương ứng với $5$ kích thước tăng gấp đôi ($N \in \{50{,}000; 100{,}000; 200{,}000; 400{,}000; 800{,}000\}$) và $5$ phân phối:
* `random`: Sử dụng thuật toán **Knuth Shuffle** (tráo đổi ngẫu nhiên với $r \in [0, i]$).
* `sorted`: Mảng tăng dần $0 \dots N-1$.
* `reverse`: Mảng giảm dần $N-1 \dots 0$.
* `partially_sorted`: Hoán đổi cục bộ $2\%$ phần tử trong bán kính $[-5, +5]$.
* `duplicates`: Giá trị ngẫu nhiên trong tập $\{0, 1, \dots, 9\}$.

---

## Lượt 7: Viết lớp chạy đo hiệu năng tự động (`BenchmarkRunner.java`)

### Người dùng (User)
> "tiếp tục viết lớp BenchmarkRunner.java đi"

### Trợ lý AI (Gemini)
Cung cấp lớp `BenchmarkRunner.java` thực hiện quy trình đo đạc tự động:
* Ghi nhận thông số phần cứng, hệ điều hành và phiên bản JVM vào `results/system_info.txt`.
* Thực hiện làm nóng JVM toàn cục và $5$ lượt làm nóng cục bộ trước mỗi cấu hình để trình biên dịch JIT ổn định hiệu năng.
* Khảo sát $17$ giá trị $\text{CUTOFF} \in \{0, 2, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 20, 24, 32, 48, 64\}$, lặp lại $15$ lần đo cho mỗi cấu hình (sao chép mảng gốc bên ngoài khối `System.nanoTime()`), kiểm tra `isSorted()` sau mỗi lần chạy và xuất toàn bộ $6{,}375$ bản ghi thô ra tệp `results/raw_benchmark.csv` với định dạng chuẩn `Locale.US`.

---

## Lượt 8: Viết script Python phân tích và lập bảng thống kê (`analyze_results.py`)

### Người dùng (User)
> "dùng python lập bảng thống kê đi"

### Trợ lý AI (Gemini)
Cung cấp tệp `scripts/analyze_results.py` sử dụng `pandas` và `numpy` để xử lý tệp `results/raw_benchmark.csv` và xuất tự động:
* `results/summary_statistics.csv`: Thống kê đầy đủ Mean, SD, Min, Q25, Median, Q75, Max, IQR và tỉ lệ cải thiện tốc độ ($\%$) so với mốc $\text{CUTOFF} = 0$.
* `results/optimal_cutoff_summary.csv`: Tổng hợp giá trị `CUTOFF` tối ưu trên từng kích thước và phân phối, đối chiếu trực tiếp với các mốc $\text{CUTOFF} \in \{0, 7, 10\}$.
* `results/doubling_hypothesis.csv`: Bảng kiểm chứng giả thuyết nhân đôi kích thước trên dữ liệu `random`.
* `results/statistical_tables.md`: Xuất các bảng định dạng Markdown sẵn sàng đưa vào báo cáo.

---

## Lượt 9: Viết script Python trực quan hóa biểu đồ (`plot_charts.py`)

### Người dùng (User)
> "viết script vẽ biểu đồ cho tôi"

### Trợ lý AI (Gemini)
Cung cấp tệp `scripts/plot_charts.py` sử dụng `matplotlib` để xuất 3 bộ biểu đồ độ phân giải cao (300 DPI) vào `results/charts/`:
1. `fig1_cutoff_optimization.png`: Đường cong chữ U tìm `CUTOFF` tối ưu kèm dải tứ phân vị (IQR) tại $N = 800{,}000$ và đường thời gian chuẩn hóa $T(N) / (N \lg N)$ qua các kích thước $N$.
2. `fig2_distributions_analysis.png`: Đường cong phần trăm cải thiện tốc độ và biểu đồ cột đối chiếu hiệu năng trên $5$ phân phối dữ liệu.
3. `fig3_empirical_growth_plots.png`: Cặp đồ thị Standard plot ($T(N)$ theo $N$) và Log-log plot ($\lg T(N)$ theo $\lg N$) kèm tính toán phương trình hồi quy tuyến tính Power Law $T(N) = a \cdot N^b$.

---

## Lượt 10: Viết báo cáo thực nghiệm hoàn chỉnh và xuất nhật ký AI

### Người dùng (User)
Cung cấp kết quả thống kê thực nghiệm (`statistical_tables.md`, `system_info.txt`) cùng các phương trình hồi quy Power Law thu được từ `plot_charts.py`, yêu cầu viết hoàn chỉnh tệp `REPORT.md` và xuất toàn bộ lịch sử trao đổi thành tệp Markdown.

### Trợ lý AI (Gemini)
Tổng hợp số liệu thực tế trên cấu hình thử nghiệm (Windows 11, CPU 8 nhân, Java 25.0.2), biện luận kết quả tối ưu $\text{CUTOFF} = 12$ ($+6.448\%$ tại $N = 800{,}000$, $+29.738\%$ tại $N = 200{,}000$), phân tích sự phân hóa trên các phân phối dữ liệu đặc thù (`sorted` đạt đỉnh ở $\text{CUTOFF} = 64$, `reverse` suy giảm $-70.122\%$ ở $\text{CUTOFF} = 64$), giải thích hiệu ứng bộ nhớ đệm L3 Cache ở $N = 800{,}000$, và xuất hoàn chỉnh hai tệp `report/REPORT.md` cùng `ai-logs/chat_history.md`.