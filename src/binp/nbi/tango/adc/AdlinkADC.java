package binp.nbi.tango.adc;

import binp.nbi.tango.util.Constants;
import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.AttributeInfo;
import fr.esrf.TangoApi.DeviceAttribute;
import fr.esrf.TangoApi.DeviceProxy;

public class AdlinkADC {
    public String host = Constants.DEFAULT_HOST;
    public String port = Constants.DEFAULT_PORT;
    public String dev = Constants.DEFAULT_DEV;
    public DeviceProxy devProxy = null;

    public AdlinkADC() throws DevFailed {
        devProxy = new DeviceProxy(dev, host, port);
    }

    public AdlinkADC(String adev, String ahost, String aport) throws DevFailed {
        dev = adev;
        host = ahost;
        port = aport;
        devProxy = new DeviceProxy(dev, host, port);
    }

    public String fullName() {
        return devProxy.fullName();
    }

    public String name() {
        return devProxy.name();
    }

    long readShot() {
        long newShot = -2;
        try {
            DeviceAttribute da = devProxy.read_attribute(Constants.SHOT_ID);
            newShot = da.extractLong();
        } catch (DevFailed e) {
        }
        return newShot;
    }

    public String[] getPropList() throws DevFailed {
        return devProxy.get_property_list("*");
    }

    public String[] getPropList(String wildcards) throws DevFailed {
            return devProxy.get_property_list(wildcards);
    }

    public AttributeInfo[] getChannels() throws DevFailed {
            AttributeInfo[] attrInfo = devProxy.get_attribute_info();
            /*
             * int n = 0; for (int i=0 ; i < attrInfo.length ; i++) { if
             * (attrInfo[i].name.startsWith("chany")) { n++; } }
             */
            return attrInfo;
    }

    public String[] getChannelNames() throws DevFailed {
            AttributeInfo[] attrInfo = devProxy.get_attribute_info();
            int n = 0;
            for (AttributeInfo attrInfo1 : attrInfo) {
                if (attrInfo1.name.startsWith(Constants.CHAN)) {
                    n++;
                }
            }
            String[] channelNames = new String[n];
            n = 0;
            for (AttributeInfo attrInfo1 : attrInfo) {
                if (attrInfo1.name.startsWith(Constants.CHAN)) {
                    channelNames[n] = attrInfo1.name;
                    n++;
                }
            }
            return channelNames;
    }

    public String[] getSignalNames() throws DevFailed {
        AttributeInfo[] attrInfo = devProxy.get_attribute_info();
        String yChannel = Constants.CHAN + "y";
        String xChannel = Constants.CHAN + "x";
        int n = 0;
        for (int i = 0; i < attrInfo.length; i++) {
            if (attrInfo[i].name.startsWith(yChannel)) {
                String xName = attrInfo[i].name.replace(yChannel, xChannel);
                for (int j = 0; j < attrInfo.length; j++) {
                    if (attrInfo[j].name.equals(xName)) {
                        n++;
                        break;
                    }
                }
            }
        }
        String[] signalNames = new String[n];
        n = 0;
        for (int i = 0; i < attrInfo.length; i++) {
            if (attrInfo[i].name.startsWith(yChannel)) {
                String xName = attrInfo[i].name.replace(yChannel, Constants.CHAN);
                for (int j = 0; j < attrInfo.length; j++) {
                    if (attrInfo[j].name.equals(xName)) {
                        signalNames[n] = attrInfo[i].name;
                        n++;
                        break;
                    }
                }
            }
        }
        return signalNames;
    }

    public AttributeInfo[] getSignals() throws DevFailed {
        AttributeInfo[] attrInfo = devProxy.get_attribute_info();
        String yChannel = Constants.CHAN + "y";
        String xChannel = Constants.CHAN + "x";
        int n = 0;
        for (int i = 0; i < attrInfo.length; i++) {
            if (attrInfo[i].name.startsWith(yChannel)) {
                String xName = attrInfo[i].name.replace(yChannel, xChannel);
                for (int j = 0; j < attrInfo.length; j++) {
                    if (attrInfo[j].name.equals(xName)) {
                        n++;
                        break;
                    }
                }
            }
        }
        AttributeInfo[] signals = new AttributeInfo[n];
        n = 0;
        for (int i = 0; i < attrInfo.length; i++) {
            if (attrInfo[i].name.startsWith(yChannel)) {
                String xName = attrInfo[i].name.replace(yChannel, xChannel);
                for (int j = 0; j < attrInfo.length; j++) {
                    if (attrInfo[j].name.equals(xName)) {
                        signals[n] = attrInfo[i];
                        n++;
                        break;
                    }
                }
            }
        }
        return signals;
    }
}
