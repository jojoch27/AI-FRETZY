package com.rplbo.app;

import junit.framework.TestCase;

import java.util.List;

public class ChatbotTest extends TestCase {
    public void testGreetingStillReturnsOriginalMessage() {
        Chatbot chatbot = new Chatbot(new Database());

        String response = chatbot.receiveInput("halo");

        assertEquals("Halo juga! Ada gitar yang sedang Anda incar hari ini?", response);
    }

    public void testUnknownMessageStillReturnsFallback() {
        Chatbot chatbot = new Chatbot(new Database());

        String response = chatbot.receiveInput("abcdefgh");

        assertEquals("Maaf, saya tidak mengerti. Coba gunakan kata kunci 'cari', 'harga', 'stok', atau 'rekomendasi' diikuti kebutuhan gitarnya.", response);
    }

    public void testThanksGetsHelpfulResponse() {
        Chatbot chatbot = new Chatbot(new Database());

        String response = chatbot.receiveInput("terima kasih ya");

        assertEquals("Sama-sama. Kalau mau, saya bisa bantu carikan gitar berdasarkan brand, kategori, atau budget.", response);
    }

    public void testInfoKeywordGetsProductDescription() {
        Database database = new Database();
        database.setupDatabase();
        Chatbot chatbot = new Chatbot(database);

        String response = chatbot.receiveInput("info gibson les paul standard 60s it");

        assertTrue(response.contains("Deskripsi:"));
        assertTrue(response.contains("Gambar:"));
    }

    public void testProductNameWithoutCommandGetsProductDescription() {
        Database database = new Database();
        database.setupDatabase();
        Chatbot chatbot = new Chatbot(database);

        String response = chatbot.receiveInput("gibson les paul standard 60s it");

        assertTrue(response.contains("Info lengkap gitar:"));
        assertTrue(response.contains("Deskripsi:"));
    }

    public void testElectricRecommendationRespectsBudgetAndCategory() {
        Database database = new Database();
        database.setupDatabase();
        Chatbot chatbot = new Chatbot(database);

        String response = chatbot.receiveInput("saya ingin gitar elektrik dengan budget 5 juta");

        assertTrue(response.contains("Electric Guitar"));
        assertFalse(response.contains("Acoustic Guitar"));
    }

    public void testMultipleCategoryRecommendationIncludesEachRequestedCategory() {
        Database database = new Database();
        database.setupDatabase();
        Chatbot chatbot = new Chatbot(database);

        String response = chatbot.receiveInput("berikan aku rekomendasi gitar arkustik dan gitar electric");

        assertTrue(response.contains("Kategori Acoustic Guitar:"));
        assertTrue(response.contains("Kategori Electric Guitar:"));
        assertTrue(response.indexOf("(Acoustic Guitar)") > response.indexOf("Kategori Acoustic Guitar:"));
        assertTrue(response.indexOf("(Electric Guitar)") > response.indexOf("Kategori Electric Guitar:"));
    }

    public void testMultipleCategoryAndBudgetRecommendationIsGroupedByCategory() {
        Database database = new Database();
        database.setupDatabase();
        Chatbot chatbot = new Chatbot(database);

        String response = chatbot.receiveInput("rekomendasi gitar akustik dan electric harga 5 juta");

        assertTrue(response.contains("Kategori Acoustic Guitar:"));
        assertTrue(response.contains("Kategori Electric Guitar:"));
        assertTrue(response.indexOf("(Acoustic Guitar)") > response.indexOf("Kategori Acoustic Guitar:"));
        assertTrue(response.indexOf("(Electric Guitar)") > response.indexOf("Kategori Electric Guitar:"));
    }

    public void testPriceOnlyRecommendationWorksWithoutCategory() {
        Database database = new Database();
        database.setupDatabase();
        Chatbot chatbot = new Chatbot(database);

        String response = chatbot.receiveInput("rekomendasi gitar harga 5 juta");

        assertTrue(response.contains("Berikut rekomendasi gitar"));
        assertTrue(response.contains("Rp "));
        assertFalse(response.contains("Saya belum menemukan rekomendasi"));
    }

    public void testAllProductsRequestReturnsCatalog() {
        Database database = new Database();
        database.setupDatabase();
        Chatbot chatbot = new Chatbot(database);

        String response = chatbot.receiveInput("tampilkan semua gitar");

        assertTrue(response.startsWith("Daftar semua gitar yang tersedia:"));
        assertTrue(response.contains("Acoustic Guitar"));
        assertTrue(response.contains("Electric Guitar"));
    }

    public void testCharacterInfoQuestionReturnsAvailableCharacters() {
        Chatbot chatbot = new Chatbot(new Database());

        String response = chatbot.receiveInput("karakter gitar apa saja yang ada");

        assertTrue(response.startsWith("Karakter gitar yang bisa saya bantu:"));
        assertTrue(response.contains("Rock"));
        assertTrue(response.contains("Metal"));
        assertTrue(response.contains("Jazz"));
        assertTrue(response.contains("Blues"));
        assertTrue(response.contains("Pop"));
        assertTrue(response.contains("Funk"));
        assertTrue(response.contains("Fingerstyle"));
        assertFalse(response.contains("Saya belum menemukan rekomendasi"));
    }

    public void testDualIntentBudgetAndRockCharacterInfersElectricRecommendation() {
        Database database = new Database();
        database.setupDatabase();
        Chatbot chatbot = new Chatbot(database);

        String response = chatbot.receiveInput("saya ingin gitar budget 4 juta dengan karakter rock");

        assertTrue(response.contains("berkarakter rock"));
        assertTrue(response.contains("Electric Guitar"));
        assertTrue(response.contains("Karakter: Rock"));
        assertFalse(response.contains("Acoustic Guitar"));
    }

    public void testCharacterRecommendationUsesDatabaseCharacterColumn() {
        Database database = new Database();
        database.setupDatabase();
        Chatbot chatbot = new Chatbot(database);

        String response = chatbot.receiveInput("rekomendasi gitar karakter jazz");

        assertTrue(response.contains("berkarakter jazz"));
        assertTrue(response.contains("Karakter: Jazz"));
    }

    public void testRecommendationLainnyaReturnsDifferentProducts() {
        Database database = new Database();
        database.setupDatabase();
        Chatbot chatbot = new Chatbot(database);

        String firstResponse = chatbot.receiveInput("saya ingin gitar budget 4 juta dengan karakter rock");
        String secondResponse = chatbot.receiveInput("rekomendasi lainnya");

        assertTrue(secondResponse.contains("lainnya"));
        assertTrue(secondResponse.contains("Electric Guitar"));
        assertNoRecommendationOverlap(firstResponse, secondResponse);
    }

    public void testFingerstyleCharacterInfersAcousticRecommendation() {
        Database database = new Database();
        database.setupDatabase();
        Chatbot chatbot = new Chatbot(database);

        String response = chatbot.receiveInput("rekomendasi gitar budget 4 juta buat fingerstyle");

        assertTrue(response.contains("berkarakter fingerstyle"));
        assertTrue(response.contains("Acoustic Guitar"));
    }

    public void testAcousticRecommendationDoesNotReturnSingleCutElectricModels() {
        Database database = new Database();
        database.setupDatabase();
        Chatbot chatbot = new Chatbot(database);

        String response = chatbot.receiveInput("rekomendasi gitar akustik budget 3 juta");

        assertFalse(response.contains("Harley Benton SC-"));
    }

    public void testLargeBudgetRecommendationUsesClosestPrices() {
        Database database = new Database();
        database.setupDatabase();
        Chatbot chatbot = new Chatbot(database);

        String response = chatbot.receiveInput("saya ingin gitar elektrik dengan budget 50 juta");

        assertTrue(response.contains("Electric Guitar"));
        assertFalse(response.contains("Epiphone Jerry Cantrell Prophecy LP Cus"));
        assertFalse(response.contains("Rp 1."));
    }

    public void testFollowUpDetailUsesPreviousRecommendationContext() {
        Database database = new Database();
        database.setupDatabase();
        Chatbot chatbot = new Chatbot(database);

        chatbot.receiveInput("rekomendasi gitar elektrik budget 50 juta");
        String response = chatbot.receiveInput("detail nomor 1");

        assertTrue(response.contains("Info lengkap gitar:"));
        assertTrue(response.contains("Electric Guitar"));
        assertTrue(response.contains("Deskripsi:"));
    }

    public void testOrdinalReferenceWithoutListContextGetsFeedback() {
        Database database = new Database();
        database.setupDatabase();
        Chatbot chatbot = new Chatbot(database);

        assertTrue(chatbot.receiveInput("yang kedua").contains("Saya belum punya daftar gitar bernomor"));
        assertTrue(chatbot.receiveInput("gitar ke 2").contains("Saya belum punya daftar gitar bernomor"));
    }

    public void testOrdinalReferenceAfterSingleProductDetailGetsFeedback() {
        Database database = new Database();
        database.setupDatabase();
        Chatbot chatbot = new Chatbot(database);

        chatbot.receiveInput("info gibson les paul standard 60s it");
        String response = chatbot.receiveInput("yang kedua");

        assertTrue(response.contains("Saya belum punya daftar gitar bernomor"));
    }

    public void testOrdinalProductReferenceUsesPreviousRecommendationContext() {
        Database database = new Database();
        database.setupDatabase();
        Chatbot chatbot = new Chatbot(database);

        chatbot.receiveInput("rekomendasi gitar elektrik budget 3 juta");
        String response = chatbot.receiveInput("gitar ke 2");

        assertTrue(response.contains("Info lengkap gitar:"));
        assertTrue(response.contains("Electric Guitar"));
        assertTrue(response.contains("Deskripsi:"));
    }

    public void testFollowUpDetailKeepsOriginalListContext() {
        Database database = new Database();
        database.setupDatabase();
        Chatbot chatbot = new Chatbot(database);

        chatbot.receiveInput("rekomendasi gitar elektrik budget 3 juta");
        String firstResponse = chatbot.receiveInput("yang pertama");
        String secondResponse = chatbot.receiveInput("yang kedua");

        assertTrue(firstResponse.contains("Info lengkap gitar:"));
        assertTrue(secondResponse.contains("Info lengkap gitar:"));
        assertTrue(firstResponse.contains("Electric Guitar"));
        assertTrue(secondResponse.contains("Electric Guitar"));
        assertFalse(firstResponse.equals(secondResponse));
    }

    public void testFollowUpRecommendationReusesPreviousCategory() {
        Database database = new Database();
        database.setupDatabase();
        Chatbot chatbot = new Chatbot(database);

        chatbot.receiveInput("rekomendasi gitar elektrik budget 50 juta");
        String response = chatbot.receiveInput("ada yang lebih murah?");

        assertTrue(response.contains("Electric Guitar"));
        assertFalse(response.contains("Acoustic Guitar"));
    }

    public void testComparisonUsesPreviousSearchResults() {
        Database database = new Database();
        database.setupDatabase();
        Chatbot chatbot = new Chatbot(database);

        chatbot.receiveInput("cari gibson");
        String response = chatbot.receiveInput("bandingkan");

        assertTrue(response.contains("Perbandingan singkat"));
        assertTrue(response.contains("Pilih 'detail nomor 1'"));
    }

    public void testOrderStatusCanBeCheckedByOrderNumber() {
        Database database = new Database();
        database.setupDatabase();
        Chatbot chatbot = new Chatbot(database);

        String response = chatbot.receiveInput("cek pesanan AKB148");

        assertTrue(response.startsWith("Status pesanan AKB148 saat ini: "));
        assertNotNull(database.findOrder("akb148"));
    }

    public void testOrderStatusIgnoresOrderNumberLabel() {
        Database database = new Database();
        database.setupDatabase();
        Chatbot chatbot = new Chatbot(database);

        String response = chatbot.receiveInput("cek status pesanan order_number=AKB148");

        assertTrue(response.startsWith("Status pesanan AKB148 saat ini: "));
    }

    public void testAdminCanAddAndDeleteOrder() {
        Database database = new Database();
        database.setupDatabase();
        List<Product> products = database.getProductsForDashboard(1);
        assertFalse(products.isEmpty());

        String orderNumber = "TEST" + System.currentTimeMillis();
        database.addOrder(orderNumber, products.get(0).getProductId(), "Customer Test", "Pending");

        Order order = database.findOrder(orderNumber.toLowerCase());
        assertNotNull(order);

        database.deleteOrder(order.getOrderId());
        assertNull(database.findOrder(orderNumber));
    }

    private void assertNoRecommendationOverlap(String firstResponse, String secondResponse) {
        String[] firstLines = firstResponse.split("\\n");
        String[] secondLines = secondResponse.split("\\n");
        for (int i = 0; i < firstLines.length; i++) {
            String firstName = extractRecommendationName(firstLines[i]);
            if (firstName.isEmpty()) {
                continue;
            }
            for (int j = 0; j < secondLines.length; j++) {
                assertFalse(firstName.equals(extractRecommendationName(secondLines[j])));
            }
        }
    }

    private String extractRecommendationName(String line) {
        if (!line.matches("\\d+\\. .+")) {
            return "";
        }
        int start = line.indexOf(". ") + 2;
        int end = line.indexOf(" (", start);
        if (end < 0) {
            return "";
        }
        return line.substring(start, end).trim();
    }
}
