package org.gordon.quiz.le;

import java.util.Optional;

/**
 * The quiz question here is to find the highest element less than or equal to a
 * given value in a sorted circular array. After a bit of playing around, I
 * discovered the best approach was to divide and conquer to find the first
 * element greater than or equal to the given value, and then use the previous
 * value as the answer. This is slight tricky for a circular array, because the
 * elements may be non-contiguous in a circular array, e.g.:
 * 
 * 7 8 9 . . . . . 2 4 5
 * 
 * where a dot represents an used element in the circular array.
 * 
 * In the problem, we push() new values onto the stack in sorted order. We
 * maintain head and tail pointers, where a push increments the head (well,
 * actually it logically increments the head, as the buffer may wrap). "Head"
 * actually points to the next free slot, so if head == tail, then the buffer is
 * empty;
 * 
 * In the problem statement, the two methods that must be implemented are
 * push(int) to add an element to the list and search_le(int) to find the
 * desired value. Since any int value is legal, we add a boolean
 * "candidateFound" to indicate that the value is not found. So for the list
 * shown above:
 * 
 * search_le(4) yields 4 (candidateFound = true) search_le(6) yields 5
 * (candidateFound = true) search_le(100) yields candidateFound == false
 * 
 * @author Gary
 *
 */

public class FindLessOrEqual {
    public static final int DEFAULT_BUF_SIZE = 1000;
    private int[] circBuffer;
    private int head;
    private int tail;
    private int candidate;
    private int bufSize;
    private boolean candidateFound = false;

    /**
     * Create an empty search object. The user will call push) to populate.
     */
    public FindLessOrEqual() {
        this.head = this.tail = 0;
        this.circBuffer = new int[DEFAULT_BUF_SIZE];
        this.bufSize = DEFAULT_BUF_SIZE;
    }

    /**
     * Create a new search object. The user may specify a head and tail and
     * pre-initialized buffer through this constructor.
     * 
     * @param head
     * @param tail
     * @param circBuffer
     */
    public FindLessOrEqual(int head, int tail, int[] circBuffer) {
        this.head = head;
        this.tail = tail;
        this.circBuffer = circBuffer;
        this.bufSize = circBuffer.length;
    }

    /**
     * Pushes a new sorted number onto stack.
     * @param num the next number to push on the stack, must be >=
     * previous element.
     */
    void push(int num) {
        if (head == bufSize || (head % bufSize) == (tail % bufSize) - 1) {
            throw new IllegalStateException("Array is full.");
        }

        boolean nonIncreasing = false;
        if (head < tail) {
            // List wraps.
            if ((head == 0 && num < circBuffer[bufSize - 1]) || (num < circBuffer[head - 1])) {
                    nonIncreasing = true;
            }
        } else if (head > tail) {
            // No wrap and non-empty.
            if (num < circBuffer[head - 1]) {
                nonIncreasing = true;
            }
        }
        if (nonIncreasing) {
            throw new IllegalStateException(num + " is not an increasing value");  
        }
        circBuffer[head % bufSize] = num;
        head++;
    }

    /**
     * Find the number, or if not found, the greatest number less than equal to
     * it.
     * 
     * @param j
     *            the target number
     * @return the candidate if found, wrapped in an Optional. The Optional is
     *         empty if no value is found.
     */
    public Optional<Integer> search_le(int j) {
        candidateFound = false;
        candidate = 0;

        // If buffer wraps, we must check the segment from [0, head) and also
        // [tail, <buffer_length>).
        // Note we might find a potential answer both segments, but we need the
        // one with
        // the best match.
        if (tail > head) {
            bin_search_le(0, head, j);
            if (!candidateFound || candidate != j) {
                bin_search_le(tail, circBuffer.length, j);
            }
        } else {
            // No buffer wrap, just check range [tail, head).
            bin_search_le(tail, head, j);
        }
        return candidateFound ? Optional.of(candidate) : Optional.empty();
    }

    // Basic strategy: binary search until the left and right pointers converge.
    // At the point of convergence, we will have found the target element, or if
    // it is not in the list, the first element greater than it. So if it is the
    // first
    // element greater than the target, we need to step back one to the previous
    // element
    // for the answer.
    // Note "right_start" is one past the end of the range, but the algorithm
    // will
    // never actually access or consider that as an array element.
    private void bin_search_le(int left_start, int right_start, int target) {
        // Is entire array out of range?
        if (circBuffer[left_start] > target) {
            return;
        }

        // Degenerate case - one value, this must be <= target.
        if (left_start == right_start) {
            setCandidateIfHighest(circBuffer[left_start]);
        }

        int low = left_start;
        int high = right_start;
        while (low != high) {
            int mid = low + (high - low) / 2;
            if (circBuffer[mid] > target) {
                // Desired value, if any, must be to the left of mid.
                high = mid;
            } else {
                // There is at least one <= value from the midpoint rightward.

                // If the midpoint matches the target, we are done.
                if (circBuffer[mid] == target) {
                    setCandidateIfHighest(target);
                    break;
                }
                low = mid + 1;
            }
        }

        // At this point (if we have not matched the item exactly) high/low
        // point to the
        // first element greater than the target, so we need to decrement by
        // one.
        if (candidate != target) {
            setCandidateIfHighest(circBuffer[high - 1]);
        }
    }

    // Since we may be dealing with two searches (the wraparound case), we need
    // to set the value only if it is a closer result.
    private void setCandidateIfHighest(int val) {
        if (!candidateFound) {
            candidate = val;
            candidateFound = true;
        } else if (val > candidate) {
            candidate = val;
        }

    }

    public static void testAssertion(Optional<Integer> opt, int expected, int value) throws Exception {
        opt.filter(v -> v == expected)
                .orElseThrow(() -> new Exception(String.format("value %d not found for %d", expected, value)));
    }

    public static void main(String[] args) throws Exception {

        // For convenience of the evaluator, I am running the tests here
        // - in real life this would be a JUnit test.
        int[] buffer = new int[2];
        FindLessOrEqual arr = new FindLessOrEqual(0, 0, buffer);
        arr.push(2);
        
        boolean caught = false;
        try {
            arr.push(1);
        } catch (Exception e) {
            caught = true;
        }
        if (!caught) {
            throw new Exception("decreasing value not detected!");
        }
        arr.push(2);

        caught = false;
        try {
            arr.push(4);
        } catch (Exception e) {
            caught = true;
        }
        if (!caught) {
            throw new Exception("full buffer not detected!");
        }

        // Test wrap-around case.
        buffer = new int[DEFAULT_BUF_SIZE];
        buffer[993] = 1;
        buffer[994] = 1;
        buffer[995] = 2;
        buffer[996] = 5;
        buffer[997] = 5;
        buffer[998] = 7;
        buffer[999] = 8;
        buffer[0] = 8;
        buffer[1] = 9;
        buffer[2] = 9;
        buffer[3] = 11;
        arr = new FindLessOrEqual(4, 993, buffer);

        Optional<Integer> res = arr.search_le(0);
        if (res.isPresent()) {
            throw new Exception("0 was unexpectedly matched");
        }

        testAssertion(arr.search_le(1), 1, 1);
        testAssertion(arr.search_le(3), 2, 3);
        testAssertion(arr.search_le(4), 2, 4);
        testAssertion(arr.search_le(65), 11, 65);
        testAssertion(arr.search_le(8), 8, 8);
        testAssertion(arr.search_le(6), 5, 6);
        testAssertion(arr.search_le(10), 9, 10);

        // Test example in test question.
        buffer = new int[1000];
        arr = new FindLessOrEqual(0, 0, buffer);
        arr.push(1);
        arr.push(2);
        arr.push(4);
        arr.push(4);
        arr.push(6);

        testAssertion(arr.search_le(6), 6, 6);
        testAssertion(arr.search_le(5), 4, 5);
        testAssertion(arr.search_le(3), 2, 3);

        System.out.println("All tests passed!");
    }
}
