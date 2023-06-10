package core;
import gui.DTNSimGUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.SplashScreen;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JLabel;

import Develop.Main_Window;
import Develop.OneSimUI;
import ui.DTNSimTextUI;

/**
 * Project Name:Large-scale Satellite Networks Simulator (LSNS)
 * File DTNSim.java
 * Package Name:core
 * Description: Simulator's main class
 * Copyright 2018 University of Science and Technology of China , Infonet
 * lijian9@mail.ustc.mail.cn. All Rights Reserved.
 */
public class DTNSim {
	/** If this option ({@value}) is given to program, batch mode and
	 * Text UI are used*/
	public static final String BATCH_MODE_FLAG = "-b";
	/** Delimiter for batch mode index range values (colon) */
	public static final String RANGE_DELIMETER = ":";

	/** Name of the static method that all resettable classes must have
	 * @see #registerForReset(String) */
	public static final String RESET_METHOD_NAME = "reset";
	/** List of class names that should be reset between batch runs */
	private static List<Class<?>> resetList = new ArrayList<Class<?>>();


	/** user setting in the sim -setting id ({@value})*/
	public static final String USERSETTINGNAME_S = "userSetting";
	/** choose GUI in the sim -setting id ({@value})*/
	public static final String GUI = "GUI";

	/** core: SimScenario */
	public static final String HOSTSMODENAME_S = "hostsMode";
	public static final String CLUSTER_S = "cluster";
	public static final String NORMAL_S = "normal";
	public static final String NROFPLANE_S = "nrofPlane";
	public static final String nrofFile_s = "nrofFile";

	/** Router : RelayRouterforInternetAccess */
	public static final String GROUP = "Group";
	public static final String HANDOVER_CRITERION = "handoverCriterion";
	public static final String LEO_RADIUS = "LEO_Radius";
	public static final String ENABLE_BACKUPSATELLITE = "enableBackupSatellite";
    public static final String PREMIGRATION_HANDOVER = "preMigrationHandover";
    public static final String NORMAL_HANDOVER = "normalHandover";
    public static final String SINR_HANDOVER = "SINRHandover";
	public static final String TRANSMISSION_MODE = "transmissionMode";
	public static final String NROF_BACKUPSATELLITES = "nrofBackupSatellites";
	public static final String NROF_ROBUSTBACKUPSATELLITE = "nrofRobustBackupSatellite";
	public static final String ACCESS_SAT_UPDATEINTERVAL = "accessSatellitesUpdateInterval";
	public static final String TRANSMIT_RANGE = "transmitRange";
	public static final String INTERFACE = "Interface";
	public static final String CHANNEL_MODEL = "channelModel";
	public static final String RAYLEIGH = "Rayleigh";
	public static final String RICE = "Rice";
    public static final String SHADOWING = "Shadowing";
	public static final String NODE_TYPE = "Type";
	public static final String GS = "GroundStation";
	public static final String SAT = "Satellite";
	public static final String USER = "TerrestrialUser";
	public static final String MIN_ELEVATIONANGLE = "minimumElevationAngle";
    public static final String MAX_CONNECTIONDURATION = "maximumConnectionDuration";
    public static final String SPEED_GSLINK = "constantSpeedOfGroundStationLink";
    public static final String SHADOWING_MODE = "shadowingMode";
    public static final String SHADOWING_GLOBAL_LINK = "globalLink";
    public static final String SHADOWING_PARTIAL_LINK = "partialLink";
    public static final String SHADOWING_TRANSFERRING_LINK = "transferringLink";
    public static final String NROF_SHADOWINGLINK= "nrofShadowingLink";
    public static final String HANDOVER_SNR_THRESHOLD = "SNR_threshold";
    public static final String FASTMODE = "fastMode";

	/** input: MessageCreateEvent */
	public static final String MESSAGECREATEMODE = "messageCreateMode";
	public static final String BATCHCREATENUMBER = "batchCreateNumber";
	public static final String BATCH = "batch";

	/** Interface: SatelliteWithChannelModelInterface */
	/** router mode in the sim -setting id ({@value})*/
	public static final String ROUTERMODENAME_S = "routerMode";

	public static final String TRANSMITTING_POWER = "TransmittingPower";
	public static final String TRANSMITTING_FREQUENCY = "TransmittingFrequency";
	public static final String BANDWIDTH = "Bandwidth";

	/** Core: VBRConnectionWithChannelModel */
	public static final String SCENARIO = "Scenario";
	public static final String UPDATEINTERVAL = "updateInterval";
	public static final String ENDTIME = "endTime";

	/**Core: channelModel */
    public static final String SHADOWING_DURATION ="shadowingDuration";
	public static final String SHADOWING_PROB = "shadowingProbability";
	public static final String ENERGYRATIO_RICE = "energyRatio_Rice";
	public static final String TRANSMIT_ANTENNADIAMETER = "transmitAntennaDiameter";
	public static final String RECEIVER_ANTENNADIAMETER = "receiverAntennaDiameter";
	public static final String SPECTRIAL_DENSITYNOISE= "spectralDensityNoisePower";
	/**
	 * Starts the user interface with given arguments.
	 * If first argument is {@link #BATCH_MODE_FLAG}, the batch mode and text UI
	 * is started. The batch mode option must be followed by the number of runs,
	 * or a with a combination of starting run and the number of runs, 
	 * delimited with a {@value #RANGE_DELIMETER}. Different settings from run
	 * arrays are used for different runs (see 
	 * {@link Settings#setRunIndex(int)}). Following arguments are the settings 
	 * files for the simulation run (if any). For GUI mode, the number before 
	 * settings files (if given) is the run index to use for that run.
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {
		boolean batchMode = false;
		int nrofRuns[] = {0,1};
		String confFiles[];
		int firstConfIndex = 0;
		int guiIndex = 0;

		/* set US locale to parse decimals in consistent way */
		java.util.Locale.setDefault(java.util.Locale.US);
		
		if (args.length > 0) {
			if (args[0].equals(BATCH_MODE_FLAG)) {
				batchMode = true;
                if (args.length == 1) {
                    firstConfIndex = 1;
                }
                else {
                    nrofRuns = parseNrofRuns(args[1]);
                    firstConfIndex = 2;
                }
			}
			else { /* GUI mode */				
				try { /* is there a run index for the GUI mode ? */
					guiIndex = Integer.parseInt(args[0]);
					firstConfIndex = 1;
				} catch (NumberFormatException e) {
					firstConfIndex = 0;
				}
			}
			confFiles = args;
		}
		else {
			confFiles = new String[] {null};
		}

		initSettings(confFiles, firstConfIndex);
		
		if (batchMode) {
			long startTime = System.currentTimeMillis();
			for (int i=nrofRuns[0]; i<nrofRuns[1]; i++) {
				print("Run " + (i+1) + "/" + nrofRuns[1]);
				Settings.setRunIndex(i);
				resetForNextRun();
				new DTNSimTextUI().start();
			}
			double duration = (System.currentTimeMillis() - startTime)/1000.0;
			print("---\nAll done in " + String.format("%.2f", duration) + "s");
		}
		else {		
			Settings.setRunIndex(guiIndex);
			
	        try
	        {
	            String[] info = new String[]
	            { "正在初始化API...", "正在加载资源...", "正在初始化图形界面..." };
	            SplashScreen splash = SplashScreen.getSplashScreen();
	            Graphics g = splash.createGraphics();
	            
	            if (splash != null)
	            {
	                for (int i = 0; i < 3; i++)
	                {
	                    g.setColor(Color.BLACK);
	                    g.drawString(info[i], 350, 270 + i * 15);
	                    splash.update();
	                    Thread.sleep((i*3 + 3)*100);
	                }
	            }
	        }
	        catch (Exception e)
	        {
	        }
			Settings s = new Settings(USERSETTINGNAME_S);
//	        if (s.getBoolean(GUI))
//	        	//new GUI -- some bugs need to be fixed
//	        	new OneSimUI().start();
//	        else
	        	//old GUI
				new DTNSimGUI().start();
		}
	}
	
	/**
	 * Initializes Settings
	 * @param confFiles File name paths where to read additional settings 
	 * @param firstIndex Index of the first config file name
	 */
	private static void initSettings(String[] confFiles, int firstIndex) {
		int i = firstIndex;

        if (i >= confFiles.length) {
            return;
        }

		try {
			Settings.init(confFiles[i]);
			for (i=firstIndex+1; i<confFiles.length; i++) {
				Settings.addSettings(confFiles[i]);
			}

		}
		catch (SettingsError er) {
			try {
				Integer.parseInt(confFiles[i]);
			}
			catch (NumberFormatException nfe) {
				/* was not a numeric value */
				System.err.println("Failed to load settings: " + er);
				System.err.println("Caught at " + er.getStackTrace()[0]);			
				System.exit(-1);
			}
			System.err.println("Warning: using deprecated way of " + 
					"expressing run indexes. Run index should be the " + 
					"first option, or right after -b option (optionally " +
					"as a range of start and end values).");
			System.exit(-1);
		}
	}
	
	/**
	 * Registers a class for resetting. Reset is performed after every
	 * batch run of the simulator to reset the class' state to initial
	 * state. All classes that have static fields that should be resetted
	 * to initial values between the batch runs should register using 
	 * this method. The given class must have a static implementation
	 * for the resetting method (a method called {@value #RESET_METHOD_NAME} 
	 * without any parameters).
	 * @param className Full name (i.e., containing the packet path) 
	 * of the class to register. For example: <code>core.SimClock</code> 
	 */
	public static void registerForReset(String className) {
		Class<?> c = null;
		try {
			c = Class.forName(className);
			c.getMethod(RESET_METHOD_NAME);
		} catch (ClassNotFoundException e) {
			System.err.println("Can't register class " + className + 
					" for resetting; class not found");
			System.exit(-1);
					
		}
		catch (NoSuchMethodException e) {
			System.err.println("Can't register class " + className + 
			" for resetting; class doesn't contain resetting method");
			System.exit(-1);
		}
		resetList.add(c);
	}
	
	/**
	 * Resets all registered classes.
	 */
	private static void resetForNextRun() {
		for (Class<?> c : resetList) {
			try {
				Method m = c.getMethod(RESET_METHOD_NAME);
				m.invoke(null);
			} catch (Exception e) {
				System.err.println("Failed to reset class " + c.getName());
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
	
	/**
	 * Parses the number of runs, and an optional starting run index, from a 
	 * command line argument
	 * @param arg The argument to parse
	 * @return The first and (last_run_index - 1) in an array
	 */
	private static int[] parseNrofRuns(String arg) {
		int val[] = {0,1};	
		try {
			if (arg.contains(RANGE_DELIMETER)) {
				val[0] = Integer.parseInt(arg.substring(0, 
						arg.indexOf(RANGE_DELIMETER))) - 1;
				val[1] = Integer.parseInt(arg.substring(arg.
						indexOf(RANGE_DELIMETER) + 1, arg.length()));
			}
			else {
				val[0] = 0;
				val[1] = Integer.parseInt(arg);
			}			
		} catch (NumberFormatException e) {
			System.err.println("Invalid argument '" + arg + "' for" +
					" number of runs");
			System.err.println("The argument must be either a single value, " + 
					"or a range of values (e.g., '2:5'). Note that this " + 
					"option has changed in version 1.3.");
			System.exit(-1);
		}
		
		if (val[0] < 0) {
			System.err.println("Starting run value can't be smaller than 1");
			System.exit(-1);
		}
		if (val[0] >= val[1]) {
			System.err.println("Starting run value can't be bigger than the " + 
					"last run value");
			System.exit(-1);			
		}
				
		return val;
	}
	
	/**
	 * Prints text to stdout
	 * @param txt Text to print
	 */
	private static void print(String txt) {
		System.out.println(txt);
	}
}
