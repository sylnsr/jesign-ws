package itext;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.LocationTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;


public class TextExtractor {

    public static byte[] GetByteArray (byte[] input) throws Exception {
        StringBuilder result = new StringBuilder();
        PdfReader reader = new PdfReader(input);
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);
        TextExtractionStrategy strategy;
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            strategy = parser.processContent(i, new LocationTextExtractionStrategy());
            result.append(strategy.getResultantText());
        }
        return result.toString().getBytes();
    }
}
