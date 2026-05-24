package com.rplbo.app;

import java.util.ArrayList;
import java.util.List;

public class Chatbot {
    private static final int RECOMMENDATION_LIMIT = 3;
    private static final int CANDIDATE_LIMIT = 25;
    private static final String MISSING_NUMBERED_LIST_CONTEXT_RESPONSE = "Saya belum punya daftar gitar bernomor. Minta rekomendasi atau tampilkan semua gitar dulu, lalu sebutkan 'gitar ke 2'.";
    private final Database database;
    private final NLPService nlpService = new NLPService();
    private final List<KnowledgeBase> knowledgeBaseEntries = new ArrayList<KnowledgeBase>();
    private List<Product> lastProducts = new ArrayList<Product>();
    private final List<Integer> shownRecommendationProductIds = new ArrayList<Integer>();
    private final List<String> shownRecommendationProductNames = new ArrayList<String>();
    private Product focusedProduct = null;
    private String lastCategory = "";
    private List<String> lastCategories = new ArrayList<String>();
    private boolean hasNumberedProductListContext = false;
    private int lastBudget = 0;
    private String lastStyle = "";
    private String lastRecommendationKey = "";
    private String lastResponse = "";

    private static class RecommendationCriteria {
        private int budget;
        private String category = "";
        private List<String> categories = new ArrayList<String>();
        private String style = "";
        private boolean strictBudget;
    }

    //knowledge base
    public Chatbot(Database database) {
        this.database = database;
        knowledgeBaseEntries.add(new KnowledgeBase("halo", "Halo juga! Ada gitar yang sedang Anda incar hari ini?"));
        knowledgeBaseEntries.add(new KnowledgeBase("hai", "Halo juga! Ada gitar yang sedang Anda incar hari ini?"));
        knowledgeBaseEntries.add(new KnowledgeBase("hello", "Halo juga! Ada gitar yang sedang Anda incar hari ini?"));
        knowledgeBaseEntries.add(new KnowledgeBase("hi", "Halo juga! Ada gitar yang sedang Anda incar hari ini?"));
        knowledgeBaseEntries.add(new KnowledgeBase("permisi", "Halo juga! Ada gitar yang sedang Anda incar hari ini?"));
        knowledgeBaseEntries.add(new KnowledgeBase("pagi", "Halo juga! Ada gitar yang sedang Anda incar hari ini?"));
        knowledgeBaseEntries.add(new KnowledgeBase("siapa namamu", "Nama saya Fretzy, asisten virtual toko gitar native ini."));
        knowledgeBaseEntries.add(new KnowledgeBase("nama kamu", "Nama saya Fretzy, asisten virtual toko gitar native ini."));
        knowledgeBaseEntries.add(new KnowledgeBase("kamu siapa", "Nama saya Fretzy, asisten virtual toko gitar native ini."));
        knowledgeBaseEntries.add(new KnowledgeBase("bantuan", "Cara mencari gitar:\nKetik 'cari [nama gitar]'\nKetik 'stok [brand]'\nKetik 'harga [model]'"));
        knowledgeBaseEntries.add(new KnowledgeBase("menu", "Cara mencari gitar:\nKetik 'cari [nama gitar]'\nKetik 'stok [brand]'\nKetik 'harga [model]'"));
        knowledgeBaseEntries.add(new KnowledgeBase("tolong", "Cara mencari gitar:\nKetik 'cari [nama gitar]'\nKetik 'stok [brand]'\nKetik 'harga [model]'"));
        knowledgeBaseEntries.add(new KnowledgeBase("help", "Cara mencari gitar:\nKetik 'cari [nama gitar]'\nKetik 'stok [brand]'\nKetik 'harga [model]'"));
        knowledgeBaseEntries.add(new KnowledgeBase("panduan", "Cara mencari gitar:\nKetik 'cari [nama gitar]'\nKetik 'stok [brand]'\nKetik 'harga [model]'"));
        knowledgeBaseEntries.add(new KnowledgeBase("terima kasih", "Sama-sama. Kalau mau, saya bisa bantu carikan gitar berdasarkan brand, kategori, atau budget."));
        knowledgeBaseEntries.add(new KnowledgeBase("makasih", "Sama-sama. Kalau mau, saya bisa bantu carikan gitar berdasarkan brand, kategori, atau budget."));
        knowledgeBaseEntries.add(new KnowledgeBase("thanks", "Sama-sama. Kalau mau, saya bisa bantu carikan gitar berdasarkan brand, kategori, atau budget."));
        knowledgeBaseEntries.add(new KnowledgeBase("thank you", "Sama-sama. Kalau mau, saya bisa bantu carikan gitar berdasarkan brand, kategori, atau budget."));
        knowledgeBaseEntries.add(new KnowledgeBase("jam operasional", "Jam operasional toko: Senin sampai Sabtu, pukul 09.00 sampai 20.00."));
        knowledgeBaseEntries.add(new KnowledgeBase("jam buka toko", "Jam operasional toko: Senin sampai Sabtu, pukul 09.00 sampai 20.00."));
        knowledgeBaseEntries.add(new KnowledgeBase("toko buka", "Jam operasional toko: Senin sampai Sabtu, pukul 09.00 sampai 20.00."));
        knowledgeBaseEntries.add(new KnowledgeBase("buka jam", "Jam operasional toko: Senin sampai Sabtu, pukul 09.00 sampai 20.00."));
        knowledgeBaseEntries.add(new KnowledgeBase("lokasi toko", "Saat ini saya fokus membantu katalog gitar dan status pesanan. Untuk lokasi toko, silakan cek informasi admin toko."));
        knowledgeBaseEntries.add(new KnowledgeBase("alamat toko", "Saat ini saya fokus membantu katalog gitar dan status pesanan. Untuk lokasi toko, silakan cek informasi admin toko."));
        knowledgeBaseEntries.add(new KnowledgeBase("dimana toko", "Saat ini saya fokus membantu katalog gitar dan status pesanan. Untuk lokasi toko, silakan cek informasi admin toko."));
        knowledgeBaseEntries.add(new KnowledgeBase("jenis gitar", "Jenis gitar yang tersedia: akustik, elektrik, bass, dan klasik. Sebutkan jenisnya untuk mendapat rekomendasi."));
        knowledgeBaseEntries.add(new KnowledgeBase("gitar akustik", "Gitar akustik cocok untuk belajar chord, fingerstyle, dan bermain tanpa amplifier."));
        knowledgeBaseEntries.add(new KnowledgeBase("acoustic guitar", "Gitar akustik cocok untuk belajar chord, fingerstyle, dan bermain tanpa amplifier."));
        knowledgeBaseEntries.add(new KnowledgeBase("gitar elektrik", "Gitar elektrik cocok untuk rock, pop, jazz, dan penggunaan efek suara melalui amplifier."));
        knowledgeBaseEntries.add(new KnowledgeBase("electric guitar", "Gitar elektrik cocok untuk rock, pop, jazz, dan penggunaan efek suara melalui amplifier."));
        knowledgeBaseEntries.add(new KnowledgeBase("gitar bass", "Bass guitar dipakai untuk menjaga ritme dan nada rendah dalam band."));
        knowledgeBaseEntries.add(new KnowledgeBase("gitar klasik", "Gitar klasik memakai senar nylon, nyaman untuk pemula dan musik klasik."));
        knowledgeBaseEntries.add(new KnowledgeBase("classical guitar", "Gitar klasik memakai senar nylon, nyaman untuk pemula dan musik klasik."));
        knowledgeBaseEntries.add(new KnowledgeBase("cara memilih gitar", "Pilih gitar berdasarkan kebutuhan: akustik untuk latihan praktis, elektrik untuk panggung dan efek, bass untuk rhythm section, klasik untuk senar yang lebih lembut."));
        knowledgeBaseEntries.add(new KnowledgeBase("tips memilih gitar", "Pilih gitar berdasarkan kebutuhan: akustik untuk latihan praktis, elektrik untuk panggung dan efek, bass untuk rhythm section, klasik untuk senar yang lebih lembut."));
        knowledgeBaseEntries.add(new KnowledgeBase("perawatan gitar", "Simpan gitar di tempat kering, bersihkan senar setelah dipakai, cek tuning, dan ganti senar jika sudah kusam atau berkarat."));
        knowledgeBaseEntries.add(new KnowledgeBase("merawat gitar", "Simpan gitar di tempat kering, bersihkan senar setelah dipakai, cek tuning, dan ganti senar jika sudah kusam atau berkarat."));
        knowledgeBaseEntries.add(new KnowledgeBase("admin", "Menu admin tersedia di sidebar. Admin harus login dengan akun terdaftar sebelum mengelola produk."));
        knowledgeBaseEntries.add(new KnowledgeBase("keluar", "Terima kasih sudah berkunjung! Sampai jumpa."));
        knowledgeBaseEntries.add(new KnowledgeBase("bye", "Terima kasih sudah berkunjung! Sampai jumpa."));
        knowledgeBaseEntries.add(new KnowledgeBase("dadah", "Terima kasih sudah berkunjung! Sampai jumpa."));
    }
    //penerima input
    public String receiveInput(String input) {
        return processQuestion(input);
    }

    //prosessor
    public String processQuestion(String input) {
        NLPService.AnalysisResult analysis = nlpService.analyze(input);
        String normalizedInput = analysis.getNormalizedInput();
        if (normalizedInput.isEmpty()) {
            lastResponse = "Silakan ketik kebutuhan gitar Anda, misalnya 'rekomendasi akustik 3 juta' atau 'info Gibson Les Paul'.";
            return generateResponse();
        }

        //order number handler
        if (analysis.getIntentType() == NLPService.IntentType.ORDER_STATUS || isOrderStatusRequest(normalizedInput)) {
            String orderNumber = analysis.getOrderNumber().isEmpty()
                    ? nlpService.extractOrderNumber(normalizedInput)
                    : analysis.getOrderNumber();
            if (orderNumber.length() < 2) {
                lastResponse = "Mohon masukkan nomor pesanan yang valid.";
                return generateResponse();
            }

            Order order = database.findOrder(orderNumber);
            lastResponse = order == null
                    ? "Maaf, nomor pesanan tidak ditemukan."
                    : "Status pesanan " + orderNumber + " saat ini: " + order.checkStatus() + ".";
            return generateResponse();
        }

        if (isProductOrdinalReferenceRequest(normalizedInput)) {
            if (!hasNumberedProductListContext) {
                lastResponse = MISSING_NUMBERED_LIST_CONTEXT_RESPONSE;
                return generateResponse();
            }

            Product product = resolveProductFromContext(normalizedInput);
            if (product == null) {
                lastResponse = "Nomor gitar itu tidak ada di daftar terakhir. Coba pilih nomor yang tersedia di list rekomendasi.";
                return generateResponse();
            }

            lastResponse = describeProduct(product);
            focusedProduct = product;
            return generateResponse();
        }

        if (isContextualProductDetailRequest(normalizedInput)) {
            Product product = resolveProductFromContext(normalizedInput);
            if (product == null) {
                lastResponse = "Saya belum punya produk acuan. Coba cari atau minta rekomendasi gitar dulu, lalu sebutkan 'detail nomor 1'.";
                return generateResponse();
            }

            lastResponse = describeProduct(product);
            focusedProduct = product;
            return generateResponse();
        }

        if (analysis.getIntentType() == NLPService.IntentType.COMPARISON || isComparisonRequest(normalizedInput)) {
            lastResponse = buildComparisonResponse();
            return generateResponse();
        }

        if (isAllProductsRequest(normalizedInput)) {
            lastResponse = buildAllProductsResponse();
            return generateResponse();
        }

        if (isGuitarCharacterInfoRequest(normalizedInput)) {
            lastResponse = buildGuitarCharacterInfoResponse();
            return generateResponse();
        }

        if (analysis.getIntentType() == NLPService.IntentType.RECOMMENDATION
                || (analysis.getIntentType() == NLPService.IntentType.UNKNOWN && isRecommendationRequest(normalizedInput))) {
            lastResponse = buildRecommendationResponse(normalizedInput);
            return generateResponse();
        }

        //search handler
        if (analysis.getIntentType() == NLPService.IntentType.PRODUCT_DETAIL || isProductDetailRequest(normalizedInput)) {
            String keyword = analysis.getKeyword().isEmpty()
                    ? cleanProductKeyword(normalizedInput)
                    : analysis.getKeyword();
            if (keyword.length() < 2) {
                lastResponse = "Bisa. Sebutkan nama gitarnya, misalnya 'Gibson Les Paul Standard 60s IT'.";
                return generateResponse();
            }

            lastResponse = describeProduct(keyword);
            return generateResponse();
        }

        if (analysis.getIntentType() == NLPService.IntentType.PRODUCT_SEARCH || isProductSearchRequest(normalizedInput)) {
            String keyword = analysis.getKeyword().isEmpty()
                    ? cleanProductKeyword(normalizedInput)
                    : analysis.getKeyword();
            if (keyword.length() < 2) {
                lastResponse = "Mohon masukkan nama gitar yang lebih spesifik.";
                return generateResponse();
            }

            lastResponse = searchProduct(keyword);
            return generateResponse();
        }

        for (KnowledgeBase knowledgeBase : knowledgeBaseEntries) {
            if (knowledgeBase.searchAnswer(normalizedInput)) {
                lastResponse = knowledgeBase.getResponse();
                return generateResponse();
            }
        }

        lastResponse = describeProductIfKnown(normalizedInput);
        if (!lastResponse.isEmpty()) {
            return generateResponse();
        }

        lastResponse = "Maaf, saya tidak mengerti. Coba gunakan kata kunci 'cari', 'harga', 'stok', atau 'rekomendasi' diikuti kebutuhan gitarnya.";
        return generateResponse();
    }

    //generator respons
    public String generateResponse() {
        return lastResponse;
    }
    //handler cari produk
    public String searchProduct(String keyword) {
        List<Product> products = database.findProducts(keyword);
        if (products.isEmpty()) {
            clearProductContext();
            return "Maaf, gitar '" + keyword + "' tidak ditemukan di database kami.";
        }

        rememberProducts(products);
        hasNumberedProductListContext = true;
        StringBuilder hasil = new StringBuilder("Fretzy menemukan beberapa gitar:\n");
        int nomor = 1;
        for (Product product : products) {
            hasil.append(nomor).append(". ")
                    .append(product.getName())
                    .append(" - Rp ")
                    .append(String.format("%,.0f", product.getPrice()))
                    .append("\n");
            nomor++;
        }
        return hasil.toString();
    }

    private String formatProductList(String intro, List<Product> products) {
        StringBuilder hasil = new StringBuilder(intro).append(":\n");
        int nomor = 1;
        for (Product product : products) {
            hasil.append(nomor).append(". ")
                    .append(product.getName())
                    .append(" (").append(product.getCategory()).append(")")
                    .append(" - Rp ")
                    .append(String.format("%,.0f", product.getPrice()))
                    .append("\n");
            nomor++;
        }
        return hasil.toString();
    }

    private String describeProduct(String keyword) {
        List<Product> products = database.findProducts(keyword);
        if (products.isEmpty()) {
            return "Maaf, detail gitar '" + keyword + "' tidak ditemukan di database kami.";
        }

        Product product = products.get(0);
        rememberProducts(singleProductList(product));
        focusedProduct = product;
        return describeProduct(product);
    }

    private String describeProduct(Product product) {
        return "Info lengkap gitar:\n"
                + "Nama: " + product.getName() + "\n"
                + "Kategori: " + product.getCategory() + "\n"
                + "Harga: Rp " + String.format("%,.0f", product.getPrice()) + "\n"
                + "Deskripsi: " + product.getDescription() + "\n"
                + "Gambar: " + product.getImageUrl();
    }

    private String describeProductIfKnown(String input) {
        if (input.length() < 4 || isSmallTalk(input)) {
            return "";
        }

        List<Product> products = database.findProducts(input);
        if (products.isEmpty()) {
            return "";
        }

        return describeProduct(input);
    }

    private boolean isSmallTalk(String input) {
        return input.contains("apa kabar")
                || input.contains("terima kasih")
                || input.contains("makasih")
                || input.contains("halo")
                || input.contains("hai")
                || input.contains("hello")
                || input.contains("hi")
                || input.contains("pagi")
                || input.contains("siang")
                || input.contains("malam");
    }

    private boolean isProductDetailRequest(String input) {
        return containsAny(input,
                "deskripsi", "detail", "gambar", "foto", "info", "informasi",
                "spesifikasi", "spec", "spek", "review", "ulasan", "jelaskan",
                "ceritakan", "tentang produk", "lihat produk");
    }

    private boolean isContextualProductDetailRequest(String input) {
        return !lastProducts.isEmpty()
                && (containsAny(input, "detail", "deskripsi", "gambar", "foto", "info", "informasi", "spesifikasi", "spek")
                || input.matches(".*\\b(nomor|no)\\s*\\d+\\b.*")
                || isProductOrdinalReferenceRequest(input)
                || containsAny(input, "yang pertama", "yang kedua", "yang ketiga", "produk tadi", "gitar tadi", "yang tadi", "itu"));
    }

    private boolean isProductOrdinalReferenceRequest(String input) {
        return input.matches(".*\\b(gitar|produk|yang)\\s+ke\\s*-?\\s*\\d+\\b.*")
                || input.matches(".*\\b(nomor|no)\\s*\\d+\\b.*")
                || containsAny(input,
                "yang pertama", "yang kedua", "yang ketiga",
                "yang ke satu", "yang ke dua", "yang ke tiga",
                "gitar pertama", "gitar kedua", "gitar ketiga",
                "gitar ke satu", "gitar ke dua", "gitar ke tiga",
                "produk pertama", "produk kedua", "produk ketiga",
                "produk ke satu", "produk ke dua", "produk ke tiga");
    }

    private boolean isComparisonRequest(String input) {
        return containsAny(input, "bandingkan", "perbandingan", "beda", "bedanya", "compare");
    }

    private boolean isProductSearchRequest(String input) {
        return containsAny(input,
                "cari", "carikan", "mencari", "nyari", "stok", "stock", "harga", "haga",
                "aku mau", "saya mau", "ingin beli", "mau beli", "beli", "jual",
                "tersedia", "ready", "ada", "punya", "lihat", "tampilkan", "show",
                "butuh gitar");
    }

    private boolean isOrderStatusRequest(String input) {
        return containsAny(input,
                "status pesanan", "status order", "cek pesanan", "cek order",
                "lacak pesanan", "lacak order", "tracking pesanan", "tracking order",
                "nomor pesanan", "nomor order");
    }

    private String cleanProductKeyword(String input) {
        return input.replace("cari", " ")
                .replace("carikan", " ")
                .replace("mencari", " ")
                .replace("nyari", " ")
                .replace("stok", " ")
                .replace("stock", " ")
                .replace("harga", " ")
                .replace("haga", " ")
                .replace("aku mau", " ")
                .replace("saya mau", " ")
                .replace("ingin beli", " ")
                .replace("mau beli", " ")
                .replace("beli", " ")
                .replace("jual", " ")
                .replace("tersedia", " ")
                .replace("ready", " ")
                .replace("ada", " ")
                .replace("punya", " ")
                .replace("lihat", " ")
                .replace("tampilkan", " ")
                .replace("show", " ")
                .replace("butuh", " ")
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
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isRecommendationRequest(String input) {
        return containsAny(input,
                "rekomendasi", "recommend", "sarankan", "saran", "pilihkan",
                "pilih", "cocok", "pemula", "beginner", "budget", "anggaran", "harga",
                "haga", "dana", "dibawah", "di bawah", "maksimal", "max", "kurang dari",
                "murah", "terjangkau", "terbaik", "untuk belajar", "karakter",
                "tone", "suara", "genre", "rock", "metal", "jazz", "blues",
                "pop", "funk", "fingerstyle", "strumming", "lainnya", "yang lain",
                "ada lagi", "pilihan lain", "opsi lain", "alternatif");
    }

    private boolean isAllProductsRequest(String input) {
        return containsAny(input,
                "tampilkan semua gitar", "lihat semua gitar", "semua gitar",
                "daftar semua gitar", "list semua gitar", "katalog semua gitar",
                "tampilkan semua produk", "lihat semua produk", "semua produk",
                "daftar produk", "daftar gitar", "list gitar", "katalog gitar");
    }

    private boolean isGuitarCharacterInfoRequest(String input) {
        return containsAny(input, "karakter", "tone", "suara", "genre")
                && containsAny(input,
                "apa saja", "apa aja", "yang ada", "tersedia", "pilihan",
                "daftar", "list", "macam", "jenis");
    }

    private String buildGuitarCharacterInfoResponse() {
        return "Karakter gitar yang bisa saya bantu:\n"
                + "1. Rock - cocok untuk riff, solo, dan distorsi sedang.\n"
                + "2. Metal - cocok untuk distorsi berat dan permainan agresif.\n"
                + "3. Jazz - cocok untuk suara warm, mellow, dan halus.\n"
                + "4. Blues - cocok untuk karakter vintage, classic, dan ekspresif.\n"
                + "5. Pop - cocok untuk suara clean, jernih, dan serbaguna.\n"
                + "6. Funk - cocok untuk groove dan permainan ritmis.\n"
                + "7. Fingerstyle - cocok untuk petikan, strumming, dan akustikan.\n"
                + "Contoh: 'rekomendasi gitar karakter rock budget 5 juta'.";
    }

    private String buildAllProductsResponse() {
        List<Product> products = database.getAllProducts();
        if (products.isEmpty()) {
            clearProductContext();
            return "Belum ada gitar yang tersimpan di database.";
        }

        rememberProducts(products);
        hasNumberedProductListContext = true;
        return formatProductList("Daftar semua gitar yang tersedia", products);
    }

    private String buildRecommendationResponse(String input) {
        RecommendationCriteria criteria = extractRecommendationCriteria(input);
        boolean wantsAlternative = isAlternativeRecommendationRequest(input);
        List<Product> products;

        if (criteria.budget > 0 && hasCategoryCriteria(criteria)) {
            if (hasMultipleCategoryCriteria(criteria)) {
                products = findRankedProductsByEachCategory(criteria, wantsAlternative);
                if (!products.isEmpty()) {
                    rememberRecommendationContext(products, criteria);
                    String intro = criteria.strictBudget
                            ? buildRecommendationIntro("Berikut rekomendasi gitar " + buildCategoryPhrase(criteria.categories) + buildStylePhrase(criteria.style) + " sesuai budget maksimal Anda", wantsAlternative)
                            : buildRecommendationIntro("Berikut rekomendasi gitar " + buildCategoryPhrase(criteria.categories) + buildStylePhrase(criteria.style) + " yang paling mendekati budget Anda", wantsAlternative);
                    return formatGroupedRecommendation(intro, products, criteria.categories);
                }

                if (wantsAlternative) {
                    return "Maaf, belum ada rekomendasi lain untuk kriteria itu. Coba ubah budget, kategori, atau karakter suara.";
                }
                return "Maaf, belum ada gitar " + buildCategoryPhrase(criteria.categories) + buildStylePhrase(criteria.style) + " dengan budget sekitar Rp " + String.format("%,.0f", (double) criteria.budget) + ". Coba naikkan budget atau pilih karakter lain.";
            }

            products = criteria.strictBudget
                    ? findProductsByCategoriesAndMaxPrice(criteria.categories, criteria.budget, CANDIDATE_LIMIT)
                    : findProductsByCategoriesNearPrice(criteria.categories, criteria.budget, calculateFlexibleMinBudget(criteria.budget), calculateFlexibleMaxBudget(criteria.budget), CANDIDATE_LIMIT);
            products = rankRecommendations(products, criteria, RECOMMENDATION_LIMIT, wantsAlternative);
            if (!products.isEmpty()) {
                rememberRecommendationContext(products, criteria);
                String intro = criteria.strictBudget
                        ? buildRecommendationIntro("Berikut rekomendasi gitar " + buildCategoryPhrase(criteria.categories) + buildStylePhrase(criteria.style) + " sesuai budget maksimal Anda", wantsAlternative)
                        : buildRecommendationIntro("Berikut rekomendasi gitar " + buildCategoryPhrase(criteria.categories) + buildStylePhrase(criteria.style) + " yang paling mendekati budget Anda", wantsAlternative);
                return formatRecommendation(intro, products);
            }

            if (wantsAlternative) {
                return "Maaf, belum ada rekomendasi lain untuk kriteria itu. Coba ubah budget, kategori, atau karakter suara.";
            }
            return "Maaf, belum ada gitar " + buildCategoryPhrase(criteria.categories) + buildStylePhrase(criteria.style) + " dengan budget sekitar Rp " + String.format("%,.0f", (double) criteria.budget) + ". Coba naikkan budget atau pilih karakter lain.";
        }

        if (criteria.budget > 0) {
            products = rankRecommendations(database.findProductsByMaxPrice(criteria.budget, CANDIDATE_LIMIT), criteria, RECOMMENDATION_LIMIT, wantsAlternative);
            if (!products.isEmpty()) {
                rememberRecommendationContext(products, criteria);
                return formatRecommendation(buildRecommendationIntro("Berikut rekomendasi gitar" + buildStylePhrase(criteria.style) + " sesuai budget Anda", wantsAlternative), products);
            }
            if (wantsAlternative) {
                return "Maaf, belum ada rekomendasi lain untuk kriteria itu. Coba ubah budget, kategori, atau karakter suara.";
            }
        }

        if (hasCategoryCriteria(criteria)) {
            if (hasMultipleCategoryCriteria(criteria)) {
                products = findRankedProductsByEachCategory(criteria, wantsAlternative);
                if (!products.isEmpty()) {
                    rememberRecommendationContext(products, criteria);
                    return formatGroupedRecommendation(buildRecommendationIntro("Berikut rekomendasi gitar kategori " + buildCategoryPhrase(criteria.categories) + buildStylePhrase(criteria.style), wantsAlternative), products, criteria.categories);
                }
                if (wantsAlternative) {
                    return "Maaf, belum ada rekomendasi lain untuk kriteria itu. Coba ubah budget, kategori, atau karakter suara.";
                }
            }

            products = rankRecommendations(findProductsByCategories(criteria.categories, CANDIDATE_LIMIT), criteria, RECOMMENDATION_LIMIT, wantsAlternative);
            if (!products.isEmpty()) {
                rememberRecommendationContext(products, criteria);
                return formatRecommendation(buildRecommendationIntro("Berikut rekomendasi gitar kategori " + buildCategoryPhrase(criteria.categories) + buildStylePhrase(criteria.style), wantsAlternative), products);
            }
            if (wantsAlternative) {
                return "Maaf, belum ada rekomendasi lain untuk kriteria itu. Coba ubah budget, kategori, atau karakter suara.";
            }
        }

        if (input.contains("pemula")) {
            RecommendationCriteria beginnerCriteria = new RecommendationCriteria();
            beginnerCriteria.budget = 3000000;
            beginnerCriteria.category = criteria.category;
            beginnerCriteria.categories = copyCategories(criteria.categories);
            beginnerCriteria.style = criteria.style;
            beginnerCriteria.strictBudget = true;
            products = hasCategoryCriteria(beginnerCriteria)
                    ? findProductsByCategoriesAndMaxPrice(beginnerCriteria.categories, beginnerCriteria.budget, CANDIDATE_LIMIT)
                    : database.findProductsByMaxPrice(3000000, CANDIDATE_LIMIT);
            products = rankRecommendations(products, beginnerCriteria, RECOMMENDATION_LIMIT, wantsAlternative);
            if (!products.isEmpty()) {
                rememberRecommendationContext(products, beginnerCriteria);
                return formatRecommendation(buildRecommendationIntro("Untuk pemula, saya sarankan model yang lebih ramah budget seperti berikut", wantsAlternative), products);
            }
            if (wantsAlternative) {
                return "Maaf, belum ada rekomendasi lain untuk kriteria itu. Coba ubah budget, kategori, atau karakter suara.";
            }
        }

        return "Saya belum menemukan rekomendasi yang pas dari permintaan itu. Coba sebutkan kategori seperti akustik, electric, bass, atau budget misalnya 'dibawah 3 juta'.";
    }

    private RecommendationCriteria extractRecommendationCriteria(String input) {
        NLPService.AnalysisResult analysis = nlpService.analyze(input);
        RecommendationCriteria criteria = new RecommendationCriteria();
        criteria.budget = analysis.getBudget();
        criteria.categories = analysis.getCategories();
        criteria.category = firstCategory(criteria.categories);
        criteria.style = analysis.getStyle();
        criteria.strictBudget = analysis.isStrictBudget();

        if (criteria.categories.isEmpty()) {
            addUniqueCategory(criteria.categories, inferCategoryFromStyle(criteria.style));
            criteria.category = firstCategory(criteria.categories);
        }
        if (criteria.budget == 0 && containsAny(input, "lebih murah", "yang murah", "murahan") && !lastProducts.isEmpty()) {
            criteria.budget = Math.max(1, findLowestContextPrice() - 1);
            criteria.strictBudget = true;
        }
        if (criteria.budget == 0 && lastBudget > 0 && isContextFollowUp(input)) {
            criteria.budget = lastBudget;
        }
        if (criteria.categories.isEmpty() && !lastCategories.isEmpty() && isContextFollowUp(input)) {
            criteria.categories = copyCategories(lastCategories);
            criteria.category = firstCategory(criteria.categories);
        } else if (criteria.category.isEmpty() && !lastCategory.isEmpty() && isContextFollowUp(input)) {
            addUniqueCategory(criteria.categories, lastCategory);
            criteria.category = firstCategory(criteria.categories);
        }
        if (criteria.style.isEmpty() && !lastStyle.isEmpty() && isContextFollowUp(input)) {
            criteria.style = lastStyle;
        }
        return criteria;
    }

    private String buildStylePhrase(String style) {
        return style == null || style.isEmpty() ? "" : " berkarakter " + style;
    }

    private boolean isAlternativeRecommendationRequest(String input) {
        return isContextFollowUp(input)
                && containsAny(input,
                "lainnya", "yang lain", "ada lagi", "rekomendasi lain",
                "pilihan lain", "opsi lain", "alternatif", "selain itu");
    }

    private String buildRecommendationIntro(String intro, boolean wantsAlternative) {
        return wantsAlternative ? intro + " lainnya" : intro;
    }

    private boolean hasCategoryCriteria(RecommendationCriteria criteria) {
        return criteria != null && criteria.categories != null && !criteria.categories.isEmpty();
    }

    private boolean hasMultipleCategoryCriteria(RecommendationCriteria criteria) {
        return hasCategoryCriteria(criteria) && criteria.categories.size() > 1;
    }

    private String buildCategoryPhrase(List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return "semua kategori";
        }
        if (categories.size() == 1) {
            return categories.get(0);
        }
        if (categories.size() == 2) {
            return categories.get(0) + " dan " + categories.get(1);
        }

        StringBuilder phrase = new StringBuilder();
        for (int i = 0; i < categories.size(); i++) {
            if (i > 0) {
                phrase.append(i == categories.size() - 1 ? ", dan " : ", ");
            }
            phrase.append(categories.get(i));
        }
        return phrase.toString();
    }

    private List<Product> findProductsByCategories(List<String> categories, int limit) {
        List<Product> products = new ArrayList<Product>();
        for (String category : categories) {
            addUniqueProducts(products, database.findProductsByCategory(category, limit));
        }
        return products;
    }

    private List<Product> findProductsByCategoriesAndMaxPrice(List<String> categories, int maxPriceIdr, int limit) {
        List<Product> products = new ArrayList<Product>();
        for (String category : categories) {
            addUniqueProducts(products, database.findProductsByCategoryAndMaxPrice(category, maxPriceIdr, limit));
        }
        return products;
    }

    private List<Product> findProductsByCategoriesNearPrice(List<String> categories, int targetPriceIdr, int minPriceIdr, int maxPriceIdr, int limit) {
        List<Product> products = new ArrayList<Product>();
        for (String category : categories) {
            List<Product> categoryProducts = database.findProductsByCategoryNearPrice(category, targetPriceIdr, minPriceIdr, maxPriceIdr, limit);
            if (categoryProducts.isEmpty()) {
                categoryProducts = database.findProductsByCategoryAndMaxPrice(category, targetPriceIdr, limit);
            }
            addUniqueProducts(products, categoryProducts);
        }
        return products;
    }

    private void addUniqueProducts(List<Product> target, List<Product> source) {
        if (source == null) {
            return;
        }
        for (Product product : source) {
            if (!containsProduct(target, product)) {
                target.add(product);
            }
        }
    }

    private boolean containsProduct(List<Product> products, Product target) {
        for (Product product : products) {
            if (product.getProductId() == target.getProductId()
                    || normalizeProductName(product).equals(normalizeProductName(target))) {
                return true;
            }
        }
        return false;
    }

    private List<Product> findRankedProductsByEachCategory(RecommendationCriteria criteria, boolean wantsAlternative) {
        List<Product> products = new ArrayList<Product>();
        for (String category : criteria.categories) {
            RecommendationCriteria categoryCriteria = copyCriteriaForCategory(criteria, category);
            addUniqueProducts(products, rankRecommendations(
                    findProductsForCategoryCriteria(categoryCriteria),
                    categoryCriteria,
                    RECOMMENDATION_LIMIT,
                    wantsAlternative
            ));
        }
        return products;
    }

    private RecommendationCriteria copyCriteriaForCategory(RecommendationCriteria criteria, String category) {
        RecommendationCriteria categoryCriteria = new RecommendationCriteria();
        categoryCriteria.budget = criteria.budget;
        categoryCriteria.category = category;
        addUniqueCategory(categoryCriteria.categories, category);
        categoryCriteria.style = criteria.style;
        categoryCriteria.strictBudget = criteria.strictBudget;
        return categoryCriteria;
    }

    private List<Product> findProductsForCategoryCriteria(RecommendationCriteria criteria) {
        if (criteria.budget > 0) {
            if (criteria.strictBudget) {
                return database.findProductsByCategoryAndMaxPrice(criteria.category, criteria.budget, CANDIDATE_LIMIT);
            }
            List<Product> products = database.findProductsByCategoryNearPrice(criteria.category, criteria.budget, calculateFlexibleMinBudget(criteria.budget), calculateFlexibleMaxBudget(criteria.budget), CANDIDATE_LIMIT);
            return products.isEmpty()
                    ? database.findProductsByCategoryAndMaxPrice(criteria.category, criteria.budget, CANDIDATE_LIMIT)
                    : products;
        }
        return database.findProductsByCategory(criteria.category, CANDIDATE_LIMIT);
    }

    private List<Product> rankRecommendations(List<Product> candidates, RecommendationCriteria criteria, int limit) {
        return rankRecommendations(candidates, criteria, limit, false);
    }

    private List<Product> rankRecommendations(List<Product> candidates, RecommendationCriteria criteria, int limit, boolean excludeShown) {
        List<Product> ranked = sortByStyleScore(candidates, criteria);
        if (excludeShown) {
            ranked = removeShownRecommendations(ranked);
        }
        return diversifyRecommendations(ranked, criteria, limit);
    }

    private List<Product> removeShownRecommendations(List<Product> candidates) {
        List<Product> filtered = new ArrayList<Product>();
        for (Product product : candidates) {
            if (!shownRecommendationProductIds.contains(Integer.valueOf(product.getProductId()))
                    && !shownRecommendationProductNames.contains(normalizeProductName(product))) {
                filtered.add(product);
            }
        }
        return filtered;
    }

    private List<Product> sortByStyleScore(List<Product> candidates, RecommendationCriteria criteria) {
        List<Product> ranked = new ArrayList<Product>();
        if (candidates == null) {
            return ranked;
        }
        ranked.addAll(candidates);
        for (int i = 1; i < ranked.size(); i++) {
            Product current = ranked.get(i);
            int currentScore = calculateRecommendationScore(current, criteria);
            int j = i - 1;
            while (j >= 0 && calculateRecommendationScore(ranked.get(j), criteria) < currentScore) {
                ranked.set(j + 1, ranked.get(j));
                j--;
            }
            ranked.set(j + 1, current);
        }
        return ranked;
    }

    private int calculateRecommendationScore(Product product, RecommendationCriteria criteria) {
        if (product == null || criteria == null) {
            return 0;
        }

        String text = (safeLower(product.getBrand()) + " " + safeLower(product.getTitle()) + " " + safeLower(product.getCategory()));
        String style = criteria.style;
        int score = 0;
        if (style != null && !style.isEmpty()) {
            if ("rock".equals(style)) {
                score += scoreText(text, "les paul", "lp", "sc-", "single cut", "sg", "monarkh", "ec-", "jet", "custom", "humbucker", "hh");
            } else if ("metal".equals(style)) {
                score += scoreText(text, "metal", "fr", "floyd", "emg", "active", "prophecy", "monarkh", "ec-", "black", "jackson", "esp", "solar");
            } else if ("jazz".equals(style)) {
                score += scoreText(text, "jazz", "semi", "hollow", "sheraton", "casino", "es-", "gretsch", "archtop", "dot");
            } else if ("blues".equals(style)) {
                score += scoreText(text, "strat", "tele", "les paul", "lp", "p90", "vintage", "classic", "gretsch");
            } else if ("pop".equals(style)) {
                score += scoreText(text, "strat", "tele", "pacifica", "yamaha", "classic", "standard");
            } else if ("funk".equals(style)) {
                score += scoreText(text, "strat", "tele", "single coil", "pacifica", "yamaha");
            } else if ("fingerstyle".equals(style)) {
                score += scoreText(text, "acoustic", "solid top", "fg800", "dreadnought", "concert", "travel");
            }
        }

        if (matchesAnyCategory(product, criteria.categories)) {
            score += 2;
        }
        return score;
    }

    private int scoreText(String text, String... keywords) {
        int score = 0;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                score += 3;
            }
        }
        return score;
    }

    private String safeLower(String text) {
        return text == null ? "" : text.toLowerCase();
    }

    private Product findFirstProductByCategory(List<Product> products, String category, List<Product> excludedProducts) {
        for (Product product : products) {
            if (matchesCategory(product, category) && !excludedProducts.contains(product)) {
                return product;
            }
        }
        return null;
    }

    private boolean matchesAnyCategory(Product product, List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return false;
        }
        for (String category : categories) {
            if (matchesCategory(product, category)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesCategory(Product product, String category) {
        return product != null
                && product.getCategory() != null
                && category != null
                && product.getCategory().equalsIgnoreCase(category);
    }

    private List<Product> diversifyRecommendations(List<Product> candidates, RecommendationCriteria criteria, int limit) {
        List<Product> selected = new ArrayList<Product>();
        if (candidates == null || candidates.isEmpty() || limit <= 0) {
            return selected;
        }

        if (criteria != null && criteria.categories.size() > 1) {
            for (String category : criteria.categories) {
                Product categoryProduct = findFirstProductByCategory(candidates, category, selected);
                if (categoryProduct != null) {
                    selected.add(categoryProduct);
                }
                if (selected.size() == limit) {
                    return selected;
                }
            }
        }

        List<String> selectedBrands = new ArrayList<String>();
        for (Product product : selected) {
            String brand = product.getBrand() == null ? "" : product.getBrand().toLowerCase().trim();
            if (!selectedBrands.contains(brand)) {
                selectedBrands.add(brand);
            }
        }
        for (Product product : candidates) {
            String brand = product.getBrand() == null ? "" : product.getBrand().toLowerCase().trim();
            if (!selected.contains(product) && !selectedBrands.contains(brand)) {
                selected.add(product);
                selectedBrands.add(brand);
            }
            if (selected.size() == limit) {
                return selected;
            }
        }

        for (Product product : candidates) {
            if (!selected.contains(product)) {
                selected.add(product);
            }
            if (selected.size() == limit) {
                return selected;
            }
        }
        return selected;
    }

    private boolean isStrictBudgetRequest(String input) {
        return input.contains("dibawah")
                || input.contains("di bawah")
                || input.contains("maksimal")
                || input.contains("max")
                || input.contains("kurang dari")
                || input.contains("tidak lebih dari")
                || input.contains("lebih murah")
                || input.contains("<");
    }

    private int calculateFlexibleMaxBudget(int budget) {
        return (int) Math.round(budget * 1.2);
    }

    private int calculateFlexibleMinBudget(int budget) {
        return (int) Math.round(budget * 0.8);
    }

    private String formatRecommendation(String intro, List<Product> products) {
        StringBuilder hasil = new StringBuilder(intro).append(":\n");
        int nomor = 1;
        for (Product product : products) {
            hasil.append(nomor).append(". ")
                    .append(product.getName())
                    .append(" (").append(product.getCategory()).append(")")
                    .append(" - Rp ")
                    .append(String.format("%,.0f", product.getPrice()))
                    .append("\n");
            nomor++;
        }
        return hasil.toString();
    }

    private String formatGroupedRecommendation(String intro, List<Product> products, List<String> categories) {
        StringBuilder hasil = new StringBuilder(intro).append(":\n");
        int nomor = 1;
        for (String category : categories) {
            hasil.append("Kategori ").append(category).append(":\n");
            int categoryStartNumber = nomor;
            for (Product product : products) {
                if (!matchesCategory(product, category)) {
                    continue;
                }
                hasil.append(nomor).append(". ")
                        .append(product.getName())
                        .append(" (").append(product.getCategory()).append(")")
                        .append(" - Rp ")
                        .append(String.format("%,.0f", product.getPrice()))
                        .append("\n");
                nomor++;
            }
            if (categoryStartNumber == nomor) {
                hasil.append("- Belum ada rekomendasi tersedia.\n");
            }
        }
        return hasil.toString();
    }

    private String buildComparisonResponse() {
        if (lastProducts.size() < 2) {
            return "Saya butuh minimal dua produk untuk dibandingkan. Coba cari atau minta rekomendasi gitar dulu.";
        }

        StringBuilder hasil = new StringBuilder("Perbandingan singkat dari pilihan terakhir:\n");
        int limit = Math.min(3, lastProducts.size());
        for (int i = 0; i < limit; i++) {
            Product product = lastProducts.get(i);
            hasil.append(i + 1).append(". ")
                    .append(product.getName())
                    .append(" - ").append(product.getCategory())
                    .append(", Rp ").append(String.format("%,.0f", product.getPrice()))
                    .append("\n");
        }
        hasil.append("Pilih 'detail nomor 1' untuk melihat deskripsi lengkap salah satu produk.");
        return hasil.toString();
    }

    private int extractBudget(String input) {
        String normalized = input.replace(".", "").replace(",", "");
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

    private String extractCategory(String input) {
        if (containsAny(input, "akustik", "arkustik", "acoustic", "accoustic")) {
            return "Acoustic Guitar";
        }
        if (containsAny(input, "electric", "elektrik", "listrik", "elec")) {
            return "Electric Guitar";
        }
        if (input.contains("bass")) {
            return "Bass Guitar";
        }
        if (containsAny(input, "classical", "klasik", "nylon", "nilon")) {
            return "Classical Guitar";
        }
        return "";
    }

    private String extractStyle(String input) {
        if (containsAny(input, "metal", "heavy", "distorsi berat", "keras")) {
            return "metal";
        }
        if (containsAny(input, "rock", "distorsi", "riff", "solo")) {
            return "rock";
        }
        if (containsAny(input, "jazz", "warm", "mellow", "halus")) {
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
        if (containsAny(input, "pop", "clean", "jernih")) {
            return "pop";
        }
        return "";
    }

    private String inferCategoryFromStyle(String style) {
        if ("fingerstyle".equals(style)) {
            return "Acoustic Guitar";
        }
        if (containsAny(style, "rock", "metal", "jazz", "blues", "funk", "pop")) {
            return "Electric Guitar";
        }
        return "";
    }

    private String normalizeInput(String input) {
        return nlpService.normalize(input);
    }

    private String cleanOrderNumber(String input) {
        return input.replace("cek", " ")
                .replace("status", " ")
                .replace("pesanan", " ")
                .replace("order", " ")
                .replace("lacak", " ")
                .replace("tracking", " ")
                .replace("nomor", " ")
                .replace("no", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String extractOrderNumber(String input) {
        String cleanedInput = cleanOrderNumber(input)
                .replace("order_number", " ")
                .replace("ordernumber", " ")
                .replace("nomor_pesanan", " ")
                .replaceAll("[^a-z0-9_-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleanedInput.isEmpty()) {
            return "";
        }

        String[] parts = cleanedInput.split("\\s+");
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i].replaceAll("^[^a-z0-9]+|[^a-z0-9]+$", "");
            if (part.matches(".*\\d.*") && part.matches("[a-z0-9][a-z0-9_-]*")) {
                return part.toUpperCase();
            }
        }
        return cleanedInput.toUpperCase();
    }

    private boolean containsAny(String input, String... phrases) {
        for (String phrase : phrases) {
            if (input.contains(phrase)) {
                return true;
            }
        }
        return false;
    }

    private boolean isContextFollowUp(String input) {
        return !lastProducts.isEmpty()
                && containsAny(input,
                "yang", "itu", "tadi", "lebih murah", "lebih mahal", "dibawah", "di bawah",
                "maksimal", "max", "budget", "anggaran", "dana", "ada lagi", "lainnya");
    }

    private Product resolveProductFromContext(String input) {
        if (lastProducts.isEmpty()) {
            return null;
        }

        int index = extractProductIndex(input);
        if (index >= 0 && index < lastProducts.size()) {
            return lastProducts.get(index);
        }

        if (focusedProduct != null && containsAny(input, "produk tadi", "gitar tadi", "yang tadi", "itu")) {
            return focusedProduct;
        }

        if (lastProducts.size() == 1) {
            return lastProducts.get(0);
        }

        return null;
    }

    private int extractProductIndex(String input) {
        String[] parts = input.split("\\s+");
        for (String part : parts) {
            if (part.matches("\\d+")) {
                return Integer.parseInt(part) - 1;
            }
            if (part.matches("ke-?\\d+")) {
                return Integer.parseInt(part.replace("ke", "").replace("-", "")) - 1;
            }
        }
        if (containsAny(input, "pertama", "satu")) {
            return 0;
        }
        if (containsAny(input, "kedua", "dua")) {
            return 1;
        }
        if (containsAny(input, "ketiga", "tiga")) {
            return 2;
        }
        return -1;
    }

    private List<Product> singleProductList(Product product) {
        List<Product> products = new ArrayList<Product>();
        products.add(product);
        return products;
    }

    private List<String> copyCategories(List<String> categories) {
        return categories == null ? new ArrayList<String>() : new ArrayList<String>(categories);
    }

    private String firstCategory(List<String> categories) {
        return categories == null || categories.isEmpty() ? "" : categories.get(0);
    }

    private void addUniqueCategory(List<String> categories, String category) {
        if (category != null && !category.isEmpty() && !categories.contains(category)) {
            categories.add(category);
        }
    }

    private void rememberProducts(List<Product> products) {
        lastProducts = new ArrayList<Product>(products);
        focusedProduct = products.isEmpty() ? null : products.get(0);
        lastCategories.clear();
        hasNumberedProductListContext = false;
        if (!products.isEmpty()) {
            lastCategory = products.get(0).getCategory();
            for (Product product : products) {
                addUniqueCategory(lastCategories, product.getCategory());
            }
        }
    }

    private void rememberRecommendationContext(List<Product> products, String category, int budget) {
        rememberProducts(products);
        hasNumberedProductListContext = true;
        if (category != null && !category.isEmpty()) {
            lastCategory = category;
            lastCategories.clear();
            addUniqueCategory(lastCategories, category);
        }
        if (budget > 0) {
            lastBudget = budget;
        }
    }

    private void rememberRecommendationContext(List<Product> products, RecommendationCriteria criteria) {
        rememberProducts(products);
        hasNumberedProductListContext = true;
        if (criteria == null) {
            return;
        }
        String recommendationKey = buildRecommendationKey(criteria);
        if (!recommendationKey.equals(lastRecommendationKey)) {
            shownRecommendationProductIds.clear();
            shownRecommendationProductNames.clear();
            lastRecommendationKey = recommendationKey;
        }
        rememberShownRecommendations(products);
        if (hasCategoryCriteria(criteria)) {
            lastCategories = copyCategories(criteria.categories);
            lastCategory = firstCategory(lastCategories);
        } else if (criteria.category != null && !criteria.category.isEmpty()) {
            lastCategory = criteria.category;
            lastCategories.clear();
            addUniqueCategory(lastCategories, criteria.category);
        }
        if (criteria.budget > 0) {
            lastBudget = criteria.budget;
        }
        if (criteria.style != null && !criteria.style.isEmpty()) {
            lastStyle = criteria.style;
        }
    }

    private String buildRecommendationKey(RecommendationCriteria criteria) {
        if (criteria == null) {
            return "";
        }
        return buildCategoryPhrase(criteria.categories) + "|" + criteria.budget + "|" + criteria.style + "|" + criteria.strictBudget;
    }

    private void rememberShownRecommendations(List<Product> products) {
        for (Product product : products) {
            Integer productId = Integer.valueOf(product.getProductId());
            if (!shownRecommendationProductIds.contains(productId)) {
                shownRecommendationProductIds.add(productId);
            }
            String productName = normalizeProductName(product);
            if (!shownRecommendationProductNames.contains(productName)) {
                shownRecommendationProductNames.add(productName);
            }
        }
    }

    private String normalizeProductName(Product product) {
        return product == null || product.getName() == null
                ? ""
                : product.getName().toLowerCase().replaceAll("\\s+", " ").trim();
    }

    private void clearProductContext() {
        lastProducts.clear();
        lastCategories.clear();
        hasNumberedProductListContext = false;
        focusedProduct = null;
    }

    private int findLowestContextPrice() {
        int lowestPrice = Integer.MAX_VALUE;
        for (Product product : lastProducts) {
            lowestPrice = Math.min(lowestPrice, (int) product.getPrice());
        }
        return lowestPrice == Integer.MAX_VALUE ? 0 : lowestPrice;
    }

    private int parseBudgetToken(String token) {
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
        return token.startsWith("juta") || "jt".equals(token);
    }

    private boolean isThousandToken(String token) {
        return token.startsWith("ribu") || "rb".equals(token);
    }
}
