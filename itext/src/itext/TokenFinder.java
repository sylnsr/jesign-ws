package itext;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import itext.TokenLocationExtractionStrategy.TokenCharacterLocation;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import org.apache.commons.lang.StringUtils;
import sun.misc.BASE64Decoder;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

@WebService
public class TokenFinder {


    @WebMethod
    public String getTokenVectors(
            @WebParam(name="pdfFile", partName="pdfFile") String pdfFile,
            @WebParam(name="token", partName="token") String token,
            @WebParam(name="tokenStrings", partName="tokenStrings") String[] tokenStrings)
            throws Exception
    {
        BASE64Decoder decoder = new BASE64Decoder();
        InputStream pdfStream = new ByteArrayInputStream(decoder.decodeBuffer(pdfFile));
        PdfReader pr = new PdfReader(pdfStream);
        String result;
        PdfReaderContentParser parser = new PdfReaderContentParser(pr);
        TokenLocationExtractionStrategy strategy;
        List<String> tokenLocations;
        List<String> tokenGroups = new ArrayList<String>();

        for (String tokenStr : tokenStrings) {
            tokenLocations = new ArrayList<String>();
            for (int i = 1; i <= pr.getNumberOfPages(); i++) {
                strategy = new TokenLocationExtractionStrategy(
                        i,
                        pr.getPageSize(i).getWidth(),
                        pr.getPageSize(i).getHeight(),
                        token,
                        tokenStr
                );
                strategy = parser.processContent(i, strategy);
                for (TokenCharacterLocation location : strategy.getTokenLocations()) {
                    tokenLocations.add(location.getVectorJson());
                }
            }
            tokenGroups.add('"'+tokenStr+'"'+":[" + StringUtils.join(tokenLocations.toArray(), ",") + "]");
        }
        pr.close();
        result = "["+StringUtils.join(tokenGroups.toArray(),",")+"]";

        return result;
    }
}