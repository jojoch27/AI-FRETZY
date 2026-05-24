package com.rplbo.app;

import junit.framework.TestCase;

import java.util.List;

public class NLPServiceTest extends TestCase {
    public void testDualIntentBudgetAndRockCharacter() {
        NLPService nlpService = new NLPService();

        NLPService.AnalysisResult result = nlpService.analyze("saya ingin gitar budget 4 juta dengan karakter rock");

        assertEquals(NLPService.IntentType.RECOMMENDATION, result.getIntentType());
        assertEquals(4000000, result.getBudget());
        assertEquals("rock", result.getStyle());
        assertEquals("Electric Guitar", result.getCategory());
        assertTrue(result.isInferredCategory());
    }

    public void testStrictBudgetTokenWithSuffix() {
        NLPService nlpService = new NLPService();

        NLPService.AnalysisResult result = nlpService.analyze("rekomendasi gitar elektrik dibawah 4jt");

        assertEquals(NLPService.IntentType.RECOMMENDATION, result.getIntentType());
        assertEquals(4000000, result.getBudget());
        assertEquals("Electric Guitar", result.getCategory());
        assertTrue(result.isStrictBudget());
    }

    public void testMultipleCategoryExtraction() {
        NLPService nlpService = new NLPService();

        NLPService.AnalysisResult result = nlpService.analyze("berikan aku rekomendasi gitar arkustik dan gitar electric");
        List<String> categories = result.getCategories();

        assertEquals(NLPService.IntentType.RECOMMENDATION, result.getIntentType());
        assertEquals("Acoustic Guitar", result.getCategory());
        assertEquals(2, categories.size());
        assertTrue(categories.contains("Acoustic Guitar"));
        assertTrue(categories.contains("Electric Guitar"));
    }

    public void testOrderNumberWithDatabaseStyleLabel() {
        NLPService nlpService = new NLPService();

        NLPService.AnalysisResult result = nlpService.analyze("cek status pesanan order_number=AKB148");

        assertEquals(NLPService.IntentType.ORDER_STATUS, result.getIntentType());
        assertEquals("AKB148", result.getOrderNumber());
    }

    public void testProductDetailKeywordExtraction() {
        NLPService nlpService = new NLPService();

        NLPService.AnalysisResult result = nlpService.analyze("info gitar gibson les paul standard");

        assertEquals(NLPService.IntentType.PRODUCT_DETAIL, result.getIntentType());
        assertEquals("gibson les paul standard", result.getKeyword());
    }
}
