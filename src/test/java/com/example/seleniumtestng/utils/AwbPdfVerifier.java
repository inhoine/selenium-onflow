package com.example.seleniumtestng.utils;

import com.example.seleniumtestng.models.CampaignAwbList;
import com.example.seleniumtestng.models.CampaignAwbTracking;
import com.example.seleniumtestng.models.OrderAwbInfo;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

public class AwbPdfVerifier {
    private final HttpClient client = HttpClient.newHttpClient();

    public void verifyCampaignAwbPdfs(
            CampaignAwbList campaignAwbList,
            List<OrderAwbInfo> orderAwbInfos) {
        if (campaignAwbList.awbUrls().isEmpty()) {
            throw new IllegalStateException("Campaign AWB API returned no PDF URLs");
        }
        if (campaignAwbList.trackings().isEmpty()) {
            throw new IllegalStateException("Campaign AWB API returned no tracking rows");
        }

        Map<String, Set<String>> expectedTokensByTracking = expectedTokensByTracking(orderAwbInfos);
        assertNoDuplicateCampaignTracking(campaignAwbList.trackings());
        assertAllCampaignTrackingsHaveAwbPrinted(campaignAwbList.trackings());
        assertCampaignTrackingsHaveOrderDetails(campaignAwbList.trackings(), expectedTokensByTracking);

        Map<String, Set<String>> matchedPdfUrlsByTracking = emptyMatches(expectedTokensByTracking.keySet());
        List<String> unmatchedPdfUrls = new ArrayList<>();
        for (String awbUrl : campaignAwbList.awbUrls()) {
            PdfContent pdfContent = readPdfContent(awbUrl);
            Set<String> matchedTrackings = matchingTrackings(pdfContent.compactText(), expectedTokensByTracking);
            if (matchedTrackings.isEmpty()) {
                unmatchedPdfUrls.add(awbUrl);
            }
            for (String tracking : matchedTrackings) {
                matchedPdfUrlsByTracking.get(tracking).add(awbUrl);
            }
            System.out.println("AWB PDF verified: url="
                    + awbUrl
                    + ", matchedTrackings="
                    + matchedTrackings.size()
                    + ", barcodes="
                    + String.join(",", pdfContent.barcodes()));
        }

        List<String> missingTrackings = missingTrackings(matchedPdfUrlsByTracking);
        Map<String, Set<String>> duplicateTrackings = duplicateTrackings(matchedPdfUrlsByTracking);
        if (!missingTrackings.isEmpty() || !duplicateTrackings.isEmpty() || !unmatchedPdfUrls.isEmpty()) {
            throw new IllegalStateException("Campaign AWB PDF verification failed. missing="
                    + missingTrackings
                    + ", duplicate="
                    + duplicateTrackings
                    + ", unmatchedPdfs="
                    + unmatchedPdfUrls);
        }

        System.out.println("Campaign AWB PDF verification passed. pdfs="
                + campaignAwbList.awbUrls().size()
                + ", trackings="
                + expectedTokensByTracking.size());
    }

    private Map<String, Set<String>> expectedTokensByTracking(List<OrderAwbInfo> orderAwbInfos) {
        Map<String, Set<String>> expected = new LinkedHashMap<>();
        for (OrderAwbInfo info : orderAwbInfos) {
            if (info.trackingCode() == null || info.trackingCode().isBlank()) {
                continue;
            }
            Set<String> tokens = new LinkedHashSet<>();
            addToken(tokens, info.trackingCode());
            addToken(tokens, info.carrierTrackingCode());
            addToken(tokens, info.externalTrackingCode());
            expected.put(info.trackingCode(), tokens);
        }
        return expected;
    }

    private void assertNoDuplicateCampaignTracking(List<CampaignAwbTracking> trackings) {
        Set<String> seen = new LinkedHashSet<>();
        Set<String> duplicates = new LinkedHashSet<>();
        for (CampaignAwbTracking tracking : trackings) {
            if (!seen.add(tracking.trackingCode())) {
                duplicates.add(tracking.trackingCode());
            }
        }
        if (!duplicates.isEmpty()) {
            throw new IllegalStateException("Campaign AWB API returned duplicate tracking rows: " + duplicates);
        }
    }

    private void assertAllCampaignTrackingsHaveAwbPrinted(List<CampaignAwbTracking> trackings) {
        List<String> notPrinted = new ArrayList<>();
        for (CampaignAwbTracking tracking : trackings) {
            if (!tracking.awbPrint()) {
                notPrinted.add(tracking.trackingCode());
            }
        }
        if (!notPrinted.isEmpty()) {
            throw new IllegalStateException("Campaign AWB API returned trackings not printed yet: " + notPrinted);
        }
    }

    private void assertCampaignTrackingsHaveOrderDetails(
            List<CampaignAwbTracking> trackings,
            Map<String, Set<String>> expectedTokensByTracking) {
        List<String> missingOrderDetails = new ArrayList<>();
        for (CampaignAwbTracking tracking : trackings) {
            if (!expectedTokensByTracking.containsKey(tracking.trackingCode())) {
                missingOrderDetails.add(tracking.trackingCode());
            }
        }
        if (!missingOrderDetails.isEmpty()) {
            throw new IllegalStateException("Campaign AWB API returned tracking rows missing order detail data: "
                    + missingOrderDetails);
        }
    }

    private Map<String, Set<String>> emptyMatches(Set<String> trackings) {
        Map<String, Set<String>> matches = new LinkedHashMap<>();
        for (String tracking : trackings) {
            matches.put(tracking, new LinkedHashSet<>());
        }
        return matches;
    }

    private Set<String> matchingTrackings(String compactPdfText, Map<String, Set<String>> expectedTokensByTracking) {
        Set<String> matches = new LinkedHashSet<>();
        for (Map.Entry<String, Set<String>> entry : expectedTokensByTracking.entrySet()) {
            for (String token : entry.getValue()) {
                if (!token.isBlank() && compactPdfText.contains(token)) {
                    matches.add(entry.getKey());
                    break;
                }
            }
        }
        return matches;
    }

    private List<String> missingTrackings(Map<String, Set<String>> matchedPdfUrlsByTracking) {
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : matchedPdfUrlsByTracking.entrySet()) {
            if (entry.getValue().isEmpty()) {
                missing.add(entry.getKey());
            }
        }
        return missing;
    }

    private Map<String, Set<String>> duplicateTrackings(Map<String, Set<String>> matchedPdfUrlsByTracking) {
        Map<String, Set<String>> duplicates = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : matchedPdfUrlsByTracking.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicates.put(entry.getKey(), entry.getValue());
            }
        }
        return duplicates;
    }

    private PdfContent readPdfContent(String awbUrl) {
        byte[] pdf = downloadPdf(awbUrl);
        try (PDDocument document = PDDocument.load(pdf)) {
            String text = new PDFTextStripper().getText(document);
            Set<String> barcodes = readBarcodes(document);
            String combined = text + " " + String.join(" ", barcodes);
            return new PdfContent(compact(combined), barcodes);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read AWB PDF: " + awbUrl, e);
        }
    }

    private byte[] downloadPdf(String awbUrl) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(awbUrl)).GET().build();
        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("AWB PDF download failed "
                        + response.statusCode()
                        + ": "
                        + awbUrl);
            }
            return response.body();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to download AWB PDF: " + awbUrl, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted downloading AWB PDF: " + awbUrl, e);
        }
    }

    private Set<String> readBarcodes(PDDocument document) throws IOException {
        Set<String> barcodes = new LinkedHashSet<>();
        PDFRenderer renderer = new PDFRenderer(document);
        for (int page = 0; page < document.getNumberOfPages(); page++) {
            BufferedImage image = renderer.renderImageWithDPI(page, 220);
            barcodes.addAll(readBarcodes(image));
        }
        return barcodes;
    }

    private Set<String> readBarcodes(BufferedImage image) {
        Set<String> barcodes = new LinkedHashSet<>();
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        MultiFormatReader reader = new MultiFormatReader();
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        reader.setHints(hints);
        try {
            Result[] results = new GenericMultipleBarcodeReader(reader).decodeMultiple(bitmap, hints);
            for (Result result : results) {
                addToken(barcodes, result.getText());
            }
        } catch (NotFoundException ignored) {
        }
        return barcodes;
    }

    private void addToken(Set<String> tokens, String value) {
        String compact = compact(value);
        if (!compact.isBlank()) {
            tokens.add(compact);
        }
    }

    private String compact(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    private static final class PdfContent {
        private final String compactText;
        private final Set<String> barcodes;

        private PdfContent(String compactText, Set<String> barcodes) {
            this.compactText = compactText;
            this.barcodes = barcodes;
        }

        private String compactText() {
            return compactText;
        }

        private Set<String> barcodes() {
            return barcodes;
        }
    }
}
