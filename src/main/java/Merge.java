public class Merge {

    private static final int DEFAULT_CUTOFF = 10; // Giá trị tham khảo trong giáo trình mới[cite: 147]

    private Merge() {}

    /**
     * Gộp hai mảng con đã sắp xếp a[lo..mid] và a[mid+1..hi] thành một mảng con sắp xếp a[lo..hi].
     * Tham khảo: Slide 2.2 Mergesort[cite: 136]
     */
    private static void merge(Comparable[] a, Comparable[] aux, int lo, int mid, int hi) {
        // Bước 1: Sao chép đoạn a[lo..hi] sang mảng phụ aux[lo..hi][cite: 136]
        // Bước 2: Gộp ngược từ aux về a theo đúng quy tắc so sánh để đảm bảo tính ổn định[cite: 136, 185]
        for (int k = lo; k <= hi; k++) {
            aux[k] = a[k];
        }

        int i = lo, j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if      (i > mid)               a[k] = aux[j++];
            else if (j > hi)                a[k] = aux[i++];
            else if (less(aux[j], aux[i]))  a[k] = aux[j++];
            else                            a[k] = aux[i++];
        }
    }

    /**
     * Hàm đệ quy sắp xếp đoạn a[lo..hi] sử dụng mảng phụ aux và ngưỡng cutoff.
     * Tham khảo: Slide 2.2 Mergesort[cite: 137, 147]
     */
    private static void sort(Comparable[] a, Comparable[] aux, int lo, int hi, int cutoff) {
        // 1. Xử lý điều kiện dừng khi mảng rỗng hoặc 1 phần tử (hi <= lo)[cite: 137]
        // 2. Kiểm tra điều kiện kích thước mảng con <= cutoff thì gọi Insertion.sort(a, lo, hi) và return[cite: 147]
        // 3. Chia đôi tại mid = lo + (hi - lo) / 2, gọi đệ quy hai nửa và gọi merge()[cite: 137]
        if (hi <= lo) return;
        if (hi <= lo + cutoff - 1) {
            Insertion.sort(a, lo, hi);
            return;
        }
        int mid = lo + (hi - lo) / 2;
        sort(a, aux, lo, mid, cutoff);
        sort(a, aux, mid+1, hi, cutoff);
        merge(a, aux, lo, mid, hi);
    }

    /**
     * Hàm public cho phép truyền giá trị cutoff tùy chỉnh (dùng cho Unit Test và Benchmark).
     */
    public static void sort(Comparable[] a, int cutoff) {
        Comparable[] aux = new Comparable[a.length];
        sort(a, aux, 0, a.length - 1, cutoff);
    }

    /**
     * Hàm public mặc định sử dụng DEFAULT_CUTOFF.
     */
    public static void sort(Comparable[] a) {
        sort(a, DEFAULT_CUTOFF);
    }

    /***************************************************************************
     * Hàm hỗ trợ (Helper function)
     ***************************************************************************/

    private static boolean less(Comparable v, Comparable w) {
        return v.compareTo(w) < 0;
    }
}