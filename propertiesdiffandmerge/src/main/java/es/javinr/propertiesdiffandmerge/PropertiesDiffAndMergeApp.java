package es.javinr.propertiesdiffandmerge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.tuple.Pair;

import es.javinr.propertiesdiffandmerge.PropertiesDiffAndMergeApp.Action;

/**
 * Hello world!
 *
 */
public class PropertiesDiffAndMergeApp {
	public enum Action {
		YES('y','Y'), NO('n','N'), EXCLUDE('e','E'), ORIGINAL('o','O'), MODIFIED('M','m');
		private Set<Character> characters;
		private Action(char... characters) {
			this.characters = new HashSet<Character>();
			for (char c : characters) {
				this.characters.add(c);
			}
		}
		public static Action fromChar(char c) {
			for (Action action : Action.values()) {
				if (action.characters.contains(c)) {
					return action;
				}
			}
			return null;
		}

	}

	public boolean isOutputToFile() {
		return outputToFile;
	}

	public void setOutputToFile(boolean outputToFile) {
		this.outputToFile = outputToFile;
	}

	public File getFile1() {
		return originalFile;
	}

	public void setFile1(File file1) {
		this.originalFile = file1;
	}

	public File getFile2() {
		return modifiedFile;
	}

	public void setFile2(File file2) {
		this.modifiedFile = file2;
	}

	private static final Options options;
	private boolean outputToFile = false;
	private File outputFile;
	private File originalFile;
	private File modifiedFile;
	private boolean interactive = false;
	
	public boolean isInteractive() {
		return interactive;
	}

	public void setInteractive(boolean interactive) {
		this.interactive = interactive;
	}

	static {
		options = createOptions();
	}
	public static void main(String[] args) {
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine commandLine = parser.parse(options, args);
			if (commandLine.getArgs() == null || commandLine.getArgs().length < 2) {
				System.err.println("No file to diff specified");
				use();
				System.exit(-2);
			}
			String originalFilePath = commandLine.getArgs()[0];
			String modifiedFile2Path = commandLine.getArgs()[1];
			File originalFile = new File(originalFilePath);
			File modifiedFile = new File(modifiedFile2Path);
			boolean error = false;
			StringBuilder sbError = new StringBuilder();
			if (!originalFile.exists()) {
				error = true;
				sbError.append("File ").append(originalFilePath).append(" does not exists\n");
			}
			if (!modifiedFile.exists()) {
				error = true;
				sbError.append("File ").append(modifiedFile2Path).append(" does not exists\n");
			}
			if (error) {
				System.err.println(sbError);
				use();
				System.exit(-1);
			}
			
			PropertiesDiffAndMergeApp app = new PropertiesDiffAndMergeApp();
			app.setFile1(originalFile);
			app.setFile2(modifiedFile);
			if (commandLine.hasOption('o')) {
				app.setOutputToFile(true);
				app.setOutputFile(new File(commandLine.getOptionValue('o')));
			}
			app.setInteractive(commandLine.hasOption('i'));
			
			System.out.println("Diffing " + originalFilePath + " and " + modifiedFile2Path);
			app.run();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
	public void run() throws Exception {
		Properties propertiesOriginal = loadFile(originalFile);
		Properties propertiesModified = loadFile(modifiedFile);
		
		Properties propertiesInOriginalNotInModified = diffPropertiesNotIn(propertiesOriginal, propertiesModified);
		Properties propertiesInModifiedNotInOriginal = diffPropertiesNotIn(propertiesModified, propertiesOriginal);
		Properties propertiesDifferent = diffProperties(propertiesOriginal, propertiesModified);
		showProperties("Properties in " + originalFile.getName() + " not in " + modifiedFile.getName() + ":", propertiesInOriginalNotInModified);
		showProperties("Properties in " + modifiedFile.getName() + " not in " + originalFile.getName() + ":", propertiesInModifiedNotInOriginal);
		showPairProperties("Different properties:", propertiesDifferent);
		if (outputToFile) {
			Properties outputProperties = new Properties();
			if (interactive) {
				interactiveOutputProperties(outputProperties, propertiesModified, propertiesInOriginalNotInModified, propertiesInModifiedNotInOriginal, propertiesDifferent);
			} else {
				outputProperties.putAll(propertiesModified);
			}
			try (FileWriter outputFileWriter = new FileWriter(getOutputFile())) {
				outputProperties.store(outputFileWriter, null);
			} catch (IOException e) {
				throw new Exception("Error writing output file " + getOutputFile().getAbsolutePath(), e);
			}
		}
	}

	private void interactiveOutputProperties(Properties outputProperties, Properties modifiedProperties,
			Properties propertiesInOriginalNotInModified, Properties propertiesInModifiedNotInOriginal, Properties propertiesDifferent) {
		for (Object key : modifiedProperties.keySet()) {
			Object value = modifiedProperties.get(key);
			boolean include = true;
			try {
				if (propertiesInModifiedNotInOriginal.containsKey(key)) {
					include = checkIncludeNotIn("original", key, value);
				} else if (propertiesDifferent.containsKey(key)) {
					Action action = checkDifferentAction(key, value, propertiesDifferent.get(key));
					switch (action) {
					case ORIGINAL:
						value = ((Pair<Object,Object>)propertiesDifferent.get(key)).getLeft();
						break;
					case MODIFIED:
						
						break;
					case EXCLUDE:
						include = false;
						break;
					default:
						break;
					}
				}
				if (include) {
					outputProperties.put(key, value);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		for (Object originalKey : propertiesInOriginalNotInModified.keySet()) {
			Object value = propertiesInOriginalNotInModified.get(originalKey);
			boolean include = true;
			try {
				include = checkIncludeNotIn("modified", originalKey, value);
				if (include) {
					outputProperties.put(originalKey, value);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	private Action checkDifferentAction(Object key, Object modifiedValue, Object originalValue) throws IOException {
		System.out.printf("Property %s has different values in original/modified file. Choose one: (E)xclude, (O)riginal value %s, (M)Modified value %s", key, originalValue, modifiedValue);
		final List<Action> validActions = Arrays.asList(Action.EXCLUDE,Action.ORIGINAL, Action.MODIFIED);
		return readAction(validActions);
	}

	private boolean checkIncludeNotIn(String originalOrModified, Object key, Object value) throws IOException {
		System.out.printf("Property %s=%s not in %s file. Include? (y, n):", key, value, originalOrModified);
		final List<Action> validActions = Arrays.asList(Action.YES, Action.NO);
		Action action = readAction(validActions);
		return Action.YES.equals(action);
	}

	private Action readAction(final List<Action> validActions) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		Action action = null;
		do {
			action = Action.fromChar((char) br.read());
		} while (action == null || !validActions.contains(action));
		return action;
	}

	private Properties loadFile(File file) throws Exception {
		Properties properties = new Properties();
		try (FileInputStream fis = new FileInputStream(file)) {
			properties.load(fis);
		} catch (FileNotFoundException e) {
			throw new Exception("File " + originalFile.getAbsolutePath() + " not found", e);
		} catch (IOException e) {
			throw new Exception("Error readin file " + originalFile.getAbsolutePath(), e);
		}
		return properties;
	}
	private static Options createOptions() {
		Options options = new Options();
		options.addOption(Option.builder("o")
				.longOpt("output")
				.hasArg()
				.argName("output file")
				.required(false)
				.desc("Output file in wich write merge results")
				.build());
		options.addOption(Option.builder("i")
				.longOpt("interactive")
				.build());
		return options;
	}

	private static void showPairProperties(String title, Properties propertiesDifferent) {
		System.out.println(title);
		for (Object key : propertiesDifferent.keySet()) {
			Pair<Object, Object> values = (Pair<Object, Object>) propertiesDifferent.get(key);
			System.out.printf("Property %s with values (%s, %s)\n", key, values.getLeft(), values.getRight());
		}
	}

	private static void showProperties(String title, Properties propertiesIn1NotIn2) {
		System.out.println(title);
		for (Object key : propertiesIn1NotIn2.keySet()) {
			System.out.printf("Property %s with value %s\n", key, propertiesIn1NotIn2.get(key));
		}
		
	}

	private static Properties diffProperties(Properties properties1, Properties properties2) {
		Properties propertiesDiff = new Properties();
		for (Object key : properties1.keySet()) {
			if (properties2.containsKey(key)) {
				Object value1 = properties1.get(key);
				Object value2 = properties2.get(key);
				if ((value1 == null && value2 != null)
						|| (value1 != null && value2 == null)
						|| !value1.equals(value2)) {
					Pair<Object, Object> values = Pair.of(value1, value2);
					propertiesDiff.put(key, values);
				}
			}
		}
		return propertiesDiff;
	}

	private static Properties diffPropertiesNotIn(Properties properties1, Properties properties2) {
		Properties propertiesIn1NotIn2 = new Properties();
		for (Object key : properties1.keySet()) {
			if (!properties2.containsKey(key)) {
				propertiesIn1NotIn2.put(key, properties1.get(key));
			}
		}
		return propertiesIn1NotIn2;
	}

	private static void use() {
		HelpFormatter helpFormatter = new HelpFormatter();
		helpFormatter.printHelp("PropertiesDiffAndMergeApp file1 file2", options);
		
	}

	public File getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}
}
