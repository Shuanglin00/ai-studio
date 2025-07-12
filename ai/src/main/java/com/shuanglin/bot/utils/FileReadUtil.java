package com.shuanglin.bot.utils;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.hwpf.HWPFDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

public class FileReadUtil {

    /**
     * 读取指定路径的文件内容，根据文件类型自动选择读取方式
     * 支持 .txt, .xls, .xlsx, .doc, .docx
     *
     * @param filePath 文件的完整路径
     * @return 文件内容的字符串表示 (对于 Excel，返回的是所有工作表的文本表示)
     * @throws IOException 如果文件读取失败
     * @throws InvalidFormatException 如果是 Office XML 格式但无效
     */
    public static String readFileContent(File file) throws IOException, InvalidFormatException {
        String fileExtension = getFileExtension(file.getPath());
        StringBuilder content = new StringBuilder();

        switch (fileExtension) {
            case "txt":
                content.append(readTxtFile(file));
                break;
            case "xls":
            case "xlsx":
                content.append(readExcelFile(file));
                break;
            case "doc":
                content.append(readDocFile(file));
                break;
            case "docx":
                content.append(readDocxFile(file));
                break;
            default:
                throw new UnsupportedOperationException("不支持的文件类型: ." + fileExtension);
        }
        return content.toString();
    }

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
                        // 获取单元格内容并转换为字符串
                        String cellValue = getCellValueAsString(cell);
                        content.append(cellValue).append("\t"); // 使用 Tab 分隔单元格
                    }
                    content.append(System.lineSeparator()); // 每行结束后换行
                }
                content.append(System.lineSeparator()); // 每个工作表结束后空一行
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
                // 可以选择评估公式结果，这里简单返回公式字符串
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
     * 读取 .doc (Word 2003) 文件的内容
     */
    private static String readDocFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             HWPFDocument doc = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(doc)) {
            return extractor.getText();
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
     * 获取文件扩展名
     */
    private static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }
}