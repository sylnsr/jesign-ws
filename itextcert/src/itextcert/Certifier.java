package itextcert;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.security.*;
import com.itextpdf.text.pdf.security.MakeSignature.CryptoStandard;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import sun.security.pkcs11.SunPKCS11;
import ws.PdfProcessor;
import ws.PdfProcessorResult;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


@WebService
public class Certifier extends PdfProcessor {


    @WebMethod
    public PdfProcessorResult getCertifiedFile (
            @WebParam(name="pdfFile", partName="pdfFile") String pdfFile,
            @WebParam(name="reason", partName="reason") String reason,
            @WebParam(name="location", partName="location") String location
    ) {
        PdfProcessorResult result = new PdfProcessorResult();
        try {
            // config
            this.setFilePath(System.getProperty("user.dir") + "/");
            Properties props = new Properties();
            props.load(getClass().getResourceAsStream("/resources/config.properties"));
            String password = props.getProperty("certPass");
            String path = props.getProperty("certPath");
            // vars
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



    @WebMethod
    public PdfProcessorResult getCertifiedFileWithPKCS11 (
            @WebParam(name="pdfFile", partName="pdfFile") String pdfFile,
            @WebParam(name="reason", partName="reason") String reason,
            @WebParam(name="location", partName="location") String location
    ) {
        PdfProcessorResult result = new PdfProcessorResult();
        try {
            //config
            this.setFilePath(System.getProperty("user.dir") + "/");
            Properties props = new Properties();
            props.load(getClass().getResourceAsStream("/resources/config.properties"));
            char[] tokenPass = props.getProperty("tokenPass").toCharArray();
            String slot = props.getProperty("tokenSlot");
            String pkcsLibPath = props.getProperty("pkcs11LibPath");
            // vars
            String pkcs11cfg = "name = PKCS11-HSM\n" + "library = " + pkcsLibPath + "\n" + "slot = " + slot;
            ByteArrayInputStream pkcs11cfgBA = new ByteArrayInputStream(pkcs11cfg.getBytes());
            BouncyCastleProvider providerBC = new BouncyCastleProvider();
            Security.addProvider(providerBC);
            Provider providerPKCS11 = new SunPKCS11(pkcs11cfgBA);
            Security.addProvider(providerPKCS11);
            BASE64Decoder decoder = new BASE64Decoder();
            InputStream pdfStream = new ByteArrayInputStream(decoder.decodeBuffer(pdfFile));
            PdfReader reader = new PdfReader(pdfStream);
            // cert stuff
            KeyStore ks = KeyStore.getInstance("PKCS11");
            ks.load(null, tokenPass);
            String alias = (String)ks.aliases().nextElement();
            PrivateKey pk = (PrivateKey)ks.getKey(alias, tokenPass);
            Certificate[] chain = ks.getCertificateChain(alias);
            OcspClient ocspClient = new OcspClientBouncyCastle();
            TSAClient tsaClient = null;
            for (int i = 0; i < chain.length; i++) {
                X509Certificate cert = (X509Certificate)chain[i];
                String tsaUrl = CertificateUtil.getTSAURL(cert);
                if (tsaUrl != null) {
                    tsaClient = new TSAClientBouncyCastle(tsaUrl);
                    break;
                }
            }
            List<CrlClient> crlList = new ArrayList<CrlClient>();
            crlList.add(new CrlClientOnline(chain));
            // reader / stamper
            FileOutputStream os = new FileOutputStream(this.filePath);
            PdfStamper stamper = PdfStamper.createSignature(reader, os, '\0');
            // digital signature
            ExternalSignature pks = new PrivateKeySignature(pk, DigestAlgorithms.SHA256, providerPKCS11.getName());
            ExternalDigest digest = new BouncyCastleDigest();
            PdfSignatureAppearance sap = stamper.getSignatureAppearance();
            sap.setReason(reason);
            sap.setLocation(location);
            MakeSignature.signDetached(sap, digest, pks, chain, crlList, ocspClient, tsaClient, 0, CryptoStandard.CMS);
            // set the file to return
            BASE64Encoder encoder = new BASE64Encoder();
            FileInputStream fileStream = new FileInputStream(this.filePath);
            result.pdf = encoder.encode(IOUtils.toByteArray(fileStream));

        } catch (Exception ex) {
            result.error = (ex.toString().length() > 0)? ex.toString(): ex.getMessage();
            System.out.println("Error " + result.error);
        } finally {
            if ((new File (this.filePath)).exists()) {
                if (!this.removeFile(filePath)) {
                    result.error = "Temp PDF file cannot be deleted (" + this.filePath + ")";
                }
            }
        }
        return result;
    }


    /**
    public static void main (String[] args) throws Exception {

        // input
        System.out.println("Starting with inputs");
        FileInputStream stream = new FileInputStream("/tmp/in.pdf");
        BASE64Encoder encoder = new BASE64Encoder();
        String testData = encoder.encode(IOUtils.toByteArray(stream));
        stream.close();
        Certifier certifier = new Certifier();
        PdfProcessorResult result = certifier.getCertifiedFileWithPKCS11(testData, "Test Reason", "Test Location");

        // output
        System.out.println("Checking outputs");
        if (result.error.trim().length() > 0) {
            System.out.println("Error " + result.error);
        } else {
            System.out.println("Writing file");
            String outFile = "/tmp/out.pdf";
            if ((new File(outFile)).exists()) {
                (new File(outFile)).delete();
            }
            BASE64Decoder decoder = new BASE64Decoder();
            FileOutputStream outputStream = new FileOutputStream(outFile);
            outputStream.write(decoder.decodeBuffer(result.pdf));
            outputStream.close();
            System.out.println("Writing done");
        }
    }
    */
}
