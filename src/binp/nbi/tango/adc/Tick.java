package binp.nbi.tango.adc;

import binp.nbi.tango.util.Constants;

public class Tick {
    public String name = "";
    public double start = 0.0;
    public double length = 0.0;

    public Tick(String _name, double _start, double _length) {
        if (_name == null) 
            name = "";
        else 
            name = _name;
        start = _start;
        length = _length;
    }

    public Tick() {
        this("", 0.0, 0.0);
    }

    public Tick(Channel chan, String tickName) {
        this();
        if (!(tickName == null || "".equals(tickName))) {
            start = chan.getPropertyAsDouble(tickName + Constants.START_SUFFIX);
            length = chan.getPropertyAsDouble(tickName + Constants.LENGTH_SUFFIX);
            name = chan.getProperty(tickName + Constants.NAME_SUFFIX);
            if (name == null || "".equals(name)) name = tickName;
        }
    }

    public Tick(Signal sig, String tickName) {
        this(sig.y, tickName);
    }

    @Override
    public boolean equals(Object tick) {
        if (!(tick instanceof Tick)) return false;
        return ((Tick)tick).name.equals(name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Tick: %s : %g : %g", name, start, length);
    }

    public String name() {
        return name;
    }
}