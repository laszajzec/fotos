package zec.unzip;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public class UnzipFileTree {

	private String sourceBaseDir;
	private static final String targetBaseDir = "c:/temp/zsu";

	public static void main(String[] args) throws IOException {
		new UnzipFileTree().doIt();
	}

	private void doIt() throws IOException {
		sourceBaseDir = "D:\\ZsuBackup\\LwC\\Users\\Susan";
		descend();
		sourceBaseDir = "D:\\ZsuBackup\\LwC\\temp";
		descend();
	}
	
	private void descend() throws IOException {
		try (Stream<Path> filesWalk = Files.walk(Paths.get(sourceBaseDir))) {
			filesWalk.forEach(f -> filter(f));
		}
	}
	
	private void filter(Path aFile) {
		if (!Files.isRegularFile(aFile)) return;
		String fileAsString = aFile.toString();
		if (fileAsString.contains("Downloads")) return;
		if (fileAsString.contains("Tremitas")) return;
		if (fileAsString.toLowerCase().endsWith(".zip")) {
			unzip(aFile);
		} else {
			copy(aFile);
		}
	}

	private void unzip(Path aFile) {
		String zipFile = aFile.toString();
		try {
			String destFolder = buildDestFolder(aFile, sourceBaseDir);
			System.out.format("unzip %s to %s%n", aFile.toString(), destFolder);
			unzipTech(zipFile, destFolder);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void copy(Path aFile) {
		try {
			String dest = aFile.toString().replace(sourceBaseDir, targetBaseDir);
			System.out.format("copy %s to %s%n", aFile.toString(), dest);
			Files.copy(aFile, Paths.get(dest));
		} catch (IOException e) {
			System.out.format("!!! copy error %s in %s%n", e.getMessage(), aFile.toString());
		}
	}
	
	private void unzipTech(String zipFileAsString, String destFolder) throws FileNotFoundException, IOException {
		try (ZipFile zipFile = new ZipFile(zipFileAsString)){
		    zipFile.extractAll(destFolder);
		} catch (ZipException e) {
			System.out.format("!!! zip  error %s in %s%n", e.getMessage(), zipFileAsString);
		}
	}

	private String buildDestFolder(Path aFile, String sourceBaseDir) throws IOException {
		String destFolder = aFile.toString().replace(sourceBaseDir, targetBaseDir);
		destFolder = destFolder.replace(aFile.getFileName().toString(), "");
		Files.createDirectories(Paths.get(destFolder));
		return destFolder;
	}

}
