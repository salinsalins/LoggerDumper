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

    static String progName = "Adlink DAQ-2204 Tango Loger";
    static String progNameShort = "LoggerDumper";
    static String progVersion = "41"; 
    public String iniFileName = progNameShort + ".ini";

    public String outDir = "d:\\nbi\\current\\TangoAttrDump\\data\\";
    int avgCount = 100;

    public String host = "192.168.111.10";
    public String port = "10000";
    public String dev = "binp/nbi/adc0";

    public String host2 = "192.168.111.11";
    public String port2 = "10000";
    public String dev2 = "binp/nbi/adc0";

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
            for (int j = 0; j < propList.length; j++) {
                // System.out.printf("%s\r\n", propList[j]);
                zipFile.format("%s\r\n", propList[j]);
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

    public void dumpAdcDataAndLog(ADC adc, ZipFormatter zipFile, Formatter logFile, String folder)
            throws IOException, DevFailed {
        AttributeInfo[] channels = adc.getChannels();
        int retryCount = 0;
        for (int i = 0; i < channels.length; i++) {
            if (channels[i].name.startsWith(Constants.CHAN + "y")) {
                try {
                    Channel chan = new Channel(adc, channels[i].name);
                    Boolean saveDataFlag = chan.getPropBoolean(Constants.SAVE_DATA);
                    Boolean saveLogFlag = chan.getPropBoolean(Constants.SAVE_LOG);
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
                catch (Exception e) {
                    //System.out.println("Channel saving exception : " + e );
                    //e.printStackTrace();
                    logFile.flush();
                    zipFile.flush();
                    zipFile.closeEntry();
                    retryCount++;
                } // catch
                if (retryCount > 0 && retryCount < 3) {
                    System.out.println("Retry reading channel");
                    i--;
                }
            } // if
        } // for
    }

    public void dumpAdcDataAndLog(ADC adc, ZipFormatter zipFile, Formatter logFile) throws IOException, DevFailed {
        dumpAdcDataAndLog(adc, zipFile, logFile, "");
    }

    public static void delay(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ex) {
        }
    }

    public void process() throws DevFailed, IOException {
        ADC adc = new ADC(dev, host, port);

        long shotOld = -8888;
        //shotOld = adc.readShot();
        long shotNew = 0;

        while (true) {
            shotNew = adc.readShot();
            if (shotNew != shotOld) {
                shotOld = shotNew;

                String folder = makeFolder();

                System.out.printf("\n%s New Shot %d\n", timeStamp(), shotNew);
                lockDir(folder);
                Formatter logFile = openLogFile(folder);
                String fmt = Constants.LOG_DELIMETER + "Shot" + Constants.PROP_VAL_DELIMETER + "%5d";
                logFile.format(fmt, shotNew);

                ZipFormatter zipFile = openZipFile(folder);

                System.out.println("Saving from " + adc.fullName());
                dumpAdcDataAndLog(adc, zipFile, logFile, "mc/");

                if (!"".equals(host2) && !"".equals(port2) && !"".equals(dev2)) {
                    ADC adc2 = new ADC(dev2, host2, port2);
                    System.out.println("Saving from " + adc2.fullName());
                    dumpAdcDataAndLog(adc2, zipFile, logFile, "rf/");
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

    private File lockFile = new File("lock.lock");
    private FileOutputStream lockFileOS;

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
    
    void readInitialParameters(String[] args) {
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
        host = args[0];
        if (args.length > 1) {
            port = args[1];
        }
    }

    private void readConfigFromIni() {
        try {
            boolean b;
            String s;
            Wini ini = new Wini(new File(iniFileName));
            int n = ini.get("Common", "ADCCount", int.class);
            if (n <= 0) return;
            
            for (int i = 0; i < n; i++) {
                String section = "ADC_" + i;
                s = ini.get(section, "host");
                s = ini.get(section, "port");
                s = ini.get(section, "device");
            }
            b = ini.get("Input", "readFromFile", boolean.class);

            // Restore log level
            s = ini.get("Log", "level");
            LOGGER.setLevel(Level.parse(s));
            
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
}
