package com.rplbo.app;

public class NLPService {
    public enum IntentType {
        GREETING,
        HELP,
        THANKS,
        STORE_HOURS,
        STORE_LOCATION,
        EXIT,
        ORDER_STATUS,
        RECOMMENDATION,
        PRODUCT_DETAIL,
        PRODUCT_SEARCH,
        COMPARISON,
        UNKNOWN
    }

    public static class AnalysisResult {
        private final IntentType intentType;
        private final String normalizedInput;
        private final String keyword;
        private final String orderNumber;
        private final int budget;
        private final String category;
        private final String style;
        private final boolean strictBudget;
        private final boolean inferredCategory;

        public AnalysisResult(IntentType intentType, String normalizedInput, String keyword, String orderNumber, int budget, String category) {
            this(intentType, normalizedInput, keyword, orderNumber, budget, category, "", false, false);
        }

        public AnalysisResult(
                IntentType intentType,
                String normalizedInput,
                String keyword,
                String orderNumber,
                int budget,
                String category,
                String style,
                boolean strictBudget,
                boolean inferredCategory
        ) {
            this.intentType = intentType;
            this.normalizedInput = normalizedInput;
            this.keyword = keyword;
            this.orderNumber = orderNumber;
            this.budget = budget;
            this.category = category;
            this.style = style;
            this.strictBudget = strictBudget;
            this.inferredCategory = inferredCategory;
        }

        public IntentType getIntentType() {
            return intentType;
        }

        public String getNormalizedInput() {
            return normalizedInput;
        }

        public String getKeyword() {
            return keyword;
        }

        public String getOrderNumber() {
            return orderNumber;
        }

        public int getBudget() {
            return budget;
        }

        public String getCategory() {
            return category;
        }

        public String getStyle() {
            return style;
        }

        public boolean isStrictBudget() {
            return strictBudget;
        }

        public boolean isInferredCategory() {
            return inferredCategory;
        }
    }

    public AnalysisResult analyze(String input) {
        String normalizedInput = normalize(input);
        int budget = extractBudget(normalizedInput);
        String style = extractStyle(normalizedInput);
        String category = extractCategory(normalizedInput);
        boolean inferredCategory = false;
        boolean strictBudget = isStrictBudgetRequest(normalizedInput);

        if (category.isEmpty()) {
            category = inferCategoryFromStyle(style);
            inferredCategory = !category.isEmpty();
        }

        if (normalizedInput.isEmpty()) {
            return result(IntentType.UNKNOWN, normalizedInput, "", "", budget, category, style, strictBudget, inferredCategory);
        }

        if (isOrderStatusIntent(normalizedInput)) {
            return result(IntentType.ORDER_STATUS, normalizedInput, "", extractOrderNumber(normalizedInput), budget, category, style, strictBudget, inferredCategory);
        }

        if (isComparisonIntent(normalizedInput)) {
            return result(IntentType.COMPARISON, normalizedInput, "", "", budget, category, style, strictBudget, inferredCategory);
        }

        if (isRecommendationIntent(normalizedInput, budget, category, style)) {
            return result(IntentType.RECOMMENDATION, normalizedInput, "", "", budget, category, style, strictBudget, inferredCategory);
        }

        if (isProductDetailIntent(normalizedInput)) {
            return result(IntentType.PRODUCT_DETAIL, normalizedInput, extractProductKeyword(normalizedInput), "", budget, category, style, strictBudget, inferredCategory);
        }

        if (isProductSearchIntent(normalizedInput)) {
            return result(IntentType.PRODUCT_SEARCH, normalizedInput, extractProductKeyword(normalizedInput), "", budget, category, style, strictBudget, inferredCategory);
        }

        if (containsAny(normalizedInput, "halo", "hai", "hello", "hi", "pagi", "siang", "malam")) {
            return result(IntentType.GREETING, normalizedInput, "", "", budget, category, style, strictBudget, inferredCategory);
        }

        if (containsAny(normalizedInput, "terima kasih", "makasih", "makasi", "thanks", "thx")) {
            return result(IntentType.THANKS, normalizedInput, "", "", budget, category, style, strictBudget, inferredCategory);
        }

        if (containsAny(normalizedInput, "bantuan", "tolong", "menu", "bisa bantu apa", "cara pakai", "panduan", "help")) {
            return result(IntentType.HELP, normalizedInput, "", "", budget, category, style, strictBudget, inferredCategory);
        }

        if (containsAny(normalizedInput, "jam operasional", "jam buka", "jam tutup", "toko buka", "buka jam")) {
            return result(IntentType.STORE_HOURS, normalizedInput, "", "", budget, category, style, strictBudget, inferredCategory);
        }

        if (containsAny(normalizedInput, "lokasi toko", "alamat toko", "dimana toko")) {
            return result(IntentType.STORE_LOCATION, normalizedInput, "", "", budget, category, style, strictBudget, inferredCategory);
        }

        if (containsAny(normalizedInput, "keluar", "bye", "dadah", "sampai jumpa")) {
            return result(IntentType.EXIT, normalizedInput, "", "", budget, category, style, strictBudget, inferredCategory);
        }

        return result(IntentType.UNKNOWN, normalizedInput, "", "", budget, category, style, strictBudget, inferredCategory);
    }

    private AnalysisResult result(
            IntentType intentType,
            String normalizedInput,
            String keyword,
            String orderNumber,
            int budget,
            String category,
            String style,
            boolean strictBudget,
            boolean inferredCategory
    ) {
        return new AnalysisResult(intentType, normalizedInput, keyword, orderNumber, budget, category, style, strictBudget, inferredCategory);
    }

    public String normalize(String input) {
        if (input == null) {
            return "";
        }

        return input.toLowerCase()
                .replace("?", " ")
                .replace("!", " ")
                .replace(".", " ")
                .replace(",", " ")
                .replace(":", " ")
                .replace(";", " ")
                .replace("=", " ")
                .replace("/", " ")
                .replace("\\", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public int extractBudget(String input) {
        String normalized = input == null ? "" : input.replace(".", "").replace(",", "");
        String[] parts = normalized.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            int budget = parseBudgetToken(parts[i]);
            if (budget > 0) {
                return budget;
            }

            if (parts[i].matches("\\d+")) {
                int value = Integer.parseInt(parts[i]);
                if (i + 1 < parts.length && isMillionToken(parts[i + 1])) {
                    return value * 1000000;
                }
                if (i + 1 < parts.length && isThousandToken(parts[i + 1])) {
                    return value * 1000;
                }
                if (value > 1000) {
                    return value;
                }
            }
        }
        return 0;
    }

    public String extractCategory(String input) {
        if (containsAny(input, "akustik", "acoustic", "accoustic")) {
            return "Acoustic Guitar";
        }
        if (containsAny(input, "electric", "elektrik", "listrik", "elec")) {
            return "Electric Guitar";
        }
        if (input != null && input.contains("bass")) {
            return "Bass Guitar";
        }
        if (containsAny(input, "classical", "klasik", "nylon", "nilon")) {
            return "Classical Guitar";
        }
        return "";
    }

    public String extractStyle(String input) {
        if (containsAny(input, "metal", "heavy", "distorsi berat", "keras", "shred")) {
            return "metal";
        }
        if (containsAny(input, "rock", "distorsi", "riff", "solo", "power chord")) {
            return "rock";
        }
        if (containsAny(input, "jazz", "warm", "mellow", "halus", "smooth")) {
            return "jazz";
        }
        if (containsAny(input, "blues", "vintage", "classic", "klasik rock")) {
            return "blues";
        }
        if (containsAny(input, "funk", "funky", "groove")) {
            return "funk";
        }
        if (containsAny(input, "fingerstyle", "finger style", "petikan", "finger picking", "fingerpicking", "strumming", "akustikan")) {
            return "fingerstyle";
        }
        if (containsAny(input, "pop", "clean", "jernih", "versatile", "serbaguna")) {
            return "pop";
        }
        return "";
    }

    public String inferCategoryFromStyle(String style) {
        if ("fingerstyle".equals(style)) {
            return "Acoustic Guitar";
        }
        if (containsAny(style, "rock", "metal", "jazz", "blues", "funk", "pop")) {
            return "Electric Guitar";
        }
        return "";
    }

    public boolean isStrictBudgetRequest(String input) {
        return containsAny(input,
                "dibawah", "di bawah", "maksimal", "max", "kurang dari",
                "tidak lebih dari", "lebih murah", "<");
    }

    private boolean isOrderStatusIntent(String input) {
        return containsAny(input,
                "status pesanan", "status order", "cek pesanan", "cek order",
                "lacak pesanan", "lacak order", "tracking pesanan", "tracking order",
                "nomor pesanan", "nomor order")
                || (containsAny(input, "pesanan", "order") && input.contains("status"));
    }

    private boolean isRecommendationIntent(String input, int budget, String category, String style) {
        boolean hasRecommendationWord = containsAny(input,
                "rekomendasi", "recommend", "sarankan", "saran", "pilihkan",
                "pilih", "cocok", "pemula", "beginner", "terbaik", "untuk belajar");
        boolean hasAlternativeWord = containsAny(input,
                "lainnya", "yang lain", "ada lagi", "rekomendasi lain",
                "pilihan lain", "opsi lain", "alternatif", "selain itu");
        boolean hasBudgetWord = containsAny(input,
                "budget", "anggaran", "harga", "dana", "dibawah", "di bawah",
                "maksimal", "max", "kurang dari", "murah", "terjangkau");
        boolean hasNeedWord = containsAny(input,
                "ingin", "mau", "butuh", "cari", "carikan", "nyari", "saya ingin", "aku ingin");
        boolean hasProductWord = containsAny(input, "gitar", "guitar", "produk");
        boolean hasStyleWord = style != null && !style.isEmpty();
        boolean hasCategoryWord = category != null && !category.isEmpty();

        return hasRecommendationWord
                || hasAlternativeWord
                || hasBudgetWord
                || (hasNeedWord && hasProductWord && (budget > 0 || hasStyleWord || hasCategoryWord))
                || (hasProductWord && hasStyleWord);
    }

    private boolean isProductDetailIntent(String input) {
        return containsAny(input,
                "deskripsi", "detail", "gambar", "foto", "info", "informasi",
                "spesifikasi", "spec", "spek", "review", "ulasan", "jelaskan",
                "ceritakan", "tentang produk", "lihat produk");
    }

    private boolean isProductSearchIntent(String input) {
        return containsAny(input,
                "cari", "carikan", "mencari", "nyari", "stok", "stock", "harga",
                "aku mau", "saya mau", "ingin beli", "mau beli", "beli", "jual",
                "tersedia", "ready", "ada", "punya", "lihat", "tampilkan", "show",
                "butuh gitar", "berapa harga");
    }

    private boolean isComparisonIntent(String input) {
        return containsAny(input, "bandingkan", "perbandingan", "beda", "bedanya", "compare");
    }

    public String extractOrderNumber(String input) {
        String cleaned = input == null ? "" : input
                .replace("cek", " ")
                .replace("lacak", " ")
                .replace("status", " ")
                .replace("pesanan", " ")
                .replace("order_number", " ")
                .replace("ordernumber", " ")
                .replace("nomor_pesanan", " ")
                .replace("nomor", " ")
                .replace("order", " ")
                .replace("tracking", " ")
                .replace("no", " ")
                .replaceAll("[^a-z0-9_-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.isEmpty()) {
            return "";
        }

        String[] parts = cleaned.split("\\s+");
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i].replaceAll("^[^a-z0-9]+|[^a-z0-9]+$", "");
            if (part.matches(".*\\d.*") && part.matches("[a-z0-9][a-z0-9_-]*")) {
                return part.toUpperCase();
            }
        }
        return cleaned.toUpperCase();
    }

    public String extractProductKeyword(String input) {
        String cleaned = input == null ? "" : input
                .replace("berapa harga", " ")
                .replace("ada gitar", " ")
                .replace("lagi cari", " ")
                .replace("nyari", " ")
                .replace("saya mau", " ")
                .replace("aku mau", " ")
                .replace("ingin beli", " ")
                .replace("mau beli", " ")
                .replace("carikan", " ")
                .replace("mencari", " ")
                .replace("cari", " ")
                .replace("stok", " ")
                .replace("stock", " ")
                .replace("harga", " ")
                .replace("beli", " ")
                .replace("jual", " ")
                .replace("tersedia", " ")
                .replace("ready", " ")
                .replace("lihat", " ")
                .replace("tampilkan", " ")
                .replace("show", " ")
                .replace("deskripsi", " ")
                .replace("detail", " ")
                .replace("gambar", " ")
                .replace("foto", " ")
                .replace("tentang", " ")
                .replace("info", " ")
                .replace("informasi", " ")
                .replace("spesifikasi", " ")
                .replace("spec", " ")
                .replace("spek", " ")
                .replace("review", " ")
                .replace("ulasan", " ")
                .replace("jelaskan", " ")
                .replace("ceritakan", " ")
                .replace("gitar", " ")
                .replace("guitar", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned;
    }

    private int parseBudgetToken(String token) {
        if (token == null) {
            return 0;
        }
        if (token.matches("\\d+jt") || token.matches("\\d+juta")) {
            return Integer.parseInt(token.replace("jt", "").replace("juta", "")) * 1000000;
        }
        if (token.matches("\\d+rb") || token.matches("\\d+ribu")) {
            return Integer.parseInt(token.replace("rb", "").replace("ribu", "")) * 1000;
        }
        if (token.matches("\\d+k")) {
            return Integer.parseInt(token.replace("k", "")) * 1000;
        }
        return 0;
    }

    private boolean isMillionToken(String token) {
        return token != null && (token.startsWith("juta") || "jt".equals(token));
    }

    private boolean isThousandToken(String token) {
        return token != null && (token.startsWith("ribu") || "rb".equals(token));
    }

    private boolean containsAny(String input, String... patterns) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (input.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}
