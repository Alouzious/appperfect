package com.example.pitchperfect.data;

import com.example.pitchperfect.models.PitchDeck;
import com.example.pitchperfect.models.PracticeSession;
import java.util.ArrayList;
import java.util.List;

public class MockDataRepository {

    public static List<PitchDeck> getMockPitchDecks() {
        List<PitchDeck> decks = new ArrayList<>();

        PitchDeck deck1 = new PitchDeck();
        deck1.setId("mock_1");
        deck1.setTitle("TechStartup Series A");
        deck1.setTotalSlides(12);
        deck1.setStatus("completed");
        decks.add(deck1);

        PitchDeck deck2 = new PitchDeck();
        deck2.setId("mock_2");
        deck2.setTitle("SaaS Product Launch");
        deck2.setTotalSlides(15);
        deck2.setStatus("completed");
        decks.add(deck2);

        PitchDeck deck3 = new PitchDeck();
        deck3.setId("mock_3");
        deck3.setTitle("Investment Round B");
        deck3.setTotalSlides(20);
        deck3.setStatus("in_progress");
        decks.add(deck3);

        return decks;
    }

    public static List<PracticeSession> getMockPracticeSessions(String deckId) {
        List<PracticeSession> sessions = new ArrayList<>();

        PracticeSession session1 = new PracticeSession();
        session1.setId("session_1");
        session1.setDeckId(deckId);
        session1.setStatus("completed");
        session1.setOverallScore(78.5);
        sessions.add(session1);

        PracticeSession session2 = new PracticeSession();
        session2.setId("session_2");
        session2.setDeckId(deckId);
        session2.setStatus("completed");
        session2.setOverallScore(82.0);
        sessions.add(session2);

        PracticeSession session3 = new PracticeSession();
        session3.setId("session_3");
        session3.setDeckId(deckId);
        session3.setStatus("completed");
        session3.setOverallScore(85.5);
        sessions.add(session3);

        return sessions;
    }

    public static class DemoPitchData {
        public static final String[] TITLES = {
            "TechStartup Pitch",
            "SaaS Product Demo",
            "Investment Pitch",
            "Product Launch"
        };

        public static final String[] DESCRIPTIONS = {
            "Innovative AI-powered fitness app that personalizes workouts using machine learning. Targets fitness enthusiasts aged 18-45. Seeking $500K for Series A.",
            "Cloud-based project management tool with AI collaboration features. Helps remote teams boost productivity by 40%. Monthly pricing: $29-99.",
            "Sustainable fashion e-commerce platform using eco-friendly materials. YoY growth: 200%. Targeting $2M Series B funding.",
            "Wearable health device tracking biometrics in real-time. FDA approved. Pre-orders: 50K units. Launch date: Q3 2026."
        };

        public static final String[] CATEGORIES = {
            "Startup",
            "SaaS",
            "Investment",
            "Product"
        };

        public static final int[] SLIDE_COUNTS = {12, 15, 18, 20};
    }

    public static class MockScores {
        public static double[] generateScores() {
            return new double[]{
                (int)(70 + Math.random() * 20),  // Pace
                (int)(65 + Math.random() * 25),  // Clarity
                (int)(72 + Math.random() * 20),  // Confidence
                (int)(68 + Math.random() * 20),  // Content
                (int)(70 + Math.random() * 20)   // Structure
            };
        }

        public static String[] STRENGTH_EXAMPLES = {
            "Clear and compelling opening hook",
            "Strong value proposition",
            "Excellent use of data and metrics",
            "Confident delivery and pacing",
            "Well-structured narrative flow",
            "Engaging storytelling",
            "Professional presentation"
        };

        public static String[] IMPROVEMENT_EXAMPLES = {
            "Add more customer success stories",
            "Slow down slightly for clarity",
            "Include competitive analysis",
            "Practice smoother transitions",
            "Emphasize unique differentiators",
            "Improve visual design",
            "Add call-to-action clarity"
        };
    }
}
