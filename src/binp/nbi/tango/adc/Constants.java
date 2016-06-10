package binp.nbi.tango.adc;

public interface Constants {
	public static final String DEFAULT_HOST = "192.168.161.74";
	public static final String DEFAULT_PORT = "10000";
	public static final String DEFAULT_DEV = "binp/nbi/adc0";
	public static final int DEFAULT_AVG = 100;

	public static final Double DEFAULT_MARK_X = Double.NaN;
	public static final Double DEFAULT_MARK_Y = 0.0;

	public static final String PROP_VAL_DELIMETER = " = ";
	public static final String PROP_VAL_DELIMETER_OLD = ": ";
	
	public final static String ZERO_NAME = "zero";
	public final static String ZERO_MARK_NAME = "Zero";
	public final static String MARK_NAME = "mark";

	public final static String START_SUFFIX = "_start";
	public final static String LENGTH_SUFFIX = "_length";
	public final static String NAME_SUFFIX = "_name";

	public final static String UNIT = "unit";
	public final static String LABEL = "label";
	public final static String DISPLAY_UNIT = "display_unit";
	public final static String SAVE_DATA = "save_data";
	public final static String SAVE_AVG = "save_avg";
	public final static String SAVE_LOG = "save_log";
	public final static String SHOT_ID = "Shot_id";
	
	public final static String CHAN = "chan";
	public final static String PARAM = "param";

	public final static String EXTENSION = ".txt";

	public final static String XY_DELIMETER = "; ";
	public final static String X_FORMAT = "%f";
	public final static String Y_FORMAT = X_FORMAT;
	public final static String XY_FORMAT = X_FORMAT + XY_DELIMETER + Y_FORMAT;

	public final static String CRLF = "\r\n";

	public final static String LOG_DELIMETER = "; ";
	public final static String LOG_FORMAT = "%s = %7.3f %s";
	public final static String LOG_CONSOLE_FORMAT = "%10s = %7.3f %s\n";

}
