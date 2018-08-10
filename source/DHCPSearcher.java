import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.Runnable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DHCPSearcher
{
	private List<DHCPServer> servers;
	private List<String> log_list;
	private List<String> ignored_servers;
	private int number_of_pooled_threads;
	private String folder_scripts;
	private String folder_log;
	private String folder_output;
	private String folder_data_servers;
	private String folder_data_scopes;
	public String folder_data_leases;
	public String folder_data_reservations;
	public String folder_data_inactive_reservations;
	private boolean check_leases;
	private boolean check_reservations;
	private boolean check_inactive_reservations;

	public enum STATE {
		OK(0),
		ERROR(1);

		private int state;

		STATE(int new_state) {
			state = new_state;
		}

		public int GetState() {
			return state;
		}
	}

	public enum LOG_TYPE {
		INFO(0),
		WARNING(1),
		ERROR(2);

		private int state;

		LOG_TYPE(int new_state) {
			state = new_state;
		}

		public int GetState() {
			return state;
		}
	}

	public static void main(String args[]) {
		DHCPSearcher searcher = new DHCPSearcher();
		searcher.initialiseFolders();
		boolean run = true;
		int menu_option = 0;
		BufferedReader console_in = new BufferedReader(new InputStreamReader(System.in));
		while(run) {
			if(menu_option == 0) {
				String option = "";
				String ignored_servers = "";

				if(!searcher.getIgnoredServers().isEmpty()) {
					for(String server : searcher.getIgnoredServers()) {
						ignored_servers += server;
						if(server.compareTo(searcher.getIgnoredServers().get(searcher.getIgnoredServers().size()-1)) != 0) {
							ignored_servers += ", ";
						}
					}
				}

				System.out.println("DHCP Searcher");
				System.out.println("");
				System.out.println("Run Leases Check: " + searcher.getCheckLeases());
				System.out.println("Run Reservations Check: " + searcher.getCheckReservations());
				System.out.println("Run Inactive Reservations Check: " + searcher.getCheckInactiveReservations());
				System.out.println("Ignored servers: " + ignored_servers);
				System.out.println("");
				System.out.println("1. Load current DHCP dataset");
				System.out.println("2. Rebuild current DHCP dataset");
				System.out.println("3. Print loaded DHCP dataset to file");
				System.out.println("4. Print log to file");
				System.out.println("5. Toggle Leases Check");
				System.out.println("6. Toggle Reservations Check");
				System.out.println("7. Toggle Inactive Reservations Check");
				System.out.println("8. Get DHCP server list");
				System.out.println("Type +<server name> to ignore a server or -<server name> to remove a server from the ignore list");
				System.out.println("0. Exit");
				System.out.println("");
				System.out.println("What would you like to do? ");
				try {
					option = console_in.readLine();
				} catch(Exception e) {
					System.out.println("Unable to read from console");
					return;
				}
				
				if(option.compareTo("1") == 0) {
					try{
						searcher.loadDHCPData();
					} catch(Exception e) {
						System.out.println(e);
					}
				} else if(option.compareTo("2") == 0) {
					try{
						searcher.buildDHCPData();
					} catch(Exception e) {
						System.out.println(e);
					}
				} else if(option.compareTo("3") == 0) {
					try{
						searcher.writeOutputToFile();
					} catch(Exception e) {
						System.out.println(e);
					}
				} else if(option.compareTo("4") == 0) {
					try{
						searcher.writeLogToFile();
					} catch(Exception e) {
						System.out.println(e);
					}
				} else if(option.compareTo("5") == 0) {
					searcher.setCheckLeases(!searcher.getCheckLeases());
				} else if(option.compareTo("6") == 0) {
					searcher.setCheckReservations(!searcher.getCheckReservations());
				} else if(option.compareTo("7") == 0) {
					searcher.setCheckInactiveReservations(!searcher.getCheckInactiveReservations());
				} else if(option.compareTo("8") == 0) {
					try{
						ArrayList<String> servers = searcher.generateDHCPServerList();
						if(servers != null && !servers.isEmpty()) {
							for(String server : servers) {
								System.out.println(server);
							}
						}
					} catch(Exception e) {
						System.out.println(e);
					}
				} else if(option.charAt(0) == '+') {
					searcher.addIgnoredServer(option.substring(1));
				} else if(option.charAt(0) == '-') {
					searcher.removeIgnoredServer(option.substring(1));
				} else if(option.compareTo("0") == 0) {
					run = false;
				}
			}
		}
	}

	public DHCPSearcher() {
		servers = new ArrayList<DHCPServer>();
		log_list = Collections.synchronizedList(new ArrayList<String>());
		ignored_servers = new ArrayList<String>();
		number_of_pooled_threads = 5;
		check_leases = true;
		check_reservations = true;
		check_inactive_reservations = true;

		folder_scripts = "scripts\\";
		folder_log = "log\\";
		folder_output = "output\\";
		folder_data_servers = "data\\servers\\";
		folder_data_scopes = "data\\scopes\\";
		folder_data_leases = "data\\leases\\";
		folder_data_reservations = "data\\reservations\\";
		folder_data_inactive_reservations = "data\\inactivereservations\\";
	}

	public boolean getCheckLeases() {
		return check_leases;
	}

	public boolean getCheckReservations() {
		return check_reservations;
	}

	public boolean getCheckInactiveReservations() {
		return check_inactive_reservations;
	}

	public void setCheckLeases(boolean check_leases) {
		this.check_leases = check_leases;
	}

	public void setCheckReservations(boolean check_reservations) {
		this.check_reservations = check_reservations;
	}

	public void setCheckInactiveReservations(boolean check_inactive_reservations) {
		this.check_inactive_reservations = check_inactive_reservations;
	}

	public List<String> getIgnoredServers() {
		return ignored_servers;
	}

	public void initialiseFolders() {
		new File("./data/servers").mkdirs();
		new File("./data/scopes").mkdirs();
		new File("./data/leases").mkdirs();
		new File("./data/reservations").mkdirs();
		new File("./data/inactivereservations").mkdirs();
		new File("./log").mkdirs();
		new File("./output").mkdirs();
	}

	public void addIgnoredServer(String server) {
		if(!ignored_servers.contains(server)) {
			ignored_servers.add(server);
		}
	}

	public void removeIgnoredServer(String server) {
		if(ignored_servers.contains(server)) {
			ignored_servers.remove(server);
		}
	}

	public ArrayList<String> generateDHCPServerList() throws IOException {
		logMessage("Attempting to generate DHCP server list", LOG_TYPE.INFO, true);
		try{
			ArrayList<String> dhcp_servers = new ArrayList<>();
			String command = "Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process | powershell.exe -File \"" + folder_scripts + "GenerateDHCPServerList.ps1\"";
			ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe", "-Command", command);
			builder.redirectErrorStream(true);
			Process power_shell_process = builder.start();
			BufferedReader powershell_process_output_stream = new BufferedReader(new InputStreamReader(power_shell_process.getInputStream()));
			String line = "";
			while((line = powershell_process_output_stream.readLine()).compareTo("EndOfScriptGenerateDHCPServerList") != 0) {
				if(!line.isEmpty()) {
					dhcp_servers.add(line);
				}
			}
			powershell_process_output_stream.close();
			power_shell_process.destroy();
			logMessage("Successfully generated DHCP server list", LOG_TYPE.INFO, true);
			return dhcp_servers;
		} catch(IOException e) {	
			logMessage("Failed to generated DHCP server list", LOG_TYPE.INFO, true);
			throw e;
		}
	}

	//Load DHCP text data into program
	public void loadDHCPData() throws IOException {
		List<DHCPServer> dhcp_servers = new ArrayList<DHCPServer>();
		
		logMessage("Attempting to load DHCP servers list", LOG_TYPE.INFO, true);
		try {
			List<List<String>> loaded_servers = loadDHCPServers();
			for(List<String> loaded_server : loaded_servers) {
				if(loaded_server.equals(loaded_servers.get(0))) {
					if(loaded_server.get(0).contains("Error: ")) {
						String error_message = "DHCPServers were not loaded successfully. Error message is " + loaded_server.get(0);
						logMessage(error_message, LOG_TYPE.ERROR, true);
						throw new IOException(error_message);
					}
				} else {
					DHCPServer server = new DHCPServer(loaded_server.get(1), loaded_server.get(0));
					dhcp_servers.add(server);
				}
			}
			logMessage("Finished loading DHCP servers list", LOG_TYPE.INFO, true);

			for(DHCPServer server : dhcp_servers) {
				logMessage("Attempting to load scopes for server " + server.server_name, LOG_TYPE.INFO, true);
				try {
					List<List<String>> loaded_scopes = loadScopesForServer(server.server_name);
					for(List<String> loaded_scope : loaded_scopes) {
						if(loaded_scope.equals(loaded_scopes.get(0))) {
							if(loaded_scope.get(0).contains("Error: ")) {
								server.state_of = STATE.ERROR;
								logMessage("Unable to get scopes on server. Error message is " + loaded_scope.get(0), LOG_TYPE.ERROR, true);
							}
						} else {
							Scope scope = new Scope(loaded_scope.get(1), loaded_scope.get(0));
							logMessage("Attempting to load scope " + scope.scope_name + " on server " + server.server_name, LOG_TYPE.INFO, true);
							try{
								logMessage("Attempting to load leases for scope " + scope.scope_name + " on server " + server.server_name, LOG_TYPE.INFO, true);
								List<List<String>> loaded_leases = loadLeasesForScope(server.server_name, scope.scope_ip);
								for(List<String> loaded_lease : loaded_leases) {
									if(loaded_lease.equals(loaded_leases.get(0))) {
										if(loaded_lease.get(0).contains("Error: ")) {
											scope.state_of_leases = STATE.ERROR;
											logMessage("Leases for scope " + scope.scope_name + " on server " + server.server_name + " were not loaded succesfully. Error message is " + loaded_lease.get(0), LOG_TYPE.ERROR, true);
										}
									} else {
										IPLease lease = new IPLease(loaded_lease.get(0), loaded_lease.get(1), loaded_lease.get(2), loaded_lease.get(3));
										scope.ip_leases.add(lease);
									}
								}
								logMessage("Finished loading leases for scope " + scope.scope_name + " on server " + server.server_name, LOG_TYPE.INFO, true);
							} catch(IOException e) {
								throw new IOException(e);
							}
							try{
								logMessage("Attempting to load reservations for scope " + scope.scope_name + " on server " + server.server_name, LOG_TYPE.INFO, true);
								List<List<String>> loaded_reservations = loadReservationsForScope(server.server_name, scope.scope_ip);
								for(List<String> loaded_reservation : loaded_reservations) {
									if(loaded_reservation.equals(loaded_reservations.get(0))) {
										if(loaded_reservation.get(0).contains("Error: ")) {
											scope.state_of_reservations = STATE.ERROR;
											logMessage("Reservations for scope " + scope.scope_name + " on server " + server.server_name + " were not loaded succesfully. Error message is " + loaded_reservation.get(0), LOG_TYPE.ERROR, true);
										}
									} else {
										IPLease reservation = new IPLease(loaded_reservation.get(0), loaded_reservation.get(1), loaded_reservation.get(2));
										scope.ip_reservations.add(reservation);
									}
								}
								logMessage("Finished loading reservations for scope " + scope.scope_name + " on server " + server.server_name, LOG_TYPE.INFO, true);
							} catch(IOException e) {
								throw new IOException(e);
							}
							try{
								logMessage("Attempting to load inactive reservations for scope " + scope.scope_name + " on server " + server.server_name, LOG_TYPE.INFO, true);
								List<List<String>> loaded_inactive_reservations = loadInactiveReservationsForScope(server.server_name, scope.scope_ip);
								for(List<String> loaded_inactive_reservation : loaded_inactive_reservations) {
									if(loaded_inactive_reservation.equals(loaded_inactive_reservations.get(0))) {
										if(loaded_inactive_reservation.get(0).contains("Error: ")) {
											scope.state_of_inactive_reservations = STATE.ERROR;
											logMessage("Inactive Reservations for scope " + scope.scope_name + " on server " + server.server_name + " were not loaded succesfully. Error message is " + loaded_inactive_reservation.get(0), LOG_TYPE.ERROR, true);
										}
									} else {
										IPLease inactive_reservation = new IPLease(loaded_inactive_reservation.get(0), loaded_inactive_reservation.get(1), loaded_inactive_reservation.get(2));
										scope.ip_inactive_reservations.add(inactive_reservation);
									}
								}
								logMessage("Finished loading inactive reservations for scope " + scope.scope_name + " on server " + server.server_name, LOG_TYPE.INFO, true);
							} catch(IOException e) {
								throw new IOException(e);
							}
							if(scope.state_of_leases == STATE.ERROR && scope.state_of_reservations == STATE.ERROR && scope.state_of_inactive_reservations == STATE.ERROR) {
								scope.state_of = STATE.ERROR;
								logMessage("Unable to load both leases and reservations for scope " + scope.scope_name + " on server " + server.server_name + ". Scope has been put into the errored state", LOG_TYPE.ERROR, true);
							}
							server.scopes.add(scope);
							logMessage("Finished loading scope " + scope.scope_name + " on server " + server.server_name, LOG_TYPE.INFO, true);
						}
					}
				} catch(IOException e) {
					throw new IOException(e);
				}
				logMessage("Finished loading scopes for server " + server.server_name, LOG_TYPE.INFO, true);
			}

			servers = dhcp_servers;
		} catch(IOException e) {
			throw new IOException(e);
		}
	}
	
	//Subfunction - Load DHCP server list
	private List<List<String>> loadDHCPServers() throws IOException {
		try{
			List<List<String>> loaded_dataset = loadDatasetFromFile(folder_data_servers + "dhcp_servers.txt");
			return loaded_dataset;
		} catch(IOException e) {
			throw new IOException(e);
		}
	}

	//Subfunction - Load scope list for single DHCP server
	private List<List<String>> loadScopesForServer(String server) throws IOException {
		String filename_friendly_server = makeFilenameFriendly(server);
		try{
			List<List<String>> loaded_dataset = loadDatasetFromFile(folder_data_scopes + filename_friendly_server + ".txt");
			return loaded_dataset;
		} catch(IOException e) {
			throw new IOException(e);
		}
	}

	//Subfunction - Load leased IP list for single scope on single DHCP server
	private List<List<String>> loadLeasesForScope (String server, String scope) throws IOException {
		String filename_friendly_server = makeFilenameFriendly(server);
		String filename_friendly_scope = makeFilenameFriendly(scope);
		try{
			List<List<String>> loaded_dataset = loadDatasetFromFile(folder_data_leases + filename_friendly_server + "_scope_" + filename_friendly_scope + ".txt");
			return loaded_dataset;
		} catch(IOException e) {
			throw new IOException(e);
		}
	}

	//Subfunction - Load reserved IP list for single scope on single DHCP server
	private List<List<String>> loadReservationsForScope (String server, String scope) throws IOException {
		String filename_friendly_server = makeFilenameFriendly(server);
		String filename_friendly_scope = makeFilenameFriendly(scope);
		try{
			List<List<String>> loaded_dataset = loadDatasetFromFile(folder_data_reservations + filename_friendly_server + "_scope_" + filename_friendly_scope + ".txt");
			return loaded_dataset;
		} catch(IOException e) {
			throw new IOException(e);
		}
	}

	//Subfunction - Load inactive reserved IP list for single scope on single DHCP server
	private List<List<String>> loadInactiveReservationsForScope (String server, String scope) throws IOException {
		String filename_friendly_server = makeFilenameFriendly(server);
		String filename_friendly_scope = makeFilenameFriendly(scope);
		try{
			List<List<String>> loaded_dataset = loadDatasetFromFile(folder_data_inactive_reservations + filename_friendly_server + "_scope_" + filename_friendly_scope + ".txt");
			return loaded_dataset;
		} catch(IOException e) {
			throw new IOException(e);
		}
	}

	//Build DHCP text data using powershell
	public void buildDHCPData() throws IOException{
		logMessage("Attempting to build DHCP server list", LOG_TYPE.INFO, true);
		try{
			buildDHCPServers();
			logMessage("Successfully built DHCP server list", LOG_TYPE.INFO, true);
			List<List<String>> dhcp_servers = loadDHCPServers();
			if(dhcp_servers.size() > 0) {
				ExecutorService thread_pool = Executors.newFixedThreadPool(number_of_pooled_threads);
				for(List<String> server : dhcp_servers) {
					if(!server.equals(dhcp_servers.get(0)) && !ignored_servers.contains(server.get(1))) {
						logMessage("Attempting to build scopes for server " + server.get(1), LOG_TYPE.INFO, true);
						try{
							buildScopesForServer(server.get(1));
							logMessage("Successfully built scopes for server " + server.get(1), LOG_TYPE.INFO, true);
							List<List<String>> loaded_scopes = loadScopesForServer(server.get(1));
							if(loaded_scopes.size() > 0) {
								for(List<String> scope : loaded_scopes) {
									if(!scope.equals(loaded_scopes.get(0))) {
										if(check_leases) {
											thread_pool.submit(new powershell_task(1, server.get(1), scope.get(0), this));
										}
										if(check_reservations) {
											thread_pool.submit(new powershell_task(2, server.get(1), scope.get(0), this));
										}
										if(check_inactive_reservations) {
											thread_pool.submit(new powershell_task(3, server.get(1), scope.get(0), this));
										}
									}
								}
							}
						} catch(IOException e) {
							logMessage(e.getMessage(), LOG_TYPE.ERROR, true);
							try{
								String filename_friendly_server = makeFilenameFriendly(server.get(1));
								writeToFile(folder_data_scopes + filename_friendly_server + ".txt", "Error: " + e.getMessage(), false);
							} catch(Exception e2) {
								throw new IOException(e2);
							}
						}
					}
				}
				logMessage("All tasks have been scheduled, awaiting task completion", LOG_TYPE.INFO, true);
				thread_pool.shutdown();
				boolean thread_pool_terminated = false;
				while(!thread_pool_terminated) {
					thread_pool_terminated = thread_pool.isTerminated();
				}
				logMessage("All tasks completed", LOG_TYPE.INFO, true);
			}
		} catch(IOException e) {
			logMessage(e.getMessage(), LOG_TYPE.ERROR, true);
			try{
				writeToFile(folder_data_servers + "dhcp_servers.txt", "Error: " + e.getMessage(), false);
			} catch(Exception e2) {
				throw new IOException(e2);
			}
		}
	}
	
	//Subfunction - Build DHCP server list
	private void buildDHCPServers() throws IOException {
		try{
			String script = folder_scripts + "GetDHCPServer.ps1\"";
			runPowershellScript(script);
		} catch(Exception e) {
			throw new IOException(e);
		}
	}
	
	//Subfunction - Build scope list for single DHCP server
	private void buildScopesForServer(String server) throws IOException {
		try{
			String script = folder_scripts + "GetScopesOnServer.ps1\" -server " + server;
			runPowershellScript(script);
		} catch(Exception e) {
			throw new IOException(e);
		}
	}

	//Subfunction - Build leased IP list for single scope on single DHCP server
	public void buildLeasesForScope (String server, String scope) throws IOException {
		try{
			String script = folder_scripts + "GetLeasedIPOnScope.ps1\" -server " + server + " -scope " + scope;
			runPowershellScript(script);
		} catch(Exception e) {
			throw new IOException(e);
		}
	}

	//Subfunction - Build reserved IP list for single scope on single DHCP server
	public void buildReservationsForScope (String server, String scope) throws IOException {
		try{
			String script = folder_scripts + "GetReservedIPOnScope.ps1\" -server " + server + " -scope " + scope;
			runPowershellScript(script);
		} catch(Exception e) {
			throw new IOException(e);
		}
	}

	//Subfunction - Build inactive reserved IP list for single scope on single DHCP server
	public void buildInactiveReservationsForScope (String server, String scope) throws IOException {
		try{
			String script = folder_scripts + "GetInactiveReservedIPOnScope.ps1\" -server " + server + " -scope " + scope;
			runPowershellScript(script);
		} catch(Exception e) {
			throw new IOException(e);
		}
	}

	private void runPowershellScript(String script) throws IOException {
		try{
			String command = "powershell.exe Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process | powershell.exe \"" + script;
  			Process power_shell_process = Runtime.getRuntime().exec(command);
			BufferedReader powershell_process_error_stream = new BufferedReader(new InputStreamReader(power_shell_process.getErrorStream()));
			AtomicBoolean process_terminated = new AtomicBoolean(false);
			Thread process_timer = new Thread() {
				public void run() {
					try{
						TimeUnit.MINUTES.sleep(2);
						logMessage("Running script " + script + " has taken more than 5 minutes, it will now be ended forcefully", LOG_TYPE.WARNING, true);
						process_terminated.set(true);
						power_shell_process.destroy();
					} catch(Exception e) {}
				};
			};
			process_timer.start();
			power_shell_process.waitFor();
			process_timer.interrupt();
			if(!process_terminated.get()) {
				String error_stream = powershell_process_error_stream.readLine();
				if(error_stream != null) {
					throw new IOException("Unable to run powershell script " + script + ". Error stream: " + error_stream);
				}
			} else {
				throw new IOException("Running script " + script + " took more than 5 minutes. Process was ended forcefully");
			}
		} catch(Exception e) {
			throw new IOException(e);
		}
	}

	public void logMessage(String message, LOG_TYPE state, boolean include_timestamp) {
		String log_message = "";
		if(state == LOG_TYPE.INFO) {
			log_message += "Info: ";
		} else if(state == LOG_TYPE.WARNING) {
			log_message += "Warning: ";
		} else if(state == LOG_TYPE.ERROR) {
			log_message += "ERROR: ";
		}

		if(include_timestamp) {
			SimpleDateFormat human_readable_timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
			Date timestamp = new Date();
			String format_timestamp = human_readable_timestamp.format(timestamp);
			log_message += "[" + format_timestamp + "] ";
		}

		log_message += message;

		log_list.add(log_message);
		System.out.println(log_message);
	}
	
	public String makeFilenameFriendly (String filename) {
		String new_filename = filename.replace('.', '_');
		return new_filename;
	}

	//Check file exists function
	private void checkFile(String filename) throws IOException {
		try {
			File file = new File(filename);
			try(BufferedReader br = new BufferedReader(new FileReader(file))) {
			}
		} catch(Exception e) {
			throw new IOException();
		}
	}

	//Load dataset from file function
	private List<List<String>> loadDatasetFromFile(String filename) throws IOException {
		List<String> read_data = new ArrayList<String>();
		try {
			read_data = readFromFile(filename);
		} catch(Exception e) {
			throw new IOException("Unable to read from file " + filename);
		}
		List<List<String>> processed_data  = new ArrayList<List<String>>();
		for(String data_line : read_data) {
			String[] split_read_data = data_line.split("\t");
			List<String> processed_line = new ArrayList<String>();
			for(String data : split_read_data) {
				processed_line.add(data);
			}
			processed_data.add(processed_line);
		}

		return processed_data;
	}

	//Read from file function
	private List<String> readFromFile(String filename) throws IOException {
		List<String> read_data = new ArrayList<String>();
		try {
			File file = new File(filename);
			try(BufferedReader br = new BufferedReader(new FileReader(file))) {
  				for(String line; (line = br.readLine()) != null; ) {
       					read_data.add(line);
   				}
			}
		} catch(Exception e) {
			throw new IOException("Unable to read from file " + filename);
		}
		return read_data;
	}

	//Write to file function - Single line
	public void writeToFile(String filename, String line, boolean append) throws IOException {
		try {
           		File file = new File(filename);
            		BufferedWriter writer = new BufferedWriter(new FileWriter(file, append));
                	writer.write(line);
            		writer.close();
        	} catch (Exception e) {
            		throw new IOException(e);
       		}
	}

	//Write to file function - Multiple lines
	private void writeToFile(String filename, List<String> lines, boolean append) throws IOException {
		try {
           		File file = new File(filename);
            		BufferedWriter writer = new BufferedWriter(new FileWriter(file, append));
            		for (String line : lines) {
				writer.write(line);
				if(!line.equals(lines.get(lines.size() - 1))) {
                			writer.write(System.lineSeparator());
				}
            		}
            		writer.close();
        	} catch (Exception e) {
            		throw new IOException(e);
       		}
	}
	
	public void writeOutputToFile() throws IOException {
		try{
			writeToFile(folder_output + "output.txt", toString(), false);
		} catch(Exception e) {
			throw new IOException(e);
		}
	}

	public void writeLogToFile() throws IOException {
		try{
			writeToFile(folder_log + "log.txt", log_list, false);
		} catch(Exception e) {
			throw new IOException(e);
		}
	}

	//Print dataset as readable string
	public String toString() {
		String to_string = "";
		int count = 0;
		for(DHCPServer server : servers) {
			if(count > 0) {
				to_string += System.lineSeparator();
			}
			to_string += server.toString();
			count++;
		}
		return to_string;
	}

}

class DHCPServer
{
	public String server_name;
	public String server_ip;
	public List<Scope> scopes;
	public DHCPSearcher.STATE state_of;

	public DHCPServer() {
		server_name = "";
		server_ip = "";
		scopes = new ArrayList<Scope>();
		state_of = DHCPSearcher.STATE.OK;
	}

	public DHCPServer(String new_server_name, String new_server_ip) {
		server_name = new_server_name;
		server_ip = new_server_ip;
		scopes = new ArrayList<Scope>();
		state_of = DHCPSearcher.STATE.OK;
	}

	public DHCPServer(DHCPSearcher.STATE new_state_of) {
		server_name = "";
		server_ip = "";
		scopes = new ArrayList<Scope>();
		state_of = new_state_of;
	}

	public String toString() {
		String to_string = "";
		to_string += server_ip + '\t' + server_name;
		if(state_of == DHCPSearcher.STATE.ERROR) {
			to_string += "\tServer is errored";
		} else {
			to_string += System.lineSeparator() + "Scopes";
			for(Scope scope : scopes) {
				to_string += System.lineSeparator();
				to_string += scope.toString();
			}
		}
		return to_string;
	}	
}

class Scope
{
	public String scope_name;
	public String scope_ip;
	public List<IPLease> ip_leases;
	public List<IPLease> ip_reservations;
	public List<IPLease> ip_inactive_reservations;
	public DHCPSearcher.STATE state_of;
	public DHCPSearcher.STATE state_of_leases;
	public DHCPSearcher.STATE state_of_reservations;
	public DHCPSearcher.STATE state_of_inactive_reservations;

	public Scope() {
		scope_name = "";
		scope_ip = "";
		ip_leases = new ArrayList<IPLease>();
		ip_reservations = new ArrayList<IPLease>();
		ip_inactive_reservations = new ArrayList<IPLease>();
		state_of = DHCPSearcher.STATE.OK;
		state_of_leases = DHCPSearcher.STATE.OK;
		state_of_reservations = DHCPSearcher.STATE.OK;
		state_of_inactive_reservations = DHCPSearcher.STATE.OK;
	}

	public Scope(String new_scope_name, String new_scope_ip) {
		scope_name = new_scope_name;
		scope_ip = new_scope_ip;
		ip_leases = new ArrayList<IPLease>();
		ip_reservations = new ArrayList<IPLease>();
		ip_inactive_reservations = new ArrayList<IPLease>();
		state_of = DHCPSearcher.STATE.OK;
		state_of_leases = DHCPSearcher.STATE.OK;
		state_of_reservations = DHCPSearcher.STATE.OK;
		state_of_inactive_reservations = DHCPSearcher.STATE.OK;
	}
	
	public Scope(DHCPSearcher.STATE new_state_of) {
		scope_name = "";
		scope_ip = "";
		ip_leases = new ArrayList<IPLease>();
		ip_reservations = new ArrayList<IPLease>();
		ip_inactive_reservations = new ArrayList<IPLease>();
		state_of = new_state_of;
		state_of_leases = new_state_of;
		state_of_reservations = new_state_of;
		state_of_inactive_reservations = new_state_of;
	}

	public String toString() {
		String to_string = "";
		to_string += '\t' + scope_ip + '\t' + scope_name;
		if(state_of == DHCPSearcher.STATE.ERROR) {
			to_string += "\tScope is errored";
		} else {
			if(state_of_leases == DHCPSearcher.STATE.ERROR) {
				to_string += System.lineSeparator() + '\t' + '\t' + "Leased IP's are errored";
			} else {
				to_string += System.lineSeparator() + '\t' + '\t' + "Leased IP's";
				for(IPLease lease : ip_leases) {
					to_string += System.lineSeparator() +'\t' + '\t' + '\t' + lease.toString();
				}
			}
			if(state_of_reservations == DHCPSearcher.STATE.ERROR) {
				to_string += System.lineSeparator() + '\t' + '\t' + "Reserved IP's are errored";
			} else {
				to_string += System.lineSeparator() + '\t' + '\t' + "Reserved IP's";
				for(IPLease lease : ip_reservations) {
					to_string += System.lineSeparator() + '\t' + '\t' + '\t' + lease.toString();
				}
			}
			if(state_of_inactive_reservations == DHCPSearcher.STATE.ERROR) {
				to_string += System.lineSeparator() + '\t' + '\t' + "Inactive Reserved IP's are errored";
			} else {
				to_string += System.lineSeparator() + '\t' + '\t' + "Inactive Reserved IP's";
				for(IPLease lease : ip_inactive_reservations) {
					to_string += System.lineSeparator() + '\t' + '\t' + '\t' + lease.toString();
				}
			}
		}
		return to_string;
	}
}

class IPLease
{
	public String ip;
	public String hostname;
	public String mac;
	public String expiry;
	public DHCPSearcher.STATE state_of;

	public IPLease() {
		ip = "";
		hostname = "";
		mac = "";
		expiry = "";
		state_of = DHCPSearcher.STATE.OK;
	}

	public IPLease(String new_ip, String new_hostname, String new_mac) {
		ip = new_ip;
		hostname = new_hostname;
		mac = new_mac;
		expiry = "";
		state_of = DHCPSearcher.STATE.OK;
	}

	public IPLease(String new_ip, String new_hostname, String new_mac, String new_expiry) {
		ip = new_ip;
		hostname = new_hostname;
		mac = new_mac;
		expiry = new_expiry;
		state_of = DHCPSearcher.STATE.OK;
	}

	public IPLease(DHCPSearcher.STATE new_state_of) {
		ip = "";
		hostname = "";
		mac = "";
		expiry = "";
		state_of = new_state_of;
	}

	public String toString() {
		String to_string = "";
		to_string += ip + '\t' + hostname + '\t' + mac;
		if(expiry.compareTo("") != 0) {
			to_string += '\t' + expiry;
		}
		if(state_of == DHCPSearcher.STATE.ERROR) {
			to_string += "\tLease is errored";
		}
		return to_string;
	}
}

class powershell_task implements Runnable {
	private int type;
	private String server;
	private String scope;
	private DHCPSearcher searcher;

	powershell_task(int type, String server, String scope, DHCPSearcher searcher) {
		this.type = type;
		this.server = server;
		this.scope = scope;
		this.searcher = searcher;
	}

	@Override
	public void run() {
		if(type == 1) {
			searcher.logMessage("Attempting to build leases for server " + server + " and scope " + scope, DHCPSearcher.LOG_TYPE.INFO, true);
		} else if(type == 2) {
			searcher.logMessage("Attempting to build reservations for server " + server + " and scope " + scope, DHCPSearcher.LOG_TYPE.INFO, true);
		} else if(type == 3) {
			searcher.logMessage("Attempting to build inactive reservations for server " + server + " and scope " + scope, DHCPSearcher.LOG_TYPE.INFO, true);
		}
	
		try{	
			if(type == 1) {
				searcher.buildLeasesForScope(server, scope);
				searcher.logMessage("Successfully built leases for server " + server + " and scope " + scope, DHCPSearcher.LOG_TYPE.INFO, true);
			} else if(type == 2) {
				searcher.buildReservationsForScope(server, scope);
				searcher.logMessage("Successfully built reservations for server " + server + " and scope " + scope, DHCPSearcher.LOG_TYPE.INFO, true);
			} else if(type == 3) {
				searcher.buildInactiveReservationsForScope(server, scope);
				searcher.logMessage("Successfully built inactive reservations for server " + server + " and scope " + scope, DHCPSearcher.LOG_TYPE.INFO, true);
			}
		} catch(IOException e) {
			searcher.logMessage(e.getMessage(), DHCPSearcher.LOG_TYPE.ERROR, true);
			try{
				String filename_friendly_server = searcher.makeFilenameFriendly(server);
				String filename_friendly_scope = searcher.makeFilenameFriendly(scope);
				if(type == 1) {
					searcher.writeToFile(searcher.folder_data_leases + filename_friendly_server + "_scope_" + filename_friendly_scope + ".txt", "Error: " + e.getMessage(), false);
				} else if(type == 2) {
					searcher.writeToFile(searcher.folder_data_leases + filename_friendly_server + "_scope_" + filename_friendly_scope + ".txt", "Error: " + e.getMessage(), false);
				} else if(type == 3) {
					searcher.writeToFile(searcher.folder_data_leases + filename_friendly_server + "_scope_" + filename_friendly_scope + ".txt", "Error: " + e.getMessage(), false);
				}
			} catch(Exception e2) {
				searcher.logMessage("A major error has occurred, data may not have been built correctly: "+ e.getMessage(), DHCPSearcher.LOG_TYPE.ERROR, true);
			}
		}
	}
}