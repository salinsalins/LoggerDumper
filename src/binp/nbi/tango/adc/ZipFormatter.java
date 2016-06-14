package binp.nbi.tango.adc;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Formatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipFormatter {

    private File file = null;
    private ZipOutputStream zipOutputStream = null;
    private Formatter formatter = null;

    public ZipFormatter(String fileName) throws FileNotFoundException {
        this(new File(fileName));
    }

    public ZipFormatter(File f) throws FileNotFoundException {
        file = f;
        FileOutputStream zipDest = new FileOutputStream(file);
        zipOutputStream = new ZipOutputStream(new BufferedOutputStream(zipDest));
        formatter = new Formatter(zipOutputStream);
    }

    public ZipFormatter format(String format, Object... args) {
        formatter.format(format, args);
        return this;
    }

    void close() {
        formatter.close();
    }

    void flush() {
        formatter.flush();
    }

    void closeEntry() throws IOException {
        zipOutputStream.closeEntry();
    }

    void putNextEntry(ZipEntry e) throws IOException {
        zipOutputStream.putNextEntry(e);
    }

    void putNextEntry(String name) throws IOException {
        ZipEntry e = new ZipEntry(name);
        zipOutputStream.putNextEntry(e);
    }
    
    String getFileName(){
        return file.getAbsolutePath();
    }
}
