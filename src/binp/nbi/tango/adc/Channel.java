package binp.nbi.tango.adc;

import binp.nbi.tango.util.Constants;
import java.util.List;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.AttributeInfo;
import fr.esrf.TangoApi.DbAttribute;
import fr.esrf.TangoApi.DeviceAttribute;
import java.util.Arrays;
import java.util.LinkedList;

public class Channel {
    public AdlinkADC adc = null;
    public DeviceAttribute devAttr = null;
    public DbAttribute dbAttr = null;
    public double[] data = null;
    public long shot = -2;
    public long time = -1;

    public Channel(AdlinkADC _adc, String channelName) throws DevFailed {
        adc = _adc;
        devAttr = adc.devProxy.read_attribute(channelName);
        dbAttr = adc.devProxy.get_attribute_property(channelName);
    }

    public String fullName() throws DevFailed {
        return adc.devProxy.fullName() + "/" + devAttr.getName();
    }

    public String name() throws DevFailed {
        return devAttr.getName();
    }

    public String label() throws DevFailed {
        AttributeInfo ai = adc.devProxy.get_attribute_info(name());
        return ai.label;
    }

    long shot() {
        return shot;
    }

    long readShot() throws DevFailed {
        return adc.readShot();
    }

    public double[] readData() throws DevFailed {
        // if (devAttr.getDataFormat() != AttrDataFormat.SPECTRUM) throw new
        // DevFailed();
        // if (devAttr.getType() != TangoConst.Tango_DEV_DOUBLE) throw new
        // DevFailed();
        data = devAttr.extractDoubleArray();
        shot = readShot();
        time = devAttr.getTime();
        return data;
    }

    public String getProperty(String propertyName) {
        return dbAttr.get_string_value(propertyName);
    }

    public Double getPropertyAsDouble(String propName) {
        Double propVal = Double.NaN;
        try {
            propVal = Double.parseDouble(getProperty(propName));
        } catch (NumberFormatException e) {
        }
        return propVal;
    }

    public Boolean getPropertyAsBoolean(String propName) {
        Boolean propVal = false;
        String propString = getProperty(propName);
        if (propString.equalsIgnoreCase("true"))
            propVal = true;
        else if (propString.equalsIgnoreCase("on"))
            propVal = true;
        else if (propString.equals("1"))
            propVal = true;
        return propVal;
    }

    public Integer getPropertyAsInteger(String propName) {
        Integer propVal = Integer.MIN_VALUE;
        try {
            propVal = new Integer(getProperty(propName));
        } catch (NumberFormatException e) {
        }
        return propVal;
    }

    public String[] getPropValList() {
        String[] propNames = dbAttr.get_property_list();
        if (propNames != null && propNames.length > 0) {
            for (int i = 0; i < propNames.length; i++) {
                String propVal = dbAttr.get_string_value(propNames[i]);
                if (propVal == null)
                    propVal = "";
                propNames[i] = propNames[i] + Constants.PROP_VAL_DELIMETER + propVal;
            }
        }
        return propNames;
    }

    public List<Tick> getTickList() throws DevFailed {
        List<Tick> tickList = new LinkedList<>();
        String[] propNames = dbAttr.get_property_list();
        if (propNames != null && propNames.length > 0) {
            for (String propName : propNames) {
                if (propName.endsWith(Constants.START_SUFFIX)) {
                    Tick tick = new Tick(this, propName.replace(Constants.START_SUFFIX, ""));
                    //System.out.println(tick);
                    if (tick.length > 0.0 && !"".equals(tick.name)) {
                        tickList.add(tick);
                    }
                }
            }
        }
        return tickList;
    }
}
