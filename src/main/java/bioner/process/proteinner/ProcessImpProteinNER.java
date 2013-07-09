//////////////////////////////////////////////////
//Usage: This is a implement for NER process. It contains two main process: tokenize and sentence based NER.
//		These two process is done by two interface so the implement for them can be changed.
//Author: Liu Jingchen
//Date:2009/12/2
//////////////////////////////////////////////////
package bioner.process.proteinner;

import bioner.data.document.BioNERDocument;
import bioner.data.document.BioNEREntity;
import bioner.data.document.BioNERSentence;
import bioner.data.document.BioNERToken;
import bioner.process.BioNERProcess;
import bioner.tools.dictionary.BioNERDictionary;
import bioner.tools.dictner.SentenceNER;
import bioner.tools.dictner.SentenceNERProcessFactory;

public class ProcessImpProteinNER implements BioNERProcess {

	private BioNERDictionary m_proteinDict = ProteinDictionaryBuilder.getProteinDictionary();
	private SentenceNER m_sentencNER = SentenceNERProcessFactory.createSentenceNER(m_proteinDict);
	@Override
	public void Process(BioNERDocument document) {
		// TODO Auto-generated method stub
		ProcessSentenceArray(document.getAllSentence());
	}
	private void ProcessSentenceArray(BioNERSentence[] sentenceArray)
	{
		for(BioNERSentence sentence : sentenceArray)
		{
			BioNERToken[] tokens = sentence.getTokens();
			sentence.clearEntities();
			BioNEREntity[] entities = m_sentencNER.SentenceBasedNER(sentence);
			//TimerSet.PrintOut();
			for(BioNEREntity entity : entities)
			{
				sentence.addEntity(entity);
			}
		}
	}
}