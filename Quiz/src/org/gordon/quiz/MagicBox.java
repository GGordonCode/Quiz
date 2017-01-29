package org.gordon.quiz;

/**
 * Solves the "Magic Box" quiz question using two different functional approaches to compare
 * performance (and also to solve the problem, of course!).  You are given a box with a given
 * number of rows and columns, where we assume the box is rectangular (or square).  Each cell
 * in the box has a 0 or 1.  Given the initial box, the goal is to find the minimum number of
 * columns that must be "flipped" (0's and 1's swapped) to maximize the row score.  A row adds
 * one point to the score if all its elements are 0's or all are 1's.
 * 
 * So this box has a score of two, as rows 1 and 3 (zero-based) are uniform with all 0's or 1's.
 * 0 0 1 1 0
 * 0 0 0 0 0
 * 1 0 1 0 1
 * 1 1 1 1 1
 * 0 1 1 1 0
 * 
 * There exists a Knuth algorithm "L" to find all the unique permutations of a set, but in the
 * spirit of the challenge, we don't use this because it's, well, it's cheating!  Besides, this
 * is a very specific subset needed here, only 0's and 1's.
 * 
 * Instead we observe that if there are n columns to flip, if we generate the sequence [0, 2**n - 1],
 * this includes all possible bit patterns of 0 and 1 without duplications, and we can do this
 * without using recursion.  Nevertheless, without tweaking the JVM parameters, the computation
 * becomes very slow (but does not go OOM) at around 30 columns (at least on my old 2010 Quad Core i7
 * 1.73 GHz laptop running Java from the IDE).  I had coded a solution using BigInteger to avoid the
 * limit of 63 columns (the limit a long will allow), but using longs is simpler to read and fine to
 * demonstrate the concept.
 * 
 * Note each score computation for a given set of flipped columns is independent, so we may do
 * the computation in parallel.  We note that for small data sets, parallelization doesn't
 * appear to help that much for the immutable case, but for larger sets (~20), we notice a speedup.
 * The mutable case does not appear to be helped by parallelism, due to exclusive resource contention.
 * 
 * The two coding approaches are as follows: the first is a "pure" functional approach that adheres
 * strictly to the principle of immutability of objects.  The second updates an AtomicReference with the
 * best solution as needed from parallel threads.  The latter significantly reduces the number of temp
 * Solution objects created at the cost of contention for exclusive access to the atomic reference, and
 * (somewhat surprisingly, perhaps) we note the latter approach is significantly slower as the data set
 * size increases.  So the contention for the Atomic Reference becomes a bottleneck.  If we remove the
 * .parallel() specification from the mutable case, thereby removing the contention, we note the performance
 * is roughly the same as with parallel(), so parallelism isn't always an automatic win.
 */

import java.util.BitSet;
import java.util.Random;
import java.util.function.BinaryOperator;
import java.util.stream.LongStream;
import java.util.concurrent.atomic.AtomicReference;

public class MagicBox {

	public static void main(String[] args) {
		MagicBox mb = new MagicBox();
		byte[][] shouldScoreTwoFlippingOneColumn = {
			{0, 0, 1, 0, 0}, {0, 0, 1, 0, 0}, {0, 0, 1, 1, 0}, { 0, 0, 0, 1, 1}, { 0, 0, 0, 1, 1 }};
		Solution sw = mb.solveImmutable(shouldScoreTwoFlippingOneColumn);
		System.out.println("winner is " + sw);
		sw = mb.solveMutable(shouldScoreTwoFlippingOneColumn);
		System.out.println("winner is " + sw);
		
		byte[][] instantWinner = {
				{1, 1, 1, 1, 1}, {1, 1, 1, 1, 1}, {1, 1, 1, 1, 1}, { 1, 1, 1, 1, 1}, { 1, 1, 1, 1, 1 }};
		sw = mb.solveImmutable(instantWinner);
		System.out.println("winner is " + sw);
		sw = mb.solveMutable(instantWinner);
		System.out.println("winner is " + sw);

		// For timing purposes.
		for (int numCol = 10; numCol < 30; numCol++) {
			byte[][] b2 = new byte[4][];
			Random r = new Random();
			for (int i = 0; i < 4; i++) {
				b2[i] = new byte[numCol];
				for (int j = 0; j < numCol; j++) {
					b2[i][j] = r.nextBoolean() ? (byte) 1 : (byte) 0;
				}
			}
			System.out.println(numCol + " columns immutable solution: " + mb.solveImmutable(b2));
			System.out.println(numCol + " columns mutable solution: " + mb.solveMutable(b2));
		}
	}
	
	// Immutable solution first.
	
	// The lambda to be used by reduce() to choose the better of two solutions.
	BinaryOperator<Solution> chooseBetterSolution = (Solution s1, Solution s2) -> {
		if (s1.getScore() > s2.getScore() || (s1.getScore() == s2.getScore()
				&& s1.getFlips().cardinality() < s2.getFlips().cardinality())) {
			return s1;
		} else {
			return s2;
		}
	};

	/**
	 * Solves the Magic Box problem for a given two dimensional array of 0's and 1's.
	 * The array should be rectangular, and the maximum number of columns is 63 bits
	 * (a bit for each column to flip).  This approach creates and compares immutable objects,
	 * as per "pure" functional programming, and reduces to the winning solution by comparing
	 * computed Solution objects.
	 * @param box the box to solve
	 * @return the <code>Solution/code> object containing the maximum score and columns
	 * flipped to accomplish this.  Note the answer will be the one with the highest score
	 * requiring the fewest columns to flip (ties are resolved arbitrarily).
	 */
	public Solution solveImmutable(final byte[][] box) {
		// Note: common code for immutable and mutable cases *not* refactored to preserve clarity.
		final int columns = box[0].length;
		if (columns > (Long.SIZE - 1)) {
			throw new IllegalArgumentException("Maximum allowed column size is " + (Long.SIZE - 1));
		}
		for (int i = 1; i < box.length; i++) {
			if (box[i].length != columns) {
				throw new IllegalArgumentException("Non-rectangular box specified!");
			}
		}
		
		// Outlier: we are already at a complete solution.
		final Solution initialState = scoreSolution(box, 0);
		if (initialState.score == box.length)
			return new Solution(box.length, new BitSet());

		final long limit = (long) (Math.pow(2, columns) - 1);
		long pstart = System.currentTimeMillis();

		// The strategy is to map each long value to a Solution object, filter out those
		// that cannot possibly be a winner (score not greater than initial score with
		// fewer flips), and then finally the reduce/compare to find the best among
		// remaining candidates.  N.B. The "identity" element I for reduce() must be such
		// that the relation accumulator.apply(I, E) = E for all elements E.  If we chose a
		// Solution with a score of -1 for the identity, the compare will always select E,
		// so this will suffice.  Also, note we can skip the bit pattern with all 1's (2**n - 1),
		// as it will yield the same score as the bit pattern with no bits set, i.e. 0.
		Solution solution = LongStream.range(0, limit).parallel().mapToObj(l -> scoreSolution(box, l))
				.filter(s -> s.getScore() > initialState.getScore() || s.getFlips().cardinality() == 0)
				.reduce(new Solution(-1, new BitSet()), chooseBetterSolution);

		long pend = System.currentTimeMillis();
		System.out.printf("parallel immutable (%d) took: %d%n", columns, (pend - pstart));
		return solution;
	}
	
	// Count the number of rows with identical values given the current mask.  Note
	// we do the flip "in-place" without actually changing the original array.
	private Solution scoreSolution(final byte[][] box, final long flipMask) {
		// Note: common code for immutable and mutable cases *not* refactored to preserve clarity.
		final int rows = box.length;
		final int columns = box[0].length;
		int score = 0;
		for (int i = 0; i < rows; i++) {
			int j = 0;
			byte left, right = 0; // The 0 value is meaningless, but the
									// compiler can't grok the loop logic.
			for (; j < columns - 1; j++) {
				// Flip the element if it is in a flipped column, as per the
				// mask. Note, we can avoid the bit calculation for the left
				// element by grabbing the previous right element (except for
				// the 0th element, obviously).
				if (j == 0) {
					left = (flipMask & (1 << j)) != 0 ? (byte) (box[i][j] ^ 1) : box[i][j];
				} else {
					left = right;
				}
				right = (flipMask & (1 << (j + 1))) != 0 ? (byte) (box[i][j + 1] ^ 1) : box[i][j + 1];
				if (left != right) {
					// This row is not uniform, so short-circuit out.
					break;
				}
			}
			if (j == columns - 1) {
				// All elements matched, score!
				score++;
			}
		}

		Solution sol = new Solution(score, BitSet.valueOf(new long[] { flipMask }));
		// System.out.println(sol);
		return sol;
	}
	
	// Mutable solution follows.
	
	/**
	 * Solves the Magic Box problem for a given two dimensional array of 0's and 1's.
	 * The array should be rectangular, and the maximum number of columns is 63 bits
	 * (a bit for each column to flip).  This solution avoids creation of all the temporary
	 * Solution objects of the immutable case and instead updates an AtomicReference if we have
	 * a new winner, but it appears to be significantly slower for larger data sets due to contention
	 * for exclusive access to the resource.
	 * @param box the box to solve
	 * @return the <code>Solution/code> object containing the maximum score and columns
	 * flipped to accomplish this.  Note the answer will be the one with the highest score
	 * requiring the fewest columns to flip (ties are resolved arbitrarily).
	 */
	public Solution solveMutable(final byte[][] box) {
		// Note: common code for immutable and mutable cases *not* refactored to preserve clarity.
		final int columns = box[0].length;
		if (columns > (Long.SIZE - 1)) {
			throw new IllegalArgumentException("Maximum allowed column size is " + (Long.SIZE - 1));
		}
		for (int i = 1; i < box.length; i++) {
			if (box[i].length != columns) {
				throw new IllegalArgumentException("Non-rectangular box specified!");
			}
		}
		
		// Outlier: we are already at a complete solution.
		if (scoreSolution(box, 0).score == box.length)
			return new Solution(box.length, new BitSet());

		final long limit = (long) (Math.pow(2, columns) - 1);
		final AtomicReference<Solution> winner = new AtomicReference<>();
		long pstart = System.currentTimeMillis();

		// Note we can skip the bit pattern with all 1's (2**n - 1), as it will yield the
		// same score as the bit pattern with no bits set, i.e. 0.
		// Remove/add the .parallel() to compare parallel with sequential.
		//LongStream.range(0, limit).forEach(l -> comapreAndUpdateSolution(box, l, winner));
		LongStream.range(0, limit).parallel().forEach(l -> comapreAndUpdateSolution(box, l, winner));

		long pend = System.currentTimeMillis();
		System.out.printf("parallel mutable (%d) took: %d%n", columns, (pend - pstart));
		return winner.get();
	}
	
	// Count the number of rows with identical values given the current mask.
	// Compare this solution against the best solution seen so far, and update
	// if this is the best Solution seen so far.  Note we do the flip "in-place"
	// without actually changing the original array.
	private void comapreAndUpdateSolution(final byte[][] box, final long flipMask,
			final AtomicReference<Solution> winner) {
		// Note: common code for immutable and mutable cases *not* refactored to preserve clarity.
		final int rows = box.length;
		final int columns = box[0].length;
		int score = 0;
		for (int i = 0; i < rows; i++) {
			int j = 0;
			byte left, right = 0; // The 0 value is meaningless, but the
									// compiler can't grok the loop logic.
			for (; j < columns - 1; j++) {
				// Flip the element if it is in a flipped column, as per the
				// mask. Note, we can avoid the bit calculation for the left
				// element by grabbing the previous right element (except for
				// the 0th element, obviously).
				if (j == 0) {
					left = (flipMask & (1 << j)) != 0 ? (byte) (box[i][j] ^ 1) : box[i][j];
				} else {
					left = right;
				}
				right = (flipMask & (1 << (j + 1))) != 0 ? (byte) (box[i][j + 1] ^ 1) : box[i][j + 1];
				if (left != right) {
					// This row is not uniform, so short-circuit out.
					break;
				}
			}
			if (j == columns - 1) {
				// All elements matched, score!
				score++;
			}

			final int theScore = score; // Ack! Java, you should recognize score
										// is unmodified onwards.

			// Atomically update the best solution if the current one is the
			// best so far. Note we avoid having to create a Solution object
			// for a Solution that cannot possible be a winner.
			winner.getAndUpdate(old -> {
				if (old == null || theScore > old.score) {
					return new Solution(theScore, BitSet.valueOf(new long[] { flipMask }));
				} else if (theScore == old.score) {
					// Only the shortest but set is the winner - if there are
					// multiple shortest winners,
					// we pick the first found.
					BitSet bs = BitSet.valueOf(new long[] { flipMask });
					if (bs.cardinality() < old.flips.cardinality()) {
						return new Solution(theScore, bs);
					}
				}
				return old;
			});
		}
	}
	
	/**
	 * A wrapper class to hold the winning solution - the score and the bit set
	 * leading to that score.
	 *
	 */
	public static class Solution {
		private int score;
		private BitSet flips;

		/**
		 * Create a new Solution object.
		 * @param flips set of bits corresponding to the columns that were flipped.
		 * @param score the number of rows that are uniform as a result of the flip
		 */
		public Solution(int score, BitSet flips) {
			this.flips = flips;
			this.score = score;
		}
		
		/**
		 * Gets the score for the box
		 * @return the number of rows that are uniform
		 */
		public int getScore() {
			return score;
		}
		
		/**
		 * Get the bit mask for the set of columns leading to the max score
		 * @return the winning set of columns
		 */
		public BitSet getFlips() {
			return flips;
		}
		
		@Override
		public String toString() {
			return String.format("score: %d, columns flipped (0-based): %s", score, flips);
		}
	}
}
