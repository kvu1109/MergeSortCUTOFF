import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class DataGenerator {

    // Cố định seed để mọi lần sinh dữ liệu đều cho ra cùng một bộ test (Reproducibility)
    private static final long SEED = 42L;
    private static final String OUTPUT_DIR = "data";

    // Dải kích thước N nhân đôi liên tiếp (Doubling hypothesis)
    private static final int[] SIZES = {
            50_000,
            100_000,
            200_000,
            400_000,
            800_000
    };

    public static void main(String[] args) {
        try {
            Path dirPath = Paths.get(OUTPUT_DIR);
            Files.createDirectories(dirPath);

            Random rand = new Random(SEED);
            long totalBytes = 0;

            System.out.println("=== BAT DAU SINH BO DU LIEU THI NGHIEM (SEED = " + SEED + ") ===");

            for (int n : SIZES) {
                totalBytes += saveDataset(dirPath, "random_" + n + ".txt", generateRandom(n, rand));
                totalBytes += saveDataset(dirPath, "sorted_" + n + ".txt", generateSorted(n));
                totalBytes += saveDataset(dirPath, "reverse_" + n + ".txt", generateReverse(n));
                totalBytes += saveDataset(dirPath, "partially_sorted_" + n + ".txt", generatePartiallySorted(n, rand));
                totalBytes += saveDataset(dirPath, "duplicates_" + n + ".txt", generateDuplicates(n, rand));
            }

            double totalMB = totalBytes / (1024.0 * 1024.0);
            System.out.printf("=== HOAN TAT! Tong dung luong thu muc '%s': %.2f MB ===%n", OUTPUT_DIR, totalMB);

        } catch (IOException e) {
            System.err.println("Loi khi ghi file du lieu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 1. Ngẫu nhiên đồng đều (Average case):
     * Tạo mảng 0..N-1 rồi tráo bằng thuật toán Knuth Shuffle để mọi hoán vị đồng xác suất.
     */
    private static int[] generateRandom(int n, Random rand) {
        int[] a = generateSorted(n);
        knuthShuffle(a, rand);
        return a;
    }

    /**
     * Thuật toán Knuth Shuffle (Fisher-Yates):
     * Tại bước i, chọn ngẫu nhiên r trong [0, i] và hoán đổi a[i] với a[r].
     */
    private static void knuthShuffle(int[] a, Random rand) {
        for (int i = 0; i < a.length; i++) {
            int r = rand.nextInt(i + 1); // Sinh số nguyên trong [0, i]
            int swap = a[i];
            a[i] = a[r];
            a[r] = swap;
        }
    }

    /**
     * 2. Đã sắp xếp tăng dần (Best case của Insertion Sort): 0, 1, ..., N-1
     */
    private static int[] generateSorted(int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = i;
        }
        return a;
    }

    /**
     * 3. Sắp xếp ngược giảm dần (Worst case của Insertion Sort): N-1, N-2, ..., 0
     */
    private static int[] generateReverse(int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = n - 1 - i;
        }
        return a;
    }

    /**
     * 4. Gần như đã sắp xếp (Partially-sorted):
     * Xuất phát từ mảng tăng dần, thực hiện khoảng 2% số lần hoán đổi cục bộ trong bán kính hẹp
     * để đảm bảo tổng số nghịch thế (inversions) <= c*N.
     */
    private static int[] generatePartiallySorted(int n, Random rand) {
        int[] a = generateSorted(n);
        int swaps = Math.max(1, n / 50); // Khoảng 2% số phần tử
        for (int s = 0; s < swaps; s++) {
            int i = rand.nextInt(n);
            int offset = rand.nextInt(11) - 5; // Dịch chuyển cục bộ trong khoảng [-5, +5]
            int j = Math.max(0, Math.min(n - 1, i + offset));
            int swap = a[i];
            a[i] = a[j];
            a[j] = swap;
        }
        return a;
    }

    /**
     * 5. Nhiều phần tử trùng lặp (Many duplicates):
     * Chỉ dùng 10 giá trị khóa phân biệt từ 0 đến 9.
     */
    private static int[] generateDuplicates(int n, Random rand) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = rand.nextInt(10);
        }
        return a;
    }

    /**
     * Lưu mảng ra file văn bản: dòng đầu là kích thước N, các dòng sau là từng phần tử.
     */
    private static long saveDataset(Path dir, String fileName, int[] data) throws IOException {
        Path filePath = dir.resolve(fileName);
        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            writer.write(Integer.toString(data.length));
            writer.newLine();
            for (int val : data) {
                writer.write(Integer.toString(val));
                writer.newLine();
            }
        }
        long size = Files.size(filePath);
        System.out.printf("Da tao: %-32s (%.2f MB)%n", filePath.toString(), size / (1024.0 * 1024.0));
        return size;
    }
}