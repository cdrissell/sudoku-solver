#!/bin/bash

echo "" >> output.txt
echo "Test Problems for Sudoku Solver" >> output.txt
echo "" >> output.txt
echo "--------------------------" >> output.txt
echo "" >> output.txt

for filename in ./sudokuPuzzles/*; do
    echo "file: " $filename >> output.txt
    java -cp .:org.sat4j.core.jar SudokuSolver $filename >> output.txt
    echo "" >> output.txt
    echo "--------------------------" >> output.txt
    echo "" >> output.txt
done