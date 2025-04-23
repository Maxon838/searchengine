package searchengine.lemma;

import lombok.NoArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@NoArgsConstructor
@Component
public class LemmaFinder {

    private static final String[] particlesNames = {"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
    private LuceneMorphology luceneMorph;

    public HashMap<String, Integer> findLemmas (String text) throws IOException
    {

        luceneMorph = new RussianLuceneMorphology();
        HashMap<String, Integer> resultMap = new HashMap<>();

        String[] onlyRussianWords = arrayOfOnlyRussianWords(text);

        for (String word : onlyRussianWords)
        {
            if (word.isBlank()) continue;

            List<String> wordsMorphInfo = luceneMorph.getMorphInfo(word);
            if (anyWordsNormalFormIsParticle(wordsMorphInfo)) continue;

            List<String> wordsNormalForms = luceneMorph.getNormalForms(word);
            if (wordsNormalForms.isEmpty()) continue;

            String normalForm = wordsNormalForms.get(0);

            if (normalForm.equals("да")&&(word.equals("д"))) normalForm = "д";

            if (resultMap.containsKey(normalForm)) resultMap.put(normalForm, resultMap.get(normalForm) + 1);
            else resultMap.put(normalForm, 1);
        }

         return resultMap;
    }

    public static String[] arrayOfOnlyRussianWords(String text)
    {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

    public boolean anyWordsNormalFormIsParticle(List<String> wordsMorphInfo)
    {
        return wordsMorphInfo.stream().anyMatch(morph->isParticle(morph));
    }

    private boolean isParticle(String morph)
    {
        for (String particle : particlesNames)
        {
            if (morph.toUpperCase().contains(particle)) return true;
        }

        return false;
    }
}
