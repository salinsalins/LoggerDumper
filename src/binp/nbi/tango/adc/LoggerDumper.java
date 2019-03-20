package binp.nbi.tango.adc;

import binp.nbi.tango.adc.LoggerDumper.Device;
import binp.nbi.tango.adc.LoggerDumper.Property;
import binp.nbi.tango.util.Constants;
import binp.nbi.tango.util.ZipFormatter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.*;

import fr.esrf.Tango.*;
import fr.esrf.TangoApi.AttributeInfo;
import fr.esrf.TangoApi.AttributeProxy;
import fr.esrf.TangoApi.DbAttribute;
import fr.esrf.TangoApi.DeviceProxy;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ini4j.Wini;

public class LoggerDumper {
    static final Logger LOGGER = Logger.getLogger(LoggerDumper.class.getPackage().getName());

    static String progName = "Adlink DAQ-2204 Tango Logger";
    static String progNameShort = "LoggerDumper";
    static String progVersion = "5.0"; 
    public String iniFileName = progNameShort + ".ini";

    public String outDir = ".\\data\\";

    List<Device> deviceList;
    List<Property> propList;

    private File lockFile = new File("lock.lock");
    private FileOutputStream lockFileOS;
    private boolean locked = false;

    public String makeFolder() {
        String folder = outDir + getLogFolderName();
        File file = new File(folder);
        file.mkdirs();
        LOGGER.log(Level.FINE, "Folder {0} created", folder);
        return folder;
    }

    public static String getLogFolderName() {
        SimpleDateFormat ydf = new SimpleDateFormat("yyyy");
        SimpleDateFormat mdf = new SimpleDateFormat("yyyy-MM");
        SimpleDateFormat ddf = new SimpleDateFormat("yyyy-MM-dd");
        Date now = new Date();
        String folder = ydf.format(now) + "\\" + mdf.format(now) + "\\" + ddf.format(now);
        return folder;
    }

    public static String getLogFileName() {
        Date now = new Date();
        SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd");
        String logFileName = dayFmt.format(now) + ".log";
        return logFileName;
    }

    public static Formatter openLogFile(String folder) throws IOException {
        String logFileName = folder + "\\" + getLogFileName();
        FileWriter fw = new FileWriter(logFileName, true);
        Formatter logFile = new Formatter(fw);
        String logTime = dateTimeStamp();
        logFile.format("%s", logTime);
        return logFile;
    }

    public static String dateTimeStamp() {
        return dateTimeStamp(new Date());
    }

    public static String dateTimeStamp(Date now) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return fmt.format(now);
    }

    public static String timeStamp() {
        Date now = new Date();
        SimpleDateFormat logTimeFmt = new SimpleDateFormat("HH:mm:ss");
        return logTimeFmt.format(now);
    }

    public static ZipFormatter openZipFile(String folder) throws IOException {
        Date now = new Date();
        SimpleDateFormat timeFmt = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        String zipFileName = folder + "\\" + timeFmt.format(now) + ".zip";
        ZipFormatter zipFile = new ZipFormatter(zipFileName);
        //System.out.printf("Created file %s\r\n", zipFileName);
        return zipFile;
    }

    public void saveToZip(ZipFormatter zipFile, double[] x, double[] y, int avgc) throws IOException {
        double xs = 0.0;
        double ys = 0.0;
        double ns = 0.0;
        String fmt = Constants.XY_FORMAT;

        zipFile.flush();

        if (y == null || x == null) {
            return;
        }
        if (y.length <= 0 || x.length <= 0) {
            return;
        }
        if (y.length > x.length) {
            return;
        }

        if (avgc < 1) {
            avgc = 1;
        }

        // System.out.printf("y: %d x: %d avgc: %d\r\n", y.length, x.length,
        // avgc);
        for (int i = 0; i < y.length; i++) {
            xs += x[i];
            ys += y[i];
            ns++;
            if (ns >= avgc) {
                if (i >= avgc) {
                    zipFile.format(Constants.CRLF);
                }
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
    }

    public void saveSignalData(ZipFormatter zipFile, Signal sig, String folder) throws IOException {
        String entryName = folder + sig.name + Constants.EXTENSION;
        ZipEntry entry = new ZipEntry(entryName);
        zipFile.putNextEntry(entry);
        //System.out.println("Saving " + sig.fullName() + " to " + sig.name + ".txt");
        int saveAvg = sig.getPropInteger(Constants.SAVE_AVG);
        if (saveAvg < 1) {
            saveAvg = 100;
        }
        // System.out.printf("saveAvg: %d\r\n", saveAvg);
        saveToZip(zipFile, sig.x.data, sig.y.data, saveAvg);
        zipFile.flush();
        zipFile.closeEntry();
    }

    public void saveSignalProp(ZipFormatter zipFile, Signal sig, String folder) throws IOException, DevFailed {
        zipFile.flush();
        String entryName = folder + Constants.PARAM + sig.name + Constants.EXTENSION;
        // ZipEntry entry = new ZipEntry(entryName);
        zipFile.putNextEntry(entryName);
        zipFile.format("Name%s%s\r\n", Constants.PROP_VAL_DELIMETER, sig.fullName());
        zipFile.format("Shot%s%d\r\n", Constants.PROP_VAL_DELIMETER, sig.shot());
        String[] propList = sig.getPropValList();
        if (propList.length > 0) {
            for (String prop : propList) {
                // System.out.printf("%s\r\n", prop);
                zipFile.format("%s\r\n", prop);
            }
        }
        zipFile.flush();
        zipFile.closeEntry();
    }

    public void saveSignalLog(Formatter logFile, Signal sig) throws IOException, DevFailed {
        //sig.name();
        //String printedSignalName = String.format("%7s ", sig.name());
        //System.out.printf("%7s ", sig.name());

        // Get signal label = default mark name
        String label = sig.getPropString(Constants.LABEL);
        //System.out.printf("label = %s\n", label);

        // Get unit name
        String unit = sig.getPropString(Constants.UNIT);
        //System.out.printf("unit = %s\n", unit);

        // Get calibration coefficient for conversion to unit
        double coeff = sig.getPropDouble(Constants.DISPLAY_UNIT);
        if (coeff == 0.0) {
            coeff = 1.0;
        }
        //System.out.printf("coeff = %g\n", coeff);

        List<Mark> marks = sig.getMarkList();

        // Find zero value
        double zero = 0.0;
        for (Mark mark : marks) {
            if (Constants.ZERO_NAME.equals(mark.name)) {
                zero = mark.yValue;
                //System.out.printf("zero = %g\n", zero);
                break;
            }
        }
        // Find all marks and log (mark - zero)*coeff
        for (Mark mark : marks) {
            boolean firstLine = true;
            if (!Constants.ZERO_NAME.equals(mark.name)) {
                double logMarkValue = (mark.yValue - zero) * coeff;

                String logMarkName = mark.name;
                if (logMarkName.equals(Constants.MARK_NAME)) {
                    logMarkName = label;
                }
                //System.out.printf(Constants.LOG_CONSOLE_FORMAT, logMarkName, logMarkValue, unit);
                if (firstLine) {
                    System.out.printf("%7s ", sig.name());
                } else {
                    System.out.printf("%7s ", "  ");
                }

                if (Math.abs(logMarkValue) >= 1000.0) {
                    System.out.printf("%10s = %7.0f %s\n", logMarkName, logMarkValue, unit);
                } else if (Math.abs(logMarkValue) >= 100.0) {
                    System.out.printf("%10s = %7.1f %s\n", logMarkName, logMarkValue, unit);
                } else if (Math.abs(logMarkValue) >= 10.0) {
                    System.out.printf("%10s = %7.2f %s\n", logMarkName, logMarkValue, unit);
                } else {
                    System.out.printf("%10s = %7.3f %s\n", logMarkName, logMarkValue, unit);
                }
                firstLine = false;

                String fmt = Constants.LOG_DELIMETER + Constants.LOG_FORMAT;
                logFile.format(fmt, logMarkName, logMarkValue, unit);
            }
        }
    }

    public void dumpADCDataAndLog(AdlinkADC adc, ZipFormatter zipFile, Formatter logFile, String folder)
            throws IOException, DevFailed {
        AttributeInfo[] channels = adc.getChannels();
        int retryCount = 0;
        for (int i = 0; i < channels.length; i++) {
            if (channels[i].name.startsWith(Constants.CHAN + "y")) {
                try {
                    Channel chan = new Channel(adc, channels[i].name);
                    Boolean saveDataFlag = chan.getPropertyAsBoolean(Constants.SAVE_DATA);
                    Boolean saveLogFlag = chan.getPropertyAsBoolean(Constants.SAVE_LOG);
                    if (saveDataFlag || saveLogFlag) {
                        Signal sig = new Signal(chan);
                        sig.readData();
                        saveSignalProp(zipFile, sig, folder);
                        if (saveDataFlag) {
                            saveSignalData(zipFile, sig, folder);
                        }
                        if (saveLogFlag) {
                            saveSignalLog(logFile, sig);
                        }
                    } // if save_auto || save_log is on
                    retryCount = 0;
                } // try
                catch (Exception ex) {
                    //System.out.println("Channel saving exception : " + ex );
                    //e.printStackTrace();
                    logFile.flush();
                    zipFile.flush();
                    zipFile.closeEntry();
                    retryCount++;
                } // catch
                if (retryCount > 0 && retryCount < 3) {
                    System.out.println("Retry reading channel " + channels[i].name);
                    i--;
                }
                if (retryCount >= 3) {
                    System.out.println("Error reading channel " + channels[i].name);
                }
            } // if
        } // for
    }

    public static void delay(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ex) {
        }
    }

    private void lockDir(String folder) throws FileNotFoundException {
        lockFile = new File(folder + "\\lock.lock");
        lockFileOS = new FileOutputStream(lockFile);
        locked = true;
        //System.out.println("Locked");
    }

    private void unlockDir() throws IOException {
        lockFileOS.close();
        lockFile.delete();
        locked = false;
        //System.out.println("Unlocked");
    }
    
    void printUsageMessage() {
        String usageMessage = "\nUsage: \n"
                + "LoggerDumper ini_file.ini "
                + "(Default: LoggerDumper.ini)"
                + "\nLoggerDumper host port device avgcount "
                + "  (Default: 192.168.111.10 10000 binp/nbi/adc0 100)\n";
        System.out.print(usageMessage);
    }
    
    void setDefaultParameters() throws DevFailed {
        deviceList = new LinkedList<>();
        deviceList.add(new Device());
    }

    void readCommandLineParameters(String[] args) {

        deviceList = new LinkedList<>();

        if (args.length <= 0) {
            readConfigFromIni();
            return;
        }

        if (args[0].endsWith(".ini")) {
            iniFileName = args[0];
            readConfigFromIni();
            return;
        }

        Device d = null;
        d = new Device();
        try {
            d.folder = "ADC_0";
            d.host = args[0];
            d.port = args[1];
            d.dev = args[2];
            d.avg = Integer.parseInt(args[3]);
            outDir = args[4];
        }
        catch (Exception ex) {
        }

        if (!outDir.endsWith("\\")) {
            outDir = outDir + "\\";
        }
        
        deviceList.add(d);
        LOGGER.log(Level.FINE, "Added device " + d.dev);
}
    
    private void readConfigFromIni() {
        try {
            String s;
            int i;
            Wini ini = new Wini(new File(iniFileName));

            // Restore log level
            s = ini.get("Log", "level");
            if (s != null) {
                LOGGER.setLevel(Level.parse(s));
            }

            // Number of ADCs
            int n = 0;
            n = ini.get("Common", "ADCCount", int.class);
            if (n <= 0) {
                LOGGER.log(Level.WARNING, "No ADC declared in ini file");
                return;
            }
            // Read ADCs
            for (int j = 0; j < n; j++) {
                Device d = new Device();
                String section = "ADC_" + j;
                s = ini.get(section, "host");
                if (s != null) d.host = s;
                s = ini.get(section, "port");
                if (s != null) d.port = s;
                s = ini.get(section, "device");
                if (s != null) d.dev = s;
                s = ini.get(section, "folder");
                if (s != null) 
                    d.folder = s;
                else
                    d.folder = section;
                d.avg = 100;
                i = ini.get(section, "avg", int.class);
                if (i != 0) 
                    d.avg = i;
                deviceList.add(d);
                LOGGER.log(Level.FINE, "Added device " + d.dev);
            }
            // Read output directory
            s = ini.get("Common", "outDir");
            if (s != null) 
                outDir = s;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Ini file read error");
            LOGGER.log(Level.INFO, "Exception info", ex);
        }
        LOGGER.log(Level.FINE, "Configuration restored from {0}", iniFileName);
    }
    
    public void process() throws IOException {
        Formatter logFile = null;
        ZipFormatter zipFile = null;
        
        if (deviceList.isEmpty())  {
            LOGGER.log(Level.SEVERE, "No ADC found.");
            return;
        }
        
        // Fill AdlinkADC in deviceList
        int count = 0;
        for (Device d:deviceList) {
            try {
                d.init();
                d.timeout = System.currentTimeMillis();
                d.active = true;
                count++;
            } 
            catch (DevFailed ex) {
                LOGGER.log(Level.INFO, "ADC {0} initialization error", d.fullName());
                d.active = false;
                d.timeout = System.currentTimeMillis() + 10000;
            }
        }
        if (count == 0) {
            LOGGER.log(Level.WARNING, "No active ADC found");
        }
        
        long shotNew = 0;

        while (true) {
            try {
                for (Device d:deviceList) {
                    try {
                        if (!d.active) {
                            if (d.timeout > System.currentTimeMillis())
                                continue;
                            d.init();
                            d.timeout = System.currentTimeMillis();
                            d.active = true;
                            LOGGER.log(Level.FINE, "ADC {0} activated", d.fullName());
                        }
                        shotNew = d.readShot();
                        if (shotNew <= d.shot) 
                            if(!locked) 
                                break;
                            else
                                continue;
                        d.shot = shotNew;
                        System.out.printf("\n%s New Shot %d\n", timeStamp(), shotNew);

                        if(!locked) {
                            String folder = makeFolder();
                            lockDir(folder);
                            logFile = openLogFile(folder);
                            String fmt = Constants.LOG_DELIMETER + "Shot" + Constants.PROP_VAL_DELIMETER + "%5d";
                            logFile.format(fmt, shotNew);
                            zipFile = openZipFile(folder);
                        }
                        System.out.println("Saving from " + d.fullName());
                        dumpADCDataAndLog(d, zipFile, logFile, d.folder);
                        zipFile.flush();
                    } 
                    catch (DevFailed df) {
                        d.active = false;
                        d.timeout = System.currentTimeMillis() + 10000;
                        LOGGER.log(Level.CONFIG, "ADC {0} timeout for 10 seconds", d.fullName());
                    }
                }
                if(locked) {
                    zipFile.flush();
                    zipFile.close();

                    String fmt = Constants.LOG_DELIMETER + "File" + Constants.PROP_VAL_DELIMETER + "%s";
                    String zipFileName = zipFile.getName();
                    logFile.format(fmt, zipFileName);
                    logFile.format(Constants.CRLF);
                    logFile.flush();
                    logFile.close();
                    unlockDir();
                }
            }
            catch (Exception ex) {
            }
            System.out.printf("\n%s Waiting for next shot ...", timeStamp());
            delay(1000);
        } // while
    }

    public static void main(String[] args) {

        LoggerDumper lgr;
        lgr = new LoggerDumper();
        try {
            lgr.readCommandLineParameters(args);
            lgr.process();
        }
        catch (Exception ex) {
            //lgr.printUsageMessage();
            LOGGER.log(Level.SEVERE, "Exception in LoggerDumper");
            LOGGER.log(Level.INFO, "Exception info", ex);
        }
    }

    class Device extends AdlinkADC {
        String folder = "";
        int avg = 100;
        boolean active = false;
        long timeout = 0;
        long shot = -8888L;

        public Device() {
            this.folder = "";
            this.avg = 100;
            this.active = false;
            this.shot = -8888L;
            this.timeout = System.currentTimeMillis();
        }

        public Device(String h, String p, String d, String f, int a) {
            super(h, p, d);
            this.folder = f;
            this.avg = a;
            this.active = false;
            this.shot = -8888L;
            this.timeout = System.currentTimeMillis();
        }
    } 

    class Property {
        String device;
        String attribute;
        String property;
        String name;

        public Property() {
        }

        public Property(String dev, String attr, String prop) {
            device = dev;
            attribute = attr;
            property = prop;
        }
        
        public String read() {
            if (device == null || "".equals(device)) return null;
            if (property == null || "".equals(property)) return null;
            try {
                String s;
                DeviceProxy devProxy = new DeviceProxy(device);
                if (attribute == null || "".equals(attribute)) {
                    s = devProxy.get_property(property).extractString();
                    name = device + property;
                } 
                else {
                    DbAttribute dbAttr = devProxy.get_attribute_property(attribute);
                    s = dbAttr.get_string_value(property);
                    name = device + attribute + property;
                }
                return s;
            } 
            catch (DevFailed df) {
                return null;
            }
        }
    } 
}
