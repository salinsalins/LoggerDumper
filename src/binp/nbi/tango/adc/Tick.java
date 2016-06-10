package binp.nbi.tango.adc;

public class Tick {
	public String name = "";
	public double start = 0.0;
	public double length = 0.0;

	public Tick(String thename, double thestart, double thelength) {
		if (thename == null) name = "";
		else name = thename;
		start = thestart;
		length = thelength;
	}

	public Tick() {
		this("", 0.0, 0.0);
	}

	public Tick(Channel chan, String tickName) {
		this();
		if (!(tickName == null || "".equals(tickName))) {
				start = chan.getPropDouble(tickName + Constants.START_SUFFIX);
				length = chan.getPropDouble(tickName + Constants.LENGTH_SUFFIX);
				name = chan.getPropString(tickName + Constants.NAME_SUFFIX);
				if (name == null || "".equals(name)) name = tickName;
		}
	}

	public Tick(Signal sig, String tickName) {
		this(sig.y, tickName);
	}
	
	public boolean equals(Object tick) {
		if (!(tick instanceof Tick)) return false;
		return ((Tick)tick).name.equals(name);
	}

	public int hashCode() {
		return name.hashCode();
	}

	public String toString() {
		return String.format("Tick: %s : %g : %g", name, start, length);
	}

	public String name() {
		return name;
	}

}