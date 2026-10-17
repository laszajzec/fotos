package zec.smartphone;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public class RearrangeFilesGUI extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// Farben
	private static final Color BACKGROUND = new Color(245, 247, 250);
	private static final Color CARD = Color.WHITE;
	private static final Color PRIMARY = new Color(153, 255, 153);
	private static final Color TEXT = new Color(45, 50, 55);
	private static final Color SECONDARY_TEXT = new Color(110, 115, 120);
	private static final Color BORDER = new Color(220, 224, 230);
	private static final Color SUCCESS = new Color(35, 145, 85);
	private static final Color ERROR = new Color(190, 55, 55);

	private static final String[] DEPLTH_CHOICE = { "ALL", "1"};
	private static final String[] SOURCE_DIRS = { "d:/tmpfotos/DCIM", "c:/Users/zechm/Pictures", "/:S24 Ultra von Laszlo/Interner Speicher/DCIM", "/S24 Ultra von Laszlo/Interner Speicher/Pictures"};
	private static final String[] TARGET_DIRS = { "d:/tmpfotosOK" };

	private final JComboBox<String> depthCombo = new JComboBox<>(DEPLTH_CHOICE);
	private final JComboBox<String> sourceCombo = new JComboBox<>(SOURCE_DIRS);
	private final JComboBox<String> targetCombo = new JComboBox<>(TARGET_DIRS);

	private final JRadioButton copyRadio = new JRadioButton("Copy", true);
	private final JRadioButton moveRadio = new JRadioButton("Move");
	
	private final JLabel statusCopied = new JLabel("Copied");
	private final JLabel statusCopiedValue = new JLabel("0");
	private final JLabel statusMoved = new JLabel("Moved");
	private final JLabel statusMovedValue = new JLabel("0");
	private final JLabel statusEquals = new JLabel("Equals");
	private final JLabel statusEqualsValue = new JLabel("0");
	private final JLabel statusDeleted = new JLabel("Deleted");
	private final JLabel statusDeletedValue = new JLabel("0");
	private final JLabel statusCreated = new JLabel("Created");
	private final JLabel statusCreatedValue = new JLabel("0");

	private final JLabel statusLabel = new JLabel("Ready");
	private final JLabel countLabel = new JLabel("Files processed: 0");
	private final JProgressBar progressBar = new JProgressBar();

	private final JButton startButton = new JButton("Start");
	private final JButton sourceButton = new JButton("Browse...");
	private final JButton targetButton = new JButton("Browse...");
	
	private final RearrangeFilesGUI guiObj;

	public RearrangeFilesGUI() {
		this.guiObj = this;
		setTitle("Rearrange files");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(700, 670));
		setSize(760, 700);
		setLocationRelativeTo(null);
		createGui();
	}

	@SuppressWarnings("unused")
	private void createGui() {

		JPanel root = new JPanel(new BorderLayout());
		root.setBackground(BACKGROUND);

		// ---------------------------------------------------------
		// HEADER
		// ---------------------------------------------------------

		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(PRIMARY);
		header.setBorder(new EmptyBorder(22, 30, 22, 30));

		JLabel title = new JLabel("Rearrange files");
		title.setForeground(Color.WHITE);
		title.setFont(new Font("SansSerif", Font.BOLD, 25));

		JLabel subtitle = new JLabel("Copy or move files between folders");
		subtitle.setForeground(new Color(220, 230, 245));
		subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));

		JPanel titlePanel = new JPanel();
		titlePanel.setOpaque(false);
		titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

		titlePanel.add(title);
		titlePanel.add(Box.createVerticalStrut(4));
		titlePanel.add(subtitle);

		header.add(titlePanel, BorderLayout.WEST);

		root.add(header, BorderLayout.NORTH);

		// ---------------------------------------------------------
		// CONTENT
		// ---------------------------------------------------------

		JPanel content = new JPanel();
		content.setBackground(BACKGROUND);
		content.setBorder(new EmptyBorder(25, 30, 20, 30));
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

		// Operation card
		JPanel operationCard = createCard();

		JLabel operationLabel = new JLabel("Operation");
		styleSectionLabel(operationLabel);

		ButtonGroup group = new ButtonGroup();
		group.add(copyRadio);
		group.add(moveRadio);

		styleRadioButton(copyRadio);
		styleRadioButton(moveRadio);

		JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		radioPanel.setOpaque(false);
		radioPanel.add(copyRadio);
		radioPanel.add(moveRadio);

		operationCard.add(operationLabel);
		operationCard.add(Box.createVerticalStrut(10));
		operationCard.add(radioPanel);
		
		JPanel depthRow = createFolderRow("Depth", depthCombo, null);
		operationCard.add(depthRow);

		content.add(operationCard);
		content.add(Box.createVerticalStrut(15));

		// Source / Target card
		JPanel folderCard = createCard();

		JLabel foldersLabel = new JLabel("Folders");
		styleSectionLabel(foldersLabel);

		folderCard.add(foldersLabel);
		folderCard.add(Box.createVerticalStrut(12));

		JPanel sourceRow = createFolderRow("Source", sourceCombo, sourceButton);
		JPanel targetRow = createFolderRow("Target", targetCombo, targetButton);
		folderCard.add(sourceRow);
		folderCard.add(Box.createVerticalStrut(12));
		folderCard.add(targetRow);
		content.add(folderCard);
		content.add(Box.createVerticalStrut(15));

		// Status card
		JPanel statusCard = createCard();

		JLabel statusTitle = new JLabel("Status");
		styleSectionLabel(statusTitle);

		JPanel statusTop = new JPanel(new BorderLayout());
		statusTop.setOpaque(false);

		statusLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
		statusLabel.setForeground(SECONDARY_TEXT);

		countLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
		countLabel.setForeground(SECONDARY_TEXT);

		statusTop.add(statusLabel, BorderLayout.WEST);
		statusTop.add(countLabel, BorderLayout.EAST);

		progressBar.setMinimum(0);
		progressBar.setMaximum(100);
		progressBar.setValue(0);
		progressBar.setStringPainted(true);
		progressBar.setString("");
		progressBar.setPreferredSize(new Dimension(100, 10));
		
		JPanel statisticRow = createFolderRow("Depth", depthCombo, null);

		statusCard.add(statusTitle);
		statusCard.add(Box.createVerticalStrut(10));
		statusCard.add(createLabelRow(new JLabel[]{statusCopied, statusCopiedValue, statusMoved, statusMovedValue, statusEquals, statusEqualsValue,
				statusDeleted, statusDeletedValue, statusCreated, statusCreatedValue}));
		statusCard.add(statusTop);
		statusCard.add(Box.createVerticalStrut(10));
		statusCard.add(progressBar);

		content.add(statusCard);

		root.add(content, BorderLayout.CENTER);

		// ---------------------------------------------------------
		// FOOTER
		// ---------------------------------------------------------

		JPanel footer = new JPanel(new BorderLayout());
		footer.setBackground(Color.WHITE);
		footer.setBorder(new EmptyBorder(15, 30, 15, 30));

		JLabel info = new JLabel("Select source and target folders");
		info.setForeground(SECONDARY_TEXT);
		info.setFont(new Font("SansSerif", Font.PLAIN, 12));

		styleButton(startButton, PRIMARY);

		startButton.setPreferredSize(new Dimension(120, 38));

		footer.add(info, BorderLayout.WEST);
		footer.add(startButton, BorderLayout.EAST);

		root.add(footer, BorderLayout.SOUTH);

		setContentPane(root);

		// ---------------------------------------------------------
		// ACTIONS
		// ---------------------------------------------------------

		sourceButton.addActionListener(e -> selectDirectory(sourceCombo));
		targetButton.addActionListener(e -> selectDirectory(targetCombo));
		startButton.addActionListener(e -> rearrangeFiles());
	}

	// =============================================================
	// GUI HELPERS
	// =============================================================

	private JPanel createCard() {
		JPanel panel = new JPanel();
		panel.setBackground(CARD);
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), new EmptyBorder(16, 18, 16, 18)));
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		return panel;
	}

	private JPanel createLabelRow(JLabel[] labels) {
		JPanel row = new JPanel(new GridLayout(1, labels.length, 10, 10));
		row.setOpaque(false);
		for (JLabel label : labels ) {
			label.setPreferredSize(new Dimension(70, 30));
			label.setFont(new Font("SansSerif", Font.BOLD, 13));
			label.setForeground(TEXT);
			row.add(label);
		}
		return row;
	}

	private JPanel createFolderRow(String labelText, JComboBox<String> comboBox, JButton button) {
		JPanel row = new JPanel(new BorderLayout(10, 0));
		row.setOpaque(false);

		JLabel label = new JLabel(labelText);
		label.setPreferredSize(new Dimension(70, 30));
		label.setFont(new Font("SansSerif", Font.BOLD, 13));
		label.setForeground(TEXT);

		styleComboBox(comboBox);
		row.add(label, BorderLayout.WEST);
		row.add(comboBox, BorderLayout.CENTER);
		if (button != null) {
			styleButton(button, new Color(235, 238, 243));
			button.setForeground(TEXT);
			row.add(button, BorderLayout.EAST);
		}
		return row;
	}

	private void styleSectionLabel(JLabel label) {
		label.setFont(new Font("SansSerif", Font.BOLD, 14));
		label.setForeground(TEXT);
	}

	private void styleRadioButton(JRadioButton radio) {
		radio.setFont(new Font("SansSerif", Font.PLAIN, 13));
		radio.setForeground(TEXT);
		radio.setOpaque(false);
		radio.setFocusPainted(false);
	}

	private void styleComboBox(JComboBox<String> comboBox) {
		comboBox.setFont(new Font("SansSerif", Font.PLAIN, 13));
		comboBox.setBackground(Color.WHITE);
		comboBox.setPreferredSize(new Dimension(100, 34));
	}

	private void styleButton(JButton button, Color background) {

		button.setBackground(background);
		button.setFont(new Font("SansSerif", Font.BOLD, 12));
		button.setFocusPainted(false);
		button.setBorderPainted(false);
		button.setOpaque(true);
		button.setCursor(new Cursor(Cursor.HAND_CURSOR));

		button.setBorder(new EmptyBorder(8, 16, 8, 16));
	}

	// =============================================================
	// DIRECTORY SELECTION
	// =============================================================

	private void selectDirectory(JComboBox<String> comboBox) {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle("Select folder");
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			String path = chooser.getSelectedFile().getAbsolutePath();
			for (int i = 0; i < comboBox.getItemCount(); i++) {
				if (comboBox.getItemAt(i).equals(path)) {
					comboBox.setSelectedItem(path);
					return;
				}
			}
			comboBox.addItem(path);
			comboBox.setSelectedItem(path);
			setStatus("Folder selected", SECONDARY_TEXT);
		}
	}

	// =============================================================
	// FILE PROCESSING
	// =============================================================

	private void rearrangeFiles() {
		String source = (String) sourceCombo.getSelectedItem();
		String target = (String) targetCombo.getSelectedItem();
		if (source == null || target == null || source.isBlank() || target.isBlank()) {
			setStatus("Please select Source and Target.", ERROR);
			JOptionPane.showMessageDialog(this, "Please select Source and Target.", "Missing folders", JOptionPane.WARNING_MESSAGE);
			return;
		}
		Path sourcePath = Paths.get(source);
		Path targetPath = Paths.get(target);
		if (!Files.isDirectory(sourcePath) || !Files.isDirectory(targetPath)) {
			setStatus("Invalid source or target folder.", ERROR);
			return;
		}

		// Gleiche Ordner verhindern
		if (sourcePath.toAbsolutePath().normalize().equals(targetPath.toAbsolutePath().normalize())) {
			setStatus("Source and Target are identical.", ERROR);
			JOptionPane.showMessageDialog(this, "Source and Target cannot be the same folder.", "Invalid selection", JOptionPane.WARNING_MESSAGE);
			return;
		}
		String selectedDepth = (String) depthCombo.getSelectedItem();
		int searchDepth = "1".equals(selectedDepth) ? 1 : Integer.MAX_VALUE;
		startButton.setEnabled(false);
		startButton.setBackground(new Color(20, 20, 20));
		setStatus("Processing files...", PRIMARY);
		countLabel.setText("Files processed: 0");
		progressBar.setValue(0);
		progressBar.setString("");

		// Verarbeitung im Hintergrund
		SwingWorker<Integer, Integer> worker = new SwingWorker<>() {

			@Override
			protected Integer doInBackground() throws Exception {
//				try (DirectoryStream<Path> files = Files.newDirectoryStream(sourcePath)) {
//					for (Path file : files) {
//						if (!Files.isRegularFile(file)) {
//							continue;
//						}
//						Path destination = targetPath.resolve(file.getFileName());
//						if (copyRadio.isSelected()) {
//							Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING);
//						} else {
//							Files.move(file, destination, StandardCopyOption.REPLACE_EXISTING);
//						}
//						count++;
//						publish(count);
//					}
//				}
				boolean moveFiles = moveRadio.isSelected();
				Path log = Paths.get("C:/temp/FotoLog" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".log");
				RearrangeFilesWorker worker = new RearrangeFilesWorker(sourcePath, targetPath, searchDepth, moveFiles, log, guiObj);
				int count = worker.run();
				return count;
			}

			@Override
			protected void process(java.util.List<Integer> values) {
				int current = values.get(values.size() - 1);
				countLabel.setText("Files processed: " + current);
				// Animierter Status
				progressBar.setIndeterminate(true);
			}

			@Override
			protected void done() {
				progressBar.setIndeterminate(false);
				try {
					int count = get();
					progressBar.setValue(100);
					progressBar.setString("Completed");
					countLabel.setText("Files processed: " + count);
					setStatus("Operation completed successfully.", SUCCESS);
				} catch (Exception ex) {
					progressBar.setValue(0);
					progressBar.setString("");
					setStatus("Error: " + ex.getCause().getMessage(), ERROR);
					JOptionPane.showMessageDialog(RearrangeFilesGUI.this, "Error:\n" + ex.getCause().getMessage(), "Operation failed", JOptionPane.ERROR_MESSAGE);
				}
//				startButton.setEnabled(true);
			}
		};
		worker.execute();
	}

	// =============================================================
	// STATUS
	// =============================================================

	private void setStatus(String text, Color color) {
		statusLabel.setText(text);
		statusLabel.setForeground(color);
	}

	public void setStatus(String text) {
		setStatus(text, SUCCESS);
	}
	
	public void setProgress(int count, int procentValue) {
		progressBar.setValue(procentValue);
		countLabel.setText("Files processed: " + count);
	}
	
	public void setCopied(int value) {
		statusCopiedValue.setText(String.format("%5d", value));
	}

	public void setMoved(int value) {
		statusMovedValue.setText(String.format("%5d", value));
	}

	public void setEquals(int value) {
		statusEqualsValue.setText(String.format("%5d", value));
	}

	public void setDeleted(int value) {
		statusDeletedValue.setText(String.format("%5d", value));
	}

	public void setCreated(int value) {
		statusCreatedValue.setText(String.format("%5d", value));
	}
	// =============================================================
	// MAIN
	// =============================================================

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ignored) { }
		SwingUtilities.invokeLater(() -> {
			RearrangeFilesGUI window = new RearrangeFilesGUI();
			window.setVisible(true);
		});
	}
}