package zec.fotos;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.swing.JFileChooser;
import javax.swing.UIManager;

public class RenameFotos {
	
	private static final String targetDir = "c:\\Temp\\Pictures\\";

	private Pattern[] patterns = {
			Pattern.compile("^\\d{8}+_\\d{6}+$", Pattern.CASE_INSENSITIVE),               // 1 yyyymmdd_nnnn
			Pattern.compile("^\\d{8}+_\\d{6}+_\\d\\d$", Pattern.CASE_INSENSITIVE),        // 2 yyyymmdd_nnnn_nn
			Pattern.compile("^\\d{8}+_\\d{6}+_\\d{3}+$", Pattern.CASE_INSENSITIVE),       // 3 yyyymmdd_nnnn_nnn
			Pattern.compile("^\\d{4}+-\\d\\d-\\d\\d_\\d{6}+$", Pattern.CASE_INSENSITIVE), // 4 yyyy-mm-dd_nnnnnn
			Pattern.compile("^image_\\d{8}+_\\d{6}+_\\d\\d$", Pattern.CASE_INSENSITIVE),  // 5 image_yyyymmdd_nnnnnn_nn
			Pattern.compile("^IMG-\\d{8}+-WA\\d{4}+$", Pattern.CASE_INSENSITIVE),         // 6 IMG-yyyymmdd-WAnnnn
			Pattern.compile("^IMG-\\d{8}+_WA\\d{4}+$", Pattern.CASE_INSENSITIVE),         // 7 IMG_yyyymmdd_WAnnnn
			Pattern.compile("^vid-\\d{8}+-WA\\d{4}+$", Pattern.CASE_INSENSITIVE)          // 8 VID-yyyymmdd-WAnnnn
	};

	private static final List<String> fileTypes = List.of(".jpg", ".jpeg", ".heic", ".mp4");

	private List<String> names = new ArrayList<>();
	private BufferedWriter writer;
	private int fileIndex = 1;
	private final Map<String, List<Path>> candidates = new HashMap<>();

	/*
	 * yyyymmdd_nnnn
	 * yyyy-mm-dd_nnnnnn
	 * image_yyyymmdd_nnnnnn_nn
	 * IMG-yyyymmdd-WAnnnn
	 * VID-yyyymmdd-WAnnnn
	 */
	public static void main(String[] args) throws IOException {
		new RenameFotos(args);
	}

	public RenameFotos(String[] args) throws IOException {
		Path dir;
		if (args.length > 0 && !args[0].isEmpty()) {
			dir = Paths.get(args[0]);
			if (!Files.exists(dir) || !Files.isDirectory(dir)) {
				System.out.format("%s is not exits or not a diractory%n", args[0]);
				return;
			}
		} else {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Choose directory");    
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			fileChooser.showOpenDialog(null);
			File selected = fileChooser.getSelectedFile();
			if (selected != null) {
				dir = selected.toPath();
			} else {
				return;
			}
		}
		System.out.println("Searching in " + dir);
		String cmdFile = targetDir + "cp.cmd";
		if (Files.exists(Paths.get(cmdFile))) {
			Files.delete(Paths.get(cmdFile));
		}
		writer = new BufferedWriter(new FileWriter(cmdFile, true));
		recurse(dir);
		writer.close();
		writer = null;
		outAlternativeCmd();
		System.out.println("Finished");
	}

	private void recurse(Path dir) throws IOException {
		try (Stream<Path> walkStream = Files.walk(dir)) {
			walkStream.filter(p -> p.toFile().isFile()).forEach(f -> {
				try {
					checkName(f);
					checkName2(f);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
	}

	private void checkName(Path file) throws IOException {
		Matcher m = null;
		boolean nameOK = false;
		boolean typeOK = false;
		int patternIndex = 0;
		String fileName = null;
		String fileType = null;

		for (; patternIndex < patterns.length; patternIndex++) {
			String fileNameAndType = file.getFileName().toString();
			int dotPos = fileNameAndType.lastIndexOf('.');
			if (dotPos > 1) {
				fileName = fileNameAndType.substring(0, dotPos);
				fileType = fileNameAndType.substring(dotPos).toLowerCase();
				typeOK = fileTypes.contains(fileType);
				m = patterns[patternIndex].matcher(fileName);
				if (m.matches()) {
					nameOK = true;
					break;
				}
			}
		}
		if (nameOK && typeOK) {
			String nfile = getNormalisedName(file, patternIndex+1);
			candidates.computeIfAbsent(nfile, k -> new ArrayList<Path>()).add(file);
		} else {
			System.out.format("Unknown format: %s%n", file);
		}
	}

	private void checkName2(Path file) throws IOException {
		String fileNameAndType = file.getFileName().toString();
		String fileName = null;
		String fileType = null;
		int dotPos = fileNameAndType.lastIndexOf('.');
		if (dotPos > 1) {
			fileName = fileNameAndType.substring(0, dotPos);
			fileType = fileNameAndType.substring(dotPos).toLowerCase();
		} else {
			fileName = fileNameAndType;
			fileType = "xxx";
		}
		String unique = String.format("P%04d", fileIndex);
		fileIndex++;
		createCopyStmt(file, compose(targetDir, unique, fileType));
	}
	
	private String getNormalisedName(Path origFile, int patternNum) {
		String fileNameAndType = origFile.getFileName().toString();
//		String prefix = origFile.toString().replace(fileNameAndType, "");
		String fileName = null;
		String fileType = null;
		int dotPos = fileNameAndType.lastIndexOf('.');
		if (dotPos > 1) {
			fileName = fileNameAndType.substring(0, dotPos);
			fileType = fileNameAndType.substring(dotPos).toLowerCase();
		}
		String normalised = switch (patternNum) {
		case 1 -> fileName.replace("_", "_00");														      // 1 yyyymmdd_nnnn
		case 2 -> fileName.substring(0, fileName.length() - 3).replace("_", "_00");                       // 2 yyyymmdd_nnnn_nn
		case 3 -> fileName.substring(0, fileName.length() - 4).replace("_", "_00");                       // 3 yyyymmdd_nnnn_nnn
		case 4 -> fileName.replace("-", "");                                                              // 4 yyyy-mm-dd_nnnnnn
		case 5 -> fileName.replace("image_", "").replace("_", "_00").substring(0, fileName.length() - 3); // 5 image_yyyymmdd_nnnnnn_nn
		case 6 -> fileName.replace("IMG-", "").replace("-", "_").replace("WA", "00");                     // 6 IMG-yyyymmdd-WAnnnn
		case 7 -> fileName.replace("IMG_", "").replace("-", "_").replace("WA", "00");                     // 7 IMG_yyyymmdd-WAnnnn
		case 8 -> fileName.replace("VID-", "").replace("-", "_").replace("WA", "00");                     // 8 VID-yyyymmdd-WAnnnn
		default -> "";
		};
		String unique = createUnique(normalised);
		return compose(targetDir, unique, fileType);
	}
		
	private String compose(String prefix, String name, String type) {
		return prefix + name + type;
	}
	
	private String createUnique(String orig) {
		if (names.contains(orig)) {
			if (Character.isAlphabetic(orig.charAt(orig.length()-1))) {
				String newName = orig.substring(0, orig.length()-2) +
						(char)orig.charAt(orig.length()-1);
				return createUnique(newName);
			} else {
				return createUnique(orig + "a");
			}
		} else {
			names.add(orig);
			return orig;
		}
	}

	private void createCopyStmt(Path origFile, String newFile) throws IOException {
		String line = String.format("copy \"%s\" \"%s\"", origFile.toString(), newFile);
//		System.out.println(line);
		writer.append(line);
		writer.newLine();
	}

	private void outAlternativeCmd() throws IOException {
		int fileIndex2 = 1;
		String cmdFile = targetDir + "cp2.cmd";
		if (Files.exists(Paths.get(cmdFile))) {
			Files.delete(Paths.get(cmdFile));
		}
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(cmdFile, true))) {
			this.writer = writer;
			for (String date : new TreeSet<String>(candidates.keySet())) {
				List<Path> files = candidates.get(date);
				Collections.sort(files);
				for (Path aFile : files) {
					createCopyStmt(aFile, String.format("%sX%03d.%s",targetDir, fileIndex2++, getFileType(aFile)));
				}
			}
		}
	}
	
	private String getFileType(Path aFile) {
		String fileName = aFile.getFileName().toString();
		int lastDotIndex = fileName.lastIndexOf('.');
		return fileName.substring(lastDotIndex + 1);
	}
}
