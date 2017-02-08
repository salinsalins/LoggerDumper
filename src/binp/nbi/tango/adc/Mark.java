package binp.nbi.tango.adc;

import binp.nbi.tango.util.Constants;

public class Mark extends Tick {
    public String markName = "";
    public Double yValue = Constants.DEFAULT_MARK_Y_VALUE;
    public Double xValue = Constants.DEFAULT_MARK_X_VALUE;

    public Mark() {
        super();
        markName = "";
        yValue = Constants.DEFAULT_MARK_Y_VALUE;
        xValue = Constants.DEFAULT_MARK_X_VALUE;
    }

    public Mark(Signal sig, String tickName) {
        super(sig, tickName);
        markName = "";
        yValue = Constants.DEFAULT_MARK_Y_VALUE;
        xValue = Constants.DEFAULT_MARK_X_VALUE;
        if (length > 0.0) {
            try {
                if (name.equals(Constants.ZERO_NAME))
                    markName = Constants.ZERO_MARK_NAME;
                else if (name.equals(Constants.MARK_NAME))
                    markName = sig.label();
                if (name == null || "".equals(name)) {
                    markName = sig.name();
                }
                else
                    markName = name;

                double x0 = sig.x.data[0];
                if (start < x0 ) return;
                double deltaX = sig.x.data[1] - sig.x.data[0];
                int index1 = (int) ((start - x0) / deltaX);
                if (index1 < 0) return;
                int index2 = (int) ((start + length) / deltaX);
                if (index2 > sig.y.data.length - 1) index2 = sig.y.data.length - 1;
                if (index2 > sig.x.data.length - 1) index2 = sig.x.data.length - 1;
                if (index2 <= index1) return;
                int count = 0;
                double xSum = 0.0;
                double ySum = 0.0;
                for (int i = index1; i <= index2; i++) {
                    xSum += sig.x.data[i];
                    ySum += sig.y.data[i];
                    count++;
                }
                if (count > 0) {
                    xValue = xSum / count;
                    yValue = ySum / count;
                }
            } catch (Exception e) {
            }
        }
    }

	public Mark(Signal sig) {
		this(sig, Constants.MARK_NAME);
	}

	public Mark(Signal sig, int number) {
		this(sig, Constants.MARK_NAME + number);
	}

	public Mark(Signal sig, Tick tick) {
		this(sig, tick.name);
	}

	public boolean equals(Object mark) {
		if (!(mark instanceof Mark)) return false;
		return ((Mark)mark).name.equals(name);
	}

	public int hashCode() {
		return name.hashCode();
	}

	public String toString() {
		return String.format("Mark: %s : %g : %g", name, yValue, xValue);
	}

}
