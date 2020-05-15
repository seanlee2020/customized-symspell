package io.github.mightguy.spellcheck.symspell;

import io.github.mightguy.spellcheck.symspell.api.DataHolder;
import io.github.mightguy.spellcheck.symspell.common.*;
import io.github.mightguy.spellcheck.symspell.exception.SpellCheckException;
import io.github.mightguy.spellcheck.symspell.exception.SpellCheckExceptionCode;
import io.github.mightguy.spellcheck.symspell.impl.InMemoryDataHolder;
import io.github.mightguy.spellcheck.symspell.impl.SymSpellCheck;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class SymSpellTest {

    static DataHolder dataHolder;
    static SymSpellCheck symSpellCheck;
    static WeightedDamerauLevenshteinDistance weightedDamerauLevenshteinDistance;


    @BeforeClass
    public static void setup() throws IOException, SpellCheckException {

        ClassLoader classLoader = SymSpellTest.class.getClassLoader();

        SpellCheckSettings spellCheckSettings = SpellCheckSettings.builder()
                .countThreshold(1)
                .deletionWeight(1)
                .insertionWeight(1)
                .replaceWeight(1)
                .maxEditDistance(2)
                .transpositionWeight(1)
                .topK(5)
                .prefixLength(10)
                .verbosity(Verbosity.ALL).build();

        weightedDamerauLevenshteinDistance =
                new WeightedDamerauLevenshteinDistance(spellCheckSettings.getDeletionWeight(),
                        spellCheckSettings.getInsertionWeight(), spellCheckSettings.getReplaceWeight(),
                        spellCheckSettings.getTranspositionWeight(), null);
        dataHolder = new InMemoryDataHolder(spellCheckSettings, new Murmur3HashFunction());

        symSpellCheck = new SymSpellCheck(dataHolder, weightedDamerauLevenshteinDistance,
                spellCheckSettings);
        List<String> result = new ArrayList<>();
        loadUniGramFile(
                new File(classLoader.getResource("frequency_dictionary_en_82_765.txt").getFile()));
        loadBiGramFile(
                // new File(classLoader.getResource("frequency_bigramdictionary_en_243_342.txt").getFile()));
                new File(classLoader.getResource("dictdata/bigram.txt").getFile()));
        loadTriGramFile(
                new File(classLoader.getResource("dictdata/trigram.txt").getFile()));

    }

    private static void loadUniGramFile(File file) throws IOException, SpellCheckException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        while ((line = br.readLine()) != null) {
            String[] arr = line.split("\\s+");
            dataHolder.addItem(new DictionaryItem(arr[0], Double.parseDouble(arr[1]), -1.0));
        }
    }

    private static void loadBiGramFile(File file) throws IOException, SpellCheckException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        while ((line = br.readLine()) != null) {
            String[] arr = line.split("\\s+");
            dataHolder
                    .addItem(new DictionaryItem(arr[0] + " " + arr[1], Double.parseDouble(arr[2]), -1.0));
        }
    }

    private static void loadTriGramFile(File file) throws IOException, SpellCheckException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        while ((line = br.readLine()) != null) {
            String[] arr = line.split("\\s+");
            dataHolder
                    .addItem(new DictionaryItem(arr[0] + " " + arr[1] + " " + arr[2], Double.parseDouble(arr[3]), -1.0));
        }
    }


    @Test
    public void testMultiWordCorrection() throws SpellCheckException {

        assertTypoAndCorrected(symSpellCheck,
                "theq uick brown f ox jumps over the lazy dog",
                "the quick brown fox jumps over the lazy dog",
                2);

        assertTypoAndCorrected(symSpellCheck,
                "Whereis th elove hehaD Dated FOREEVER forImuch of thepast who couqdn'tread in sixthgrade AND ins pired him",
                "where is the love he had dated forever for much of the past who couldn't read in sixth grade and inspired him",
                2);
    }

    @Test
    public void testQueryTypo() throws SpellCheckException {
        String typo = "dariy of a wimpy kid";
        typo = "stephen kiing";
        //String typo = "application lettter for work ";
        // typo = "scipt in ibong adarna";
        int maxEd = 2;
        List<SuggestionItem> suggestionItems = symSpellCheck
                .lookupCompound(typo.toLowerCase().trim(), maxEd);

        System.out.println(suggestionItems);


    }

    @Test
    public void testMultiWordCorrection2() throws SpellCheckException {

        assertTypoAndCorrected(symSpellCheck,
                "Whereis th elove hehaD",
                "where is the love he had",
                2);
    }


    @Test
    public void testSingleWordCorrection() throws SpellCheckException {
        SymSpellTest.assertTypoAndCorrected(symSpellCheck,
                "bigjest", "biggest", 2);
        SymSpellTest.assertTypoAndCorrected(symSpellCheck,
                "playrs", "players", 2);
        SymSpellTest.assertTypoAndCorrected(symSpellCheck,
                "slatew", "slate", 2);
        SymSpellTest.assertTypoAndCorrected(symSpellCheck,
                "ith", "with", 2);
        SymSpellTest.assertTypoAndCorrected(symSpellCheck,
                "plety", "plenty", 2);
        SymSpellTest.assertTypoAndCorrected(symSpellCheck,
                "funn", "fun", 2);
    }

    @Test
    public void testDoubleWordCorrection() throws SpellCheckException {
        String testPhrase = "couqdn'tread".toLowerCase();
        String correctedPhrase = "couldn't read";
        List<SuggestionItem> suggestionItems = symSpellCheck
                .lookupCompound(testPhrase.toLowerCase(), 2);

        Assert.assertTrue(suggestionItems.size() > 0);
        Assert.assertEquals(correctedPhrase.toLowerCase(), suggestionItems.get(0).getTerm().trim());
    }


    @Test
    public void testDoubleComparison() {
        Assert.assertTrue(SpellHelper.isEqualDouble(1.00999, 1, 0.01));
        Assert.assertTrue(SpellHelper.isLessDouble(0.90999, 1, 0.01));
        Assert.assertTrue(SpellHelper.isLessOrEqualDouble(0.7, 1, 0.01));
    }

    @Test(expected = SpellCheckException.class)
    public void testEdgeCases() throws SpellCheckException {
        List<SuggestionItem> suggestionItems = symSpellCheck
                .lookupCompound(null, 2);
        Assert.assertNotNull(suggestionItems);
        SymSpellTest.assertTypoAndCorrected(symSpellCheck,
                "", "with", 2);
        SymSpellTest.assertTypoAndCorrected(symSpellCheck,
                "", "with", 3);
    }

    @Test(expected = SpellCheckException.class)
    public void testEdgeCases2() throws SpellCheckException {
        List<SuggestionItem> suggestionItems = symSpellCheck
                .lookupCompound("tes", 5);
        Assert.assertNotNull(suggestionItems);
        SymSpellTest.assertTypoAndCorrected(symSpellCheck,
                "", "with", 2);
        SymSpellTest.assertTypoAndCorrected(symSpellCheck,
                "", "with", 3);
    }

    @Test(expected = SpellCheckException.class)
    public void testEdgeCases3() throws SpellCheckException {
        List<SuggestionItem> suggestionItems = symSpellCheck
                .lookupCompound("a", 5);
        Assert.assertNotNull(suggestionItems);
        SymSpellTest.assertTypoAndCorrected(symSpellCheck,
                "", "with", 2);
        SymSpellTest.assertTypoAndCorrected(symSpellCheck,
                "", "with", 3);
    }


    @Test
    public void testExceptionCodeCases() {
        try {
            List<SuggestionItem> suggestionItems = symSpellCheck
                    .lookupCompound(null, 2);
            Assert.assertNotNull(suggestionItems);
            SymSpellTest.assertTypoAndCorrected(symSpellCheck,
                    "", "with", 2);
            SymSpellTest.assertTypoAndCorrected(symSpellCheck,
                    "", "with", 3);
        } catch (Exception ex) {
            Assert.assertTrue(ex instanceof SpellCheckException);
            Assert.assertTrue(((SpellCheckException) ex).getCustomMessage().length() > 5);
            Assert.assertTrue(((SpellCheckException) ex).getSpellCheckExceptionCode().equals(
                    SpellCheckExceptionCode.LOOKUP_ERROR));
        }
    }

    @Test
    public void testLookup() throws SpellCheckException {
        List<SuggestionItem> suggestionItems = symSpellCheck.lookup("hel");
        Collections.sort(suggestionItems);
        Assert.assertNotNull(suggestionItems);
        Assert.assertTrue(suggestionItems.size() > 0);
        Assert.assertEquals(78, suggestionItems.size());

        suggestionItems = symSpellCheck.lookup("hel", Verbosity.ALL);
        Assert.assertEquals(78, suggestionItems.size());
    }

    @Test
    public void testLookupCloset() throws SpellCheckException {
        List<SuggestionItem> suggestionItems = symSpellCheck.lookup("resial", Verbosity.CLOSEST);
        Collections.sort(suggestionItems);
        Assert.assertNotNull(suggestionItems);
        Assert.assertTrue(suggestionItems.size() > 0);
        Assert.assertEquals(3, suggestionItems.size());
    }

    public static void assertTypoAndCorrected(SymSpellCheck spellCheck, String typo, String correct,
                                              double maxEd) throws SpellCheckException {
        List<SuggestionItem> suggestionItems = spellCheck
                .lookupCompound(typo.toLowerCase().trim(), maxEd);
        Assert.assertTrue(suggestionItems.size() > 0);
        Assert.assertEquals(correct.toLowerCase().trim(), suggestionItems.get(0).getTerm().trim());
    }

    @Test
    public void testWordBreak() throws Exception {
        Composition suggestionItems = symSpellCheck
                .wordBreakSegmentation("itwasabrightcolddayinaprilandtheclockswerestrikingthirteen", 10,
                        2.0);
        Assert.assertNotNull(suggestionItems);
        Assert.assertEquals("it was bright cold day in april and the clock were striking thirteen",
                suggestionItems.getCorrectedString());
    }


}
