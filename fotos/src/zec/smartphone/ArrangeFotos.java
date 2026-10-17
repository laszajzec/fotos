package zec.smartphone;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.mp4.Mp4Directory;

import picocli.CommandLine;
import picocli.CommandLine.Option;

//TODO Dictionary panel
//TODO prohibit _ number duplicates 
public class ArrangeFotos implements Callable<Integer> {
	
	private static final String DEFAULT_DATE = "19900101";
    private static final DateFormat df = new SimpleDateFormat("yyyyMMdd");

    @Option(names = {"-s", "--source"}, defaultValue = "d:/tmpfotos/DCIM", description = "Source path")
	public String sourceRoot;
    @Option(names = {"-t", "--target"}, defaultValue = "d:/tmpfotosOK/DCIM", description = "Target path")
    public String targetRoot;
    @Option(names = {"-d", "--depth"}, defaultValue = "2000", description = "Depth of search")
    public int searchDepth;
    @Option(names = {"-m", "--moveFiles"}, defaultValue = "false", description = "Delete original file")
    boolean moveFiles = false;
    
    private int numOfCopiedFiles = 0;
    private int numOfMovedFiles = 0;
    private int numOfEqualFiles = 0;
    private int numOfDeletedFiles = 0;
    private int numOfCreatedVerionFiles = 0;
    
    
	public static void main(String[] args) throws IOException {
        int exitCode = new CommandLine(new ArrangeFotos()).execute(args);
        System.exit(exitCode);
	}
	
	 public Integer call() throws Exception {
		walkTheFiles(Paths.get(sourceRoot));
		reportStat();
		return 0;
	 }

	private void walkTheFiles(Path startDir) throws IOException {
		Files.walkFileTree(startDir, Collections.emptySet(), searchDepth, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//                System.out.println(file);
                foundSourceFile(file);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                System.err.println(" -- Failed to visit: " + file);
                return FileVisitResult.SKIP_SIBLINGS;
            }
        });
	}
	
	private void foundSourceFile(Path aFile) throws IOException {
		String fileName = aFile.getFileName().toString();
		switch(fileName.substring(fileName.lastIndexOf('.')).toLowerCase()) {
		case ".jpg":
		case ".heic":
		case ".jpeg":
			copyFileToTarget(aFile, getExifDatePicture(aFile));
			break;
		case ".mp4":
			copyFileToTarget(aFile, getExifDateFilm(aFile));
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
            System.out.format(" -- Unknown file type: %s%n ", aFile.toString());
		}
	}
	
	private void copyFileToTarget(Path aFile, String exposureDateExif) throws IOException {
		String exposureDate = getDate(aFile, exposureDateExif);
		String exposureYear = exposureDate.substring(0, 4);
		Path yearDir = Paths.get(targetRoot).resolve(exposureYear);
		String newDirName = findDirIfExists(aFile, exposureDate);
		Path newDir = yearDir.resolve(newDirName);
		Files.createDirectories(newDir);
		Path newPath = newDir.resolve(aFile.getFileName());
		try {
			if (moveFiles) {
				Files.move(aFile, newPath);
				numOfMovedFiles++;
			} else {
				Files.copy(aFile, newPath);
				numOfCopiedFiles++;
			}
		} catch (FileAlreadyExistsException e) {
			long diffPos = Files.mismatch(aFile, newPath);
			if (diffPos < 0) {
				// Ignore if equal
//	            System.out.format(" -- Files are equals: %s %s%n ", aFile.toString(), newDir.toString());
				if (moveFiles) {
					Files.delete(aFile);
					numOfDeletedFiles++;
				} else {
					numOfEqualFiles++;
				}
			} else {
				Path newName = createNewName(newDir, aFile.getFileName());
	            System.out.format(" -- Files are different: %s %s !!!!%n ", aFile.toString(), newName.toString());
				Files.copy(aFile, newName);
				numOfCreatedVerionFiles++;
			}
		} catch (IOException e) {
            System.out.format(" -- IO error: %s %s%n ", aFile.toString(), e);
		}
	}
	
	private Path createNewName(Path newDir, Path fileName) {
		int counter = 0;
		String origFileName = fileName.toString();
		int posLastDot = origFileName.lastIndexOf('.');
		String preparedFileName = origFileName.substring(0, posLastDot) + "_";
		String fileType = origFileName.substring(posLastDot);
		do {
			counter++;
			String tryFileName = preparedFileName + counter + fileType;
			Path newFilePath = newDir.resolve(tryFileName);
			if (!Files.exists(newFilePath)) return newFilePath;
		} while (true);
	}
	
	private String findDirIfExists(Path aFile, String exposureDate) {
		List<String> oldNames = new ArrayList<>();
		for (int i = 0; i < aFile.getNameCount(); i++) {
			oldNames.add(aFile.getName(i).toString());
		}
		if (oldNames.size() > 1 && oldNames.get(oldNames.size() - 2).startsWith(exposureDate)) {
			return oldNames.get(oldNames.size() - 2);
		}
		return exposureDate;
		/*
		Could be searched a similar old directory
		String[] subdirs = yearDir.toFile().list(new FilenameFilter() {
		  @Override
		  public boolean accept(File current, String name) {
		    return new File(current, name).isDirectory();
		  }
		});
		 
		 */
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
			System.err.format("Fehler beim Lesen der Datei %s%n", aFilePath);
			e.printStackTrace();
		}
		return null;
	}
	
	// yyyymmdd
	private static String getExifDateFilm(Path aFilePath) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(aFilePath.toFile());
            Mp4Directory directory = metadata.getFirstDirectoryOfType(Mp4Directory.class);
            
            if (directory != null) {
                // Das Aufnahmedatum / Erstellungsdatum aus den MP4-Metadaten
                Date creationDate = directory.getDate(Mp4Directory.TAG_CREATION_TIME);
                if (creationDate != null) {
                    return df.format(creationDate);
                } else {
                    System.out.println("Kein Aufnahmedatum in den Metadaten gefunden " + aFilePath);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
	}
	
	private String getDate(Path aFilePath, String exifDate) {
		if (exifDate != null) 
			return exifDate;
		String fileNameType = aFilePath.getFileName().toString();
		int pos20 = fileNameType.indexOf("20");
		if (pos20 >= 0) {
			int posDot = fileNameType.lastIndexOf('.');
			String fileName = posDot > 0 ? fileNameType.substring(pos20, posDot) : fileNameType.substring(pos20);
			// accepted formats
			// yyyymmdd
			if (fileName.length() >= 8 && fileName.substring(0, 8).matches("\\d+")) return fileName.substring(0, 8);
		}
		long fileTime;
		try {
			fileTime = Files.getLastModifiedTime(aFilePath).toMillis();
		} catch (IOException e) {
			System.out.println("Last modificaton date can not be read " + aFilePath);
			fileTime = 0;
		}
		if (fileTime > 0) {
			Date lastModdate = new Date(fileTime);
			return df.format(lastModdate);
		}
		return DEFAULT_DATE;
	}
	
	private void reportStat() {
		System.out.format("File copied:  %5d%n", numOfCopiedFiles);
		System.out.format("File moved:   %5d%n", numOfMovedFiles);
		System.out.format("File equals:  %5d%n", numOfEqualFiles);
		System.out.format("File deleted: %5d%n", numOfDeletedFiles);
		System.out.format("File created: %5d%n", numOfCreatedVerionFiles);
	}

}
