package itextcert;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.security.*;
import com.itextpdf.text.pdf.security.MakeSignature.CryptoStandard;
import org.apache.commons.io.IOUtils;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import ws.PdfProcessor;
import ws.PdfProcessorResult;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import java.io.*;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.Properties;


@WebService
public class Certifier extends PdfProcessor {


    @WebMethod
    public PdfProcessorResult getCertifiedFile (
            @WebParam(name="pdfFile", partName="pdfFile") String pdfFile,
            @WebParam(name="reason", partName="reason") String reason,
            @WebParam(name="location", partName="location") String location
    ) {

        try {
            // setup properties
            this.setFilePath(System.getProperty("user.dir") + "/");
            Properties props = new Properties();
            props.load(getClass().getResourceAsStream("/resources/config.properties"));
            String password = props.getProperty("certPass");
            String path = props.getProperty("certPath");
            result = new PdfProcessorResult();
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            BASE64Decoder decoder = new BASE64Decoder();
            InputStream pdfStream = new ByteArrayInputStream(decoder.decodeBuffer(pdfFile));
            PdfReader reader = new PdfReader(pdfStream);
            // cert stuff
            KeyStore ks = KeyStore.getInstance("pkcs12");
            ks.load(new FileInputStream(path), password.toCharArray());
            String alias = (String)ks.aliases().nextElement();
            PrivateKey pk = (PrivateKey)ks.getKey(alias, password.toCharArray());
            Certificate[] chain = ks.getCertificateChain(alias);
            // reader / stamper
            FileOutputStream os = new FileOutputStream(this.filePath);
            PdfStamper stamper = PdfStamper.createSignature(reader, os, '\0', null, true);
            // digital signature
            ExternalSignature es = new PrivateKeySignature(pk, "SHA-256", "BC");
            ExternalDigest digest = new BouncyCastleDigest();
            PdfSignatureAppearance sap = stamper.getSignatureAppearance();
            sap.setReason(reason);
            sap.setLocation(location);
            MakeSignature.signDetached(sap, digest, es, chain, null, null, null, 0, CryptoStandard.CMS);
            // set the file to return
            BASE64Encoder encoder = new BASE64Encoder();
            FileInputStream fileStream = new FileInputStream(this.filePath);
            result.pdf = encoder.encode(IOUtils.toByteArray(fileStream));

        } catch (Exception ex) {
            result.error = (ex.toString().length() > 0)? ex.toString(): ex.getMessage();
            System.out.println(result.error);
        } finally {
            if ((new File (this.filePath)).exists()) {
                if (!this.removeFile(filePath)) {
                    result.error = "Temp PDF file cannot be deleted (" + this.filePath + ")";
                }
            }
        }
        return result;
    }
}
