package binp.nbi.tango.adc;

import binp.nbi.tango.util.Constants;
import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.AttributeInfo;
import fr.esrf.TangoApi.DeviceAttribute;
import fr.esrf.TangoApi.DeviceProxy;
import java.util.ArrayList;
import java.util.List;

public class AdlinkADC {
    public String host = Constants.DEFAULT_HOST;
    public String port = Constants.DEFAULT_PORT;
    public String dev = Constants.DEFAULT_DEV;
    public DeviceProxy devProxy = null;

    public AdlinkADC() {
        host = Constants.DEFAULT_HOST;
        port = Constants.DEFAULT_PORT;
        dev = Constants.DEFAULT_DEV;
        devProxy = null;
    }

    public AdlinkADC(String d, String h, String p) {
        dev = d;
        host = h;
        port = p;
        devProxy = null;
    }

    public String fullName() {
        if (devProxy == null)
            return "Not_initialized";
        else
            return devProxy.fullName();
    }

    public void init() throws DevFailed {
        devProxy = new DeviceProxy(dev, host, port);
    }

    public String getName() {
        if (devProxy == null)
            return "Not_initialized";
        return devProxy.name();
    }

    long readShot() throws DevFailed {
        DeviceAttribute da = devProxy.read_attribute(Constants.SHOT_ID);
        long s = da.extractLong();
        return s;
    }

    public AttributeInfo[] getChannels() throws DevFailed {
            AttributeInfo[] attrInfo = devProxy.get_attribute_info();
            return attrInfo;
    }
}
