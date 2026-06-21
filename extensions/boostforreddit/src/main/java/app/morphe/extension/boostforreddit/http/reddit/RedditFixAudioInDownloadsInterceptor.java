/*
 * Copyright 2026 wchill.
 * Modifications Copyright 2026 brealorg.
 * https://github.com/wchill/patcheddit
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to this code.
 */

package app.morphe.extension.boostforreddit.http.reddit;

import androidx.annotation.NonNull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import app.morphe.extension.boostforreddit.http.HttpUtils;
import app.morphe.extension.boostforreddit.utils.LoggingUtils;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @noinspection unused
 */
public class RedditFixAudioInDownloadsInterceptor implements Interceptor {
    /*
     * Keep this enabled once the patch is installed. The interceptor only rewrites
     * narrow v.redd.it download URLs (/audio and /DASH_<height> fallback URLs),
     * and video downloads may request the video stream before Boost reaches the
     * original audio-specific enable() call.
     */
    private static boolean enabled = true;

    private static final Pattern AUDIO_REGEX =
            Pattern.compile("^(https?://v\\.redd\\.it/[a-z0-9]+)/audio");

    private static final Pattern VIDEO_REGEX =
            Pattern.compile("^(https?://v\\.redd\\.it/[a-z0-9]+)/DASH_([0-9]+)(?:\\.mp4)?(?:\\?source=fallback)?$");

    public static void enable() {
        enabled = true;
    }

    public static void disable() {
        enabled = false;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        String url = request.url().toString();

        if (!enabled) {
            return chain.proceed(request);
        }

        Matcher audioMatcher = AUDIO_REGEX.matcher(url);
        if (audioMatcher.find()) {
            try {
                String baseUrl = audioMatcher.group(1);
                String audioFilename = getBestAudioFilename(baseUrl);
                return HttpUtils.get(baseUrl + "/" + audioFilename);
            } catch (NoMatchingRepresentationException e) {
                return chain.proceed(request);
            } catch (Exception e) {
                LoggingUtils.logException(false, () -> "Failed to retrieve audio: " + e);
                return chain.proceed(request);
            }
        }

        Matcher videoMatcher = VIDEO_REGEX.matcher(url);
        if (videoMatcher.find()) {
            try {
                String baseUrl = videoMatcher.group(1);
                int requestedHeight = Integer.parseInt(videoMatcher.group(2));
                String videoFilename = getBestVideoFilename(baseUrl, requestedHeight);
                return HttpUtils.get(baseUrl + "/" + videoFilename);
            } catch (NoMatchingRepresentationException e) {
                return chain.proceed(request);
            } catch (Exception e) {
                LoggingUtils.logException(false, () -> "Failed to retrieve video: " + e);
                return chain.proceed(request);
            }
        }

        return chain.proceed(request);
    }

    private String getBestAudioFilename(String baseUrl) throws IOException, SAXException, ParserConfigurationException {
        try (Response dashPlaylistResponse = HttpUtils.get(baseUrl + "/DASHPlaylist.mpd")) {
            if (dashPlaylistResponse.body() == null) {
                throw new RuntimeException("DASH playlist response body was null");
            }

            try (InputStream dashPlaylistStream = dashPlaylistResponse.body().byteStream()) {
                return parseDashPlaylistForAudio(dashPlaylistStream);
            }
        }
    }

    private String getBestVideoFilename(String baseUrl, int requestedHeight) throws IOException, SAXException, ParserConfigurationException {
        try (Response dashPlaylistResponse = HttpUtils.get(baseUrl + "/DASHPlaylist.mpd")) {
            if (dashPlaylistResponse.body() == null) {
                throw new RuntimeException("DASH playlist response body was null");
            }

            try (InputStream dashPlaylistStream = dashPlaylistResponse.body().byteStream()) {
                return parseDashPlaylistForVideo(dashPlaylistStream, requestedHeight);
            }
        }
    }

    private String parseDashPlaylistForAudio(InputStream xmlDocumentStream) throws IOException, SAXException, ParserConfigurationException {
        Document doc = parseXml(xmlDocumentStream);
        NodeList representations = doc.getElementsByTagName("Representation");

        int maxBandwidth = 0;
        String baseUrl = null;

        for (int i = 0; i < representations.getLength(); i++) {
            Element representation = (Element) representations.item(i);

            if (!looksLikeAudioRepresentation(representation)) {
                continue;
            }

            String candidateBaseUrl = getFirstBaseUrl(representation);
            if (candidateBaseUrl == null || candidateBaseUrl.isBlank()) {
                continue;
            }

            int bandwidth = parseIntOrDefault(representation.getAttribute("bandwidth"), 0);
            if (bandwidth > maxBandwidth) {
                maxBandwidth = bandwidth;
                baseUrl = candidateBaseUrl;
            }
        }

        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl;
        }

        throw new NoMatchingRepresentationException();
    }

    private String parseDashPlaylistForVideo(InputStream xmlDocumentStream, int requestedHeight) throws IOException, SAXException, ParserConfigurationException {
        Document doc = parseXml(xmlDocumentStream);
        NodeList representations = doc.getElementsByTagName("Representation");

        int bestDistance = Integer.MAX_VALUE;
        int bestBandwidth = 0;
        String baseUrl = null;

        for (int i = 0; i < representations.getLength(); i++) {
            Element representation = (Element) representations.item(i);

            if (!looksLikeVideoRepresentation(representation)) {
                continue;
            }

            String candidateBaseUrl = getFirstBaseUrl(representation);
            if (candidateBaseUrl == null || candidateBaseUrl.isBlank()) {
                continue;
            }

            int height = parseIntOrDefault(representation.getAttribute("height"), -1);
            int bandwidth = parseIntOrDefault(representation.getAttribute("bandwidth"), 0);
            int distance = height >= 0 ? Math.abs(height - requestedHeight) : Integer.MAX_VALUE;

            if (baseUrl == null || distance < bestDistance || distance == bestDistance && bandwidth > bestBandwidth) {
                bestDistance = distance;
                bestBandwidth = bandwidth;
                baseUrl = candidateBaseUrl;
            }
        }

        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl;
        }

        throw new NoMatchingRepresentationException();
    }

    private Document parseXml(InputStream xmlDocumentStream) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlDocumentStream);
        doc.getDocumentElement().normalize();
        return doc;
    }

    private String getFirstBaseUrl(Element representation) {
        NodeList baseUrls = representation.getElementsByTagName("BaseURL");
        if (baseUrls == null || baseUrls.getLength() == 0 || baseUrls.item(0) == null) {
            return null;
        }

        return baseUrls.item(0).getTextContent();
    }

    private boolean looksLikeAudioRepresentation(Element representation) {
        if (attributeContains(representation, "mimeType", "audio")) return true;
        if (attributeContains(representation, "contentType", "audio")) return true;
        if (attributeStartsWith(representation, "codecs", "mp4a")) return true;

        Node parent = representation.getParentNode();
        if (parent instanceof Element) {
            Element parentElement = (Element) parent;
            if (attributeContains(parentElement, "mimeType", "audio")) return true;
            if (attributeContains(parentElement, "contentType", "audio")) return true;
            if (attributeStartsWith(parentElement, "codecs", "mp4a")) return true;
        }

        return false;
    }

    private boolean looksLikeVideoRepresentation(Element representation) {
        if (attributeContains(representation, "mimeType", "video")) return true;
        if (attributeContains(representation, "contentType", "video")) return true;
        if (attributeStartsWith(representation, "codecs", "avc")) return true;
        if (attributeStartsWith(representation, "codecs", "hvc")) return true;
        if (attributeStartsWith(representation, "codecs", "vp9")) return true;
        if (attributeStartsWith(representation, "codecs", "vp09")) return true;

        Node parent = representation.getParentNode();
        if (parent instanceof Element) {
            Element parentElement = (Element) parent;
            if (attributeContains(parentElement, "mimeType", "video")) return true;
            if (attributeContains(parentElement, "contentType", "video")) return true;
            if (attributeStartsWith(parentElement, "codecs", "avc")) return true;
            if (attributeStartsWith(parentElement, "codecs", "hvc")) return true;
            if (attributeStartsWith(parentElement, "codecs", "vp9")) return true;
            if (attributeStartsWith(parentElement, "codecs", "vp09")) return true;
        }

        return false;
    }

    private boolean attributeContains(Element element, String attributeName, String needle) {
        String value = element.getAttribute(attributeName);
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private boolean attributeStartsWith(Element element, String attributeName, String prefix) {
        String value = element.getAttribute(attributeName);
        return value != null && value.toLowerCase(Locale.ROOT).startsWith(prefix);
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static class NoMatchingRepresentationException extends RuntimeException {
    }
}
