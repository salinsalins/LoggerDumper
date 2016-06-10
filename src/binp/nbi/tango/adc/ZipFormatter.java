package binp.nbi.tango.adc;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Formatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipFormatter {
    public String fileName = null;
    public ZipOutputStream zipOutputStream = null;
    public Formatter formatter = null;

	public ZipFormatter(String fileName) throws FileNotFoundException {
        this.fileName = "";
        //ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream(10000);
        FileOutputStream zipDest = new FileOutputStream(fileName);
        zipOutputStream = new ZipOutputStream(new BufferedOutputStream(zipDest));
        formatter = new Formatter(zipOutputStream);
        this.fileName = fileName;
	}
	
	public ZipFormatter format(String format, Object ... args) {
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
}
