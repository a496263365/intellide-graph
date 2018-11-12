package cn.edu.pku.sei.intellide.graph.qa.nl_query.NlpInterface.wrapper;

import cn.edu.pku.sei.intellide.graph.qa.nl_query.NlpInterface.entity.NLPToken;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.PropertiesUtils;
import edu.stanford.nlp.util.StringUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class StanfordParser {

    public static Map<String, StanfordParser> instances = new HashMap<>();

    public StanfordCoreNLP pipeline = null;

    public synchronized static StanfordParser getInstance(String languageIdentifier){
        StanfordParser instance = instances.get(languageIdentifier);
        if (instance != null){
            return instance;
        }
        instance = new StanfordParser(languageIdentifier);
        instances.put(languageIdentifier, instance);
        return instance;
    }

    private StanfordParser(String languageIdentifier){
        InputStream in = StanfordParser.class.getResourceAsStream("/nli/corenlp/" +languageIdentifier+".properties");
        String propStr = "";
        try {
            propStr = StringUtils.join(IOUtils.readLines(in, "utf-8"),"\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        pipeline = new StanfordCoreNLP(PropertiesUtils.fromString(propStr));
    }

    public List<NLPToken> runAllAnnotators(String text){
        // create an empty Annotation just with the given text
        if (!(text.endsWith(".")||text.endsWith("?"))) text = text+".";
        Annotation document = new Annotation(text);
        // run all Annotators on this text
        pipeline.annotate(document);
        return parserOutput(document);
    }

    private List<NLPToken> parserOutput(Annotation document){
        List<NLPToken> set = new ArrayList<>();
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        long offset = 0;
        String name = "";
        for(CoreMap sentence: sentences) {
            for (CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);

                if (ne.equals("PERSON")){
                    if (!name.equals("")) name += " ";
                    name += word;
                    continue;
                }
                if (!name.equals("")){
                    NLPToken nlptoken = new NLPToken(name,"NN","PERSON");
                    nlptoken.offset = offset++;
                    set.add(nlptoken);
                    name = "";
                }
                NLPToken nlptoken = new NLPToken(word,pos,ne);
                //System.out.println(pos +" " + word);
                nlptoken.offset = offset++;
                set.add(nlptoken);
            }
        }
        if (!name.equals("")){
            NLPToken nlptoken = new NLPToken(name,"NN","PERSON");
            nlptoken.offset = offset++;
            set.add(nlptoken);
        }
        return set;
    }

}