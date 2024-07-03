import io;
import Arrays;
class MergeSort {
    int[] merge(int[] left, int[] right) {
        int[] res;
        int i;
        int j;
        int k;
        res = new int[left.length + right.length];
        i = 0;
        j = 0;
        k = 0;
        while (i < left.length && j < right.length) {
            if (left[i] < right[j]) {
                res[k] = left[i];
                i = i + 1;
            }
            else {
                res[k] = right[j];
                j = j + 1;
            }
            k = k + 1;
        }
        while (i < left.length) {
            res[k] = left[i];
            i = i + 1;
            k = k + 1;
        }
        while (j < right.length) {
            res[k] = right[j];
            j = j + 1;
            k = k + 1;
        }
        return res;
    }
    int[] sort(int... arr) {
        int mid;
        int[] left;
        int[] right;
        int[] res;
        if (arr.length < 2) {
            res = arr;
        }
        else {
            mid = arr.length / 2;
            left = this.sort(Arrays.copyOfRange(arr, 0, mid));
            right = this.sort(Arrays.copyOfRange(arr, mid, arr.length));
            res = this.merge(left, right);
        }
        return res;
    }

    public static void main(String[] wellwellwell) {
        MergeSort ms;
        int[] arr;
        int[] sorted;
        int i;
        ms = new MergeSort();
        arr = [1, 2, 3, 4, 5];

        sorted = ms.sort(arr);
        sorted = ms.sort([1, 2, 3, 4, 5]);
        i = 0;
        while (i < sorted.length) {
            io.println(sorted[i]);
            i = i + 1;
        }
    }
}
