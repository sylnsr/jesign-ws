package itext;

import com.itextpdf.text.pdf.parser.LineSegment;
import com.itextpdf.text.pdf.parser.LocationTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.Matrix;
import com.itextpdf.text.pdf.parser.TextRenderInfo;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class TokenLocationExtractionStrategy extends LocationTextExtractionStrategy {

    //region Privates
    private List<TokenCharacterLocation> tokenLocationalResult = new ArrayList<TokenCharacterLocation>();
    private String tokenString;
    private String token;
    private String inspectionString;
    private TextChunk lastChunk;
    private int currentPage;
    private float pageWidth;
    private float pageHeight;
    private int tokenFindCount = 0;

    private static void echo(String string) {
        System.out.println(string);
    }

    private final List<TextChunk> locationalResult = new ArrayList<TextChunk>();

    private String getChunkSpacer(boolean maintainNewLine, TextChunk chunk, TextChunk lastChunk) {
        String result = "";

        try {
            // if the text chunk is on the same line the we need to discern if a space is needed
            if (chunk.sameLine(lastChunk)) {

                String chunk_text = chunk.getText();
                // the chunk should default to a space if the chunk is empty
                if (chunk.getText().length() == 0) {
                    result = " ";

                    // otherwise inspect the chunk we have
                } else {
                    float dist = chunk.distanceFromEndOf(lastChunk);

                    // see: http://grepcode.com/file/repo1.maven.org/maven2/com.itextpdf/itextpdf/5.5.0/com/itextpdf/text/pdf/parser/LocationTextExtractionStrategy.java#LocationTextExtractionStrategy.isChunkAtWordBoundary%28com.itextpdf.text.pdf.parser.LocationTextExtractionStrategy.TextChunk%2Ccom.itextpdf.text.pdf.parser.LocationTextExtractionStrategy.TextChunk%29
                    if (this.isChunkAtWordBoundary(chunk, lastChunk))
                        result = " ";

                        // otherwise we only insert a blank space if ...
                    else if (
                        // the leading character of the current string isn't a space
                            dist > chunk.getCharSpaceWidth() / 2.0f && chunk_text.charAt(0) != ' '
                            &&
                            // AND the trailing character of the previous string wasn't a space
                            lastChunk.getText().charAt(lastChunk.getText().length() - 1) != ' '
                            )
                        result = " ";
                }

                // otherwise if it's on a different line then separate with a space or a new line, as requested
            } else {
                if (maintainNewLine)
                    result = "\n";
                else
                    result = " ";
            }

        } catch (Exception ex) {
            System.out.println("Exception in getChunkSpacer. Using return default value.");
        }
        return result;
    }


    private float shortenFloat(float inFloat) {
        DecimalFormat df = new DecimalFormat("0.00");
        return Float.parseFloat(df.format(inFloat));
    }
    //endregion

    public TokenLocationExtractionStrategy(
            int currentPage,
            float pageWidth,
            float pageHeight,
            String token,
            String tokenString)
    {
        this.currentPage = currentPage;
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        this.tokenString = tokenString + token.toString();
        this.token = token;
    }

    public List<TokenCharacterLocation> getTokenLocations() {
        return tokenLocationalResult;
    }

    public void renderText(TextRenderInfo renderInfo) {
        float top;
        float left;
        String chunkWithSpace;

        try {
            LineSegment segment = renderInfo.getBaseline();
            if (renderInfo.getRise() != 0) {
                // remove the rise from the baseline - we do this because the text from a super/subscript render operations
                // should probably be considered as part of the baseline of the text the super/sub is relative to
                Matrix riseOffsetTransform = new Matrix(0, -renderInfo.getRise());
                segment = segment.transformBy(riseOffsetTransform);
            }
            TextChunk location = new TextChunk(
                    renderInfo.getText(),
                    segment.getStartPoint(),
                    segment.getEndPoint(),
                    renderInfo.getSingleSpaceWidth());
            locationalResult.add(location);

            // maintain spacing
            if (lastChunk == null)
                chunkWithSpace = "";
            else
                chunkWithSpace = getChunkSpacer(false, location, lastChunk) + location.getText();

            // keep a small inspection chunk to look into to see if we can find our string
            inspectionString += chunkWithSpace;

            // limit the size of the inspection chunk to save resources
            if (inspectionString.length() > 99)
                inspectionString = inspectionString.substring(inspectionString.length() - 99);

            // location of the token
            if (inspectionString.contains(this.tokenString)) {
                tokenFindCount++;
                top = shortenFloat(((pageHeight - location.getEndLocation().get(1)) / pageHeight) * 100);
                left = shortenFloat((location.getEndLocation().get(0) / pageWidth) * 100);
                tokenLocationalResult.add( new TokenCharacterLocation( currentPage, tokenFindCount, top, left ) );
                // start a new inspection string after we found what we're looking for
                inspectionString = inspectionString.substring(inspectionString.lastIndexOf(this.token) + 1);
            }
            this.lastChunk = location;
        } catch (Exception e) {
            echo("renderText" + e.getMessage());
            throw e;
        }
    }

    public class TokenCharacterLocation {
        private int pageNumber;
        private int sequenceId;
        private float topPercent;
        private float leftPercent;

        public TokenCharacterLocation(int pageNumber, int sequenceId, float topPercent, float leftPercent) {
            this.pageNumber = pageNumber;
            this.topPercent = topPercent;
            this.leftPercent = leftPercent;
            this.sequenceId = sequenceId;
        }

        public String getVectorJson() {
            return "[" + pageNumber + "," +
                   "" + sequenceId + "," +
                   "" + topPercent + "," +
                   "" + leftPercent + "]";
        }
    }
}