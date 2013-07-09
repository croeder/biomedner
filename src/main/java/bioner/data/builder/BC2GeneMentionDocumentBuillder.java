package bioner.data.builder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import bioner.data.document.BioNERDocument;
import bioner.data.document.BioNEREntity;
import bioner.data.document.BioNERParagraph;
import bioner.data.document.BioNERSection;
import bioner.data.document.BioNERSentence;
import bioner.global.GlobalConfig;

public class BC2GeneMentionDocumentBuillder implements BioNERDocumentBuilder {

	@Override
	public BioNERDocument[] buildDocuments(String filepath) {
		// TODO Auto-generated method stub
		BioNERDocument[] documents = null;
		try {
			Vector<BioNERDocument> docVector = new Vector<BioNERDocument>();
			BufferedReader freader = new BufferedReader(new FileReader(filepath));
			Hashtable<String, BioNERDocument> docTable = new Hashtable<String, BioNERDocument>();
			String line;
			while((line=freader.readLine()) != null)
			{
				int pos = line.indexOf(' ');
				if(pos>0)
				{
					String id = line.substring(0, pos);
					String sentenceStr = line.substring(pos+1);
					BioNERDocument document = new BioNERDocument();
					document.setID(id);
					BioNERSection section = new BioNERSection();
					BioNERParagraph paragraph = new BioNERParagraph();
					BioNERSentence sentence = new BioNERSentence(sentenceStr, 0);
					paragraph.addSentence(sentence);
					section.addParagraph(paragraph);
					document.setAbstractSection(section);
					docTable.put(id, document);
					docVector.add(document);
				}
			}
			freader.close();
			
			freader = new BufferedReader(new FileReader(filepath+".eval"));
			while((line=freader.readLine()) != null)
			{
				String[] parts = line.split("\\|");
				if(parts.length==3)
				{
					String id = parts[0];
					int pos = parts[1].indexOf(' ');
					String beginStr = parts[1].substring(0, pos);
					String endStr = parts[1].substring(pos+1);
					int begin = Integer.parseInt(beginStr);
					int end = Integer.parseInt(endStr);
					
					
					
					BioNERDocument document = docTable.get(id);
					BioNERSentence sentence = document.getAbstractSentences()[0];
					
					int unspaceNum = 0;
					int spaceNum = 0;
					String sentenceStr = sentence.getSentenceText();
					for(int i=0; unspaceNum<=begin; i++)
					{
						if(sentenceStr.charAt(i)==' ') spaceNum++;
						else unspaceNum++;
					}
					
					begin += spaceNum;
					end += spaceNum+1;
					
					BioNEREntity entity = new BioNEREntity();
					entity.set_Sentence(sentence);
					entity.set_position(begin, end);
					entity.set_Type(GlobalConfig.PROTEIN_TYPE_LABEL);
					sentence.addEntity(entity);
				}
			}
			freader.close();
			
			int size = docVector.size();
			documents = new BioNERDocument[size];
			for(int i=0; i<size; i++)
			{
				documents[i] = docVector.elementAt(i);
			}
					
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return documents;
	}

}