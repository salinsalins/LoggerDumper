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
    long shot;

    public AdlinkADC() {
        host = Constants.DEFAULT_HOST;
        port = Constants.DEFAULT_PORT;
        dev = Constants.DEFAULT_DEV;
        devProxy = null;
        shot = 0;
   }

    public AdlinkADC(String d, String h, String p) {
        dev = d;
        host = h;
        port = p;
        devProxy = null;
        shot = 0;
    }

    public String fullName() {
        if (devProxy == null)
            return "Not_initialized";
        else
            return "ADC";
    }

    public void init() {
    }

    long readShot() {
        return ++shot;
    }
}
