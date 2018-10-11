package com.sylweb.listedecourses;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.print.PrintAttributes;
import android.print.pdf.PrintedPdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by sylvain on 01/10/2018.
 */

public class PDFUtils {

    private static final int TOP_PAGE_PADDING  =  10;
    private static final int PADDING_HEIGHT    =   5;
    private static final int LEFT_PAGE_PADDING =  10;


    public static String exportReportAsPDF(Context c, ArrayList<Article> data) {

        String pdfName = "Liste_%d.pdf";
        pdfName = String.format(pdfName, Calendar.getInstance().getTimeInMillis());

        String pdfPath = c.getFilesDir().getAbsolutePath()+ File.separator+"pdf"+File.separator;
        checkPDFPath(pdfPath, pdfName);

        PrintAttributes pdfPrintAttrs = new PrintAttributes.Builder().
                setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME).
                setMediaSize(PrintAttributes.MediaSize.ISO_A4.asPortrait()).
                setResolution(new PrintAttributes.Resolution("pdf", "pdf",300, 300)).
                setMinMargins(PrintAttributes.Margins.NO_MARGINS).
                build();

        // open a new document
        PrintedPdfDocument document = new PrintedPdfDocument(c,pdfPrintAttrs);

        int PAGE_HEIGHT = document.getPageHeight();
        int PAGE_WIDTH = document.getPageWidth();

        // start a page
        PdfDocument.Page page = document.startPage(1);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFakeBoldText(true);
        paint.setColor(Color.BLACK);

        float posY = TOP_PAGE_PADDING;

        Rect bounds = new Rect();
        String headerText = "Liste de courses";
        paint.setTextSize(24.0f);
        paint.getTextBounds(headerText, 0, headerText.length(), bounds);
        posY += bounds.height();
        page.getCanvas().drawText(headerText,PAGE_WIDTH/2.0f-bounds.width()/2.0f,posY,paint);



        posY += PADDING_HEIGHT * 8.0f;
        paint.setFakeBoldText(false);
        paint.setTextSize(18.0f);

        for(Article article : data) {
            String text = String.format("%d - %s",article.quantity,article.name);
            page.getCanvas().drawText(text,LEFT_PAGE_PADDING,posY,paint);
            paint.getTextBounds(text, 0, text.length(), bounds);
            posY += (bounds.height() + 2.0f * PADDING_HEIGHT);
        }

        // finish the last page
        document.finishPage(page);

        // write the document content
        String outFileName = pdfPath + pdfName;
        try {
            OutputStream mOutputStream = new FileOutputStream(outFileName);
            document.writeTo(mOutputStream);
        }
        catch (Exception ex) {

        }

        //close the document
        document.close();

        return pdfName;
    }

    private static void checkPDFPath(String pdfPath, String pdfName) {
        final String mPath = pdfPath + pdfName;
        final File file = new File(mPath);
        if (!file.exists()) createPDFFile(pdfPath, pdfName);
    }

    private static void createPDFFile(String pdfPath, String pdfName) {
        try {
            File file = new File(pdfPath+File.separator);
            if(!file.exists()) file.mkdirs();
            file = new File(pdfPath+pdfName);
            file.createNewFile();
        }
        catch (Exception ex) {
        }
    }
}
