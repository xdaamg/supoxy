package de.waldmensch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class SuPoxyUtils {

	public static String chkStringForNull (Object value){
		if(value == null)
			return "0";
		else
			return value.toString();
	}

	public static Date LongToDate(Long value){

		Date date = new Date();
		date.setTime(value);

		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.setTimeZone(TimeZone.getTimeZone("GMT"));
		cal.add(Calendar.HOUR, SuPoxySettings.timecorrection);

		return cal.getTime();
	}

	public static Integer IntParser(String parameter, String value, Integer defaultvalue){
		try {
			return Integer.parseInt(value);
		} catch ( NumberFormatException e) {
			log("Use default value for " + parameter + " (" + defaultvalue + ")");
			return defaultvalue;
		}
	}

	public static String fromStream(InputStream in) throws IOException
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		StringBuilder out = new StringBuilder();
		String newLine = System.getProperty("line.separator");
		String line;
		while ((line = reader.readLine()) != null) {
			out.append(line);
			out.append(newLine);
		}
		return out.toString();
	}
	
	public static void log(String message){
		Date date = new Date();
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		String reportDate = df.format(date);
		System.out.println(reportDate + "\t" + message);
	}

}
