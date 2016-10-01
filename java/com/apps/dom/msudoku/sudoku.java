package com.apps.dom.msudoku;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

/**
 * Created by User on 2016-08-14.
 */
public class sudoku {
    //Grid as it was started
    private int[][] originalGrid;
    //Current grid shown to the player
    private int[][] activeGrid;
    //Fully solved grid
    private int[][] solvedGrid;
    private int valuesFound;
    public boolean showSolution;
    private boolean solved;

    //Grid's large and small dimension
    private int dim;
    private int subDim;

    //Each position has a set that contains the remaining possible values
    Set<Integer>[][] possibilities;

    public boolean isPlayerSolved(){
        if (checkForX(solvedGrid,0)){
            superSolve();
            //basicSolve();
            solved = true;
        }
        for (int i = 0; i < dim*dim; ++i){
            if (solvedGrid[i/dim][i%dim] == 0 || solvedGrid[i/dim][i%dim] != activeGrid[i/dim][i%dim]){
                return false;
            }
        }
        return true;
    }

    public boolean checkActiveRuleBreak(int row, int col,int[][] grid){
        if (grid[row][col] != 0){
            int val = grid[row][col];
            for (int c = 0; c < dim; ++c){
                if (grid[row][c] == val && c != col)
                    return true;
            }
            for (int r = 0; r < dim; ++r){
                if (grid[r][col] == val && r != row)
                    return true;
            }
            for (int sqr = 0; sqr < subDim; ++sqr){
                for (int sqc = 0; sqc < subDim; ++sqc){
                    int r = subDim*(row/subDim) + sqr;
                    int c = subDim*(col/subDim) + sqc;
                    if (grid[r][c] == val && (r != row || col != col))
                        return true;
                }
            }
        }
        return false;
    }

    public boolean checkActiveRuleBreak(int row, int col){
        if (activeGrid[row][col] != 0 && originalGrid[row][col] == 0){
            int val = activeGrid[row][col];
            for (int c = 0; c < dim; ++c){
                if (activeGrid[row][c] == val && c != col)
                    return true;
            }
            for (int r = 0; r < dim; ++r){
                if (activeGrid[r][col] == val && r != row)
                    return true;
            }
            for (int sqr = 0; sqr < subDim; ++sqr){
                for (int sqc = 0; sqc < subDim; ++sqc){
                    int r = subDim*(row/subDim) + sqr;
                    int c = subDim*(col/subDim) + sqc;
                    if (activeGrid[r][c] == val && (r != row || col != col))
                        return true;
                }
            }
        }
        return false;
    }

    public void setActiveValue(int row, int col, int val){
        activeGrid[row][col] = val;
    }

    public boolean isSolved(){
        return solved;
    }

    //Gets the square corresponding to the coordinates
    private int square(int row,int col){
        if (row >= dim || col >= dim)
            return dim*dim;
        else
            return (subDim*(row/subDim) + col/subDim);
    }


    //Remove a possibility from all positions of a specified row with the possibility of exceptions
    private void removeRowPossibilities(int row, int val){
        if (row >= dim || val > dim)
            return;
        for (int i = 0; i < dim; ++i) {
            possibilities[row][i].remove(val);
        }
    }
    private void removeRowPossibilities(int row, int val, Set<Integer> exceptions){
        if (row >= dim || val > dim)
            return;
        for (int i = 0; i < dim; ++i) {
            if (!exceptions.contains(i))
                possibilities[row][i].remove(val);
        }
    }


    //Remove a possibility from all positions of a specified column with the possibility of exceptions
    void removeColumnPossibilities(int col, int val){
        if (col >= dim || val > dim)
            return;
        for (int i = 0; i < dim; ++i) {
            possibilities[i][col].remove(val);
        }
    }
    void removeColumnPossibilities(int col, int val, Set<Integer> exceptions){
        if (col >= dim || val > dim)
            return;
        for (int i = 0; i < dim; ++i) {
            if (!exceptions.contains(i))
                possibilities[i][col].remove(val);
        }
    }


    //Remove a possibility from all positions of a specified square with the possibility of exceptions
    private void removeSquarePossibilities( int square,  int val){
        if (square >= dim || val > dim)
            return;
        for (int i = 0; i < dim; ++i) {
            possibilities[i / subDim + subDim*(square / subDim)][(i%subDim) + subDim*(square % subDim)].remove(val);
        }
    }
    private void removeSquarePossibilities( int square,  int val, Set<Integer> exceptions){
        if (square >= dim || val > dim)
            return;
        for (int i = 0; i < dim; ++i) {
            if (!exceptions.contains(i))
                possibilities[i / subDim + subDim*(square / subDim)][(i%subDim) + subDim*(square % subDim)].remove(val);
        }
    }


    //Update all the sets from the values of the original grid
    private void updateBasicPossibilities(){
        for (int i = 0; i < dim; ++i) {
            for (int j = 0; j < dim; ++j) {
                if (solvedGrid[i][j] != 0) {
                    removeRowPossibilities(i, solvedGrid[i][j]);
                    removeColumnPossibilities(j, solvedGrid[i][j]);
                    removeSquarePossibilities(square(i,j), solvedGrid[i][j]);
                    possibilities[i][j].clear();
                }
            }
        }
    }



    //Checks for 2 numbers in a square/row/column that only have 2 identical possibilities, reducing the possibilities of other positions
    private void checkSquaresPairs(){
        for (int square = 0; square < dim; ++square) {
            //Create counts for the number of possible positions for each value in the square
            int[] counts = new int[dim + 1];
            for (int i = 0; i < dim + 1; ++i) {
                counts[i] = 0;
            }

            //Count the number of possible positions for each value in the square
            for (int i = 0; i < dim; ++i) {
                int tmpRow = i / subDim + subDim*(square / subDim);
                int tmpCol = i % subDim + subDim*(square % subDim);

                for (int it : possibilities[tmpRow][tmpCol]) {
                    ++counts[it];
                }

            }

            List<Integer> pairs = new ArrayList<Integer>(); //Set representing the values that only have 2 possibilities in the current square

            for (int i = 1; i <= dim; ++i) {
                if (counts[i] == 2) {
                    pairs.add(i);
                }
            }

            //Check if any pairs share positions, and if so remove all other possibilities from this position
            if (pairs.size() > 1) {
                for (int it1 : pairs) {
                    for (int it2 : pairs) {
                        if (it1 != it2) {
                            int found = 0;
                            Set<Integer> exceptions = new HashSet<Integer>();
                            int exception = -1;
                            for (int i = 0; i < dim; ++i) {
                                int tmpRow = i / subDim + subDim*(square / subDim);
                                int tmpCol = i % subDim + subDim*(square % subDim);
                                if (possibilities[tmpRow][tmpCol].contains(it1) && possibilities[tmpRow][tmpCol].contains(it2)) {
                                    if (found == 0) {
                                        ++found;
                                        exception = i;
                                    }
                                    else {
                                        exceptions.add(exception);
                                        exceptions.add(i);
                                        removeSquarePossibilities(square, it1, exceptions);
                                        removeSquarePossibilities(square, it2, exceptions);
                                        for (int a = 1; a <= dim; ++a) {
                                            if (a != it1 && a != it2) {
                                                possibilities[tmpRow][tmpCol].remove(a);
                                                possibilities[exception / subDim + subDim*(square / subDim)][exception % subDim + subDim*(square % subDim)].remove(a);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private void checkRowsPairs(){
        for (int row = 0; row < dim; ++row) {
            //Create counts for the number of possible positions for each value in the square
            int[] counts = new int[dim + 1];
            for (int i = 0; i < dim + 1; ++i) {
                counts[i] = 0;
            }

            //Count the number of possible positions for each value in the square
            for (int i = 0; i < dim; ++i) {
                for (int it : possibilities[row][i]) {
                    ++counts[it];
                }

            }

            Set<Integer> pairs = new HashSet<Integer>(); //Set of the values that only have a 2 possibilities

            for (int i = 1; i <= dim; ++i) {
                if (counts[i] == 2) {
                    pairs.add(i);
                }
            }

            //Check if any pairs share positions, and if so remove all other possibilities from this position
            if (pairs.size() > 1) {
                for (int it1 : pairs) {
                    for (int it2 : pairs) {
                        if (it1 != it2) {
                            int found = 0;
                            Set<Integer> exceptions = new HashSet<Integer>();
                            int exception = -1;
                            for (int i = 0; i < dim; ++i) {
                                if (possibilities[row][i].contains(it1) && possibilities[row][i].contains(it2)) {
                                    if (found == 0) {
                                        ++found;
                                        exception = i;
                                    }
                                    else {
                                        exceptions.add(exception);
                                        exceptions.add(i);
                                        removeRowPossibilities(row, it1, exceptions);
                                        removeRowPossibilities(row, it2, exceptions);
                                        for (int a = 1; a <= dim; ++a) {
                                            if (a != it1 && a != it2) {
                                                possibilities[row][i].remove(a);
                                                possibilities[row][exception].remove(a);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private void checkColumnsPairs(){
        for (int col = 0; col < dim; ++col) {
            //Create counts for the number of possible positions for each value in the square
            int[] counts = new int[dim + 1];
            for (int i = 0; i < dim + 1; ++i) {
                counts[i] = 0;
            }

            //Count the number of possible positions for each value in the square
            for (int i = 0; i < dim; ++i) {
                for (int it : possibilities[i][col]) {
                    ++counts[it];
                }

            }

            Set<Integer> pairs = new HashSet<Integer>(); //Set of the values that only have a 2 possibilities

            for (int i = 1; i <= dim; ++i) {
                if (counts[i] == 2) {
                    pairs.add(i);
                }
            }

            //Check if any pairs share positions, and if so remove all other possibilities from this position
            if (pairs.size() > 1) {
                for (int it1 : pairs) {
                    for (int it2 : pairs) {
                        if (it1 != it2) {
                            int found = 0;
                            Set<Integer> exceptions = new HashSet<Integer>();
                            int exception = -1;
                            for (int i = 0; i < dim; ++i) {
                                if (possibilities[i][col].contains(it1) && possibilities[i][col].contains(it2)) {
                                    if (found == 0) {
                                        ++found;
                                        exception = i;
                                    }
                                    else {
                                        exceptions.add(exception);
                                        exceptions.add(i);
                                        removeColumnPossibilities(col, it1, exceptions);
                                        removeColumnPossibilities(col, it2, exceptions);
                                        for (int a = 1; a <= dim; ++a) {
                                            if (a != it1 && a != it2) {
                                                possibilities[i][col].remove(a);
                                                possibilities[exception][col].remove(a);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    //Removes possibilities, updates the sets and prints the grid and step if needed following a new known value
    private void newValueChanges(int row, int col, int val){
        solvedGrid[row][col] = val;
        removeRowPossibilities(row, val);
        removeColumnPossibilities(col, val);
        removeSquarePossibilities(square(row, col), val);
        possibilities[row][col].clear();
    }

    //Initialisers of counting arrays used during solving
    private int[] newCount(){
        int[] counts = new int[dim + 1];
        for (int i = 0; i < dim + 1; ++i) {
            counts[i] = 0;
        }
        return counts;
    }
    private int[] newSquareCount(int square){
        int[] counts = newCount();
        for (int i = 0; i < dim; ++i) {
            int tmpRow = i / subDim + subDim*(square / subDim);
            int tmpCol = i % subDim + subDim*(square % subDim);
            for (int it : possibilities[tmpRow][tmpCol]) {
                ++counts[it];
            }
        }
        return counts;
    }
    private int[] newRowCount(int row){
        int[] counts = newCount();
        for (int i = 0; i < dim; ++i) {
            for (int it : possibilities[row][i]) {
                ++counts[it];
            }
        }
        return counts;
    }
    private int[] newColumnCount(int col){
        int[] counts = newCount();
        for (int i = 0; i < dim; ++i) {
            for (int it : possibilities[i][col]) {
                ++counts[it];
            }
        }
        return counts;
    }

    private boolean hasSinglePossiblity(int row, int col){
        return possibilities[row][col].size() == 1;
    }

    //Functions that check for positions with only one possibility
    //Checks if a coordinate has only 1 possibility...
    private void checkSinglePossibilities(){
        for (int row = 0; row < dim; ++row) {
            for (int col = 0; col < dim; ++col) {
                if (hasSinglePossiblity(row,col)) {
                    int val = possibilities[row][col].iterator().next();
                    newValueChanges(row, col, val);
                    ++valuesFound;
                }
            }
        }
    }

    //Check if a value can only exist in 1 position of a row
    private void checkSinglePossibilityRows(){
        for (int row = 0; row < dim; ++row) {
            //Create counts for the number of possible positions for each value in the row
            int[] counts = newRowCount(row);
            for (int val = 1; val <= dim; ++val) {
                if (counts[val] == 1) {
                    for (int col = 0; col < dim; ++col) {
                        if (possibilities[row][col].contains(val)) {
                            newValueChanges(row, col, val);
                            ++valuesFound;
                            break;
                        }
                    }
                }
            }
        }
    }

    //Check if a value can only exist in 1 position of a column
    private void checkSinglePossibilityColumns(){
        for (int col = 0; col < dim; ++col) {
            //Create counts for the number of possible positions for each value in the column
            int[] counts = newColumnCount(col);
            for (int val = 1; val <= dim; ++val) {
                if (counts[val] == 1) {
                    for (int row = 0; row < dim; ++row) {
                        if (possibilities[row][col].contains(val)) {
                            newValueChanges(row, col, val);
                            ++valuesFound;
                            break;
                        }
                    }
                }
            }
        }
    }


    //Check if a value can only exist in 1 position of a square
    private void checkSinglePossibilitySquares(){
        for (int square = 0; square < dim; ++square) {
            //Create counts for the number of possible positions for each value in the square
            int[] counts = newSquareCount(square);
            for (int val = 1; val <= dim; ++val) {
                if (counts[val] == 1) {
                    for (int i = 0; i < dim; ++i) {
                        int row = i / subDim + subDim*(square / subDim);
                        int col = i % subDim + subDim*(square % subDim);
                        if (possibilities[row][col].contains(val)) {
                            newValueChanges(row, col, val);
                            ++valuesFound;
                            break;
                        }
                    }
                }
            }
        }
    }
    //Checks if a grid contains x in any of its cells
    public boolean checkForX(int[][] grid,int x){
        for ( int i = 0; i < dim; ++i) {
            for (int j = 0; j < dim; ++j) {
                if (grid[i][j] == x){
                    return true;
                }
            }
        }
        return false;
    }

    //Fills the possibilities with all values from 1 to 9 and assigns the values of the original grid to the active and solved grid
    public void initPossibilities(int[][] grid){
        possibilities = new HashSet[dim][dim];
        for ( int i = 0; i < dim; ++i) {
            for (int j = 0; j < dim; ++j) {
                possibilities[i][j] = new HashSet<Integer>();
                if (grid[i][j] == 0) {
                    for (int k = 1; k <= dim; ++k) {
                        possibilities[i][j].add(new Integer(k));
                    }
                }
            }
        }
    }

    //Fills the possibilities with all values from 1 to 9 and assigns the values of the original grid to the active and solved grid
    public void initPossibilitiesAndGrids(){
        activeGrid = new int[dim][dim];
        solvedGrid = new int[dim][dim];
        possibilities = new HashSet[dim][dim];
        for ( int i = 0; i < dim; ++i) {
            for (int j = 0; j < dim; ++j) {
                activeGrid[i][j] = originalGrid[i][j];
                solvedGrid[i][j] = originalGrid[i][j];
                possibilities[i][j] = new HashSet<Integer>();
                if (originalGrid[i][j] == 0) {
                    for (int k = 0; k < dim; ++k) {
                        possibilities[i][j].add(new Integer(k+1));
                    }
                }
            }
        }
    }

    //Checks if insterting a value in a cell respects the square rule
    public boolean isLegalInSquare(int square, int val){
        for (int i = 0; i < dim; ++i) {
            if (solvedGrid[i / subDim + subDim*(square / subDim)][(i%subDim) + subDim*(square % subDim)] == val){
                return false;
            }
        }
        return true;
    }

    //Checks if insterting a value in a cell respects the column rule
    public boolean isLegalInCol(int col, int val){
        for (int i = 0; i < dim; ++i){
            if (solvedGrid[i][col] == val){
                return false;
            }
        }
        return true;
    }

    //Checks if insterting a value in a cell respects the row rule
    public boolean isLegalInRow(int row, int val){
        for (int i = 0; i < dim; ++i){
            if (solvedGrid[row][i] == val){
                return false;
            }
        }
        return true;
    }

    //Checks if inserting a value in a cell is respecting the 3 rules
    public boolean isLegal(int row, int col, int val){
        return (isLegalInRow(row,val)&&isLegalInCol(col,val)&&isLegalInSquare(square(row,col),val));
    }

    //Restores the possibility following the removal of a value
    public void restoreValue(int row,int col,int val){
        for (int i = 0; i < dim; ++i) {
            possibilities[row][i].add(val);
        }
        for (int i = 0; i < dim; ++i) {
            possibilities[i][col].add(val);
        }
        for (int i = 0; i < dim; ++i) {
            possibilities[i / subDim + subDim*(square(row,col) / subDim)][(i%subDim) + subDim*(square(row,col) % subDim)].add(val);
        }

    }

    //Checks the number of possibilities a cell can have after checking basic possibilities and pairs
    public int checkPossibilities(int row, int col){
        updateBasicPossibilities();
        checkColumnsPairs();
        checkRowsPairs();
        checkSquaresPairs();
        return possibilities[row][col].size();
    }


    //Counts the number of non 0s in the grid
    public int countNot0(int grid[][]){
        int count = 0;
        for (int i = 0; i < dim; ++i){
            for (int j = 0; j < dim; ++j){
                if (grid[i][j] != 0)
                    ++count;
            }
        }
        return count;
    }


    //generates a filled valid sudoku grid...
    private int[][] generateFullGrid(){
        int[][] grid = new int[dim][dim];
        List<Integer>[][] potentials = new ArrayList[dim][dim];
        //Generate a full valid grid
        for ( int i = 0; i < dim; ++i) {
            for (int j = 0; j < dim; ++j) {
                potentials[i][j] = new ArrayList<Integer>();
                for (int k = 1; k <= dim; ++k){
                    potentials[i][j].add(k);
                }
            }
        }
        Random r = new Random();
        int rind = 0;//Random index value
        for (int i = 0; i < dim*dim;){
            int nbrLeft = potentials[i/dim][i%dim].size();
            //Checks if all values have been tried for this cell...
            if (nbrLeft != 0){
                rind = r.nextInt(nbrLeft);
                int newVal = potentials[i/dim][i%dim].get(rind);
                grid[i/dim][i%dim] = newVal;
                //Checks if the tried value is acceptable...
                if (!checkActiveRuleBreak(i/dim,i%dim,grid)){
                    grid[i/dim][i%dim] = newVal;
                    potentials[i/dim][i%dim].remove(new Integer(newVal));
                    ++i;
                }
                else{
                    potentials[i/dim][i%dim].remove(new Integer(newVal));
                }

            }
            else{
                grid[i/dim][i%dim] = 0;
                for (int k = 1; k <= dim; ++k){
                    potentials[i/dim][i%dim].add(k);
                }
                --i;
            }
        }
        return grid;
    }



    public int[][] generateHardGrid(int n, int[][] fullGrid){
        solvedGrid = fullGrid;
        int[][] full = new int[dim][dim];
        int[][] save = new int[dim][dim];
        Random r = new Random();
        Stack<Integer> removedIndexes = new Stack<Integer>();
        Stack<ArrayList<Integer>> potentials = new Stack<>();
        potentials.push(new ArrayList<Integer>());
        int count = 0;

        for ( int i = 0; i < dim; ++i) {
            for (int j = 0; j < dim; ++j) {
                save[i][j] = solvedGrid[i][j];
                full[i][j] = solvedGrid[i][j];
                potentials.peek().add(i*dim+j);
            }
        }
        int[] remaining = new int[]{9,9,9,9,9,9,9,9,9};
        int empty = 0;
        //Remove values until a valid puzzle is created...
        for  (int i = 0; i < dim*dim-n;){
            //Check if any index can be removed...
            if (potentials.peek().size() == 0){
                //Backtracking...
                int restoredIndex = removedIndexes.pop();//peek();
                int restoredValue = full[restoredIndex/dim][restoredIndex%dim];
                save[restoredIndex/dim][restoredIndex%dim] = restoredValue;
                solvedGrid[restoredIndex/dim][restoredIndex%dim] = restoredValue;
                ++remaining[restoredValue-1];
                --i;
                potentials.pop();
            }
            else{
                //Chooses the next index to try to remove...
                int indexToRemove = 0;
                //Checks that the index will not "extinct" a second variable...
                boolean approved = false;
                while (!approved){
                    if (potentials.peek().size() != 0)
                        indexToRemove = potentials.peek().get(r.nextInt(potentials.peek().size()));
                    else
                        break;
                    if (solvedGrid[indexToRemove/dim][indexToRemove%dim] == 0){
                        //Shouldn't happen...
                        potentials.peek().remove(new Integer(indexToRemove));
                    }
                    else{
                        if (remaining[solvedGrid[indexToRemove/dim][indexToRemove%dim]-1] == 1 && empty > 0){
                            potentials.peek().remove(new Integer(indexToRemove));
                        }
                        else{
                            potentials.peek().remove(new Integer(indexToRemove));
                            approved = true;
                        }
                    }
                }
                if (!approved)
                    continue;
                //Removes the value and prepares the partial solve...
                int removedVal = solvedGrid[indexToRemove/dim][indexToRemove%dim];
                solvedGrid[indexToRemove/dim][indexToRemove%dim] = 0;
                initPossibilities(solvedGrid);
                updateBasicPossibilities();
                hard = 0;
                //Checks if it is possible to solve for the removed value...
                if (solveForIndex(indexToRemove,removedVal)){
                    //Removes the value from the grid and updates the remaining values counts...

                    removedIndexes.push(indexToRemove);
                    potentials.peek().remove(new Integer(indexToRemove));
                    remaining[save[indexToRemove/dim][indexToRemove%dim]-1]--;
                    if (remaining[save[indexToRemove/dim][indexToRemove%dim]-1] == 0)
                        ++empty;
                    save[indexToRemove/dim][indexToRemove%dim] = 0;
                    ++i;
                    //Creates a new list of potentials...
                    ArrayList<Integer> list = new ArrayList<>();
                    for (int k = 0; k < potentials.peek().size(); ++k){
                        list.add(potentials.peek().get(k));
                    }
                    potentials.push(list);
                }
                else{
                    potentials.peek().remove(new Integer(indexToRemove));
                }
            }

            for ( int row = 0; row < dim; ++row) {
                for (int col = 0; col < dim; ++col) {
                    solvedGrid[row][col] = save[row][col];
                }
            }
            if (i == dim*dim-n){
                hard = 0;
                initPossibilities(solvedGrid);
                updateBasicPossibilities();
                superSolve();

                if (hard < 1 ){//&& count < 10){
                    ++count;
                    if (count > dim*dim-n)
                        count = dim*dim-n;
                    for (int k = 0; k < count; ++k){
                        potentials.pop();
                        int restoredIndex = removedIndexes.pop();//peek();
                        int restoredValue = full[restoredIndex/dim][restoredIndex%dim];
                        save[restoredIndex/dim][restoredIndex%dim] = restoredValue;
                        potentials.peek().remove(new Integer(restoredIndex));
                        solvedGrid[restoredIndex/dim][restoredIndex%dim] = restoredValue;
                        ++remaining[restoredValue-1];
                        --i;
                    }


                }
                for ( int row = 0; row < dim; ++row) {
                    for (int col = 0; col < dim; ++col) {
                        solvedGrid[row][col] = save[row][col];
                    }
                }
            }
        }
        //superSolve();
        /*
        if (hard < 1){
            hard = 0;
            count++;
            return generateHardGrid(n);//,full);
        }
        */
        count = 0;
        return save;
    }

    //other try...
    public int[][] generateHardGrid(int n){
        return generateHardGrid(n,generateFullGrid());
    }

    //Generate a sudoku grid with n starting values
    public int[][] generateEasyGrid(int n){
        solvedGrid = new int[9][9];
        for ( int i = 0; i < dim; ++i) {
            for (int j = 0; j < dim; ++j) {
                solvedGrid[i][j] = 0;
            }
        }
        int foundValues = 0;
        List<Integer>[][] potentials = new ArrayList[dim][dim];
        possibilities = new HashSet[dim][dim];
        //Generate a full valid grid
        for ( int i = 0; i < dim; ++i) {
            for (int j = 0; j < dim; ++j) {
                potentials[i][j] = new ArrayList<Integer>();
                possibilities[i][j] = new HashSet<Integer>();
                for (int k = 1; k <= dim; ++k){
                    potentials[i][j].add(k);
                    possibilities[i][j].add(new Integer(k));
                }
            }
        }
        Random r = new Random();
        int rind = 0;//Random index value
        for (int i = 0; i < dim*dim;){

            int nbrLeft = potentials[i/dim][i%dim].size();
            if (nbrLeft != 0){
                rind = r.nextInt(nbrLeft);
                int newVal = potentials[i/dim][i%dim].get(rind);
                if (isLegal(i/dim,i%dim,newVal)){
                    newValueChanges(i/dim,i%dim,newVal);
                    potentials[i/dim][i%dim].remove(new Integer(newVal));
                    ++i;
                }
                else{
                    potentials[i/dim][i%dim].remove(new Integer(newVal));
                }

            }
            else{
                restoreValue(i/dim,i%dim,solvedGrid[i/dim][i%dim]);
                solvedGrid[i/dim][i%dim] = 0;
                for (int k = 1; k <= dim; ++k){
                    potentials[i/dim][i%dim].add(k);
                    possibilities[i/dim][i%dim].add(new Integer(k));
                }
                --i;
            }
        }
        int[][] full = new int[dim][dim];
        int[][] save = new int[dim][dim];
        for ( int i = 0; i < dim; ++i) {
            for (int j = 0; j < dim; ++j) {
                possibilities[i][j].clear();
                save[i][j] = solvedGrid[i][j];
                full[i][j] = solvedGrid[i][j];
            }
        }

        Set<Integer> notWorking = new HashSet<Integer>();

        int[] remaining = new int[]{9,9,9,9,9,9,9,9,9};
        int empty = 0;

        while (countNot0(save) != n){
            List<Integer> validInd = new ArrayList<Integer>();
            for (int i = 0; i < dim*dim; ++i){
                if (solvedGrid[i/dim][i%dim] != 0 && !notWorking.contains(i)){
                    validInd.add(i);
                }
            }
            if (validInd.size() > 0){
                rind = r.nextInt(validInd.size());
                int index = validInd.get(rind);
                while (remaining[solvedGrid[index/dim][index%dim]-1] == 1 && empty > 0){
                    rind = r.nextInt(validInd.size());
                    index = validInd.get(rind);
                }
                int removedVal = solvedGrid[index/dim][index%dim];
                solvedGrid[index/dim][index%dim] = 0;
                validInd.clear();

                int[][] tmp = new int[dim][dim];
                for ( int i = 0; i < dim; ++i) {
                    for (int j = 0; j < dim; ++j) {
                        tmp[i][j] = solvedGrid[i][j];
                    }
                }
                initPossibilities(solvedGrid);
                updateBasicPossibilities();
                superSolve();
                if (checkForX(solvedGrid,0)){
                    notWorking.add(index);
                    solvedGrid[index/dim][index%dim] = removedVal;
                    for ( int i = 0; i < dim; ++i) {
                        for (int j = 0; j < dim; ++j) {
                            possibilities[i][j].clear();
                            solvedGrid[i][j] = save[i][j];
                        }
                    }
                }
                else{
                    notWorking.clear();
                    remaining[removedVal-1]--;
                    if (remaining[removedVal-1] == 0)
                        empty++;
                    for ( int i = 0; i < dim; ++i) {
                        for (int j = 0; j < dim; ++j) {
                            save[i][j] = tmp[i][j];
                            solvedGrid[i][j] = tmp[i][j];
                        }
                    }
                }
            }
            else{
                hard = 0;
                return generateEasyGrid(n);
            }
        }
        if (n<=25 && hard < 2){
            hard = 0;
            return generateEasyGrid(n);
        }
        return save;
    }

    //Sets the original grid to the active grid following a custom sudoku
    public void applyCustomGrid(){
        for (int r = 0; r < dim; ++r){
            for (int c = 0 ; c < dim; ++c){
                originalGrid[r][c] = activeGrid[r][c];
            }
        }
        initPossibilitiesAndGrids();
        updateBasicPossibilities();
    }
    //Constructor to generate a sudoku object with n starting clues
    public sudoku(int n){
        showSolution = false;
        dim = 9;
        subDim = 3;
        if (n != -1){
            if (n <= 30)
                originalGrid = generateHardGrid(n);
            else
                originalGrid = generateEasyGrid(n);
            initPossibilitiesAndGrids();
            updateBasicPossibilities();
        }
        else {
            originalGrid = new int[dim][dim];
            for (int r = 0; r < dim; ++r){
                for (int c = 0; c < dim; ++c){
                    originalGrid[r][c] = -1;
                }
            }
            initPossibilitiesAndGrids();
        }
    }

    //Creates a sudoku object from a given grid
    public sudoku(int[][] grid){
        showSolution = false;
        originalGrid = grid;
        dim = 9;
        subDim = 3;
        initPossibilitiesAndGrids();
        updateBasicPossibilities();
    }

    //Default constructor
    public sudoku(){
        showSolution = false;
        originalGrid = new int[][]{
                {0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0}
        };
        dim = 9;
        subDim = 3;
        initPossibilitiesAndGrids();
        updateBasicPossibilities();
    }

    //Return a value from the grid currently shown to the player
    public int getActiveVal(int row, int col){
        return activeGrid[row][col];
    }

    //Return a value from the starting grid
    public int getOriginalVal(int row, int col){
        return originalGrid[row][col];
    }

    //Counts the number of possibilities of a given value exist in a row...
    private int countValInRow(int row, int val){
        int count = 0;
        for (int c = 0; c < dim; ++c){
            if (possibilities[row][c].contains(val)){
                ++count;
            }
        }
        return count;
    }


    //Counts the number of possibilities of a given value exist in a row...
    private int countValInCol(int col, int val){
        int count = 0;
        for (int r = 0; r < dim; ++r){
            if (possibilities[r][col].contains(val)){
                ++count;
            }
        }
        return count;
    }

    //Updates the sets according to the X-Wing pattern...
    private void XWingUpdate(int r1, int r2, int c1, int c2, int val){
        for (int r = 0; r < dim; ++r){
            if (r != r1 && r != r2){
                possibilities[r][c1].remove(val);
                possibilities[r][c2].remove(val);
            }
        }
        for (int c = 0; c < dim; ++c){
            if (c != c1 && c != c2){
                possibilities[r1][c].remove(val);
                possibilities[r2][c].remove(val);
            }
        }
    }

    //Updates the sets according to the Swordfish pattern...
    private void swordFishUpdate(int r1, int r2,int r3, int c1, int c2,int c3, int val){
        for (int r = 0; r < dim; ++r){
            if (r != r1 && r != r2 && r != r3){
                possibilities[r][c1].remove(val);
                possibilities[r][c2].remove(val);
                possibilities[r][c3].remove(val);
            }
        }
        for (int c = 0; c < dim; ++c){
            if (c != c1 && c != c2 && c != c3){
                possibilities[r1][c].remove(val);
                possibilities[r2][c].remove(val);
                possibilities[r3][c].remove(val);
            }
        }
    }

    //Checks for a swordfish pattern...
    public void findSwordfish(){
        for (int val = 1; val < dim+1; ++val){
            ArrayList<Integer> colList = new ArrayList<Integer>();
            ArrayList<Integer> rowList = new ArrayList<Integer>();
            for (int r = 0; r < dim; ++r){
                if (countValInRow(r,val) == 3){
                    rowList.add(r);
                }
            }
            for (int i = 0; i < rowList.size()-2; ++i){
                for (int j = i+1; j < rowList.size()-1; ++j) {
                    for (int k = j+1; k < rowList.size(); ++k) {
                        int r1 = rowList.get(i);
                        int r2 = rowList.get(j);
                        int r3 = rowList.get(k);
                        int found = 0;
                        int col1 = -1;
                        int col2 = -1;
                        for (int col = 0; col < dim; ++col) {
                            if (possibilities[r1][col].contains(val) && possibilities[r2][col].contains(val) && possibilities[r3][col].contains(val)) {
                                if (found == 0) {
                                    ++found;
                                    col1 = col;
                                } else if (found == 1) {
                                    ++found;
                                    col2 = col;
                                } else {
                                    swordFishUpdate(r1,r2,r3,col1,col2,col,val);
                                    ++hard;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            for (int c = 0; c < dim; ++c){
                if (countValInCol(c,val) == 2){
                    colList.add(c);
                }
            }
            for (int i = 0; i < colList.size()-2; ++i){
                for (int j = i+1; j < colList.size()-1; ++j) {
                    for (int k = j+1; k < colList.size(); ++k) {
                        int c1 = colList.get(i);
                        int c2 = colList.get(j);
                        int c3 = colList.get(k);
                        int found = 0;
                        int row1 = -1;
                        int row2 = -1;
                        for (int row = 0; row < dim; ++row) {
                            if (possibilities[row][c1].contains(val) && possibilities[row][c2].contains(val) && possibilities[row][c2].contains(val)) {
                                if (found == 0) {
                                    ++found;
                                    row1 = row;
                                } else if (found == 1) {
                                    ++found;
                                    row2 = row;
                                } else {
                                    swordFishUpdate(row1,row2,row,c1,c2,c3,val);
                                    ++hard;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    //Checks for an X-Wing pattern...
    public void findXWings(){
        for (int val = 1; val < dim+1; ++val){
            ArrayList<Integer> colList = new ArrayList<Integer>();
            ArrayList<Integer> rowList = new ArrayList<Integer>();
            for (int r = 0; r < dim; ++r){
                if (countValInRow(r,val) == 2){
                    rowList.add(r);
                }
            }
            for (int i = 0; i < rowList.size()-1; ++i){
                for (int j = i+1; j < rowList.size(); ++j) {
                    int r1 = rowList.get(i);
                    int r2 = rowList.get(j);
                    int found = 0;
                    int col1 = -1;
                    for (int col = 0; col < dim; ++col) {
                        if (possibilities[r1][col].contains(val) && possibilities[r2][col].contains(val)) {
                            if (found == 0){
                                ++found;
                                col1 = col;
                            }
                            else{
                                XWingUpdate(r1,r2,col1,col,val);
                                ++hard;
                                break;
                            }
                        }
                    }
                }
            }
            for (int c = 0; c < dim; ++c){
                if (countValInCol(c,val) == 2){
                    colList.add(c);
                }
            }
            for (int i = 0; i < colList.size()-1; ++i){
                for (int j = i+1; j < colList.size(); ++j) {
                    int c1 = colList.get(i);
                    int c2 = colList.get(j);
                    int found = 0;
                    int row1 = -1;
                    for (int row = 0; row < dim; ++row) {
                        if (possibilities[row][c1].contains(val) && possibilities[row][c2].contains(val)) {
                            if (found == 0){
                                ++found;
                                row1 = row;
                            }
                            else{
                                XWingUpdate(row1,row,c1,c2,val);
                                ++hard;
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private int hard;

    //Solve using brute force, will produce a result if one exists
    public void bruteSolve(){
        List<Integer>[][] potentials = new ArrayList[dim][dim];
        //Puts all possibilities for each cell...
        for ( int i = 0; i < dim; ++i) {
            for (int j = 0; j < dim; ++j) {
                potentials[i][j] = new ArrayList<Integer>();
                if (originalGrid[i][j] == 0) {
                    for (int k = 1; k <= dim; ++k) {
                        potentials[i][j].add(k);
                    }
                }
                else{
                    solvedGrid[i][j] = originalGrid[i][j];
                }
            }
        }
        boolean backtracking = false;
        //Brute-forcing for a solution...
        for (int i = 0; i < dim*dim;){
            //Checks if the value is known from the original grid...
            if (originalGrid[i/dim][i%dim] == 0){
                if (backtracking){
                    int val = solvedGrid[i/dim][i%dim];
                    solvedGrid[i/dim][i%dim] = 0;
                    potentials[i/dim][i%dim].remove(new Integer(val));
                }
                //Checks if all values have been tried for this cell...
                if (potentials[i/dim][i%dim].size() > 0){
                    int val = potentials[i/dim][i%dim].get(0);
                    solvedGrid[i/dim][i%dim] = val;
                    //Checks if the tried value is acceptable...
                    if (!checkActiveRuleBreak(i/dim,i%dim,solvedGrid)){
                        backtracking = false;
                        ++i;
                    }
                    else{
                        potentials[i/dim][i%dim].remove(new Integer(val));
                        solvedGrid[i/dim][i%dim] = 0;
                    }
                }
                else{
                    for (int k = 1; k <= dim; ++k) {
                        potentials[i/dim][i%dim].add(k);
                    }
                    solvedGrid[i/dim][i%dim] = 0;
                    backtracking = true;
                    --i;
                }
            }
            else{
                i = (backtracking)? --i:++i;
            }
        }
        solved = true;
    }

    //Solve until a specific index is found...
    public boolean solveForIndex(int index, int correctVal){
        valuesFound = 0;
        int lvl = 0;
        while(solvedGrid[index/dim][index%9] != correctVal){
            int startFound = -1;
            while (startFound != valuesFound){
                startFound = valuesFound;
                checkSinglePossibilities();
                checkSinglePossibilityRows();
                checkSinglePossibilitySquares();
                checkSinglePossibilityColumns();
                if (startFound == valuesFound)
                    ++lvl;
                else
                    lvl = 0;
            }
            if (!checkForX(solvedGrid,0)){
                return (solvedGrid[index/dim][index%9] == correctVal);
            }
            if (lvl >= 1){
                checkSquaresPairs();
                checkRowsPairs();
                checkColumnsPairs();
            }
            if (lvl >= 2){
                findXWings();
                findSwordfish();
            }
            if (lvl >= 3){
                solved = false;
                return (solvedGrid[index/dim][index%9] == correctVal);
            }
        }
        return (solvedGrid[index/dim][index%9] == correctVal);
    }

    //Solve with higher level strategies...
    public void superSolve(){
        valuesFound = 0;
        int lvl = 0;
        while(checkForX(solvedGrid,0)){
            int startFound = -1;
            while (startFound != valuesFound){
                startFound = valuesFound;
                checkSinglePossibilities();
                checkSinglePossibilityRows();
                checkSinglePossibilitySquares();
                checkSinglePossibilityColumns();
                if (startFound == valuesFound)
                    ++lvl;
                else
                    lvl = 0;
            }
            if (!checkForX(solvedGrid,0)){
                solved = true;
                return;
            }
            if (lvl >= 1){
                checkSquaresPairs();
                checkRowsPairs();
                checkColumnsPairs();
            }
            if (lvl >= 2){
                findXWings();
                findSwordfish();
            }
            if (lvl >= 3){
                solved = false;
                hard = 0;
                return;
            }
        }
    }

    //Fully solves the sudoku and keeps it in "solvedGrid"
    public void basicSolve(){
        valuesFound = 0;
        boolean justCheckedPairs = false;
        //Loops while at least one value has been discovered in the grid
        while (true) {
            //int valuesFound = 0;
            checkSinglePossibilities();
            checkSinglePossibilityRows();
            checkSinglePossibilitySquares();
            checkSinglePossibilityColumns();


            //Checks if any new values were found in this cycle

            if (valuesFound == 0) {
                if (!justCheckedPairs) {
                    checkSquaresPairs();
                    checkRowsPairs();
                    checkColumnsPairs();
                    justCheckedPairs = true;
                }
                else{

                    solved = !checkForX(solvedGrid,0);
                    return;
                }
            }
            else {
                valuesFound = 0;
                justCheckedPairs = false;
            }
        }

    }
    public int getDimension(){return dim;}

    //Check if a value was found by the player/solver or was in the starting sudoku
    public boolean isNewValue(int row, int col){
        return (originalGrid[row][col] != solvedGrid[row][col]);
    }

    //Get the solved value of a cell
    public int getSolvedValue(int row, int col){
        return solvedGrid[row][col];
    }
    //Verifies if a square/row/column is valid
    public boolean verifySquare( int square){
        if (square >= dim)
            return false;
        int[] counts = new int[dim+1];
        for (int i = 0; i <= dim; ++i) {
            counts[i] = 0;
        }
        for (int i = 0; i < dim; ++i) {
            int val = solvedGrid[i / subDim + subDim*(square / subDim)][(i%subDim) + subDim*(square % subDim)];
            if (val <= dim)
                ++counts[val];
            else {
                return false;
            }
        }
        for (int i = 1; i <= dim; ++i) {
            if (counts[i] > 1) {
                return false;
            }
        }
        return true;
    }
    public boolean verifyRow( int row){
        if (row >= dim)
            return false;
        int[] counts = new int[dim + 1];
        for (int i = 0; i < dim + 1; ++i) {
            counts[i] = 0;
        }
        for (int i = 0; i < dim; ++i) {
            int val = solvedGrid[row][i];
            if (val <= dim)
                ++counts[val];
            else {
                return false;
            }
        }
        for (int i = 1; i <= dim; ++i) {
            if (counts[i] > 1) {
                return false;
            }
        }
        return true;
    }
    public boolean verifyColumn( int col){
        if (col >= dim)
            return false;
        int[] counts = new int[dim + 1];
        for (int i = 0; i <= dim; ++i) {
            counts[i] = 0;
        }
        for (int i = 0; i < dim; ++i) {
            int val = solvedGrid[i][col];
            if (val <= dim)
                ++counts[val];
            else {
                return false;
            }
        }
        for (int i = 1; i <= dim; ++i) {
            if (counts[i] > 1) {
                return false;
            }
        }
        return true;
    }


    //Verifies every square/row/column and prints the invalid ones
    public boolean checkGrid(){
        int errors = 0;
        for (int i = 0; i < dim; ++i) {
            if (!verifySquare(i)) {
                return false;
            }
        }
        for (int i = 0; i < dim; ++i) {
            if (!verifyColumn(i)) {
                return false;
            }
        }
        for (int i = 0; i < dim; ++i) {
            if (!verifyRow(i)) {
                return false;
            }
        }
        if (errors > 0) {
            return false;
        }
        return true;
    }
}
