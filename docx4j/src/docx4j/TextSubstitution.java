/*
 *
    docx4j and docx4j-ws are licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.

    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */

package docx4j;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.WebParam;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import org.docx4j.model.structure.HeaderFooterPolicy;
import org.docx4j.model.structure.SectionWrapper;
import org.docx4j.openpackaging.parts.WordprocessingML.FooterPart;
import org.docx4j.openpackaging.parts.WordprocessingML.HeaderPart;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.*;

import org.docx4j.fonts.IdentityPlusMapper;
import org.docx4j.model.datastorage.BindingHandler;
import org.docx4j.model.datastorage.CustomXmlDataStorage;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.CustomXmlDataStoragePart;


/**
 * This is a WORK IN PROGRESS. I have just recently started this project
 * so not everything is working yet. What is this project? This project uses
 * sample code from the docx4j project and uses it in another context, as a web
 * service that takes a docx file and an XML file and replaces the customXML
 * data part in the docx file with the provided XML file.
 */

@WebService
public final class TextSubstitution extends ws.PdfProcessor {

    private String lastError;

    @WebMethod
    public SubstitutionResult getPdfFileFromName (
            @WebParam(name="fileName", partName="fileName") String fileName
    ) {
        SubstitutionResult result = new SubstitutionResult();
        String filePath = this.getFolderPath() + fileName;

        try {

            // get the file as a base64 string
            BASE64Encoder encoder = new BASE64Encoder();
            FileInputStream fileStream = new FileInputStream(filePath);

            result.pdf = encoder.encode(IOUtils.toByteArray(fileStream));
            result.error = "";

        } catch (FileNotFoundException e) {
            result.error = "lost";
        } catch (IOException e) {
            result.error = "unreadable";
        }

        return result;
    }

    @WebMethod
    public SubstitutionResult getSubstitutionFile (
            @WebParam(name="templateFile", partName="templateFile") String templateFile,
            @WebParam(name="xmlData", partName="xmlData") String xmlData,
            @WebParam(name="addWatermark", partName="addWatermark") int addWatermark
    ) {
        SubstitutionResult result = new SubstitutionResult();
        String originalFileName;
        String watermarkedFileName;
        String error;
        int pages = 0;

        try {

            PreparationResult inputs = this.getInputStreamsFromWebServiceValues(templateFile, xmlData);
            while (this.setFilePath(this.getFolderPath()) == false) {
                System.out.println("UUID collision for file name on " + this.fileName);
            }

            if (!this.substituteUsingDocx4j(inputs.template, inputs.xmlData)) {
                error = this.lastError;

            } else {
                originalFileName = this.fileName;
                watermarkedFileName = "wm" + originalFileName;
                pages = this.getPageCount();

                /**
                 * At this point result.pdf points to the PDF file (by name) and is not an actual PDF file.
                 * We need to make it a base64 encoded file. We either make a watermarked version or use the original.
                 * In either case result.pdf will change from a string that is the file name to a string that is a
                 * base64 encoded PDF that we need to return to the client. Since we read the PDF file from disk
                 * and encoded it to return to the client. We also need to remove the files that are still on disk.
                 */

                // if a watermark version is needed
                if (addWatermark > 0) {
                    // create it
                    error = this.addWaterMark();
                    if (error == null) {
                        // set the result to the watermarked version of the file
                        result = getPdfFileFromName(watermarkedFileName);
                        error  = result.error;
                        // delete the original file because the new with the watermark file is being passed as a result
                        error += this.deleteFileFromName(originalFileName);
                        // delete the watermark version as well, after we set the result
                        error += this.deleteFileFromName(watermarkedFileName);
                    }

                // if the original version is needed
                } else {
                    result = this.getPdfFileFromName(originalFileName);
                    error = result.error;
                }
            }

            // catch-all for any processes above which may have set "error"
            if (error.length() > 0) {
                result.error = error;
                result.pages = 0;
                result.pdf = "";
            } else {
                result.pages = pages;
                result.error = "";
            }







        // catch for exceptional problems
        } catch (Exception e) {
            result.error = e.getMessage();
            result.pages = 0;
            result.pdf = "";
        }

        return result;
    }



    //region Privates


    private String deleteFileFromName (String fileName) {
        String result = "";
        String filePath = this.getFolderPath() + fileName;

        try {
            if ( ! (new File(filePath).delete()) ) {
                result = "Could not delete " + filePath;
            }
        } catch (Exception e) {
            result = e.getMessage();
        }
        return result;
    }

    private String getFolderPath () {
        String result;
        try {
            Properties props = new Properties();
            props.load(getClass().getResourceAsStream("/resources/config.properties"));

            result = (props.getProperty("folderpath"));

            if (result == null) {
                result = "/var/opt/docx4j/docs/";
            }

        } catch (IOException e) {
            result = "/var/opt/docx4j/docs/";
        }

        return result;
    }


    private PreparationResult getInputStreamsFromWebServiceValues (String templateFile, String xmlData) throws IOException{

        PreparationResult result = new PreparationResult();
        BASE64Decoder decoder = new BASE64Decoder();
        xmlData = xmlData.trim();

        result.template = new ByteArrayInputStream(decoder.decodeBuffer(templateFile));
        xmlData = xmlData.substring(9, xmlData.length()-3); // strip CDATA tags from start and end
        result.xmlData = new ByteArrayInputStream(xmlData.getBytes());

        return result;
    }

    private boolean substituteUsingDocx4j (InputStream docxFile, InputStream xmlData) {
        boolean result = false;
        HeaderPart header = null;
        FooterPart footer = null;

        try {
            WordprocessingMLPackage wordMLPackage;
            wordMLPackage = WordprocessingMLPackage.load(docxFile);
            if (wordMLPackage.getCustomXmlDataStorageParts().isEmpty()) {
                this.lastError = "No custom XML data storage parts were found";
                return false;
            }
            // grab the first custom XML data storage part
            CustomXmlDataStoragePart customXmlDataStoragePart =
                    (CustomXmlDataStoragePart)wordMLPackage.getCustomXmlDataStorageParts().values().toArray()[0];
            // get a reference to the data in it
            CustomXmlDataStorage customXmlDataStorage = customXmlDataStoragePart.getData();
            // replace it's data with the new XML data
            customXmlDataStorage.setDocument(xmlData);
            // get references to the section wrappers to get references to the header and footer
            List<SectionWrapper> sectionWrappers = wordMLPackage.getDocumentModel().getSections();
            // get references to the header and footer
            for (SectionWrapper sw : sectionWrappers) {
                HeaderFooterPolicy hfp = sw.getHeaderFooterPolicy();
                if (hfp.getDefaultHeader()!=null) {
                    header = hfp.getDefaultHeader();
                    footer = hfp.getDefaultFooter();
                }
            }
            // bindings must be applied before the DOCX can be exported to HTML / PDF
            BindingHandler.applyBindings(wordMLPackage.getMainDocumentPart());
            BindingHandler.applyBindings(header);
            BindingHandler.applyBindings(footer);
            // save as PDF
            wordMLPackage.setFontMapper(new IdentityPlusMapper());
            org.docx4j.convert.out.pdf.PdfConversion conversion = new org.docx4j.convert.out.pdf.viaXSLFO.Conversion(wordMLPackage);
            OutputStream stream = new FileOutputStream(this.filePath);
            org.docx4j.convert.out.pdf.viaXSLFO.PdfSettings pdfSettings = new org.docx4j.convert.out.pdf.viaXSLFO.PdfSettings();
            conversion.output(stream, pdfSettings);
            // save as new DOCX
            //wordMLPackage.save(new java.io.File(folderPath + "/document.docx"));

            docxFile = null;
            wordMLPackage = null;
            result = true;
        } catch (Exception exception) {
            this.lastError = exception.getMessage();
        }
        return result;
    }

    private int getPageCount () throws IOException
    {
        int result;
        // get the number of pages in the file
        PdfReader reader = new PdfReader(this.filePath);
        result = reader.getNumberOfPages();
        reader.close();
        return result;
    }

    private String addWaterMark () throws IOException, DocumentException {
        String result = null;
        String newFileName = this.getFolderPath() + "wm" + this.fileName;

        try {
            // try to delete the file if it already exists so that we refresh the watermark
            // if it fails then at least we have the existing version which can be deleted manually
            File target = new File(newFileName);
            if (target.exists()) { if ( ! target.delete() ){
                return "existing"; }
            }

            // add the watermark
            PdfReader reader = new PdfReader(this.filePath);
            PdfStamper stamp = new PdfStamper(reader, new FileOutputStream(newFileName));
            Image watermark_image = Image.getInstance(this.getFolderPath() + "doc-watermark.png");
            watermark_image.setAbsolutePosition(5, 5);
            int i = 0;
            PdfContentByte add_watermark;

            // loop the pages adding the watermark
            while (i < reader.getNumberOfPages()) {
                i++;
                add_watermark = stamp.getUnderContent(i);
                add_watermark.addImage(watermark_image);
            }
            stamp.close();
            reader.close();
        }
        catch (Exception e) {
            result = e.getMessage();
        }
        return result;
    }


    //endregion
}