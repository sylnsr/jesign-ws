package itext;

import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import sun.misc.BASE64Decoder;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import java.io.*;
import org.json.JSONObject;
import sun.misc.BASE64Encoder;
import ws.PdfProcessor;
import ws.PdfProcessorResult;

import java.util.Iterator;


/**
 * Please note, I am not processing files in memory because it's not recommended
 * if and when you are signing many large PDF files. For more info, please see:
 * http://stackoverflow.com/questions/15760309/signing-pdf-with-java-itext-library-crashing-for-big-files
 */

@WebService
public class ImageStamper extends PdfProcessor {


    @WebMethod
    public PdfProcessorResult getStampedFile(
            @WebParam(name = "pdfFile", partName = "pdfFile") String pdfFile,
            @WebParam(name = "pngImage", partName = "pngImage") String pngImage,
            @WebParam(name = "jsonVectors", partName = "jsonVectors") String jsonVectors
    ) {
        result = new PdfProcessorResult();

        try {
            this.setFilePath(System.getProperty("user.dir") + "/");

            if (!this.removeFile(filePath)) {
                result.error = "Existing PDF file cannot be deleted (" + this.filePath + ")";
                return result;
            }
            if (!this.removeFile(filePath + ".png")) {
                result.error = "Existing PNG file cannot be deleted (" + this.filePath + ".png)";
                return result;
            }

            // decode the data and move it to disk
            BASE64Decoder decoder = new BASE64Decoder();
            InputStream pngStream = new ByteArrayInputStream(decoder.decodeBuffer(pngImage));
            InputStream pdfStream = new ByteArrayInputStream(decoder.decodeBuffer(pdfFile));
            PdfReader reader = new PdfReader(pdfStream);
            PdfStamper stamp = new PdfStamper(reader, new FileOutputStream(this.filePath));
            FileOutputStream fos = new FileOutputStream(filePath + ".png");
            IOUtils.copy(pngStream, fos);
            IOUtils.closeQuietly(pngStream);
            IOUtils.closeQuietly(fos);
            Image signature_image = Image.getInstance(filePath + ".png");
            signature_image.scalePercent(17);
            PdfContentByte add_signature_image;
            float pageWidth;
            float pageHeight;
            float bottom;
            float left;
            JSONObject vectors = new JSONObject(jsonVectors);
            Iterator<?> keys = vectors.keys();
            String pageId;
            int pageNumber;

            // loop the pages adding the image
            while (keys.hasNext()) {
                Object next = keys.next();
                pageId = next.toString();
                pageNumber = Integer.parseInt(pageId.split("")[2]); // input should be like "p1"; "p2" etc
                pageHeight = reader.getPageSize(pageNumber).getHeight();
                pageWidth = reader.getPageSize(pageNumber).getWidth();

                if (pageNumber > 0) {
                    JSONArray pageVectors = (JSONArray)vectors.get(pageId);
                    for (int i =0; i < pageVectors.length(); i++) {
                        JSONArray vector = (JSONArray)pageVectors.get(i);
                        // percentages
                        bottom = (float)vector.getDouble(0);
                        left = (float)vector.getDouble(1);
                        // absolutes
                        bottom =  (pageHeight - (( bottom / 100) * pageHeight)) - 8;
                        left = ((( left / 100) * pageWidth) / 2) * 0.93f;

                        signature_image.setAbsolutePosition(left, bottom);
                        add_signature_image = stamp.getUnderContent(pageNumber);  // set the correct page number
                        add_signature_image.addImage(signature_image);
                    }
                }
            }
            stamp.close();
            reader.close();



            // set the file to return
            BASE64Encoder encoder = new BASE64Encoder();
            FileInputStream fileStream = new FileInputStream(this.filePath);
            result.pdf = encoder.encode(IOUtils.toByteArray(fileStream));



        } catch (Exception ex) {
            result.error = (ex.toString().length() > 0)? ex.toString(): ex.getMessage();

        } finally {
            if (!this.removeFile(filePath)) {
                result.error = "Temp PDF file cannot be deleted (" + this.filePath + ")";
                return result;
            }
            if (!this.removeFile(filePath + ".png")) {
                result.error = "Temp PNG file cannot be deleted (" + this.filePath + ".png)";
                return result;
            }
        }

        return result;
    }
}
