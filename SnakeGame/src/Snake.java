/*
 * Author: Tiangang Chen, tiangang2014@my.fit.edu
 * Course: CSE 1002, Section 02, Fall 2015
 * Project: snake
 */

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

/*
 * ============================================================================
 * SPEC:
 * You program should start off with a snake of a short length in the middle
 * of the board. The game starts when the player picks a direction for the
 * snake to go (using the arrow keys), the snake should continue moving in that
 * direction until a new direction is picked using the arrow keys.
 * A piece of "food" should appear on the board in a randomly selected position.
 * When the snake "consumes" the food, the snake should grow in length.
 * Another piece of food should appear on the board in a random position.
 * More than one piece of "food" can appear on the board at a time
 * (the whole board cannot be filled though).
 * Food should disappear after a random amount of time.
 * The user "wins" when the snake fills up all available positions on the board.
 * If the snake "dies" by crashing into a wall or itself, show the current
 * calculated score.
 * ============================================================================
 */

/**
 * @author Tiangang Chen
 */
public final class Snake {

    // PARAMETERS =============================================================
    // Magic numbers
    private static final int A_GOOD_FETCH_CONTROL_RESOLUTION = 75;
    private static final int WINDOW_HEIGHT = 756;
    private static final int WINDOW_WIDTH = 720;
    private static final double MARGIN_RATIO = 1.05;
    private static final int JUST_AN_UNREASONABLE_NUMBER = 999;
    private static final double DIFFICULTY_DIVISOR = 1.4;
    private static final int DIFFICULTY_OFFSET = 10;
    private static final int SCORE_EACH_LENGTH = 100;
    private static final double ZERO_POINT_FOUR_FOUR = 0.44;
    private static final int REALLY_BIG_FONT_SIZE = 30;
    private static final double ZERO_POINT_FIVE_SEVEN = 0.57;
    private static final int VERY_BIG_FONT_SIZE = 36;
    private static final double ZERO_POINT_TWO = 0.2;
    private static final double ZERO_POINT_THREE_THREE = 0.33;
    private static final double ZERO_POINT_FIVE = 0.5;
    private static final double THICK_PEN_RADIUS = 0.02;
    private static final int WINDOW_BORDER_WIDTH_PIXELS = 10;
    private static final int MID_FONT_SIZE = 20;
    private static final double EASY_XTRA_PROB = 0.5;
    private static final double EASY_SUPER_PROB = 0.8;
    private static final double HARD_XTRA_PROB = 0.6;
    private static final double HARD_SUPER_PROB = 0.97;
    private static final double REGULAR_XTRA_PROB = 0.65;
    private static final double REGULAR_SUPER_PROB = 0.96;
    private static final double FIELD_MARGIN_MULTIPLIER = 1.05;

    static final int MILLIS_IN_A_SECOND = 1000;
    static final double HALF_CELL_WIDTH = 0.5;
    static final int CONTROL_RESOLUTION = 25; // Miliseconds
    static final int FOOD_DISPENSE_CHECK_RESOLUTION = 500; // Miliseconds
    static final int DEFAULT_FIELD_SIZE = 19;
    static final int DEFAULT_START_LENGTH = 6;

    // Default values. Will be applied on start-up
    static int fIELdSIZE = DEFAULT_FIELD_SIZE;
    private static int startLength = DEFAULT_START_LENGTH;
    static Direction startDir;
    private static DifficultyLevels difficulty = DifficultyLevels.REGULAR;
    private static FoodScarcityLevels foodQuantity = FoodScarcityLevels.ABUNDANT;
    private static int speedMultiplier; // Ratio btwn cntrl check & move
    static double superProb;
    static double xtraProb;

    // SHARED DATA ============================================================
    static boolean boringMode = false;
    private static int frameCount; // Num. of frames elapsed
    private static boolean keepPlaying = true;

    private static Node head; // head of the snake
    private static Node tail; // tail of the snake
    static int snakeLen; // snake length
    private static boolean alive;
    private static int growCount = 0;

    private static LinkedList<Sweet> sweetPlaces;

    // Shared manipulation ================================================
    private static JPanel settingsPanel;
    private static JLabel msgLabel1;
    private static JLabel msgLabel2;
    private static JLabel msgLabel3;
    private static JLabel msgLabel4;
    private static JLabel msgLabel5;

    static Random rGen = new Random(Long.getLong("seed", System.nanoTime()));
    private static Thread interactionThread;
    private static Thread foodDispenseThread;

    private static JFrame setupFrame;
    private static JTextField sizeTextField;
    private static JTextField startingLenTextField;
    private static JComboBox<DifficultyLevels> difficultyComboBox;
    private static JComboBox<FoodScarcityLevels> foodQuantityComboBox;

    // For synchronization
    private static CountDownLatch holdOnLatch;
    private static Object syncLock = new Object();

    // No need to instantiate this class
    private Snake() {
    }

    // FUNCTIONAL METHODS =====================================================
    /**
     * Set up game according to parameters.
     * Display control tips.
     */
    private static void initGame () {
        StdDraw.show(0);

        // Graphics setup
        StdDraw.clear(StdDraw.BLACK);
        fIELdSIZE--;
        final double marginWidth = fIELdSIZE * 0.01;
        StdDraw.setXscale(-marginWidth, fIELdSIZE + marginWidth);
        StdDraw.setYscale(-marginWidth,
                (FIELD_MARGIN_MULTIPLIER * fIELdSIZE) + marginWidth);

        // Initialize avlues
        sweetPlaces = new LinkedList<Sweet>();
        alive = true;
        frameCount = 0;
        snakeLen = startLength;
        speedMultiplier = difficulty.val();

        switch (difficulty) {
        case REGULAR:
            superProb = REGULAR_SUPER_PROB;
            xtraProb = REGULAR_XTRA_PROB;
            break;
        case HARD:
            superProb = HARD_SUPER_PROB;
            xtraProb = HARD_XTRA_PROB;
            break;
        case EASY:
            superProb = EASY_SUPER_PROB;
            xtraProb = EASY_XTRA_PROB;
            break;
        default:
            // unreachable
            break;
        }

        // Build snake at starting location
        final int startX = fIELdSIZE / 2;
        final int startY = fIELdSIZE / 2;

        head = new Node(startX, startY, Direction.U);
        tail = head;
        for (int i = 1; i < startLength; i++) {
            linkLast(Direction.D);
        }

        RenderLoop.drawFrame();
        StdDraw.setFont(new Font("SansSerif", Font.BOLD, REALLY_BIG_FONT_SIZE));
        StdDraw.text(fIELdSIZE * ZERO_POINT_FIVE,
                fIELdSIZE * ZERO_POINT_FIVE_SEVEN,
                "Select a direction to start");
        StdDraw.setFont();
        StdDraw.show();
    }

    /**
     * Fetch the direction key the player is currently pressing
     */
    private static Direction fetchControl () {
        if (StdDraw.isKeyPressed(Direction.D.keyVal)) {
            return Direction.D;
        } else if (StdDraw.isKeyPressed(Direction.U.keyVal)) {
            return Direction.U;
        } else if (StdDraw.isKeyPressed(Direction.L.keyVal)) {
            return Direction.L;
        } else if (StdDraw.isKeyPressed(Direction.R.keyVal)) {
            return Direction.R;
        } else {
            return null;
        }
    }

    /**
     * Setup dialog
     * Stupid checkStyle says the method is too long. So I broke it in parts
     * so that it is even more unreadable.
     */
    private static void showSetupDialogue () {
        setupFrame = new JFrame("Start Game");
        settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));

        // COMPONENTS
        // Mode Selection
        final Box modeBox = new Box(BoxLayout.Y_AXIS);
        final ButtonGroup raiodButtonGroup = new ButtonGroup();
        final JRadioButton classicsRadioButton = new JRadioButton(
                "Classic (Boring) Mode");

        final JRadioButton funRadioButton = new JRadioButton("Fun Mode");

        showSetupDialogue3(modeBox, raiodButtonGroup, classicsRadioButton,
                funRadioButton);

        // Field size
        final Box sizeBox = new Box(BoxLayout.X_AXIS);
        final JLabel sizeLabel = new JLabel("Field Size");
        sizeTextField = new JTextField();
        sizeBox.add(sizeLabel);
        sizeBox.add(sizeTextField);

        // Starting length
        final Box startLenBox = new Box(BoxLayout.X_AXIS);
        final JLabel startingLenLabel = new JLabel("Strating Length");
        startingLenTextField = new JTextField();
        startLenBox.add(startingLenLabel);
        startLenBox.add(startingLenTextField);

        // Difficulty
        final Box difficultyBox = new Box(BoxLayout.X_AXIS);
        final JLabel difficultyLabel = new JLabel("Difficulty");
        difficultyComboBox = new JComboBox<DifficultyLevels>();
        difficultyComboBox.addItem(DifficultyLevels.EASY);
        difficultyComboBox.addItem(DifficultyLevels.REGULAR);
        difficultyComboBox.addItem(DifficultyLevels.HARD);
        difficultyBox.add(difficultyLabel);
        difficultyBox.add(difficultyComboBox);

        // Food quantity
        final Box foodQuanBox = new Box(BoxLayout.X_AXIS);
        final JLabel foodQuanLabel = new JLabel("Food quantity");
        foodQuantityComboBox = new JComboBox<FoodScarcityLevels>();
        foodQuantityComboBox.addItem(FoodScarcityLevels.SCARCE);
        foodQuantityComboBox.addItem(FoodScarcityLevels.ABUNDANT);
        foodQuantityComboBox.addItem(FoodScarcityLevels.EXTRA);
        foodQuanBox.add(foodQuanLabel);
        foodQuanBox.add(foodQuantityComboBox);

        // Play button
        final Box playButtonBox = new Box(BoxLayout.X_AXIS);
        final JButton playButton = new JButton("Play");
        final SetupDialogListener playButtonListener = new SetupDialogListener();
        playButton.addActionListener(playButtonListener);
        playButton.setSelected(true);
        playButtonBox.add(playButton);

        // Set default values
        showSetupDialogue2(modeBox, sizeBox, startLenBox, difficultyBox,
                foodQuanBox, playButtonBox);
    }

    /**
     * Stupid checkStyle says the method is too long. So I broke it in parts
     * so that it is even more unreadable.
     *
     * @param modeBox
     * @param raiodButtonGroup
     * @param classicsRadioButton
     * @param funRadioButton
     */
    private static void showSetupDialogue3 (final Box modeBox,
            final ButtonGroup raiodButtonGroup,
            final JRadioButton classicsRadioButton,
            final JRadioButton funRadioButton) {
        classicsRadioButton.addActionListener(new ClassicButtonListener());
        funRadioButton.addActionListener(new FunButtonListener());

        raiodButtonGroup.add(classicsRadioButton);
        raiodButtonGroup.add(funRadioButton);

        modeBox.add(classicsRadioButton);
        modeBox.add(funRadioButton);

        // Message
        msgLabel1 = new JLabel("Use arrow keys to control");
        msgLabel2 = new JLabel("White dot: Sweet");
        msgLabel3 = new JLabel("White ring: Extra calorie sweet");
        msgLabel4 = new JLabel("Blue dot: Super self-eliminating potion");
        msgLabel5 = new JLabel(
                "You WIN by reaching a cumulative length count of the size of field");
        msgLabel1.setFont(new Font("SansSerif", Font.BOLD, MID_FONT_SIZE));

        if (!boringMode) {
            raiodButtonGroup.setSelected(funRadioButton.getModel(), true);
            funRadioButton.doClick();
        } else {
            raiodButtonGroup.setSelected(classicsRadioButton.getModel(), true);
            classicsRadioButton.doClick();
        }
    }

    /**
     * Stupid checkStyle says the method is too long. So I broke it in parts
     * so that it is even more unreadable.
     *
     * @param modeBox
     * @param sizeBox
     * @param startLenBox
     * @param difficultyBox
     * @param foodQuanBox
     * @param playButtonBox
     */
    private static void showSetupDialogue2 (final Box modeBox,
            final Box sizeBox, final Box startLenBox, final Box difficultyBox,
            final Box foodQuanBox, final Box playButtonBox) {
        setTextFieldValues(fIELdSIZE + 1, startLength, difficulty,
                foodQuantity);

        // Arrange components
        settingsPanel.add(modeBox);
        settingsPanel.add(msgLabel1);
        settingsPanel.add(msgLabel2);
        settingsPanel.add(msgLabel3);
        settingsPanel.add(msgLabel4);
        settingsPanel.add(msgLabel5);
        settingsPanel.add(sizeBox);
        settingsPanel.add(startLenBox);
        settingsPanel.add(difficultyBox);
        settingsPanel.add(foodQuanBox);
        settingsPanel.add(playButtonBox);
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(
                WINDOW_BORDER_WIDTH_PIXELS, WINDOW_BORDER_WIDTH_PIXELS,
                WINDOW_BORDER_WIDTH_PIXELS, WINDOW_BORDER_WIDTH_PIXELS));
        setupFrame.add(settingsPanel);
        setupFrame.pack();
        setupFrame.setResizable(false);
        setupFrame.setVisible(true);

        // Do not proceed until fetched input
        holdOnLatch = new CountDownLatch(1);
        try {
            holdOnLatch.await();
        } catch (final InterruptedException e) {
            e.printStackTrace();
            System.out.println("Unexpected interruption");
        }

        setupFrame.setVisible(false);
    }

    /**
     * Shows the game over message as a text box in the game window
     */
    private static void showGameOver () {
        // Frame
        StdDraw.setXscale();
        StdDraw.setYscale();
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.setPenRadius(THICK_PEN_RADIUS);
        StdDraw.setPenColor(StdDraw.RED);
        StdDraw.rectangle(ZERO_POINT_FIVE, ZERO_POINT_FIVE,
                ZERO_POINT_THREE_THREE, ZERO_POINT_TWO);
        // Message
        StdDraw.setFont(new Font("SansSerif", Font.BOLD, VERY_BIG_FONT_SIZE));
        if (snakeLen == ((fIELdSIZE + 1) * (fIELdSIZE + 1))) {
            if (boringMode) {
                StdDraw.text(ZERO_POINT_FIVE, ZERO_POINT_FIVE_SEVEN,
                        "SHIT! YOU REALLY WON!");
            } else {
                StdDraw.text(ZERO_POINT_FIVE, ZERO_POINT_FIVE_SEVEN,
                        "YOU WON!");
            }
        } else {
            StdDraw.text(ZERO_POINT_FIVE, ZERO_POINT_FIVE_SEVEN, "GAME OVER");
        }
        StdDraw.setFont(new Font("SansSerif", Font.BOLD, REALLY_BIG_FONT_SIZE));
        StdDraw.text(ZERO_POINT_FIVE, ZERO_POINT_FIVE,
                "SCORE: " + calculateScore());
        StdDraw.setFont(new Font("SansSerif", Font.BOLD, MID_FONT_SIZE));
        StdDraw.text(ZERO_POINT_FIVE, ZERO_POINT_FOUR_FOUR,
                "Press ENTER key to start a new round");

        StdDraw.show();
    }

    // HELPER METHODS =========================================================
    /**
     * Calculate the score according to the current frame count,
     * snake length and difficulty.
     *
     * @todo rule for scoring?
     * @return
     */
    private static int calculateScore () {
        return (int) ((((snakeLen * SCORE_EACH_LENGTH)
                + ((frameCount * 2 * CONTROL_RESOLUTION) / MILLIS_IN_A_SECOND))
                * (DIFFICULTY_OFFSET - difficulty.val())) / DIFFICULTY_DIVISOR);
    }

    /**
     * @deprecated
     *             Link a new node to the snake at front by specifying exact
     *             coordinate
     * @param xCor
     * @param yCor
     */
    @Deprecated
    @SuppressWarnings("unused")
    private static void linkFirst (final int xCor, final int yCor) {
        final Node newHead = new Node(xCor, yCor, head.heading);
        newHead.next = head;
        head.prev = newHead;
        head = newHead;
    }

    /**
     * @deprecated
     *             Link a new node to the snake at tail by specifying exact
     *             coordinate
     * @param xCor
     * @param yCor
     */
    @Deprecated
    @SuppressWarnings("unused")
    private static void linkLast (final int xCor, final int yCor) {
        final Node newTail = new Node(xCor, yCor, tail.heading);
        tail.next = newTail;
        newTail.prev = tail;
        tail = newTail;
    }

    /**
     * Move from original position to the position that
     * the direction specified.
     *
     * @param origin
     * @param dir
     * @return
     */
    private static Coordinate move (final Coordinate origin,
            final Direction dir) {
        final int oriX = origin.x;
        final int oriY = origin.y;
        switch (dir) {
        case U:
            return new Coordinate(oriX, oriY + 1);
        case D:
            return new Coordinate(oriX, oriY + -1);
        case L:
            return new Coordinate(oriX - 1, oriY);
        case R:
            return new Coordinate(oriX + 1, oriY);
        default:
            System.out.println("Unreachable");
            return new Coordinate(JUST_AN_UNREASONABLE_NUMBER,
                    JUST_AN_UNREASONABLE_NUMBER);
        }
    }

    /**
     * Link a new node to the tail with specified position
     * relative to the last node
     *
     * @param dir
     */
    private static void linkLast (final Direction dir) {
        if (dir.equals(tail.heading)) {
            throw new IllegalArgumentException("New node collides with old");
        }
        // Determine position of new node
        int newX = tail.x();
        int newY = tail.y();
        switch (dir) {
        case U:
            newY++;
            break;
        case D:
            newY--;
            break;
        case L:
            newX--;
            break;
        case R:
            newX++;
            break;
        default:
            System.out.println("Unreachable");
        }

        // Link the new node
        final Node newTail = new Node(newX, newY, dir.opposite());
        tail.next = newTail;
        newTail.prev = tail;
        tail = newTail;
    }

    private static void setTextFieldValues (final int size, final int length,
            final DifficultyLevels dffclty, final FoodScarcityLevels foodQuan) {
        // Just to avoid some duplicated code
        sizeTextField.setText(Integer.toString(size));
        startingLenTextField.setText(Integer.toString(length));
        difficultyComboBox.setSelectedItem(dffclty);
        foodQuantityComboBox.setSelectedItem(foodQuan);
    }

    private static void setScoreBoard (final int score) {
        StdDraw.textLeft(0, MARGIN_RATIO * fIELdSIZE,
                String.format("SCORE: %010d", score));
    }

    // MAIN ===================================================================
    /**
     * Lanuches the game.
     *
     * @param args
     */
    public static void main (final String[] args) {
        StdDraw.setCanvasSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        while (keepPlaying) {
            showSetupDialogue();
            keepPlaying = false;
            initGame();
            interactionThread = new Thread(new RenderLoop());
            foodDispenseThread = new Thread(new FoodDispenser());

            while (true) {
                if (StdDraw.isKeyPressed(Direction.U.keyVal)) {
                    startDir = Direction.U;
                    break;
                } else if (StdDraw.isKeyPressed(Direction.L.keyVal)) {
                    startDir = Direction.L;
                    break;
                } else if (StdDraw.isKeyPressed(Direction.R.keyVal)) {
                    startDir = Direction.R;
                    break;
                }
                try {
                    Thread.sleep(A_GOOD_FETCH_CONTROL_RESOLUTION);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }

            foodDispenseThread.start();
            interactionThread.start();

            try {
                interactionThread.join();
                foodDispenseThread.interrupt();
                growCount = 0;

            } catch (final InterruptedException e) {
                e.printStackTrace();
            }

            showGameOver();

            // Hold on until obtained key input
            waitReplay();

        }

    }

    private static void waitReplay () {
        boolean receivedReplayInput = false;
        while (!receivedReplayInput) {
            if (StdDraw.isKeyPressed(KeyEvent.VK_ENTER)) {
                keepPlaying = true;
                receivedReplayInput = true;
            }
            // Two purposes for this sleep:
            // 1. Save resource. It's too brutal just using a simple loop
            // 2. Give StdDraw enough time to receive the KeyReleased
            // event. Otherwise a buggy behavior arises.
            try {
                Thread.sleep(A_GOOD_FETCH_CONTROL_RESOLUTION);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // LISTENERS ==============================================================
    static class ClassicButtonListener implements ActionListener {
        @Override
        public void actionPerformed (final ActionEvent e) {
            boringMode = true;
            // Change message
            msgLabel1.setText("Use arrow keys to control");
            msgLabel2.setText("White dot: Sweet");
            msgLabel3.setText(
                    "You WIN by filling the field with the snake body");
            msgLabel4.setText("");
            msgLabel5.setText("");
            setupFrame.repaint();
            setupFrame.pack();
        }

    }

    static class FunButtonListener implements ActionListener {
        @Override
        public void actionPerformed (final ActionEvent e) {
            boringMode = false;
            // Change message
            msgLabel1.setText("Use arrow keys to control");
            msgLabel2.setText("White dot: Sweet");
            msgLabel3.setText("White ring: Extra calorie sweet");
            msgLabel4.setText("Blue pentagon: Super self-eliminating potion");
            msgLabel5.setText(
                    "You WIN by reaching a cumulative length count of the size of field");
            setupFrame.repaint();
            setupFrame.pack();
        }

    }

    static final class SetupDialogListener implements ActionListener {
        private static final int TOO_SMALL_FIELD_SIZE = 4;
        private static boolean validInput = false;

        @Override
        public void actionPerformed (final ActionEvent e) {
            checkInputValid();
            if (validInput) {
                holdOnLatch.countDown();
            }
        }

        /**
         * @todo
         *       Check if input values are valid. Either out-of-range or
         *       NumberFormatException should result in resetting the text
         *       fields
         *       back to default values, then wait for another input.
         */
        private static void checkInputValid () {
            // Work-around for NumberFormatException
            final int sizePending = (int) Double
                    .parseDouble(sizeTextField.getText());
            final int lenPending = (int) Double
                    .parseDouble(startingLenTextField.getText());
            final DifficultyLevels diffPending =
                    (DifficultyLevels) difficultyComboBox
                    .getSelectedItem();
            final FoodScarcityLevels foodPending =
                    (FoodScarcityLevels) foodQuantityComboBox
                    .getSelectedItem();

            // if illegal arguments, set value back to default values
            if ((lenPending < 1) || (sizePending < TOO_SMALL_FIELD_SIZE)) {
                setTextFieldValues(fIELdSIZE + 1, startLength, difficulty,
                        foodQuantity);
            } else {
                validInput = true;
                fIELdSIZE = sizePending;
                if (lenPending > (fIELdSIZE / 2)) {
                    startLength = fIELdSIZE / 2;
                    growCount = lenPending - startLength;
                } else {
                    startLength = lenPending;
                }
                difficulty = diffPending;
                foodQuantity = foodPending;
            }
        }

    }

    // THREADS ===============================================================
    static final class FoodDispenser implements Runnable {

        private static final int FOOD_DISPENSE_FLUCTUATION = 200;

        @Override
        public void run () {
            while (true) {
                synchronized (syncLock) {
                    clearExpired();
                    int currentSweetSize = 0;
                    currentSweetSize = sweetPlaces.size();
                    if (currentSweetSize <= foodQuantity.val()) {
                        dispense();
                    }
                }

                // One dispense loop finished, sleep for next loop
                try {
                    Thread.sleep(FOOD_DISPENSE_CHECK_RESOLUTION
                            - rGen.nextInt(FOOD_DISPENSE_FLUCTUATION));
                } catch (final InterruptedException e) {
                    // Will always be interrupted by main thread when game ends
                    return;
                }
            }
        }

        /**
         * Clear all the expired food. These are bad for health.
         */
        private static void clearExpired () {
            final Iterator<Sweet> sweetItr = sweetPlaces.iterator();
            while (sweetItr.hasNext()) {
                final Sweet current = sweetItr.next();
                if (current.livedLoops++ >= current.loopsToLive) {
                    sweetItr.remove();
                }
            }
        }

        /**
         * Dispense sweet at random position but not on the body of the snake
         * nor at at position where a sweet already exists.
         */
        private static void dispense () {
            // DEBUG NOTE: have to compare the x and y values instead of
            // the Coordinate object. Don't know why it won't work.
            Coordinate randCor;
            rGenLoop: while (true) {
                randCor = new Coordinate(rGen.nextInt(fIELdSIZE),
                        rGen.nextInt(fIELdSIZE));

                // Check overlapped sweet
                final Iterator<Sweet> sweetItr = sweetPlaces.iterator();
                while (sweetItr.hasNext()) {
                    final Sweet thisSweet = sweetItr.next();
                    if ((thisSweet.x() == randCor.x)
                            && (thisSweet.y() == randCor.y)) {
                        continue rGenLoop;
                    }
                }

                // Check on snake body
                Node currentNode = head;
                while (currentNode != null) {
                    if ((currentNode.x() == randCor.x)
                            && (currentNode.y() == randCor.y)) {
                        continue rGenLoop;
                    }
                    currentNode = currentNode.next;
                }
                break;
            }

            // Choose a random sweet type and add it to the list
            SweetType randomType = null;
            if (!boringMode) {
                final double randomVal = rGen.nextDouble();
                if (randomVal > superProb) {
                    randomType = SweetType.SUPER;
                } else if (randomVal > xtraProb) {
                    randomType = SweetType.XTRA_CALORIE;
                } else {
                    randomType = SweetType.REGULAR;
                }
            } else {
                randomType = SweetType.REGULAR;
            }
            sweetPlaces.add(new Sweet(randCor.x, randCor.y, randomType));
        }

    }

    static final class RenderLoop implements Runnable {
        private static final double FAIRLY_THIN_PEN_RADIUS = 0.008;
        private static final double VERY_THIN_PEN_RADIUS = 0.0006;
        private static final int JUST_A_GREY_ISH_COLOR = 170;
        // # of fetchControl() executed after last stepAhead()
        // Used to control the speed of the snake
        private static int diffCount = 0;
        private static boolean superMode = false;

        @Override
        public void run () {
            makeStartingTurn();
            while (true) {
                makeTurn();

                if (diffCount++ == speedMultiplier) {
                    stepAhead();
                    if (growCount > 0) {
                        grow();
                    }
                    diffCount = 0;
                }

                if (snakeLen == ((fIELdSIZE + 1) * (fIELdSIZE + 1))) {
                    break;
                }

                if (!alive) {
                    break;
                }

                updateScene();
            }
            superMode = false;
        }

        private void makeStartingTurn () {
            head.heading = startDir;
            stepAhead();
        }

        /**
         * Fetch the control and then
         */
        static void makeTurn () {
            final Direction toGo = fetchControl();
            if ((toGo != null) && !toGo.equals(head.heading.opposite())
                    && !toGo.equals(head.heading)) {
                head.heading = toGo;
                stepAhead();
            }
        }

        /**
         * Move the whole snake by one position according to the direction
         * stored in each Node.
         */
        static void stepAhead () {
            // Always look before you go!
            lookAhead();

            // Now it's safe to go, so:
            // First, move each node to its heading position
            Node currentNode = head;
            while (currentNode != null) {
                currentNode.position = move(currentNode.position,
                        currentNode.heading);
                currentNode = currentNode.next;
            }

            // Then, from tail to head, update the node's heading to
            // be equal to the heading of the previous node.
            currentNode = tail;
            while (currentNode != head) {
                currentNode.heading = currentNode.prev.heading;
                currentNode = currentNode.prev;
            }

            diffCount = 0;
        }

        /**
         * Draw the current scene (snake and food)
         * Controls the dispensing of food
         */
        static void updateScene () {
            drawFrame();
            frameCount++;
            StdDraw.show(CONTROL_RESOLUTION);
        }

        /**
         * Draws the current frame
         */
        private static void drawFrame () {
            StdDraw.clear(StdDraw.BLACK);

            drawSnakeBody();

            drawFieldBorder();

            drawSweets();

            drawProgressGrid();

            // @scaffold draws grid lines for easy reading=====
            // StdDraw.setPenRadius(0.001);
            // for (int i = 0; i <= FIELD_SIZE; i++) {
            // StdDraw.text(0, i, Integer.toString(i));
            // StdDraw.text(i, 0, Integer.toString(i),60);
            // StdDraw.line(i, -1, i, FIELD_SIZE+1);
            // StdDraw.line(-1, i, FIELD_SIZE+1, i);
            // }

            setScoreBoard(calculateScore());
        }

        private static void drawProgressGrid () {
            if (!boringMode) {
                // Draw grid squares to represent snakeLen. When the whole field
                // is filled up, the player wins.
                final int rowsNum = snakeLen / (fIELdSIZE + 1);
                final int remainNum = snakeLen % (fIELdSIZE + 1);
                StdDraw.setPenRadius(VERY_THIN_PEN_RADIUS);
                for (int i = fIELdSIZE; i > (fIELdSIZE - rowsNum); i--) {
                    for (int j = 0; j <= fIELdSIZE; j++) {
                        StdDraw.square(j, i, HALF_CELL_WIDTH);
                    }
                }
                for (int j = 0; j < remainNum; j++) {
                    StdDraw.square(j, fIELdSIZE - rowsNum, HALF_CELL_WIDTH);
                }
            }
        }

        private static void drawSweets () {
            synchronized (syncLock) {
                final Iterator<Sweet> foodIterator = sweetPlaces.iterator();
                while (foodIterator.hasNext()) {
                    final Sweet oneSweet = foodIterator.next();
                    oneSweet.drawSelf();
                }
            }
        }

        /**
         * Draw the snake
         */
        private static void drawFieldBorder () {
            // Draw field outer border lines
            StdDraw.setPenRadius(FAIRLY_THIN_PEN_RADIUS);
            StdDraw.setPenColor(StdDraw.WHITE);
            StdDraw.line(-HALF_CELL_WIDTH, -HALF_CELL_WIDTH,
                    fIELdSIZE + HALF_CELL_WIDTH, -HALF_CELL_WIDTH);
            StdDraw.line(-HALF_CELL_WIDTH, -HALF_CELL_WIDTH, -HALF_CELL_WIDTH,
                    fIELdSIZE + HALF_CELL_WIDTH);
            StdDraw.line(fIELdSIZE + HALF_CELL_WIDTH,
                    fIELdSIZE + HALF_CELL_WIDTH, -HALF_CELL_WIDTH,
                    fIELdSIZE + HALF_CELL_WIDTH);
            StdDraw.line(fIELdSIZE + HALF_CELL_WIDTH,
                    fIELdSIZE + HALF_CELL_WIDTH, fIELdSIZE + HALF_CELL_WIDTH,
                    -HALF_CELL_WIDTH);
        }

        /**
         * Draw the snake body
         */
        private static void drawSnakeBody () {
            // Draw the snake head in red
            StdDraw.setPenColor(StdDraw.BOOK_RED);
            StdDraw.filledSquare(head.x(), head.y(), HALF_CELL_WIDTH);
            // Draw the rest of snake body
            if (superMode) {
                StdDraw.setPenColor(StdDraw.BOOK_LIGHT_BLUE);
            } else {
                StdDraw.setPenColor(new Color(JUST_A_GREY_ISH_COLOR,
                        JUST_A_GREY_ISH_COLOR, JUST_A_GREY_ISH_COLOR));
            }
            Node current = head.next;
            while (current != null) {
                StdDraw.filledSquare(current.x(), current.y(), HALF_CELL_WIDTH);
                current = current.next;
            }
        }

        /**
         * Check the position of head in the next loop for these conditions:
         * 1. open
         * 2. hit
         * 3. eat
         */
        static void lookAhead () {
            synchronized (syncLock) {
                // Figure out the position ahead
                final Coordinate nextHead = figureOutPositionAhead();

                // Will it eat a food?
                if (!sweetPlaces.isEmpty()) {
                    final Iterator<Sweet> sweetsIterator = sweetPlaces
                            .iterator();
                    while (sweetsIterator.hasNext()) {
                        final Sweet sweet = sweetsIterator.next();
                        if ((nextHead.x == sweet.x())
                                && (nextHead.y == sweet.y())) {
                            final SweetType thisType = sweet.type;
                            dealWithTheFood(thisType);
                            sweetsIterator.remove();
                            return;
                        }
                    }
                }

                // Will it hit the wall?
                if ((nextHead.x < 0) || (nextHead.x > fIELdSIZE)
                        || (nextHead.y < 0) || (nextHead.y > fIELdSIZE)) {
                    alive = false;
                    return;
                }

                // Will it hit its body?
                if (superMode) {
                    Node nowLooking = head.next;
                    while (nowLooking != null) {
                        if ((nextHead.x == nowLooking.x())
                                && (nextHead.y == nowLooking.y())) {
                            tail = nowLooking.prev;
                            tail.next = null;
                            superMode = false;
                            return;
                        }
                        nowLooking = nowLooking.next;
                    }
                } else {
                    Node nowLooking = head.next;
                    while (nowLooking != null) {
                        if ((nextHead.x == nowLooking.x())
                                && (nextHead.y == nowLooking.y())) {
                            alive = false;
                            return;
                        }
                        nowLooking = nowLooking.next;
                    }
                }

            }

        }

        private static void dealWithTheFood (final SweetType thisType) {
            switch (thisType) {
            case REGULAR:
                growCount += 1;
                break;
            case XTRA_CALORIE:
                growCount += fIELdSIZE / 2;
                break;
            case SUPER:
                if (superMode) {
                    superMode = false;
                } else {
                    superMode = true;
                }
                break;
            default:
                // unreachable
                break;
            }
        }

        /**
         * CheckStyle says the NPath complexity is too high
         *
         * @return
         */
        private static Coordinate figureOutPositionAhead () {
            Coordinate nextHead;
            switch (head.heading) {
            case U:
                nextHead = new Coordinate(head.x(), head.y() + 1);
                break;
            case D:
                nextHead = new Coordinate(head.x(), head.y() - 1);
                break;
            case L:
                nextHead = new Coordinate(head.x() - 1, head.y());
                break;
            case R:
                nextHead = new Coordinate(head.x() + 1, head.y());
                break;
            default:
                // Will get here only if heading is null
                nextHead = new Coordinate(Integer.MAX_VALUE, Integer.MAX_VALUE);
                break;
            }
            return nextHead;
        }

        /**
         * Grow in length by one by adding a node to the tail
         */
        static void grow () {
            linkLast(tail.heading.opposite());
            snakeLen++;
            growCount--;
        }

    }

}

/*
 * DATA STRUCTURES ============================================================
 */
/**
 * For representing four directions.
 */
enum Direction {
    U(KeyEvent.VK_UP), D(KeyEvent.VK_DOWN), L(KeyEvent.VK_LEFT), R(
            KeyEvent.VK_RIGHT);

    int keyVal;

    private Direction(final int keyValue) {
        this.keyVal = keyValue;
    }

    public Direction opposite () {
        switch (this) {
        case U:
            return D;
        case D:
            return U;
        case L:
            return R;
        case R:
            return L;
        default:
            // Unreachable
            return U;
        }
    }

}

/**
 * Types of sweets
 */
enum SweetType {
    REGULAR, XTRA_CALORIE, SUPER
}

/**
 * Difficulty levels
 */
enum DifficultyLevels {
    EASY(9), REGULAR(5), HARD(2);

    private int multiplierVal;

    private DifficultyLevels(final int val) {
        this.multiplierVal = val;
    }

    public int val () {
        return this.multiplierVal;
    }
}

enum FoodScarcityLevels {
    SCARCE(0.01), ABUNDANT(0.05), EXTRA(0.1);

    private static final double ONE_POINT_FIVE = 1.5;
    private double quantity;

    private FoodScarcityLevels(final double quant) {
        this.quantity = quant;
    }

    public double val () {
        return Math.ceil(
                this.quantity * (((Snake.fIELdSIZE + 1) * (Snake.fIELdSIZE + 1))
                        - Snake.snakeLen / ONE_POINT_FIVE));
    }
}

/**
 * struct Coordinate: for representing position.
 */
final class Coordinate {
    final int x;
    final int y;

    public Coordinate(final int xCor, final int yCor) {
        x = xCor;
        y = yCor;
    }
}

final class Sweet {
    private static final double POINT_EIGHT_O_NINE = 0.809;
    private static final double POINT_THREE_O_NINE = 0.309;
    private static final double POINT_FIVE_EIGHT_EIGHT = 0.588;
    private static final double POINT_NINE_ONE_FIVE = 0.951;
    private static final double ZERO_POINT_SIX = 0.6;
    private static final int TWO_HUNDRED = 200;
    private static final int SEVEN = 7;
    private static final int TEN = 10;
    // Maximum seconds to live: 15 s; Minimum: 7 s
    final int loopsToLive;
    final Coordinate position;
    int livedLoops;
    SweetType type;

    public Sweet(final int xCor, final int yCor, final SweetType theType) {
        position = new Coordinate(xCor, yCor);
        livedLoops = 0;
        type = theType;

        switch (theType) {
        case REGULAR:
            loopsToLive = ((Snake.MILLIS_IN_A_SECOND
                    * (Snake.rGen.nextInt(TEN) + TEN))
                    / (Snake.FOOD_DISPENSE_CHECK_RESOLUTION + TWO_HUNDRED)) * 2;
            break;
        case SUPER:
            loopsToLive = ((Snake.MILLIS_IN_A_SECOND * SEVEN)
                    / (Snake.FOOD_DISPENSE_CHECK_RESOLUTION + TWO_HUNDRED)) * 2;
            break;
        case XTRA_CALORIE:
            loopsToLive = ((Snake.MILLIS_IN_A_SECOND
                    * (Snake.rGen.nextInt(TEN) + SEVEN))
                    / (Snake.FOOD_DISPENSE_CHECK_RESOLUTION + TWO_HUNDRED)) * 2;
            break;
        default:
            // Unreachable
            loopsToLive = 0;
            break;
        }
    }

    public int x () {
        return position.x;
    }

    public int y () {
        return position.y;
    }

    public void drawSelf () {
        switch (this.type) {
        case REGULAR:
            StdDraw.filledCircle(this.x(), this.y(),
                    Snake.HALF_CELL_WIDTH * ZERO_POINT_SIX);
            break;
        case XTRA_CALORIE:
            StdDraw.circle(this.x(), this.y(), Snake.HALF_CELL_WIDTH);
            break;
        case SUPER:
            StdDraw.setPenColor(StdDraw.BOOK_BLUE);

            final double[] xVals = {
                    this.x(),
                    this.x() - (POINT_NINE_ONE_FIVE * Snake.HALF_CELL_WIDTH),
                    this.x() - (POINT_FIVE_EIGHT_EIGHT * Snake.HALF_CELL_WIDTH),
                    this.x() + (POINT_FIVE_EIGHT_EIGHT * Snake.HALF_CELL_WIDTH),
                    this.x() + (POINT_NINE_ONE_FIVE * Snake.HALF_CELL_WIDTH) };
            final double[] yVals = {
                    this.y() - Snake.HALF_CELL_WIDTH,
                    this.y() - (POINT_THREE_O_NINE * Snake.HALF_CELL_WIDTH),
                    this.y() + (POINT_EIGHT_O_NINE * Snake.HALF_CELL_WIDTH),
                    this.y() + (POINT_EIGHT_O_NINE * Snake.HALF_CELL_WIDTH),
                    this.y() - (POINT_THREE_O_NINE * Snake.HALF_CELL_WIDTH) };
            StdDraw.filledPolygon(xVals, yVals);
            // StdDraw.filledCircle(this.x(), this.y(), Snake.HALF_CELL_WIDTH);

            StdDraw.setPenColor(StdDraw.WHITE);
            break;
        default:
            // unreachable
            break;
        }
    }
}

/**
 * struct Node: for representing snake's body
 */
final class Node {
    Direction heading;
    Coordinate position;
    Node prev;
    Node next;

    public Node(final int xCor, final int yCor, final Direction headTo) {
        position = new Coordinate(xCor, yCor);
        heading = headTo;
    }

    public int x () {
        return position.x;
    }

    public int y () {
        return position.y;
    }
}
