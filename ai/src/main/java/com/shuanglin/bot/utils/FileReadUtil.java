package com.shuanglin.bot.utils;

import lombok.extern.slf4j.Slf4j;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubReader;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class FileReadUtil {

    /**
     * 读取指定路径的文件内容，根据文件类型自动选择读取方式
     * 支持 .txt, .xls, .xlsx, .docx, .epub
     *
     * @param filePath 文件的完整路径（字符串形式）
     * @return 文件内容的字符串表示
     * @throws IOException 如果文件读取失败
     * @throws InvalidFormatException 如果是 Office XML 格式但无效
     */
    public static String readFileContent(String filePath) throws IOException, InvalidFormatException {
        Objects.requireNonNull(filePath, "文件路径不能为空");
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("文件不存在: " + filePath);
        }
        return readFileContent(file);
    }

    /**
     * 读取指定文件对象的内容，根据文件类型自动选择读取方式
     * 支持 .txt, .xls, .xlsx, .docx, .epub
     *
     * @param file 文件对象
     * @return 文件内容的字符串表示
     * @throws IOException 如果文件读取失败
     * @throws InvalidFormatException 如果是 Office XML 格式但无效
     */
    public static String readFileContent(File file) throws IOException, InvalidFormatException {
        String fileExtension = getFileExtension(file.getPath()).toLowerCase();
        StringBuilder content = new StringBuilder();

        switch (fileExtension) {
            case "txt":
                content.append(readTxtFile(file));
                break;
            case "xls":
            case "xlsx":
                content.append(readExcelFile(file));
                break;
            case "docx":
                content.append(readDocxFile(file));
                break;
            case "epub":
                content.append(readEpubFile(file));
                break;
            default:
                throw new UnsupportedOperationException("不支持的文件类型: ." + fileExtension);
        }
        return content.toString();
    }

    // ==================== 私有方法（按顺序放于下方）====================

    /**
     * 读取 .txt 文件的内容
     */
    private static String readTxtFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
        }
        return content.toString();
    }

    /**
     * 读取 .xls (Excel 2003) 和 .xlsx (Excel 2007+) 文件的内容
     * 将所有工作表的内容拼接成一个字符串
     */
    private static String readExcelFile(File file) throws IOException, InvalidFormatException {
        StringBuilder content = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file)) {
            Workbook workbook;
            String fileName = file.getName();

            if (fileName.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fis); // For .xlsx
            } else if (fileName.endsWith(".xls")) {
                workbook = new HSSFWorkbook(fis); // For .xls
            } else {
                throw new IllegalArgumentException("文件类型不支持: " + fileName + " (仅支持 .xls 或 .xlsx)");
            }

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                content.append("--- Sheet: ").append(sheet.getSheetName()).append(" ---").append(System.lineSeparator());

                for (Row row : sheet) {
                    for (Cell cell : row) {
                        String cellValue = getCellValueAsString(cell);
                        content.append(cellValue).append("\t");
                    }
                    content.append(System.lineSeparator());
                }
                content.append(System.lineSeparator());
            }
        }
        return content.toString();
    }

    /**
     * 获取单元格内容并根据类型转换为字符串
     */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            case ERROR:
                return "ERROR:" + cell.getErrorCellValue();
            default:
                return cell.toString();
        }
    }

    /**
     * 读取 .docx (Word 2007+) 文件的内容
     */
    private static String readDocxFile(File file) throws IOException, InvalidFormatException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument docx = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(docx)) {
            return extractor.getText();
        }
    }

    /**
     * 读取 .epub 文件的内容（使用 epublib）
     */
    public static List<ParseResult> readEpubFile(File file){
        List<ParseResult> results = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file)) {
            Book book = new EpubReader().readEpub(fis);
            // 遍历目录（TOC）并提取每个章节内容
            if (book.getTableOfContents() != null) {
                results = extractTocRecursively(book.getContents());
            }
        } catch (Exception e) {
            log.error("读取 EPUB 文件失败: {}", file.getAbsolutePath(), e);
        }
        return results;
    }

    /**
     * 递归遍历 EPUB 目录（TOC），提取每一章内容
     */
    private static List<ParseResult> extractTocRecursively(List<Resource> contents) throws IOException {
        List<ParseResult> results = new ArrayList<>();
        for (Resource resource : contents) {
            ParseResult parseResult = parseHtml(new String(resource.getData(), "UTF-8"));
            results.add(parseResult);
        }
        return results;
    }

    /**
     * 获取文件扩展名（忽略大小写）
     */
    private static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }

    public static ParseResult parseHtml(String htmlString) {
        try {
            // 解析HTML字符串
            Document document = Jsoup.parse(htmlString);

            // 提取title
            String title = document.title();

            // 提取所有p标签的内容
            Elements pElements = document.select("p");
            List<String> contentList = new ArrayList<>();

            for (Element pElement : pElements) {
                String text = pElement.text().trim();
                if (!text.isEmpty()) {
                    contentList.add(text);
                }
            }

            return new ParseResult(title, contentList);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) throws IOException, InvalidFormatException {
        readEpubFile(new File("C:\\Users\\Shuan\\Downloads\\斗破苍穹-天蚕土豆.epub"));
    }

    public static class ParseResult {
        private String title;
        private List<String> contentList;

        public ParseResult(String title, List<String> contentList) {
            this.title = title;
            this.contentList = contentList;
        }

        public String getTitle() {
            return title;
        }

        public List<String> getContentList() {
            return contentList;
        }
    }
}