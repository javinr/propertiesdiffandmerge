package es.javinr.propertiesdiffandmerge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Hello world!
 *
 */
public class PropertiesDiffAndMergeApp {
	public static void main(String[] args) {
		Options options = new Options();
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine commandLine = parser.parse(options, args);
			if (commandLine.getArgs() == null || commandLine.getArgs().length < 2) {
				System.err.println("No file to diff specified");
				use();
				System.exit(-2);
			}
			String file1Path = commandLine.getArgs()[0];
			String file2Path = commandLine.getArgs()[1];
			File file1 = new File(file1Path);
			File file2 = new File(file2Path);
			boolean error = false;
			StringBuilder sbError = new StringBuilder();
			if (!file1.exists()) {
				error = true;
				sbError.append("File ").append(file1Path).append(" does not exists\n");
			}
			if (!file2.exists()) {
				error = true;
				sbError.append("File ").append(file2Path).append(" does not exists\n");
			}
			if (error) {
				System.err.println(sbError);
				use();
				System.exit(-1);
			}
			System.out.println("Diffing " + file1Path + " and " + file2Path);
			Properties properties1 = new Properties();
			Properties properties2 = new Properties();
			properties1.load(new FileInputStream(file1));
			properties2.load(new FileInputStream(file2));
			Properties propertiesIn1NotIn2 = diffPropertiesNotIn(properties1, properties2);
			Properties propertiesIn2NotIn1 = diffPropertiesNotIn(properties2, properties1);
			Properties propertiesDifferent = diffProperties(properties1, properties2);
			showProperties("Properties in " + file1Path + " not in " + file2Path + ":", propertiesIn1NotIn2);
			showProperties("Properties in " + file2Path + " not in " + file1Path + ":", propertiesIn2NotIn1);
			showPairProperties("Different properties:", propertiesDifferent);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		// TODO Auto-generated method stub
		
	}
}
