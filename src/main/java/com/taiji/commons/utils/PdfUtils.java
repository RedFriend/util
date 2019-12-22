package com.taiji.commons.utils;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;
import com.itextpdf.kernel.utils.PageRange;
import com.itextpdf.kernel.utils.PdfSplitter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.property.TextAlignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 处理pdf文档相关工具类
 * 采用新的itext7,优化转换效率
 *
 * @author Ryan.Peng
 * @Since 2019年12月12日20:57:35
 */
public class PdfUtils {

    public static ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 5000, 5000, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(8), new ThreadFactory() {
        private final AtomicInteger mThreadNum = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "my-thread-" + mThreadNum.getAndIncrement());
            System.out.println(t.getName() + " has been created");
            return t;
        }
    });
    private static Logger logger = LoggerFactory.getLogger(PdfUtils.class);

    public static void main(String[] args) throws Exception {
//        splitByPageCount("C:\\Users\\pengh\\Desktop\\test.pdf", "C:\\Users\\pengh\\Desktop\\test", 0);
        imagesToPdf(new File("C:\\Users\\pengh\\Desktop\\test").listFiles(), new File("C:\\\\Users\\\\pengh\\\\Desktop\\\\test2.pdf"));
//        pdfToImages("C:\\Users\\pengh\\Desktop\\test.pdf", "C:\\Users\\pengh\\Desktop\\test");
//        System.out.println(extractText("C:\\Users\\pengh\\Desktop\\test.pdf"));
    }

    /**
     * 多个图片合并成PDF
     * Gif,Jpeg,Png,Tif
     *
     * @param imagePaths 图片路径
     * @param destPath   生成PDF文件路径
     * @author Ryan.Peng
     */
    public static void imagesToPdf2(String[] imagePaths, String destPath) {
        try {
            Long start = System.currentTimeMillis();
            Document document = new Document(new PdfDocument(new PdfWriter(destPath)));
            document.setMargins(0, 0, 0, 0);
            for (String imagePath : imagePaths) {
                Image img = new Image(ImageDataFactory.create(imagePath));
                img.setTextAlignment(TextAlignment.CENTER);
                document.add(img);
            }
            document.close();
            logger.info("合并耗时:{}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            logger.error("多个图片合并成PDF异常", e);
        }
    }

    /**
     * 多个图片合并成PDF
     * Gif,Jpeg,Png,Tif
     *
     * @param imagePaths 图片路径
     * @param destPath   生成PDF文件路径
     * @author Ryan.Peng
     */
    public static void imagesToPdf(String[] imagePaths, String destPath) {
        try {
            Long start = System.currentTimeMillis();
            PdfDocument pdfDocument = new PdfDocument(new PdfWriter(destPath));
            Document document = new Document(pdfDocument);
            for (String imagePath : imagePaths) {
                ImageData imageData = ImageDataFactory.create(imagePath);
                Image img = new Image(imageData, imageData.getWidth(), imageData.getHeight());
                PageSize pageSize = new PageSize(new Rectangle(img.getImageWidth(), img.getImageHeight()));
                PdfCanvas canvas = new PdfCanvas(pdfDocument.addNewPage(pageSize));
                canvas.addImage(imageData, pageSize, false);
            }
            document.close();
            logger.info("合并耗时:{}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            logger.error("多个图片合并成PDF异常", e);
        }
    }

    /**
     * 多个图片合并成PDF
     * Gif,Jpeg,Png,Tif
     *
     * @param files    图片路径
     * @param destFile 生成PDF文件路径
     * @author Ryan.Peng
     */
    public static void imagesToPdf(File[] files, File destFile) {
        String[] imagePaths = Arrays.stream(files)
                .sorted(Comparator.comparing((f) -> Integer.valueOf(f.getName().substring(0, f.getName().lastIndexOf(".")))))
                .map(File::getPath).toArray(String[]::new);
        imagesToPdf(imagePaths, destFile.getPath());
    }

    /**
     * pdf转jpg
     *
     * @param pdfPath   pdf文件路径
     * @param storePath jpg存储路径
     * @author Ryan.Peng
     */
    public static void pdfToImages(String pdfPath, String storePath) {
        try {
            PdfReader reader = new PdfReader(pdfPath);
            PdfDocument document = new PdfDocument(reader);
            Long start = System.currentTimeMillis();
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                PdfDictionary page = document.getPage(i + 1).getPdfObject();
                PdfDictionary resources = page.getAsDictionary(PdfName.Resources);
                PdfDictionary xObjects = resources.getAsDictionary(PdfName.XObject);
                PdfName imgRef = xObjects.keySet().iterator().next();
                PdfStream imgStream = xObjects.getAsStream(imgRef);
                PdfImageXObject pdfImageXObject = new PdfImageXObject(imgStream);
                FileOutputStream fos = new FileOutputStream(storePath + File.separatorChar + (i + 1) + ".jpg");
                fos.write(pdfImageXObject.getImageBytes());
            }
            logger.info("pdf转jpg耗时:{}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            logger.error("转换异常", e);
        }
    }

    /**
     * pdf转jpg
     * 测试过程发现效率不高,废弃使用
     *
     * @param pdfPath
     * @param storePath
     * @param imageType
     * @author Ryan.Peng
     */
    @Deprecated
    public static void pdfToImagesThread(String pdfPath, String storePath, String imageType) {
        try {
            PdfReader reader = new PdfReader(pdfPath);
            PdfDocument document = new PdfDocument(reader);
            Long start = System.currentTimeMillis();
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                PdfDictionary page = document.getPage(i + 1).getPdfObject();
                PdfDictionary resources = page.getAsDictionary(PdfName.Resources);
                PdfDictionary xobjects = resources.getAsDictionary(PdfName.XObject);
                PdfName imgRef = xobjects.keySet().iterator().next();
                PdfStream imgStream = xobjects.getAsStream(imgRef);
                PdfImageXObject pdfImageXObject = new PdfImageXObject(imgStream);
                BufferedImage bufferedImage = pdfImageXObject.getBufferedImage();
                File imageFile = new File(storePath + File.separatorChar + (i + 1) + "." + imageType);
                executor.execute(new Runnable() {
                    private File file;
                    private BufferedImage image;
                    private String type;

                    public Runnable setParam(File file, BufferedImage image, String type) {
                        this.file = file;
                        this.image = image;
                        this.type = type;
                        return this;
                    }

                    @Override
                    public void run() {
                        try {
                            ImageIO.write(image, type, file);
                            System.out.println(Thread.currentThread().getName());
                            System.out.println(file.getAbsolutePath());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.setParam(imageFile, bufferedImage, imageType));
            }
            System.out.println(System.currentTimeMillis() - start);
        } catch (Exception e) {
            logger.error("pdf转jpg异常", e);
        }
    }

    /**
     * 分割pdf
     *
     * @param pdfPath   pdf文件路径
     * @param storePath pdf的存储路径
     * @param pageCount 每个pdf页数,默认为1
     * @author Ryan.Peng
     */
    public static void splitByPageCount(String pdfPath, String storePath, Integer pageCount) {
        try {
            Long start = System.currentTimeMillis();
            File dir = new File(storePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            pageCount = pageCount == null ? 1 : pageCount;

            PdfReader reader = new PdfReader(pdfPath);
            PdfDocument document = new PdfDocument(reader);
            List<PdfDocument> splitDocuments = new PdfSplitter(document) {
                int partNumber = 1;

                @Override
                protected PdfWriter getNextPdfWriter(PageRange documentPageRange) {
                    try {
                        return new PdfWriter(storePath + File.separatorChar + String.valueOf(partNumber++) + ".pdf");
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException();
                    }
                }
            }.splitByPageCount(pageCount);
            for (PdfDocument doc : splitDocuments) {
                doc.close();
            }
            document.close();
            logger.info("分割耗时:{}ms", System.currentTimeMillis() - start);
        } catch (IOException e) {
            logger.error("分割pdf异常", e);
        }
    }

    /**
     * 获取PDF文本内容
     *
     * @param pdfPath   pdf文件路径
     * @param startPage 提取文本的开始页数
     * @param endPage   提取文本的结束页数
     * @return 文本内容
     * @author Ryan.Peng
     */
    public static String extractText(String pdfPath, int startPage, int endPage) {
        StringBuffer text = new StringBuffer();
        try {
            Long start = System.currentTimeMillis();
            PdfDocument pdfDocument = new PdfDocument(new PdfReader(pdfPath));
            for (int i = startPage; i <= endPage; i++) {
                PdfPage pdfPage = pdfDocument.getPage(i);
                text.append(PdfTextExtractor.getTextFromPage(pdfPage));
            }
            logger.info("获取文本耗时:{}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            logger.error("获取文本异常:{}", e.getMessage(), e);
        }
        return text.toString();
    }

    /**
     * 获取PDF文本首页文本内容
     *
     * @param pdfPath pdf文件路径
     * @return 文本内容
     */
    public static String extractTextFirstPage(String pdfPath) {
        return extractText(pdfPath, 1, 1);
    }

    /**
     * 获取PDF全部文本内容
     *
     * @param pdfPath pdf文件路径
     * @return 全部文本内容
     */
    public static String extractText(String pdfPath) {
        return extractText(pdfPath, 1, getPageNumber(pdfPath));
    }

    /**
     * 获取PDF页数
     *
     * @param pdfPath pdf文件路径
     * @return PDF总页数
     * @author Ryan.Peng
     */
    public static int getPageNumber(String pdfPath) {
        int number = -1;
        try {
            number = new PdfDocument(new PdfReader(pdfPath)).getNumberOfPages();
        } catch (Exception e) {
            logger.error("获取PDF总页数异常:{}", e.getMessage(), e);
        }
        return number;
    }
}
