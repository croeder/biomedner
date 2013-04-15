package bioner.normalization.rerank.feature;

import java.util.HashMap;
import java.util.Vector;

import bioner.data.document.BioNERDocument;
import bioner.data.document.BioNEREntity;
import bioner.normalization.data.BioNERCandidate;
import bioner.normalization.feature.builder.SpeciesEntityStore;
import bioner.normalization.rerank.RerankFeatureBuilder;

public class SpeciesCountRateFeautureBuilder implements RerankFeatureBuilder {

	@Override
	public String extractFeature(BioNERDocument document,
			HashMap<String, Vector<BioNEREntity>> map, BioNERCandidate candidate) {
		// TODO Auto-generated method stub
		String speciesID = candidate.getRecord().getSpeciesID();
		Vector<BioNEREntity> speciesVector= SpeciesEntityStore.getSpeciesEntities(document);
		int num=0;
		for(BioNEREntity speciesEntity : speciesVector)
		{
			if(speciesEntity.hasID(speciesID)) num++;
		}
		if(speciesVector.size()==0) return "0.0";
		return Double.toString((double)num/(double)speciesVector.size());
	}

}