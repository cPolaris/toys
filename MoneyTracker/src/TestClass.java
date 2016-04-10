import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;


public class TestClass {
	
	private String refreshExchangeRate() {
        // http://finance.yahoo.com/d/quotes.csv?e=.csv&f=sl1d1t1&s=USDCNY=X
    	String queryURLString = "http://finance.yahoo.com/d/quotes.csv?e=.csv&f=sl1d1t1&s=USDCNY=X";
    	String result = "";
    	try {
			URL queryURL = new URL(queryURLString);
	    	HttpURLConnection connection = (HttpURLConnection) queryURL.openConnection();
	    	System.out.println("CONNECTING...");
	    	connection.connect();
	    	
	    	BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	    	String unprocessedString = reader.readLine();
	    	String[] stringArray = unprocessedString.split(",");
	    	
	    	result = stringArray[1];
	    	
	    	reader.close();
	    	connection.disconnect();
	    	System.out.println("[ Exchange rate refreshed! ]");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (UnknownHostException uhe) {
			System.out.println("*** Not Connected to Internet ***");
		} catch (ConnectException ce) {
			ce.printStackTrace();
			System.out.println("*** CONNECTION FALIURE ***");
		} catch (IOException exp) {
			// TODO Auto-generated catch block
			exp.printStackTrace();
		} 
    	return result;
    }
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		TestClass anInstance = new TestClass();
		System.out.println(anInstance.refreshExchangeRate());
	}

}
