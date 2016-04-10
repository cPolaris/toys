
public class TestStdDraw {

    private static int left;
    private static int right;
    private static int upper;
    private static int lower;
    
    private static int row;
    private static int col;
    
    private static int scale = 15;

    private static boolean[][] arena;
    
    private static boolean[][] trial2D = {{true,true,true,true,true,true,true,true,true},
            {true,false,false,false,false,false,false,false,true},
            {true,false,false,false,false,false,false,false,true},
            {true,false,false,false,false,false,false,false,true},
            {true,false,false,false,false,false,false,false,true},
            {true,false,false,false,false,false,false,false,true},
            {true,false,false,false,false,false,false,false,true},
            {true,false,false,false,false,false,false,false,true},
            {true,true,true,true,true,true,true,true,true}};;
    
    private static boolean[][] dot = {{true}};
    
    private static boolean[][] penis = {{false,true,false},
                                         {false,true,false},
                                         {true,true,true}};

            
    public static void main(String[] args) {
        StdDraw.setPenColor(StdDraw.BOOK_RED);
        
        StdDraw.setXscale(-1,scale);
        StdDraw.setYscale(-1,scale);
        
        arena = new boolean[scale][scale];
        
        row = arena.length;
        col = arena[0].length;
        
        putAtXY(penis, col / 2, row / 2);
        
        initializeEdges();
        System.out.println(left + " " + right + " " + upper + " " + lower);
        
        printAxisLabels();
        
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                if (arena[i][j]) {
                    StdDraw.filledSquare(j, row -1 - i, 0.5);
                }
            }
        }
    }
    
    private static void printAxisLabels() {
        for (int i = -1; i <= scale; i++) {
            StdDraw.text(i, 0, Integer.toString(i));
        }
        for (int i = -1; i <= scale; i++) {
            StdDraw.text(0, i, Integer.toString(i));
        }
    }
    
    private static void initializeEdges() {
        left = col;
        right = -1;
        upper = row;
        lower = -1;
        
        for (int i = 1; i <= row-1; i++) {
            for (int j = 1; j <= col-1; j++) {
                if (arena[i][j]) {
                    if (j < left) left = j;
                    if (j > right) right = j;
                    if (i > lower) lower = i;
                    if (i < upper) upper = i;
                }
            }
        }
        left--; right++; upper--; lower++;
    }
    
    private static void putAtXY(boolean[][] being, int x, int y) {
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

}
