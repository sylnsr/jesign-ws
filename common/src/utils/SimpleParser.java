package utils;


public class SimpleParser {

    public static int IntOrZero (String var) {
        int result;
        try {
            result = Integer.parseInt(var);
        } catch (Exception e) {
            result = 0;
        }
        return result;
    }

}
