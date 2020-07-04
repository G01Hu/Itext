package org.hu.itext.htmlpdf;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.font.FontProvider;
import com.itextpdf.layout.font.FontSet;
import com.itextpdf.layout.property.Property;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import com.itextpdf.layout.property.VerticalAlignment;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @Description
 * @Author huqy
 * @Date 2020/07/04
 */
@Slf4j
public class CreateHtml {

    //resources目录下
    private static final String TEMPLATE_DIR = "/htmlpdf";
    private static final String TEMPLATE_FILE_NAME = "CheckInfo.ftl";
    //生成html的位置
    private static final String HTML_FILE_PATH = "/tmp/mortgage/html/%s.html";
    private static final String LOGO_PATH = "htmlpdf/logo.png";
    //中文字体 注意后边要加,0 具体我也不知道为啥 有小伙伴知道可留言
    public static final String FONT = "htmlpdf/simsun.ttc,0";
    public static final String DEST = "/tmp/mortgage/html/Accessibility.pdf";

    public static void main(String[] args) {
        String htmlpath = getHtml();
        getPdf(htmlpath);
        deleteHtml(htmlpath);
    }

    private static void getPdf(String htmlpath) {
//        可写到本地也可通过http输出流输出
//        HttpServletResponse resp = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getResponse();
//        resp.setContentType("application/octet-stream");
//        try {
//            resp.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(UUID.randomUUID().toString()+FILE_NAME_SUFFIX, "UTF-8"));
//        } catch (UnsupportedEncodingException e) {
//            log.warn("查询结果生成PDF：",e);
//        }
        File file = new File(DEST);
        file.getParentFile().mkdirs();

        try (FileInputStream fileInputStream = new FileInputStream(htmlpath)) {
            FileOutputStream outputStream = new FileOutputStream(file);
            WriterProperties writerProperties = new WriterProperties();
//            http输出流输出
//            PdfWriter pdfWriter = new PdfWriter(resp.getOutputStream(), writerProperties);
            PdfWriter pdfWriter = new PdfWriter(outputStream, writerProperties);
            PdfDocument pdfDoc = new PdfDocument(pdfWriter);
            //转换为pfd中的属性设置对象
            ConverterProperties proper = new ConverterProperties();
            //字体设置，解决中文不显示问题
            FontSet fontSet = new FontSet();

            FontProgram font = FontProgramFactory.createFont(FONT);
            fontSet.addFont(font, PdfEncodings.IDENTITY_H);
            FontProvider fontProvider = new FontProvider(fontSet);
            proper.setFontProvider(fontProvider);
            //添加水印
            pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, new PDFEventHandler());

            HtmlConverter.convertToPdf(fileInputStream, pdfDoc, proper);
            pdfDoc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getHtml() {
        Param param1 = new Param();
        param1.setA("我是A");
        param1.setB("我是B");
        List<String> param2 = new ArrayList<>();
        param2.add("数组第一个元素");
        param2.add("数组第二个元素");
        // 定义数据
        Map<String, Object> root = new HashMap<>();
        root.put("param1", param1);
        root.put("param2", param2);

        Configuration conf = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        //加载模板文件(模板的路径)
        Writer out = null;
        // 定义输出
        UUID uuid = UUID.randomUUID();
        String htmlFilePath = String.format(HTML_FILE_PATH, uuid.toString());
        try {
            //如果不是启动器引入的依赖，这里需要这样写
            conf.setClassForTemplateLoading(CreateHtml.class,TEMPLATE_DIR);
            // 加载模板
            Template template = conf.getTemplate(TEMPLATE_FILE_NAME);

            File file = new File(htmlFilePath);
            file.getParentFile().mkdirs();
            out = new FileWriter(file);
            template.process(root, out);
            return htmlFilePath;
        } catch (IOException | TemplateException e) {
            log.warn("生成HTML模板失败：{} e:", htmlFilePath,e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    log.warn("生成HTML模板失败：{} e:", htmlFilePath,e);
                }
            }
        }
        return null;
    }
    /**
     * PDF事件触发器
     * 生成水印
     */
    static class PDFEventHandler implements IEventHandler {
        private static final String WATERMARK_CONTENT = "操作员姓名：%s 操作员号：%s";
        PdfFont helveticaBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        private PDFEventHandler() throws IOException {
        }

        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdfDoc = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdfDoc);

            String format = String.format(WATERMARK_CONTENT, "二狗", "9527");
            //Add watermark
            Canvas canvas = new Canvas(pdfCanvas, page.getPageSize());
            canvas.setFontColor(ColorConstants.GRAY);
            canvas.setProperty(Property.FONT_SIZE, UnitValue.createPointValue(20));
            canvas.setProperty(Property.FONT, helveticaBold);
            PdfFont f = null;
            ImageData image = null;
            try {
                f = PdfFontFactory.createFont(FONT, PdfEncodings.IDENTITY_H);
                InputStream in = this.getClass().getClassLoader().getResourceAsStream(LOGO_PATH);
                //这里需要使用这个方法的byte[]的重载方法 不然jar启动会报错找不到文件
                image = ImageDataFactory.create(toByteArray(in));
            } catch (IOException e) {
                e.printStackTrace();
            }
            canvas.showTextAligned(new Paragraph(format).setFont(f), 298, 421, pdfDoc.getPageNumber(page),
                    TextAlignment.CENTER, VerticalAlignment.MIDDLE, 45);
            //添加背景
            pdfCanvas.saveState();
            PdfExtGState state = new PdfExtGState().setFillOpacity(0.1f);
            pdfCanvas.setExtGState(state);
            Rectangle rotate = PageSize.A4.rotate();
            pdfCanvas.addImage(image, rotate.getWidth() / 8, rotate.getHeight() / 2, 400, false);
            pdfCanvas.restoreState();

            pdfCanvas.release();
        }
    }
    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024*4];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }
    /**
     * 移除生成的HTML
     *
     * @param htmlPath
     */
    private static void deleteHtml(String htmlPath) {
        try {
            Files.delete(Paths.get(htmlPath));
        } catch (IOException e) {
            log.warn("删除HTML文件失败：path：{}, e:", htmlPath, e);
        }
    }

}

