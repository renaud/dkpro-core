/**
 * Copyright 2007-2014
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.tudarmstadt.ukp.dkpro.core.stanfordnlp;

import static org.apache.uima.fit.util.JCasUtil.select;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.V;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.Messages;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import edu.stanford.nlp.ling.WordTag;
import edu.stanford.nlp.process.Morphology;

/**
 * Stanford Lemmatizer component. The Stanford Morphology-class computes the base form of English
 * words, by removing just inflections (not derivational morphology). That is, it only does noun
 * plurals, pronoun case, and verb endings, and not things like comparative adjectives or derived
 * nominals. It is based on a finite-state transducer implemented by John Carroll et al., written in
 * flex and publicly available. See:
 * http://www.informatics.susx.ac.uk/research/nlp/carroll/morph.html
 * 
 * <p>This only works for ENGLISH.</p>
 */
@TypeCapability(
        inputs = {"de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token"},
        outputs = {"de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma"})
public class StanfordLemmatizer
    extends JCasAnnotator_ImplBase
{
    private Morphology morphology;

    @Override
    public void initialize(UimaContext aContext)
        throws ResourceInitializationException
    {
        super.initialize(aContext);

        morphology = new Morphology();
    }

    @Override
    public void process(JCas aJCas)
        throws AnalysisEngineProcessException
    {
        if (!"en".equals(aJCas.getDocumentLanguage())) {
            throw new AnalysisEngineProcessException(Messages.BUNDLE,
                    Messages.ERR_UNSUPPORTED_LANGUAGE, new String[] { aJCas.getDocumentLanguage() });
        }
        
        for (Token t : select(aJCas, Token.class)) {
            // Only verbs are lemmatized, the other words are simply stemmed. This corresponds
            // roughly to what is happening in MorphaAnnotator.
            String token = t.getCoveredText();
            String lemma;
            if (t.getPos() instanceof V) {
                lemma = morphology.lemmatize(new WordTag(token, t.getPos().getPosValue())).lemma();
            }
            else {
                lemma = morphology.stem(token);
            }
            if (lemma == null) {
                lemma = token;
            }
            Lemma l = new Lemma(aJCas, t.getBegin(), t.getEnd());
            l.setValue(lemma);
            l.addToIndexes();
            t.setLemma(l);
        }
    }
}
