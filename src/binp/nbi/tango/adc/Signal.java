package binp.nbi.tango.adc;

import binp.nbi.tango.util.Constants;
import binp.nbi.tango.util.ZipFormatter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;

import fr.esrf.Tango.DevFailed;

public class Signal {

	public String name = null;
	public Integer number = -1;
	public Channel x = null;
	public Channel y = null;

	public Signal(AdlinkADC adc, String channelName) throws DevFailed {
		String yChannelName = "";
		if (channelName.startsWith("chany"))
			yChannelName = channelName;
		else if (channelName.startsWith("chanx"))
			yChannelName = channelName.replace("chanx", "chany");
		else if (channelName.startsWith("chan"))
			yChannelName = channelName.replace("chan", "chany");
		else
			return;
		try {
			number = Integer.parseInt(yChannelName.substring(5));
		} catch (NumberFormatException e) {
			name = channelName;
			number = -1;
			x = null;
			y = null;
			return;
		}
		y = new Channel(adc, yChannelName);
		// y.readData();
		String xChannelName = yChannelName.replace("chany", "chanx");
		x = new Channel(adc, xChannelName);
		// x.readData();
		name = yChannelName.replace("chany", "chan");
	}

	public Signal(AdlinkADC adc, int channelNumber) throws DevFailed {
		this(adc, "chany" + channelNumber);
	}

	public Signal(Channel chan) throws DevFailed {
		this(chan.adc, chan.name());
	}

	public String fullName() throws DevFailed {
		return this.y.fullName();
	}

	public String name() {
		return name;
	}

	long shot() {
		return y.shot();
	}

	long readShot() {
		return y.readShot();
	}

	String label() throws DevFailed {
		return y.label();
	}

	String description() throws DevFailed {
		return y.description();
	}

	public void readData() throws DevFailed {
		y.readData();
		x.readData();
	}

	public Mark getMark(String markName) {
		return new Mark(this, markName);
	}

	public Mark getMark() {
		return new Mark(this);
	}

	public Mark getMark(int number) {
		return new Mark(this, number);
	}

	public Double getMarkValue(String markName) {
		return getMark(markName).yValue;
	}

	public Double getMarkValue() {
		return getMark().yValue;
	}

	public Double getMarkValue(int number) {
		return getMark(number).yValue;
	}

	public String getMarkName(String name) {
		return getMark(name).name;
	}

	public String getMarkName() {
		return getMark().name;
	}

	public String getMarkName(int number) throws DevFailed {
		return getMark(number).name;
	}

	public Integer getPropInteger(String propName) {
		return y.getPropInteger(propName);
	}

	public Double getPropDouble(String propName) {
		return y.getPropDouble(propName);
	}

	public Boolean getPropBoolean(String propName) {
		return y.getPropBoolean(propName);
	}

	public String getPropString(String propName) {
		return y.getPropertyAsString(propName);
	}

	public String[] getPropValList() throws DevFailed {
		return y.getPropValList();
	}

	public List<Mark> getMarkList() throws DevFailed {
		ArrayList<Mark> markList = new ArrayList<Mark>();
		List<Tick> tickList = y.getTickList();
		for (Tick tick : tickList) {
			//System.out.println(tick);
			if (tick.length > 0.0 && !"".equals(tick.name)) {
				Mark mark = new Mark(this, tick.name);
				//System.out.println(mark);
				markList.add(mark);
			}
		}
		Channel s0 = new Channel(y.adc, "chan0");
		List<Tick> defTickList = s0.getTickList();
		for (Tick tick : defTickList) {
			//System.out.println(tick);
			//System.out.println(tickList.contains(tick));
			if (!tickList.contains(tick)) {
				if (tick.length > 0.0 && !"".equals(tick.name)) {
					Mark mark = new Mark(this, tick.name);
					//System.out.println(mark);
					markList.add(mark);
				}
			}
		}
		/*
		String[] propNames = y.dbAttr.get_property_list();
		if (propNames != null && propNames.length > 0) {
			for (int i = 0; i < propNames.length; i++) {
				if (propNames[i].endsWith("_start")) {
					Mark mark = new Mark(this, propNames[i].replace("_start", ""));
					if (!mark.xValue.isNaN()) {
						markList.add(mark);
					}
				}
			}
		}
		*/
		return markList;
	}

	public Mark[] getMarkArray() throws DevFailed {
		List<Mark> markList = getMarkList();
		Mark[] markArray = new Mark[markList.size()];
		markArray = markList.toArray(markArray);
		return markArray;
	}

	public void saveToZipFile(ZipFormatter zipFile, String folder) throws IOException, DevFailed {
		String entryName = folder + name + Constants.EXTENSION;
		ZipEntry entry = new ZipEntry(entryName);
		zipFile.putNextEntry(entry);
		int saveAvg = getPropInteger(Constants.SAVE_AVG);
		if (saveAvg < 1)
			saveAvg = Constants.DEFAULT_AVG;
		double xs = 0.0;
		double ys = 0.0;
		double ns = 0.0;
		String fmt = Constants.XY_FORMAT;

		zipFile.flush();

		if (y == null || x == null)
			return;
		if (y.data.length <= 0 || x.data.length <= 0)
			return;
		if (y.data.length > x.data.length)
			return;

		if (saveAvg < 1) saveAvg = 1;

		for (int i = 0; i < y.data.length; i++) {
			xs += x.data[i];
			ys += y.data[i];
			ns++;
			if (ns >= saveAvg) {
				if (i >= saveAvg)
					zipFile.format(Constants.CRLF);
				zipFile.format(fmt, xs / ns, ys / ns);
				xs = 0.0;
				ys = 0.0;
				ns = 0.0;
			}
		}
		if (ns > 0) {
			zipFile.format(Constants.CRLF + fmt, xs / ns, ys / ns);
			xs = 0.0;
			ys = 0.0;
			ns = 0.0;
		}
		zipFile.flush();
		zipFile.closeEntry();
	}

}
