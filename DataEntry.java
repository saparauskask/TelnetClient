import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class DataEntry {
	private String domainName;
	private String ipAddress;
	private String port;
	private String time;
	
	public DataEntry(String domainName, String ipAddress, String port, String time) {
		this.domainName = domainName;
		this.ipAddress = ipAddress;
		this.port = port;
		this.time = time;
	}

	public String getDomainName() {
		return this.domainName;
	}

	public String getIpAddress() {
		return this.ipAddress;
	}

	public String getTime() {
		return this.time;
	}

	public String getPort() {
		return this.port;
	}
	
	public String toCsvString() {
		return String.join(",", domainName, ipAddress, port, time);
	}
	
	// Append the CSV string to a CSV file
    public static void appendToCsvFile(String csvFilePath, String csvString) {
        try (FileWriter writer = new FileWriter(csvFilePath, true)) {
            writer.append(csvString);
            writer.append("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	public static List<DataEntry> getAllEntriesFromFile(String csvFilePath) {
		List<DataEntry> entryList = new ArrayList<>();

		try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] parts = line.split(",");
				if (parts.length == 4) { // Ensure the line has four parts
					String domainName = parts[0].trim();
					String ipAddress = parts[1].trim();
					String port = parts[2].trim();
					String time = parts[3].trim();
					entryList.add(new DataEntry(domainName, ipAddress, port, time));
				}
			}
		} catch (IOException e) {
			System.out.println("Error occured when trying to get the list from the cache");
			e.printStackTrace();
		}

		return entryList;
	}


	//REMOVING ENTRIES FROM FILE
	public static void removeEntryByIndex(List<DataEntry> entryList, int index, String csvFilePath) {
		if (index >= 0 && index < entryList.size()) {
			entryList.remove(index);
			updateCsvFile(entryList, csvFilePath);
			System.out.println("Entry was deleted successfully");
		} else {
			System.out.println("Invalid index.");
		}
	}
	
	private static void updateCsvFile(List<DataEntry> entryList, String csvFilePath) {
		try (FileWriter writer = new FileWriter(csvFilePath)) {
			for (DataEntry entry : entryList) {
				writer.append(entry.toCsvString());
				writer.append("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void clearCsvFile(String csvFilePath) { // THIS WILL DELETE ALL CONTENTS FROM THE FILE
		try (FileWriter writer = new FileWriter(csvFilePath)) {
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}