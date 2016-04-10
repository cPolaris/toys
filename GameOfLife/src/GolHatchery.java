import java.util.Random;

public class GolHatchery {

    public static final boolean[][] GLIDER = {{false,true,false},
                                              {false,false,true},
                                              {true,true,true}};
    
    public static final boolean[][] MY_STANDING_INVADER = {{false,false,true,false,true,false,false},
                                                           {false,false,true,true,true,false,false},
                                                           {false,true,false,true,false,true,false},
                                                           {true,true,true,true,true,true,true},
                                                           {false,false,true,true,true,false,false},
                                                           {false,false,true,false,true,false,false}};
    
    public static final boolean[][] NOT_MY_PENIS = {{false,true,false},
                                                    {false,true,false},
                                                    {true,true,true}};
    
    public static final boolean[][] EPIC_INSECT = {{false,true,false},
                                            {true,true,true},
                                            {true,false,true}};
    
    public static final boolean[][] EPIC_VIRUS = {{false,false,true},
                                                    {true,false,true},
                                                    {true,true,true}};
    
    public static final boolean[][] RING = {{true,true,true},
                                            {true,false,true},
                                            {true,true,true}};
    public static final boolean[][] DOT = {{true}};
    
    public static boolean[][] randomThreeByThree() {
        Random rgen = new Random();
        boolean[][] result = new boolean[3][3];
        
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                result[i][j] = rgen.nextBoolean();
                System.out.printf("%b ", result[i][j]);
            }
            System.out.printf("\n");
        }
        
        return result;
    }
    
}
