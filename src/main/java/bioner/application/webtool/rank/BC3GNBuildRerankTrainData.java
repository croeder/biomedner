package bioner.application.webtool.rank;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

import bioner.application.api.BioNERProcessFactory;

import bioner.application.webtool.BC3GNDataFileReader;
import bioner.application.webtool.BC3GNFirstRankFeatureBuilder;
import bioner.application.webtool.BC3GNGeneIDRerankFeatureBuilder;
import bioner.application.webtool.BC3GNProcessFactory;
import bioner.application.webtool.BC3GNTaskRun;

import bioner.data.document.BioNERDocument;
import bioner.data.document.BioNEREntity;
import bioner.global.GlobalConfig;
import bioner.normalization.FirstRankFeatureBuilder;
import bioner.normalization.GeneIDRerankFeatureBuilder;
import bioner.normalization.candidate.CandidateFinder;
import bioner.normalization.data.BioNERCandidate;
import bioner.normalization.rerank.BuildGeneIDVectorMap;
import bioner.normalization.rerank.FilterBySpecies;
import bioner.process.BioNERProcess;

/**
 * copied from bc3gn and chnange the associated classes as in the 
 * import comments above. The initial GoogleCode distro didn't
 * have this class, but the feature sets suggested it should.
 */
public class BC3GNBuildRerankTrainData {

	public static void writerDataFile(String dataDir, String genelistFilename, String candidateTrainDataFilename, 
         String filterFilepath, String outputFilename, int maxNum)
    throws IOException
	{
		GlobalConfig.ReadConfigFile();
		
		HashMap<String, Vector<String>> idTable = getGeneIDTable(genelistFilename);
		BC3GNDataFileReader docBuilder = new BC3GNDataFileReader(dataDir);
		CandidateFinder finder = new CandidateFinder();
		BioNERDocument[] documents = docBuilder.buildDocuments();
		BioNERProcessFactory processFactory = new BC3GNProcessFactory(finder, candidateTrainDataFilename, filterFilepath, outputFilename);
		BioNERProcess[] pipeline = processFactory.buildProcessPipeline();
		
		GeneIDRerankFeatureBuilder rerankFeatureBuilder = new BC3GNGeneIDRerankFeatureBuilder();
		BufferedWriter fwriter = new BufferedWriter(new FileWriter(outputFilename));
		
		String[] fileAttributeHeads = rerankFeatureBuilder.getWekaAttributeFileHead();
		
		fwriter.write("@relation gene_normalization");
		fwriter.newLine();
		fwriter.write("@attribute class {1,0}");
		fwriter.newLine();
		
		for( int i=0; i<fileAttributeHeads.length; i++) {
			fwriter.write(fileAttributeHeads[i]);
			fwriter.newLine();
		}
		
		fwriter.write("@data");
		fwriter.newLine();
		fwriter.newLine();
		
		
		int rank = maxNum;
		Vector<String> goldDocIDVector = new Vector<String>();
		for (int i=0; i<documents.length ; i++) {
			long beginTime = System.currentTimeMillis();
			System.out.print("BC3GNBuldRerankTrainData: Build rerank train data. Processing #"+i+" "+documents[i].getID()+"....");
			BioNERDocument document = documents[i];
			Vector<String> idVector = idTable.get(document.getID());
			goldDocIDVector.add(document.getID());
			if (idVector==null) {
				System.out.println("no gold standard, skipping this document.");
				continue;
			}
			for (int j=0; j<pipeline.length; j++) {
                //System.out.println("BC3GNBuildRerankTrainData starting process # " + j + pipeline[j].getClass().getName() );
				pipeline[j].Process(document);
                //System.out.println("BC3GNBuildRerankTrainData completed process # " + j );
			}
			HashMap<String, Vector<BioNEREntity>> geneIDMap = new HashMap<String, Vector<BioNEREntity>>();
			Vector<BioNERCandidate> geneIDVector = new Vector<BioNERCandidate>();
			BuildGeneIDVectorMap.buildGeneIDVectorMap(documents[i], geneIDMap, geneIDVector, BC3GNTaskRun.rank);
			FilterBySpecies.filter(geneIDVector, documents[i],geneIDMap);
			
			BioNERCandidate[] candidates = new BioNERCandidate[geneIDVector.size()];
			for (int j=0; j<geneIDVector.size(); j++) {
				candidates[j] = geneIDVector.elementAt(j);
			}
			
			int correctNum=0;
			Vector<String> lineVector = new Vector<String>();
			for (BioNERCandidate candidate : candidates) {
				String id = candidate.getRecord().getID();
				if (idVector.contains(id)) correctNum++;
				StringBuffer sb = new StringBuffer();
				if (candidates.length<=0) continue;
				
				if (idVector.contains(id)) {
					sb.append("{0 1");
				}
				else {
					sb.append("{0 0");
				}
				String[] features = rerankFeatureBuilder.getFeatures(document, geneIDMap, candidate);
				for (int k=0; k<features.length; k++) {
					if (!features[k].equals("0") && !features[k].equals("0.0")) {
						sb.append(","+(k+1)+" "+features[k]);
					}
				}
				sb.append("}");
				String line = sb.toString();
				if (!lineVector.contains(line)) {
					lineVector.add(line);
					if (idVector.contains(id)) correctNum++;
				}
			}
					
			
			
			
			fwriter.write("%"+document.getID()+" "+correctNum);
			fwriter.newLine();
			for (String line : lineVector) {
				fwriter.write(line);
				fwriter.newLine();
			}
			fwriter.newLine();
			documents[i]=null;
			long endTime = System.currentTimeMillis();
			long time = endTime - beginTime;
			//System.out.println("Finished! "+time+" ms");
		}

           // TODO: more file paths hard-coded		
		/*GlobalConfig.BC3GN_DATADIR = "../../BC3GN/525_data/";
		File[] files = (new File("../../BC3GN/525_data/")).listFiles();
		idTable = getGeneIDTable("../../BC3GN/TrainingSet2.txt");

		for (int i=0; i<files.length; i++) {
			long beginTime = System.currentTimeMillis();
			BioNERDocument document = docBuilder.getOneDocument(files[i]);
			//if(!document.getID().equals("2883592")) continue;
			System.out.print("Build rerank train data for 523 data. #"+i+" "+document.getID()+"....");
			Vector<String> idVector = idTable.get(document.getID());
			if (idVector==null) {
				//System.out.println("no gold standard.");
				continue;
			}
			if (goldDocIDVector.contains(document.getID())) {
				//System.out.println("in 32 gold standard. Skip.");
				continue;
			}
			for (int j=0; j<pipeline.length; j++) {
				pipeline[j].Process(document);
			}
			HashMap<String, Vector<BioNEREntity>> geneIDMap = new HashMap<String, Vector<BioNEREntity>>();
			Vector<BioNERCandidate> geneIDVector = new Vector<BioNERCandidate>();
			BuildGeneIDVectorMap.buildGeneIDVectorMap(document, geneIDMap, geneIDVector, BC3GNTaskRun.rank);
			FilterBySpecies.filter(geneIDVector, document,geneIDMap);
			
			BioNERCandidate[] candidates = new BioNERCandidate[geneIDVector.size()];
			for (int j=0; j<geneIDVector.size(); j++) {
				candidates[j] = geneIDVector.elementAt(j);
			}
			
			
			
			int correctNum=0;
			Vector<String> lineVector = new Vector<String>();
			for (BioNERCandidate candidate : candidates) {
				String id = candidate.getRecord().getID();
				if (idVector.contains(id)) correctNum++; 
                StringBuffer sb = new StringBuffer();
				if (candidates.length<=0) continue;
				
				if (idVector.contains(id)) {
					sb.append("{0 1");
				}
				else {
					continue;
				}
				String[] features = rerankFeatureBuilder.getFeatures(document, geneIDMap, candidate);
				for (int k=0; k<features.length; k++) {
					if (!features[k].equals("0") && !features[k].equals("0.0")) {
						sb.append(","+(k+1)+" "+features[k]);
					}
				}
				sb.append("}");
				String line = sb.toString();
				if (!lineVector.contains(line)) {
					lineVector.add(line);
					if (idVector.contains(id)) correctNum++;
				}
			}
					
			
			
			
			fwriter.write("%"+document.getID()+" "+correctNum);
			fwriter.newLine();
			for (String line : lineVector) {
				fwriter.write(line);
				fwriter.newLine();
			}
			fwriter.newLine();
			long endTime = System.currentTimeMillis();
			long time = endTime - beginTime;
			System.out.println("Finished! "+time+" ms");
		}*/
		
		
		fwriter.close();
		finder.close();
	}
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String genelistFilename = "../../BC3GN/data/TrainingSet2.txt";
		String dataDir = "../../BC3GN/xmls/";
		String outputFilename = "../../BC3GN/TrainData_10.txt";
		String candidateTrainDataFilename = "../../BC3GN/TrainData_50.txt";
		outputFilename = "../../BC3GN/RerankTrainData.txt";
        String filterFilepath = GlobalConfig.ENTITYFILTER_TABULIST_PATH;

		if (args.length==5) {
			dataDir = args[0];
			genelistFilename = args[1];
			candidateTrainDataFilename = args[2];
            filterFilepath = args[3];
			outputFilename = args[4];
		}
        else  if (args.length != 0) {
            System.out.println("Error. needs 5 args:");
            System.out.println(" datadir, genelist file, train data file, filter file, output file.");
        }


        System.out.println("dataDir " + dataDir);
        System.out.println("genelist" + genelistFilename);
        System.out.println("candidate" + candidateTrainDataFilename);
        System.out.println("outputFilename" + outputFilename);
        System.out.println("entity filter tabulist Filename" + filterFilepath);

		GlobalConfig.BC3GN_DATADIR = dataDir;
		writerDataFile(dataDir, genelistFilename, candidateTrainDataFilename, filterFilepath, outputFilename, 50);
		
	}
	
	public static int haveCorrectID(BioNERCandidate[] candidates, int rank, Vector<String> idVector)
	{
		for (int i=0; i<rank && i<candidates.length; i++) {
			String id= candidates[i].getRecord().getID();
			if (idVector.contains(id)) return i;
		}
		return -1;
	}
	
	
	public static HashMap<String,Vector<String>> getGeneIDTable(String filename)
	{
		HashMap<String,Vector<String>> table = new HashMap<String, Vector<String>>();
		try {
			BufferedReader freader = new BufferedReader(new FileReader(filename));
			String line;
			while((line=freader.readLine()) != null)
			{
				String[] parts = line.split("\\t+");
				if (parts.length<2) continue;
				String docID = parts[0];
				Vector<String> entityVector = table.get(docID);
				if (entityVector==null) {
					entityVector = new Vector<String>();
					table.put(docID, entityVector);
				}
				
				entityVector.add(parts[1]);
				
			}
			freader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return table;
	}
}
