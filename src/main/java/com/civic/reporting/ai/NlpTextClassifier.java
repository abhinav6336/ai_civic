package com.civic.reporting.ai;

import com.civic.reporting.enums.IssueCategory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Probabilistic NLP Text Classifier for civic issue reporting.
 * Uses n-gram tokenization, domain vocabulary weighting, and softmax distribution
 * to predict issue categories and calculate confidence scores.
 */
@Component
public class NlpTextClassifier {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9\\s]");
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren't",
            "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by", "can",
            "cant", "cannot", "could", "couldn't", "did", "didn't", "do", "does", "doesn't", "doing", "don't",
            "down", "during", "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't", "have",
            "haven't", "having", "he", "he'd", "he'll", "he's", "her", "here", "here's", "hers", "herself", "him",
            "himself", "his", "how", "how's", "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't",
            "it", "it's", "its", "itself", "let's", "me", "more", "most", "mustn't", "my", "myself", "no", "nor",
            "not", "of", "off", "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves", "out",
            "over", "own", "same", "shan't", "she", "she'd", "she'll", "she's", "should", "shouldn't", "so", "some",
            "such", "than", "that", "that's", "the", "their", "theirs", "them", "themselves", "then", "there",
            "there's", "these", "they", "they'd", "they'll", "they're", "they've", "this", "those", "through", "to",
            "too", "under", "until", "up", "very", "was", "wasn't", "we", "we'd", "we'll", "we're", "we've", "were",
            "weren't", "what", "what's", "when", "when's", "where", "where's", "which", "while", "who", "who's",
            "whom", "why", "why's", "with", "won't", "would", "wouldn't", "you", "you'd", "you'll", "you're", "you've",
            "your", "yours", "yourself", "yourselves", "please", "problem", "issue", "area", "near", "location"
    );

    // Weighted category lexicons (key phrase -> importance weight)
    private final Map<IssueCategory, Map<String, Double>> categoryLexicons = new EnumMap<>(IssueCategory.class);

    public NlpTextClassifier() {
        initLexicons();
    }

    private void initLexicons() {
        // ROADS
        Map<String, Double> roads = new HashMap<>();
        roads.put("pothole", 4.0);
        roads.put("potholes", 4.0);
        roads.put("road", 3.0);
        roads.put("roads", 3.0);
        roads.put("asphalt", 3.5);
        roads.put("tar", 3.0);
        roads.put("pavement", 3.0);
        roads.put("speedbreaker", 3.5);
        roads.put("speed breaker", 4.0);
        roads.put("crater", 3.5);
        roads.put("craters", 3.5);
        roads.put("sinkhole", 4.0);
        roads.put("cave in", 4.0);
        roads.put("cavein", 3.5);
        roads.put("divider", 3.0);
        roads.put("curb", 2.5);
        roads.put("sidewalk", 3.0);
        roads.put("footpath", 3.0);
        roads.put("traffic signage", 3.0);
        roads.put("zebra crossing", 3.0);
        roads.put("road damage", 4.0);
        roads.put("damaged road", 4.0);
        roads.put("bumpy road", 3.5);
        roads.put("highway", 2.5);
        roads.put("lane", 2.0);
        categoryLexicons.put(IssueCategory.ROADS, roads);

        // ELECTRICITY
        Map<String, Double> elec = new HashMap<>();
        elec.put("streetlight", 4.5);
        elec.put("street light", 4.5);
        elec.put("streetlights", 4.5);
        elec.put("street lights", 4.5);
        elec.put("light", 2.0);
        elec.put("lights", 2.0);
        elec.put("lamp", 3.0);
        elec.put("pole", 3.0);
        elec.put("electric pole", 4.0);
        elec.put("transformer", 4.5);
        elec.put("wire", 3.0);
        elec.put("wires", 3.0);
        elec.put("sparking", 4.5);
        elec.put("sparks", 4.0);
        elec.put("short circuit", 4.5);
        elec.put("blackout", 4.0);
        elec.put("power cut", 4.0);
        elec.put("power outage", 4.0);
        elec.put("dark", 2.0);
        elec.put("flickering", 3.5);
        elec.put("high voltage", 4.0);
        elec.put("live wire", 5.0);
        elec.put("exposed wire", 4.5);
        elec.put("hanging wire", 4.0);
        elec.put("junction box", 3.5);
        elec.put("bulb", 3.0);
        elec.put("electricity", 3.5);
        elec.put("electrical", 3.5);
        categoryLexicons.put(IssueCategory.ELECTRICITY, elec);

        // GARBAGE_SANITATION
        Map<String, Double> garbage = new HashMap<>();
        garbage.put("garbage", 4.5);
        garbage.put("trash", 4.5);
        garbage.put("waste", 4.0);
        garbage.put("dump", 3.5);
        garbage.put("dustbin", 4.0);
        garbage.put("dustbins", 4.0);
        garbage.put("bin", 2.5);
        garbage.put("bins", 2.5);
        garbage.put("rubbish", 4.0);
        garbage.put("debris", 3.0);
        garbage.put("litter", 3.5);
        garbage.put("stench", 3.5);
        garbage.put("smell", 2.5);
        garbage.put("foul smell", 4.0);
        garbage.put("stinking", 3.5);
        garbage.put("odor", 3.0);
        garbage.put("uncollected", 3.5);
        garbage.put("overflowing bin", 4.5);
        garbage.put("garbage dump", 4.5);
        garbage.put("solid waste", 4.0);
        garbage.put("filth", 3.0);
        garbage.put("sweeping", 3.0);
        garbage.put("sanitation", 3.5);
        garbage.put("plastic waste", 3.5);
        categoryLexicons.put(IssueCategory.GARBAGE_SANITATION, garbage);

        // WATER
        Map<String, Double> water = new HashMap<>();
        water.put("water", 3.0);
        water.put("water pipeline", 4.5);
        water.put("pipeline", 3.5);
        water.put("water pipe", 4.5);
        water.put("pipe leak", 4.5);
        water.put("water leak", 4.5);
        water.put("leaking pipe", 4.5);
        water.put("pipe burst", 5.0);
        water.put("burst pipe", 5.0);
        water.put("gushing", 3.5);
        water.put("drinking water", 4.5);
        water.put("water supply", 4.5);
        water.put("tap", 3.0);
        water.put("valve", 3.0);
        water.put("low pressure", 3.5);
        water.put("water shortage", 4.0);
        water.put("contaminated water", 4.5);
        water.put("dirty drinking water", 4.5);
        water.put("water meter", 3.5);
        water.put("pump station", 4.0);
        categoryLexicons.put(IssueCategory.WATER, water);

        // DRAINAGE
        Map<String, Double> drainage = new HashMap<>();
        drainage.put("drain", 4.0);
        drainage.put("drainage", 4.5);
        drainage.put("sewer", 4.5);
        drainage.put("sewerage", 4.5);
        drainage.put("sewage", 4.5);
        drainage.put("manhole", 4.5);
        drainage.put("open manhole", 5.0);
        drainage.put("manhole cover", 4.5);
        drainage.put("gutter", 4.0);
        drainage.put("clogged drain", 4.5);
        drainage.put("drain blockage", 4.5);
        drainage.put("blocked drain", 4.5);
        drainage.put("waterlogging", 4.0);
        drainage.put("flooded", 3.0);
        drainage.put("flooding", 3.0);
        drainage.put("stormwater", 4.0);
        drainage.put("desilting", 4.0);
        drainage.put("culvert", 4.0);
        drainage.put("wastewater", 4.0);
        drainage.put("overflowing drain", 4.5);
        drainage.put("sewage leak", 4.5);
        categoryLexicons.put(IssueCategory.DRAINAGE, drainage);
    }

    /**
     * Classifies raw title and description into the most probable IssueCategory with confidence score.
     */
    public ClassificationOutput classify(String title, String description) {
        String combined = ((title != null ? title : "") + " " + (description != null ? description : "")).trim();
        if (combined.isEmpty()) {
            return new ClassificationOutput(IssueCategory.OTHER, 0.50, Collections.emptyMap());
        }

        String cleaned = cleanText(combined);
        List<String> tokens = tokenize(cleaned);
        Set<String> ngrams = extractNgrams(cleaned, tokens);

        Map<IssueCategory, Double> rawScores = new EnumMap<>(IssueCategory.class);

        for (IssueCategory cat : List.of(IssueCategory.ROADS, IssueCategory.ELECTRICITY, IssueCategory.GARBAGE_SANITATION, IssueCategory.WATER, IssueCategory.DRAINAGE)) {
            Map<String, Double> lexicon = categoryLexicons.get(cat);
            double score = 0.0;
            if (lexicon != null) {
                for (Map.Entry<String, Double> entry : lexicon.entrySet()) {
                    String phrase = entry.getKey();
                    double weight = entry.getValue();
                    if (phrase.contains(" ")) {
                        if (cleaned.contains(phrase)) {
                            score += (weight * 2.0);
                        }
                    } else {
                        if (ngrams.contains(phrase)) {
                            score += weight;
                        }
                    }
                }
            }
            rawScores.put(cat, score);
        }

        // Check if any category has positive score
        double maxScore = rawScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

        if (maxScore <= 0.0) {
            // No matches found
            Map<String, Double> probs = new LinkedHashMap<>();
            probs.put(IssueCategory.OTHER.name(), 1.0);
            return new ClassificationOutput(IssueCategory.OTHER, 0.40, probs);
        }

        // Apply Softmax transformation with temperature scaling for smooth probabilistic distribution
        double temperature = 2.5;
        Map<String, Double> probabilities = new LinkedHashMap<>();
        double expSum = 0.0;
        for (Map.Entry<IssueCategory, Double> entry : rawScores.entrySet()) {
            double exp = Math.exp(entry.getValue() / temperature);
            expSum += exp;
        }

        IssueCategory topCategory = IssueCategory.OTHER;
        double topProb = 0.0;

        for (Map.Entry<IssueCategory, Double> entry : rawScores.entrySet()) {
            double prob = Math.exp(entry.getValue() / temperature) / expSum;
            prob = Math.round(prob * 1000.0) / 1000.0;
            probabilities.put(entry.getKey().name(), prob);
            if (prob > topProb) {
                topProb = prob;
                topCategory = entry.getKey();
            }
        }

        // Bound top confidence between 0.70 and 0.98 if confident, or lower if ambiguous
        double confidence = Math.min(0.98, Math.max(0.65, topProb * 1.15));
        confidence = Math.round(confidence * 100.0) / 100.0;

        return new ClassificationOutput(topCategory, confidence, probabilities);
    }

    private String cleanText(String input) {
        String lower = input.toLowerCase();
        String replaced = NON_ALPHANUMERIC.matcher(lower).replaceAll(" ");
        return MULTIPLE_SPACES.matcher(replaced).replaceAll(" ").trim();
    }

    private List<String> tokenize(String cleaned) {
        String[] parts = cleaned.split(" ");
        List<String> valid = new ArrayList<>();
        for (String p : parts) {
            if (p.length() >= 2 && !STOP_WORDS.contains(p)) {
                valid.add(p);
            }
        }
        return valid;
    }

    private Set<String> extractNgrams(String cleaned, List<String> tokens) {
        Set<String> ngrams = new HashSet<>(tokens);
        // Add bigrams
        for (int i = 0; i < tokens.size() - 1; i++) {
            ngrams.add(tokens.get(i) + " " + tokens.get(i + 1));
        }
        return ngrams;
    }

    public static class ClassificationOutput {
        private final IssueCategory category;
        private final double confidence;
        private final Map<String, Double> probabilities;

        public ClassificationOutput(IssueCategory category, double confidence, Map<String, Double> probabilities) {
            this.category = category;
            this.confidence = confidence;
            this.probabilities = probabilities;
        }

        public IssueCategory getCategory() {
            return category;
        }

        public double getConfidence() {
            return confidence;
        }

        public Map<String, Double> getProbabilities() {
            return probabilities;
        }
    }
}
