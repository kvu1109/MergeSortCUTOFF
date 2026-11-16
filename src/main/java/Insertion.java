public class Insertion {

    // Không cho phép khởi tạo đối tượng từ lớp tiện ích
    private Insertion() {}

    /**
     * Sắp xếp toàn bộ mảng theo thứ tự tăng dần.
     * Tham khảo: Slide 2.1 Elementary Sorts[cite: 92]
     */
    public static void sort(Comparable[] a) {
        int N = a.length;
        for (int i = 0; i < N; i++) {
            for (int j = i; j > 0; j--) {
                if (less(a[j], a[j-1])) {
                    exch(a, j, j-1);
                }
                else break;
            }
        }
    }

    /**
     * Sắp xếp mảng con a[lo..hi] (bao gồm cả hai đầu mút lo và hi) theo thứ tự tăng dần.
     * Tham khảo: Slide 2.2 Mergesort (phần Practical improvements)[cite: 147]
     */
    public static void sort(Comparable[] a, int lo, int hi) {
        // Lưu ý điều kiện dừng của vòng lặp trong để không vượt quá biên trái lo.
        for (int i = lo + 1; i <= hi; i++) {
            for (int j = i; j > lo && less(a[j], a[j - 1]); j--) {
                exch(a, j, j - 1);
            }
        }
    }

    /***************************************************************************
     * Các hàm hỗ trợ (Helper functions) - Slide 2.1[cite: 82]
     ***************************************************************************/

    // Kiểm tra v có nhỏ hơn w hay không
    private static boolean less(Comparable v, Comparable w) {
        return v.compareTo(w) < 0;
    }

    // Hoán đổi vị trí phần tử tại chỉ số i và j trong mảng a
    private static void exch(Comparable[] a, int i, int j) {
        Comparable swap = a[i];
        a[i] = a[j];
        a[j] = swap;
    }
}