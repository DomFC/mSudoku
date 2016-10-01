package com.apps.dom.msudoku;


import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class MainActivity extends AppCompatActivity {

    private sudoku currentSudoku;
    private GridLayout gridLayout;
    private int selected;
    private SeekBar cluesBar;
    private int startingClues;
    private Button[] buttons;
    private boolean customizing;
    private boolean isInit;
    private boolean[][] mistakes;
    public static Activity activity;
    private int textSize;


    ViewFlipper vf;
    View menuLayout;
    View gameLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isInit = false;
        startingClues = 31;
        customizing = false;
        setContentView(R.layout.activity_main);
        vf = (ViewFlipper) findViewById( R.id.viewFlipper );
        menuLayout = vf.findViewById(R.id.menu_layout);
        gameLayout = vf.findViewById(R.id.game_layout);
        gridLayout = (GridLayout)findViewById(R.id.grid);

        selected = -1;
        cluesBar = ((SeekBar) findViewById(R.id.seekBar));//.setMax(10);
        cluesBar.setMax(56);
        cluesBar.setProgress(6);

        cluesBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        startingClues = 25 + progress;
                        String clues = "CLUES : ";
                        clues = clues.concat(Integer.toString(startingClues));
                        ((TextView) findViewById(R.id.clues)).setText(clues);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                }
        );
        findViewById(R.id.resume_button).setClickable(false);
        ((Button)findViewById(R.id.resume_button)).setTextColor(0xff999999);
        vf.showNext();
        textSize = 0;

    }

    public void resumeButtonClick(View v){
        vf.showNext();
    }


    private void newSelection(int newS) {
        if (selected != -1) {
            (gridLayout.getChildAt(selected)).setBackgroundColor(Color.TRANSPARENT);
            if (mistakes != null) {
                if (mistakes[selected / 9][selected % 9]) {
                    (gridLayout.getChildAt(selected)).setBackgroundColor(0x66ff0000);
                }
            }
        }
        if (newS != -1)
            gridLayout.getChildAt(newS).setBackgroundResource(R.drawable.select);
        selected = newS;
    }

    private void initGridLayout() {
        int side = findViewById(R.id.new_game_button).getWidth()/9;// gridLayout.getWidth()/9;
        for (int i = 0; i < 9; ++i) {
            for (int j = 0; j < 9; ++j) {
                GridLayout.Spec row = GridLayout.spec(i);
                GridLayout.Spec col = GridLayout.spec(j);
                TextView cell = new TextView(getApplicationContext());
                cell.setId(i*9+j);
                cell.setTextColor(0xff000000);
                cell.setText("");
                cell.setWidth(side);
                cell.setHeight(side);
                cell.setTextSize(25);
                cell.setGravity(0x11);
                cell.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        if (currentSudoku != null) {
                            if (!currentSudoku.showSolution && currentSudoku.getOriginalVal(view.getId() / 9, view.getId() % 9) == 0) {
                                newSelection(view.getId());
                            } else
                                newSelection(-1);
                        }
                    }
                });
                gridLayout.addView(cell, new GridLayout.LayoutParams(row,col));//.addView(cell, new GridLayout.LayoutParams(row, col));
            }
        }
        buttons = new Button[10];
        buttons[0] = (Button) findViewById(R.id.but0);
        buttons[1] = (Button) findViewById(R.id.but1);
        buttons[2] = (Button) findViewById(R.id.but2);
        buttons[3] = (Button) findViewById(R.id.but3);
        buttons[4] = (Button) findViewById(R.id.but4);
        buttons[5] = (Button) findViewById(R.id.but5);
        buttons[6] = (Button) findViewById(R.id.but6);
        buttons[7] = (Button) findViewById(R.id.but7);
        buttons[8] = (Button) findViewById(R.id.but8);
        buttons[9] = (Button) findViewById(R.id.but9);
    }


    Thread th;

    public void newSudokuButtonClick(View v) {

        findViewById(R.id.resume_button).setClickable(true);
        ((Button)findViewById(R.id.resume_button)).setTextColor(0xff000000);
        final ProgressDialog pd = ProgressDialog.show(this, "", "Generating your Sudoku...", true);

        th = new Thread(new Runnable() {
            public void run() {
                currentSudoku = new sudoku(startingClues);
                pd.dismiss();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        vf.showNext();
                        if (!isInit) {
                            initGridLayout();
                            isInit = true;
                        }
                        newSelection(-1);
                        for (int i = 0; i < 9 * 9; ++i) {
                            int val = currentSudoku.getActiveVal(i / 9, i % 9);

                            TextView cell = (TextView)gridLayout.getChildAt(i);
                            if (val > 0) {
                                cell.setText(Integer.toString(val));
                                cell.setTextColor(0xff000000);
                            } else
                                cell.setText("");
                        }
                        currentSudoku.superSolve();
                    }
                });
            }


        });
        th.start();
    }

    public void solveButton(View v) {

        if (currentSudoku == null)
            return;


        vf.showNext();

        if (!currentSudoku.checkGrid()) {
            Toast myToast = Toast.makeText(getApplicationContext(), "There seems to be something illegal in the grid!\n Please submit a valid one.", Toast.LENGTH_LONG);
            myToast.show();
            return;
        }

        currentSudoku.bruteSolve();
        Toast myToast = Toast.makeText(getApplicationContext(), "It is possible that this is not the only solution.\n Blame the dev for only showing one.", Toast.LENGTH_LONG);
        myToast.show();


        int dim = currentSudoku.getDimension();
        newSelection(-1);
        for (int i = 0; i < dim * dim; ++i) {
            if (currentSudoku.isNewValue(i / dim, i % dim)) {
                TextView cell = ((TextView) gridLayout.getChildAt(i));
                cell.setTextColor(0xff0000ff);
                cell.setText(Integer.toString(currentSudoku.getSolvedValue(i / dim, i % dim)));
            }
            currentSudoku.setActiveValue(i/dim,i%dim,currentSudoku.getSolvedValue(i/dim,i%dim));
        }
        for (int i = 0; i < dim * dim; ++i) {
            if (currentSudoku.getSolvedValue(i / dim, i % dim) == 0) {
                Toast mToast = Toast.makeText(getApplicationContext(), "The app is not able to fully solve this grid.\nBlame the terrible dev! Actually, if you know him tell him you saw this message, that's really not supposed to happen.", Toast.LENGTH_LONG);
                mToast.show();
                return;
            }
        }
        currentSudoku.showSolution = true;
    }

    public void valueButtonClick(View v) {
        if (selected != -1 && currentSudoku != null) {
            if (currentSudoku.getOriginalVal(selected / 9, selected % 9) == 0) {
                int row = selected / 9;
                int col = selected % 9;
                for (int i = 0; 0 < 10; ++i) {
                    if (buttons[i] == v) {
                        currentSudoku.setActiveValue(row, col, i);
                        if (i > 0) {
                            ((TextView) gridLayout.getChildAt(selected)).setTextColor(0xffff7800);
                            ((TextView) gridLayout.getChildAt(selected)).setText(Integer.toString(i));
                        } else {
                            ((TextView) gridLayout.getChildAt(selected)).setText("");
                        }
                        break;
                    }
                }
            }
        }
    }


    public void checkButtonClick(View v) {
        if (!customizing){
            newSelection(-1);
            boolean clean = true;
            mistakes = new boolean[9][9];
            for (int c = 0; c < 9; ++c) {
                for (int r = 0; r < 9; ++r) {
                    gridLayout.getChildAt(r*9+c).setBackgroundColor(Color.TRANSPARENT);
                }
            }
            if (currentSudoku != null) {
                for (int r = 0; r < 9; ++r) {
                    for (int c = 0; c < 9; ++c) {
                        if (currentSudoku.getActiveVal(r, c) != 0 && currentSudoku.getOriginalVal(r, c) == 0) {
                            if (currentSudoku.checkActiveRuleBreak(r, c)) {
                                ((TextView) gridLayout.getChildAt(r * 9 + c)).setTextColor(0xffff0000);
                                gridLayout.getChildAt(r * 9 + c).setBackgroundColor(0x66ff0000);
                                mistakes[r][c] = true;
                                clean = false;
                            }
                        }
                    }
                }
                if (currentSudoku.isPlayerSolved()) {
                    for (int i = 0; i < 81; ++i)
                        if (currentSudoku.getOriginalVal(i / 9, i % 9) == 0)
                            ((TextView) gridLayout.getChildAt(i)).setTextColor(0xff0000ff);

                    Toast myToast = Toast.makeText(getApplicationContext(), "Congratulations!\nYou have succesfully solved this Sudoku!", Toast.LENGTH_LONG);
                    myToast.show();
                    return;
                }
            }
            if (clean) {
                Toast myToast = Toast.makeText(getApplicationContext(), "Nothing is illegal in the grid.\nFor now...", Toast.LENGTH_LONG);
                myToast.show();
            } else {
                Toast myToast = Toast.makeText(getApplicationContext(), "There seems to be something illegal in the grid!", Toast.LENGTH_LONG);
                myToast.show();
            }
        }
        else{
            customizing = false;
            newSelection(-1);
            currentSudoku.applyCustomGrid();
            for (int i = 0; i < 9 * 9; ++i) {
                int val = currentSudoku.getActiveVal(i / 9, i % 9);
                TextView cell = (TextView) gridLayout.getChildAt(i);
                if (val > 0) {
                    cell.setText(Integer.toString(val));
                } else {
                    cell.setText("");
                }

                cell.setTextColor(0xff000000);
            }

            for (int i = 0; i < 10; ++i){
                final int b = i;
                buttons[b].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        valueButtonClick(buttons[b]);
                    }
                });
            }

            ((Button) findViewById(R.id.check_button)).setText("Check");
            ((Button) findViewById(R.id.menu_button)).setClickable(true);
            ((Button) findViewById(R.id.menu_button)).setTextColor(0xff000000);
        }

    }

    public void menuButtonClick(View v) {
        vf.showNext();
        //mistakes = new boolean[9][9];
    }
    public void customGameButtonClick(View v) {

        findViewById(R.id.resume_button).setClickable(true);
        ((Button)findViewById(R.id.resume_button)).setTextColor(0xff000000);
        if (!isInit) {
            initGridLayout();
            isInit = true;
        }
        vf.showNext();
        currentSudoku = new sudoku();
        for (int i = 0; i < 10; ++i) {
            buttons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selected != -1 && currentSudoku != null) {
                        if (currentSudoku.getOriginalVal(selected / 9, selected % 9) == 0) {
                            int row = selected / 9;
                            int col = selected % 9;
                            for (int i = 0; 0 < 10; ++i) {
                                if (buttons[i] == v) {
                                    currentSudoku.setActiveValue(row, col, i);
                                    if (i > 0) {
                                        ((TextView) gridLayout.getChildAt(selected)).setTextColor(0xff00bb00);
                                        ((TextView) gridLayout.getChildAt(selected)).setText(Integer.toString(i));
                                    } else {
                                        ((TextView) gridLayout.getChildAt(selected)).setText("");
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            });
        }
        for (int i = 0; i < 9 * 9; ++i) {
            //int val = currentSudoku.getActiveVal(i / 9, i % 9);
            TextView cell = (TextView) gridLayout.getChildAt(i);
            cell.setText("");
        }
        customizing = true;
        ((Button) findViewById(R.id.check_button)).setText("Done");
        ((Button) findViewById(R.id.menu_button)).setClickable(false);
        ((Button) findViewById(R.id.menu_button)).setTextColor(0xff999999);
    }
}

