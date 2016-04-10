import java.util.Calendar;
import java.io.*;

// Text file implementation
public class Entry implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static int objCount = 0;
    private final long TIMESTAMP;      // Time in Millis
    private final String FLOWNUMBER;   // 6 digit string
    private double amountInUSD;
    private double exchangeRate;
    private String categoryI;
    private String categoryII;
    private String description;
    private boolean isOutput;

    // Constructors
    public Entry(boolean ifIsOutput, Calendar theTime, double theAmountInUSD, double theExchangeRate,
        String theCategoryI, String theCategoryII, String theDescription) {
        objCount++;
        TIMESTAMP = theTime.getTimeInMillis();
        FLOWNUMBER = String.format("%06d",objCount);
        amountInUSD = theAmountInUSD;
        exchangeRate = theExchangeRate;
        categoryI = theCategoryI;
        categoryII = theCategoryII;
        description = theDescription;
        isOutput = ifIsOutput;
    }


    // Utility methods
    public String toString() {
        // String i = "|";
        // return FLOWNUMBER+i+TIMESTAMP+i+amountInUSD+i+amountInUSD*exchangeRate+i+categoryI+i+categoryII+i+description;
    	// Column count 380
    	String stringTimestamp = timestampToString(TIMESTAMP);
    	//                    6  10 12     12     14 14 306 
        return String.format("%6s | %10s |$%12.2f |¥%12.2f | %-14s | %-14s | %s",FLOWNUMBER,stringTimestamp,amountInUSD,amountInUSD*exchangeRate,categoryI,categoryII,description);
    }
    
    private String timestampToString(long theTimestamp) {
    	Calendar temporaryCalendar = Calendar.getInstance();
    	temporaryCalendar.setTimeInMillis(theTimestamp);
    	return String.format("%2d/%2d/%4d", temporaryCalendar.get(Calendar.MONTH) + 1,temporaryCalendar.get(Calendar.DATE),temporaryCalendar.get(Calendar.YEAR));
    }

    // getter and setter methods
    public static void setFlowNumber(String flowNum) {
        objCount = Integer.parseInt(flowNum);
    }

    public boolean isOutput() {
        return isOutput;
    }

    public long getTimeStamp() {
        return TIMESTAMP;
    }

    public String getFlowNumber() {
        return FLOWNUMBER;
    }

    public double getAmountInUSD() {
        return amountInUSD;
    }

    public double getAmountInCNY() {
        return amountInUSD * exchangeRate;
    }

    public double getExchangeRate() {
        return exchangeRate;
    }

    public String getCategoryI() {
        return categoryI;
    }

    public String getCategoryII() {
        return categoryII;
    }

    public String getDescription() {
        return description;
    }
    

    // Not a much meaningful functionality. Just for experiment
    private static int splitOutFlowNumber(Entry entryToProcess) {
    	String stringToProcess = entryToProcess.toString();
    	String[] stringProcessed = stringToProcess.split(" | ", 2);
    	return Integer.parseInt(stringProcessed[0]);
    }
    
    // Test client
    public static void main(String[] args) {
        Calendar testCalendar = Calendar.getInstance();
        Entry testEntry = new Entry(true,testCalendar,16993847.99,6.23,"Education","Tuition","Paid with Credit");
        System.out.println(testEntry.toString());
        System.out.println("Flownum: " + splitOutFlowNumber(testEntry));
    }
}