package org.gordon.quiz;

/**
 * Solves the "Magic Box" quiz question.  You are given a box with a given number of rows
 * and columns, where we assume the box is rectangular (or square).  Each cell in the box
 * has a 0 or 1.  Given the initial box, the goal is to find the minimum number of columns
 * that must be "flipped" (0's and 1's swapped) to maximize the row score.  A row adds one
 * point to the score if all its elements are 0's or all are 1's.
 * 
 * So this box has a score of two, the second and fourth rows are uniform with all 0's or 1's.
 * 0 0 1 1 0
 * 0 0 0 0 0
 * 1 0 1 0 1
 * 1 1 1 1 1
 * 0 1 1 1 0
 * 
 * There exists a Knuth "L" algorithm to find all the unique permutations of a set, but in the
 * spirit of the challenge, we don't use this because it's, well, it's cheating!
 * 
 * Instead we observe that if there a n columns to flip, if we generate the sequence [0, 2**n - 1],
 * this includes all possible bit patterns of 0 and 1 without duplications, and we can do this
 * without using recursion.  Nevertheless, without tweaking the JVM parameters, the computation
 * becomes prohibitive at around 30 columns.  I had coded a solution using BigInteger to avoid the limit
 * of 64 columns (the limit a long will allow), but I went with the simpler long-based approach due to
 * the computational infeasibility.
 * 
 * Note each score computation for a given set of flipped columns is independent, so do
 * the computation in parallel.  We note that for mall data sets, parallelization doesn't
 * appear to help that much, but for larger sets (~20), we notice a speedup.
 */

import java.util.BitSet;
import java.util.Random;
import java.util.stream.LongStream;

public class MagicBox {

	public static void main(String[] args) {
		MagicBox mb = new MagicBox();
		Solution sw = mb.solve(new byte[][]{
			{0, 0, 1, 0, 0}, {0, 0, 1, 0, 0}, {0, 0, 1, 1, 0}, { 0, 0, 0, 1, 1}, { 0, 0, 0, 1, 1 }});
		System.out.println("winner is: " + sw);

		// For timing purposes.
		int numCol = 30;
		byte[][] b2 = new byte[4][];
		Random r = new Random();
		for (int i = 0; i < 4; i++) {
			b2[i] = new byte[numCol];
			for (int j = 0; j < numCol; j++) {
				b2[i][j] = r.nextBoolean() ? (byte) 1 : (byte) 0;
			}	
		}
		System.out.println("lotsa columns: " + mb.solve(b2));
	}

	/**
	 * Solves the Magic Box problem for a given two dimensional array of 0's and 1's.
	 * The array should be rectangular, and the maximum number of columns is 64 bits
	 * (a bit for each column to flip).
	 * @param box the box to solve
	 * @return the <code>Solution/code> object containing the maximum score and columns
	 * flipped to accomplish this.  Note the answer will be the one with the highest score
	 * requiring the fewest columns to flip (ties are resolved arbitrarily).
	 */
	public Solution solve(final byte[][] box) {
		final int columns = box[0].length;
		if (columns > Long.SIZE) {
			throw new IllegalArgumentException("Maximum allowed column size is " + Long.SIZE);
		}
		for (int i = 1; i < box.length; i++) {
			if (box[i].length != columns) {
				throw new IllegalArgumentException("Non-rectangular box specified!");
			}
		}
		final long limit = (long) (Math.pow(2, columns) - 1);
		long pstart = System.currentTimeMillis();

		Solution solution = LongStream.range(1, limit).parallel().mapToObj(l -> scoreSolution(box, l))
				.reduce(scoreSolution(box, 0),
						(r, e) -> {
							if (e.getScore() > r.getScore() || (e.getScore() == r.getScore()
									&& e.getFlips().cardinality() < r.getFlips().cardinality())) {
								return e;
							} else {
								return r;
							}
						});

		long pend = System.currentTimeMillis();
		System.out.printf("parallel long (%d) took: %d%n", columns, (pend - pstart));
		return solution;
	}

	// Count the number of rows with identical values given the current mask.
	// Note we do the flip "in-place" without actually changing the original array.
	private Solution scoreSolution(final byte[][] box, final long flipMask) {
		final int rows = box.length;
		final int columns = box[0].length;
		int score = 0;
		for (int i = 0; i < rows; i++) {
			int j = 0;
			byte left, right = 0; // The 0 value is meaningless, but the compiler can't grok the loop logic.
			for (; j < columns - 1; j++) {
				// Flip the element if it is in a flipped column, as per the mask.  Note, we
				// can avoid the bit calculation for the left element by grabbing the previous
				// right element (except for the 0th element, obviously).
				if (j == 0) {
					left = (flipMask & (1 << j)) != 0 ? (byte)(box[i][j] ^ 1) : box[i][j];
				} else {
					left = right;
				}
				right = (flipMask & (1 << (j + 1))) != 0 ? (byte)(box[i][j + 1] ^ 1) : box[i][j + 1];
				if (left != right) {
					// This row is not uniform, so short-circuit out.
					break;
				}
			}
			if (j == columns - 1) {
				// All elements matched, score!  Ole! Ole!
				score++;
			}
		}
		
		Solution sol = new Solution(BitSet.valueOf(new long[] { flipMask }), score);
		//System.out.println(sol);
		return sol;
	}
	
	/**
	 * A wrapper class to hold the winning solution - the score and the bit set
	 * leading to that score.
	 *
	 */
	public static class Solution {
		private BitSet flips;
		private int score;

		/**
		 * Create a new SolutionWrapper.
		 * @param flips set of bits corresponding to the columns that were flipped.
		 * @param score the number of rows that are uniform as a result of the flip
		 */
		public Solution(BitSet flips, int score) {
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
			return String.format("BitSet: %s, score: %d", flips, score);
		}
	}
}
