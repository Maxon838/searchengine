package searchengine.lemma;

import lombok.NoArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor
@Component
public class LemmaFinder {

    private static final String[] particlesNames = {"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
    private LuceneMorphology luceneMorph;

    public HashMap<String, Integer> getLemmas(String text) throws IOException
    {

        luceneMorph = new RussianLuceneMorphology();
        HashMap<String, Integer> resultMap = new HashMap<>();
        Pattern pattern = Pattern.compile("[а-яёА-ЯЁ]+");

        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String word = matcher.group().toLowerCase();
            if (word.isBlank()) continue;

            List<String> wordsMorphInfo = luceneMorph.getMorphInfo(word);
            if (anyWordsNormalFormIsParticle(wordsMorphInfo)) continue;

            List<String> wordsNormalForms = luceneMorph.getNormalForms(word);
            if (wordsNormalForms.isEmpty()) continue;

            for (String normalForm : wordsNormalForms)
            {
                if (normalForm.equals("да")&&(word.equals("д"))) normalForm = "д";
                resultMap.merge(normalForm, 1, Integer::sum);
            }
        }

         return resultMap;
    }

    private boolean anyWordsNormalFormIsParticle(List<String> wordsMorphInfo)
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

    public String getSnippet (List<String> lemmasList, String htmlText) throws IOException {

        luceneMorph = new RussianLuceneMorphology();
        String text = "";
        List <WordMatch> matchesList = new ArrayList<>();

        try {
            Document doc = Jsoup.parse(htmlText);
            text = doc.body().text();
        }
        catch (Exception ex) {
            System.out.println("Проблема чтения html-документа: " + ex.getMessage());
        }

        Pattern pattern = Pattern.compile("[а-яёА-ЯЁ]+");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {

            String word = matcher.group().toLowerCase();
            int start = matcher.start();
            int end = matcher.end();

            List<String> wordsMorphInfo = luceneMorph.getMorphInfo(word);
            if (anyWordsNormalFormIsParticle(wordsMorphInfo)) continue;

            List<String> wordsNormalForms = luceneMorph.getNormalForms(word);
            if (wordsNormalForms.isEmpty()) continue;

            if (wordsNormalForms.stream().anyMatch(lemmasList::contains))
                matchesList.add(new WordMatch(word, start, end));
        }
        if (matchesList.isEmpty()) return "";

        WordMatch match;
        if (lemmasList.size() == 1) {
            match = matchesList.get(0);
        }
        else match = getBestMatch(matchesList);

        int snippetStart = Math.max(0, match.start - 90);
        int snippetEnd = Math.min(text.length(), match.end + 90);

        while (snippetStart > 0 && !Character.isWhitespace(text.charAt(snippetStart))) snippetStart--;
        while (snippetEnd < text.length() && !Character.isWhitespace(text.charAt(snippetEnd))) snippetEnd++;

        String snippet = text.substring(snippetStart, snippetEnd).trim();

        return getBoldSnippet(snippet, lemmasList);
    }

    private WordMatch getBestMatch(List <WordMatch> matchesList) {

        if (matchesList == null || matchesList.isEmpty()) return null;

        long maxNeighbors = -1;
        WordMatch bestMatch = null;

        for (WordMatch match : matchesList) {
            long neighbors = matchesList.stream()
                    .filter(m -> Math.abs(match.start - m.start) < 90 && Math.abs(match.start - m.start) > 0)
                    .count();
            if (neighbors > maxNeighbors) {
                maxNeighbors = neighbors;
                bestMatch = match;
            }
        }

        return bestMatch;
    }

    private String getBoldSnippet(String snippet, List<String> lemmasList) {

        if (snippet == null || snippet.trim().isEmpty()) return "";
        String words[] = snippet.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) continue;
            String cleanWord = word.replaceAll("[^а-яА-ЯёЁ]", "").toLowerCase();
            if (cleanWord.isEmpty() || !cleanWord.matches("[а-яА-ЯёЁ]+")) {
                result.append(word).append(" ");
                continue;
            }
            List<String> wordsMorphInfo = luceneMorph.getMorphInfo(cleanWord);

            if (anyWordsNormalFormIsParticle(wordsMorphInfo)) {
                result.append(word).append(" ");
                continue;
            }

            List<String> wordsNormalForms = luceneMorph.getNormalForms(cleanWord);
            if (wordsNormalForms.isEmpty()) continue;

            if (wordsNormalForms.stream().anyMatch(lemmasList::contains)) {
                result.append("<b>").append(word).append("</b>");
            }
            else result.append(word);

            result.append(" ");
        }

        return result.toString().trim();
    }
}
