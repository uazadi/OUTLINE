package it.unimib.disco.essere.core;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import it.unimib.disco.essere.experiment.DataExperimenter;
import it.unimib.disco.essere.load.LoaderProperties;

/**
 * The class that receive the input as flag, extract the values of each one of those and 
 * call Handler's method that will contain all the steps for perform the request
 * */

public class InputParser {

	/** The instance used for print out the information, warning and error messages */
	private static final Logger LOGGER = Logger.getLogger(Handler.class.getName());
	
	/** The moment the program starts running */
	private static long startTime = System.currentTimeMillis();
	
	/** An instance of the object that handle prediction */
	private PredictionHandler predHandler;
	
	/** An instance of the object that handle classification */
	private ClassificationHandler classHandler;
	
	/** An instance of the object that handle the dataset */
	private DatasetHandler datasetHandler;

	/** The vector of arguments that specify the request */
	private String[] args;
	
	/**
	 * Where the execution start using the jar
	 * */
	public static void main(String[] args){
		
		InputParser workflow = new InputParser();
		try {	
			workflow.start(args);
		} catch (Exception e) {
			LOGGER.severe("Program fail because: " + e);
			//e.printStackTrace();
			// do nothing, the error message are already print out
		}
		
		long endTime = System.currentTimeMillis();
		String time = String.format("Total time in sec: %1$s", (endTime - startTime)/1000);
		LOGGER.info(time);
		
	}
	
	/**
	 * @return the arguments
	 * */
	public String[] getArgs() {
		return args;
	}

	
	/**
	 * Set the arguments
	 * 
	 * @param args the new arguments
	 */
	public void setArgs(String[] args) {
		this.args = args;
	}

	/**
	 * The method that parse the flag
	 * 
	 * @param args the arguments that have to be parse
	 * */
	public void start(String[] args) throws Exception{
		List<String> input = Arrays.asList(args);
		this.args = args;
		
		if(args.length == 0)
			this.printAvailableFlag();

		if(input.contains("-pred")){
			pred();
		}
		else if(input.contains("-toArff")){
			toArff();
		}
		else if(input.contains("-toCSV")){
			toCSV();
		}
		else{
			if(input.contains("-cross")){
				cross();
			}
			if(input.contains("-wekaExp")){
				wekaExp();
			}
			if(input.contains("-customExp")){
				customExp();
			}
			if(classHandler == null){
				classify();
			}
			if(input.contains("-print")){
				print();
			}
			if(input.contains("-save")){
				save();
			}
			if(input.contains("-ser")){
				ser();
			}
		}

		if(classHandler == null && predHandler == null && datasetHandler == null){
			printAvailableFlag();
		}
	}

	/**
	 * Call the method that will build all the classifiers 
	 * */
	public void classify() throws Exception{
		classHandler = new ClassificationHandler(new LoaderProperties(), args[args.length - 1]);
		classHandler.classify();
	}

	/**
	 * Call the method that will print on screen the summary of all the classifiers 
	 * */
	public void print() throws Exception {
		if(classHandler != null){
			classHandler.printClassifier();
		}else{
			LOGGER.severe("Unable to print the classifiers, no operation specified");
			this.printAvailableFlag();
		}
	}

	/**
	 * Call the method that will save the summary of all the classifiers
	 * */
	public void save() throws Exception {
		if(classHandler != null){
			LOGGER.info("Salving the human-readable description of the classifiers...");
			classHandler.saveClassifier();
		}else{
			LOGGER.severe("Unable to save the classifiers, no operation specified");
			this.printAvailableFlag();
		}
	}

	/**
	 * Call the metohd that will serialize all the classifiers
	 * */
	public void ser() throws Exception {
		if(classHandler != null){
			LOGGER.info("Serializing the classifiers...");
			classHandler.serialize();
		}else{
			LOGGER.severe("Unable to serialize the classifiers, no operation specified");
			this.printAvailableFlag();
		}
	}

	
	/**
	 * Call the method that will perform the prediction
	 * */
	public void pred() throws Exception {
		LOGGER.info("Predicting...");
		
		if(args.length == 2){
			predHandler = new PredictionHandler(new LoaderProperties(), args[args.length - 1]);
			predHandler.predict();
		}
		else{
			predHandler = new PredictionHandler(); 
			
			if(args.length > 3)
				predHandler.predict(args[args.length - 2], args[args.length - 3], args[args.length - 1]);
			else
				predHandler.predict(args[args.length - 1], args[args.length - 2], null);
		}
	}
	
	/**
	 * Call the method that will perform the cross validation
	 * */
	public void cross() throws Exception {
		List<String> input = Arrays.asList(args);
		String path = args[args.length - 1];
		
		LOGGER.info("Crossvalidating...");
		classHandler = new ClassificationHandler(new LoaderProperties(), path);
		int fold = 10;
		int seed = 1;

		if(input.contains("-fold")){
			try{
				int indexFold = input.indexOf("-fold") + 1;
				fold = Integer.parseInt(input.get(indexFold));
				fold = Math.abs(fold);
			}catch(Exception e){
				fold = 10;
				LOGGER.warning(" the number of fold are not correct specified [" + e + "], the default number (10) will be used");
			}
		}
		if(input.contains("-seed")){
			try{
				int indexSeed = input.indexOf("-seed") + 1;
				seed = Integer.parseInt(input.get(indexSeed));
				seed = Math.abs(seed);
			}catch(Exception e){
				seed = 1;
				LOGGER.warning("the number of seed are not correct specified [" + e + "], the default number (1) will be used");
			}
		}

		classHandler.crossValidation(fold, seed);
	}

	/**
	 * Call the method that will perform a custom experiment, which is basically a standard weka experiment where folds are 
	 * created before the execution and then executed concurrently
	 * */
	public void customExp() throws Exception{
		String[] custom = {"-exptype", "custom", "-splittype", "custom", args[args.length - 1]};
		String[] newArgs = new String[args.length + custom.length - 1];
		System.arraycopy(args, 0, newArgs, 0, args.length);
		System.arraycopy(custom, 0, newArgs, args.length - 1, custom.length);
		args = newArgs;
		
		wekaExp();
	}

	/**
	 * Call the method that will perform a standard weka experiment
	 * */
	public void wekaExp() throws Exception{
		List<String> input = Arrays.asList(args);
		String path = args[args.length - 1];
		classHandler = new ClassificationHandler(new LoaderProperties(), path);

		String exptype = DataExperimenter.getDefaultExptype();
		String splittype = DataExperimenter.getDefaultSplittype();
		int folds = DataExperimenter.getDefaultFolds();
		boolean randomized = DataExperimenter.isDefaultRandomized();
		double percentage = DataExperimenter.getDefaultPercentage();
		int runs = DataExperimenter.getDefaultRuns();

		if(input.contains("-exptype")){
			exptype = input.get(input.indexOf("-exptype") + 1);

			if(!DataExperimenter.exptypeCheckValue(exptype)){
				String warning = "the exptype value wasn't valid, the default value will be used (" + DataExperimenter.getExptypeValues()[0] + ")"; 	 
				LOGGER.warning(warning);
				exptype = DataExperimenter.getDefaultExptype();
			}
		}

		if(input.contains("-splittype")){
			splittype = input.get(input.indexOf("-splittype") + 1);

			if(!DataExperimenter.splittypeCheckValue(splittype)){  
				String warning = "the splittype value wasn't valid, the default value will be used (" + DataExperimenter.getDefaultSplittype() + ") ";
				LOGGER.warning(warning);
				splittype = DataExperimenter.getDefaultSplittype();
			}
		}

		try{
			if(input.contains("-folds")){
				folds = Integer.parseInt(input.get(input.indexOf("-folds")+1));
			}	
		}catch(Exception e){
			LOGGER.warning("the fold value wasn't valid [" + e + "], the default value will be used (" 
					+ DataExperimenter.getDefaultFolds()+ ") ");
			folds = DataExperimenter.getDefaultFolds();
		}

		if(input.contains("-randomized")){
			randomized = true;
		}

		try{
			if(input.contains("-percentage")){
				percentage = Double.parseDouble(input.get(input.indexOf("-percentage") + 1));
			}
		}catch(Exception e){
			LOGGER.warning("the percentage value wasn't valid [" + e + "], the default value will be used (" 
					+ DataExperimenter.getDefaultPercentage()+ ") ");
			percentage = DataExperimenter.getDefaultPercentage();
		}

		try{
			if(input.contains("-runs")){
				runs = Integer.parseInt(input.get(input.indexOf("-runs") + 1));
			}
		}catch(Exception e){
			LOGGER.warning("the runs value wasn't valid [" + e + "], the default value will be used (" 
					+ DataExperimenter.getDefaultRuns() + ")");
			runs = DataExperimenter.getDefaultRuns();
		}

		classHandler.wekaExperiment(exptype, splittype, runs, folds, percentage, randomized);
	}
	
	/**
	 * Call the method that will convert a dataset in a .arff file
	 **/ 
	public void toArff() throws Exception{
		List<String> input = Arrays.asList(args);
		
		String path = input.get(input.indexOf("-toArff") + 1);
		datasetHandler = new DatasetHandler(path);
		path = path.substring(0,path.lastIndexOf(".")-1) + ".arff";
		datasetHandler.toArff(path);
	}

	/**
	 * Call the method that will convert a dataset in a .csv file
	 **/ 
	public void toCSV() throws Exception{
		List<String> input = Arrays.asList(args);
		
		String path = input.get(input.indexOf("-toCSV") + 1);
		datasetHandler = new DatasetHandler(path);
		path = path.substring(0,path.lastIndexOf(".")-1) + ".csv";
		datasetHandler.toArff(path);
	}

	/**
	 * Print out the available flag if no even one of them is specified
	 * */
	public void printAvailableFlag() {
		LOGGER.severe("No valid operation selected, please use:\n"
				+"-ser for serialize the classifier specified in the configuaration file\n"
				+"-print for print the human-readable result of classification\n"
				+"-save for save the human-readable result of classification\n"
				+"-pred for predict the class of a new dataset\n"
				+"-cross for using the cross validation for classify and evaluate\n"
				+"-wekaExp for perform an experiment (as intended by weka)\n"
				+"-customExp for perform an experiment (as intended by weka), but customized for make it faster\n"
				+"-toArff for converting a dataset in an .arff file\n"
				+"-toCSV for converting a dataset in a .csv file\n"
				+"\n For more information on how to use it please read the README.md\n");
	}

}
