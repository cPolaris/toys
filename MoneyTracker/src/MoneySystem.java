import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Calendar;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class MoneySystem {

    // Reading and saving functionalities
    private LinkedList<Entry> archiveList;
    private int archiveListSize;
    private LinkedList settingsList;

    // In settings
    private double currentExchangeRate;

    // In Main window
    private JFrame mainFrame;
    private JPanel statusPanel;
    private JPanel entryPanel;
    private JPanel viewOptionsPanel;
    private JPanel buttonsPanel;
    private JList entryList;
    private JLabel blankLabel;
    private DefaultListModel<String> entryListModel;
    private JTextField liveEntryTextField;
    private JTextField totalInTextField;
    private JTextField totalOutTextField;
    private JTextField monthInTextField;
    private JTextField monthOutTextField;
    private JTextField totalIMinusOTextField;
    private JTextField monthIMinusOTextField;
    private JTextField totalExchangeLossTextField;
    private JTextField lowerBoundAmountTextField;
    private JTextField upperBoundAmountTextField;
    private String refreshedDate;
    private Entry theNewEntry;

    // In New Entry window
    private JFrame newEntryFrame;
    private Calendar setDateCalendar = Calendar.getInstance();
    private final int THISYEAR = setDateCalendar.get(Calendar.YEAR);
    private final int THISMONTH = setDateCalendar.get(Calendar.MONTH);
    private final int THISDAY = setDateCalendar.get(Calendar.DATE);

    private JComboBox monthComboBox;
    private JComboBox dayComboBox;
    private JComboBox yearComboBox;
    private JCheckBox inCNY;
    private JCheckBox moneyIn;
    private JTextField amountTextField;
    private JList<String> categoryIList;
    private JList<String> categoryIIList;
    private JTextField descriptionText;

    private JFrame settingsFrame;
    private JButton newEntryButton = new JButton("New Entry");
    private JButton settingsButton = new JButton("System Settings");
    private JButton changeArchiveButton = new JButton("Change Archive");
    private JLabel currentExchangeRateLabel;
    private JLabel refreshedDateLabel;

    private boolean rateDidRefreshed = false;
    private int refreshErrorCode = 0;     // 0-success 1-offline 2-timeout
    private int selectedDataSource;

    public static void main(String[] args) {
        MoneySystem oneSystem = new MoneySystem();
        oneSystem.readArchive();
        oneSystem.readSettings();
        oneSystem.newEntryButton.setEnabled(false);
        oneSystem.settingsButton.setEnabled(false);
        oneSystem.changeArchiveButton.setEnabled(false);

//         Refresh data in a new thread
        ExchangeRateRefresher refresher = oneSystem.new ExchangeRateRefresher();
        Thread refreshData = new Thread(refresher);
        refreshData.start();

        oneSystem.buildMainGUI();

        int sleepCount = 0;
        while (true) {
            if (oneSystem.rateDidRefreshed) {
                break;
            }
            try {
                sleepCount++;
                Thread.sleep(1000);
                if (oneSystem.selectedDataSource == 0) {
                    oneSystem.refreshedDateLabel.setText("度娘... " + (15 - sleepCount));
                } else {
                    oneSystem.refreshedDateLabel.setText("雅虎... " + (15 - sleepCount));
                }
                // (/ω＼)　(/ω・＼)
                if (sleepCount % 2 != 0) {
                    oneSystem.blankLabel.setText("              (/ ω ＼)ﾞ");
                } else {
                    oneSystem.blankLabel.setText("              (/ ω °＼)");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        oneSystem.newEntryButton.setEnabled(true);
        oneSystem.settingsButton.setEnabled(true);
        oneSystem.changeArchiveButton.setEnabled(true);

        oneSystem.loadExchangeRate();
        oneSystem.loadTotalStats();
    }

    private void buildMainGUI() {
        mainFrame = new JFrame("Money Tracker System v0.2");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        statusPanel = new JPanel();
        entryPanel = new JPanel();
        viewOptionsPanel = new JPanel();
        buttonsPanel = new JPanel();

        // configurations
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        entryPanel.setLayout(new BoxLayout(entryPanel, BoxLayout.Y_AXIS));
        viewOptionsPanel.setLayout(new BoxLayout(viewOptionsPanel, BoxLayout.Y_AXIS));

        // Items in statusPanel
        blankLabel = new JLabel("              ヾ(◍°∇°◍)ﾉﾞ");
        JLabel totalInLabel = new JLabel("Total In $");
        JLabel totalOutLabel = new JLabel("Total Out $");
        JLabel thisMonthInLabel = new JLabel("Current Month In $");
        JLabel thisMonthOutLabel = new JLabel("Current Month Out $");
        JLabel totalIMinusOLabel = new JLabel("Total In - Out $");
        JLabel monthIMinusOLabel = new JLabel("Current Month In - Out $");
        JLabel totalExchangeLossLabel = new JLabel("Total Exchange Loss $");
        JLabel numsOfEntriesLabel = new JLabel("Live entries count");
        String dateToday = String.format("➠ %d/%d/%d", THISMONTH + 1, THISDAY, THISYEAR);
        JLabel todaysDate = new JLabel(dateToday);

        currentExchangeRateLabel = new JLabel();
        refreshedDateLabel = new JLabel();
        liveEntryTextField = new JTextField(8);
        totalInTextField = new JTextField(8);
        totalOutTextField = new JTextField(8);
        monthInTextField = new JTextField(8);
        monthOutTextField = new JTextField(8);
        totalIMinusOTextField = new JTextField(8);
        monthIMinusOTextField = new JTextField(8);
        totalExchangeLossTextField = new JTextField(8);

        liveEntryTextField.setEditable(false);
        totalInTextField.setEditable(false);
        totalOutTextField.setEditable(false);
        monthInTextField.setEditable(false);
        monthOutTextField.setEditable(false);
        totalIMinusOTextField.setEditable(false);
        monthIMinusOTextField.setEditable(false);
        totalExchangeLossTextField.setEditable(false);

        statusPanel.add(todaysDate);
        statusPanel.add(currentExchangeRateLabel);
        statusPanel.add(refreshedDateLabel);
        statusPanel.add(blankLabel);
        statusPanel.add(numsOfEntriesLabel);
        statusPanel.add(liveEntryTextField);
        statusPanel.add(totalInLabel);
        statusPanel.add(totalInTextField);
        statusPanel.add(totalOutLabel);
        statusPanel.add(totalOutTextField);
        statusPanel.add(thisMonthInLabel);
        statusPanel.add(monthInTextField);
        statusPanel.add(thisMonthOutLabel);
        statusPanel.add(monthOutTextField);
        statusPanel.add(totalIMinusOLabel);
        statusPanel.add(totalIMinusOTextField);
        statusPanel.add(monthIMinusOLabel);
        statusPanel.add(monthIMinusOTextField);
        statusPanel.add(totalExchangeLossLabel);
        statusPanel.add(totalExchangeLossTextField);

        // Items in entryPanel
        entryList = new JList();
        loadEntireEntryList();
        entryList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        entryList.setFont(new Font("Lucida Console", Font.PLAIN, 12));
        JScrollPane entryScroller = new JScrollPane(entryList);
        JLabel entryListLabel = new JLabel("Recent Entries");

        // configurations
        entryScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        entryPanel.add(entryListLabel);
        entryPanel.add(entryScroller);

        // Items in viewoptionsPanel
        JLabel optionLabel = new JLabel("Operations");
        JButton viewButton = new JButton("View All");
        JButton filterButton = new JButton("Apply Filter");
        JButton deleteEntryButton = new JButton("Delete Selected");

        viewButton.addActionListener(new ViewButtonListener());
        filterButton.addActionListener(new FilterButtonListener());
        deleteEntryButton.addActionListener(new DeleteEntryListener());
        viewOptionsPanel.add(optionLabel);
        viewOptionsPanel.add(filterButton);
        viewOptionsPanel.add(viewButton);
        viewOptionsPanel.add(deleteEntryButton);

        // Items in buttonsPanel
        newEntryButton.addActionListener(new NewEntryListener());
        settingsButton.addActionListener(new SettingsListener());
        changeArchiveButton.addActionListener(new ChangeButtonListener());

        buttonsPanel.add(newEntryButton);
        buttonsPanel.add(settingsButton);
        buttonsPanel.add(changeArchiveButton);
        // Add panels to mainFrame
        mainFrame.getContentPane().add(BorderLayout.WEST, statusPanel);
        mainFrame.getContentPane().add(BorderLayout.CENTER, entryPanel);
        mainFrame.getContentPane().add(BorderLayout.EAST, viewOptionsPanel);
        mainFrame.getContentPane().add(BorderLayout.SOUTH, buttonsPanel);

        mainFrame.getRootPane().setDefaultButton(newEntryButton);
        mainFrame.setSize(1100, 600);

        entryList.setSelectedIndex(entryListModel.getSize() - 1);
        entryList.ensureIndexIsVisible(entryListModel.getSize() - 1);

        mainFrame.setVisible(true);
    }


    private class ExchangeRateRefresher implements Runnable {
        public void run() {
            switch (selectedDataSource) {
                case 0:
                    // http://finance.yahoo.com/d/quotes.csv?e=.csv&f=sl1d1t1&s=USDCNY=X
                    String yahooUrlString = "http://finance.yahoo.com/d/quotes.csv?e=.csv&f=sl1d1t1&s=USDCNY=X";

                    try {
                        URL queryURL = new URL(yahooUrlString);
                        HttpURLConnection connection = (HttpURLConnection) queryURL.openConnection(Proxy.NO_PROXY);
                        connection.setConnectTimeout(15000);
                        System.out.println("CONNECTING... Yahoo Finance");

                        connection.connect();

                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        String unprocessedString = reader.readLine();
                        String[] stringArray = unprocessedString.split(",");

                        reader.close();
                        connection.disconnect();

                        currentExchangeRate = Double.parseDouble(stringArray[1]);
                        refreshedDate = "@ " + removeBrackets(stringArray[2]) + removeBrackets(stringArray[3]);

                    } catch (SocketTimeoutException timeout) {
                        System.out.println("*** Timeout ***");
                        refreshErrorCode = 2;
                        // (＃°Д°)  ╯︵┴─┴
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (UnknownHostException uhe) {
                        System.out.println("*** Not Connected to Internet ***");
                        refreshErrorCode = 3;
                    } catch (ConnectException ce) {
                        ce.printStackTrace();
                        System.out.println("*** CONNECTION FALIURE ***");
                        refreshErrorCode = 1;
                    } catch (IOException exp) {
                        exp.printStackTrace();
                    }

                    break;
                case 1:
                    // http://apis.baidu.com/apistore/currencyservice/currency
                    String baiduUrlString = "http://apis.baidu.com/apistore/currencyservice/currency?fromCurrency=USD&toCurrency=CNY";
                    StringBuffer baiduResultBuffer = new StringBuffer();

                    try {
                        URL baiduUrl = new URL(baiduUrlString);
                        HttpURLConnection baiduConnection = (HttpURLConnection) baiduUrl.openConnection();
                        baiduConnection.setRequestMethod("GET");
                        baiduConnection.setConnectTimeout(15000);
                        // 填入apikey到HTTP header
                        baiduConnection.setRequestProperty("apikey", "65573676fee42bb5557b71fa3c887048");

                        System.out.println("CONNECTING... Baidu API");
                        baiduConnection.connect();

                        BufferedReader baiduReader = new BufferedReader(
                                new InputStreamReader(baiduConnection.getInputStream(), "UTF-8"));
                        String strRead = null;
                        while ((strRead = baiduReader.readLine()) != null) {
                            baiduResultBuffer.append(strRead);
                            baiduResultBuffer.append("\r\n");
                        }
                        baiduReader.close();
                        baiduConnection.disconnect();
                        String baiduResultString = baiduResultBuffer.toString();
                        String[] baiduResultArray = baiduResultString.split("\"\\w+\":");

                        String dateStrNotProcessed = baiduResultArray[4];
                        String[] dateArray = dateStrNotProcessed.split("/");
                        String monthStr = dateArray[0];
                        String dayStr = dateArray[1];
                        String yearStr = dateArray[2];
                        String rateStr = baiduResultArray[9];
                        String timeStr = baiduResultArray[5];

                        currentExchangeRate = Double.parseDouble(rateStr.substring(0, rateStr.length() - 1));
                        refreshedDate = String.format(" @ ︎%s/%s/%s %s", monthStr.substring(1, monthStr.length() - 1),
                                dayStr.substring(0, dayStr.length() - 1), yearStr.substring(0, yearStr.length() - 2),
                                timeStr.substring(1, timeStr.length() - 2));

                    } catch (SocketTimeoutException timeout) {
                        System.out.println("*** Timeout ***");
                        //                 "              ヾ(◍°∇°◍)ﾉﾞ"
                        refreshErrorCode = 2;
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (UnknownHostException uhe) {
                        System.out.println("*** Not Connected to Internet ***");
                        refreshErrorCode = 3;
                    } catch (ConnectException ce) {
                        ce.printStackTrace();
                        System.out.println("*** CONNECTION FALIURE ***");
                        refreshErrorCode = 1;
                    } catch (IOException exp) {
                        exp.printStackTrace();
                    }
                    break;

                default:
                    System.out.println("Unreachable");
                    break;
            }

            System.out.println(" [Exchange rate refreshed] ");
            settingsList.set(0, null);
            settingsList.set(1, null);
            settingsList.set(0, currentExchangeRate);
            settingsList.set(1, refreshedDate);
            saveSettings();
            rateDidRefreshed = true;
        }
    }

    private void loadExchangeRate() {
        currentExchangeRateLabel.setText(String.format(" $ 1 = ¥ %6.3f", currentExchangeRate));
        if (refreshErrorCode == 0) {
            blankLabel.setText("    [Updated]  ヾ(◍°∇°◍)ﾉﾞ");
        } else if (refreshErrorCode == 1) {
            // ┏(゜ロ゜;)┛
            blankLabel.setText("[Connection Failed] ┏(゜ロ゜;)┛ﾞ");
        } else if (refreshErrorCode == 2) {
            blankLabel.setText("    [TimeOut]  (#°Д°)╯︵┴┴");
        } else if (refreshErrorCode == 3) {
            blankLabel.setText("    [Offline] ┏(゜ロ゜;)┛ﾞ");
        }
        refreshedDateLabel.setText(refreshedDate);
    }

    private void setCurrentFlowNumber() {
        String currentFlowNum = String.format("%06d", archiveListSize);
        Entry.setFlowNumber(currentFlowNum);
    }

    private String removeBrackets(String input) {
        return input.substring(1, input.length() - 1);
    }

    private void refreshTotalStats() {
        readArchive();
        readSettings();
        totalInTextField.setText("");
        totalOutTextField.setText("");
        monthInTextField.setText("");
        monthOutTextField.setText("");
        totalExchangeLossTextField.setText("");
        loadExchangeRate();
        loadTotalStats();
    }


    private void loadTotalStats() {
        int liveEntryCount = 0;
        double totalIn = 0;
        double totalOut = 0;
        double monthIn = 0;
        double monthOut = 0;
        double totalExchangeLoss = 0;
        Calendar tempCal = Calendar.getInstance();

        for (int i = 0; i < archiveListSize; i++) {
            Entry entryToProcess = (Entry) archiveList.get(i);
            if (entryToProcess == null) {
                continue;
            }

            liveEntryCount++;
            double amountUSD = entryToProcess.getAmountInUSD();
            double originalAmountCNY = entryToProcess.getAmountInCNY();  // Calculated with original exchange rate
            double currentAmountCNY = entryToProcess.getAmountInUSD() * currentExchangeRate;
            tempCal.setTimeInMillis(entryToProcess.getTimeStamp());

            // Count total EL
            totalExchangeLoss += originalAmountCNY - currentAmountCNY;
            // Count total IO and monthly IO
            if (entryToProcess.isOutput()) {
                totalOut += amountUSD;

                if (tempCal.get(Calendar.YEAR) == THISYEAR && tempCal.get(Calendar.MONTH) == THISMONTH) {
                    monthOut += amountUSD;
                }
            } else {
                totalIn += entryToProcess.getAmountInUSD();

                if (tempCal.get(Calendar.YEAR) == THISYEAR && tempCal.get(Calendar.MONTH) == THISMONTH) {
                    monthIn += amountUSD;
                }
            }

        }

        liveEntryTextField.setText(Integer.toString(liveEntryCount));
        totalInTextField.setText(String.format("%.2f", totalIn));
        totalOutTextField.setText(String.format("%.2f", totalOut));
        monthInTextField.setText(String.format("%.2f", monthIn));
        monthOutTextField.setText(String.format("%.2f", monthOut));
        totalIMinusOTextField.setText(String.format("%.2f", totalIn - totalOut));
        monthIMinusOTextField.setText(String.format("%.2f", monthIn - monthOut));
        totalExchangeLossTextField.setText(String.format("%.2f", totalExchangeLoss / currentExchangeRate));
        System.out.println(" [Stats loaded!] ");
    }


    private void loadEntireEntryList() {
        entryListModel = new DefaultListModel<String>();

        for (int i = 0; i < archiveListSize; i++) {
            Entry currentEntryToLoad = (Entry) archiveList.get(i);
            if (currentEntryToLoad == null) continue;

            entryListModel.addElement(currentEntryToLoad.toString());
        }

        entryList.setModel(entryListModel);
        entryList.setSelectedIndex(entryListModel.getSize() - 1);
        entryList.ensureIndexIsVisible(entryListModel.getSize() - 1);

        System.out.println(" [All entries loaded!] ");
    }


    private void saveArchive() {
        try {
            ObjectOutputStream objectOutput = new ObjectOutputStream(new FileOutputStream("MoneyArchive.msar"));
            objectOutput.writeObject(archiveList);
            System.out.println(" [Archive saved!] ");
            objectOutput.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("FATAL ERROR");
        }
    }

//   private void addToEntryPanel(Entry entryToAdd) {
//   	entryListContent.add(entryToAdd);
//   }

    private void readArchive() {
        try {
            ObjectInputStream objectInput = new ObjectInputStream(new FileInputStream("MoneyArchive.msar"));
            archiveList = (LinkedList<Entry>) objectInput.readObject();
            System.out.println(" [Archive Loaded!] ");
            objectInput.close();
            archiveListSize = archiveList.size();
            setCurrentFlowNumber();
        } catch (FileNotFoundException e) {
            System.out.println("First run. No exsisting library.");
            archiveList = new LinkedList<Entry>();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void saveSettings() {
        try {
            ObjectOutputStream objectOutput = new ObjectOutputStream(new FileOutputStream("Settings.mss"));
            objectOutput.writeObject(settingsList);
            System.out.println(" [Settings Saved!] ");
            objectOutput.close();
        } catch (Exception exc) {
            exc.printStackTrace();
            System.out.println("FATAL ERROR");
        }
    }

    private void readSettings() {
        try {
            ObjectInputStream objectInput = new ObjectInputStream(new FileInputStream("Settings.mss"));
            settingsList = (LinkedList) objectInput.readObject();
            System.out.println(" [Settings Loaded!] ");
            objectInput.close();
            // Set up
            currentExchangeRate = (double) settingsList.get(0);
            refreshedDate = (String) settingsList.get(1);
            selectedDataSource = (int) settingsList.get(2);

        } catch (FileNotFoundException e) {
            System.out.println("First run. No exsisting settings library.");
            settingsList = new LinkedList();

            // Set default values
            currentExchangeRate = 6.200;
            refreshedDate = "↑ Default Value ↑";
            // Put default values in settings LinkedList
            settingsList.add(currentExchangeRate);
            settingsList.add(refreshedDate);
            settingsList.add(0);
            // Save the settings
            try {
                ObjectOutputStream objectOutput = new ObjectOutputStream(new FileOutputStream("Settings.mss"));
                objectOutput.writeObject(settingsList);
                System.out.println(" [Settings Saved!] ");
                objectOutput.close();
            } catch (Exception exc) {
                exc.printStackTrace();
                System.out.println("FATAL ERROR");
            }

            return;

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Load values from settings file
        currentExchangeRate = (double) settingsList.get(0);
        refreshedDate = (String) settingsList.get(1);
    }

    private void setYearComboBox() {
        yearComboBox = new JComboBox();
        for (int i = THISYEAR - 2; i <= THISYEAR + 2; i++) {
            yearComboBox.addItem(Integer.toString(i));
        }
    }

    private void setDayComboBox() {
        dayComboBox = new JComboBox();
        for (int i = 1; i <= setDateCalendar.getActualMaximum(setDateCalendar.DAY_OF_MONTH); i++) {
            dayComboBox.addItem(Integer.toString(i));
        }
    }

    private int listModelIndexToArchiveListIndex(int listModelIndex) {
        String stringToProcess = entryListModel.get(listModelIndex);
        String[] stringProcessed = stringToProcess.split(" | ", 2);
        return Integer.parseInt(stringProcessed[0]) - 1;
    }

    // Button action listeners
    private class ViewButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            loadEntireEntryList();
        }
    }

    private class DeleteEntryListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            int indexToDelete = entryList.getSelectedIndex();
            int indexToSetNull = listModelIndexToArchiveListIndex(indexToDelete);

            entryListModel.remove(indexToDelete);
            archiveList.set(indexToSetNull, null);
            if (indexToDelete == 0) {
                entryList.setSelectedIndex(0);
            } else {
                entryList.setSelectedIndex(indexToDelete - 1);
            }

            saveArchive();
            loadTotalStats();
        }
    }

    private class FilterButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            JFrame filterFrame = new JFrame("Filter Entries");
            JPanel filterPanel = new JPanel();
            filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));

            JLabel titleLabel = new JLabel("Filter By:");
            JLabel dateFilterLabel = new JLabel("Date");
            JLabel catFilterLabel = new JLabel("Category");
            JLabel amtFilterLabel = new JLabel("Amount  $ ");
            JLabel desFilterLabel = new JLabel("Description");

            Box dateFilterBox = new Box(BoxLayout.X_AXIS);
            Box catFilterBox = new Box(BoxLayout.X_AXIS);
            Box amountFilterBox = new Box(BoxLayout.X_AXIS);
            Box desFilterBox = new Box(BoxLayout.X_AXIS);

            // by date
            setYearComboBox();

            String[] monthsArray = {"January", "February", "March", "April", "May",
                    "June", "July", "August", "September", "October",
                    "November", "December"};
            monthComboBox = new JComboBox(monthsArray);

            setDayComboBox();

            monthComboBox.addItem(null);
            dayComboBox.addItem(null);
            yearComboBox.addItem(null);

            // Default is null
            monthComboBox.setSelectedIndex(monthComboBox.getItemCount() - 1);
            dayComboBox.setSelectedIndex(dayComboBox.getItemCount() - 1);
            yearComboBox.setSelectedIndex(yearComboBox.getItemCount() - 1);

            monthComboBox.setEditable(false);
            dayComboBox.setEditable(false);
            yearComboBox.setEditable(false);

            monthComboBox.setEnabled(false);
            dayComboBox.setEnabled(false);

            yearComboBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    monthComboBox.setEnabled(true);
                    try {
                        setDateCalendar.set(setDateCalendar.YEAR, yearComboBox.getSelectedIndex() - 2 + THISYEAR);
                    } catch (Exception exc) {
                        exc.printStackTrace();
                    }
                    int daysInThisMonth = setDateCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    dayComboBox.removeAllItems();
                    for (int i = 1; i <= daysInThisMonth; i++) {
                        dayComboBox.addItem(Integer.toString(i));
                    }

                    dayComboBox.addItem(null);
                    dayComboBox.setSelectedIndex(dayComboBox.getItemCount() - 1);
                }
            });

            monthComboBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    dayComboBox.setEnabled(true);
                    try {
                        setDateCalendar.set(setDateCalendar.MONTH, monthComboBox.getSelectedIndex());
                    } catch (Exception exx) {
                        exx.printStackTrace();
                    }
                    int daysInThisMonth = setDateCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                    dayComboBox.removeAllItems();
                    for (int i = 1; i <= daysInThisMonth; i++) {
                        dayComboBox.addItem(Integer.toString(i));
                    }

                    dayComboBox.addItem(null);
                    dayComboBox.setSelectedIndex(dayComboBox.getItemCount() - 1);
                }
            });

            dateFilterBox.add(dateFilterLabel);
            dateFilterBox.add(yearComboBox);
            dateFilterBox.add(monthComboBox);
            dateFilterBox.add(dayComboBox);


            // by category
            categoryIList = new JList<String>();
            categoryIIList = new JList<String>();
            JScrollPane categoryIScroller = new JScrollPane(categoryIList);
            JScrollPane categoryIIScroller = new JScrollPane(categoryIIList);

            categoryIScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            categoryIScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            categoryIList.setFixedCellWidth(20);
            categoryIList.setVisibleRowCount(6);
            categoryIList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            categoryIIScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            categoryIIScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            categoryIIList.setFixedCellWidth(20);
            categoryIIList.setVisibleRowCount(6);
            categoryIIList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            // Category box contents
            DefaultListModel<String> categoryIListModel = new DefaultListModel<String>();
            DefaultListModel<String> categoryIIListModel = new DefaultListModel<String>();

            categoryIListModel.addElement("Meal");
            categoryIListModel.addElement("Transportation");
            categoryIListModel.addElement("WIRE");
            categoryIListModel.addElement("Hardware");
            categoryIListModel.addElement("Software");
            categoryIListModel.addElement("Affair");
            categoryIList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent event) {
                    String catI = categoryIList.getSelectedValue();
                    switch (catI) {
                        case "Meal":
                            categoryIIListModel.clear();
                            categoryIIListModel.addElement("restaruant");
                            categoryIIListModel.addElement("meat");
                            categoryIIListModel.addElement("vegi/fruit");
                            categoryIIListModel.addElement("drink");
                            break;
                        case "Transportation":
                            categoryIIListModel.clear();
                            categoryIIListModel.addElement("ticket");
                            categoryIIListModel.addElement("gas");
                            break;
                        case "WIRE":
                            categoryIIListModel.clear();
                            categoryIIListModel.addElement("water");
                            categoryIIListModel.addElement("internet");
                            categoryIIListModel.addElement("rent/hotel");
                            categoryIIListModel.addElement("electricity");
                            break;
                        case "Hardware":
                            categoryIIListModel.clear();
                            categoryIIListModel.addElement("book");
                            categoryIIListModel.addElement("electronics");
                            categoryIIListModel.addElement("stationary");
                            categoryIIListModel.addElement("tool");
                            categoryIIListModel.addElement("decor");
                            break;
                        case "Software":
                            categoryIIListModel.clear();
                            categoryIIListModel.addElement("music");
                            categoryIIListModel.addElement("movie");
                            categoryIIListModel.addElement("application");
                            categoryIIListModel.addElement("decor");
                            break;
                        case "Affair":
                            categoryIIListModel.clear();
                            categoryIIListModel.addElement("tuition");
                            categoryIIListModel.addElement("bureaucratic");
                            categoryIIListModel.addElement("family");
                            break;
                        default:
                            System.out.println("This line should not be reached");
                            break;
                    }
                }
            });

            categoryIList.setModel(categoryIListModel);
            categoryIIList.setModel(categoryIIListModel);
            catFilterBox.add(catFilterLabel);
            catFilterBox.add(categoryIScroller);
            catFilterBox.add(categoryIIScroller);

            // by amount
            lowerBoundAmountTextField = new JTextField(null);
            upperBoundAmountTextField = new JTextField(null);
            JLabel curveLineLabel = new JLabel(" ~ ");

            amountFilterBox.add(amtFilterLabel);
            amountFilterBox.add(lowerBoundAmountTextField);
            amountFilterBox.add(curveLineLabel);
            amountFilterBox.add(upperBoundAmountTextField);

            // by description
            JTextField desFilterTextField = new JTextField();
            desFilterTextField.setColumns(200);
            desFilterBox.add(desFilterLabel);
            desFilterBox.add(desFilterTextField);

            // Apply button
            JButton applyButton = new JButton("Apply");

            applyButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    int selectedYear = 0;
                    int selectedMonth = 0;
                    int selectedDay = 0;

                    boolean haveYear = false;
                    boolean haveMonth = false;
                    boolean haveDay = false;
                    boolean haveCatI = false;
                    boolean haveCatII = false;
                    boolean haveLowerBound = false;
                    boolean haveUpperBound = false;

                    String catI = "";
                    String catII = "";

                    int lowerBound = 0;
                    int upperBound = 0;

                    Calendar tempCal = Calendar.getInstance();

                    // Filter Date
                    if (yearComboBox.getSelectedItem() != null) {
                        haveYear = true;
                        selectedYear = yearComboBox.getSelectedIndex() - 2 + THISYEAR;
                        System.out.println("year = " + selectedYear);
                    }
                    if (monthComboBox.getSelectedItem() != null) {
                        haveMonth = true;
                        selectedMonth = monthComboBox.getSelectedIndex();
                        System.out.println("month = " + selectedMonth);
                    }
                    if (dayComboBox.getSelectedItem() != null) {
                        haveDay = true;
                        selectedDay = dayComboBox.getSelectedIndex() + 1;
                        System.out.println("day = " + selectedDay);
                    }

                    if (haveYear && !haveMonth && !haveDay) {
                        for (int i = entryListModel.getSize() - 1; i >= 0; i--) {
                            Entry entryToFilter = archiveList.get(listModelIndexToArchiveListIndex(i));
                            tempCal.setTimeInMillis(entryToFilter.getTimeStamp());

                            if (tempCal.get(Calendar.YEAR) != selectedYear) {
                                entryListModel.remove(i);
                            }
                        }
                    } else if (haveYear && haveMonth && !haveDay) {
                        for (int i = entryListModel.getSize() - 1; i >= 0; i--) {
                            Entry entryToFilter = archiveList.get(listModelIndexToArchiveListIndex(i));
                            tempCal.setTimeInMillis(entryToFilter.getTimeStamp());

                            if (tempCal.get(Calendar.YEAR) != selectedYear) {
                                entryListModel.remove(i);
                            } else if (tempCal.get(Calendar.MONTH) != selectedMonth) {
                                entryListModel.remove(i);
                            }
                        }
                    } else if (haveYear && haveMonth && haveDay) {
                        for (int i = entryListModel.getSize() - 1; i >= 0; i--) {
                            Entry entryToFilter = archiveList.get(listModelIndexToArchiveListIndex(i));
                            tempCal.setTimeInMillis(entryToFilter.getTimeStamp());

                            if (tempCal.get(Calendar.YEAR) != selectedYear) {
                                entryListModel.remove(i);
                            } else if (tempCal.get(Calendar.MONTH) != selectedMonth) {
                                entryListModel.remove(i);
                            } else if (tempCal.get(Calendar.DATE) != selectedDay) {
                                entryListModel.remove(i);
                            }
                        }
                    }


                    // Filter Cat
                    if (categoryIList.getSelectedValue() != null) {
                        haveCatI = true;
                        catI = categoryIList.getSelectedValue();
                        System.out.println("catI is not null");
                    }
                    if (categoryIIList.getSelectedValue() != null) {
                        haveCatII = true;
                        catII = categoryIIList.getSelectedValue();
                        System.out.println("catII is not null");
                    }

                    if (haveCatI && !haveCatII) {
                        // only catI
                        for (int i = entryListModel.getSize() - 1; i >= 0; i--) {
                            Entry entryToFilter = archiveList.get(listModelIndexToArchiveListIndex(i));

                            if (entryToFilter.getCategoryI() == null) {
                                entryListModel.remove(i);
                            } else if (!entryToFilter.getCategoryI().equals(catI)) {
                                entryListModel.remove(i);
                            }
                        }
                    } else if (haveCatI && haveCatII) {
                        // both
                        for (int i = entryListModel.getSize() - 1; i >= 0; i--) {
                            Entry entryToFilter = archiveList.get(listModelIndexToArchiveListIndex(i));

                            if (entryToFilter.getCategoryI() == null) {
                                entryListModel.remove(i);
                            } else if (!entryToFilter.getCategoryI().equals(catI)) {
                                entryListModel.remove(i);
                            } else if (!entryToFilter.getCategoryII().equals(catII)) {
                                entryListModel.remove(i);
                            }
                        }
                    }


                    // Filter amount
                    if (!lowerBoundAmountTextField.getText().equalsIgnoreCase("")) {
                        haveLowerBound = true;
                        lowerBound = Integer.parseInt(lowerBoundAmountTextField.getText());
                    }
                    if (!upperBoundAmountTextField.getText().equalsIgnoreCase("")) {
                        haveUpperBound = true;
                        upperBound = Integer.parseInt(upperBoundAmountTextField.getText());
                    }


                    if (haveLowerBound && !haveUpperBound) {
                        // lower bound only
                        for (int i = entryListModel.getSize() - 1; i >= 0; i--) {
                            Entry entryToFilter = archiveList.get(listModelIndexToArchiveListIndex(i));

                            if (entryToFilter.getAmountInUSD() < lowerBound) {
                                entryListModel.remove(i);
                            }
                        }
                    } else if (!haveLowerBound && haveUpperBound) {
                        // upper bound only
                        for (int i = entryListModel.getSize() - 1; i >= 0; i--) {
                            Entry entryToFilter = archiveList.get(listModelIndexToArchiveListIndex(i));

                            if (entryToFilter.getAmountInUSD() > upperBound) {
                                entryListModel.remove(i);
                            }
                        }
                    } else if (haveLowerBound && haveUpperBound) {
                        // both bounds
                        for (int i = entryListModel.getSize() - 1; i >= 0; i--) {
                            Entry entryToFilter = archiveList.get(listModelIndexToArchiveListIndex(i));

                            if (entryToFilter.getAmountInUSD() < lowerBound) {
                                entryListModel.remove(i);
                            } else if (entryToFilter.getAmountInUSD() > upperBound) {
                                entryListModel.remove(i);
                            }
                        } // close for
                    } // close if

                    // By description
                    String filterText = desFilterTextField.getText();
                    if (!desFilterTextField.getText().equalsIgnoreCase("")) {
                        for (int i = entryListModel.getSize() - 1; i >= 0; i--) {
                            Entry entryToFilter = archiveList.get(listModelIndexToArchiveListIndex(i));
                            boolean matches = entryToFilter.getDescription().toUpperCase().contains(filterText.toUpperCase());
                            System.out.printf("%s < %s [%b]\n", entryToFilter.getDescription(), filterText, matches);
                            if (!matches) {
                                entryListModel.remove(i);
                            }
                        } // close for
                    } //close if

                } // close method
            });

            // Set up
            filterPanel.add(titleLabel);
            filterPanel.add(dateFilterBox);
            filterPanel.add(catFilterBox);
            filterPanel.add(amountFilterBox);
            filterPanel.add(desFilterBox);
            filterPanel.add(applyButton);

            filterFrame.getContentPane().add(filterPanel);
            filterFrame.getRootPane().setDefaultButton(applyButton);

            filterFrame.setSize(500, 300);
            filterFrame.setVisible(true);
        }
    }

    private class ChangeButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            try {
                JFileChooser archiveChooser = new JFileChooser();
                archiveChooser.showOpenDialog(mainFrame);
                ObjectInputStream objectInput = new ObjectInputStream(new FileInputStream(archiveChooser.getSelectedFile()));
                archiveList = (LinkedList<Entry>) objectInput.readObject();
                System.out.println(" [Archive Loaded!] ");
                objectInput.close();

                // Re-initialize
                saveArchive();
                archiveListSize = archiveList.size();
                setCurrentFlowNumber();
                loadTotalStats();
                loadEntireEntryList();
            } catch (NullPointerException npointer) {
                System.out.println("[Archive Change Canceled---]");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class NewEntryListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            newEntryFrame = new JFrame("New Entry");
            JPanel newEntryMainPanel = new JPanel();
            newEntryMainPanel.setLayout(new BoxLayout(newEntryMainPanel, BoxLayout.Y_AXIS));
            Box dateBox = new Box(BoxLayout.X_AXIS);
            Box isIOBox = new Box(BoxLayout.X_AXIS);
            Box amountBox = new Box(BoxLayout.X_AXIS);
            Box categoryBox = new Box(BoxLayout.X_AXIS);
            Box descriptionBox = new Box(BoxLayout.X_AXIS);

            // Items in dateBox
            JLabel dateLabel = new JLabel("Date");

            setDayComboBox();
            setYearComboBox();

            String[] monthsArray = {"January", "February", "March", "April", "May",
                    "June", "July", "August", "September", "October",
                    "November", "December"};
            monthComboBox = new JComboBox(monthsArray);

            dayComboBox.setSelectedIndex(THISDAY - 1);
            monthComboBox.setSelectedIndex(THISMONTH);
            yearComboBox.setSelectedIndex(2);

            monthComboBox.setEditable(false);
            dayComboBox.setEditable(false);
            yearComboBox.setEditable(false);

            monthComboBox.addItemListener(new MonthComboBoxListener());
            yearComboBox.addItemListener(new YearComboBoxListener());

            dateBox.add(dateLabel);
            dateBox.add(monthComboBox);
            dateBox.add(dayComboBox);
            dateBox.add(yearComboBox);

            // Items in isIOBox
            ButtonGroup ioCheckBoxes = new ButtonGroup();
            JCheckBox moneyOut = new JCheckBox("Out", true);
            moneyIn = new JCheckBox("In", false);
            ioCheckBoxes.add(moneyOut);
            ioCheckBoxes.add(moneyIn);

            isIOBox.add(moneyOut);
            isIOBox.add(moneyIn);

            // Items in amountBox
            JLabel amountLabel = new JLabel("Amount");
            amountTextField = new JTextField(12);

            ButtonGroup currencyCheckBoxes = new ButtonGroup();
            JCheckBox inUSD = new JCheckBox("USD", true);
            inCNY = new JCheckBox("CNY", false);
            currencyCheckBoxes.add(inUSD);
            currencyCheckBoxes.add(inCNY);

            amountBox.add(amountLabel);
            amountBox.add(amountTextField);
            amountBox.add(inUSD);
            amountBox.add(inCNY);

            JLabel exchangeRateLabel = new JLabel("Exchange Rate: " + currentExchangeRate);

            // Items in categoryBox
            JLabel categoryLabel = new JLabel("Category");
            categoryIList = new JList<String>();
            categoryIIList = new JList<String>();
            JScrollPane categoryIScroller = new JScrollPane(categoryIList);
            JScrollPane categoryIIScroller = new JScrollPane(categoryIIList);

            categoryIScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            categoryIScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            categoryIList.setFixedCellWidth(20);
            categoryIList.setVisibleRowCount(6);
            categoryIList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            categoryIIScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            categoryIIScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            categoryIIList.setFixedCellWidth(20);
            categoryIIList.setVisibleRowCount(6);
            categoryIIList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            // Category box contents
            DefaultListModel<String> categoryIListModel = new DefaultListModel<String>();
            DefaultListModel<String> categoryIIListModel = new DefaultListModel<String>();

            categoryIListModel.addElement("MEAL");
            categoryIListModel.addElement("TRANSPORTATION");
            categoryIListModel.addElement("WIRE");
            categoryIListModel.addElement("HARDWARE");
            categoryIListModel.addElement("SOFTWARE");
            categoryIListModel.addElement("AFFAIR");
            categoryIList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent event) {
                    String catI = categoryIList.getSelectedValue();
                    switch (catI) {
                        case "MEAL":
                            categoryIIListModel.clear();
                            categoryIIListModel.addElement("restaruant");
                            categoryIIListModel.addElement("meat");
                            categoryIIListModel.addElement("vegi/fruit");
                            categoryIIListModel.addElement("drink");
                            break;
                        case "TRANSPORTATION":
                            categoryIIListModel.clear();
                            categoryIIListModel.addElement("ticket");
                            categoryIIListModel.addElement("gas");
                            break;
                        case "WIRE":
                            categoryIIListModel.clear();
                            categoryIIListModel.addElement("water");
                            categoryIIListModel.addElement("internet");
                            categoryIIListModel.addElement("rent/hotel");
                            categoryIIListModel.addElement("electricity");
                            break;
                        case "HARDWARE":
                            categoryIIListModel.clear();
                            categoryIIListModel.addElement("book");
                            categoryIIListModel.addElement("electronics");
                            categoryIIListModel.addElement("stationary");
                            categoryIIListModel.addElement("tool");
                            categoryIIListModel.addElement("decor");
                            break;
                        case "SOFTWARE":
                            categoryIIListModel.clear();
                            categoryIIListModel.addElement("music");
                            categoryIIListModel.addElement("movie");
                            categoryIIListModel.addElement("application");
                            categoryIIListModel.addElement("decor");
                            break;
                        case "AFFAIR":
                            categoryIIListModel.clear();
                            categoryIIListModel.addElement("tuition");
                            categoryIIListModel.addElement("bureaucratic");
                            categoryIIListModel.addElement("family");
                            break;
                        default:
                            System.out.println("This should not be reached");
                            break;
                    }
                }
            });

            categoryIList.setModel(categoryIListModel);
            categoryIIList.setModel(categoryIIListModel);
            categoryBox.add(categoryLabel);
            categoryBox.add(categoryIScroller);
            categoryBox.add(categoryIIScroller);

            // Items in descriptionBox
            JLabel descriptionLabel = new JLabel("Description");
            descriptionText = new JTextField();
            descriptionText.setColumns(200);

            descriptionBox.add(descriptionLabel);
            descriptionBox.add(descriptionText);

            // Items in frame
            JButton addButton = new JButton("Add");
            addButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    int selectedMonth = monthComboBox.getSelectedIndex();
                    int selectedDay = dayComboBox.getSelectedIndex() + 1;
                    int selectedYear = yearComboBox.getSelectedIndex() - 2 + THISYEAR;
                    setDateCalendar.set(selectedYear, selectedMonth, selectedDay);

                    double theAmountInUSD = Double.parseDouble(amountTextField.getText());
                    if (inCNY.isSelected()) {
                        theAmountInUSD = theAmountInUSD / currentExchangeRate;
                    }

                    String categoryI = categoryIList.getSelectedValue();
                    String categoryII = categoryIIList.getSelectedValue();
                    String description = descriptionText.getText();
                    boolean isOutput = moneyOut.isSelected();

                    theNewEntry = new Entry(isOutput, setDateCalendar, theAmountInUSD, currentExchangeRate, categoryI, categoryII, description);
                    archiveList.add(theNewEntry);
                    System.out.println(theNewEntry.toString());

                    // Save the modified archive
                    saveArchive();
                    // close window
                    newEntryFrame.setVisible(false);
                    newEntryFrame = null;
                    // Append latest entry
                    entryListModel.addElement(theNewEntry.toString());
                    entryList.setSelectedIndex(entryListModel.getSize() - 1);
                    entryList.ensureIndexIsVisible(entryListModel.getSize() - 1);
                    // refresh stats
                    refreshTotalStats();
                }
            });
            newEntryMainPanel.add(dateBox);
            newEntryMainPanel.add(isIOBox);
            newEntryMainPanel.add(amountBox);
            newEntryMainPanel.add(exchangeRateLabel);
            newEntryMainPanel.add(categoryBox);
            newEntryMainPanel.add(descriptionBox);
            newEntryFrame.getContentPane().add(BorderLayout.NORTH, newEntryMainPanel);
            newEntryFrame.getContentPane().add(BorderLayout.SOUTH, addButton);

            newEntryFrame.getRootPane().setDefaultButton(addButton);
            newEntryFrame.setSize(500, 320);
            newEntryFrame.setVisible(true);
            amountTextField.requestFocus();

        }
    }


    private class MonthComboBoxListener implements ItemListener {
        public void itemStateChanged(ItemEvent event) {
            // Add items to day list according to the month and year
            try {
                setDateCalendar.set(setDateCalendar.MONTH, monthComboBox.getSelectedIndex());
            } catch (Exception e) {
                e.printStackTrace();
            }
            int daysInThisMonth = setDateCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

            dayComboBox.removeAllItems();
            for (int i = 1; i <= daysInThisMonth; i++) {
                dayComboBox.addItem(Integer.toString(i));
            }
        }
    }

    private class YearComboBoxListener implements ItemListener {
        public void itemStateChanged(ItemEvent event) {
            // Add items to day list according to the month and year
            try {
                setDateCalendar.set(setDateCalendar.YEAR, yearComboBox.getSelectedIndex() - 2 + THISYEAR);
            } catch (Exception e) {
                e.printStackTrace();
            }
            int daysInThisMonth = setDateCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

            dayComboBox.removeAllItems();
            for (int i = 1; i <= daysInThisMonth; i++) {
                dayComboBox.addItem(Integer.toString(i));
            }
        }
    }


    private class SettingsListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            settingsFrame = new JFrame("System Settings");
            JPanel settingsMainPanel = new JPanel();
            settingsMainPanel.setLayout(new BoxLayout(settingsMainPanel, BoxLayout.Y_AXIS));

            // 0. set exchange rate
            Box exchangeRateBox = new Box(BoxLayout.X_AXIS);
            JLabel exchangeRateLabel = new JLabel("Override: Exchange Rate  $1 =");
            JLabel exchangeRateLabel2 = new JLabel("¥");
            JTextField exchangeRateTextField = new JTextField();
            exchangeRateTextField.setColumns(6);

            exchangeRateBox.add(exchangeRateLabel);
            exchangeRateBox.add(exchangeRateTextField);
            exchangeRateBox.add(exchangeRateLabel2);

            // 1. set preferred data source
            Box dataSourceBox = new Box(BoxLayout.X_AXIS);
            JLabel dataSouceLabel = new JLabel("Currency data source");
            JComboBox<String> sourceComboBox = new JComboBox<String>();
            sourceComboBox.addItem("Yahoo Finance");
            sourceComboBox.addItem("Baidu API");
            sourceComboBox.setSelectedIndex(selectedDataSource);

            dataSourceBox.add(dataSouceLabel);
            dataSourceBox.add(sourceComboBox);

            // Save button
            JButton saveSettingsButton = new JButton("Save Settings");
            saveSettingsButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    settingsList.clear();
                    settingsList.add(Double.parseDouble(exchangeRateTextField.getText()));
                    settingsList.add(refreshedDate);
                    settingsList.add(sourceComboBox.getSelectedIndex());
                    saveSettings();

                    settingsFrame.setVisible(false);
                    settingsFrame = null;
                    refreshTotalStats();
                }
            });

            // Set up
            exchangeRateTextField.setText(Double.toString(currentExchangeRate));

            settingsMainPanel.add(exchangeRateBox);
            settingsMainPanel.add(dataSourceBox);
            settingsMainPanel.add(saveSettingsButton);
            settingsFrame.getContentPane().add(settingsMainPanel);
            settingsFrame.setSize(300, 200);
            settingsFrame.getRootPane().setDefaultButton(saveSettingsButton);
            settingsFrame.setVisible(true);

        }
    }

}
