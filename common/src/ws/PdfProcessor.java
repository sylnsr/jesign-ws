package ws;

import java.io.File;
import java.util.UUID;

public abstract class PdfProcessor {

    protected String fileName;
    protected String filePath;
    protected PdfProcessorResult result;


    protected boolean removeFile (String filePath) {
        boolean result = true;
        File target = new File(filePath);
        if (target.exists()) {
            if ( ! target.delete() ) {
                return false;
            }
        }
        return result;
    }

    protected boolean setFilePath(String folderPath) {
        boolean result = false;
        this.fileName = UUID.randomUUID().toString() + ".pdf";
        this.filePath = folderPath + this.fileName;

        try {
            // paranoia ...
            if (!(new File(filePath)).exists()) {
                result = true;
            }
        } catch (Exception exception) {
            this.result.error = exception.getMessage();
        }
        return result;
    }
}
