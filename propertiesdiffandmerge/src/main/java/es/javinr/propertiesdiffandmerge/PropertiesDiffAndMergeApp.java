package es.javinr.propertiesdiffandmerge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Hello world!
 *
 */
public class PropertiesDiffAndMergeApp {
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
			System.out.println("Diffing " + originalFilePath + " and " + modifiedFile2Path);
			app.run();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
	public void run() throws Exception {
		Properties properties1 = loadFile(originalFile);
		Properties properties2 = loadFile(modifiedFile);
		
		Properties propertiesIn1NotIn2 = diffPropertiesNotIn(properties1, properties2);
		Properties propertiesIn2NotIn1 = diffPropertiesNotIn(properties2, properties1);
		Properties propertiesDifferent = diffProperties(properties1, properties2);
		showProperties("Properties in " + originalFile.getName() + " not in " + modifiedFile.getName() + ":", propertiesIn1NotIn2);
		showProperties("Properties in " + modifiedFile.getName() + " not in " + originalFile.getName() + ":", propertiesIn2NotIn1);
		showPairProperties("Different properties:", propertiesDifferent);
		if (outputToFile) {
			Properties outputProperties = new Properties();
			outputProperties.putAll(properties2);
			try (FileWriter outputFileWriter = new FileWriter(getOutputFile())) {
				outputProperties.store(outputFileWriter, null);
			} catch (IOException e) {
				throw new Exception("Error writing output file " + getOutputFile().getAbsolutePath(), e);
			}
		}
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
