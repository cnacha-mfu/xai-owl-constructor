package th.mfu.xai.owl;

import java.util.regex.Pattern;

public class test {

	public static void main(String[] args) {
		test test = new test();
		System.out.println(test.isNumeric("1"));
		System.out.println(test.getLeftInRange("2.0-66.0"));
		System.out.println(test.getRightInRange("2.0-66.0"));
	}
	
	private Pattern numberPattern = Pattern.compile("-?\\d+(\\.\\d+)?");

	
	public double getLeftInRange(String range) {
		return Double.parseDouble(range.substring(0, range.lastIndexOf('-')));
	}
	public double getRightInRange(String range) {
		return Double.parseDouble(range.substring(range.lastIndexOf('-')+1));
	}
	
	public boolean isNumeric(String strNum) {
	    if (strNum == null) {
	        return false; 
	    }
	    return numberPattern.matcher(strNum).matches();
	}
	
	public boolean isNumberRange(String strNum) {
	    if (strNum == null) {
	        return false; 
	    }
	    return numberPattern.matcher(strNum).matches();
	}

}
