
/*
 * Cullen Drissell
 * 5-20-19
 * 
 * Reduce a sudoku problem to a SAT problem.
 * Solve using SAT solver.
 * Only works for 9x9 puzzles.
 *
 * Compile: javac -cp .:org.sat4j.core.jar SudokuSolver.java
 * Run: java -cp .:org.sat4j.core.jar SudokuSolver path/to/puzzle/file
 */

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;

import java.io.IOException;
import java.io.FileNotFoundException;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

public class SudokuSolver {
	public int[][] puzzle; // sudoku problem stored in 2-D array
	public int boxLen; // one box in grid
	public int gridLen; // number of rows/cols in puzzle
	public ArrayList<ArrayList<Integer>> formula = new ArrayList<ArrayList<Integer>>(); // SAT formula
	// comments in sudoku file
	public static final Pattern commentPattern = Pattern.compile("c");

	public SudokuSolver(String fname) {
		// init puzzle
		readFile(fname);
	}

	/**
	 * Read sudoku into 2D array.
	 * 
	 * @param fname File containing puzzle.
	 */
	public void readFile(String fname) {
		Scanner s = null;
		try {
			s = new Scanner(new BufferedReader(new FileReader(fname)));
			// skip comments
			while (s.hasNext(commentPattern))
				s.nextLine();
			// assume boxLen by boxLen puzzle
			boxLen = s.nextInt();
			boxLen = s.nextInt();
			gridLen = boxLen * boxLen;
			puzzle = new int[gridLen][gridLen];

			// read puzzle into array
			for (int w = 0; w < gridLen; w++) {
				for (int h = 0; h < gridLen; h++) {
					puzzle[w][h] = s.nextInt();
				}
			}
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
			System.exit(-1);
		} finally {
			if (s != null)
				s.close();
		}

	}

	/**
	 * Return string version of puzzle.
	 */
	public String showPuzzle() {
		StringBuilder sb = new StringBuilder();

		for (int r = 0; r < puzzle.length; r++) {
			for (int c = 0; c < puzzle[r].length; c++) {
				sb.append(puzzle[r][c]);
				sb.append(' ');
			}
			sb.append('\n');
		}
		sb.append('\n');
		return sb.toString();
	}

	/*
	 * Take digits x, y and z and create xyz PRE: x,y,and z are in {1,2,...,9}
	 */
	private int xyzInt(int x, int y, int z) {
		return x * 100 + y * 10 + z;
	}

	/**
	 * Constraint 1: Rows must have all digits and no digit more than once.
	 */
	public void constraint1() {
		ArrayList<Integer> clz = null;
		int row, col, m = 1;

		// Constraint 1.A
		for (row = 1; row <= gridLen; row++) {
			for (m = 1; m <= gridLen; m++) {
				clz = new ArrayList<Integer>();
				for (col = 1; col <= gridLen; col++) {
					clz.add(xyzInt(row, col, m));
				}
				formula.add(clz);
			}
		}

		// Constraint 1.B
		for (row = 1; row <= gridLen; row++) {
			for (m = 1; m <= gridLen; m++) {
				for (col = row + 1; col <= gridLen; col++) {
					clz = new ArrayList<Integer>();
					clz.add(-1 * xyzInt(row, row, m));
					clz.add(-1 * xyzInt(row, col, m));
					formula.add(clz);
				}
			}
		}
	}

	/**
	 * Constraint 2: Columns must have all digits and no digit more than once.
	 */
	public void constraint2() {
		ArrayList<Integer> clz = null;
		int col, row, m = 1;

		// Constraint 2.A
		for (col = 1; col <= gridLen; col++) {
			for (m = 1; m <= gridLen; m++) {
				clz = new ArrayList<Integer>();
				for (row = 1; row <= gridLen; row++) {
					clz.add(xyzInt(row, col, m));
				}
				formula.add(clz);
			}
		}

		// Constraint 2.B
		for (col = 1; col <= gridLen; col++) {
			for (m = 1; m <= gridLen; m++) {
				for (row = col + 1; row <= gridLen; row++) {
					clz = new ArrayList<Integer>();
					clz.add(-1 * xyzInt(col, col, m));
					clz.add(-1 * xyzInt(row, col, m));
					formula.add(clz);
				}
			}
		}
	}

	/**
	 * Each box must have all digits 1-9 and no digit more than once.
	 */
	public void constraint3() {
		ArrayList<Integer> clz = new ArrayList<Integer>();

		// Constraint 3.A
		for (int boxRow = 1; boxRow <= gridLen; boxRow = boxRow + boxLen) {
			for (int boxCol = 1; boxCol <= gridLen; boxCol = boxCol + boxLen) {
				for (int m = 1; m <= gridLen; m++) {
					clz = new ArrayList<Integer>();
					for (int row = boxRow; row < boxRow + boxLen; row++) {
						for (int col = boxCol; col < boxCol + boxLen; col++) {
							clz.add(xyzInt(row, col, m));
						}
					}
					formula.add(clz);
				}
			}
		}

		// Constraint 3.B
		for (int m = 1; m <= gridLen; m++) {
			for (int boxRow = 1; boxRow <= gridLen; boxRow = boxRow + boxLen) {
				for (int boxCol = 1; boxCol <= gridLen; boxCol = boxCol + boxLen) {
					// convert box to array
					int pos = 0;
					String[] boxArray = new String[gridLen];
					for (int row = boxRow; row < boxRow + boxLen; row++) {
						for (int col = boxCol; col < boxCol + boxLen; col++) {
							boxArray[pos] = "" + row + col + m;
							pos++;
						}
					}
					// add array to formula
					for (int j = 0; j <= boxArray.length; j++) {
						for (int i = j + 1; i < boxArray.length; i++) {
							ArrayList<Integer> temp = new ArrayList<Integer>();
							temp.add(-1 * Integer.parseInt(boxArray[j]));
							temp.add(-1 * Integer.parseInt(boxArray[i]));
							formula.add(temp);
						}
					}
				}
			}
		}
	}

	/**
	 * Each cell must have exactly one value in the range 1 through 9.
	 */
	public void constraint4() {
		ArrayList<Integer> clz = new ArrayList<Integer>();
		int row, col, m = 1, n = 1;

		// Constraint 4.A
		for (row = 1; row <= gridLen; row++) {
			for (col = 1; col <= gridLen; col++) {
				clz = new ArrayList<Integer>();
				for (m = 1; m <= gridLen; m++) {
					clz.add(xyzInt(row, col, m));
				}
				formula.add(clz);
			}
		}

		// Constraint 4.B
		for (row = 1; row <= gridLen; row++) {
			for (col = 1; col <= gridLen; col++) {
				for (m = 1; m <= gridLen; m++) {
					for (n = 1; n <= gridLen; n++) {
						if (m != n) {
							clz = new ArrayList<Integer>();
							clz.add(-1 * xyzInt(row, col, m));
							clz.add(-1 * xyzInt(row, col, n));
							formula.add(clz);
						}
					}
				}
			}
		}
	}

	/**
	 * Any preset values that appear in a Sudoku puzzle lead to additional clauses
	 * in its translation.
	 */
	public void preset() {
		for (int w = 0; w < puzzle.length; w++) {
			for (int h = 0; h < puzzle[0].length; h++) {
				ArrayList<Integer> clz = new ArrayList<Integer>();
				// add the non-zero elements in sudoku to the SAT formula
				if (puzzle[w][h] != 0) {
					clz.add(xyzInt(w + 1, h + 1, puzzle[w][h]));
					formula.add(clz);
				}
			}
		}
	}

	/**
	 * Certifier for the sudoku solution. Caution: side effects this.puzzle!
	 */
	public Boolean certifier(int[] solution) {
		if (solution == null)
			return false;
		// First, change puzzle to solution.
		for (int pos = 0; pos < solution.length; pos++) {
			if (solution[pos] > 0) {
				String s = Integer.toString(solution[pos]);
				int x = Character.getNumericValue(s.charAt(0));
				int y = Character.getNumericValue(s.charAt(1));
				int z = Character.getNumericValue(s.charAt(2));

				// Set puzzle elements, IF they are settable.
				if (puzzle[x - 1][y - 1] == 0)
					puzzle[x - 1][y - 1] = z;
			}
		}

		// Ensure the solution is correct by checking that each digit
		// 1-9 appears in each row,col, and box exactly once.
		for (int num = 1; num <= 9; num++) {
			// For check: i is row,col,box.. j is 9 values for that row,col,box
			int[][] check = new int[boxLen][boxLen * boxLen];

			for (int row = 0; row < puzzle.length; row++) {
				for (int col = 0; col < puzzle[row].length; col++) {
					if (puzzle[row][col] == num) { // a match found
						check[0][row] += 1; // increment check of num in row-th row
						check[1][col] += 1; // increment check of num in h-th col.
						int x = row / 3;
						int y = col / 3;
						check[2][x+(3*y)] += 1;
					}
				}
			}
			// Check cells MUST all be 1.
			for (int o = 0; o < check.length; o++) {
				for (int p = 0; p < check[o].length; p++) {
					if (check[o][p] != 1) {
						return false;
					}
				}
			}

		}

		System.out.println("Solution:\n" + showPuzzle());
		return true;
	}

	/**
	 * Write SAT clauses to a file.
	 */
	public void writeSATfile(String fname) throws IOException {
		File file = new File(fname);
		file.createNewFile();
		FileWriter fw = new FileWriter(fname);
		BufferedWriter bwriter = new BufferedWriter(fw);
		// Header
		// 999 for all digits 1..9 for i,j, and k
		bwriter.write("p cnf " + " 999 " + formula.size() + "\n");

		for (int w = 0; w < formula.size(); w++) {
			for (int h = 0; h < formula.get(w).size(); h++) {
				bwriter.write(formula.get(w).get(h) + " ");
			}
			bwriter.write("0");
			bwriter.newLine();
		}

		bwriter.close();
	}

	/**
	 * Main
	 */
	public static void main(String[] args) throws ParseFormatException, ContradictionException, TimeoutException {

		long start = System.currentTimeMillis();
		SudokuSolver solver = new SudokuSolver(args[0]);
		System.out.println("\nSudoku problem:\n" + solver.showPuzzle());
		// Reduce Sudoku to SAT
		solver.constraint1();
		solver.constraint2();
		solver.constraint3();
		solver.constraint4();
		solver.preset();
		try {
			solver.writeSATfile("sudokuSAT.cnf");
		} catch (IOException ioex) {
			ioex.printStackTrace();
			System.err.println("Failed to create SAT DIMACS file.");
			System.exit(-1);
		}

		// solve the SAT formula using given file
		int[] assignment = null;
		try {
			assignment = SATSolver.solve("sudokuSAT.cnf");
		} catch (IOException fnfex) {
			fnfex.printStackTrace();
			System.exit(-1);
		}

		// Certify the SAT solution solves the sudoku problem
		Boolean valid = solver.certifier(assignment);
		System.out.println("Solution is " + (valid ? "valid" : "invalid"));

		long end = System.currentTimeMillis();
		System.out.println("Run Time = " + (end - start) + " msec\n");
	}

} // end of SudokuSolver class
