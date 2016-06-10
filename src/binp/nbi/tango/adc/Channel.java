package binp.nbi.tango.adc;

import java.util.ArrayList;
import java.util.List;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.AttributeInfo;
import fr.esrf.TangoApi.DbAttribute;
import fr.esrf.TangoApi.DeviceAttribute;

public class Channel {
	public ADC adc = null;
	public DeviceAttribute devAttr = null;
	public DbAttribute dbAttr = null;
	public double[] data = null;
	public long shot = -2;
	public long time = -1;

	public Channel(ADC theadc, String channelName) throws DevFailed {
		adc = theadc;
		devAttr = theadc.devProxy.read_attribute(channelName);
		dbAttr = theadc.devProxy.get_attribute_property(channelName);
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

	public String description() throws DevFailed {
		AttributeInfo ai = adc.devProxy.get_attribute_info(name());
		return ai.description;
	}

	long shot() {
		return shot;
	}

	long readShot() {
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

	public String getPropString(String propName) {
		String propVal = "";
		try {
			propVal = dbAttr.get_string_value(propName);
		} catch (Exception e) {
			propVal = "";
		}
		return propVal;
	}

	public Double getPropDouble(String propName) {
		Double propVal = 0.0;
		try {
			propVal = Double.parseDouble(getPropString(propName));
		} catch (Exception e) {
			propVal = 0.0;
		}
		return propVal;
	}

	public Boolean getPropBoolean(String propName) {
		Boolean propVal = false;
		try {
			String propString = getPropString(propName);
			if (propString.toLowerCase().equals("true"))
				propVal = true;
			else if (propString.toLowerCase().equals("on"))
				propVal = true;
		} catch (Exception e) {
			propVal = false;
		}
		return propVal;
	}

	public Integer getPropInteger(String propName) {
		Integer propVal = 0;
		try {
			propVal = new Integer(getPropString(propName));
		} catch (Exception e) {
			propVal = 0;
		}
		return propVal;
	}

	public String[] getPropList() throws DevFailed {
		String[] propList = dbAttr.get_property_list();
		return propList;
	}

	public String[] getPropValList() throws DevFailed {
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
		ArrayList<Tick> tickList = new ArrayList<Tick>();
		String[] propNames = dbAttr.get_property_list();
		if (propNames != null && propNames.length > 0) {
			for (int i = 0; i < propNames.length; i++) {
				if (propNames[i].endsWith(Constants.START_SUFFIX)) {
					Tick tick = new Tick(this, propNames[i].replace(Constants.START_SUFFIX, ""));
					//System.out.println(tick);
					if (tick.length > 0.0 && tick.name != "") {
						tickList.add(tick);
					}
				}
			}
		}
		return tickList;
	}

}
