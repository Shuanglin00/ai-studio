package com.shuanglin.bot.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 用于读取 ZIP 压缩包中项目文件的工具类。
 * 能够识别常见的文本文件（代码、配置文件、文档等）并提取其内容。
 */
public class ProjectReaderUtil {

	// 常见的文本文件扩展名，用于识别是否是文本文件。
	// 你可以根据实际项目需求，在此列表中添加或删除扩展名。
	private static final String[] TEXT_EXTENSIONS = {
			// 编程语言文件
			".java", ".kt", ".py", ".js", ".ts", ".jsx", ".tsx", ".html", ".htm", ".css", ".scss", ".sass", ".less",
			".xml", ".json", ".yml", ".yaml", ".properties", ".env", ".ini", ".conf",
			".md", ".txt", ".rst", ".adoc", // 文档
			".sql", // 数据库脚本
			".sh", ".bash", ".ksh", ".zsh", // Shell 脚本
			".bat", ".cmd", // Windows 脚本
			".gradle", ".groovy", ".gradle.kts", // Gradle
			".rb", // Ruby
			".php", // PHP
			".cs", // C#
			".go", // Go
			".rs", // Rust
			".scala", // Scala
			".c", ".cpp", ".h", ".hpp", ".cxx", ".hxx", // C/C++
			".feature", ".feature.md", ".feature.txt", ".feature.json", ".feature.yaml", ".feature.yml", ".gherkin" // Gherkin/Cucumber
			// 其他可能遇到的文本文件类型
	};

	// 一些常见但没有扩展名，或以点开头的文本文件，需要特殊处理
	private static final String[] SPECIAL_TEXT_FILENAMES = {
			"makefile", "makefile.unix", "makefile.windows", // Makefiles
			"dockerfile", "docker-compose.yml", // Docker
			".gitignore", ".gitattributes", ".gitmodules", // Git 配置
			".travis.yml", ".gitlab-ci.yml", "jenkinsfile", // CI/CD 配置
			"pom.xml", // Maven
			"build.gradle", "build.gradle.kts", // Gradle
			"package.json", "package-lock.json", // npm/yarn
			"requirements.txt", "setup.py", "pyproject.toml", // Python
			"composer.json", "composer.lock", // PHP Composer
			".editorconfig", ".prettierrc", ".eslintrc" // 代码风格配置
	};

	/**
	 * 读取 ZIP 压缩包内所有被识别为文本的文件内容，并组合成一个字符串。
	 * 主要用于分析 GitHub 等代码项目。
	 *
	 * @param zipFile 要读取的 ZIP 文件
	 * @return ZIP 文件内所有文本文件的内容组合
	 * @throws IOException 如果读取 ZIP 文件或其内部文件时发生错误
	 */
	public static String readAllProjectTextFiles(File zipFile) throws IOException {
		StringBuilder allContent = new StringBuilder();

		if (!zipFile.exists() || !zipFile.isFile()) {
			throw new IllegalArgumentException("指定的 ZIP 文件不存在或不是一个有效文件: " + zipFile.getAbsolutePath());
		}

		// 使用 ZipFile 来读取 ZIP 文件，确保资源被正确管理
		try (ZipFile zip = new ZipFile(zipFile)) {
			Enumeration<? extends ZipEntry> entries = zip.entries();

			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();

				// 忽略 ZIP 文件中的目录条目
				if (!entry.isDirectory()) {
					String entryName = entry.getName();
					String lowerCaseEntryName = entryName.toLowerCase();

					// 检查文件是否是文本文件
					if (isTextFile(entryName, lowerCaseEntryName)) {
						allContent.append("--- File: ").append(entryName).append(" ---").append(System.lineSeparator());

						try (InputStream entryInputStream = zip.getInputStream(entry)) {
							// 以 UTF-8 编码读取文件内容
							String fileContent = readInputStreamAsText(entryInputStream, StandardCharsets.UTF_8);
							allContent.append(fileContent);
						} catch (IOException e) {
							// 如果读取文件时发生 IO 错误，记录错误信息并继续处理下一个文件
							allContent.append("Error reading file content: ").append(e.getMessage()).append(System.lineSeparator());
							allContent.append("--- END File: ").append(entryName).append(" (Read Error) ---").append(System.lineSeparator());
						}
					} else {
						// 对于非文本文件，可以根据需要进行处理：
						// 1. 忽略（当前行为）
						// 2. 记录其存在，例如 allContent.append("--- Skipped Binary File: ").append(entryName).append(" ---").append(System.lineSeparator());
						// 3. 尝试以文本形式读取（可能得到乱码），例如 readInputStreamAsText(entryInputStream, StandardCharsets.ISO_8859_1)
						// 建议在此阶段仅记录或忽略，避免处理非文本内容的复杂性。
						// System.out.println("Skipping non-text file: " + entryName);
					}
					allContent.append(System.lineSeparator()); // 在每个文件内容后添加一个空行，以分隔不同文件
				}
			}
		}
		return allContent.toString();
	}

	/**
	 * 判断一个文件是否是文本文件，基于其文件名和扩展名。
	 *
	 * @param entryName 文件名（可能包含路径）
	 * @param lowerCaseEntryName 文件名（小写，可能包含路径）
	 * @return 如果是文本文件则返回 true，否则返回 false
	 */
	public static boolean isTextFile(String entryName, String lowerCaseEntryName) {
		// 首先检查是否有扩展名，或者文件名本身是否是特殊文本文件名
		int dotIndex = entryName.lastIndexOf('.');

		// 检查是否是特殊处理的无扩展名或点开头文件名
		for (String specialFilename : SPECIAL_TEXT_FILENAMES) {
			if (lowerCaseEntryName.equals(specialFilename) || lowerCaseEntryName.startsWith(specialFilename + ".")) { // 匹配如 .gitignore.global
				return true;
			}
		}

		// 如果有扩展名，则检查扩展名是否在支持的文本扩展名列表中
		if (dotIndex > 0 && dotIndex < entryName.length() - 1) {
			String extension = entryName.substring(dotIndex); // 获取包含点的扩展名，例如 ".java"
			for (String textExt : TEXT_EXTENSIONS) {
				if (lowerCaseEntryName.endsWith(textExt)) {
					return true;
				}
			}
		}

		// 如果不是上述情况，则认为是二进制文件或不确定类型的文件
		return false;
	}

	/**
	 * 从 InputStream 读取内容，并尝试使用指定的 Charset 将其转换为 String。
	 * 如果读取失败，会捕获 IOException 并返回错误信息。
	 *
	 * @param inputStream 要读取的输入流
	 * @param charset     用于解码字节的字符集
	 * @return 文件内容的字符串表示，或读取错误时的错误信息
	 * @throws IOException 如果在打开 InputStreamReader 时发生错误
	 */
	private static String readInputStreamAsText(InputStream inputStream, Charset charset) throws IOException {
		StringBuilder stringBuilder = new StringBuilder();
		// 使用 InputStreamReader 来正确处理字符编码
		try (InputStreamReader reader = new InputStreamReader(inputStream, charset)) {
			char[] buffer = new char[8192]; // 4KB 缓冲区，可以根据需要调整
			int charsRead;
			// 循环读取字符，直到流结束
			while ((charsRead = reader.read(buffer, 0, buffer.length)) != -1) {
				stringBuilder.append(buffer, 0, charsRead);
			}
		} catch (IOException e) {
			// 如果读取过程中发生 IO 错误，重新抛出，由调用者处理
			throw e;
		}
		return stringBuilder.toString();
	}

	// --- Example Usage ---
	public static void main(String[] args) {
		// 请将 "path/to/your/github_project.zip" 替换为你实际的 ZIP 文件路径
		File githubProjectZip = new File("path/to/your/github_project.zip");

		try {
			System.out.println("开始读取 ZIP 文件: " + githubProjectZip.getAbsolutePath());
			String projectOverview = ProjectReaderUtil.readAllProjectTextFiles(githubProjectZip);
			System.out.println("\n--- Project Content Overview ---");
			System.out.println(projectOverview);
			System.out.println("--- End of Project Content ---");
		} catch (IOException e) {
			System.err.println("读取 ZIP 文件时发生错误: " + e.getMessage());
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			System.err.println("文件路径错误: " + e.getMessage());
		}
	}
}