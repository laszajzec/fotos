package zec.smartphone;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.mp4.Mp4Directory;

public class RearrangeFilesWorker {
	
	private static final String DEFAULT_DATE = "19900101";
    private static final DateFormat df = new SimpleDateFormat("yyyyMMdd");
    
	private final Path sourceRoot;
	private final Path targetRoot;
	private final int searchDepth;
	private final boolean moveFiles;
	private final File logFile;
	private Writer logWriter;

    private int numOfCopiedFiles = 0;
    private int numOfMovedFiles = 0;
    private int numOfEqualFiles = 0;
    private int numOfDeletedFiles = 0;
    private int numOfCreatedVersionFiles = 0;
    private int numOfAllFiles = 0;
    
    private RearrangeFilesGUI gui;

	public RearrangeFilesWorker(
			Path sourceDir,
			Path targetDir,
			int searchDepth,
			boolean moveFiles,
			Path logfile,
			RearrangeFilesGUI rearrangeGUI) {
		this.sourceRoot = sourceDir;
		this.targetRoot = targetDir;
		this.searchDepth = searchDepth;
		this.moveFiles = moveFiles;
		this.logFile = logfile == null ? null : logfile.toFile();
		this.gui = rearrangeGUI;
	}
	
	public int run() {
		try {
			return runReally();
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	private int runReally() throws IOException {
		if (logFile == null) {
			logWriter = null;
			walkTheFiles(sourceRoot);
			reportStat();
		} else {
			try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile), "utf-8"))) {
				logWriter = writer;
				walkTheFiles(sourceRoot);
				reportStat();
			}
		}
		return 0;
	}
	
	private void walkTheFiles(Path startDir) throws IOException {
		Files.walkFileTree(startDir, Collections.emptySet(), searchDepth, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println(file);
                foundSourceFile(file);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
//                System.err.println(" -- Failed to visit: " + file);
                log(" -- Failed to visit: " + file);
                return FileVisitResult.SKIP_SIBLINGS;
            }
        });
	}

	private void foundSourceFile(Path aFile) throws IOException {
		numOfAllFiles++;
		gui.setProgress(numOfAllFiles, (int)(numOfAllFiles/15000.0));
		FileDescr descr = new FileDescr(aFile);
		switch(descr.fileType.toLowerCase()) {
		case ".jpg":
		case ".heic":
		case ".jpeg":
		case ".png":
			copyFileToTarget(descr, getExifDatePicture(aFile));
			break;
		case ".mp4":
			copyFileToTarget(descr, getExifDateFilm(aFile));
			break;
		case ".ini":
		case ".html":
		case ".htm":
		case ".pptx":
		case ".docx":
		case ".ods":
		case ".pdf":
		case ".zip":
			// ignore
			break;
		default:
			//            System.out.format(" -- Unknown file type: %s%n ", aFile.toString());
			log(String.format(" -- Unknown file type: %s", aFile.toString()));
		}
	}
	
	private void copyFileToTarget(FileDescr origFileDescr, String exposureDateExif) throws IOException {
		FileDescr newFileDescr = createTragetFile(origFileDescr, exposureDateExif);
		Files.createDirectories(newFileDescr.getDir());
		try {
			if (moveFiles) {
				Files.move(origFileDescr.origPath, newFileDescr.origPath);
				numOfMovedFiles++;
				gui.setMoved(numOfMovedFiles);
			} else {
				Files.copy(origFileDescr.origPath, newFileDescr.origPath);
				numOfCopiedFiles++;
				gui.setCopied(numOfCopiedFiles);
			}
			System.out.format("Copied %s -> %s%n", origFileDescr.origPath, newFileDescr.origPath);
		} catch (FileAlreadyExistsException e) {
			if (hasEqual(origFileDescr, newFileDescr)) {
//				System.out.println("Equals " + origFileDescr.origPath);
				numOfEqualFiles++;
				if (moveFiles) {
					// Delete original as equal
					Files.delete(origFileDescr.origPath);
					numOfDeletedFiles++;
					gui.setDeleted(numOfDeletedFiles);
				} else {
					gui.setEquals(numOfEqualFiles);
				}
			} else {
				// Check if underscore equal file exists
				Path newName = createNewName(newFileDescr);
	            log(String.format(" -- Files are different: %s %s !!!!", origFileDescr.toString(), newName.toString()));
				Files.copy(origFileDescr.origPath, newName);
				numOfCreatedVersionFiles++;
				gui.setCreated(numOfCreatedVersionFiles);
			}
			} catch (IOException e) {
            log(String.format(" -- IO error: %s %s", origFileDescr.toString(), e));
		}
	}
	
	private FileDescr createTragetFile(FileDescr aFile, String exposureDateExif) throws IOException {
		// new form: yyyy/yyyymmdd name
		// old form yyyymmdd name orr name dd.mm.yyyy
		String exposureDate = getDate(aFile, exposureDateExif);
		String exposureYear = exposureDate.substring(0, 4);
		Path yearDir = targetRoot.resolve(exposureYear);
		String newDirName = createNewDirName(aFile.origPath, exposureDate);
		Path newDir = yearDir.resolve(newDirName);
		Path newFilePath = newDir.resolve(aFile.origPath.getFileName());
		return new FileDescr(newFilePath);
	}
	
	private final Pattern compactDateBefore = Pattern.compile("^\\d{8}.*$");
	private final Pattern compactDateBehind = Pattern.compile("^.*\\d{8}$");
	private final Pattern dateDMYBefore = Pattern.compile(  "^(0?[1-9]|[12][0-9]|3[01])\\.(0?[1-9]|1[012])\\.((19|20)\\d\\d).*$");
	private final Pattern dateDMYBehind = Pattern.compile("^.* (0?[1-9]|[12][0-9]|3[01])\\.(0?[1-9]|1[012])\\.((19|20)\\d\\d)$");
	private Matcher m;
	private String createNewDirName(Path aFile, String exposureDate) {
		String oldDirName = aFile.getParent().getFileName().toString();
		// yyyymmdd name
		m = compactDateBefore.matcher(oldDirName);
		if (m.matches()) {
			return oldDirName;
		}
		// name yyyymmdd
		m = compactDateBehind.matcher(oldDirName);
		if (m.matches()) {
			return oldDirName.substring(oldDirName.length()-8) + " " + oldDirName.substring(0, oldDirName.length() - 8).trim();
		}
		// name dd.mm.yyyy
		m = dateDMYBehind.matcher(oldDirName);
		if (m.matches()) {
			DateAndLength dateAndLength = formattedDateDir(m.group(1), m.group(2), m.group(3));
			return dateAndLength.date + " " + oldDirName.substring(0, oldDirName.length() - dateAndLength.length - 1).trim();
		}
		// dd.mm.yyyy name
		m = dateDMYBefore.matcher(oldDirName);
		if (m.matches()) {
			DateAndLength dateAndLength = formattedDateDir(m.group(1), m.group(2), m.group(3));
			return dateAndLength.date + " " + oldDirName.substring(dateAndLength.length).replace(".", "");
		}
		if ("Camera".equals(oldDirName) || "Camera".equals(oldDirName)) {
			return exposureDate;
		} else {
			return exposureDate + " " + oldDirName;
		}
	}
	
	private record DateAndLength(String date, int length) {};
	
	private DateAndLength formattedDateDir(String day, String month, String year) {
		return new DateAndLength(year + padLeft0(month) + padLeft0(day), day.length() + month.length() + year.length() + 2);
	}
	
	private String padLeft0(String in) {
		return in.length() >= 2 ? in : "0" + in;
	}
	
	private Path createNewName(FileDescr newFile) {
		int counter = 0;
		String preparedFileName = newFile.fileName + "_";
		do {
			counter++;
			String tryFileName = preparedFileName + counter + newFile.fileType;
			Path newFilePath = newFile.getDir().resolve(tryFileName);
			if (!Files.exists(newFilePath)) return newFilePath;
		} while (true);
	}
	
	private boolean hasEqual(FileDescr origFile, FileDescr newFile) throws IOException {
		if (areEqual(origFile, newFile.origPath)) 
			return true;
		File newDirAsFile = newFile.getDir().toFile();
		if (!newDirAsFile.isDirectory()) {
			log("Wrong dir reference " + newFile.origPath);
			return false;
		}
		for (File fil : newDirAsFile.listFiles(f -> f.getName().startsWith(origFile.fileName))) {
			if (areEqual(origFile, fil.toPath())) return true;
		}
		return false;
	}
	
	private boolean areEqual(FileDescr origFile, Path newFile) throws IOException {
		return Files.mismatch(origFile.origPath, newFile) < 0;
	}
	
	// yyyymmdd
	private String getExifDatePicture(Path aFilePath) {
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(aFilePath.toFile());
			ExifSubIFDDirectory exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
			if (exif != null) {
				String dateTime = exif.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
				if (dateTime != null) {
					return dateTime.substring(0, 10).replace(":", "");
				} else {
//					System.out.println("Kein EXIF-Aufnahmedatum gefunden " + aFilePath);
				}
			} else {
//				System.out.println("Keine EXIF-Daten gefunden " + aFilePath);
			}
		} catch (Exception e) {
			log(String.format("Fehler beim Lesen der Datei %s", aFilePath));
			e.printStackTrace();
		}
		return null;
	}
	
	// yyyymmdd
	private String getExifDateFilm(Path aFilePath) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(aFilePath.toFile());
            Mp4Directory directory = metadata.getFirstDirectoryOfType(Mp4Directory.class);
            
            if (directory != null) {
                // Das Aufnahmedatum / Erstellungsdatum aus den MP4-Metadaten
                Date creationDate = directory.getDate(Mp4Directory.TAG_CREATION_TIME);
                if (creationDate != null) {
                    return df.format(creationDate);
                } else {
                    log("Kein Aufnahmedatum in den Metadaten gefunden " + aFilePath);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
	}
	
	private String getDate(FileDescr aFile, String exifDate) {
		if (exifDate != null) 
			return exifDate;
		int pos20 = aFile.fileName.indexOf("20");
		if (pos20 >= 0) {
			// accepted formats
			// yyyymmdd
			if (aFile.fileName.length() >= 8 && aFile.fileName.substring(0, 8).matches("\\d+")) return aFile.fileName.substring(0, 8);
		}
		long fileTime;
		try {
			fileTime = Files.getLastModifiedTime(aFile.origPath).toMillis();
		} catch (IOException e) {
			log("Last modificaton date can not be read " + aFile.origPath);
			fileTime = 0;
		}
		if (fileTime > 0) {
			Date lastModdate = new Date(fileTime);
			return df.format(lastModdate);
		}
		return DEFAULT_DATE;
	}
	
	private void log(String text) {
		System.out.println(text);
		if (logWriter != null) {
		   try {
			logWriter.write(text);
			logWriter.append(System.lineSeparator());
		   } catch (IOException e) {
			System.err.println("Can not write to log file");
		   }
		}
	}
	
	private void reportStat() throws IOException {
		log(String.format("File copied:  %5d", numOfCopiedFiles));
		log(String.format("File moved:   %5d", numOfMovedFiles));
		log(String.format("File equals:  %5d", numOfEqualFiles));
		log(String.format("File deleted: %5d", numOfDeletedFiles));
		log(String.format("File created: %5d", numOfCreatedVersionFiles));
	}

	private static class FileDescr {
		final Path origPath;
		final String fileName;
		final String fileType; 
		public FileDescr(Path aFile) {
			this.origPath = aFile;
			String f = aFile.getFileName().toString();
			int lastDotPos = f.lastIndexOf('.');
			if (lastDotPos < 0) {
				this.fileName = f;
				this.fileType = ".";
			} else {
				this.fileName = f.substring(0, lastDotPos);
				this.fileType = f.substring(lastDotPos);
			}
		}
		public Path getDir() {
			return origPath.getParent();
		}
		public String toString() {
			return origPath.toString();
		}
	}
}
