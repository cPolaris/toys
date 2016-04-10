import java.io.*;
import java.util.*;

public class PressureTestLinkedListMS {

    Calendar testCalendar = Calendar.getInstance();
    int originalSize = 0;
    int finalSize = 0;
    LinkedList<Entry> archiveToTest;

    public void loadArchive() {
        try {
            ObjectInputStream objectInput = new ObjectInputStream(new FileInputStream("LinkedListImpl.msar"));
            archiveToTest = (LinkedList<Entry>)objectInput.readObject();
            System.out.println("Loaded!");
            objectInput.close();
            originalSize = archiveToTest.size();
            Entry lastEntry = archiveToTest.get(archiveToTest.size() - 1);
            Entry.setFlowNumber(lastEntry.getFlowNumber());

        } catch (FileNotFoundException e) {
            System.out.println("First run, no file.");
            archiveToTest = new LinkedList<Entry>();
            System.out.println("New list created.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pressureTest(int n) {
        
        for (int i = 0; i < n; i++) {
                Entry entryToWrite = new Entry(true,testCalendar,332.34,6.23,"Education","Tuition","Paid with Credit");
                archiveToTest.add(entryToWrite);
                double progress = (double)i/(double)n * 100;
                System.out.print(String.format("%7.2f%%\r", progress));
                if (i == n) System.out.print("100%       \r\n");
            }

        try {
            ObjectOutputStream objectOutput = new ObjectOutputStream(new FileOutputStream("MoneyArchive.msar"));
            objectOutput.writeObject(archiveToTest);
            objectOutput.close();
            System.out.println("LinkedList written!");
            finalSize = archiveToTest.size();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("FATAL ERROR!!!");
        }

            
    }

    public void printResult() {
        System.out.println("original size: " + originalSize);
        System.out.println("current size: " + finalSize);

    }

    public static void main(String[] args) {
        long startTime = System.nanoTime();
//        int n = Integer.parseInt(args[0]);
        int n = 4000;
        PressureTestLinkedListMS newTest = new PressureTestLinkedListMS();
        newTest.loadArchive();
        newTest.pressureTest(n);
        newTest.printResult();
        long endTime = System.nanoTime();
        long elapsedTime = endTime - startTime;
        System.out.println("Elapsed time: " + Long.toString(elapsedTime));
    }
}