/*******************************************************************************
 * Copyright 2010
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package de.tudarmstadt.ukp.dkpro.core.decompounding.uima.annotator;

import static org.uimafit.util.JCasUtil.select;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.descriptor.ExternalResource;
import org.uimafit.descriptor.TypeCapability;
import org.uimafit.util.FSCollectionFactory;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Compound;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.CompoundPart;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.LinkingMorpheme;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Split;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.decompounding.ranking.Ranker;
import de.tudarmstadt.ukp.dkpro.core.decompounding.splitter.DecompoundedWord;
import de.tudarmstadt.ukp.dkpro.core.decompounding.splitter.Fragment;
import de.tudarmstadt.ukp.dkpro.core.decompounding.splitter.SplitterAlgorithm;

@TypeCapability(
      inputs = {"de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token" },
      outputs = {
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Compound",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Split",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.CompoundPart",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.LinkingMorpheme"})

public class CompoundAnnotator
    extends JCasAnnotator_ImplBase
{

    // Splitting algorithm
    public static final String PARAM_SPLITTING_ALGO = "splittingAlgorithm";
    @ExternalResource(key = PARAM_SPLITTING_ALGO)
    private SplitterAlgorithm splitter;

    // Ranking algorithm
    public static final String PARAM_RANKING_ALGO = "rankingAlgorithm";
    @ExternalResource(key = PARAM_RANKING_ALGO)
    private Ranker ranker;

    @Override
    public void initialize(final UimaContext context)
        throws ResourceInitializationException
    {
        super.initialize(context);
    }

    @Override
    public void process(final JCas aJCas)
        throws AnalysisEngineProcessException
    {
        for (Token token : select(aJCas, Token.class)) {
            final String coveredText = token.getCoveredText();
            final DecompoundedWord result = ranker.highestRank(splitter.split(coveredText));
            if (!result.isCompound()) {
                continue;
            }
            final int beginIndex = token.getBegin();
            final Compound compound = new Compound(aJCas, beginIndex, token.getEnd());
            indexSplits(aJCas, result.getSplits(), beginIndex, token.getEnd(), null, compound);
            compound.addToIndexes();

        }
    }

    private void indexSplits(final JCas aJCas, final List<Fragment> splits, final int beginIndex,
            final int tokenEndIndex, final Split parentSplit, final Compound compound)
    {
        if (splits.size() == 1) {
            return;
        }
        final List<Split> splitChildren = new ArrayList<Split>();
        final Fragment element = splits.get(0);
        int endIndex = beginIndex + element.getWord().length();
        final Split split = new CompoundPart(aJCas, beginIndex, endIndex);
        split.addToIndexes();
        splitChildren.add(split);
        int newBeginIndex = endIndex;
        if (element.hasMorpheme()) {
            endIndex = newBeginIndex + element.getMorpheme().length();
            final Split morpheme = new LinkingMorpheme(aJCas, newBeginIndex, endIndex);
            morpheme.addToIndexes();
            splitChildren.add(morpheme);
            newBeginIndex = endIndex;
        }
        final Split remainingSplit = new CompoundPart(aJCas, newBeginIndex, tokenEndIndex);
        splitChildren.add(remainingSplit);
        final FSArray childArray = (FSArray) FSCollectionFactory.createFSArray(aJCas, splitChildren);
        if (parentSplit == null) {
            compound.setSplits(childArray);
        }
        else {
            parentSplit.setSplits(childArray);
        }
        indexSplits(aJCas, splits.subList(1, splits.size()), newBeginIndex, tokenEndIndex,
                remainingSplit, compound);
        remainingSplit.addToIndexes();

    }
}