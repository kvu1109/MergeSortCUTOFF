import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SortTest {

    // Lớp dữ liệu phụ trợ để kiểm tra tính ổn định (Stability) của thuật toán[cite: 179]
    private static class Item implements Comparable<Item> {
        final int key;
        final int originalIndex;

        Item(int key, int originalIndex) {
            this.key = key;
            this.originalIndex = originalIndex;
        }

        @Override
        public int compareTo(Item other) {
            return Integer.compare(this.key, other.key);
        }
    }

    // Hàm tiện ích kiểm tra mảng đã tăng dần chưa
    private boolean isSorted(Comparable[] a) {
        for (int i = 1; i < a.length; i++) {
            if (a[i].compareTo(a[i - 1]) < 0) {
                return false;
            }
        }
        return true;
    }

    // =========================================================================
    // 1. KIỂM THỬ INSERTION SORT (TOÀN MẢNG & MẢNG CON)
    // =========================================================================

    @Test
    @DisplayName("Insertion.sort(a, lo, hi) chỉ sắp xếp đúng đoạn [lo, hi] và giữ nguyên phần còn lại")
    void testInsertionSortSubarray() {
        Integer[] a = {99, 88, 5, 2, 8, 1, 9, 77, 66};
        // Chỉ sắp xếp từ index 2 đến index 6: {5, 2, 8, 1, 9} -> {1, 2, 5, 8, 9}
        Insertion.sort(a, 2, 6);

        Integer[] expected = {99, 88, 1, 2, 5, 8, 9, 77, 66};
        assertArrayEquals(expected, a, "Các phần tử ngoài đoạn [lo, hi] không được phép thay đổi vị trí");
    }

    @Test
    @DisplayName("Insertion.sort xử lý tốt các trường hợp biên")
    void testInsertionSortEdgeCases() {
        Integer[] empty = {};
        Insertion.sort(empty);
        assertEquals(0, empty.length);

        Integer[] single = {42};
        Insertion.sort(single);
        assertArrayEquals(new Integer[]{42}, single);

        Integer[] reverse = {5, 4, 3, 2, 1};
        Insertion.sort(reverse);
        assertArrayEquals(new Integer[]{1, 2, 3, 4, 5}, reverse);
    }

    // =========================================================================
    // 2. KIỂM THỬ MERGESORT VỚI NHIỀU GIÁ TRỊ CUTOFF KHÁC NHAU
    // =========================================================================

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 7, 10, 15, 32})
    @DisplayName("Mergesort hoạt động đúng với các trường hợp biên (N = 0, 1, 2) ở mọi mức CUTOFF")
    void testMergeSortEdgeCases(int cutoff) {
        Integer[] empty = {};
        Merge.sort(empty, cutoff);
        assertEquals(0, empty.length);

        Integer[] single = {10};
        Merge.sort(single, cutoff);
        assertArrayEquals(new Integer[]{10}, single);

        Integer[] twoElements = {20, 10};
        Merge.sort(twoElements, cutoff);
        assertArrayEquals(new Integer[]{10, 20}, twoElements);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 7, 10, 16, 50})
    @DisplayName("Mergesort kiểm tra ranh giới kích thước mảng quanh ngưỡng CUTOFF (N = cutoff - 1, cutoff, cutoff + 1)")
    void testMergeSortAroundCutoffBoundary(int cutoff) {
        Random rand = new Random(42);
        int[] testSizes = {Math.max(0, cutoff - 1), cutoff, cutoff + 1};

        for (int n : testSizes) {
            Integer[] actual = new Integer[n];
            for (int i = 0; i < n; i++) {
                actual[i] = rand.nextInt(1000);
            }
            Integer[] expected = actual.clone();
            Arrays.sort(expected);

            Merge.sort(actual, cutoff);
            assertArrayEquals(expected, actual, "Sai lệch tại N = " + n + " với cutoff = " + cutoff);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 7, 10, 20})
    @DisplayName("Mergesort sắp xếp đúng trên các phân phối dữ liệu đặc thù (Đã sắp xếp, Ngược, Trùng lặp)")
    void testMergeSortSpecialDistributions(int cutoff) {
        int n = 100;

        // 1. Mảng đã sắp xếp tăng dần
        Integer[] sorted = new Integer[n];
        for (int i = 0; i < n; i++) sorted[i] = i;
        Merge.sort(sorted, cutoff);
        assertTrue(isSorted(sorted));

        // 2. Mảng sắp xếp ngược giảm dần
        Integer[] reverse = new Integer[n];
        for (int i = 0; i < n; i++) reverse[i] = n - i;
        Merge.sort(reverse, cutoff);
        assertTrue(isSorted(reverse));

        // 3. Mảng nhiều phần tử trùng lặp
        Integer[] duplicates = new Integer[n];
        for (int i = 0; i < n; i++) duplicates[i] = i % 5;
        Integer[] expectedDup = duplicates.clone();
        Arrays.sort(expectedDup);
        Merge.sort(duplicates, cutoff);
        assertArrayEquals(expectedDup, duplicates);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 7, 10, 25})
    @DisplayName("Mergesort đối chiếu với Arrays.sort() trên mảng ngẫu nhiên kích thước lớn")
    void testMergeSortLargeRandomArray(int cutoff) {
        int n = 10_000;
        Random rand = new Random(12345);
        Double[] actual = new Double[n];
        for (int i = 0; i < n; i++) {
            actual[i] = rand.nextDouble();
        }
        Double[] expected = actual.clone();

        Arrays.sort(expected);
        Merge.sort(actual, cutoff);

        assertArrayEquals(expected, actual);
    }

    // =========================================================================
    // 3. KIỂM THỬ TÍNH ỔN ĐỊNH (STABILITY) - SLIDE 2.2[cite: 179, 184]
    // =========================================================================

    @ParameterizedTest
    @ValueSource(ints = {0, 7, 10, 15})
    @DisplayName("Mergesort + InsertionSort giữ nguyên thứ tự tương đối của các khóa bằng nhau (Stability)")
    void testStability(int cutoff) {
        int n = 200;
        Random rand = new Random(999);
        Item[] items = new Item[n];

        // Tạo mảng với khóa chỉ nằm trong [0..9] để ép xuất hiện rất nhiều khóa trùng nhau
        for (int i = 0; i < n; i++) {
            items[i] = new Item(rand.nextInt(10), i);
        }

        Merge.sort(items, cutoff);

        for (int i = 1; i < n; i++) {
            // Đảm bảo mảng đã được sắp xếp theo key
            assertTrue(items[i - 1].key <= items[i].key);
            // Nếu hai phần tử có cùng key, phần tử đứng trước ban đầu vẫn phải đứng trước
            if (items[i - 1].key == items[i].key) {
                assertTrue(items[i - 1].originalIndex < items[i].originalIndex,
                        "Vi phạm tính ổn định (Stability) tại vị trí " + i + " với cutoff = " + cutoff);
            }
        }
    }
}