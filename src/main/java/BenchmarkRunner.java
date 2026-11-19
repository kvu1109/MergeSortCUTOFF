import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

public class BenchmarkRunner {

    private static final String DATA_DIR = "data";
    private static final String RESULTS_DIR = "results";
    private static final String CSV_FILE = "raw_benchmark.csv";
    private static final String SYS_INFO_FILE = "system_info.txt";

    // Khớp với các kích thước và phân phối đã sinh ở DataGenerator
    private static final int[] SIZES = {50_000, 100_000, 200_000, 400_000, 800_000};
    private static final String[] DISTRIBUTIONS = {
            "random",
            "sorted",
            "reverse",
            "partially_sorted",
            "duplicates"
    };

    // Dải giá trị CUTOFF cần khảo sát:
    // 0 là mốc Mergesort thuần túy; 7 là mốc Java 6; 10 là mốc giáo trình mới
    private static final int[] CUTOFFS = {
            0, 2, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 20, 24, 32, 48, 64
    };

    private static final int WARMUP_RUNS = 5;    // Số lần chạy nháp để JIT compiler tối ưu hóa
    private static final int MEASURED_RUNS = 15; // Số lần đo chính thức cho mỗi cấu hình

    public static void main(String[] args) {
        try {
            Path resultsPath = Paths.get(RESULTS_DIR);
            Files.createDirectories(resultsPath);

            // 1. Ghi nhận cấu hình phần cứng/phần mềm phục vụ viết báo cáo
            saveSystemInfo(resultsPath.resolve(SYS_INFO_FILE));

            // 2. Làm nóng JVM toàn cục trước khi bước vào đo đạc chính thức
            System.out.println("Dang lam nong JVM (Global Warm-up)...");
            warmUpJVM();

            Path csvPath = resultsPath.resolve(CSV_FILE);
            try (BufferedWriter writer = Files.newBufferedWriter(csvPath)) {
                // Ghi dòng tiêu đề CSV
                writer.write("distribution,size,cutoff,trial,time_ms");
                writer.newLine();

                System.out.println("=== BAT DAU CHAY THUC NGHIEM DO THOI GIAN ===");

                for (String dist : DISTRIBUTIONS) {
                    for (int size : SIZES) {
                        String fileName = dist + "_" + size + ".txt";
                        Path filePath = Paths.get(DATA_DIR, fileName);

                        if (!Files.exists(filePath)) {
                            System.err.println("Bo qua vi khong tim thay file: " + filePath);
                            continue;
                        }

                        // Đọc dữ liệu từ ổ cứng vào RAM trước (không tính vào thời gian sắp xếp)
                        Integer[] originalData = loadDataset(filePath);
                        System.out.printf("%n--- Dang do: %-18s | N = %,d ---%n", dist, size);

                        for (int cutoff : CUTOFFS) {
                            // Chạy làm nóng cục bộ cho từng mức cutoff
                            for (int w = 0; w < WARMUP_RUNS; w++) {
                                Integer[] warmCopy = originalData.clone();
                                Merge.sort(warmCopy, cutoff);
                            }

                            double[] times = new double[MEASURED_RUNS];

                            for (int trial = 1; trial <= MEASURED_RUNS; trial++) {
                                // Sao chép mảng gốc trước khi bấm giờ để tránh sắp xếp trên mảng đã có thứ tự
                                Integer[] dataCopy = originalData.clone();

                                long startTime = System.nanoTime();
                                Merge.sort(dataCopy, cutoff);
                                long endTime = System.nanoTime();

                                // Kiểm tra nhanh tính đúng đắn (nằm ngoài vùng bấm giờ)
                                if (!isSorted(dataCopy)) {
                                    throw new IllegalStateException(
                                            "Loi thuat toan: Mang chua duoc sap xep dung tai cutoff = " + cutoff
                                    );
                                }

                                double elapsedMs = (endTime - startTime) / 1_000_000.0;
                                times[trial - 1] = elapsedMs;

                                // Dùng Locale.US để dấu thập phân luôn là dấu chấm (.), tránh hỏng cấu trúc CSV
                                writer.write(String.format(
                                        Locale.US,
                                        "%s,%d,%d,%d,%.6f",
                                        dist, size, cutoff, trial, elapsedMs
                                ));
                                writer.newLine();
                            }

                            writer.flush(); // Đẩy dữ liệu xuống file ngay sau mỗi mức cutoff
                            printProgress(cutoff, times);
                        }
                    }
                }
            }

            System.out.println("\n=== HOAN TAT! Ket qua da duoc luu tai: " + csvPath + " ===");

        } catch (IOException e) {
            System.err.println("Loi I/O trong qua trinh chay benchmark: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Đọc mảng số nguyên từ file dữ liệu do DataGenerator tạo ra.
     */
    private static Integer[] loadDataset(Path filePath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String firstLine = reader.readLine();
            if (firstLine == null) {
                throw new IOException("File rong: " + filePath);
            }
            int n = Integer.parseInt(firstLine.trim());
            Integer[] data = new Integer[n];
            for (int i = 0; i < n; i++) {
                String line = reader.readLine();
                if (line == null) {
                    throw new IOException("Du lieu thieu so voi N khai bao trong file: " + filePath);
                }
                data[i] = Integer.parseInt(line.trim());
            }
            return data;
        }
    }

    /**
     * Chạy nháp một số lượt trên mảng giả lập để trình biên dịch JIT của JVM ổn định hiệu năng.
     */
    private static void warmUpJVM() {
        Integer[] dummy = new Integer[20_000];
        for (int i = 0; i < dummy.length; i++) {
            dummy[i] = dummy.length - i;
        }
        for (int i = 0; i < 30; i++) {
            Integer[] copy = dummy.clone();
            Merge.sort(copy, i % 15);
        }
    }

    /**
     * Kiểm tra mảng đã được sắp xếp không giảm hay chưa.
     */
    private static boolean isSorted(Integer[] a) {
        for (int i = 1; i < a.length; i++) {
            if (a[i].compareTo(a[i - 1]) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * In tóm tắt giá trị trung bình và trung vị ra màn hình console để theo dõi tiến độ.
     */
    private static void printProgress(int cutoff, double[] times) {
        double sum = 0;
        for (double t : times) sum += t;
        double mean = sum / times.length;

        double[] sortedTimes = times.clone();
        Arrays.sort(sortedTimes);
        double median = sortedTimes[sortedTimes.length / 2];

        System.out.printf(
                Locale.US,
                "  CUTOFF = %2d | Mean: %8.3f ms | Median: %8.3f ms%n",
                cutoff, mean, median
        );
    }

    /**
     * Lưu thông tin môi trường phần cứng và JVM vào file text để đưa vào báo cáo.
     */
    private static void saveSystemInfo(Path outPath) throws IOException {
        Runtime rt = Runtime.getRuntime();
        String info = String.format(
                "=== THONG SO MOI TRUONG THI NGHIEM ===%n" +
                        "Thoi gian chay : %s%n" +
                        "He dieu hanh   : %s (%s) - Phien ban: %s%n" +
                        "So luong CPU   : %d logical cores%n" +
                        "Java Version   : %s (%s)%n" +
                        "JVM Name       : %s%n" +
                        "Max Heap Memory: %.2f MB%n" +
                        "Warmup runs    : %d%n" +
                        "Measured runs  : %d%n",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                System.getProperty("os.version"),
                rt.availableProcessors(),
                System.getProperty("java.version"),
                System.getProperty("java.vendor"),
                System.getProperty("java.vm.name"),
                rt.maxMemory() / (1024.0 * 1024.0),
                WARMUP_RUNS,
                MEASURED_RUNS
        );
        Files.writeString(outPath, info);
        System.out.println(info);
    }
}