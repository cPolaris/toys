import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;

public class GameOfLife {

/* RULES OF THE GAME
 * 1. A dead cell with exactly three live neighbors becomes live.
 * 2. A live cell with exactly one live neighbor becomes dead.
 * 3. A live cell with more than three live neighbors becomes dead.
 */

    private static int frameDuration;
    private static int totalFrames;
    private static int frameCount = 0;
    private static int arenaRow;
    private static int arenaCol;
    private static boolean[][] arena;
    private static boolean[][] nextArena;
    private static boolean hit = false;

    // edges
    private static int left;
    private static int right;
    private static int upper;
    private static int lower;

    public static void drawArena() {
        for (int i = 0; i < arenaRow; i++) {
            for (int j = 0; j < arenaCol; j++) {
                if (arena[i][j]) {
                    StdDraw.filledSquare(j, arenaRow - 1 - i, 0.5, true);
                }
            }
        }
    }

    public static void putAtCenter(boolean[][] being) {
        putAtXY(being, arenaCol / 2, arenaRow / 2);
    }

    public static void putAtXY(boolean[][] being, int x, int y) {
        int row = being.length;
        int col = being[0].length;
        int xStart = x - col / 2;
        int yStart = y - row / 2;

        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                arena[yStart+i][xStart+j] = being[i][j];
            }
        }

        initializeEdges();
    }

    public static void putAtScale(boolean[][] being, double xScale,
                                                   double yScale) {
        Double xPosition = xScale * arenaCol;
        Double yPosition = yScale * arenaRow;
        putAtXY(being, xPosition.intValue(), yPosition.intValue());
    }

    private static void evolve() {
        nextArena = new boolean[arenaRow][arenaCol];
        for (int i = upper; i <= lower; i++) {
            for (int j = left; j <= right; j++) {
                if (i == 0 || i == arenaRow-1) {
                    hit = true;
                    return;
                }
                if (j == 0 || j == arenaCol-1) {
                    hit = true;
                    return;
                }
                judge(i, j);
            }
        }
        arena = nextArena;
    }

    private static void judge(int row, int col) {
        if (arena[row][col]) {
            int neighbors = liveNeighbors(arena, row, col);
            // Original rule was neighbors <= 1
            // But leaving lonely cells alive makes it more lively
            if (neighbors == 1 || neighbors > 3) {
                nextArena[row][col] = false;
            } else {
                nextArena[row][col] = true;
                updateEdges(row, col);
            }
        } else {
            if (liveNeighbors(arena, row, col) == 3) {
                nextArena[row][col] = true;
                updateEdges(row, col);
            }
        }

    }

    private static int liveNeighbors(boolean[][] anArena,
                                     int row, int col) {
        int count = 0;
        for (int i = row-1; i <= row+1; i++) {
            for (int j = col-1; j <= col+1; j++) {
                if (i == row && j == col) continue;
                if (anArena[i][j])    count++;
            }
        }
        return count;
    }

    private static void updateEdges(int row, int col) {
        if (col - 1 < left)  left  = col - 1;
        if (col + 1 > right) right = col + 1;
        if (row - 1 < upper) upper = row - 1;
        if (row + 1 > lower) lower = row + 1;
    }

    private static void initializeEdges() {
        left = arenaCol;
        right = -1;
        upper = arenaRow;
        lower = -1;

        for (int i = 1; i < arenaRow-1; i++) {
            for (int j = 1; j < arenaCol-1; j++) {
                if (arena[i][j]) {
                    if (j - 1 < left)  left  = j - 1;
                    if (j + 1 > right) right = j + 1;
                    if (i - 1 < upper) upper = i - 1;
                    if (i + 1 > lower) lower = i + 1;
                }
            }
        }
    }

    public static void evolveUntilHit() {
        while (!hit) {
            evolve();
//            printStats();
        }
    }

    private static void printStats() {
        System.out.printf("Frame: %7d | Upper: %5d / 1 | Lower: %5d /%5d | Right: %5d /%5d\r",
                            ++frameCount,
                            upper,
                            lower,
                            arenaRow-2,
                            right,
                            arenaCol-2);
    }


    public static void saveArenaAsText() {
        File theArenaText = new File("a.txt");
        try {
            BufferedWriter output = new BufferedWriter(new FileWriter(theArenaText));
            output.write(String.format("%d %d\n", arenaRow, arenaCol));
            for (int i = 0; i < arena.length; i++) {
                for (int j = 0; j < arena[0].length; j++) {
                    if(arena[i][j]) {
                        output.write("1 ");
                    } else {
                        output.write("0 ");;
                    }
                }
                output.write("\n");
            }
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Final state > a.txt DONE!");
    }

    public static void loadTextArena(String textFileName, double growth) {
        File inputFile = new File(textFileName);
        System.out.println();
        try {
            Scanner in = new Scanner(inputFile);
            int oldRow = in.nextInt();
            int oldCol = in.nextInt();
            boolean[][] inputArena = new boolean[oldRow][oldCol];

            System.out.print("LOADING...\r");
            for (int i = 0; i < oldRow; i++) {
                for (int j = 0; j < oldCol; j++) {
                    int numerical = in.nextInt();
                    if (numerical == 0) {
                        inputArena[i][j] = false;
                    } else {
                        inputArena[i][j] = true;
                    }
                }
            }
            in.close();
            System.out.println("Text file loaded!");

            arenaRow = (int)(oldRow * growth);
            arenaCol = (int)(oldCol * growth);
            arena = new boolean[arenaRow][arenaCol];
            System.out.printf("ROW: %6d -> %6d", oldRow, arenaRow);
            System.out.printf(" | COL: %6d -> %6d\n", oldCol, arenaCol);

            putAtCenter(inputArena);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println();
    }

    public static void saveCurrentArenaAsPicture() {
        StdDraw.save("a.png");
        StdDraw.save("a.jpg");
        System.out.println("Final state > a.png and jpg DONE!");
    }

    public static void animateForHit(boolean fastMode) {
        if (fastMode) {
            while (!hit) {
                StdDraw.clear();
                drawArena();
                StdDraw.show(true);
                evolve();
//                printStats();
            }
        } else {
            while (!hit) {
                StdDraw.clear();
                drawArena();
                StdDraw.show(frameDuration);
                evolve();
//                printStats();
            }
        }
    }

    private static void startEmptyArena(int size, boolean[][] seed,
                                        double xScale, double yScale) {
        arenaRow = size;
        arenaCol = size;
        arena = new boolean[arenaRow][arenaCol];
        nextArena = new boolean[arenaRow][arenaCol];
        putAtScale(seed, xScale, yScale);
        initializeStdDraw();
    }

    private static void initializeStdDraw() {
        StdDraw.setPenColor(StdDraw.GRAY);
        int scale = arenaRow;
        if (arenaCol > arenaRow) scale = arenaCol;
        StdDraw.setXscale(-1, scale);
        StdDraw.setYscale(-1, scale);
    }

    public static void setAnimationParameters() {
      int fps = 5;
      int time = 10;

      Double millisDuration = 1.0 / fps * 1000.0;
      frameDuration = millisDuration.intValue();
      totalFrames = fps * time;
  }

    public static void main(String[] args) {

        startEmptyArena(300, GolHatchery.EPIC_INSECT, 0.6, 0.4);
//        loadTextArena("a.txt", 1.05);
//        StdDraw.setCanvasSize(arenaCol, arenaRow);

        long start = System.currentTimeMillis();
        animateForHit(true);

        System.out.println();
        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("Size: %d px | Took time: %.1f sec (%.2f min)\n",
                          arenaCol*arenaRow, elapsed/1000.0,
                          elapsed/60000.0);

        // SAVING
        // saveArenaAsText();
//         saveCurrentArenaAsPicture();

    }

}
