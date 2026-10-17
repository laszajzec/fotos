package zec.smartphone;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class FindAndClearDuplicates {
	
	private static final String START_DIR = "d:/tmpfotosOK";
	private static final boolean deleteReally = true;
	
	private Map<Long, List<Path>> lengthDict;
	private final String logFile = "C:/temp/FotoCmp" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".log";
	private Writer logWriter;
	private int numAllFiles;
	private int numCandidates;
	private int numDeleted;


	public static void main(String[] args) throws IOException {
		new FindAndClearDuplicates().doit(args);
	}

	public void doit(String[] args) throws IOException {
		lengthDict = new HashMap<>();
		numAllFiles = 0;
		numCandidates = 0;
		numDeleted = 0;

		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile), "utf-8"))) {
			logWriter = writer;
			buildLengthDict();
			evaluateLengthDict();
			log("Finished");
			log(String.format("Found %d files.%n", numAllFiles));
			log(String.format("Found %d candidates.%n", numCandidates));
			log(String.format("Deleted %d files.%n", numDeleted));
		}
	}
	
	private void buildLengthDict() throws IOException {
		Files.walkFileTree(Paths.get(START_DIR), Collections.emptySet(), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            	numAllFiles++;
        		lengthDict.computeIfAbsent(file.toFile().length(), _ -> new ArrayList<Path>()).add(file);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                log(" -- Failed to visit: " + file);
                return FileVisitResult.SKIP_SIBLINGS;
            }
        });
	}
	
	private void evaluateLengthDict() throws IOException {
		Map<Integer, Integer> equals = new HashMap<>();
		for (Map.Entry<Long, List<Path>> aLength : lengthDict.entrySet()) {
			int numOfEq = aLength.getValue().size();
			if (numOfEq > 1) {
				numCandidates++;
				equals.compute(numOfEq, (_, v) -> v == null ? 1 : v + 1);
			}
		}
		for (int l : new TreeSet<Integer>(equals.keySet())) {
			log(String.format("%3d %5d", l, equals.get(l)));
		}
		for (Map.Entry<Long, List<Path>> aLength : lengthDict.entrySet()) {
			if (aLength.getValue().size() >= 2) {
				log("For length " + aLength.getKey());
				printEquivalentCandidates(aLength.getKey(), aLength.getValue());
				deleteIdentical(new ArrayList<Path>(aLength.getValue()));
			}
		}
	}
	
	private void printEquivalentCandidates(long length, Collection<Path> cand) {
		for (Path aPath : cand) {
			log(aPath.toString());
		}
	}
	
	private void deleteIdentical(List<Path> candidates) throws IOException {
		Set<Integer> indicesToDelete = new HashSet<>();
		for (int i = 0; i < candidates.size() - 1; i++) {
			if (!indicesToDelete.contains(i)) {
				compareWith(i, candidates, indicesToDelete);
			}
		}
		
		for (int indexToDelete : indicesToDelete) {
//			candidates.remove(indexToDelete);
			deleteFile(candidates.get(indexToDelete));
		}
	}
	
	private void compareWith(int baseIndex, List<Path> candidates, Set<Integer> indicesToDelete) throws IOException {
		for (int j = baseIndex + 1; j < candidates.size(); j++) {
			Path cand1 = candidates.get(baseIndex);
			Path cand2 = candidates.get(j);
			// Are equal?
			if (Files.mismatch(cand1, cand2) < 0) {
				// Which one should be deleted ?
				int cmpPath = compareNames(cand1.getParent().toString(), cand2.getParent().toString()); 
				if (cmpPath == 0) {
					int cmpName = compareNames(cand1.getFileName().toString(), cand2.getFileName().toString());
					indicesToDelete.add(cmpName < 0 ? baseIndex : j);
				} else {
					indicesToDelete.add(cmpPath < 0 ? baseIndex : j);
				}
			}
		}
	}
	
	private final Set<String> specialNames = Set.of(" - Kopie", " Pictures");
	private int compareNames(String name1, String name2) {
		boolean name1special = specialNames.stream().anyMatch(x -> name1.contains(x));
		boolean name2special = specialNames.stream().anyMatch(x -> name2.contains(x));
		if (name1special) {
			return name2special ? name1.compareTo(name2) : -1;
		} else {
			return name2special ? 1 : name1.compareTo(name2);
		}
	}

	private void deleteFile(Path aFile) throws IOException {
		log("Delete " + aFile);
		numDeleted++;
		if (deleteReally) {
			Files.delete(aFile);
		}
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

}
