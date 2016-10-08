package binp.nbi.tango.adc;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ini4j.Wini;

public class LoggerDumper {
    static final Logger LOGGER = Logger.getLogger(LoggerDumper.class.getPackage().getName());

    static String progName = "Adlink DAQ-2204 Tango Logger";
    static String progNameShort = "LoggerDumper";
    static String progVersion = "4.1"; 
    public String iniFileName = progNameShort + ".ini";

    public String outDir = "d:\\nbi\\current\\TangoAttrDump\\data\\";
    int avgCount = 100;

    public String host = "192.168.111.10";
    public String port = "10000";
    public String dev = "binp/nbi/adc0";

    public String host2 = "192.168.111.11";
    public String port2 = "10000";
    public String dev2 = "binp/nbi/adc0";
    
    List<Device> deviceList;

    private File lockFile = new File("lock.lock");
    private FileOutputStream lockFileOS;

    public String makeFolder() {
        String folder = outDir + getLogFolderName();
        File file = new File(folder);
        file.mkdirs();
        //System.out.println("Created folder " + folder);
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
            saveAvg = avgCount;
        }
        // System.out.printf("saveAvg: %d\r\n", saveAvg);
        saveToZip(zipFile, sig.x.data, sig.y.data, saveAvg);
        zipFile.flush();
        zipFile.closeEntry();
    }

    public void saveSignalData(ZipFormatter zipFile, Signal sig) throws IOException {
        saveSignalData(zipFile, sig, "");
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
            for (String propList1 : propList) {
                // System.out.printf("%s\r\n", propList1);
                zipFile.format("%s\r\n", propList1);
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

    public void dumpAdcDataAndLog(AdlinkADC adc, ZipFormatter zipFile, Formatter logFile, String folder)
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

    public void dumpAdcDataAndLog(AdlinkADC adc, ZipFormatter zipFile, Formatter logFile) throws IOException, DevFailed {
        dumpAdcDataAndLog(adc, zipFile, logFile, "");
    }

    public static void delay(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ex) {
        }
    }

    public void process() throws DevFailed, IOException {
        // Fill AdlinkADC in deviceList
        AdlinkADC adc0 = null;
        for (Device d:deviceList) {
            try {
                AdlinkADC adc = new AdlinkADC(d.dev, d.host, d.port);
                d.timeout = System.currentTimeMillis();
                d.active = true;
                d.adc = adc;
                if (adc0 == null) adc0 = adc;
            } catch (DevFailed ex) {
                d.active = false;
                d.timeout = System.currentTimeMillis() + 10000;
            }
        }
        if (adc0 == null){
            System.out.printf("\nNo ADC found. Exit.");
            return;
        }
        
        long shotOld = -8888;
        //shotOld = adc.readShot();
        long shotNew = 0;

        while (true) {
            shotNew = adc0.readShot();
            if (shotNew != shotOld) {
                shotOld = shotNew;

                String folder = makeFolder();
                System.out.printf("\n%s New Shot %d\n", timeStamp(), shotNew);
                lockDir(folder);
                Formatter logFile = openLogFile(folder);
                String fmt = Constants.LOG_DELIMETER + "Shot" + Constants.PROP_VAL_DELIMETER + "%5d";
                logFile.format(fmt, shotNew);

                ZipFormatter zipFile = openZipFile(folder);
                
                for (Device d:deviceList) {
                    try {
                        if (!d.active) {
                            if (d.timeout > System.currentTimeMillis())
                                continue;
                            AdlinkADC adc = new AdlinkADC(d.dev, d.host, d.port);
                            d.timeout = System.currentTimeMillis();
                            d.active = true;
                            d.adc = adc;
                        }
                        System.out.println("Saving from " + d.adc.fullName());
                        dumpAdcDataAndLog(d.adc, zipFile, logFile, d.folder);
                        zipFile.flush();
                    } catch (DevFailed devFailed) {
                        d.active = false;
                        d.timeout = System.currentTimeMillis() + 10000;
                    }
                }

                zipFile.flush();
                zipFile.close();

                fmt = Constants.LOG_DELIMETER + "File" + Constants.PROP_VAL_DELIMETER + "%s";
                String zipFileName = zipFile.getName();
                logFile.format(fmt, zipFileName);
                //System.out.printf(fmt, fileName);
                //System.out.println();
                logFile.format(Constants.CRLF);
                logFile.flush();
                logFile.close();
                unlockDir();

                System.out.printf("\n%s Waiting for a shot ...", timeStamp());
            } // if shotOld != shotNew
            delay(1000);
        } // while
    }

    private void lockDir(String folder) throws FileNotFoundException {
        //WatchService watcher = null;
        //lockFile.toPath().register(watcher, ENTRY_CREATE);
        lockFile = new File(folder + "\\lock.lock");
        lockFileOS = new FileOutputStream(lockFile);
        //System.out.println("Locked");
    }

    private void unlockDir() throws IOException {
        lockFileOS.close();
        lockFile.delete();
        //System.out.println("Unlocked");
    }
    
    void printUsageMessage() {
        String usageMessage = "Usage: \n"
                + "LoggerDumper ini_file.ini or\n"
                + "LoggerDumper host port device avgcount\n"
                + "Default:  192.168.111.10 10000 binp/nbi/adc0 100";
        System.out.print(usageMessage);
    }
    
    void setDefaultParameters() throws DevFailed {
        deviceList = new LinkedList<>();
        deviceList.add(new Device());
    }

    void readCommandLineParameters(String[] args) {
        int length = args.length;
        if (length <= 0) {
            readConfigFromIni();
            return;
        }
        if (args[0].endsWith(".ini")) {
            iniFileName = args[0];
            readConfigFromIni();
            return;
        }

        deviceList = new LinkedList();
        Device adc = new Device();

        adc.host = args[0];
        if (args.length > 1) {
            adc.port = args[1];
        }
        if (args.length > 2) {
            adc.dev = args[2];
        }
        try {
            if (args.length > 3) {
                adc.avg = Integer.parseInt(args[3]);
            }
        }
        catch (NumberFormatException ex) {
        }

        deviceList.add(adc);
        
        if (args.length > 4) {
            outDir = args[4];
        }
        if (!outDir.endsWith("\\")) {
            outDir = outDir + "\\";
        }
}

    private void readConfigFromIni() {
        try {
            String s;
            int i;
            Wini ini = new Wini(new File(iniFileName));
            // Restore log level
            s = ini.get("Log", "level");
            if (s != null) LOGGER.setLevel(Level.parse(s));
            // Number of ADCs
            int n = 0;
            try {
                n = ini.get("Common", "ADCCount", int.class);
            } catch (Exception ex) {
            }
            deviceList = new LinkedList();
            if (n <= 0) return;
            // Read ADCs
            for (int j = 0; j < n; j++) {
                Device adc = new Device();
                String section = "ADC_" + j;
                s = ini.get(section, "host");
                if (s != null) adc.host = s;
                s = ini.get(section, "port");
                if (s != null) adc.port = s;
                s = ini.get(section, "device");
                if (s != null) adc.dev = s;
                s = ini.get(section, "folder");
                if (s != null) 
                    adc.folder = s;
                else
                    adc.folder = section;
                s = ini.get(section, "avg");
                if (s != null) 
                    try {
                        i = ini.get(section, "avg", int.class);
                        adc.avg = i;
                    } catch (Exception ex) {
                    }
                deviceList.add(adc);
                LOGGER.log(Level.FINE, "Added for processing " + adc.dev);
            }
            // Read output directory
            s = ini.get("Common", "outDir");
            if (s != null) outDir = s;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Configuration read error");
            LOGGER.log(Level.INFO, "Exception info", ex);
        }
        LOGGER.log(Level.FINE, "Configuration restored from {0}", iniFileName);
    }
    
    public static void main(String[] args) {

        LoggerDumper nbiLogger;
        nbiLogger = new LoggerDumper();

        try {
            if (args.length > 0) {
                nbiLogger.host = args[0];
            }
            if (args.length > 1) {
                nbiLogger.port = args[1];
            }
            if (args.length > 2) {
                nbiLogger.dev = args[2];
            }
            if (args.length > 3) {
                nbiLogger.avgCount = Integer.parseInt(args[3]);
            }
            if (args.length > 4) {
                nbiLogger.outDir = args[4];
            }
            if (!nbiLogger.outDir.endsWith("\\")) {
                nbiLogger.outDir = nbiLogger.outDir + "\\";
            }
            if (args.length > 5) {
                nbiLogger.host2 = args[5];
            }
            if (args.length > 6) {
                nbiLogger.port2 = args[6];
            }
            if (args.length > 7) {
                nbiLogger.dev2 = args[7];
            }
            if (args.length > 0 && args.length < 6) {
                nbiLogger.host2 = "";
            }

            nbiLogger.process();
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Exception in LoggerDumper");
            LOGGER.log(Level.INFO, "Exception info", ex);
        }
    }

    class Device {
        String host = "192.168.111.10";
        String port = "10000";
        String dev = "binp/nbi/adc0";
        String folder = "";
        int avg = 100;
        AdlinkADC adc;
        boolean active = false;
        long timeout = 0;

        public Device() {
        }

        public Device(String _host, String _port, String _dev, String _folder, int _avg) {
            host = _host;
            port = _port;
            dev = _dev;
            folder = _folder;
            avg = _avg;
            active = false;
            timeout = System.currentTimeMillis();
        }
    } 
}
