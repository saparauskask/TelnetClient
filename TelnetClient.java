import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;

public class TelnetClient {

	public static void main(String[] args) {
		System.out.println("Welcome to telnet\n Use command 'open' to connect to a telnet server\n Use command '?' for help");
        Scanner in = new Scanner(System.in);
        String s;
		while(true) {	
            System.out.print("telnet: ");
			if (in.hasNextLine()) {
                s = in.nextLine();
            } else {
                continue;
            }
			
			String[] tokens = s.split(" ");
			
			if (tokens[0].equals("open")) {
				mainMenu(tokens);
				continue;
			} else if(tokens[0].equals("cache")) {
				cachingMenu(tokens);
				continue;
			} else if(tokens[0].equals("clear")) {
                clearMenu(tokens);
                continue;
            } else if(tokens[0].equals("?")) {
                helpMenu();
                continue;
            } { // Default case
				System.out.println("Incorrect command");
			}
		}
	}
	
	public static void open (String host, int port) {
		try {
            InetSocketAddress address = new InetSocketAddress(host, port);
            // Connect to the Telnet server
            Socket socket = new Socket();
            socket.connect(address, 5000);
            System.out.println("Connected successfully to: " + host);
            
            String newPort;
            if (port == 23) {
                newPort = "-";
            } else {
                newPort = Integer.toString(port);
            }

            //LOGIC FOR APPENDING A NEW ENTRY AUTOMATICALLY
            int dotIndex = host.indexOf('.');
            if (dotIndex != -1 && host.indexOf('.', dotIndex + 1) == -1) {
                String[] domainParts = socket.getInetAddress().toString().split("/");
                addNewEntry(host, domainParts[1], newPort);
            } else {
                //addNewEntry(null, socket.getInetAddress().toString(), newPort);
                addNewEntry(null, host, newPort);
            }

            // Open input and output streams for reading and writing data
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            // Create a thread to read data from the server
              Thread readerThread = new Thread(() -> {
                  try {
                    //    String line;
                    //    while ((line = reader.readLine()) != null) {
                    //         System.out.println(line);
                    //    }
                    String string = reader.readLine();
                    int character;
                    while ((character = reader.read()) != -1) {
                        char c = (char) character;
                        System.out.print(c); // Print each character
                    }
                  } catch (IOException e) {
                    System.out.println("The connection to " + host + " was closed");
                    System.exit(0);
                  }
              });
            readerThread.start();

            // Read input from the user and send it to the server
            BufferedReader userInputReader = new BufferedReader(new InputStreamReader(System.in));
            String userInput;
            while ((userInput = userInputReader.readLine()) != null) {
            	String normalizedInput = Normalizer.normalize(userInput, Normalizer.Form.NFC);
            	// Ensure line endings are CRLF
                writer.println(normalizedInput + "\r\n");
				if (normalizedInput.equalsIgnoreCase("close") || normalizedInput.equalsIgnoreCase("exit")) {
                    break;
            	} else if (normalizedInput.equals("back")) { // Check for Ctrl-C input
                    writer.print((char) 0x03); // Send Ctrl-C to the server
                    writer.flush();
                }

                if (!readerThread.isAlive()) {
                    break;
                }

                }

            // Close the socket and streams when done
            reader.close();
            writer.close();
            userInputReader.close();
            readerThread.interrupt();
            if(socket != null) {
                socket.close();
            }       
        
        } catch (SocketTimeoutException e) {
            System.out.println("Unable to connect");
            System.exit(0);
        } catch (IOException e) {
            //e.printStackTrace();
        } finally{
            System.out.println("The connection to " + host + " was closed");
            System.exit(0);
        }
	}

    public static boolean containsUnreadableCharacters(String text) {
        // Iterate over each character in the string
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            // Check if the character is not a printable ASCII character or a common control character
            if ((ch < 32) && !Character.isWhitespace(ch)) {
                // If the character is not a whitespace and not within the printable ASCII range, it's potentially unreadable
                return true;
            }
        }
        // If no unreadable characters are found, return false
        return false;
    }

    // PARSING METHODS
    
    public static void mainMenu(String[] command) {
        if(command.length == 1) {
            System.out.println("Incomplete or incorrect 'open' command");
            return;
        }
    	int port = 0;
		String host = command[1];
        if (command.length == 3 && Integer.parseInt(command[2]) > 0 && Integer.parseInt(command[2]) < 65537) {
            port = Integer.parseInt(command[2]);
        } else {
            port = 23;
        }
        open(host, port);
        System.out.print("telnet: ");
    	
    }

    public static void cachingMenu(String[] command) {
        
        if (command.length == 2 && command[1].equals("list")) {
        	printAllEntries();
        	//open/create file cache file
        	//display its contents
        	//if its empty display a message that the file is empty
        } else if ((command.length == 3 || command.length == 4 || command.length == 5) && command[1].equals("insert")) {
            if (command.length == 5) {
                addNewEntry(command[2], command[3], command[4]);
            } else if (command.length == 4 && isDomainName(command[2]) && isPort(command[3])) {
                addNewEntry(command[2], null, command[3]);
            } else if (command.length == 4 && isIpAddress(command[2]) && isPort(command[3])) {
                addNewEntry(null, command[2], command[3]);
            } else if (command.length == 3 && isDomainName(command[2])) {
                addNewEntry(command[2], null, null);
            } else if (command.length == 3 && isIpAddress(command[2])) {
                addNewEntry(null, command[2], null);
            }
            
        } else if (command.length == 3 && command[1].equals("open") && Integer.parseInt(command[2]) > 0) {
            openSelectedConnection(Integer.parseInt(command[2]));
        } else {
        	System.out.println("Incomplete or incorrect 'cache' command");
        }
    }

    public static void clearMenu(String[] command) {
        if (command.length == 2 && command[1].equals("cache")) {
            removeAllEntries();
        } else if (command.length == 3 && command[1].equals("cache") && Integer.parseInt(command[2]) > 0) {
            if (removeSelectedEntry(Integer.parseInt(command[2]))) {
                //System.out.println("Entry was removed successfully");
            } else {
                System.out.println("Unable to remove specified entry");
            }
        } else {
            System.out.println("Incomplete or incorrect 'clear' command");
        }
    }

    public static void helpMenu() {
        System.out.println("\nTelnet Client Help Menu\n");
        System.out.println("OPEN Command:");
        System.out.println("  open <ipv4_address|URL_address> [port_number]");
        System.out.println("    - Establishes a connection to the specified Telnet server.");
        System.out.println("    - The port number is optional. If not provided, the default port (23) is used.");
        System.out.println("\nCACHE Commands:");
        System.out.println("  cache list");
        System.out.println("    - Displays a list of saved connections.");
        System.out.println("    - Each entry includes the domain name (if available), IP address, port, and time of entry.");
        System.out.println("  cache open <number_of_entry>");
        System.out.println("    - Opens a connection to a Telnet server from a specified entry in the cache list.");
        System.out.println("  cache insert <ipv4_address|URL_address> [port_number]");
        System.out.println("    - Adds an entry to the list. You can specify either a domain name or IP address, with an optional port number.");
        System.out.println("\nCLEAR Command:");
        System.out.println("  clear cache [number_of_entry]");
        System.out.println("    - Clears all cached entries if no entry number is specified.");
        System.out.println("    - If a number of entry is provided, it removes that specific entry.");
        System.out.println("\nAdditional Commands:");
        System.out.println("  ? - Displays this help menu.");
        System.out.println("  close or exit - Closes the current connection to the Telnet server.");
        System.out.println("  back - Sends Ctrl-C to the server to attempt an interruption of a command.");
    }

    // HELPER METHODS

    public static String getCacheFilePath() {
    	// Define the folder and file names
        String folderName = "Data";
        String fileName = "cache.csv";
        
        // Create a File object for the folder
        File folder = new File(folderName);
        
        // Check if the folder exists, create it if it doesn't
        if (!folder.exists()) {
            folder.mkdir();
            //System.out.println("Folder '" + folderName + "' created.");
        } else {
        	//System.out.println("Folder '" + folderName + "' already exists");
        }
        
        // Create a File object for the file within the folder
        File file = new File(folder, fileName);
        
        // Check if the file exists, create it if it doesn't
        try {
            if (!file.exists()) {
                file.createNewFile();
                System.out.println("File '" + fileName + "' created in folder '" + folderName + "'.");
            } else {
                //System.out.println("File '" + fileName + "' already exists in folder '" + folderName + "'.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        	return null;
        }
    	
        return file.getAbsolutePath();
    }
    
    public static boolean addNewEntry(String domainName, String ipAddress, String port) {
        //System.out.println("addNewEntry method");
        if (ipAddress == null) {
            ipAddress = "-";
        }

        if (port == null) {
            port = "-";
        }

        try {
            int dotCount = 0;
            String path = getCacheFilePath();
    	    String time = LocalDateTime.now().toString();
            if (domainName == null) {
                domainName = "-";
            } else {
                for (int i = 0; i < domainName.length(); ++i) {
                    if (domainName.charAt(i) == '.') {
                        dotCount++;
                    }
                }
                if (dotCount != 1) {
                    System.out.println("error: Incorrect data format");
                    return false;
                }
            }
            // dotCount = 0;
            // for (int i = 0; i < ipAddress.length(); ++i) {
            //     if (ipAddress.charAt(i) == '.') {
            //         dotCount++;
            //     }
            // }
            // if (dotCount != 3) {
            //     System.out.println("error: Incorrect data format");
            //     return false;
            // }

            // if (port != "-" && (Integer.parseInt(port) < 1 || Integer.parseInt(port) > 65535)) {
            //     System.out.println("Incorrect port number. New entry could not be added");
            //     return false;
            // }
    	
            DataEntry entry = new DataEntry(domainName, ipAddress, port, time);
            String csvString = entry.toCsvString();
            DataEntry.appendToCsvFile(path, csvString);
            //System.out.println("Data was appended to csv file successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("error: new entry could not be appended");
            return false;
        }
    	
    	
    	return true;
    }
    
    public static void printAllEntries() {
    	String path = getCacheFilePath();
    	List<DataEntry> entries = DataEntry.getAllEntriesFromFile(path);

        if (entries.isEmpty()) {
            System.out.println("The cache is empty");
        } else {
            System.out.println("Press ESC to cancel");
            System.out.printf("%-7s%-30s%-20s%-7s%-25s\n", "Index", "Domain Name", "IP Address", "Port", "Time");
            System.out.printf("-----------------------------------------------------------------------------------\n");
            int index = 1;
            int displayedEntries = 0;
            for (int i = (entries.size() - 1); i > -1; i--) {
                try {
                    System.out.printf("%-7s%-30s%-20s%-7s%-25s\n", index++, entries.get(i).getDomainName(), entries.get(i).getIpAddress(), entries.get(i).getPort(), entries.get(i).getTime());
                }
                catch(Exception e) {}
                
                ++displayedEntries;
                if (displayedEntries % 5 == 0) {
                    System.out.println("[" + (displayedEntries * 100 / entries.size()) + "% shown]; press SPACE then ENTER to continue");
                    try {
                        char choice = System.console().readLine().charAt(0);
                        if (choice == 0x1B) // Escape key
                            break;
                    }
                    catch(Exception e) {}   
                }
            }
            System.out.println("All entries shown");
        }
    }
    
    public static void printSelectedEntries(int num) {
    	System.out.println("printSelectedEntries method");
    }

    public static void openSelectedConnection(int request) {
        String path = getCacheFilePath();
    	List<DataEntry> entries = DataEntry.getAllEntriesFromFile(path);
        if (entries.isEmpty()) {
            System.out.println("The cache is empty");
        } else {
            int index = 1;

            for (int i = (entries.size() -1); i > -1; i--) {
                if (index == request) {

                     if (entries.get(i).getPort().equals("-")) {
                         if (entries.get(i).getIpAddress().equals("-")) {
                             open(entries.get(i).getDomainName(), 23);
                         } else {
                             open(entries.get(i).getIpAddress(), 23);
                         }                      
                     } else {
                         if (entries.get(i).getIpAddress().equals("-")) {
                             open(entries.get(i).getDomainName(), Integer.parseInt(entries.get(i).getPort()));  
                         } else {
                             open(entries.get(i).getIpAddress(), Integer.parseInt(entries.get(i).getPort()));
                         }                       
                     }
                    
                    break;
                }
                ++index;
            }
            System.out.println("Entry was not found");
        }
    }

    public static boolean isDomainName(String str) {
        if (str == null) {
            return false;
        }

        int dotCount = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '.') {
                dotCount++;
            }
        }

        if (dotCount == 1) {
            return true;
        }

        return false;
    }

    public static boolean isIpAddress(String str) {
        if (str == null) {
            return false;
        }

        int dotCount = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '.') {
                dotCount++;
            }
        }

        if (dotCount == 3) {
            return true;
        }

        return false;
    }

    public static boolean isPort(String str) {
        if (str == null) {
            return false;
        }

        if (Integer.parseInt(str) > 1 && Integer.parseInt(str) < 65536) {
            return true;
        }

        return false;
    }

    public static boolean removeSelectedEntry(int request) {
        String path = getCacheFilePath();
    	List<DataEntry> entries = DataEntry.getAllEntriesFromFile(path);
        if (entries.isEmpty()) {
            System.out.println("The cache is empty");
        } else {
            int index = 1;

            for (int i = (entries.size() -1); i > -1; i--) {
                if (index == request) {
                    DataEntry.removeEntryByIndex(entries, i, path);
                    return true;
                }
                ++index;
            }
            System.out.println("Entry was not found");
        }
        return false;
    }

    public static void removeAllEntries() {
        String path = getCacheFilePath();
        DataEntry.clearCsvFile(path);
        System.out.println("Cache was cleared");
    }
}